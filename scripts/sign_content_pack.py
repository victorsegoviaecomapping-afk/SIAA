#!/usr/bin/env python3
"""Build and sign a compact SIAA content pack.

The private key is supplied externally and is never stored in the repository.
Every file shipped in the pack is SHA-256 covered by the ECDSA signature.
"""
from __future__ import annotations
import argparse, base64, hashlib, json, shutil, subprocess, tempfile, zipfile
from pathlib import Path

REQUIRED=("kcs.json","edges.json","exercises.json","lexemes.json")
EXTRAS=("practice/practice_catalog.json",)

def sha(p: Path) -> str:
    return hashlib.sha256(p.read_bytes()).hexdigest()

def canonical(m: dict) -> str:
    parts=[m.get("id",""),m.get("version",""),m.get("schemaVersion","")]
    parts += [f"{n}={m['checksums'].get(n,'')}" for n in REQUIRED]
    parts += [f"{n}={m.get('packChecksums',{}).get(n,'')}" for n in sorted(m.get('packChecksums',{}))]
    return "|".join(parts)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--root',default='.')
    ap.add_argument('--private-key',required=True)
    ap.add_argument('--out',required=True)
    a=ap.parse_args()
    root=Path(a.root).resolve(); assets=root/'app/src/main/assets'; content=assets/'content'
    with tempfile.TemporaryDirectory() as td:
        stage=Path(td)
        m=json.loads((content/'manifest.json').read_text())
        m['id']='siaa-core-content'; m['version']='2.4.0'; m['contentVersion']='2.4.0'; m['schemaVersion']='1.1'
        m['checksums']={n:sha(content/n) for n in REQUIRED}
        m['packChecksums']={}
        for n in REQUIRED:
            shutil.copy2(content/n,stage/n)
        for rel in EXTRAS:
            src=assets/rel
            if src.exists():
                dst=stage/rel; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src,dst)
                m['packChecksums'][rel]=sha(src)
        payload_file=stage/'payload.txt'; payload_file.write_text(canonical(m),encoding='utf-8')
        sig_file=stage/'sig.bin'
        subprocess.run(['openssl','dgst','-sha256','-sign',str(Path(a.private_key).resolve()),'-out',str(sig_file),str(payload_file)],check=True)
        m['signature']={'algorithm':'SHA256withECDSA','keyId':'siaa-content-v24-dev-p256','value':base64.b64encode(sig_file.read_bytes()).decode()}
        (stage/'manifest.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        out=Path(a.out); out.parent.mkdir(parents=True,exist_ok=True)
        with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
            for f in sorted(stage.rglob('*')):
                if f.is_file() and f.name not in {'payload.txt','sig.bin'}:
                    z.write(f,f.relative_to(stage).as_posix())
        print(out, hashlib.sha256(out.read_bytes()).hexdigest())
if __name__=='__main__': main()
