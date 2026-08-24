#!/usr/bin/env bash
set -euo pipefail

# Creates a portable Behave7.lnk and copies it to projects/behave/zip-extras/.
#
# The shortcut targets cmd.exe (exists on every Windows box at the same path)
# with a relative path argument, so it works regardless of where the ZIP is extracted.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT="${SCRIPT_DIR}/../projects/behave/zip-extras/Behave7.lnk"

"${SCRIPT_DIR}/mslink.sh" \
    -l 'C:\Windows\System32\cmd.exe' \
    -a '/c start "" ".\bin\Behave7.exe"' \
    -i '.\bin\Behave7.exe' \
    -n 'BehavePlus 7' \
    -o "$OUTPUT"

echo "Created: ${OUTPUT}"
