---
status: current-mvp
last_verified: 2026-08-15
audience: owner-testers-and-maintainers
sensitivity: internal
---

# Panduan Tes Manual MVP

Panduan ini menguji MVP debug, bukan release Play Store. Gunakan data sintetis; jangan jadikan MVP saat ini sebagai satu-satunya penyimpanan catatan keuangan asli.

## 1. Jalankan dari Android Studio

1. Buka Android Studio dari `E:\Android\Android Studio`.
2. Pilih **Open** lalu buka `E:\Managed Projects\Arina\arina-agri-calculator`.
3. Tunggu Gradle sync selesai. SDK Location harus `E:\Android\Sdk`.
4. Buka **Tools → Device Manager** dan nyalakan `arina_api30`, atau sambungkan ponsel Android dengan USB debugging.
5. Pilih konfigurasi `app`, lalu tekan tombol Run hijau.

Alternatif tanpa Android Studio: pasang `app\build\outputs\apk\debug\app-debug.apk` pada ponsel uji. Package `com.example.arinaagri.calculator` dan debug signing bukan identitas release.

## 2. Smoke test pencatatan dan akuntansi

Pada lingkup awal `Usaha Saya` → `Umum`, simpan empat catatan berikut:

| Jenis | Keterangan | Kategori | Jumlah |
| --- | --- | --- | ---: |
| Uang masuk | Servis traktor | Jasa | Rp1.250.000 |
| Uang keluar | Suku cadang | Operasional | Rp250.001 |
| Modal | Setoran pemilik | Modal | Rp5.000.000 |
| Pinjaman | Pinjaman koperasi | Pembiayaan | Rp2.000.000 |

Buka **Laporan** dan cocokkan:

| Baris laporan | Hasil yang benar |
| --- | ---: |
| Pendapatan usaha | Rp1.250.000 |
| Beban usaha + bunga | Rp250.001 |
| Laba/rugi | Rp999.999 |
| Kas operasi | Rp999.999 |
| Kas pembiayaan | Rp7.000.000 |
| Saldo kas | Rp7.999.999 |

Modal dan pinjaman tidak boleh menambah pendapatan atau laba.

## 3. Persistensi dan isolasi lingkup

1. Tutup paksa aplikasi, kemudian buka lagi. Keempat catatan dan semua angka harus tetap sama.
2. Tambahkan usaha `Usaha Uji Kedua` dan proyek `Proyek B`.
3. Pindah ke lingkup baru. Laporan harus dimulai dari Rp0.
4. Kembali ke `Usaha Saya` → `Umum`. Empat catatan awal harus muncul kembali tanpa tercampur.

## 4. Tes impor tanpa enrichment

Gunakan proyek kosong dan fixture sintetis:

`..\arina-finance-workbook-fixtures\fixtures\canonical\WB-CAN-001\workbook.xlsx`

Di menu **Impor**, pilih workbook tersebut. Pratinjau yang benar:

- 5 lembar dikenali;
- 16 catatan langsung: 15 pengeluaran dan 1 pemasukan;
- 0 pemulihan/enrichment;
- pemasukan Rp45.500.000;
- pengeluaran Rp22.159.000;
- tidak ada konflik atau tindakan manual.

Setelah disetujui, laporan harus menunjukkan laba Rp23.341.000. Memilih file yang sama lagi harus ditolak sebagai duplicate hash.

## 5. Tes enrichment yang terlihat

Gunakan proyek kosong lain dan fixture:

`..\arina-finance-workbook-fixtures\fixtures\enrichment\WB-ENR-001\workbook.xlsx`

Pratinjau yang benar:

- 5 lembar dikenali;
- 15 catatan langsung: 14 pengeluaran dan 1 pemasukan;
- tepat 1 pemulihan otomatis;
- `Sewa Lahan` Rp7.000.000 terlihat sebelum commit;
- sumber yang ditampilkan mencakup RAB, laporan laba-rugi, dan arus kas;
- asumsi tanggal/periode ditampilkan, bukan disembunyikan;
- total akhir tetap: pemasukan Rp45.500.000, pengeluaran Rp22.159.000, laba Rp23.341.000.

Jika enrichment muncul tanpa sumber atau notifikasi, anggap tes gagal.

## 6. Tes tampilan tablet

Di Device Manager, buat perangkat virtual kategori tablet menggunakan image API 30 Google APIs x86_64 yang sudah tersedia. Jalankan aplikasi dan pastikan:

- navigasi berpindah dari baris bawah menjadi rail di sisi kiri;
- kartu ringkasan tidak saling menimpa;
- pemilih usaha/proyek, form pencatatan, laporan, dan pratinjau impor tetap dapat digunakan.

## 7. Batas kegagalan yang harus dicatat

Jangan menyatakan MVP lolos jika terjadi salah satu hal berikut:

- angka berubah setelah restart;
- transaksi usaha/proyek lain tercampur;
- modal atau pinjaman menaikkan pendapatan/laba;
- workbook dimutasi sebelum pratinjau disetujui;
- enrichment terjadi tanpa bukti dan notifikasi;
- workbook konflik tetap dapat di-commit;
- aplikasi crash, membeku, atau kehilangan data.

Catat perangkat/API, langkah, expected result, actual result, screenshot, dan apakah masalah dapat diulang. Fitur ekspor, pembalikan impor, piutang/utang, backup terenkripsi, dan release signing memang belum tersedia dan tidak dihitung sebagai kegagalan implementasi MVP saat ini.
