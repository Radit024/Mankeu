"""Verify recorded screenshot hashes, image signatures, gallery count and local links."""
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import unquote, urlsplit
import hashlib
import json

ROOT=Path(__file__).resolve().parents[2]/'docs/qa/2026-10-24-ui-simulation'

class Links(HTMLParser):
    def __init__(self): super().__init__(); self.urls=[]; self.images=0; self.ids=set()
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=='img': self.images+=1
        if 'id' in a: self.ids.add(a['id'])
        self.urls.extend(a[k] for k in ('src','href') if k in a)

def main():
    manifest=json.loads((ROOT/'manifest.json').read_text(encoding='utf-8'))
    seen=set()
    assert manifest['count']==91==len(manifest['captures'])
    for c in manifest['captures']:
        assert c['file'] not in seen, c['file']
        seen.add(c['file'])
        p=(ROOT/c['file']).resolve()
        assert p.parent==ROOT.resolve(), p
        data=p.read_bytes()
        assert len(data)==c['bytes'] and hashlib.sha256(data).hexdigest()==c['sha256'], p.name
        assert data.startswith(b'\x89PNG\r\n\x1a\n') or (data[:4]==b'RIFF' and data[8:12]==b'WEBP'), p.name
    parser=Links(); parser.feed((ROOT/'index.html').read_text(encoding='utf-8'))
    assert parser.images==manifest['count']
    for url in parser.urls:
        u=urlsplit(url)
        if u.scheme or u.netloc: continue
        if u.path:
            p=(ROOT/unquote(u.path)).resolve()
            assert p.is_relative_to(ROOT.resolve()) and p.is_file(),url
        elif u.fragment: assert u.fragment in parser.ids,url
    assert (ROOT/'SIMULASI-UI-KOPI.xlsx').read_bytes().startswith(b'PK')
    assert (ROOT/'EXPORT-SIMULASI.xlsx').read_bytes().startswith(b'PK')
    assert (ROOT/'EXPORT-SIMULASI.pdf').read_bytes().startswith(b'%PDF')
    print(f'PASS: {len(seen)} image files, SHA-256/signatures, {len(parser.urls)} gallery links, fixture and exports.')

if __name__=='__main__': main()
