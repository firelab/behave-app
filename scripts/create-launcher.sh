#!/usr/bin/env bash
set -euo pipefail

# Builds the portable Behave7 launcher exe on a remote Windows box and copies
# it to projects/behave/zip-extras/Behave7.exe.
#
# The launcher is a tiny .NET exe with the app icon embedded that starts
# bin\Behave7.exe relative to its own location — this is the only approach
# that gives a double-clickable root item with the correct icon regardless
# of where the ZIP is extracted (.lnk files cannot resolve relative icon
# paths; see scripts/create-shortcut.sh history).
#
# Requires: SSH access to a Windows box with .NET Framework 4.x (csc.exe).

usage() {
    echo "Usage: $0 <ip-address> <username>"
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
ICON="${SCRIPT_DIR}/../projects/behave/resources/public/images/favicon.ico"
OUTPUT="${SCRIPT_DIR}/../projects/behave/zip-extras/Behave7.exe"
BUILD_DIR="C:/Users/${USER}/launcher-build"
CSC="C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe"

echo "==> Uploading source and icon to ${REMOTE}..."
ssh "$REMOTE" "mkdir -p '${BUILD_DIR}'"
cat "${SCRIPT_DIR}/launcher/Launcher.cs" | ssh "$REMOTE" "cat > '${BUILD_DIR}/Launcher.cs'"
cat "$ICON" | ssh "$REMOTE" "cat > '${BUILD_DIR}/behave7.ico'"

echo "==> Compiling with csc.exe..."
ssh "$REMOTE" "cd '${BUILD_DIR}' && '${CSC}' -nologo -target:winexe -out:Behave7.exe -win32icon:behave7.ico -r:System.Windows.Forms.dll Launcher.cs"

echo "==> Copying launcher back..."
ssh "$REMOTE" "cat '${BUILD_DIR}/Behave7.exe'" > "$OUTPUT"

echo "Created: ${OUTPUT}"
