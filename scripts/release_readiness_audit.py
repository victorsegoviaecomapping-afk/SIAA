#!/usr/bin/env python3
from __future__ import annotations
import json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
errors=[]

def need(path:str):
    p=ROOT/path
    if not p.exists(): errors.append(f'missing {path}')
    return p

pm=json.loads(need('project_manifest.json').read_text(encoding='utf-8'))
md=json.loads(need('metadata.json').read_text(encoding='utf-8'))
cm=json.loads(need('app/src/main/assets/content/manifest.json').read_text(encoding='utf-8'))
ai=json.loads(need('app/src/main/assets/audio/audio_index.json').read_text(encoding='utf-8'))
build=need('app/build.gradle.kts').read_text(encoding='utf-8')
for label,val in [('project',pm.get('version')),('metadata',md.get('version')),('content',cm.get('version')),('audio',ai.get('version'))]:
    if val!='2.3.0': errors.append(f'{label} version={val!r}, expected 2.3.0')
if not re.search(r'versionCode\s*=\s*23\b',build): errors.append('app versionCode != 23')
if 'versionName = "2.3.0"' not in build: errors.append('app versionName != 2.3.0')

# Release engineering assets that make the remaining external QA executable.
for rel in [
    '.github/workflows/android-ci.yml','.github/workflows/android-device-tests.yml',
    'app/src/androidTest/java/com/siaa/app/media/EarbudCommandRouterInstrumentedTest.kt',
    'scripts/device_qa_collect.sh','scripts/device_media_probe.sh',
    'scripts/import_human_audio.py','scripts/audio_signal_audit.py','scripts/linguistic_qa_audit.py',
    'docs/DEVICE_QA_PROTOCOL_V23.md','docs/LINGUISTIC_QA_V23.md',
    'docs/HUMAN_AUDIO_PIPELINE_V23.md','docs/RELEASE_READINESS_V23.md'
]: need(rel)

service=need('app/src/main/java/com/siaa/app/media/SiaaPlaybackService.kt').read_text(encoding='utf-8')
for marker in ['stopServiceIfIdle(startId)','finishService(startId)','calibrationActive','audioFocus.request()','hasSafePrivateOutput()']:
    if marker not in service: errors.append(f'service hardening marker absent: {marker}')

kcs=json.loads(need('app/src/main/assets/content/kcs.json').read_text(encoding='utf-8'))
ex=json.loads(need('app/src/main/assets/content/exercises.json').read_text(encoding='utf-8'))
ids={k['id'] for k in kcs if k['id'].startswith('G_CEFRJ')}
if len(ids)!=411: errors.append(f'CEFR-J KC count {len(ids)} != 411')
counts={kid:{'TEACH':0,'AB':0,'SELF_ASSESS':0} for kid in ids}
for q in ex:
    kid=next((x for x in q.get('kcIds',[]) if x in counts),None)
    if kid and q.get('type') in counts[kid]: counts[kid][q['type']]+=1
for kid,c in counts.items():
    if c != {'TEACH':1,'AB':1,'SELF_ASSESS':1}: errors.append(f'{kid}: learner-facing coverage {c}')

entries=ai.get('entries',{})
candidates=json.loads(need('app/src/main/assets/reference/human_audio/candidates.json').read_text(encoding='utf-8'))
for c in candidates.get('candidates',[]):
    direct=c.get('directIndexKey')
    if c.get('importReady') and (not direct or direct not in entries):
        errors.append(f'human candidate {c.get("id")}: importReady but directIndexKey is not valid')
    if not c.get('importReady') and direct:
        errors.append(f'human candidate {c.get("id")}: non-ready candidate should not expose a directIndexKey')
human=sum(1 for e in entries.values() if e.get('synthetic',True) is False)
if human:
    # Human audio is allowed, but every activated entry must be fully attributable.
    for key,e in entries.items():
        if e.get('synthetic',True) is False:
            for f in ('speakerId','accent','license','sourceUrl','release'):
                if not str(e.get(f,'')).strip(): errors.append(f'human audio {key}: missing {f}')

print(f'release readiness: versions=2.3.0 cefrj={len(ids)} learner_tasks={sum(sum(v.values()) for v in counts.values())} audio={len(entries)} human={human}')
if errors:
    for e in errors[:100]: print('ERROR',e)
    if len(errors)>100: print('ERROR ...',len(errors)-100,'more')
    sys.exit(1)
print('release readiness audit OK')
