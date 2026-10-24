"""Build the review gallery from the recorded 2026-10-24 native Android captures.

This is a screenshot evidence viewer, not a reimplementation of the Android app.
Run from any directory with Python 3.10+. It never talks to a device or modifies books.
"""
from pathlib import Path
from html import escape
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/qa/2026-10-24-ui-simulation'
TITLES = '''Selamat datang
Pemakaian pertama tanpa usaha
Kelola usaha dan unit
Berkas dan Data sebelum impor
Pratinjau impor — konten bertumpuk
Hasil impor — konten bertumpuk
Beranda dengan data
Riwayat transaksi
Pilihan cepat pencatatan
Form pemasukan
Form pengeluaran
Rincian transaksi tambahan
Transaksi berhasil disimpan
Konfirmasi pembalikan
Riwayat sesudah pembalikan
Stok dengan satu barang menipis
Form tambah barang
Form stok masuk
Form stok keluar
Form stok opname
Laporan laba rugi dan arus kas
Grafik dan rincian kas bulanan
Kelayakan dan BEP aktual
Perbandingan proyeksi dan realisasi
Ringkasan rencana usaha
Editor periode rencana
Produk dan biaya tetap
Editor produk dan resep
Komponen dan margin produk
Editor biaya tetap
Biaya lain, aset, dan modal
Editor aset dan penyusutan
Editor setoran modal
Editor biaya dan pendapatan lain
Riwayat impor dan ekspor
Pemilih lokasi ekspor Android
Hasil ekspor Excel
Hasil ekspor PDF
Pilihan cadangan lokal dan cloud
Form buat cadangan lokal
Form pulihkan cadangan lokal
Kelola kategori pembukuan
Pengaturan profil
Pengaturan akun
Pengaturan umum
Pengaturan tema gelap
Pengaturan sistem
Pengaturan panduan
Pusat Bantuan
Artikel bantuan
Contoh layar dan topik terkait
Tunjukkan di aplikasi
Konfirmasi pembalikan batch impor
Penolakan workbook bukan template
Konfirmasi hapus transaksi'''.splitlines()

GROUPS = [
 ('mulai', 'Mulai dan ruang usaha', [1,2,3], 'Alur tanpa akun, usaha fiktif, dan unit terpisah berhasil dibuat.'),
 ('beranda', 'Beranda', [7], 'Perlu variasi: empat kartu metrik memakai aksen mint dan angka hijau yang hampir sama. Usulan: bedakan kategori dengan permukaan pastel, angka gelap, dan status finansial tersendiri.'),
 ('transaksi', 'Pencatatan dan koreksi', list(range(8,16))+[55], 'Simpan Rp50.000 dan pembalikan dijalankan. Konfirmasi hapus hanya dibuka lalu dibatalkan. Tinjau label deskripsi panjang serta konsistensi warna pemasukan/pengeluaran.'),
 ('stok', 'Persediaan', list(range(16,21)), 'Daftar berisi tiga barang dan satu peringatan stok menipis. Form mutasi dibuka dan dibatalkan; penyimpanan mutasi belum diuji. Usulan visual: ringkasan stok/minimum pada baris barang agar status lebih cepat dipindai.'),
 ('laporan', 'Laporan dan perbandingan', list(range(21,25)), 'Ketiga tab dibuka dengan data nyata dari fixture lokal. Usulan visual: perjelas pasangan nilai proyeksi/realisasi dan selisih; hindari menonjolkan semua baris dengan bobot sama. Grafik hanya memiliki satu bulan data.'),
 ('rencana', 'Rencana usaha dan HPP', list(range(25,35)), 'Periode, produk, resep, biaya tetap, biaya lain, aset, dan modal terlihat. Editor dibuka tanpa menyimpan perubahan. Usulan: utamakan margin/HPP pada ringkasan produk; pisahkan input dan hasil hitung secara visual.'),
 ('data', 'Impor, kategori, dan ekspor', [4,5,6,35,36,37,38,42,53,54], 'PRIORITAS P1: pratinjau, hasil, dan kegagalan impor memiliki teks/tombol saling menimpa. Impor dapat diselesaikan dengan navigasi keyboard untuk fixture ini, tetapi jalur sentuh tetap bermasalah. Ekspor Excel dan PDF menghasilkan berkas lokal.'),
 ('cadangan', 'Cadangan dan pemulihan', [39,40,41], 'Form lokal dan keadaan belum masuk Google dibuka. Pembuatan/pemulihan cadangan penuh dan layanan Google sungguhan belum dijalankan.'),
 ('pengaturan', 'Profil dan pengaturan', list(range(43,49)), 'Tab Profil, Umum, Sistem serta pergantian tema dibuka. Penyimpanan profil dan perubahan bahasa belum diuji. Field profil panjang perlu ditinjau menurut kebutuhan pengguna, bukan ditambah dekorasi.'),
 ('bantuan', 'Bantuan dan panduan', list(range(49,53)), 'Artikel, contoh gambar, dan Tunjukkan di aplikasi berhasil dibuka. Daftar topik memakai ikon buku seragam; usulan: ikon per kelompok dan pengelompokan yang membantu pemindaian.'),
]

def main():
    captures=[]
    for number,title in enumerate(TITLES,1):
        matches=list(OUT.glob(f'{number:02d}-*.png'))
        if len(matches)!=1: raise ValueError(f'Expected one capture for {number}: {matches}')
        p=matches[0]
        captures.append(dict(file=p.name,title=title,group=next(g[0] for g in GROUPS if number in g[2]),source='native-navigation',state='captured',number=number))
    for p in sorted(OUT.glob('phone-*.webp'))+sorted(OUT.glob('tablet-light-*.webp')):
        captures.append(dict(file=p.name,title=p.stem,group='adaptif',source='in-memory-native-fixture',state='captured'))
    for name in ['light-reports','light-stock','light-plan','light-import','light-cloud']:
        captures.append(dict(file=name+'.png',title=name,group='kosong',source='in-memory-native-fixture; cloud auth is synthetic',state='captured'))
    for p in sorted(OUT.glob('help_*.webp')):
        captures.append(dict(file=p.name,title=p.stem,group='sorotan',source='native-help-capture; intentional spotlight scrim',state='captured'))
    for c in captures:
        p=OUT/c['file']
        c['sha256']=hashlib.sha256(p.read_bytes()).hexdigest()
        c['bytes']=p.stat().st_size
    manifest=dict(date='2026-10-24',platform='Native Kotlin / Jetpack Compose',device='codex_arina_welcome / emulator-5554',phone='1080x2400 px, density 420',tablet='1600x2560 px, density 240 (emulator viewport override; restored)',count=len(captures),captures=captures)
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    groups=GROUPS+[
      ('adaptif','Tema terang, gelap, dan tablet',[], 'Layar aplikasi asli dengan fixture in-memory: tujuh transaksi, dua produk, dan dua stok. Berbeda dari demo persisten yang mempunyai tiga stok. Ini pemeriksaan tata letak, bukan bukti seluruh workflow pada tablet.'),
      ('kosong','Keadaan kosong dan cloud',[], 'Keadaan kosong memakai fixture terpisah. Form cloud memakai identitas fiktif dalam pengujian; tidak ada login Google atau unggahan nyata.'),
      ('sorotan','19 titik fitur dalam panduan',[], 'Area gelap pada gambar adalah scrim sorotan panduan yang sengaja diaktifkan. Ini bukan tema gelap dan bukan cacat kontras layar biasa.'),
    ]
    chunks=[]
    for gid,title,_,note in groups:
        entries=[c for c in captures if c['group']==gid]
        cards=''.join(f'<figure><a href="{escape(c["file"])}" target="_blank" rel="noopener"><img loading="lazy" src="{escape(c["file"])}" alt="{escape(c["title"])}"></a><figcaption>{escape(c["title"])}<small>{escape(c["source"])}</small></figcaption></figure>' for c in entries)
        chunks.append(f'<section id="{gid}"><h2>{escape(title)} <span>{len(entries)} layar</span></h2><p class="note">{escape(note)}</p><div class="grid">{cards}</div></section>')
    nav=''.join(f'<a href="#{gid}">{escape(title)}</a>' for gid,title,_,_ in groups)
    html='''<!doctype html><html lang="id"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Arina Agri — Simulasi dan audit UI</title>
<style>*{box-sizing:border-box}html{scroll-behavior:auto}body{margin:0;background:#f3f4f1;color:#202b25;font:16px/1.6 system-ui,sans-serif}main{max-width:1440px;margin:auto;padding:32px}h1{font-size:32px;line-height:1.2}h2{font-size:24px}h2 span{font:14px system-ui;color:#526258}header{max-width:980px}nav{display:flex;flex-wrap:wrap;gap:8px;margin:24px 0}a{color:#24513e}nav a{padding:8px 12px;border:1px solid #c1ccc5;border-radius:8px;background:#fff}.notice{border-left:5px solid #9d4c34;background:#fff1e9;padding:16px}section{margin:48px 0;scroll-margin-top:24px}.note{max-width:1000px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:20px;align-items:start}figure{margin:0;background:white;border:1px solid #d5ddd7;border-radius:10px;overflow:hidden}figure a{display:block;background:#e8ece8}img{display:block;width:100%;height:510px;object-fit:contain;object-position:top}figcaption{padding:12px;font-weight:600;font-size:14px}small{display:block;color:#647269;font-weight:400;font-size:11px;overflow-wrap:anywhere}footer{padding:24px;background:#e5ebe6}@media(max-width:600px){main{padding:16px}.grid{grid-template-columns:1fr 1fr;gap:10px}img{height:340px}h1{font-size:26px}figcaption{font-size:12px}}
</style><main><header><p>ARINA AGRI / 24 OKTOBER 2026 / AUDIT UI</p><h1>Simulasi fitur dari layar Android asli</h1><p>COUNT tangkapan layar. Klik gambar untuk ukuran penuh. Navigasi kelompok di bawah untuk memeriksa setiap fitur. Galeri ini merekam hasil simulasi; demo yang dapat dioperasikan berada di emulator Android.</p><p class="notice"><strong>Temuan utama:</strong> konten impor bertumpuk; kartu metrik Beranda terlalu seragam; daftar stok serta perbandingan laporan dapat memiliki hierarki yang lebih jelas. Perbaikan desain belum diterapkan.</p><p><a href="README.md">Laporan, cakupan, dan cara mengulang</a> · <a href="SIMULASI-UI-KOPI.xlsx">Workbook fiktif</a> · <a href="EXPORT-SIMULASI.xlsx">Hasil ekspor Excel</a> · <a href="EXPORT-SIMULASI.pdf">Hasil ekspor PDF</a></p></header><nav>NAV</nav>SECTIONS<footer>Data sepenuhnya fiktif. Screenshot bukan bukti semua perhitungan, aksesibilitas, animasi, atau layanan cloud telah lulus pengujian. Lihat laporan untuk batas pemeriksaan. <a href="#">Kembali ke atas</a></footer></main></html>'''
    html=html.replace('COUNT',str(len(captures))).replace('NAV',nav).replace('SECTIONS',''.join(chunks))
    (OUT/'index.html').write_text(html,encoding='utf-8')
    print(f'Gallery built: {len(captures)} screenshots / {len(groups)} groups')

if __name__=='__main__': main()
