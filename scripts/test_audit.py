"""Offline dataset regression tests. These do not compile or run Android code."""
import collections
import csv
import hashlib
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'core/data/src/main/assets'
AUDIT = ROOT / 'docs/audit-2026-11-03'


def read(path):
    return json.loads(path.read_text())


class AuditRegressionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = read(ROOT / 'docs/content-intake-2026-11-04/before-catalog.json')
        cls.items = cls.catalog['items']
        cls.original = read(AUDIT / 'original-212.json')['items']
        cls.rows = read(AUDIT / 'entries.json')
        cls.groups = read(ROOT / 'docs/content-intake-2026-11-04/before-presentation_mapping.json')['items']

    def test_every_original_id_and_exact_code_survives(self):
        self.assertEqual(212, len(self.original))
        self.assertEqual([(x['id'], x['code']) for x in self.original],
                         [(x['id'], x['code']) for x in self.items])
        self.assertTrue(all(isinstance(x['code'], str) for x in self.items))

    def test_every_row_audited_with_evidence_and_image_review(self):
        self.assertEqual(list(range(1, 213)), [r['original_index'] for r in self.rows])
        sources = read(AUDIT / 'sources.json')
        self.assertEqual([x['id'] for x in self.items], [r['id'] for r in self.rows])
        for row in self.rows:
            for key in ('evidence', 'verification', 'category_review', 'visual_observation', 'image_request'):
                self.assertTrue(row[key], (row['id'], key))
            self.assertTrue(all(key in sources for key in row['source_ids'].split(';')))
            self.assertEqual('NOT RUN', row['on_device_test'])

    def test_wrong_game_quarantine_preserves_saved_id_targets(self):
        retired = [x for x in self.items if x['status'] == 'wrong_game']
        self.assertEqual({'875', '309', '247', '736', '914'}, {x['code'] for x in retired})
        self.assertEqual(207, sum(x['status'] != 'wrong_game' for x in self.items))
        original_ids = {x['id'] for x in self.original}
        self.assertTrue(all(x['id'] in original_ids for x in retired))

    def test_image_quarantine_is_exhaustive_and_safe(self):
        by_id = {x['id']: x for x in self.items}
        quarantined = [r for r in self.rows if r['image_review'] == 'uncertain_placeholder']
        self.assertEqual(13, len(quarantined))
        self.assertTrue(all(by_id[r['id']]['image'] is None for r in quarantined))
        for x in self.items:
            if x.get('image'):
                path = (ASSETS / x['image']).resolve()
                self.assertTrue(path.is_relative_to(ASSETS.resolve()))
                self.assertTrue(path.is_file(), x['id'])

    def test_duplicate_values_are_not_deduplicated(self):
        self.assertEqual(collections.Counter(x['code'] for x in self.original),
                         collections.Counter(x['code'] for x in self.items))
        self.assertEqual(len(self.items), len({x['id'] for x in self.items}))

    def test_ambiguous_official_entries_remain_explicit(self):
        rows = [x for x in self.items if x['verification'] == 'official_ambiguous']
        self.assertEqual(4, len(rows))
        self.assertEqual({'6666', '12345'}, {x['code'] for x in rows})
        self.assertTrue(all(x['status'] == 'official' for x in rows))

    def test_categories_and_input_methods_are_consistent(self):
        for x in self.items:
            self.assertIn(self.groups[x['id']]['chip'], {'Bike', 'Car', 'Animal', 'Other'})
            if x['status'] == 'rgs_tool':
                self.assertEqual('rgs_tool', x['inputMethod'])
            if x['code'].isupper() and x['code'].isalpha():
                self.assertEqual('unknown', x['inputMethod'])
        tarzen = next(x for x in self.items if x['code'] == '300')
        self.assertEqual({'chip': 'Car', 'vehiclesShortcut': True}, self.groups[tarzen['id']])

    def test_image_report_covers_every_missing_or_uncertain_entry(self):
        with (AUDIT / 'image-requests.csv').open(encoding='utf-8-sig', newline='') as stream:
            report = list(csv.DictReader(stream))
        wanted = {r['id'] for r in self.rows if r['image_review'] != 'visually_consistent'}
        self.assertEqual(wanted, {r['id'] for r in report})
        self.assertEqual(130, len(report))

    def test_no_remote_images_or_accidental_content_download(self):
        self.assertTrue(all(not x.get('image') or x['image'].startswith('images/') for x in self.items))
        self.assertTrue(all(x['reviewedOn'] == '2026-11-03' for x in self.items))
        self.assertEqual(len(self.items), self.catalog['count'])
        self.assertEqual(0, self.catalog['audit']['newConfirmedEntries'])

    def test_duplicate_asset_removed_without_dropping_mapped_original(self):
        self.assertFalse((ASSETS / 'images/9129_infinity_health__alt.png').exists())
        with (AUDIT / 'asset-inventory.csv').open(encoding='utf-8-sig', newline='') as stream:
            inventory = list(csv.DictReader(stream))
        for asset in inventory:
            file = ASSETS / asset['file']
            if asset['file'].endswith('9129_infinity_health__alt.png'):
                continue
            self.assertEqual(asset['sha256'], hashlib.sha256(file.read_bytes()).hexdigest())
        self.assertEqual(122, len(inventory))


if __name__ == '__main__':
    unittest.main(verbosity=2)
