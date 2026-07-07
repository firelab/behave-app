#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "Usage: $0 <ip-address> <username>"
    echo "  Deploys the latest Windows ZIP and launches Behave7 on the logged-in desktop."
    echo ""
    echo "Example: $0 192.0.2.10 winuser"
    exit 1
}

if [ $# -lt 2 ]; then
    usage
fi

HOST="$1"
USER="$2"
REMOTE="${USER}@${HOST}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../projects/behave" && pwd)"
OUTPUT_DIR="${PROJECT_DIR}/output/windows"
REMOTE_DIR="C:/Users/${USER}/behave7-test"

# Find the latest ZIP
ZIP_FILE=$(ls "${OUTPUT_DIR}"/behave7-*-windows-amd64.zip 2>/dev/null | sort -V | tail -1)
if [ -z "$ZIP_FILE" ]; then
    echo "ERROR: No Windows ZIP found in ${OUTPUT_DIR}"
    exit 1
fi
ZIP_NAME=$(basename "$ZIP_FILE")

echo "==> Using ZIP: ${ZIP_NAME}"
echo "==> Target: ${REMOTE}"
echo "==> Remote dir: ${REMOTE_DIR}"
echo ""

# 1. Create remote test directory
echo "==> Creating remote directory..."
ssh "$REMOTE" "mkdir -p '${REMOTE_DIR}'"

# 2. Copy the ZIP
echo "==> Copying ${ZIP_NAME} to remote..."
cat "$ZIP_FILE" | ssh "$REMOTE" "cat > '${REMOTE_DIR}/${ZIP_NAME}'"

# 3. Extract and launch via shortcut
echo "==> Extracting ZIP and launching Behave7..."
ssh "$REMOTE" bash -s "$REMOTE_DIR" "$ZIP_NAME" <<'REMOTE_SCRIPT'
REMOTE_DIR="$1"
ZIP_NAME="$2"

cd "$REMOTE_DIR"

# Kill any running Behave7 before extracting
echo "  -> Stopping any running Behave7 instances..."
powershell -Command "Get-Process -Name Behave7 -ErrorAction SilentlyContinue | Stop-Process -Force" 2>/dev/null
powershell -Command "Get-Process -Name jcef_helper -ErrorAction SilentlyContinue | Stop-Process -Force" 2>/dev/null
sleep 2

# Extract (overwrite existing)
echo "  -> Extracting ${ZIP_NAME}..."
powershell -Command "Expand-Archive -Path '${REMOTE_DIR}/${ZIP_NAME}' -DestinationPath '${REMOTE_DIR}' -Force"

# Verify launcher and app exe exist
if [ ! -f "${REMOTE_DIR}/Behave7.exe" ]; then
    echo "ERROR: Behave7.exe launcher not found after extraction"
    exit 1
fi
if [ ! -f "${REMOTE_DIR}/bin/Behave7.exe" ]; then
    echo "ERROR: bin/Behave7.exe not found after extraction"
    exit 1
fi
echo "  -> Found launcher and app exe"

# Launch via a scheduled task with the interactive flag so the window appears
# on the logged-in user's desktop. A plain Start-Process from SSH would run the
# app in Session 0 (hidden desktop) — process runs, but no visible window.
echo "  -> Launching Behave7.exe into the interactive desktop session..."
WINDIR=$(cd "$REMOTE_DIR" && pwd -W | sed 's|/|\\|g')
powershell -Command "schtasks /create /tn Behave7Test /tr '${WINDIR}\\Behave7.exe' /sc once /st 00:00 /it /f | Out-Null; schtasks /run /tn Behave7Test | Out-Null"

# Verify the app actually started, and in which session
echo "  -> Verifying app process..."
sleep 5
powershell -Command "
  \$p = Get-Process -Name Behave7 -ErrorAction SilentlyContinue
  if (-not \$p) {
      Write-Output '  -> FAILED: Behave7 process is not running.'
      exit 1
  }
  \$console = (Get-Process -Name explorer -ErrorAction SilentlyContinue | Select-Object -First 1).SessionId
  \$p | ForEach-Object {
      if (\$_.SessionId -eq 0) {
          Write-Output ('  -> WARNING: Behave7 (PID ' + \$_.Id + ') is in Session 0 (hidden). Is a user logged in?')
      } else {
          Write-Output ('  -> SUCCESS: Behave7 (PID ' + \$_.Id + ') running in interactive Session ' + \$_.SessionId + '.')
      }
  }
"
STATUS=$?

# Clean up the scheduled task (does not stop the launched app)
powershell -Command "schtasks /delete /tn Behave7Test /f | Out-Null" 2>/dev/null
exit $STATUS
REMOTE_SCRIPT
