#!/usr/bin/env python3
"""Importa grabaciones humanas sobre IDs de audio existentes sin romper el contenido.

CSV esperado: key,input_path,speaker_id,accent,license,source_url,release
`key` es el texto normalizado que aparece en audio/audio_index.json.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,re,shutil,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
AUDIO=ASSETS/'audio'
CONTENT=ASSETS/'content'

def norm(s:str)->str:return re.sub(r'\s+',' ',s.strip()).casefold()
def sha(p:Path)->str:return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('manifest_csv',type=Path)
    ap.add_argument('--dry-run',action='store_true')
    args=ap.parse_args()
    if not shutil.which('ffmpeg'): raise SystemExit('ffmpeg es obligatorio')
    idx_path=AUDIO/'audio_index.json'; idx=json.loads(idx_path.read_text(encoding='utf-8'))
    entries=idx['entries']; changed=[]
    rows=list(csv.DictReader(args.manifest_csv.open(encoding='utf-8-sig',newline='')))
    required={'key','input_path','speaker_id','accent','license','source_url','release'}
    if not rows or not required.issubset(rows[0]): raise SystemExit(f'CSV debe incluir: {sorted(required)}')
    for row in rows:
        key=norm(row['key'])
        if key not in entries: raise SystemExit(f'key no existe en audio_index: {row["key"]}')
        for f in ('speaker_id','accent','license','source_url','release'):
            if not row.get(f,'').strip(): raise SystemExit(f'{row["key"]}: falta provenance {f}')
        src=(args.manifest_csv.parent/row['input_path']).resolve()
        if not src.exists(): raise SystemExit(f'no existe: {src}')
        out=ASSETS/entries[key]['rel']
        if args.dry_run:
            print(f'DRY {src} -> {out}')
            continue
        out.parent.mkdir(parents=True,exist_ok=True)
        with tempfile.TemporaryDirectory() as td:
            tmp=Path(td)/'normalized.ogg'
            subprocess.run([
                'ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(src),
                '-af','loudnorm=I=-20:TP=-2:LRA=7','-ac','1','-ar','44100',
                '-c:a','libvorbis','-q:a','4',str(tmp)
            ],check=True)
            tmp.replace(out)
        e=entries[key]
        e.update({
            'sha256':sha(out),'source':'human recording','synthetic':False,
            'speakerId':row['speaker_id'].strip(),'accent':row['accent'].strip(),
            'license':row['license'].strip(),'sourceUrl':row['source_url'].strip(),
            'release':row['release'].strip()
        })
        e.pop('voice',None); e.pop('speedWpm',None)
        changed.append(key)
    if args.dry_run:return
    flags=[]
    for e in entries.values():
        is_syn=e.get('synthetic')
        if is_syn is None: is_syn='synthetic' in (e.get('source') or '').casefold()
        flags.append(bool(is_syn))
    idx['synthetic']=all(flags)
    idx['audioMode']='synthetic' if all(flags) else ('human' if not any(flags) else 'mixed')
    idx['humanEntries']=sum(not f for f in flags)
    idx['syntheticEntries']=sum(flags)
    idx_path.write_text(json.dumps(idx,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manifest_path=CONTENT/'manifest.json'; manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
    audio=manifest.setdefault('audio',{})
    audio.update({
        'index':'audio/audio_index.json','indexSha256':sha(idx_path),
        'synthetic':idx['synthetic'],'audioMode':idx['audioMode'],
        'humanEntries':idx['humanEntries'],'syntheticEntries':idx['syntheticEntries']
    })
    manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Imported {len(changed)} human recordings; mode={idx["audioMode"]}')

if __name__=='__main__':main()
