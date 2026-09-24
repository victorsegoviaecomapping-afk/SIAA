#!/usr/bin/env python3
import hashlib
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]

# 1. Verificar manifest
manifest_file = root / 'project_manifest.json'
if manifest_file.exists():
    manifest = json.loads(manifest_file.read_text(encoding='utf-8'))
    assert manifest.get('project') == 'SIAA', "project_manifest.json must have project == 'SIAA'"

# 2. Verificar Master PDF
pdf = root / 'reference/SIAA_documento_maestro_v1_5_android_v0_7.pdf'
pdf_expected = root / 'reference/MASTER_PDF_SHA256.txt'
if pdf.exists() and pdf_expected.exists():
    digest = hashlib.sha256(pdf.read_bytes()).hexdigest()
    expected = pdf_expected.read_text(encoding='utf-8').strip()
    assert digest == expected, f'Master PDF hash mismatch: {digest} vs {expected}'

# 3. Exclusiones para archivos relevantes
ignore_prefixes = {'.git', '.gradle', '.aistudio', '.build-outputs', '.idea'}
ignore_names = {'.DS_Store', 'local.properties', 'FILES_SHA256.txt'}

def is_ignored(rel_path: Path) -> bool:
    parts = rel_path.parts
    if any(p in ignore_prefixes for p in parts):
        return True
    if any(p == 'build' for p in parts):
        return True
    if any(p == '__pycache__' for p in parts):
        return True
    if rel_path.name in ignore_names or rel_path.suffix in {'.pyc', '.tmp', '.hprof'}:
        return True
    return False

# 4. Verificar FILES_SHA256.txt
files_sha = root / 'FILES_SHA256.txt'
errors = []
tracked_files = set()

if files_sha.exists():
    lines = files_sha.read_text(encoding='utf-8').splitlines()
    for line in lines:
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        parts = line.split(maxsplit=1)
        if len(parts) != 2:
            continue
        expected_hash, file_path_str = parts
        rel_str = file_path_str[2:] if file_path_str.startswith('./') else file_path_str.lstrip('/')
        target_file = root / rel_str
        tracked_files.add(Path(rel_str))

        if not target_file.exists():
            errors.append(f"MISSING: {rel_str}")
        else:
            actual_hash = hashlib.sha256(target_file.read_bytes()).hexdigest()
            if actual_hash.lower() != expected_hash.lower():
                errors.append(f"HASH MISMATCH: {rel_str} (expected {expected_hash}, got {actual_hash})")

# 5. Detectar archivos relevantes no listados
for p in root.rglob('*'):
    if p.is_file():
        rel = p.relative_to(root)
        if not is_ignored(rel):
            if rel not in tracked_files:
                errors.append(f"UNTRACKED RELEVANT FILE: {rel}")

if errors:
    print("VERIFICATION FAILED:")
    for err in errors:
        print(f" - {err}")
    sys.exit(1)

print(f"release verification OK: {len(tracked_files)} files verified cleanly")

