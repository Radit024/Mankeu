"""Verify the proposed Arina green dark palette and browser references."""
from pathlib import Path
import hashlib
import json
from verify_palette_extension import contrast

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/design/2026-10-25-arina-green-dark'


def main():
    p = json.loads((BASE / 'palette.json').read_text(encoding='utf-8'))
    assert p['status'] == 'proposed' and p['brand'] == '#52B788'
    checks = []
    for background in ('background', 'surface', 'input', 'elevated'):
        checks.extend((p[fg], p[background], 4.5) for fg in ('text', 'muted'))
    checks += [(p['onPrimary'], p['primary'], 4.5),
               (p['accent'], p['surface'], 4.5),
               (p['secondary'], p['surface'], 4.5),
               (p['onPrimaryContainer'], p['primaryContainer'], 4.5),
               (p['borderStrong'], p['input'], 3),
               (p['accent'], p['background'], 3),
               (p['primary'], p['surface'], 3)]
    checks.extend((p[role], p[role + 'Container'], 4.5) for role in ('success', 'warning', 'danger', 'info'))
    checks.extend((color, p['surface'], 3) for color in p['chart'])
    for foreground, background, minimum in checks:
        actual = contrast(foreground, background)
        assert actual >= minimum, (foreground, background, actual, minimum)
    manifest = json.loads((BASE / 'sources.json').read_text(encoding='utf-8'))
    assert len(manifest['references']) == 2
    for ref in manifest['references']:
        data = (BASE / ref['file']).read_bytes()
        assert data.startswith(b'\x89PNG\r\n\x1a\n')
        assert hashlib.sha256(data).hexdigest() == ref['sha256']
    assert (BASE / 'forest-charcoal-preview.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    print(f"PASS: {len(checks)} contrast pairs, retained Arina brand green, 2 reference hashes, PNG preview.")
    print(f"Button text: {contrast(p['onPrimary'], p['primary']):.2f}:1; accent/card: {contrast(p['accent'], p['surface']):.2f}:1")


if __name__ == '__main__':
    main()
