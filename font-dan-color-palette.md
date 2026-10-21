# Penggunaan Font dan Color Palette Arina Agri

Dokumen ini merangkum konfigurasi font dan palet warna aplikasi berdasarkan kode yang diperiksa pada 21 Oktober 2026. Nilai dalam `lib/theme.ts` menjadi sumber utama; dokumen ini perlu diperbarui bila konfigurasi tersebut berubah.

## 1. Karakter visual

Arina Agri menggunakan hijau sebagai warna utama, terakota sebagai aksen, serta warna netral yang hangat untuk latar dan teks. Tersedia dua tema: light dengan latar off-white dan dark bernuansa hijau gelap yang diberi nama **Slate Garden** pada kode theme.

## 2. Font

| Font | Penggunaan | Konfigurasi |
| --- | --- | --- |
| **Plus Jakarta Sans** | Font default untuk body, label, tombol, form, tabel, dan teks antarmuka yang tidak diberi override | `typography.fontFamily` pada theme; fallback `sans-serif` |
| **Sora** | Heading `h1–h6`, judul bagian, serta beberapa angka KPI | Heading pada theme dan override lokal melalui `var(--font-sora)`; fallback `sans-serif` pada theme |
| **Monospace sistem** | Beberapa kode batch, angka keuangan, dan teks teknis | `fontFamily: 'monospace'`; keluarga font aktual mengikuti perangkat/browser |

### Bobot heading default

| Variant | Font | Font weight |
| --- | --- | --- |
| `h1`, `h2` | Sora | `700` |
| `h3`, `h4`, `h5`, `h6` | Sora | `600` |

Sebagian komponen memberikan override bobot, misalnya `700` atau `800`. Tabel di atas menjelaskan default theme, bukan seluruh variasi lokal.

### Pemuatan font

Plus Jakarta Sans dan Sora dimuat melalui `next/font/google` di `app/layout.tsx`, menggunakan subset `latin` dan `display: 'swap'`. Konfigurasi tidak membatasi daftar weight secara eksplisit. Sora memakai `preload: false`.

| Font | CSS variable |
| --- | --- |
| Plus Jakarta Sans | `--font-plus-jakarta-sans` |
| Sora | `--font-sora` |

### Panduan penggunaan

- Gunakan `Typography` dan variant yang sesuai agar font mengikuti theme.
- Gunakan Sora untuk judul dan penekanan angka yang sudah mengikuti pola komponen terkait.
- Gunakan Plus Jakarta Sans untuk teks penjelasan dan kontrol antarmuka.
- Gunakan monospace pada data yang membutuhkan karakter berjarak tetap, seperti kode batch.
- Perubahan font default dilakukan melalui theme dan konfigurasi pemuatan font, kemudian periksa override lokal yang masih menyebut font lama.

## 3. Palet utama

Kolom fungsi berikut merupakan panduan penggunaan token. Kode hex mengikuti nilai eksplisit pada theme.

| Token | Fungsi | Light mode | Dark mode |
| --- | --- | --- | --- |
| `primary.main` | Aksi dan penekanan utama | `#2D6A4F` | `#52B788` |
| `secondary.main` | Aksen sekunder terakota | `#E07A5F` | `#D4836A` |
| `success.main` | Status berhasil atau positif | `#52B788` | `#68C99A` |
| `warning.main` | Peringatan atau perhatian | `#F4E285` | `#D4C060` |
| `error.main` | Kesalahan atau aksi destruktif | `#E07A5F` | `#D4836A` |
| `info.main` | Informasi pendukung | `#74A57F` | `#7DB88A` |

Secondary dan error menggunakan nilai `main` yang sama. Maknanya tetap dibedakan melalui token, konteks, label, dan ikon.

## 4. Warna netral dan permukaan

| Token | Penggunaan | Light mode | Dark mode |
| --- | --- | --- | --- |
| `background.default` | Latar halaman | `#FAFAF8` | `#1E2620` |
| `background.paper` | Permukaan kartu, panel, dan dialog | `#FFFFFF` | `#262D28` |
| `text.primary` | Teks utama | `#2C2A29` | `#DDE8DF` |
| `text.secondary` | Teks pendukung | `#6B6866` | `#8BA08E` |
| `divider` | Garis pembatas | `#EBEBE6` | `#2E3830` |

## 5. Variasi warna semantik

### Light mode

| Intent | Main | Light | Dark | Contrast text |
| --- | --- | --- | --- | --- |
| Primary | `#2D6A4F` | `#D8F3DC` | `#1B4332` | `#FFFFFF` |
| Secondary | `#E07A5F` | Otomatis MUI | Otomatis MUI | `#2C2A29` |
| Success | `#52B788` | `#D8F3DC` | `#2D6A4F` | `#FFFFFF` |
| Warning | `#F4E285` | `#FFF7D6` | `#B08C2C` | `#2C2A29` |
| Error | `#E07A5F` | `#FCDACF` | `#C85A3F` | `#2C2A29` |
| Info | `#74A57F` | `#E1EFE4` | `#4B7A56` | `#2C2A29` |

### Dark mode

| Intent | Main | Light | Dark | Contrast text |
| --- | --- | --- | --- | --- |
| Primary | `#52B788` | `#68C99A` | `#3A9A70` | `#1E2620` |
| Secondary | `#D4836A` | Otomatis MUI | Otomatis MUI | `#1E2620` |
| Success | `#68C99A` | `#2A3E2F` | `#52B788` | `#1E2620` |
| Warning | `#D4C060` | `#35300A` | `#A89030` | `#1A1A10` |
| Error | `#D4836A` | `#3A2018` | `#B05A42` | `#1E2620` |
| Info | `#7DB88A` | `#1E2E22` | `#5A9068` | `#1E2620` |

`secondary.light` dan `secondary.dark` tidak ditulis eksplisit pada konfigurasi proyek, sehingga dibentuk oleh MUI. Pada dark mode, nama `light` untuk warna status juga dipakai sebagai permukaan gelap lembut; jangan menganggapnya selalu lebih terang secara visual.

## 6. Warna interaksi

| Token | Light mode | Dark mode |
| --- | --- | --- |
| `action.hover` | `rgba(44, 42, 41, 0.04)` | `rgba(82, 183, 136, 0.08)` |
| `action.selected` | `rgba(45, 106, 79, 0.08)` | `rgba(82, 183, 136, 0.12)` |

## 7. Penerapan pada komponen

Gunakan token semantik, bukan menyalin kode hex ke setiap komponen. Dengan demikian, warna mengikuti tema aktif.

```tsx
<Typography variant="h5" color="text.primary">
  Ringkasan Stok
</Typography>

<Typography variant="body2" color="text.secondary">
  Informasi persediaan terbaru.
</Typography>

<Button variant="contained" color="primary">
  Simpan
</Button>

<Box sx={{ bgcolor: 'background.paper', border: 1, borderColor: 'divider' }}>
  Konten panel
</Box>
```

Untuk permukaan status lembut, gunakan helper yang sudah tersedia di `lib/themeColors.ts`:

| Helper | Perilaku |
| --- | --- |
| `softBg(theme, intent)` | Light: memakai `palette.light`. Dark: memakai `palette.main` dengan alpha default `0.14`. |
| `softHoverBg(theme, intent)` | Light: alpha `main` sebesar `0.22`. Dark: alpha `main` sebesar `0.20`. |
| `softText(theme, intent)` | Light: memakai `palette.dark`. Dark: memakai `palette.main`. |
| `softBorder(theme, intent)` | Alpha `main` sebesar `0.45` pada light dan `0.38` pada dark. |
| `accentText(theme, intent)` | Mengambil `palette.contrastText`. |

Token visual dasar tombol dan kartu tetap dikelola melalui `MuiButton.styleOverrides` dan `MuiCard.styleOverrides` di `lib/theme.ts`, sesuai aturan repository. Wrapper digunakan untuk perilaku tambahan.

Palet ini merangkum theme global. Beberapa ilustrasi dan komponen khusus masih memiliki warna lokal. Daftar warna ini juga tidak membuktikan bahwa seluruh kombinasi teks dan latar telah lulus uji kontras; keterbacaan perlu diperiksa pada penggunaan aktual, termasuk kedua tema.

## 8. Sumber kode

- [Theme dan typography](../../lib/theme.ts)
- [Pemuatan font](../../app/layout.tsx)
- [Helper warna](../../lib/themeColors.ts)
- [Provider MUI](../../components/shared/MuiProvider.tsx)

Dokumen ini tidak mengubah font, palet, atau tampilan aplikasi.
