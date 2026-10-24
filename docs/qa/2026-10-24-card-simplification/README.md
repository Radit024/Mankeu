# QA & Acceptance: Penyederhanaan Informasi Card

Dokumentasi baseline, verifikasi angka keuangan, dan pembuktian implementasi penyederhanaan card Arina Agri sesuai [Implementation Plan](../../superpowers/plans/2026-10-24-card-information-simplification.md).

## 1. Baseline Angka & Kontrak Keuangan

Semua pengujian dan tampilan wajib mematuhi pemisahan domain berikut:
- **`state.summary` (Seluruh Catatan)**: Menghitung total kas dari awal pencatatan sampai sekarang pada buku yang dipilih (`selectedBookId`).
  - Pendapatan Kas: Rp5.500.000
  - Beban Kas Operasional: Rp4.150.000
  - Laba / Rugi Kas: Rp1.350.000
  - Saldo Kas Akhir: Rp9.150.000 (termasuk modal awal Rp15.000.000 dan pembelian aset Rp7.200.000)
- **`actualMonth` (Bulan Berjalan / Sumber Analisis)**: Menghitung analisis akuntansi khusus untuk 1 bulan tertentu (`actualMonth.month`), memperhitungkan penyusutan aset bulanan.
  - Penyusutan: Rp200.000 / bulan (Mesin espresso Rp7.200.000 umur 36 bulan)
  - Laba Bersih Setelah Penyusutan: Rp1.150.000 (Laba kas Rp1.350.000 - Penyusutan Rp200.000)
- **Invarian Produk & HPP**:
  - Biaya variabel per unit dan margin kontribusi per unit dihitung dari `PlanAssembly.products(...)` dengan pembulatan `RoundingMode.HALF_UP`.
  - Margin kontribusi = Harga Jual - Biaya Variabel Satuan (bukan laba bersih akhir).

## 2. Sasaran Penyederhanaan

1. **CardDetails**: Komponen Compose disclosure instan beraksesibilitas penuh (TalkBack `stateDescription`, target sentuh $\ge 48\text{ dp}$, state terisolasi per `stateKey`).
2. **Beranda**:
   - Saldo kas menampilkan nominal lengkap dan basis periode (`Seluruh catatan`), defisit tetap terlihat jelas, tombol tindakan menuju rincian kas.
   - Hasil usaha menyatukan laba/rugi kas sebagai headline dengan rincian pendapatan & biaya di bawahnya, dan penjelasan kas operasional di dalam disclosure.
   - Pengeluaran menampilkan maksimal 3 kategori teratas, sisanya dapat dibuka lewat `CardDetails`.
   - Card stok di Beranda disederhanakan (`compact = true`) fokus pada peringatan jumlah barang menipis/habis atau status aman.
3. **BEP & Kelayakan Usaha (`PlanCards.kt`)**:
   - Peringatan kerugian (`sellsAtALoss`, margin negatif, kekurangan modal) SELALU tampil di luar disclosure.
   - Angka utama (status impas, omzet terhadap target/ambang) tampil langsung, sedangkan rincian tabel per produk, rasio R/C, B/C berada dalam disclosure.
4. **Form & Editor (`PlanScreen.kt`, `EntryScreen.kt`)**:
   - Bantuan pengisian panjang dipindahkan ke disclosure sekunder, seluruh field input dan pesan error tetap berada di luar.
5. **Review Impor (`MoreScreen.kt`)**:
   - Ringkasan impor menampilkan nama file, jumlah baris, total nominal, dan peringatan duplikat/konflik secara transparan sebelum commit.
