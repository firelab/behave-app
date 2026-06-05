#!/usr/bin/env python3
"""Regenerate projects/behave/zip-extras/Behave7.lnk.

Emits a relative Windows shortcut whose target is the signed launcher
EXE (bin/Behave7.exe) rather than explorer.exe. When the target EXE is
Authenticode-signed, Windows reads the publisher from its certificate
and displays "Spatial Informatics Group" on the shortcut's properties
dialog. See Jira BHP1-1548.

Usage:
    python3 -m venv .lnk-venv
    ./.lnk-venv/bin/pip install pylnk3
    ./.lnk-venv/bin/python projects/behave/scripts/make-windows-shortcut.py

Writes to projects/behave/zip-extras/Behave7.lnk (relative to repo root).
"""

from pathlib import Path

import pylnk3

REPO_ROOT = Path(__file__).resolve().parents[3]
OUT = REPO_ROOT / "projects/behave/zip-extras/Behave7.lnk"

TARGET_RELATIVE = r"bin\Behave7.exe"
WORK_DIR = r"bin"
ICON_PATH = r"app\app.ico"
DESCRIPTION = "Behave7"


def main() -> None:
    lnk = pylnk3.create(str(OUT))
    lnk.link_flags.IsUnicode = True
    lnk.link_flags.HasRelativePath = True
    lnk.link_flags.HasWorkingDir = True
    lnk.link_flags.HasIconLocation = True
    lnk.link_flags.HasName = True
    # EnableTargetMetadata tells Windows to refresh publisher / file
    # metadata from the resolved target — important for picking up the
    # Authenticode publisher ("Spatial Informatics Group") from the
    # signed Behave7.exe.
    lnk.link_flags.EnableTargetMetadata = True
    lnk.link_info = None
    lnk.relative_path = TARGET_RELATIVE
    lnk.work_dir = WORK_DIR
    lnk.icon = ICON_PATH
    lnk.description = DESCRIPTION
    lnk.save()
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
