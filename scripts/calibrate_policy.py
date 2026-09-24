#!/usr/bin/env python3
"""Diagnóstico descriptivo de calibración a partir de un backup SIAA. No prueba causalidad."""
import argparse,json,math,re,statistics
from pathlib import Path

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('backup'); ap.add_argument('--out',default='policy_calibration_report.json'); a=ap.parse_args()
    data=json.loads(Path(a.backup).read_text())
    ints={(x['sessionId'],x['turnId']):x for x in data.get('interactions',[]) if x.get('graded')}
    pairs=[]
    for e in data.get('runtimeEvents',[]):
        if e.get('eventType')!='PREDICTION_RECORDED': continue
        m=re.search(r'successProbability=([0-9.]+)',e.get('payload') or '')
        i=ints.get((e.get('sessionId'),e.get('turnId')))
        if m and i: pairs.append((min(1,max(0,float(m.group(1)))),1.0 if i.get('correct') else 0.0))
    bins=[]
    for lo in [i/10 for i in range(10)]:
        xs=[x for x in pairs if lo<=x[0]<(lo+.1) or (lo==.9 and x[0]==1)]
        if xs: bins.append({'range':[round(lo,1),round(lo+.1,1)],'n':len(xs),'meanPredicted':sum(x for x,_ in xs)/len(xs),'observed':sum(y for _,y in xs)/len(xs)})
    if pairs:
        brier=sum((p-y)**2 for p,y in pairs)/len(pairs)
        logloss=-sum(y*math.log(max(p,1e-6))+(1-y)*math.log(max(1-p,1e-6)) for p,y in pairs)/len(pairs)
    else: brier=logloss=None
    sessions=data.get('sessions',[]); evidence=data.get('skillEvidence',[])
    report={'status':'descriptive-only','nPredictions':len(pairs),'brier':brier,'logLoss':logloss,'calibrationBins':bins,'nSessions':len(sessions),'nObjectiveSkillEvidence':len(evidence),'recommendation':'Collect >=1000 objective predictions and delayed retention probes before changing policy priors.' if len(pairs)<1000 else 'Enough volume for a held-out calibration fit; use train/validation split and compare Brier/log loss before deploying.'}
    Path(a.out).write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n'); print(json.dumps(report,indent=2,ensure_ascii=False))
if __name__=='__main__': main()
