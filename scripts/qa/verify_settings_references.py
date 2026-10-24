"""Verify local settings research screenshots, attribution and document links."""
from pathlib import Path
from urllib.parse import urlsplit, unquote
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/design/2026-10-25-settings-ui-references'


def main():
    content = (BASE / 'README.md').read_text(encoding='utf-8')
    images = re.findall(r'!\[[^\]]*\]\(([^)]+)\)', content)
    assert len(images) == 5
    for url in re.findall(r'\]\(([^)]+)\)', content):
        parsed = urlsplit(url)
        if parsed.scheme or parsed.netloc:
            continue
        path = (BASE / unquote(parsed.path)).resolve()
        assert path.is_relative_to(ROOT.resolve()) and path.is_file(), url
    for url in images:
        assert (BASE / url).read_bytes().startswith(b'\x89PNG\r\n\x1a\n'), url
    manifest = json.loads((BASE / 'sources.json').read_text(encoding='utf-8'))
    assert len(manifest['references']) == 3
    for ref in manifest['references']:
        assert ref['url'] in content
        assert hashlib.sha256((BASE / ref['file']).read_bytes()).hexdigest() == ref['sha256']
    assert len(re.findall(r'^```', content, re.M)) % 2 == 0
    for invariant in ('opsional', 'proposed', '48dp', 'draft', 'SETTINGS_DISPLAY', 'Berkas dan data'):
        assert invariant.casefold() in content.casefold(), invariant
    print('PASS: 5 embedded PNG screenshots, local links, 3 attributed source hashes, balanced fences and scope safeguards.')


if __name__ == '__main__':
    main()
