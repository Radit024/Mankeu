"""Validate local image/link evidence and task structure of the UI refinement plan."""
from pathlib import Path
from urllib.parse import urlsplit, unquote
import hashlib
import json
import re

ROOT=Path(__file__).resolve().parents[2]
PLAN=ROOT/'docs/superpowers/plans/2026-10-24-ui-refinement.md'
ASSETS=PLAN.parent/'assets/2026-10-24-ui-refinement'

def main():
    content=PLAN.read_text(encoding='utf-8')
    tasks=re.findall(r'^### Task (\d+)\b',content,re.M)
    assert tasks==[str(n) for n in range(1,11)],tasks
    assert len(re.findall(r'^```',content,re.M))%2==0,'Unbalanced code fences'
    assert not re.search(r'\b(?:T'+'BD|T'+'ODO)\b|implement later|fill in details',content,re.I),'Placeholder'
    images=re.findall(r'!\[[^\]]*\]\(([^)]+)\)',content)
    assert len(images)>=20,len(images)
    local_links=0
    for url in re.findall(r'\]\(([^)]+)\)',content):
        u=urlsplit(url)
        if u.scheme or u.netloc: continue
        p=(PLAN.parent/unquote(u.path)).resolve()
        assert p.is_relative_to(ROOT.resolve()) and p.is_file(),url
        local_links+=1
    manifest=json.loads((ASSETS/'sources.json').read_text(encoding='utf-8'))
    assert len(manifest['references'])==3
    for r in manifest['references']:
        p=ASSETS/r['file']
        data=p.read_bytes()
        assert data.startswith(b'\x89PNG\r\n\x1a\n'),p.name
        assert hashlib.sha256(data).hexdigest()==r['sha256'],p.name
        assert r['url'].startswith('https://')
    print(f'PASS: 10 tasks, {len(images)} embedded images, {local_links} local links, 3 browser screenshot hashes, balanced fences, no placeholders.')

if __name__=='__main__': main()
