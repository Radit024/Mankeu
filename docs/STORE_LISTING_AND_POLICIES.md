# Google Play Store Listing, Policy Declarations, and Compliance Guide

This guide provides the complete, authoritative compliance declarations, store listing copy (Indonesian and English), privacy policy hosting instructions, and visual asset inventory for **Mankeu** (`com.mankeu`).

---

## 1. Privacy Policy Hosting (GitHub Pages)

### Live URL
- **Public Privacy Policy URL:** `https://adsurkasur.github.io/umkm-man-keu/privacy-policy.html`

### Activation Steps on GitHub Pages:
1. Go to repository settings on GitHub: `https://github.com/adsurkasur/umkm-man-keu/settings/pages`
2. Under **Build and deployment** > **Source**: Select **Deploy from a branch**.
3. Under **Branch**: Select **`main`** and folder **`/docs`**.
4. Click **Save**.
5. GitHub Pages will build and publish the site at `https://adsurkasur.github.io/umkm-man-keu/privacy-policy.html`.

---

## 2. Google Play Console Compliance & Policy Declarations

Follow these exact steps and answers in Google Play Console under **Policy and programs** > **App content**:

### A. Privacy Policy
* **Privacy Policy URL:** `https://adsurkasur.github.io/umkm-man-keu/privacy-policy.html`

---

### B. App Access (Akses Aplikasi)
* **Question:** Is all or part of your app restricted based on login credentials, memberships, or other authentication?
* **Answer:** Select **"All functionality is available without special access"** (*Semua fungsi tersedia tanpa batasan khusus*).
* **Rationale:** The entire core financial functionality (budgeting, actual expense recording, feasibility metrics calculation, BEP/HPP/BCR, Excel workbook import, PDF export) operates 100% locally on device without registration or login. Google Sign-In is strictly optional for Drive backup.

---

### C. Ads (Iklan)
* **Question:** Does your app contain ads?
* **Answer:** Select **"No, my app does not contain ads"** (*Tidak, aplikasi saya tidak berisi iklan*).
* **Rationale:** The application is 100% ad-free, with zero third-party advertising SDKs or banner frameworks per project invariants (`O-011`).

---

### D. Content Rating (Kuesioner Rating Konten IARC)
Fill in the IARC Content Rating questionnaire as follows:
* **Category:** **Utility, Productivity, Communication, or Other** (*Utilitas, Produktivitas, atau Lainnya*).
* **Violence:** No
* **Sexuality / Adult Material:** No
* **Offensive Language:** No
* **Controlled Substance (Drugs/Alcohol):** No
* **User Interaction / Social Features:** No (Users do not exchange text or media with unknown third parties).
* **Physical Location Sharing:** No
* **Digital Goods / Purchases:** No
* **Expected Result:** **Rated for 3+ / Everyone** (PEGI 3, USK 0, ESRB Everyone).

---

### E. Target Audience and Content (Audiens Target dan Konten)
* **Target Age Group:** Select **"18 and over"** (*18 tahun ke atas*).
* **Could your store listing appeal to children?** Select **"No"** (*Tidak*).
* **Rationale:** The application is designed for adult MSME owners, small business managers, entrepreneurs, and operators managing business finances.

---

### F. Government Apps (Aplikasi Pemerintah)
* **Question:** Is this app developed by or on behalf of a government entity?
* **Answer:** Select **"No"** (*Bukan*).

---

### G. Financial Features Declaration (Fitur Keuangan)
* **Question:** What financial features does your app provide?
* **Answer:** Select **"Personal/Business accounting and expense tracking"** (*Akuntansi pribadi/bisnis dan pelacakan pengeluaran*).
* **Important Guardrails:**
  * **DO NOT** select: Banking, P2P Lending, Crypto, Digital Wallet, Money Transfer, or Investment Brokerage.
  * **Declaration Note:** Mankeu is an on-device MSME cost bookkeeping tool, business financial feasibility calculator (BEP, HPP, Gross/Net Margins), and Excel spreadsheet reconciler. It does not process financial transactions, loan applications, banking, or money transfers.

---

### H. Health & Medical Apps (Aplikasi Kesehatan)
* **Question:** Does your app provide human health, medical diagnosis, or clinical features?
* **Answer:** Select **"No"** (*Bukan*).

---

### I. Data Safety Form (Keamanan Data)

Fill in the Google Play Data Safety form with the following answers:

#### 1. Data Collection & Sharing Overview:
* **Does your app collect or share any user data?**
  * Select **"Yes"** (Only for optional Google account metadata during Drive backup).
* **Is all of the user data collected by your app encrypted in transit?**
  * Select **"Yes"** (HTTPS / OAuth 2.0).
* **Do you provide a way for users to request that their data be deleted?**
  * Select **"Yes"** (Users can delete all local data in Settings and delete cloud backups from Google Drive).

#### 2. Data Types & Purposes:
* **Personal Info > Name & Email Address:**
  * **Collected?** Yes (Optional, collected only if the user explicitly signs in with Google).
  * **Shared?** No (Never shared with third parties or developer servers).
  * **Ephemeral?** No (Cached locally in Room/DataStore).
  * **Required or Optional?** **Optional** (Users can use 100% of calculation/import features offline without signing in).
  * **Purpose:** **Account Management** & **App Functionality** (Drive backup sync identity).
* **Financial Info (User payment info, Purchase history, Credit score):**
  * **Collected?** **No** (Farm transaction records and budgets are stored exclusively on device in local SQLite Room database).
* **Files and Docs:**
  * **Collected?** **No** (Excel workbooks and backups are processed on-device; cloud backups are uploaded directly to the user's personal Google Drive without developer server intermediation).
* **Device or Other IDs:**
  * **Collected?** **No** (No advertising IDs, no device trackers).

---

## 3. Store Listing Copy (Teks Listing Toko)

### Bahasa Indonesia (Utama / Default)

#### Nama Aplikasi (Maks 30 Karakter)
```text
Mankeu
```

#### Deskripsi Singkat (Maks 80 Karakter)
```text
Kalkulator keuangan UMKM, analisis kelayakan BEP/HPP, dan pembukuan kas offline.
```

#### Deskripsi Lengkap (Maks 4000 Karakter)
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

### English (United States)

#### App Name (Max 30 Characters)
```text
Mankeu
```

#### Short Description (Max 80 Characters)
```text
MSME business finance, BEP/HPP/margin calculator, and offline cash flow.
```

#### Full Description (Max 4000 Characters)
```text
Mankeu is a modern agricultural finance and farm cost accounting tool engineered for farmers, agribusiness operators, agricultural cooperatives, and farm advisors.

Plan crop budgets, track real-world field expenses, and calculate enterprise feasibility metrics automatically, accurately, and 100% offline!

🌾 KEY FEATURES:

1. AUTOMATED FARM FEASIBILITY CALCULATOR
• Calculate Break-Even Points (BEP Revenue & BEP Yield Volume).
• Compute Cost of Goods Sold (HPP per kg/unit).
• Analyze Benefit-Cost Ratio (B/C Ratio) and Return-Cost Ratio (R/C Ratio).
• Evaluate net profit and margins before and after harvest.

2. ISOLATED DUAL LEDGERS (PROJECTION VS ACTUAL)
• Maintain an Actual Ledger (real expenses & harvest revenue) strictly separated from your Projection Ledger (budget/RAB).
• Instantly detect cost overruns by comparing actual expenditures against planned budgets.
• Farm-ready expense categories: Seeds, Fertilizers, Crop Protection (Pesticides), Labor, Machinery Rental, Land Rent, and Miscellaneous Operations.

3. SMART EXCEL SPREADSHEET IMPORT & EXPORT (.XLSX)
• Seamlessly import financial workbooks from Excel (.xlsx).
• Powered by an intelligent on-device Semantic Matcher tailored for agricultural operations.
• Export comprehensive financial statements to printable PDF and Excel formats.

4. 100% OFFLINE-FIRST & STRICT DATA PRIVACY
• All your agricultural records remain strictly on your local device (SQLite Room).
• Zero advertisements, zero third-party tracking, and zero telemetry. Works seamlessly in remote fields without internet access.

5. ENCRYPTED CLOUD BACKUP (GOOGLE DRIVE)
• Optional backup synchronization directly to your personal Google Drive (drive.appdata).
• Protected with military-grade client-side AES-256-GCM encryption using your custom passphrase.

🌱 PERFECT FOR ALL AGRICULTURAL ENTERPRISES:
• Food Crops (Paddy/Rice, Corn, Soybean, Cassava)
• Horticulture & Vegetables (Chili, Shallots, Tomato, Melon)
• Cash Crops & Plantations (Coffee, Cocoa, Oil Palm, Rubber)
• Agricultural Machinery Rentals, Farm Inputs, and Commodity Trading.

Take control of your farm finances with precision and ease using Mankeu!
```

---

## 4. Visual Store Assets Inventory (`docs/assets/store/`)

The following visual assets are prepared and cataloged in `docs/assets/store/`:

| Asset Name | Resolution / Format | Purpose | File Location |
| :--- | :--- | :--- | :--- |
| **App Icon** | 512 x 512 px PNG (32-bit) | Launcher & Google Play Store Icon | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` |
| **Feature Graphic** | 1024 x 500 px JPEG/PNG | Google Play Store Banner Header | `docs/assets/store/feature-graphic.png` |
| **Screenshot 01** | High-Res PNG Phone | Beranda & Ringkasan Keuangan Tani | `docs/assets/store/screenshot-01-beranda.png` |
| **Screenshot 02** | High-Res PNG Tablet/Phone | Analisis Kelayakan Usaha Tani (BEP/HPP/BCR) | `docs/assets/store/screenshot-02-kelayakan-bep-hpp.png` |
| **Screenshot 03** | High-Res PNG Phone | Impor Cerdas Spreadsheet Excel & Pencocok Semantik | `docs/assets/store/screenshot-03-impor-excel.png` |
| **Screenshot 04** | High-Res PNG Phone | Laporan Rincian Arus Kas & Biaya Operasional | `docs/assets/store/screenshot-04-laporan-keuangan.png` |
| **Screenshot 05** | High-Res PNG Tablet/Phone | Buku Kas Terisolasi (Realisasi vs Proyeksi Anggaran) | `docs/assets/store/screenshot-05-buku-kas-anggaran.png` |
| **Screenshot 06** | High-Res PNG Phone | Pengaturan Akun & Cadangan Terenkripsi AES-256-GCM | `docs/assets/store/screenshot-06-cadangan-terenkripsi.png` |

---

## 5. Contact & Support Information

* **Developer Name:** Mankeu
* **Support Email:** `adsurkasur.dev@gmail.com`
* **Website / Privacy Policy:** `https://adsurkasur.github.io/umkm-man-keu/privacy-policy.html`
* **Target Category:** Productivity / Business / Tools
* **Primary Tags:** Agriculture, Farm Finance, Calculator, Bookkeeping, Agribusiness, Feasibility