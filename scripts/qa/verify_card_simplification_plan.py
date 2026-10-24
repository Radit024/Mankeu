"""Check task structure, local evidence and source hashes for the card plan."""
from pathlib import Path
from urllib.parse import unquote, urlsplit
import hashlib
import json
import re
import struct

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'docs/superpowers/plans/2026-10-24-card-information-simplification.md'
ASSETS = ROOT / 'docs/design/2026-10-24-card-simplification'


def main():
    content = PLAN.read_text(encoding='utf-8')
    tasks = re.findall(r'^### Task (\d+):', content, re.M)
    assert tasks == [str(n) for n in range(1, 9)], tasks
    assert len(re.findall(r'^```', content, re.M)) % 2 == 0, 'Unbalanced fences'
    assert not re.search(r'\b(?:T' + 'ODO|T' + 'BD)\b|fill in details|implement later', content, re.I)
    images = re.findall(r'!\[[^\]]*\]\(([^)]+)\)', content)
    assert len(images) == 4, images
    links = re.findall(r'\]\(([^)]+)\)', content)
    local_count = 0
    for target in links:
        parsed = urlsplit(target)
        if parsed.scheme or parsed.netloc:
            continue
        path = (PLAN.parent / unquote(parsed.path)).resolve()
        assert path.is_relative_to(ROOT.resolve()) and path.is_file(), target
        local_count += 1
    for target in images:
        data = (PLAN.parent / target).read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n', target
        width, height = struct.unpack('>II', data[16:24])
        assert width > 0 and height > 0, target
    manifest = json.loads((ASSETS / 'sources.json').read_text(encoding='utf-8'))
    assert len(manifest['references']) == 3
    for ref in manifest['references']:
        data = (ASSETS / ref['file']).read_bytes()
        assert hashlib.sha256(data).hexdigest() == ref['sha256'], ref['file']
        assert ref['url'].startswith('https://'), ref['url']
        assert ref['url'] in content, ref['url']
    for required in ('FinancialSummary', 'ActualMonthAnalysis', 'StockCalculation',
                     '48.dp', '200%', 'Reduced/Off', 'rollback', 'preview.canCommit'):
        assert required.casefold() in content.casefold(), required
    print(f'PASS: 8 tasks, 4 PNG screenshots, {local_count} local links, '
          '3 source hashes, required safeguards, balanced fences, no placeholders.')


if __name__ == '__main__':
    main()
