"""Offline regression tests. No Android build or gameplay verification."""
import json, hashlib, struct, unittest, xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
R=Path(__file__).resolve().parents[1]; A=R/'core/data/src/main/assets'; D=R/'docs/content-intake-2026-11-04'
def read(p):return json.loads(p.read_text())
class Integration(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.c=read(A/'catalog.json'); cls.rows=cls.c['items']; cls.ids={r['id']:r for r in cls.rows}; cls.g=read(A/'presentation_mapping.json')['items']
 def active(self,code):return [r for r in self.rows if r['code']==code and r['status'] not in ['wrong_game','superseded']]
 def test_original_identifiers_and_strings(self):
  for old in read(D/'before-catalog.json')['items']:self.assertEqual(old['code'],self.ids[old['id']]['code'])
  self.assertTrue(all(isinstance(r['code'],str) for r in self.rows))
 def test_unique_ids_complete_mapping(self):
  self.assertEqual(len(self.rows),len(self.ids)); self.assertEqual(self.c['count'],len(self.rows)); self.assertEqual(set(self.ids),set(self.g))
 def test_cobra_is_single_bike_with_image(self):
  rows=self.active('4444'); self.assertEqual(1,len(rows)); self.assertEqual('Cobra Bike',rows[0]['name']); self.assertEqual('Bike',self.g[rows[0]['id']]['chip']); self.assertTrue(rows[0]['image'])
 def test_corrected_vehicle_and_animal_mappings(self):
  for code,name in [('500','Bugatti'),('800','Bugatti v2'),('6','Buffalo'),('6999','Elephant'),('6666','Range Rover'),('9000','Flying Bike')]:self.assertEqual([name],[r['name'] for r in self.active(code)])
 def test_alias_targets_active_same_code(self):
  for r in self.rows:
   if r.get('replacementId'):
    t=self.ids[r['replacementId']]; self.assertEqual(r['code'],t['code']); self.assertNotEqual('superseded',t['status'])
 def test_leading_zeros(self):
  for code in ['0000','0015','0701','09090','01000','01212','00','0']:self.assertTrue(self.active(code),code)
 def test_weather_cycle_keeps_meaningful_duplicates(self):
  rows=self.active('99'); self.assertEqual(6,len(rows)); self.assertEqual(6,len({r['name'] for r in rows}))
 def test_no_disproved_vehicle_base_name(self):
  self.assertEqual('conflict',self.active('04444')[0]['status']); self.assertNotIn('Bugatti Chiron',self.active('04444')[0]['name']); self.assertIn('Bugatti',self.active('0500')[0]['name'])
 def test_sections(self):
  self.assertEqual(86,sum(r['section']=='plugins' for r in self.rows)); self.assertEqual(23,sum(r['section']=='loads' for r in self.rows)); self.assertTrue(any(r['section']=='rgs' and r['code']=='fan on' for r in self.rows))
 def test_all_images_decode_offline(self):
  for r in self.rows:
   if r.get('image'):
    self.assertTrue(r['image'].startswith('images/'))
    with Image.open(A/r['image']) as im:im.verify()
 def test_model_bytes_headers_hashes(self):
  ledger={x['file']:x for x in read(D/'asset-rights-register.json')}
  for r in self.rows:
   if not r.get('modelAsset'):continue
   p=A/r['modelAsset']; parts=p.with_name(p.name+'.parts'); files=[A/s for s in parts.read_text().splitlines()] if parts.exists() else [p]
   sha=hashlib.sha256(); size=0
   for f in files:
    with f.open('rb') as stream:
     while b:=stream.read(1048576):sha.update(b); size+=len(b)
   with files[0].open('rb') as stream:head=stream.read(256)
   self.assertTrue(head.startswith(b'UnityFS\0')); pos=12
   for _ in range(2):pos=head.index(b'\0',pos)+1
   self.assertEqual(size,struct.unpack('>Q',head[pos:pos+8])[0]); self.assertEqual(ledger[r['modelAsset']]['sha256'],sha.hexdigest())
 def test_gym_excluded(self):
  self.assertFalse(any(r['section']=='loads' and 'gym' in r['name'].lower() for r in self.rows)); self.assertFalse(any('gym' in p.name.lower() for p in (A/'models').iterdir()))
 def test_xml_parses(self):
  for p in (R/'app/src/main').rglob('*.xml'):ET.parse(p)
 def test_sources_do_not_claim_license(self):
  self.assertTrue(all(r['license']=='permission_not_supplied' for r in read(D/'asset-rights-register.json')))
 def test_rights_register_covers_referenced_assets(self):
  files={r['file'] for r in read(D/'asset-rights-register.json')}
  for r in self.rows:
   for key in ['image','modelAsset']:
    if r.get(key):self.assertIn(r[key],files)
 def test_model_segments_complete_and_fit_git_limit(self):
  expected=set()
  for r in self.rows:
   if not r.get('modelAsset'):continue
   p=A/r['modelAsset']; parts=p.with_name(p.name+'.parts')
   if parts.exists():expected.add(parts); expected.update(A/s for s in parts.read_text().splitlines())
   else:expected.add(p)
  actual={p for p in (A/'models').iterdir() if not p.name.startswith('.')}
  self.assertEqual(expected,actual); self.assertTrue(all(p.stat().st_size<=32*1024*1024 for p in actual))
if __name__=='__main__':unittest.main(verbosity=2)
