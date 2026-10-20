# Uji Alur Nyata Lintas Jenis Usaha — 21 Oktober 2026

Tujuan: memastikan Rencana Usaha, BEP rencana, Status BEP bulan ini, impor, ekspor Excel, dan PDF
benar untuk usaha yang berbeda-beda, bukan hanya kedai kopi. Semua angka di layar dibandingkan
dengan hitungan pembanding yang dibuat terpisah dari aplikasi
(`docs/evidence/2026-10-21_simulasi_lintas_usaha/make_sims.py`, hasilnya `expected.json`). Skrip
itu menghitung dari prinsip dasar (laba nol tepat di titik impas), bukan memanggil kode aplikasi.

Perangkat: emulator Android API 34 (`emulator-5554`), build debug dari commit kerja hari ini.
Semua data uji fiktif.

## Simulasi

| Unit | Jenis usaha | Yang diuji khusus |
|---|---|---|
| A Toko Kelontong | Dagang, 3 produk | Bauran 3 produk, margin tipis (harus terbaca tidak layak), stok |
| B Laundry Kiloan | Jasa, 2 layanan, periode 12 bulan | Resep dengan yield (Rp45.000 untuk 30 kg), biaya sekali dibagi 12, penjualan tanpa produk dipecah menurut bauran |
| C Budidaya Cabai | Pertanian satu musim, 4 bulan | Biaya variabel di luar resep (bibit, pupuk), bulan tanpa penjualan, aset ditambah lewat layar |
| D Rental Traktor | Sewa alat, aset besar | Penyusutan besar, kebutuhan modal vs setoran, override sifat biaya "Upah operator" → Variabel |
| E Laundry Salinan | Salinan B | Ekspor B → impor ke unit kosong → harus identik; lalu batch dibalik |

Alur yang dijalankan lewat layar: buat usaha dan 5 unit, impor template per unit (pratinjau →
setujui), atur periode 12 bulan, baca Rencana Usaha, Laporan → Kelayakan, Beranda dan Laporan
Realisasi, tab Realisasi vs Target, ekspor Excel dan PDF, impor ulang hasil ekspor, balik batch,
tambah aset lewat formulir. Data yang masuk diperiksa juga langsung di database (`run-as` + sqlite).

## Hasil: layar vs hitungan pembanding

| Unit | Angka | Pembanding | Layar |
|---|---|---|---|
| A | BEP omzet / bulan | Rp29.271.186 | Rp29.271.186, status **Waspada** |
| A | Laba / bulan, R/C | −Rp975.000, 0,96 | sama |
| A Realisasi | BEP bulan ini, laba, selisih resep | Rp29.442.158, −Rp1.275.000, +Rp180.000 | sama |
| B | BEP per layanan | 851,955 kg dan 170,391 kg | sama |
| B | BEP omzet | Rp8.860.335 | Rp8.860.337 → **diperbaiki** → Rp8.860.335 |
| B Realisasi | Unit dari penjualan borongan Rp300.000 | 1.428,846 kg / 255,769 kg | sama |
| B Realisasi | BEP, laba, R/C, selisih | Rp8.742.367, Rp3.900.000, 1,37, Rp51.540 | sama |
| C | Dasar BEP, BEP | Rp6.225.000, 194,531 kg / Rp6.808.594 | sama (sebelum perbaikan aplikasi memberi 121,875 kg) |
| C + aset lewat layar | BEP, laba, kebutuhan modal | 198,438 kg / Rp6.945.313, Rp1.650.000, Rp13.100.000 | sama |
| D | BEP, laba, R/C | 6,429 hari / Rp3.857.143, Rp4.050.000, 1,60 | sama |
| D | Kebutuhan modal, setoran, kurang | Rp84.450.000, Rp50.000.000, Rp34.450.000 | sama |
| D Realisasi | BEP, laba, R/C, selisih | Rp3.857.143, Rp3.250.000, 1,51, +Rp100.000 | sama (override Variabel terbaca) |
| E vs B | Transaksi, baris rencana, resep, periode | identik | identik (8/5/6, Nov 2026–Okt 2027) |

## Temuan dan perbaikannya

| # | Temuan | Dampak bisnis | Perbaikan |
|---|---|---|---|
| T1 | Biaya variabel di luar resep dan pendapatan lain tidak masuk dasar BEP | Pada "titik impas" usaha masih rugi (cabai: laba −Rp2.325.000 di BEP lama) | Dasar BEP = biaya tetap + biaya variabel di luar resep − pendapatan lain; ditampilkan sebagai baris "Dasar BEP" |
| T2 | Kebutuhan modal dihitung setelah setoran yang direncanakan | Mengisi setoran membuat kebutuhan modal tampak nol | Tiga angka: kebutuhan modal (sebelum setoran), setoran direncanakan, masih kurang |
| T3 | BEP omzet bauran meleset Rp2 | Selisih tak terjelaskan | Rumus eksak satu kali bagi: FC × target × harga ÷ Σ(target × CM) |
| T4 | Bulan tanpa penjualan berstatus "Tidak Layak" dan menyuruh mengisi resep | Menyesatkan untuk usaha musiman | Status "Belum Dapat Dinilai" dan pesan "belum ada penjualan bulan ini" |
| T5 | Dua "laba" berbeda tanpa keterangan (kas vs setelah penyusutan) | Pemilik bingung angka mana yang benar | Label "Untung / Rugi kas", "Laba / Rugi kas", "Laba setelah penyusutan" |
| T6 | Realisasi vs Target membandingkan total rencana 12 bulan dengan realisasi 1 bulan; ada baris "Total Anggaran (RAB)" yang menjumlah modal dan aset | Selisih −Rp172.700.000 yang tidak bermakna | Rata-rata per bulan di kedua sisi, baris RAB dan saldo kas dihapus |
| T7 | Notifikasi impor tampil di semua usaha dan unit | Unit D menampilkan impor milik C | Notifikasi disaring per unit (notifikasi sistem tetap global) |
| T8 | Pratinjau impor tetap tampil setelah pindah unit | Berkas bisa tersimpan ke unit lain dengan pengecekan untuk unit lama | Pratinjau direset saat unit berganti |
| T9 | "Total pendapatan" di pratinjau ikut menjumlah setoran modal | Modal terbaca sebagai pendapatan | "Total uang masuk (termasuk setoran modal)" |
| T10 | Ringkasan Proyeksi di Excel dan PDF selalu Rp0 | Laporan tidak sama dengan layar | Diambil dari rencana usaha |
| T11 | Periode rencana tidak ikut diekspor | Impor ulang mengubah BEP (biaya sekali tidak lagi dibagi 12) | Baris "Periode rencana" di lembar Petunjuk; dipakai bila unit belum punya periode |
| T12 | Desimal di PDF/Excel bergaya Inggris ("851.955 kg", "1.42") | Dalam format Indonesia "851.955" terbaca delapan ratus ribu | PDF memakai desimal Indonesia; Excel menulis sel angka |
| T13 | "Rp-24.000.000", label "Proyek / pekerjaan", waktu ekspor dalam UTC | Kecil | "−Rp24.000.000", "Unit usaha", waktu lokal |
| T14 | Selisih resep di usaha dagang disebut "di atas resep" | Pembelian yang masih jadi stok terbaca boros | Kalimat menyebut kemungkinan: harga naik, boros, atau masih tersisa sebagai stok |
| T15 | Form biaya tetap memilih kategori variabel sebagai bawaan | Ditemukan kemarin | Memilih kategori bersifat tetap |

Tes otomatis yang ditambahkan untuk temuan ini: `BusinessPlanTest` (laba nol di BEP, kebutuhan
modal vs setoran, BEP bauran tepat ke rupiah), `TemplateImportTest` (periode ikut round trip,
periode salah ditolak), `ImportCommitTest` (periode diterapkan ke unit).

## Belum diselesaikan (perlu keputusan pemilik atau di luar lingkup)

> **Pembaruan 2026-10-21 (schema 10, DEC-017):** butir 1–5 sudah diselesaikan dan diuji nyata, lihat
> `2026-10-21_UJI_SCHEMA10.md`. Butir 6 tetap.

1. **Penjualan musiman.** Target produk selalu per bulan sejak bulan pertama. Untuk cabai yang
   baru panen di bulan ke-3, kebutuhan modal tercatat Rp13,1 juta padahal kas sebenarnya turun lebih
   dalam sebelum panen. Usul: kolom "mulai dijual bulan ke-" per produk.
2. **Balik batch tidak mengembalikan periode rencana** yang diisi oleh impor. Produk, transaksi,
   dan baris rencana sudah dibalik. Memperbaikinya butuh kolom baru di skema 9.
3. **Stok milik usaha, bukan unit.** Stok toko (A) tampil dan ikut terekspor di unit laundry dan
   traktor. Sesuai desain DEC-010, tetapi terasa janggal untuk usaha dengan unit berbeda jenis.
4. Pilihan usaha/unit tidak diingat setelah aplikasi ditutup.
5. Ringkasan batch menjumlah uang masuk dan keluar menjadi satu angka; nama berkas ekspor selalu
   `umkm-mankeu-<tanggal>` tanpa nama unit; tombol "impor N baris" ikut menghitung stok yang
   dilewati; periode dipilih lewat kalender harian.
6. Pemilih berkas Android di emulator kadang mengabaikan ketukan (dijalankan dengan navigasi
   keyboard); tidak terjadi di perangkat fisik pada uji sebelumnya.

## Verifikasi akhir

- Unit test 56/56, instrumented 19/19 di emulator, lint 0 error.
- Ekspor kedua unit B diperiksa: periode tertulis, angka berupa sel angka, ringkasan Proyeksi
  Rp187.200.000 = 12 × Rp15.600.000, PDF memuat "851,955 kg", "R/C 1,42", "−Rp24.000.000".
