"""Read-only content audit. Run with Python 3; does not invoke Gradle or Android tools."""
import collections
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
assets = root / 'core/data/src/main/assets'
content = (assets / 'catalog.json').read_bytes()
catalog = json.loads(content)
groups = json.loads((assets / 'presentation_mapping.json').read_text())['items']
items = catalog['items']
assert catalog['schemaVersion'] == 1
assert catalog['count'] == len(items)
assert len({item['id'] for item in items}) == len(items)
assert all(isinstance(item['code'], str) and item['code'] for item in items)
assert all(item['id'] in groups for item in items)
assert all(groups[item['id']]['chip'] in {'Bike', 'Car', 'Animal', 'Other'} for item in items)
assert all(isinstance(groups[item['id']]['vehiclesShortcut'], bool) for item in items)
for item in items:
    if item.get('image'):
        target = (assets / item['image']).resolve()
        assert target.is_relative_to(assets.resolve()), item['id']
        assert target.is_file(), str(target)
counts = collections.Counter(item['code'] for item in items)
result = {
    'checks_passed': True,
    'catalog_sha256': hashlib.sha256(content).hexdigest(),
    'entries': len(items),
    'unique_ids': len({item['id'] for item in items}),
    'code_values_all_strings': True,
    'leading_zero_codes': sorted({item['code'] for item in items if item['code'].startswith('0')}),
    'duplicate_code_values': {code: count for code, count in counts.items() if count > 1},
    'status_counts': dict(collections.Counter(item['status'] for item in items)),
    'mapped_entries': sum(bool(item.get('image')) for item in items),
    'placeholder_entries': sum(not item.get('image') for item in items),
    'local_images': len([p for p in (assets / 'images').iterdir() if p.is_file()]),
    'all_mapped_images_exist': True,
}
print(json.dumps(result, indent=2, ensure_ascii=False))
