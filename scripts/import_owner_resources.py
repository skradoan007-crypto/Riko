"""Reproduce the owner-supplied resource migration from its frozen baseline."""
import json, zipfile, re, hashlib, io, struct, difflib, sys
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]; UP=Path(sys.argv[1])
A=ROOT/'core/data/src/main/assets'; D=ROOT/'docs/content-intake-2026-11-04'
c=json.loads((D/'before-catalog.json').read_text()); items=c['items']
groups=json.loads((D/'before-presentation_mapping.json').read_text())
originals={x['id']:dict(x) for x in items}; ledger=[]; imported=[]
def saveimage(b):
 with Image.open(io.BytesIO(b)) as im: im.verify()
 sha=hashlib.sha256(b).hexdigest(); ext='.png' if b.startswith(b'\x89PNG') else '.jpg'
 p='images/imported/'+sha[:20]+ext
 (A/p).parent.mkdir(parents=True,exist_ok=True); (A/p).write_bytes(b); return p
for n in range(1,6):
 with zipfile.ZipFile(next(UP.glob(f'IBD3D_Images_Batch_{n:02d}*zip'))) as z:
  assert z.testzip() is None
  manifest=next(x for x in z.namelist() if x.endswith('/manifest.json')); prefix=manifest.rsplit('/',1)[0]
  for r in json.loads(z.read(manifest)):
   assert r['status']=='ok'; b=z.read(prefix+'/'+r['file']); assert hashlib.sha256(b).hexdigest()==r['sha256']
   r.update(image=saveimage(b),batch=n); imported.append(r)
   ledger.append(dict(file=r['image'],source=r['url'],sha256=r['sha256'],license='permission_not_supplied',title=r['name']))
def norm(s):return re.sub('[^a-z0-9]','',s.lower().replace('(conflict)','').replace('(alternative)',''))
def choose(rows,name):return max(rows,key=lambda x:difflib.SequenceMatcher(None,norm(x['name']),norm(name)).ratio()) if rows else None
def group(x,chip):groups['items'][x['id']]={'chip':chip,'vehiclesShortcut':chip in ['Bike','Car']}
def new(code,name,section,cat,idx):
 x=dict(id=f'import_{section}_{idx}',code=code,name=name,category=cat,status='community',inputMethod='phone_dialer',image=None,notes='',section=section)
 items.append(x); group(x,'Other'); return x
canonical={}; aliases={}
for r in json.loads((D/'user-supplied-code-list.json').read_text())['items']:
 candidates=[x for x in items if x['code']==r['code']]; x=choose(candidates,r['name']); canonical[r['code']]=x
 assert x is not None
 x.update(name=r['name'],verification='user_supplied_list',reviewedOn='2026-11-05',section='rgs' if r['code'][0].isalpha() else 'codes',status='rgs_tool' if r['code'][0].isalpha() else 'community',notes='Name/code reconciled with the newer list supplied by the app owner on 2026-11-04. Not independently gameplay-tested.',sources=['user-supplied-list:2026-11-04'])
 for old in candidates:
  if old is not x:old.update(status='superseded',replacementId=x['id']); aliases[old['id']]=x['id']
 cat=r['sourceCategory']; chip='Animal' if cat=='Animals' else 'Bike' if cat in ['Motorcycles','Bicycles'] else 'Car' if cat in ['Cars & SUVs','Heavy Vehicles'] else 'Other'
 if r['code']=='4444':chip='Bike'; x.update(name='Cobra Bike',verification='user_gameplay',notes='User gameplay screenshots show 4444 and “Spawn Cobra Bike.”')
 if r['code']=='1001':chip='Car'
 x['category']='RGS Tools' if x['section']=='rgs' else chip
 x['inputMethod']='rgs_tool' if x['section']=='rgs' else 'phone_dialer'; group(x,chip)
reject={('6','Elephant'),('800','Audi'),('9000','Super Splendor'),('7112','Ferrari'),('6666','G Wagon')}
for i,r in enumerate(imported):
 if r['batch']==5:
  x=new(r['plugin_url'],r['name'],'plugins',r['group'].replace('_',' ').title(),i)
  x.update(inputMethod='plugin_url',image=r['image'],imageStatus='owner_supplied',notes='Copy this URL into the game tool that accepts plugin links. Fetching a plugin requires internet.',status='plugin',sources=[r['plugin_url'],r['url']]); continue
 code=r['code']; name=r['name']; code='npdance' if code=='npcdance' else code
 if code=='99':name={'Cloud Mode With Rain':'Cloud With Rain Mode','Night With Rain':'Night With Rain Mode'}.get(name,name)
 if (code,name) in reject:continue
 if code=='10' and name=='Buffalo':code='6'
 section='rgs' if r['batch']==4 or not code[0].isdigit() else 'codes'
 candidates=[x for x in items if x['code']==code and x['status'] not in ['wrong_game','superseded']]
 if code=='99':candidates=[x for x in candidates if norm(x['name'])==norm(name)]
 x=canonical.get(code) if code!='99' else None
 if x is None:x=choose(candidates,name)
 if x is None:x=new(code,name,section,r.get('group','Other'),i)
 x.update(image=r['image'],imageStatus='owner_supplied',reviewedOn='2026-11-05',section=section)
 if code not in canonical:x.update(name=name,status='rgs_tool' if section=='rgs' else 'community',verification='owner_supplied',notes='Mapping supplied by the app owner from website HTML.',inputMethod='rgs_tool' if section=='rgs' else 'phone_dialer')
 if code=='99':x['notes']=r.get('source_text') or r.get('note') or 'Call once to begin the weather cycle.'
 if code=='99' and name=='Cloud Mode':x['notes']='Call once. Repeated calls to 99 progress through the source website’s weather/time cycle.'
 if section=='rgs':x['category']=r.get('group','Tools').replace('_',' ').title()
 x['sources']=list(dict.fromkeys(x.get('sources',[])+[r['url']]))
 if code not in canonical:group(x,{'bike':'Bike','car':'Car','animal':'Animal'}.get(r.get('group'),'Other'))
elephant=next(r for r in imported if r['name']=='Elephant')
canonical['6999'].update(image=elephant['image'],imageStatus='owner_supplied',sources=canonical['6999']['sources']+[elephant['url']])
for x in items:
 x.setdefault('section','rgs' if x['inputMethod']=='rgs_tool' else 'codes')
 if x['status']=='derived' and x['code'].startswith('0'):
  base=canonical.get(x['code'][1:])
  if base:
   if groups['items'][base['id']]['chip']=='Car':
    x['name']='Monster Truck based on '+base['name']; x['notes']='Experimental prefix-derived claim. Base name reconciled with the owner’s newer list; this exact variant has not been gameplay-tested.'
   else:x.update(name='Unconfirmed monster variant ('+x['code']+')',status='conflict',image=None,imageStatus='missing',notes='Legacy prefix-derived claim. The old Bugatti Chiron base was incorrect: 4444 spawns Cobra Bike. No evidence that this prefix spawns a monster truck.')
  x['reviewedOn']='2026-11-05'
with zipfile.ZipFile(UP/'IBD3D_RGS_24_Pack.zip') as z:
 assert z.testzip() is None
 for r in json.loads(z.read('IBD3D_RGS_24_Pack/manifest.json')):
  if r['index']==17:continue
  idx=r['index']; prefix='IBD3D_RGS_24_Pack/'; b=z.read(prefix+'models/'+r['model_file'])
  assert b.startswith(b'UnityFS\0'); pos=12
  for _ in range(2):pos=b.index(b'\0',pos)+1
  assert struct.unpack('>Q',b[pos:pos+8])[0]==len(b)
  dest='models/'+r['model_file']; (A/'models').mkdir(exist_ok=True)
  if len(b)>32*1024*1024:
   parts=[]
   for i,offset in enumerate(range(0,len(b),32*1024*1024),1):
    name=dest+f'.part{i:03d}'; (A/name).write_bytes(b[offset:offset+32*1024*1024]); parts.append(name)
   (A/(dest+'.parts')).write_text('\n'.join(parts))
  else:(A/dest).write_bytes(b)
  img=prefix+'images/'+r['image_file']
  if img not in z.namelist():img=next(n for n in z.namelist() if n.startswith(prefix+f'images/{idx:02d}_'))
  ib=z.read(img); x=new(r['model_source'],r['name'],'loads','RGS Load Files',idx)
  x.update(image=saveimage(ib),imageStatus='owner_supplied',modelAsset=dest,modelFormat='Unity AssetBundle',inputMethod='local_model',status='model',notes='Bundled offline. Original .glb filename contains a Unity AssetBundle, not a glTF mesh.',sources=[r['model_source'],r['image_source']])
  ledger.extend([dict(file=dest,source=r['model_source'],sha256=hashlib.sha256(b).hexdigest(),license='permission_not_supplied',title=r['name']),dict(file=x['image'],source=r['image_source'],sha256=hashlib.sha256(ib).hexdigest(),license='permission_not_supplied',title=r['name'])])
known={r['file'] for r in ledger}
for x in items:
 if x.get('image') and x['image'] not in known:
  p=x['image']; ledger.append(dict(file=p,source=x.get('sources',[]),sha256=hashlib.sha256((A/p).read_bytes()).hexdigest(),license='permission_not_supplied',title=x['name'])); known.add(p)
c.update(catalogVersion=3,count=len(items),lastCompiled='2026-11-05'); c.pop('audit',None)
for p,value in [(A/'catalog.json',c),(A/'presentation_mapping.json',groups),(D/'asset-rights-register.json',ledger),(D/'changes.json',[dict(id=x['id'],before=originals.get(x['id']),after=x) for x in items])]:p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
print('Imported',len(items),'records; retained',len(originals),'original IDs; archived',len(aliases),'aliases; models 23')
