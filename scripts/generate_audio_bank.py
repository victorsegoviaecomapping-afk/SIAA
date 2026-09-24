#!/usr/bin/env python3
from __future__ import annotations
import json, os, re, subprocess, tempfile, hashlib
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
CONTENT=ASSETS/'content'
AUDIO=ASSETS/'audio'
(AUDIO/'vocab').mkdir(parents=True,exist_ok=True)
(AUDIO/'listening').mkdir(parents=True,exist_ok=True)
(AUDIO/'phonology').mkdir(parents=True,exist_ok=True)

lex=json.loads((CONTENT/'lexemes.json').read_text(encoding='utf-8'))
exercises=json.loads((CONTENT/'exercises.json').read_text(encoding='utf-8'))

def norm(t:str)->str:
    return re.sub(r'\s+',' ',t.strip()).casefold()

def voice_for(kind,level,key):
    if kind in ('listening','phrase','phonology'):
        # Alternating US/UK provides modest accent variability without pretending to be human speech.
        return 'en-us' if int(hashlib.sha1(key.encode()).hexdigest()[-1],16)%2==0 else 'en-gb'
    return 'en-us'

def speed_for(kind,level):
    if kind=='vocab': return 145
    if kind=='phrase': return {'Pre-A1':118,'A1':125,'A2':132,'B1':140,'B2':148,'C1':154,'C2':160}.get(level,145)
    return {'Pre-A1':118,'A1':125,'A2':135,'B1':145,'B2':155,'C1':165,'C2':175}.get(level,145)

items=[]
seen=set()
for lx in lex:
    text=lx['lemma'].strip(); key=norm(text)
    if not text or key in seen: continue
    seen.add(key)
    items.append({'text':text,'key':key,'kind':'vocab','level':lx.get('cefr','A1'),'rel':f"audio/vocab/{lx['id'].lower()}.ogg"})

for e in exercises:
    text=(e.get('stimulusEn') or '').strip(); typ=e.get('type',''); tags=set(e.get('tags',[]));
    if not text or norm(text) in seen: continue
    kind=None
    if typ=='PRON_DISCRIMINATION' or 'pronunciation' in tags: kind='phonology'
    elif 'phrase-audio' in tags: kind='phrase'
    elif typ=='LISTENING_AB' or 'audio-asset' in tags: kind='listening'
    if not kind: continue
    key=norm(text); seen.add(key)
    items.append({'text':text,'key':key,'kind':kind,'level':e.get('cefr','A1'),'rel':f"audio/{kind}/{e['id'].lower()}.ogg"})

def make_one(item):
    out=ASSETS/item['rel']; out.parent.mkdir(parents=True,exist_ok=True)
    voice=voice_for(item['kind'],item['level'],item['key']); speed=speed_for(item['kind'],item['level'])
    if not out.exists() or out.stat().st_size < 300:
        with tempfile.TemporaryDirectory() as td:
            wav=Path(td)/'x.wav'
            subprocess.run(['espeak','-v',voice,'-s',str(speed),'-w',str(wav),item['text']],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','2','-ac','1',str(out)],check=True)
    return {**item,'voice':voice,'speedWpm':speed,'bytes':out.stat().st_size,
            'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),
            'source':'SIAA pre-rendered synthetic speech (eSpeak + Vorbis); not human-recorded audio'}

workers=max(2,min(10,(os.cpu_count() or 4)))
results=[]
with ThreadPoolExecutor(max_workers=workers) as pool:
    futs=[pool.submit(make_one,x) for x in items]
    for n,f in enumerate(as_completed(futs),1):
        results.append(f.result())
        if n%200==0: print(f'audio {n}/{len(items)}')
results.sort(key=lambda x:(x['kind'],x['rel']))
content_manifest=json.loads((CONTENT/'manifest.json').read_text(encoding='utf-8'))
index={'version':content_manifest.get('version','1.2.0'),'normalization':'trim + collapse whitespace + Unicode casefold','defaultLanguage':'en-US',
       'synthetic':True,'engine':'eSpeak','codec':'Ogg Vorbis',
       'entries':{r['key']:{k:v for k,v in r.items() if k not in ('key','bytes')} for r in results},
       'counts':{},'totalBytes':sum(r['bytes'] for r in results)}
for r in results:index['counts'][r['kind']]=index['counts'].get(r['kind'],0)+1
index_path=AUDIO/'audio_index.json'
index_path.write_text(json.dumps(index,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
# Keep the content manifest cryptographically tied to the exact generated audio index.
manifest_path=CONTENT/'manifest.json'
manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
manifest['audio']={
    'index':'audio/audio_index.json',
    'indexSha256':hashlib.sha256(index_path.read_bytes()).hexdigest(),
    'synthetic':True,
    'engine':'eSpeak',
    'codec':'Ogg Vorbis',
    'counts':index['counts'],
    'totalBytes':index['totalBytes']
}
manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'items':len(results),'counts':index['counts'],'bytes':index['totalBytes']},indent=2))
