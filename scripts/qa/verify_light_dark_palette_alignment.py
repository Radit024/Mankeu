"""Check proposed light/dark pair coherence against current native light tokens."""
from pathlib import Path
import json
from verify_palette_extension import contrast

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/design/2026-10-25-light-dark-palette-alignment'


def main():
    p = json.loads((BASE / 'palette.json').read_text(encoding='utf-8'))
    assert p['status'] == 'proposed' and len(p['families']) == 6
    metric = (ROOT / 'app/src/main/java/com/mankeu/presentation/MetricColors.kt').read_text(encoding='utf-8')
    theme = (ROOT / 'app/src/main/java/com/mankeu/presentation/Theme.kt').read_text(encoding='utf-8')
    count = 0
    for family in p['families']:
        for mode in ('light', 'dark'):
            colors = family[mode]
            for foreground in (colors['accent'], p[mode]['text']):
                ratio = contrast(foreground, colors['background'])
                assert ratio >= 4.5, (family['id'], mode, foreground, ratio)
                count += 1
        source = theme if family['id'] in ('terracotta', 'teal') else metric
        keys = ('accent',) if family['id'] == 'teal' else ('accent', 'background')
        for key in keys:
            assert ('0xFF' + family['light'][key][1:]) in source, (family['id'], key)
        print(f"{family['name']}: light {contrast(family['light']['accent'], family['light']['background']):.2f}:1; "
              f"dark {contrast(family['dark']['accent'], family['dark']['background']):.2f}:1")
    for background in ('background', 'surface', 'elevated'):
        for foreground in ('text', 'muted'):
            assert contrast(p['dark'][foreground], p['dark'][background]) >= 4.5
            count += 1
    assert contrast(p['dark']['onButton'], p['dark']['button']) >= 4.5
    count += 1
    assert p['dark']['brand'] == '#52B788'
    assert (BASE / 'light-dark-preview.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    print(f'PASS: 6 color families, {count} contrast pairs, native light anchors, Arina brand green, preview PNG.')


if __name__ == '__main__':
    main()
