# Variasi dark mode yang selaras dengan light Arina

Tanggal: 25 Oktober 2026. Status: **proposed**, belum diterapkan pada Kotlin/APK.

**user-confirmed:** berikan variasi warna pada dark mode dan sesuaikan dengan light mode. Hijau Arina tetap menjadi identitas, sesuai arahan sebelumnya. Rancangan ini melengkapi [Forest Charcoal](../2026-10-25-arina-green-dark/README.md) dengan keluarga warna pendukung yang terlihat berpasangan antar tema.

## Dasar yang diperiksa dari source

**Verified:** `MetricColors.kt` light menyediakan empat keluarga: kas biru, profit lavender, income mint, expense sand. `Theme.kt` light mempunyai secondary terakota dengan container `#FCDACF`/foreground `#713A28`, dan chart mempunyai teal `#326B68`. Primary light tetap `#2D6A4F`, background `#FAFAF8`, serta text `#2C2A29`.

**Proposed:** pertahankan keluarga dan makna tersebut saat berpindah ke dark. Container pastel terang menjadi container gelap bertint, sedangkan foreground menjadi lebih terang. Tidak membalik warna secara mekanis atau membuat identitas warna baru untuk tiap tema. Warna mint dark existing sudah sesuai dan dapat dipakai kembali.

## Preview pasangan light dan dark

![Pasangan keluarga warna light dan dark](light-dark-preview.png)

Ini diagram usulan palet, bukan screenshot aplikasi. Kolom light memakai pasangan existing kecuali container teal yang baru diusulkan dari accent chart existing. Seluruh pasangan dark berstatus proposed untuk rancangan ini; pasangan mint kebetulan sama dengan implementasi existing.

| Keluarga | Light foreground / container | Dark foreground / container | Peran |
| --- | --- | --- | --- |
| Hijau–mint | `#2D6A4F` / `#EAF6EF` | `#A9D8BC` / `#1C2E24` | Identitas dan pemasukan |
| Terakota–peach | `#713A28` / `#FCDACF` | `#F0B49B` / `#36251F` | Aksen sekunder brand |
| Biru–sky | `#315D90` / `#EDF4FF` | `#A8CBE8` / `#1A2735` | Kas dan informasi data |
| Lavender–lilac | `#66508D` / `#F2EEFF` | `#C7B6E8` / `#2B2437` | Hasil usaha dan analisis |
| Sand–oat | `#76603F` / `#F5F0E6` | `#DEC9A3` / `#302A20` | Pengeluaran normal |
| Teal–seafoam | `#326B68` / `#E6F3F1` | `#9AD9D2` / `#19312F` | Stok dan kategori grafik |

Foreground terakota light adalah warna teks di atas container, bukan secondary main brand `#E07A5F`. Perbedaan itu disengaja untuk keterbacaan; keduanya berasal dari keluarga sama.

## Komposisi yang disarankan

- Fondasi dark tetap Forest Charcoal: halaman `#101613`, card netral `#19211C`, dialog `#243027`, teks utama `#F1F5F2`, teks sekunder `#AAB8AF`.
- Hijau brand `#52B788` tetap menjadi identitas. Tombol primer memakai green fill `#357956` dengan teks putih; bukan diganti cyan/plum.
- Gunakan container berwarna pada ringkasan terpilih: kas biru, hasil usaha lavender, income mint, pengeluaran sand. Tidak perlu mewarnai seluruh formulir, tabel dan dialog.
- Terakota dapat menandai unsur brand sekunder atau ikon editorial; tidak otomatis menjadi badge status. Warning tetap amber, error/rugi tetap danger, dengan label/ikon. Jangan mewarnai seluruh pengeluaran merah.
- Teal dipakai bila bagian Stok/grafik membutuhkan keluarga tersendiri. Peringatan menipis/habis tetap warning/danger agar tidak hilang di antara variasi.
- Nilai rupiah utama tetap menggunakan teks utama pada card; foreground accent dipakai pada label/ikon. Kerugian ditampilkan berdasarkan data, bukan berdasarkan keluarga warna card.
- Warna kategori grafik mengikuti identitas kategori secara stabil. Maksud warna sama di light/dark, sementara intensitas menyesuaikan latar.

Hasil yang dituju: dark tetap hijau Arina, tetapi card tidak didominasi satu hue. Pengguna mengenali bagian yang sama dari keluarga warnanya ketika berganti tema.

## Referensi dan batas sesi

Riset browser sebelumnya tetap menjadi dasar: [Supabase](https://supabase.com/design-system/docs/color-usage#brand-green) untuk pemisahan ink/fill/brand dan [MongoDB](https://www.mongodb.com/) untuk identitas hijau di fondasi gelap. Pada sesi ini tidak diperlukan browser ulang: keputusan yang belum terjawab adalah pencocokan dengan token light aktual, sehingga source native digunakan sebagai bukti utama. Tidak menyalin hue brand produk lain.

Jalankan `rtk proxy python scripts/qa/verify_light_dark_palette_alignment.py`. Verifier memeriksa 12 pasangan accent/container, 12 pasangan body/container, enam pasangan teks fondasi dark serta satu pasangan teks tombol: total 31 pasangan solid memenuhi minimal 4.5:1. Ia juga memeriksa delapan nilai light metric terhadap source, pasangan terakota dan accent teal light terhadap `Theme.kt`, serta keberadaan preview PNG.

Kontras statis tidak membuktikan aksesibilitas keseluruhan, focus/disabled/pressed/alpha states, color vision deficiency, chart differentiation atau keterbacaan pada perangkat. Tidak ada perubahan kode aplikasi, build atau pengujian workflow. Implementasi nanti tetap dilakukan pada pemilik token `Theme.kt` dan `MetricColors.kt`, dengan regresi light dan verifikasi dark pada layar nyata. Tidak ada dependency, schema atau data yang berubah.
