"""Generate complete reconciliation and image-request reports, not gameplay certification."""
import collections, hashlib, json
from pathlib import Path
R=Path(__file__).resolve().parents[1]; A=R/'core/data/src/main/assets'; D=R/'docs/content-intake-2026-11-04'
rows=json.loads((A/'catalog.json').read_text())['items']; old=json.loads((D/'before-catalog.json').read_text())['items']
ids={r['id']:r for r in rows}; active=[r for r in rows if r['status'] not in ['wrong_game','superseded']]
def cell(v):return str(v or '—').replace('|','\\|').replace('\n',' ')
def table(headers,values):return '\n'.join(['| '+' | '.join(headers)+' |','| '+' | '.join('---' for _ in headers)+' |',*('| '+' | '.join(cell(v) for v in row)+' |' for row in values)])+'\n'
changes=[dict(id=x['id'],code=x['code'],before=x,after=ids[x['id']],changedFields=[key for key in sorted(set(x)|set(ids[x['id']])) if x.get(key)!=ids[x['id']].get(key)]) for x in old]
(D/'all-original-entries.json').write_text(json.dumps(changes,ensure_ascii=False,indent=2)+'\n')
(D/'ALL_ORIGINAL_ENTRIES.md').write_text('# মূল ২১২টি entry-এর বর্তমান অবস্থা\n\nপ্রতিটি মূল ID/code individually মিলিয়ে ফল দেওয়া হয়েছে। এটি owner-supplied content reconciliation, সব code নতুন করে game-এ পরীক্ষা নয়। সম্পূর্ণ before/after/source/notes all-original-entries.json-এ আছে।\n\n'+table(['#','ID','Code','আগের নাম → বর্তমান নাম','Section / status','Image','বদলানো field'],[[i,r['id'],r['code'],r['before']['name']+' → '+r['after']['name'],r['after']['section']+' / '+r['after']['status'],r['after'].get('image') or 'Placeholder',', '.join(r['changedFields'])] for i,r in enumerate(changes,1)]))
new=[r for r in rows if r['id'] not in {x['id'] for x in old}]
(D/'NEW_ENTRIES.md').write_text('# নতুন records\n\nসব নতুন phone cheat নয়: plugin URL, model, RGS command ও weather stage-ও আছে।\n\n'+table(['ID','নাম','Code / URL','Section','Source'],[[r['id'],r['name'],r['code'],r['section'],'; '.join(r.get('sources',[]))] for r in new]))
missing=[r for r in active if not r.get('image')]
uncertain=[r for r in active if r.get('image') and r.get('imageStatus') in ['unconfirmed_match','illustrative']]
by_hash=collections.defaultdict(list)
for r in active:
 if r.get('image'):by_hash[hashlib.sha256((A/r['image']).read_bytes()).hexdigest()].append(r)
shared={k:v for k,v in by_hash.items() if len(v)>1}
(D/'image-issues.json').write_text(json.dumps(dict(missing=missing,uncertain=uncertain,sharedImageHashes=shared),ensure_ascii=False,indent=2)+'\n')
(D/'IMAGE_ISSUES.md').write_text('# ছবির বাকি কাজ\n\nSupplied mapping গ্রহণ করা হয়েছে। Decode/hash পরীক্ষা image identity বা copyright license নিশ্চিত করে না।\n\n## ছবি নেই: '+str(len(missing))+'টি\n\n'+table(['নাম','Code','Section/status','প্রয়োজন'],[[r['name'],r['code'],r['section']+' / '+r['status'],'প্রথমে code-এর ফল যাচাই, তারপর সেই ফলের পরিষ্কার game screenshot' if r['status'] in ['derived','conflict','unverified'] else r['name']+'-এর game screenshot'] for r in missing])+'\n## আগের uncertain/illustrative ছবি\n\n'+table(['নাম','Code','Image','Status'],[[r['name'],r['code'],r['image'],r['imageStatus']] for r in uncertain])+'\n## একই image bytes একাধিক item-এ\n\nShared image সবসময় ভুল নয়। Source association পর্যালোচনার জন্য সব group এখানে আছে।\n\n'+table(['SHA-256','নাম ও code','Paths'],[[h,'; '.join(r['name']+' ['+r['code']+']' for r in rs),'; '.join(sorted({r['image'] for r in rs}))] for h,rs in shared.items()]))
payload=0
for r in rows:
 if not r.get('modelAsset'):continue
 p=A/r['modelAsset']; parts=A/(r['modelAsset']+'.parts')
 payload+=sum((A/f).stat().st_size for f in parts.read_text().splitlines()) if parts.exists() else p.stat().st_size
summary=dict(originalRecords=len(old),allRecords=len(rows),newRecords=len(new),activeSections=dict(collections.Counter(r['section'] for r in active)),archivedStatuses=dict(collections.Counter(r['status'] for r in rows if r not in active)),missingImages=len(missing),uncertainImages=len(uncertain),sharedImageGroups=len(shared),modelPayloadBytes=payload,androidBuildRun=False,gameplayTestedByAgent=False)
(D/'summary.json').write_text(json.dumps(summary,indent=2)+'\n'); print(json.dumps(summary,indent=2))
