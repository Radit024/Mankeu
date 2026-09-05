# Panduan Lengkap Isian Google Play Console - Mankeu

Dokumen ini berisi seluruh teks, pilihan jawaban kuesioner kebijakan, dan aset visual yang siap di-copy-paste langsung ke Google Play Console saat mendaftarkan aplikasi **Mankeu** (`com.mankeu`).

---

## 1. Data Aplikasi Dasar (Create App)

| Kolom Input | Isian / Jawaban |
| :--- | :--- |
| **App name** (Nama Aplikasi) | `Mankeu` |
| **Default language** (Bahasa default) | `Indonesian (Indonesia) – id` (atau `English – en-US`) |
| **App or game** | **App** |
| **Free or paid** | **Free** |
| **Declarations** (Pernyataan) | Centang kedua kotak persetujuan kebijakan & hukum ekspor AS. |

---

## 2. Kebijakan & Program Aplikasi (App Content)

Buka menu: **Policy and programs** > **App content** (atau ikuti daftar tugas pada *Dashboard > Set up your app*).

### A. Kebijakan Privasi (Privacy Policy)
* **Privacy Policy URL:**
  ```text
  https://adsurkasur.my.id/mankeu/privacy-policy
  ```

### B. Akses Aplikasi (App Access)
* **Pertanyaan:** Apakah sebagian/seluruh fungsi aplikasi memerlukan login/kredensial khusus?
* **Jawaban:** Pilih **"All functionality is available without special access"** *(Semua fungsi tersedia tanpa batasan khusus)*.
* *Alasan:* Seluruh kalkulator finansial, pembukuan kas harian, dan impor/ekspor Excel berjalan 100% offline tanpa registrasi.

### C. Iklan (Ads)
* **Pertanyaan:** Apakah aplikasi Anda memuat iklan?
* **Jawaban:** Pilih **"No, my app does not contain ads"** *(Tidak, aplikasi saya bebas iklan)*.

### D. Rating Konten (IARC Content Rating)
* Masukkan email kontak pengembang: `adsurkasur.dev@gmail.com`
* Kategori aplikasi: **Utility, Productivity, Communication, or Other** *(Utilitas/Produktivitas)*
* Kuesioner:
  * Kekerasan (Violence): **No**
  * Seksualitas: **No**
  * Bahasa kasar: **No**
  * Obat terlarang / Alkohol / Rokok: **No**
  * Fasilitas interaksi sosial dengan pengguna lain: **No**
  * Berbagi lokasi fisik: **No**
  * Pembelian barang digital: **No**
* Hasil yang diharapkan: **Rated for 3+ / Everyone**.

### E. Target Audiens & Konten (Target Audience)
* **Target Age Group:** Pilih **18 and over** *(18 tahun ke atas)*.
* **Apakah menarik bagi anak-anak:** Pilih **No**.

### F. Aplikasi Pemerintah (Government Apps)
* **Jawaban:** Pilih **No** *(Bukan aplikasi resmi pemerintah)*.

### G. Fitur Keuangan (Financial Features Declaration)
* **Jawaban:** Pilih **"Personal/Business accounting and expense tracking"** *(Akuntansi pribadi/bisnis & pencatatan biaya)*.
* *Peringatan:* **JANGAN** memilih opsi pinjaman (lending), perbankan, dompet digital, atau kripto.

### H. Keamanan Data (Data Safety Form)
* **Apakah aplikasi mengumpulkan data:** **Yes** *(Hanya untuk metadata login opsional jika mencadangkan ke Google Drive pribadi)*.
* **Apakah data dienkripsi in-transit:** **Yes** (HTTPS).
* **Apakah pengguna dapat meminta penghapusan data:** **Yes**.
* **Rincian Data yang Dikumpulkan:**
  * **Personal Info > Name & Email:**
    * Dikumpulkan? **Yes** (Opsional, hanya jika pengguna memilih *Sign in with Google*).
    * Dibagikan ke pihak ketiga? **No**.
    * Tujuan: *Account management* & *App functionality*.
  * **Financial Info (Data Keuangan):** **No** (Semua catatan transaksi bisnis dan uang kas disimpan secara lokal di HP pengguna).
  * **Files and Docs:** **No** (File Excel diproses di memori HP, file backup cloud langsung dikirim ke Drive pribadi pengguna).
  * **Device or Other IDs:** **No**.

---

## 3. Detail Halaman Toko (Main Store Listing)

Buka menu: **Grow** > **Store presence** > **Main store listing**.

### A. Pengaturan Toko & Kontak
* **App category:** **Business** (atau **Finance** / **Productivity**)
* **Tags (Label):** Bisnis, Finansial, Pembukuan Kas, Usaha Kecil
* **Email kontak pengembang:** `adsurkasur.dev@gmail.com`
* **Website:** `https://adsurkasur.my.id`

### B. Teks Listing Toko

#### 1. Nama Aplikasi (Maks 30 Karakter)
```text
Mankeu
```

#### 2. Deskripsi Singkat (Maks 80 Karakter)
```text
Kalkulator keuangan UMKM, analisis kelayakan BEP/HPP, dan pembukuan kas offline.
```

#### 3. Deskripsi Lengkap (Maks 4000 Karakter)
```text
Mankeu adalah aplikasi manajemen keuangan dan pencatatan kas modern yang dirancang khusus untuk pelaku UMKM (Usaha Mikro, Kecil, dan Menengah), pedagang, wirausaha kuliner, dan penyedia jasa di Indonesia.

Kelola rencana anggaran biaya (RAB), catat transaksi kas harian, dan hitung kelayakan bisnis Anda secara otomatis, akurat, dan 100% offline!

📊 FITUR UTAMA MANKEU:

1. KALKULATOR KELAYAKAN USAHA & METRIK UMKM OTOMATIS
• Hitung Titik Impas (BEP Rupiah Omzet & BEP Volume Penjualan).
• Hitung Harga Pokok Penjualan / Produksi (HPP per Unit/Porsi/Barang).
• Analisis Margin Laba Kotor (Gross Profit Margin) dan Laba Bersih (Net Profit Margin).
• Evaluasi keuntungan operasional riil berjalan secara real-time.

2. PEMBUKUAN KAS TERISOLASI (PROYEKSI VS REALISASI)
• Kelola Buku Realisasi (transaksi nyata berjalan) terpisah dari Buku Proyeksi (simulasi/anggaran RAB).
• Bandingkan selisih biaya aktual vs anggaran secara langsung untuk mencegah pembengkakan biaya.
• Kategorisasi fleksibel per sektor usaha: Stok Dagang, Bahan Baku, Gaji/Upah, Sewa Tempat, Kemasan, dan Operasional.

3. IMPOR & EKSPOR CERDAS SPREADSHEET EXCEL (.XLSX)
• Impor catatan keuangan harian langsung dari berkas Excel (.xlsx).
• Pencocok Semantik Otomatis yang mengklasifikasikan transaksi kas secara cerdas di perangkat.
• Ekspor laporan keuangan lengkap ke format PDF dan Excel (.xlsx) siap cetak/arsip.

4. 100% OFFLINE-FIRST & PRIVASI TOTAL
• Semua data keuangan Anda tersimpan aman di basis data lokal HP Anda (SQLite Room).
• Bebas iklan (No Ads), bebas pelacak (No Trackers), dan tidak memerlukan koneksi internet untuk pencatatan harian.

5. CADANGAN AWAN TERENKRIPSI (GOOGLE DRIVE)
• Sinkronisasi cadangan aman ke Google Drive pribadi Anda (opsional).
• Dilindungi enkripsi standar militer AES-256-GCM dengan kata sandi pribadi (passphrase).

💼 COCOK UNTUK BERBAGAI SEKTOR UMKM:
• Usaha Dagang / Ritel: Toko kelontong, kios, warung sembako, agen, reseller.
• Kuliner & F&B: Kafe, warung makan, resto, katering, bakery, kedai kopi.
• Jasa & Servis: Bengkel, laundry, barbershop/salon, rental/sewa alat, jasa servis.
• Usaha Agribisnis & Produksi: Pengolahan pangan, budidaya, kerajinan, dan manufaktur kecil.

Kelola arus kas dan kembangkan usaha UMKM Anda secara profesional bersama Mankeu!
```

---

## 4. Berkas Aset Gambar Siap Upload

Aset visual resmi dengan resolusi presisi telah dibuat dan siap di-drag-and-drop dari folder komputer Anda:

| Aset | Ukuran / Format | Lokasi Berkas di Komputer |
| :--- | :--- | :--- |
| **App Icon** | 512 × 512 px (PNG) | `E:\Managed Projects\umkm-man-keu\docs\playstore\app_icon_512x512.png` |
| **Feature Graphic** | 1024 × 500 px (PNG) | `E:\Managed Projects\umkm-man-keu\docs\playstore\feature_graphic_1024x500.png` |
| **Screenshot 1** | Beranda & Realisasi Kas | `E:\Managed Projects\umkm-man-keu\docs\playstore\screenshots\01_beranda_realisasi.png` |
| **Screenshot 2** | Proyeksi Anggaran M3 | `E:\Managed Projects\umkm-man-keu\docs\playstore\screenshots\02_proyeksi_anggaran.png` |
| **Screenshot 3** | Tampilan Mode Gelap | `E:\Managed Projects\umkm-man-keu\docs\playstore\screenshots\03_mode_gelap.png` |
| **Screenshot 4** | Navigasi Unit Usaha | `E:\Managed Projects\umkm-man-keu\docs\playstore\screenshots\04_unit_usaha_aktif.png` |

---

## 5. Menghubungkan Google Cloud Service Account via "Users and Permissions"

Jika ingin CI/CD GitHub Actions mengupload rilis secara otomatis:

1. **Di Google Cloud Console (`console.cloud.google.com`):**
   - Buat project baru: `Mankeu Cloud`.
   - Buka menu **APIs & Services > Library** -> Cari `Google Play Android Developer API` -> Klik **Enable**.
   - Buka menu **IAM & Admin > Service Accounts** -> Klik **Create Service Account**.
   - Beri nama: `github-actions-deployer` -> Klik **Done**.
   - Klik Service Account tersebut -> Tab **Keys** -> **Add Key > Create new key > JSON** -> Simpan file JSON yang terdownload.
   - Salin alamat email Service Account tersebut (misal `github-actions-deployer@mankeu-cloud.iam.gserviceaccount.com`).

2. **Di Google Play Console (`play.google.com/console`):**
   - Di menu navigasi samping, klik **Users and permissions** (Pengguna dan izin).
   - Klik tombol **Invite new users** (Undang pengguna baru).
   - Masukkan email Service Account Google Cloud di atas ke kolom **Email address**.
   - Pada bagian **App permissions**: Klik **Add app**, centang **Mankeu** (`com.mankeu`), lalu klik **Apply**.
   - Pada bagian **Permissions**: Centang **Manage track releases** dan **Upload release assets**.
   - Klik **Invite user** di pojok kanan bawah.

3. **Di GitHub Repository:**
   - Masukkan seluruh isi teks file JSON tersebut ke Secret:
     `PLAY_STORE_SERVICE_ACCOUNT_JSON` di `https://github.com/adsurkasur/umkm-man-keu/settings/secrets/actions`.