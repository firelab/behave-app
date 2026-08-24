#!/usr/bin/env bash
#
# Sign the Windows Behave7 package with Azure Trusted Signing.
#
# This is a thin wrapper around the az-cli repo's `sign-zip.sh`. It runs that
# script inside az-cli's direnv environment (Nix devShell + the AZURE_REGION_DOMAIN
# / TS_ALIAS exports from az-cli/.envrc), then drops the signed archive back next
# to the original in output/windows/.
#
# Usage:
#   scripts/sign-windows-zip.sh [ZIP_FILE]
#
#   ZIP_FILE   Optional. Defaults to the newest unsigned
#              output/windows/behave7-*-windows-amd64.zip.
#
# Environment overrides:
#   AZ_CLI_DIR           Path to the az-cli repo
#                        (default: $HOME/az-cli)
#   TS_ALIAS             Signing alias (else az-cli/.envrc default)
#   AZURE_REGION_DOMAIN  Region domain (else az-cli/.envrc default)
#
# Prerequisites: `az login` must have been run, and az-cli/.envrc must be
# direnv-allowed (this script will allow it if it is not).

set -euo pipefail

AZ_CLI_DIR="${AZ_CLI_DIR:-$HOME/az-cli}"
OUTPUT_DIR="output/windows"

die() { echo "Error: $*" >&2; exit 1; }

command -v direnv >/dev/null 2>&1 || die "direnv is required but not on PATH."
[[ -d "$AZ_CLI_DIR" ]]            || die "az-cli dir not found: $AZ_CLI_DIR (set AZ_CLI_DIR)."
[[ -f "$AZ_CLI_DIR/sign-zip.sh" ]] || die "sign-zip.sh missing in $AZ_CLI_DIR."

## Resolve the zip to sign

zip_file="${1:-}"
if [[ -z "$zip_file" ]]; then
    zip_file="$(ls -t "$OUTPUT_DIR"/behave7-*-windows-amd64.zip 2>/dev/null \
                    | grep -v -- '-signed\.zip' | head -n1 || true)"
    [[ -n "$zip_file" ]] || die "No unsigned zip in $OUTPUT_DIR; pass one explicitly."
fi
[[ -f "$zip_file" ]] || die "Zip not found: $zip_file"

# Absolute path so it resolves after we cd into the az-cli dir.
zip_abs="$(cd "$(dirname "$zip_file")" && pwd)/$(basename "$zip_file")"

## Detect version (az-cli only auto-detects it when anchored at the end of the
## filename; ours has it in the middle, e.g. behave7-7.1.5-windows-amd64.zip).

version="$(basename "$zip_file" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1 || true)"
[[ -n "$version" ]] || die "Could not detect version from $(basename "$zip_file")."

signed_name="$(basename "$zip_file" .zip)-signed.zip"

echo "Signing $zip_abs (v$version) via $AZ_CLI_DIR"

## Sign inside az-cli's direnv env, from its dir (sign-zip.sh uses relative paths).

(
    cd "$AZ_CLI_DIR"
    direnv allow . >/dev/null 2>&1 || true
    direnv exec . ./sign-zip.sh -f "$zip_abs" -v "$version"
)

signed_src="$AZ_CLI_DIR/$signed_name"
[[ -f "$signed_src" ]] || die "Expected signed file not produced: $signed_src"

dest="$OUTPUT_DIR/$signed_name"
mv -f "$signed_src" "$dest"
echo "✓ Signed package: $dest"
