#!/usr/bin/env python3
"""Offline SQL smoke test for Room MIGRATION_1_2 and MIGRATION_2_3.

This does not replace Android's MigrationTestHelper, but it executes the exact SQL
strings from Migrations.kt against SQLite and verifies preservation/constraints.
"""
from pathlib import Path
import re
import sqlite3
import textwrap

root = Path(__file__).resolve().parents[1]
source = (root / 'core/data/src/main/java/com/siaa/core/data/migration/Migrations.kt').read_text(encoding='utf-8')
pattern = re.compile(r'db\.execSQL\((?:"""(.*?)"""\.trimIndent\(\)|"((?:\\.|[^"\\])*)")\)', re.S)
sqls = []
for triple, single in pattern.findall(source):
    if triple:
        sqls.append(textwrap.dedent(triple).strip())
    else:
        sqls.append(bytes(single, 'utf-8').decode('unicode_escape'))

assert len(sqls) >= 10, f'Could not extract migration SQL; found {len(sqls)} statements'

con = sqlite3.connect(':memory:')
cur = con.cursor()
cur.executescript('''
CREATE TABLE learner_kc_state (
  kcId TEXT PRIMARY KEY NOT NULL,
  mastery REAL NOT NULL, recognition REAL NOT NULL, production REAL NOT NULL,
  orthography REAL NOT NULL, automaticity REAL NOT NULL, halfLifeHours REAL NOT NULL,
  uncertainty REAL NOT NULL, lastReviewedAtEpochMs INTEGER,
  consecutiveSuccess INTEGER NOT NULL, consecutiveFailure INTEGER NOT NULL,
  totalAttempts INTEGER NOT NULL, totalCorrect INTEGER NOT NULL
);
CREATE TABLE exercises (
  id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, kcIdsCsv TEXT NOT NULL,
  cefr TEXT NOT NULL, difficulty REAL NOT NULL, promptEs TEXT NOT NULL,
  stimulusEn TEXT NOT NULL, optionA TEXT NOT NULL, optionB TEXT NOT NULL,
  correctOption TEXT NOT NULL, explanationEs TEXT NOT NULL, spellTarget TEXT NOT NULL,
  estimatedSeconds INTEGER NOT NULL, tagsCsv TEXT NOT NULL
);
CREATE TABLE device_profiles (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL,
  playPauseAvailable INTEGER NOT NULL, nextAvailable INTEGER NOT NULL,
  previousAvailable INTEGER NOT NULL, lastSeenAtEpochMs INTEGER NOT NULL
);
CREATE TABLE interactions (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  sessionId INTEGER NOT NULL, exerciseId TEXT NOT NULL, timestampEpochMs INTEGER NOT NULL,
  response TEXT NOT NULL, correct INTEGER NOT NULL, confidence TEXT,
  latencyMs INTEGER, hintDepth INTEGER NOT NULL, plannerScore REAL,
  stateBeforeMastery REAL, stateAfterMastery REAL
);
''')
cur.execute("INSERT INTO learner_kc_state VALUES ('G',.4,.4,.3,.3,.2,8,.4,NULL,1,0,3,2)")
cur.execute("INSERT INTO exercises VALUES ('E','AB','G','A1',.4,'q','','a','b','B','','',20,'x')")
cur.execute("INSERT INTO device_profiles(name,playPauseAvailable,nextAvailable,previousAvailable,lastSeenAtEpochMs) VALUES ('buds',1,1,1,10)")
cur.execute("INSERT INTO interactions(id,sessionId,exerciseId,timestampEpochMs,response,correct,confidence,latencyMs,hintDepth,plannerScore,stateBeforeMastery,stateAfterMastery) VALUES (7,2,'E',11,'B',1,NULL,100,0,.5,.4,.5)")
con.commit()

for sql in sqls:
    cur.execute(sql)
con.commit()

cols = lambda table: {row[1]: row for row in cur.execute(f'PRAGMA table_info({table})')}
assert 'exposureCount' in cols('learner_kc_state')
assert 'lastExposedAtEpochMs' in cols('learner_kc_state')
assert 'transferSuccesses' in cols('learner_kc_state')
assert 'novelSuccesses' in cols('learner_kc_state')
assert cur.execute("SELECT exposureCount,lastExposedAtEpochMs,transferSuccesses,novelSuccesses FROM learner_kc_state WHERE kcId='G'").fetchone() == (0, None, 0, 0)
assert 'misconceptionIdsCsv' in cols('exercises')
assert cur.execute("SELECT misconceptionIdsCsv FROM exercises WHERE id='E'").fetchone()[0] == ''
for name in ('primaryKeyCode','secondaryKeyCode','backKeyCode','stopKeyCode'):
    assert name in cols('device_profiles')
row = cur.execute("SELECT id,sessionId,turnId,exerciseId,graded,kind FROM interactions WHERE id=7").fetchone()
assert row == (7,2,7,'E',1,'GRADED_RESPONSE'), row
assert cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='runtime_events'").fetchone()
idx = {r[1]: r for r in cur.execute("PRAGMA index_list(interactions)")}
assert 'index_interactions_sessionId_turnId' in idx and idx['index_interactions_sessionId_turnId'][2] == 1
try:
    cur.execute("INSERT INTO interactions(sessionId,turnId,exerciseId,timestampEpochMs,response,correct,graded,kind,hintDepth) VALUES (2,7,'E',12,'A',0,1,'GRADED_RESPONSE',0)")
    con.commit()
    raise AssertionError('unique (sessionId, turnId) constraint did not reject duplicate')
except sqlite3.IntegrityError:
    pass

print(f'SIAA migration smoke OK statements={len(sqls)}')
