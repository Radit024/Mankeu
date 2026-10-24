"""Verify proposed solid-color text pairs and saved palette research evidence."""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'docs/design/2026-10-25-palette-extension'


def luminance(value):
    channels = [int(value[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return sum(a * b for a, b in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(a, b):
    high, low = sorted((luminance(a), luminance(b)), reverse=True)
    return (high + 0.05) / (low + 0.05)


def main():
    palette = json.loads((BASE / 'palette.json').read_text(encoding='utf-8'))
    assert palette['status'] == 'proposed'
    assert len(palette['colors']) == 4
    pairs = 0
    for color in palette['colors']:
        for theme, body in (('light', '#2C2A29'), ('dark', '#DDE8DF')):
            values = color[theme]
            for value in values.values():
                assert re.fullmatch(r'#[0-9A-F]{6}', value), value
            for foreground in (values['accent'], body):
                ratio = contrast(foreground, values['background'])
                assert ratio >= 4.5, (color['name'], theme, foreground, ratio)
                pairs += 1
            print(f"{color['name']} {theme}: accent {contrast(values['accent'], values['background']):.2f}:1")
    manifest = json.loads((BASE / 'sources.json').read_text(encoding='utf-8'))
    assert len(manifest['references']) == 2
    for ref in manifest['references']:
        data = (BASE / ref['file']).read_bytes()
        assert data.startswith(b'\x89PNG\r\n\x1a\n')
        assert hashlib.sha256(data).hexdigest() == ref['sha256'], ref['file']
    assert (BASE / 'palette-preview.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    print(f'PASS: {pairs} solid text/background pairs >= 4.5:1, 2 source hashes, preview PNG.')


if __name__ == '__main__':
    main()
