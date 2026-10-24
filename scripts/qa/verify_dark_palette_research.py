"""Check replacement dark-theme candidates and browser research evidence."""
from pathlib import Path
import hashlib
import json
import re
from verify_palette_extension import contrast

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/design/2026-10-25-dark-palette-replacement'


def main():
    data = json.loads((BASE / 'palettes.json').read_text(encoding='utf-8'))
    assert data['status'] == 'proposed' and data['recommended'] == 'A'
    assert [p['id'] for p in data['options']] == ['A', 'B', 'C']
    count = 0
    for p in data['options']:
        for name, value in p.items():
            if isinstance(value, str) and value.startswith('#'):
                assert re.fullmatch(r'#[0-9A-F]{6}', value), (name, value)
        pairs = []
        for background in ('background', 'surface', 'input', 'elevated'):
            pairs.extend((foreground, background, 4.5) for foreground in ('text', 'muted'))
        pairs += [('onPrimary', 'primary', 4.5), ('onSecondary', 'secondary', 4.5)]
        pairs += [(name, name + 'Container', 4.5) for name in ('success', 'warning', 'danger', 'info')]
        pairs += [('borderStrong', 'input', 3.0), ('primary', 'background', 3.0)]
        for foreground, background, minimum in pairs:
            value = contrast(p[foreground], p[background])
            assert value >= minimum, (p['id'], foreground, background, value)
            count += 1
        assert len(p['chart']) == 6
        for foreground in p['chart']:
            assert re.fullmatch(r'#[0-9A-F]{6}', foreground)
            assert contrast(foreground, p['surface']) >= 3.0, (p['id'], foreground)
            count += 1
        print(f"{p['id']} {p['name']}: body/card {contrast(p['text'], p['surface']):.2f}:1; "
              f"muted/card {contrast(p['muted'], p['surface']):.2f}:1; "
              f"button {contrast(p['onPrimary'], p['primary']):.2f}:1")
    manifest = json.loads((BASE / 'sources.json').read_text(encoding='utf-8'))
    assert len(manifest['references']) == 2
    for ref in manifest['references']:
        image = (BASE / ref['file']).read_bytes()
        assert image.startswith(b'\x89PNG\r\n\x1a\n')
        assert hashlib.sha256(image).hexdigest() == ref['sha256']
    assert (BASE / 'dark-options.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    content = (BASE / 'README.md').read_text(encoding='utf-8')
    assert len(re.findall(r'^```', content, re.M)) % 2 == 0
    print(f'PASS: 3 proposed themes, {count} contrast pairs, 2 browser source hashes, preview PNG.')


if __name__ == '__main__':
    main()
