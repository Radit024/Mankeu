# Katalog Diagram Sistem Mankeu

Folder ini memuat seluruh diagram arsitektur, basis data, alur pengguna, proses bisnis, dan siklus hidup sistem untuk aplikasi **Mankeu (Manajemen Keuangan UMKM)**. 

Semua diagram ditulis menggunakan **Mermaid Markdown** standar industri sehingga:
- Bersifat **teks-murni (*plain text*)** dan tersimpan rapi di Git.
- **Mudah diedit** oleh developer atau asisten AI kapan saja saat aplikasi berkembang.
- Dapat dirender langsung secara otomatis di GitHub, VS Code, Android Studio, Notion, Obsidian, atau browser.

---

## 📂 Daftar Diagram Tersedia

| No | Dokumen Diagram | Deskripsi & Cakupan | Tipe Diagram |
| :---: | :--- | :--- | :---: |
| 1 | [01_entity_relationship_diagram.md](01_entity_relationship_diagram.md) | Skema lengkap basis data Room SQLite **Schema v4** (14 entitas, foreign keys, cascade rules, dan aturan migrasi tanpa kehilangan data). | **ERD** |
| 2 | [02_architecture_diagram.md](02_architecture_diagram.md) | Arsitektur berlapis (*Clean Layered Architecture*): Presentation (Compose, Vico), State (ViewModel), Pure Kotlin Domain Calculators, Room DAO Data, dan Subsistem Cadangan & Impor. | **System Architecture** |
| 3 | [03_use_case_diagram.md](03_use_case_diagram.md) | Interaksi aktor (Pemilik Usaha, Kasir, Excel, Google Drive) dengan 6 modul use case utama. | **UML Use Case** |
| 4 | [04_class_diagram.md](04_class_diagram.md) | Struktur kelas domain (`MoneyEvent`, `JournalEntry`, `Obligation`, dll.), kalkulator bisnis, repository, dan interface DAO. | **UML Class** |
| 5 | [05_user_flow_diagram.md](05_user_flow_diagram.md) | Perjalanan navigasi pengguna: onboarding usaha, peralihan mode Proyeksi vs Realisasi, pencatatan kas 5-tap, eksplorasi laporan, hingga impor berkas. | **User Flow** |
| 6 | [06_activity_diagrams.md](06_activity_diagrams.md) | Logika eksekusi 4 proses krusial: Penegakan Double-Entry, Reversal Guard, Impor Excel Multi-sheet, dan Enkripsi Cadangan AES-GCM. | **UML Activity** |
| 7 | [07_state_diagrams.md](07_state_diagrams.md) | Mesin status (*State Machines*) untuk siklus hidup Piutang/Utang, Transaksi Keuangan, Batch Impor, dan Mode Buku. | **UML State** |
| 8 | [08_sequence_diagram.md](08_sequence_diagram.md) | Interaksi urutan waktu (timeline) saat pencatatan transaksi dari Compose UI, ViewModel, Repository, hingga SQLite Transaction Commit. | **UML Sequence** |

---

## 🌐 Cara Melihat & Mengedit Diagram

### 1. Pratinjau Interaktif Web (HTML Dashboard)
Tersedia berkas `preview.html` yang merender semua diagram secara langsung di peramban web (*browser*) dengan fitur zoom, dark/light mode otomatis, dan navigasi cepat.

Untuk membuka pratinjau:
- Cukup klik ganda berkas `docs/diagrams/preview.html` atau buka di browser pilihan Anda.

Untuk memperbarui berkas HTML setelah mengedit markdown diagram:
```bash
py docs/diagrams/generate_html_preview.py
```

### 2. Melalui VS Code / Android Studio
- **VS Code**: Pasang ekstensi **Markdown Preview Mermaid Support** atau **Mermaid Chart**. Buka berkas `.md` lalu tekan `Ctrl + Shift + V` untuk melihat pratinjau langsung.
- **Android Studio / IntelliJ**: Pasang plugin **Mermaid** dari Marketplace. Pratinjau akan langsung aktif di tab *Editor and Preview*.

### 3. Melalui Mermaid Live Editor
Salin kode di dalam blok ` ```mermaid ` ke [mermaid.live](https://mermaid.live/) untuk mengedit diagram secara visual, menggeser node, atau mengekspornya ke format PNG/SVG berkualitas tinggi.

---

## 🔒 Aturan Pemeliharaan (*Maintenance Invariants*)

1. **Sinkronisasi Skema Basis Data**:
   - Jika ada perubahan pada `Entities.kt` atau migrasi Room baru (misal Schema v5), perbarui `01_entity_relationship_diagram.md` dan `04_class_diagram.md`.
2. **Kekekalan Integritas Double-Entry**:
   - Setiap alur pencatatan transaksi di `06_activity_diagrams.md` dan `08_sequence_diagram.md` wajib mempertahankan prinsip keseimbangan debit-kredit ($\sum \text{Debit} == \sum \text{Credit}$).
3. **Pemisahan Mode Buku**:
   - Diagram harus selalu merefleksikan isolasi tegas antara buku `PROJECTION` (Rencana/Simulasi) dan `REALIZATION` (Transaksi Nyata).
