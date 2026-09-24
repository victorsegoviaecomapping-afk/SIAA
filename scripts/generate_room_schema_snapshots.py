#!/usr/bin/env python3
"""Generate checked-in Room schema snapshots for SiaaDatabase v1-v4.

These snapshots mirror the historical entity layouts encoded by MIGRATION_1_2 and
MIGRATION_2_3. Android CI's MigrationTestHelper consumes them from core/data/schemas.
"""
from __future__ import annotations
from pathlib import Path
import hashlib, json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "core/data/schemas/com.siaa.core.data.SiaaDatabase"
OUT.mkdir(parents=True, exist_ok=True)

# field tuple: fieldPath, columnName, affinity, notNull, defaultValue

def f(name, affinity, nn=True, default="undefined"):
    return {"fieldPath": name, "columnName": name, "affinity": affinity, "notNull": nn, "defaultValue": default}

def entity(table, fields, pk=(), auto=False, indices=()):
    defs=[]
    inline_auto_pk = auto and len(pk) == 1
    for x in fields:
        part=f"`{x['columnName']}` {x['affinity']}"
        if inline_auto_pk and x['columnName'] == pk[0]:
            part += " PRIMARY KEY AUTOINCREMENT"
        if x['notNull']: part += " NOT NULL"
        if x['defaultValue'] != "undefined": part += f" DEFAULT {x['defaultValue']}"
        defs.append(part)
    if pk and not inline_auto_pk:
        defs.append("PRIMARY KEY(" + ", ".join(f"`{x}`" for x in pk) + ")")
    create=f"CREATE TABLE IF NOT EXISTS `${{TABLE_NAME}}` ({', '.join(defs)})"
    return {
        "tableName": table,
        "createSql": create,
        "fields": fields,
        "primaryKey": {"autoGenerate": auto, "columnNames": list(pk)},
        "indices": list(indices),
        "foreignKeys": []
    }

def idx(name, unique, cols):
    uq="UNIQUE " if unique else ""
    return {
        "name": name, "unique": unique, "columnNames": list(cols),
        "orders": ["ASC"]*len(cols),
        "createSql": f"CREATE {uq}INDEX IF NOT EXISTS `{name}` ON `${{TABLE_NAME}}` (" + ", ".join(f"`{c}` ASC" for c in cols) + ")"
    }

base = [
    entity("knowledge_components", [f("id","TEXT"),f("name","TEXT"),f("cefr","TEXT"),f("domain","TEXT"),f("form","TEXT"),f("meaning","TEXT"),f("useText","TEXT"),f("importance","REAL"),f("priorMastery","REAL"),f("tagsCsv","TEXT")], ["id"]),
    entity("knowledge_edges", [f("fromId","TEXT"),f("toId","TEXT"),f("weight","REAL"),f("hardPrerequisite","INTEGER")], ["fromId","toId"]),
    entity("sessions", [f("id","INTEGER"),f("mode","TEXT"),f("startedAtEpochMs","INTEGER"),f("endedAtEpochMs","INTEGER",False)], ["id"], True),
    entity("misconceptions", [f("id","TEXT"),f("kcId","TEXT"),f("label","TEXT"),f("probability","REAL"),f("lastObservedAtEpochMs","INTEGER")], ["id"]),
    entity("app_meta", [f("key","TEXT"),f("value","TEXT")], ["key"]),
]

learner_v1=[f("kcId","TEXT"),f("mastery","REAL"),f("recognition","REAL"),f("production","REAL"),f("orthography","REAL"),f("automaticity","REAL"),f("halfLifeHours","REAL"),f("uncertainty","REAL"),f("lastReviewedAtEpochMs","INTEGER",False),f("consecutiveSuccess","INTEGER"),f("consecutiveFailure","INTEGER"),f("totalAttempts","INTEGER"),f("totalCorrect","INTEGER")]
learner_v2=learner_v1+[f("exposureCount","INTEGER",True,"0"),f("lastExposedAtEpochMs","INTEGER",False)]
learner_v3=learner_v2+[f("transferSuccesses","INTEGER",True,"0"),f("novelSuccesses","INTEGER",True,"0")]

exercise_base=[f("id","TEXT"),f("type","TEXT"),f("kcIdsCsv","TEXT"),f("cefr","TEXT"),f("difficulty","REAL"),f("promptEs","TEXT"),f("stimulusEn","TEXT"),f("optionA","TEXT"),f("optionB","TEXT"),f("correctOption","TEXT"),f("explanationEs","TEXT"),f("spellTarget","TEXT"),f("estimatedSeconds","INTEGER"),f("tagsCsv","TEXT")]
exercise_v2=exercise_base+[f("misconceptionIdsCsv","TEXT",True,"''")]

device_v1=[f("id","INTEGER"),f("name","TEXT"),f("playPauseAvailable","INTEGER"),f("nextAvailable","INTEGER"),f("previousAvailable","INTEGER"),f("lastSeenAtEpochMs","INTEGER")]
device_v2=[f("id","INTEGER"),f("name","TEXT"),f("primaryKeyCode","INTEGER",False),f("secondaryKeyCode","INTEGER",False),f("backKeyCode","INTEGER",False),f("stopKeyCode","INTEGER",False),f("playPauseAvailable","INTEGER"),f("nextAvailable","INTEGER"),f("previousAvailable","INTEGER"),f("lastSeenAtEpochMs","INTEGER")]

interaction_v1=[f("id","INTEGER"),f("sessionId","INTEGER"),f("exerciseId","TEXT"),f("timestampEpochMs","INTEGER"),f("response","TEXT"),f("correct","INTEGER"),f("confidence","TEXT",False),f("latencyMs","INTEGER",False),f("hintDepth","INTEGER"),f("plannerScore","REAL",False),f("stateBeforeMastery","REAL",False),f("stateAfterMastery","REAL",False)]
interaction_v2=[f("id","INTEGER"),f("sessionId","INTEGER"),f("turnId","INTEGER",True,"0"),f("exerciseId","TEXT"),f("timestampEpochMs","INTEGER"),f("response","TEXT"),f("correct","INTEGER"),f("graded","INTEGER",True,"1"),f("kind","TEXT",True,"'GRADED_RESPONSE'"),f("confidence","TEXT",False),f("latencyMs","INTEGER",False),f("hintDepth","INTEGER"),f("plannerScore","REAL",False),f("stateBeforeMastery","REAL",False),f("stateAfterMastery","REAL",False)]
interaction_index=idx("index_interactions_sessionId_turnId",True,["sessionId","turnId"])
runtime_events=entity("runtime_events",[f("id","INTEGER"),f("sessionId","INTEGER"),f("turnId","INTEGER"),f("timestampEpochMs","INTEGER"),f("eventType","TEXT"),f("stateBefore","TEXT"),f("stateAfter","TEXT"),f("exerciseId","TEXT",False),f("runtimeCommand","TEXT",False),f("mediaKeyCode","INTEGER",False),f("payload","TEXT",False)],["id"],True)

skill_evidence=entity("skill_evidence",[f("id","INTEGER"),f("sessionId","INTEGER",False),f("activityId","TEXT"),f("kcIdsCsv","TEXT"),f("dimension","TEXT"),f("score","REAL"),f("timestampEpochMs","INTEGER"),f("latencyMs","INTEGER",False),f("rawResponse","TEXT"),f("source","TEXT")],["id"],True)

versions={
1: base+[entity("learner_kc_state",learner_v1,["kcId"]),entity("exercises",exercise_base,["id"]),entity("device_profiles",device_v1,["id"],True),entity("interactions",interaction_v1,["id"],True)],
2: base+[entity("learner_kc_state",learner_v2,["kcId"]),entity("exercises",exercise_v2,["id"]),entity("device_profiles",device_v2,["id"],True),entity("interactions",interaction_v2,["id"],True,[interaction_index]),runtime_events],
3: base+[entity("learner_kc_state",learner_v3,["kcId"]),entity("exercises",exercise_v2,["id"]),entity("device_profiles",device_v2,["id"],True),entity("interactions",interaction_v2,["id"],True,[interaction_index]),runtime_events],
4: base+[entity("learner_kc_state",learner_v3,["kcId"]),entity("exercises",exercise_v2,["id"]),entity("device_profiles",device_v2,["id"],True),entity("interactions",interaction_v2,["id"],True,[interaction_index]),runtime_events,skill_evidence],
}

for version, entities in versions.items():
    canonical=json.dumps(entities,sort_keys=True,separators=(",",":"))
    identity=hashlib.sha256((str(version)+canonical).encode()).hexdigest()
    payload={
        "formatVersion":1,
        "database":{
            "version":version,
            "identityHash":identity,
            "entities":entities,
            "views":[],
            "setupQueries":[
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                f"INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '{identity}')"
            ]
        }
    }
    (OUT/f"{version}.json").write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(OUT/f"{version}.json")
