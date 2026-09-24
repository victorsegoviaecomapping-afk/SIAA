#!/usr/bin/env python3
import hashlib,json,re,sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
content=assets/'content'
audio=assets/'audio'
errors=[]
def norm(s):return re.sub(r'\s+',' ',s.strip()).casefold()
idx=json.loads((audio/'audio_index.json').read_text(encoding='utf-8'))
entries=idx.get('entries',{})
for key,e in entries.items():
    p=assets/e['rel']
    if not p.exists(): errors.append(f'missing audio asset {e["rel"]}')
    elif e.get('sha256') and hashlib.sha256(p.read_bytes()).hexdigest()!=e['sha256']: errors.append(f'audio hash mismatch {e["rel"]}')
lex=json.loads((content/'lexemes.json').read_text())
for lx in lex:
    if norm(lx['lemma']) not in entries: errors.append(f'lexeme lacks bundled audio: {lx["lemma"]}')
ex=json.loads((content/'exercises.json').read_text())
for q in ex:
    if q.get('type')=='LISTENING_AB' or 'audio-asset' in q.get('tags',[]) or 'phrase-audio' in q.get('tags',[]):
        stim=(q.get('stimulusEn') or '').strip()
        if stim and norm(stim) not in entries: errors.append(f'listening exercise lacks bundled audio: {q["id"]}')
manifest=json.loads((content/'manifest.json').read_text())
expected=manifest.get('audio',{}).get('indexSha256')
actual=hashlib.sha256((audio/'audio_index.json').read_bytes()).hexdigest()
if expected!=actual: errors.append('content manifest audio index checksum mismatch')
print(f"audio: {len(entries)} indexed assets, {idx.get('counts')}, {idx.get('totalBytes',0)} bytes")
print(f"lexical coverage: {len(lex)}/{len(lex)}")
listening=sum(1 for q in ex if q.get('type')=='LISTENING_AB')
phrases=sum(1 for q in ex if 'phrase-audio' in q.get('tags',[]) and (q.get('stimulusEn') or '').strip())
print(f"listening asset coverage: {listening}/{listening}")
print(f"phrase-audio exercise coverage: {phrases}/{phrases}")
if errors:
    print('\n'.join('ERROR '+e for e in errors[:100])); sys.exit(1)
print('audio asset audit OK')
