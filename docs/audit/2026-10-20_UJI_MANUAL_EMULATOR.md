# Uji Manual di Emulator — 20 Oktober 2026

> Uji aktual lewat UI aplikasi (bukan unit test), pada emulator `Medium_Phone` (API 36, 1080×2400) dengan data contoh nyata yang sudah ada di perangkat (skema 7 → diperbarui ke 8).
> Build: `main` setelah `75fae29` + perbaikan sesi ini. Pelaksana: Claude Code (Opus 5.5).

## Hasil

| # | Skenario | Langkah | Hasil |
|---|---|---|---|
| U1 | Pembaruan database 7 → 8 pada data nyata | Pasang APK baru di atas data lama, buka aplikasi | ✅ `user_version` 8; 13 catatan non-kas dihapus; notifikasi rincian tampil di Berkas & Data; laba = uang masuk bersih (Rp8.340.000); Biaya Operasional = Komposisi Biaya |
| U2 | Setoran modal lewat kategori | Uang masuk → kategori "Modal Sendiri" → Rp1.000.000 | ✅ Kas +Rp1.000.000, laba tetap |
| U3 | Form catat dengan keyboard terbuka | Isi nominal & keterangan, tekan Simpan tanpa menutup keyboard | ✅ setelah perbaikan (sebelumnya ❌ tombol tertutup keyboard) |
| U4 | Tab Stok | Tambah barang, Keluar, Opname | ✅ (diuji sesi sebelumnya) |
| U5 | Unduh template | Berkas & Data → Template Excel → simpan | ✅ 6 lembar & judul kolom sesuai `MankeuTemplate`; lembar Kategori berisi kategori usaha ini |
| U6 | Impor template yang diisi seperti Excel (sel tanggal asli, angka) | openpyxl mengisi 3 transaksi, 1 RAB, parameter BEP, 2 barang → pilih file → tinjau → setujui | ✅ setelah perbaikan B1. Kas 6.140.000 → 6.290.000, laba 8.340.000 → 8.690.000 (prive tidak mengurangi laba), Gula Aren masuk, Gula Pasir dilewati, parameter BEP terisi |
| U7 | File bukan template | Pilih workbook simulasi lama | ✅ ditolak, daftar lembar yang kurang, tombol "Unduh template Mankeu" |
| U8 | Ekspor Excel | Ekspor → buka dengan openpyxl | ✅ lembar template + Laporan/Kelayakan; masuk Rp150.340.000 − keluar Rp144.050.000 = kas Rp6.290.000 = lembar Laporan |
| U9 | Ekspor → impor ulang (round trip) | Pilih file hasil ekspor, lihat pratinjau | ✅ 47 transaksi, 46 RAB, total identik, tidak ada kategori baru tak terduga (tidak disimpan agar tidak dobel) |
| U10 | Batalkan impor (batch) | Riwayat → Balik batch | ✅ kas & laba kembali tepat; barang impor terhapus, barang lama utuh |
| U11 | Ekspor PDF | Ekspor PDF → ekstrak teks | ✅ bagian "Titik Impas (BEP) & Kelayakan Usaha" ada; tidak ada kata pinjaman/utang/piutang/bunga. Menemukan B2 |
| U12 | Cadangan terenkripsi + pulihkan | Buat cadangan (sandi), catat pengeluaran Rp10.000, pulihkan | ✅ pratinjau pemulihan benar (skema 8, 52 transaksi); setelah pulih kas kembali Rp6.140.000 |
| U13 | Tombol Back sistem | Back dari Catatan / Berkas & Data | ⚠️ **tidak dapat diuji**: Back juga tidak berfungsi di aplikasi Settings bawaan emulator ini, jadi masalahnya injeksi tombol di emulator, bukan Mankeu. Catatan A-21 "Back tidak menutup form" kemungkinan besar akibat hal yang sama. Perlu dicek di HP fisik. |

## Bug yang ditemukan oleh uji manual (lolos dari unit test) — sudah diperbaiki

- **B1 · Usaha lama tidak punya kategori Prive/Aset.** Usaha yang dibuat sebelum DEC-011 tidak memiliki kategori peran Ambil Pribadi, sehingga prive tidak bisa dicatat. Impor juga akan membuat "Ambil untuk Pribadi (Prive)" sebagai *biaya biasa* dan mengurangi laba.
  Perbaikan: saat aplikasi dibuka, usaha yang belum punya kategori Setoran Modal/Prive/Aset aktif diberi kategori dasarnya. Saat impor, nama kategori dasar memakai peran dari template, bukan dari kolom.
  Uji: `ImportCommitTest.anOlderBusinessGetsTheBasicNonOperatingCategoriesAndImportKeepsTheirMeaning`.
- **B2 · BEP Nilai meleset.** FC 3.000.000, P 15.000, VC 6.000 menghasilkan Rp4.999.995, padahal seharusnya Rp5.000.000. Penyebabnya: volume BEP yang sudah dibulatkan dikalikan harga.
  Perbaikan: rumus eksak FC × P ÷ (P − VC), dibulatkan sekali di akhir.
  Uji: `BusinessAnalysisTest.BEP revenue is exact…`.
- **B3 · Teks usang.** Dialog cadangan menyebut "piutang/utang". Tur Catatan menyebut menu "Jenis lain", Pinjaman, dan penjualan tempo. Tur Laporan menyebut basis akrual dan penjualan tempo.
  Perbaikan: semua ditulis ulang (id + en), dan 116 string yang tidak terpakai dihapus.

## Catatan teknis pelaksanaan

- Pemilih berkas Android (DocumentsUI) di emulator tidak merespons ketukan setelah fokus jendela macet. Berkas dipilih dengan navigasi keyboard (Tab/Enter). Ini perilaku emulator, bukan aplikasi.
- Belum diuji manual: login Google dan sinkron/cadangan Google Drive (butuh akun), tampilan tablet, mode gelap, bahasa Inggris.
