"""Rebuild/check licensed, offline Arina derivatives from the five pinned source archives."""
from pathlib import Path
import copy
import hashlib
import json
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/design/lottie-sources'
RAW = ROOT / 'app/src/main/res/raw'
SOURCES = {
    'business_plan': ('Business Plan', 'Md. Imam Hossain', 'business-plan-zHvKR0xWyF', 'bed10a2a-117b-11ee-a433-3b881884990b/IxlwxIzFfY', 'a4f4e939f5a93c674e6c150ccf90eb45e17dfa4b2ebabdfeeef4eb995a0566ad'),
    'growth_chart': ('Growth Chart', 'Zakaria Harry Utomo', 'growth-chart-7g4UY8O9D1', '55092d42-1168-11ee-8db1-0b4fb66fa345/l3EGlF193j', '5a72fb0fd5880064629846b4dadbc9ce887abc878ef1c468500db96f0a8aa7c0'),
    'stock_box': ('Cardboard Box Open | Loading 9', 'Jeffrey Christopher', 'cardboard-box-open-loading-9-seOUA2yuYw', '74d12a4c-1151-11ee-b23f-1bc152d8366c/TBvfFGlgco', 'ee0fa4240ca25508077844a9f4bfbee1991b782db036f5783a2f6551a55298b0'),
    'file_import': ('Upload File Icon Animation', 'Kohinoor Film', 'upload-file-icon-animation-qBUMesDbQN', 'e2d11648-117d-11ee-b9e7-3ba140dc92ed/7sUiGvJ4ft', 'aa4db9d18db26f79bb2a182c22c8737ed2348bb9ae2c76ce27468d6279e27eea'),
    'cloud_backup': ('Cloud Data Backup Store', 'MD Abdur Rahim', 'cloud-data-backup-store-3f4KENC8bj', '3ff7cdae-117d-11ee-b1d4-a702bbd41c91/qtcuq7OtbA', '8785fb39f65bcb0aac943bbfd34a1977c1de6d886363a55e79fdf19ddb2a4acc'),
}
# Only exact static fill/stroke/gradient colours change. Paths, timing, skin tones and masks stay intact.
PALETTES = {
    'business_plan': {'#EB485C': ('#E07A5F','#F0B49B'), '#EF5668': ('#D4836A','#F0B49B'), '#D1334A': ('#713A28','#36251F'), '#4D73C6': ('#2D6A4F','#8ED8AE'), '#FFDE70': ('#95D5B2','#A9D8BC'), '#FFCF4A': ('#74C69D','#8ED8AE')},
    'growth_chart': {'#009EF7': ('#2D6A4F','#8ED8AE'), '#FF0000': ('#713A28','#F0B49B'), '#FFB64D': ('#95D5B2','#A9D8BC'), '#F88951': ('#E07A5F','#F0B49B'), '#E54281': ('#2D6A4F','#8ED8AE'), '#00D4BD': ('#52796F','#A9D8BC')},
    'stock_box': {'#3931AC': ('#2D6A4F','#8ED8AE'), '#FFFFFF': ('#FFFFFF','#19211C')},
    'file_import': {'#0F68A4': ('#1B4332','#1B3A2A'), '#65CBFC': ('#95D5B2','#A9D8BC'), '#3092CE': ('#2D6A4F','#8ED8AE'), '#FFF0E9': ('#FAFAF8','#F1F5F2')},
    'cloud_backup': {'#5E87CA': ('#2D6A4F','#8ED8AE'), '#C0DAFC': ('#95D5B2','#A9D8BC'), '#B52F28': ('#713A28','#F0B49B'), '#E2E7F6': ('#D8F3DC','#D8F3DC'), '#F6B940': ('#E07A5F','#F0B49B'), '#F4A93C': ('#D4836A','#F0B49B')},
}
PLAN_GRADIENTS = {
    '#68B2FC': ('#74C69D','#8ED8AE'), '#B4D8FD': ('#D8F3DC','#D8F3DC'),
    '#285ECA': ('#1B4332','#357956'), '#3D7CE0': ('#2D6A4F','#52B788'),
    '#529BF7': ('#52B788','#8ED8AE'), '#5DA7F9': ('#52B788','#8ED8AE'),
}

def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values(): yield from walk(child)
    elif isinstance(value, list):
        for child in value: yield from walk(child)

def encode(value):
    return (json.dumps(value, ensure_ascii=False, separators=(',', ':'))+'\n').encode('utf-8')

def build(write=False):
    manifest = []
    total = 0
    for name, (title, author, slug, remote, source_hash) in SOURCES.items():
        archive = SOURCE / (name+'.lottie')
        assert hashlib.sha256(archive.read_bytes()).hexdigest() == source_hash, f'Source changed: {name}'
        with zipfile.ZipFile(archive) as z:
            files = [n for n in z.namelist() if n.startswith('animations/') and n.endswith('.json')]
            assert len(files) == 1, name
            original = json.loads(z.read(files[0]))
        assert not original.get('fonts') and not original.get('chars'), f'External font: {name}'
        for asset in original.get('assets', []):
            assert asset.get('layers') and not asset.get('p') and not asset.get('u'), f'External media: {name}'
        assert all(not isinstance(o.get('x'), str) for o in walk(original)), f'Expression: {name}'
        variants = {}
        for mode in range(2):
            data = copy.deepcopy(original)
            for obj in walk(data):
                if name == 'business_plan' and obj.get('ty') in ('gf','gs'):
                    gradient = obj.get('g',{})
                    stops = gradient.get('k',{}).get('k')
                    if isinstance(stops,list) and all(isinstance(v,(int,float)) for v in stops):
                        for i in range(gradient.get('p',0)):
                            start = i*4+1
                            old = '#'+''.join(f'{round(v*255):02X}' for v in stops[start:start+3])
                            if old in PLAN_GRADIENTS:
                                new = PLAN_GRADIENTS[old][mode].lstrip('#')
                                stops[start:start+3] = [int(new[n:n+2],16)/255 for n in (0,2,4)]
                if obj.get('ty') not in ('fl','st'): continue
                color = obj.get('c', {}).get('k')
                if not isinstance(color, list) or len(color)<3 or not all(isinstance(v,(float,int)) for v in color): continue
                old = '#'+''.join(f'{round(v*255):02X}' for v in color[:3])
                if old in PALETTES[name]:
                    new = PALETTES[name][old][mode].lstrip('#')
                    color[:3] = [int(new[n:n+2],16)/255 for n in (0,2,4)]
            filename = 'arina_'+name+('_dark' if mode else '')+'.json'
            raw = encode(data)
            assert len(raw)<200_000, f'Unexpectedly large asset: {filename}'
            if write: (RAW/filename).write_bytes(raw)
            else: assert (RAW/filename).read_bytes() == raw, f'Rebuild needed: {filename}'
            variants['dark' if mode else 'light'] = {'file':filename,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}
            total += len(raw)
        manifest.append({'name':name,'title':title,'author':author,'page':'https://lottiefiles.com/free-animation/'+slug,'download':'https://assets-v2.lottiefiles.com/a/'+remote+'.lottie','source_sha256':source_hash,'license':'Lottie Simple License (FL 9.13.21)','variants':variants})
    output = json.dumps(manifest, ensure_ascii=False, indent=2)+'\n'
    path = SOURCE/'manifest.json'
    if write: path.write_text(output,encoding='utf-8')
    else: assert path.read_text(encoding='utf-8') == output, 'Manifest changed'
    assert (ROOT/'app/src/main/assets/licenses/lottie-simple-license.txt').exists()
    attribution = 'Arina contextual animation credits\n\n' + '\n\n'.join(
        f'{r["title"]} — {r["author"]}\n{r["page"]}' for r in manifest
    ) + '\n\nArina color adaptations retain the Lottie Simple License (FL 9.13.21). Application code is separately licensed under GPL-3.0.\n'
    credits = ROOT/'app/src/main/assets/licenses/lottie-attribution.txt'
    if write: credits.write_text(attribution,encoding='utf-8')
    else: assert credits.read_text(encoding='utf-8') == attribution, 'Credits changed'
    print(f'PASS: 5 pinned vector sources; 10 deterministic offline variants; {total:,} bundled JSON bytes; license included')

if __name__ == '__main__':
    build('--write' in sys.argv)
