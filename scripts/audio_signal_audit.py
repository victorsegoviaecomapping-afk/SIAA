#!/usr/bin/env python3
from __future__ import annotations
import json,re,shutil,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; ASSETS=ROOT/'app/src/main/assets'; AUDIO=ASSETS/'audio'
idx=json.loads((AUDIO/'audio_index.json').read_text(encoding='utf-8'))
if not shutil.which('ffprobe'): raise SystemExit('ffprobe no disponible')
errors=[]; checked=0
for key,e in idx.get('entries',{}).items():
    # El riesgo pedagógico mayor está en contrastes fonológicos y grabaciones humanas sustituidas.
    if e.get('kind')!='phonology' and e.get('synthetic',True) is not False: continue
    p=ASSETS/e['rel']; checked+=1
    cp=subprocess.run(['ffprobe','-v','error','-show_entries','stream=duration,channels,sample_rate','-of','default=nw=1',str(p)],capture_output=True,text=True)
    if cp.returncode: errors.append(f'{e["rel"]}: ffprobe failed'); continue
    vals={}
    for line in cp.stdout.splitlines():
        if '=' in line:
            a,b=line.split('=',1); vals[a]=b
    try: dur=float(vals.get('duration','nan'))
    except ValueError: dur=float('nan')
    if not (0.12 <= dur <= 30.0): errors.append(f'{e["rel"]}: suspicious duration {dur}')
    if vals.get('channels') not in {'1',None}: errors.append(f'{e["rel"]}: expected mono, got {vals.get("channels")} channels')
    if e.get('synthetic',True) is False:
        for f in ('speakerId','accent','license','sourceUrl','release'):
            if not (e.get(f) or '').strip(): errors.append(f'{e["rel"]}: human audio missing {f}')
print(f'audio signal audit: {checked} critical/human assets checked')
if errors:
    for x in errors[:100]: print('ERROR',x)
    sys.exit(1)
print('audio signal audit OK')
