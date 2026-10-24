"""Validate consolidated plan scope, reference evidence and proposed dark roles."""
from pathlib import Path
from urllib.parse import urlsplit, unquote
import hashlib
import json
import re
from verify_palette_extension import contrast

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'docs/superpowers/plans/2026-10-25-consolidated-ui-colors-settings.md'


def main():
    content = PLAN.read_text(encoding='utf-8')
    tasks = re.findall(r'^### Task (\d+):', content, re.M)
    assert tasks == [str(n) for n in range(1, 13)], tasks
    assert len(re.findall(r'^```', content, re.M)) % 2 == 0
    assert not re.search(r'\b(?:T' + 'ODO|T' + 'BD)\b|fill in details|implement later', content, re.I)
    images = re.findall(r'!\[[^\]]*\]\(([^)]+)\)', content)
    assert len(images) == 8, images
    local_links = 0
    for url in re.findall(r'\]\(([^)]+)\)', content):
        parsed = urlsplit(url)
        if parsed.scheme or parsed.netloc:
            continue
        path = (PLAN.parent / unquote(parsed.path)).resolve()
        assert path.is_relative_to(ROOT.resolve()) and path.is_file(), url
        local_links += 1
    for url in images:
        assert (PLAN.parent / url).read_bytes().startswith(b'\x89PNG\r\n\x1a\n'), url
    reference_count = 0
    for folder in ('2026-10-25-arina-green-dark', '2026-10-25-settings-ui-references'):
        base = ROOT / 'docs/design' / folder
        manifest = json.loads((base / 'sources.json').read_text(encoding='utf-8'))
        for ref in manifest['references']:
            assert hashlib.sha256((base / ref['file']).read_bytes()).hexdigest() == ref['sha256']
            reference_count += 1
    assert reference_count == 5
    match = re.search(r'internal val MankeuDarkColors = darkColorScheme\((.*?)\n\)', content, re.S)
    assert match, 'Missing full dark mapping'
    roles = dict(re.findall(r'(\w+)\s*=\s*Color\(0xFF([A-F0-9]{6})\)', match[1]))
    assert len(roles) == 48, len(roles)
    pairs = [
        ('onPrimary', 'primary'), ('onPrimaryContainer', 'primaryContainer'),
        ('onSecondary', 'secondary'), ('onSecondaryContainer', 'secondaryContainer'),
        ('onTertiary', 'tertiary'), ('onTertiaryContainer', 'tertiaryContainer'),
        ('onBackground', 'background'), ('onError', 'error'),
        ('onErrorContainer', 'errorContainer'), ('inverseOnSurface', 'inverseSurface'),
        ('onPrimaryFixed', 'primaryFixedDim'), ('onSecondaryFixed', 'secondaryFixedDim'),
        ('onTertiaryFixed', 'tertiaryFixedDim'),
    ]
    for surface in ('surface', 'surfaceContainerLowest', 'surfaceContainerLow',
                    'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest'):
        pairs += [(foreground, surface) for foreground in ('onSurface', 'onSurfaceVariant')]
    for foreground, background in pairs:
        ratio = contrast('#' + roles[foreground], '#' + roles[background])
        assert ratio >= 4.5, (foreground, background, ratio)
    assert contrast('#' + roles['outline'], '#' + roles['surfaceContainerLow']) >= 3
    assert contrast('#FFFFFF', '#357956') >= 4.5
    for marker in ('ProfileDraft', 'CONFIRM_DISCARD', 'settingsPageForTour', 'Activity.recreate()',
                   'actionContainer', 'onActionContainer', 'Full/Reduced/Off', '48dp', '200%',
                   'Berkas dan Data', 'rollback', 'unknown', 'satu pemilik Back'):
        assert marker.casefold() in content.casefold(), marker
    print(f'PASS: 12 tasks, 8 PNG images, {local_links} local links, 5 source hashes, '
          f'48 dark roles, {len(pairs) + 2} planned contrast pairs, scope safeguards, balanced fences.')


if __name__ == '__main__':
    main()
