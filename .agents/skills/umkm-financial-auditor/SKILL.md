---
name: umkm-financial-auditor
description: Audit codebase aplikasi keuangan UMKM terhadap standar MVP: BEP Unit & Rupiah, R/C Ratio, B/C Ratio, HPP dasar, pembersihan fitur pinjaman/bunga, dan penambahan manajemen stok minimal.
---

# Standar Evaluasi Finansial MVP UMKM

Gunakan tolok ukur formula dan prinsip berikut saat mengaudit logika bisnis dalam codebase:

## 1. Pemisahan Struktur Biaya & HPP
* Biaya Tetap (Fixed Cost / FC): Beban bulanan yang tidak terpengaruh volume penjualan (sewa tempat, gaji pokok, retribusi, listrik dasar).
* Biaya Variabel (Variable Cost / VC per Unit): Biaya langsung per unit produk (bahan baku, kemasan/packaging, upah borongan langsung, fee transaksi jika ada).
* HPP Satuan (COGS per Unit): Total Biaya Variabel per unit produk.

## 2. Titik Impas / Break Even Point (CVP Analysis)
* Margin Kontribusi Satuan (Unit Contribution Margin):
  CM = P - VC
  (P = Harga Jual per Unit, VC = Biaya Variabel per Unit)
* Rasio Margin Kontribusi (Contribution Margin Ratio):
  CMR = CM / P = (P - VC) / P
* BEP dalam Satuan Unit:
  BEP_unit = Total FC / CM = Total FC / (P - VC)
* BEP dalam Nilai Rupiah / Omzet:
  BEP_rupiah = Total FC / CMR = BEP_unit * P

## 3. Rasio Kelayakan & Efisiensi Usaha UMKM
* Revenue-Cost Ratio (R/C Ratio):
  R/C = Total Revenue (TR) / Total Cost (TC)
  - R/C > 1: Usaha efisien dan menguntungkan.
  - R/C = 1: Impas.
  - R/C < 1: Usaha merugi / tidak efisien.
* Benefit-Cost Ratio (B/C Ratio):
  B/C = Keuntungan Bersih (Net Profit) / Total Cost (TC) = (TR - TC) / TC = (R/C) - 1
  - B/C > 0: Layak dan memberikan nilai tambah laba terhadap modal yang dikeluarkan.

## 4. Standar Manajemen Persediaan (Stok) Minimal UMKM
* Formula Alur Stok:
  Stok Akhir = Stok Awal + Barang Masuk - Barang Keluar
* Fitur Minimal:
  1. Master barang (nama, satuan: pcs/cup/kg, harga beli dasar).
  2. Kuantitas stok terkini (Current Stock).
  3. Peringatan stok minimum (Low Stock Alert).
  4. Penyesuaian stok opname manual.

## 5. Aturan Eliminasi Fitur (Fitur Tidak Relevan):
* HAPUS: Fitur pinjaman uang (loan amount) dan kalkulasi bunga (interest rate / basis points). Fitur ini tidak relevan untuk pencatatan operasional harian UMKM mikro dan membebani domain model.