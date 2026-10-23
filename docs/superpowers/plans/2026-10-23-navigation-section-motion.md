# Animasi Navigasi dan Section — Implementation Plan

> **For agentic workers:** Gunakan skill `executing-plans` untuk mengimplementasikan task secara berurutan dengan checkpoint verifikasi. Semua checklist masih berupa pekerjaan yang diusulkan. Dokumen ini tidak menyatakan animasi sudah diimplementasikan atau diuji di perangkat.

**Goal:** Membuat perpindahan tab, mode buku, unit usaha, overlay, form, dan status proses terasa halus tanpa mengaburkan data keuangan atau menghilangkan keadaan pengguna.

**Architecture:** Extend Compose Animation, `MankeuMotion`, dan `LocalMotion` yang sudah tersedia. Pemilihan pola dan preferensi gerak dimiliki presentation; screen tetap memiliki state, daftar, serta aksi masing-masing. Transisi mengonsumsi state yang konsisten; lifecycle animasi tidak menjalankan operasi bisnis.

**Tech Stack:** Kotlin, Jetpack Compose Animation dan Material 3, Lottie Compose lokal yang sudah terpasang, JUnit, Compose instrumentation tests.

## Status, sumber, dan batas pengerjaan

- **user-confirmed:** Pengguna meminta identifikasi animasi dan referensi melalui browser, kemudian meminta implementation plan. Implementasi aplikasi belum diminta pada tahap ini.
- **verified:** Working tree pada 23 Oktober 2026 memiliki perubahan lokal pada sembilan file Kotlin dan tiga log. Baseline rencana adalah working tree tersebut, bukan hanya HEAD. Jangan menimpa, mereset, atau memasukkan perubahan itu ke commit baru secara massal.
- **verified:** `MankeuApp.kt` memiliki `AnimatedContent` untuk overlay dan pasangan destination/book mode. `HomeScreen.kt` memiliki animasi kartu saldo yang juga bereaksi terhadap mode/proyek. `ReportsScreen.kt` memiliki animasi isi tab.
- **verified:** `AccountSettingsScreen.kt` mengganti isi tab dengan kondisi `if`; `HelpUi.kt` mengganti daftar/artikel melalui early return; `MoreScreen.kt` mengganti status impor melalui `when`.
- **verified:** `MankeuMotion` memiliki Full 180/320 ms, Reduced 90/140 ms, dan Off 0/0 ms. `expressive` hanya aktif pada Full. Transisi Laporan belum memakai guard `expressive`; beberapa transisi memakai `maxOf(duration, 1)`.
- **inferred:** Kombinasi transisi horizontal layar dan vertikal kartu saldo dapat terasa bertumpuk. Ini temuan struktur kode, bukan pengukuran perceptual atau jank.
- **proposed:** Pakai fade-through untuk tujuan utama, shared-axis X pendek untuk tab terkait/mode buku, shared-axis Z untuk overlay, expand/fade untuk detail form, dan item placement untuk perubahan daftar.
- **unknown:** Frame timing, stabilitas scroll/draft selama interupsi, dan hasil visual pada perangkat pengguna sampai acceptance checks dijalankan.

Sumber yang telah dibuka melalui browser saat audit:

1. [Material Motion / Reply](https://developer.android.com/codelabs/material-motion-android): fade-through, shared-axis, dan container transform. Ini referensi pola dari Views/Fragments; jangan menyalin kelas transisinya ke Compose.
2. [Compose Animation Quick Guide](https://developer.android.com/develop/ui/compose/animation/quick-guide): AnimatedContent, AnimatedVisibility, animateContentSize, dan pertimbangan performa.
3. [Compose lazy-list item animations](https://developer.android.com/develop/ui/compose/lists#item-animations): animateItem dan stable keys.

UIZZE tidak menghasilkan kandidat pada pencarian alur keuangan saat audit; tidak dipakai sebagai bukti perilaku animasi.

## Pilihan pendekatan

1. **Direkomendasikan — extend sistem Compose yang ada.** Tambahkan helper transisi kecil untuk pola yang benar-benar dipakai berulang, lalu integrasikan pada pemilik kontennya. Tanpa library, aset, atau navigasi baru.
2. Duplikasi transitionSpec di semua screen: perubahan awal mudah, tetapi Reduced/Off dan durasi mudah kembali berbeda. Tidak dipilih.
3. Migrasi seluruh navigasi atau memakai container transform di semua alur: biaya migrasi/state restoration tidak sebanding dengan kebutuhan. Tidak dipilih.

Tidak mengubah warna, teks, susunan section, formula, schema Room, format Excel, login opsional, atau akses offline. Lottie tetap untuk ilustrasi kontekstual; bukan mesin pergantian halaman. Animasi tema global dan rekreasi Activity saat ganti bahasa tidak dirombak.

## Kontrak motion yang diusulkan

| Interaksi | Full | Reduced | Off | Pemilik |
| --- | --- | --- | --- | --- |
| Tab utama | Fade-through: keluar 90 ms, masuk 180 ms setelah fase keluar, total 270 ms; tanpa spatial travel | Fade keluar/masuk berurutan, total maksimal 140 ms | Langsung | ScreenContent |
| Rencana/Realisasi | Shared-axis X + fade, perjalanan 24 dp, masuk 320 ms/keluar 180 ms | Fade maksimal 140 ms, tanpa slide | Langsung | ScreenContent |
| Tab Pengaturan/Laporan | Shared-axis X + fade, perjalanan 16 dp mengikuti urutan tab; 320/180 ms | Fade maksimal 140 ms | Langsung | Isi tab screen |
| Usaha/unit aktif | Fade-through isi yang berubah, maksimal 270 ms; header konteks tetap jelas | Fade maksimal 140 ms | Langsung | Batas scope screen |
| Overlay Data/Pengaturan/Bantuan | Pertahankan scale + fade, 320/180 ms; reverse saat kembali | Fade maksimal 140 ms | Langsung | MankeuApp |
| Daftar Bantuan/artikel | Shared-axis X 24 dp + fade, arah terbalik saat kembali | Fade maksimal 140 ms | Langsung | HelpCenter |
| Form tambahan | Expand/shrink + fade 180 ms | Fade maksimal 90 ms, tinggi berubah langsung | Langsung | Pemilik field |
| Item daftar berubah | Fade masuk 180 ms/keluar 90 ms; placement 180 ms, tanpa bounce | Fade saja maksimal 90 ms; tanpa placement | Langsung | Lazy item |
| Status impor / loading / empty / welcome | Fade antarstate 180 ms; perubahan tinggi hanya untuk kartu terbatas | Fade maksimal 90 ms | Langsung | Pemilik status |

Angka ini **proposed**, bukan hasil benchmark. Reuse token fast/medium; turunkan fase fade-through dari fast dan batasi Reduced ke medium-nya. Shared-axis menghormati layout direction, termasuk RTL. Gunakan easing `FastOutSlowInEasing` yang sudah dipakai; tidak menambahkan spring bouncy.

Aturan lintas layar:

- Satu animasi perpindahan dominan per interaksi. Saat seluruh layar berganti mode/scope, kartu saldo tidak menjalankan slide tambahan.
- Label, angka, daftar, dan aksi harus merujuk scope/book yang sama. Jangan membuat angka berjalan dari nominal lama menuju nominal baru.
- Jangan menambahkan delay untuk menunggu animasi sebelum navigasi, load, save, commit, cancel, atau error. Interupsi langsung menargetkan state terbaru.
- Konten keluar tidak boleh menerima klik atau fokus TalkBack; pada pertukaran dua state, hanya target aktif yang menawarkan aksi. Pertahankan semantik konten target tanpa duplikasi label/CTA yang membingungkan.
- Off menggunakan durasi nol atau EnterTransition.None/ExitTransition.None; jangan mengubah nol menjadi 1 ms. Reduce menghapus slide, scale, expansion dan placement, bukan hanya mempercepatnya.
- Stable identity berdasarkan destination, scope, mode, tab, atau jenis status. Jangan memakai seluruh AppUiState, nominal, query setiap ketikan, atau object list sebagai animation key.
- Loading awal bukan empty. Jangan mengulang animasi masuk saat scroll, recomposition, atau emission data yang tidak mengubah konteks.
- Pertahankan scroll/draft per konteks yang sesuai. Draft transaksi milik unit A tidak boleh tersimpan ke unit B setelah scope berganti.

## Peta file dan ownership

Semua path relatif di bawah berakar pada `D:\Arina Agri\Arina Agri Kotlin`.

| File | Tanggung jawab perubahan |
| --- | --- |
| `app/src/main/java/com/mankeu/presentation/Theme.kt` | Tetap memiliki token dan resolusi Full/Reduced/Off; extend hanya jika kontrak membutuhkan |
| `app/src/main/java/com/mankeu/presentation/components/ContentTransitions.kt` (baru) | Helper ContentTransform berulang: fade-through dan shared-axis X; tanpa repository, ViewModel, state registry, atau service |
| `app/src/main/java/com/mankeu/presentation/MankeuApp.kt` | Navigasi, mode, scope boundary, overlay, dan welcome/content boundary |
| `app/src/main/java/com/mankeu/presentation/screens/HomeScreen.kt` | Kartu saldo serta loading/empty/content Beranda |
| `app/src/main/java/com/mankeu/presentation/screens/ReportsScreen.kt` | Isi tab, scroll per tab, Reduced/Off dan empty/content |
| `app/src/main/java/com/mankeu/presentation/screens/AccountSettingsScreen.kt` | Isi tab dan penyimpanan draft profil/scroll |
| `app/src/main/java/com/mankeu/presentation/guide/HelpUi.kt` | Daftar/artikel Bantuan, kembali, query dan scroll |
| `app/src/main/java/com/mankeu/presentation/screens/EntryScreen.kt` | Field kondisional, daftar, first-entry dan scope draft |
| `app/src/main/java/com/mankeu/presentation/screens/PlanScreen.kt` | Field kondisional, daftar produk/biaya dan empty/content |
| `app/src/main/java/com/mankeu/presentation/screens/StockSection.kt` | Item placement dan empty/content stok |
| `app/src/main/java/com/mankeu/presentation/screens/MoreScreen.kt` | Isi status impor serta transfer/backup yang sudah memiliki status |
| `app/src/main/java/com/mankeu/presentation/components/MankeuSheet.kt` | Guard preferensi pada perubahan tinggi isi; motion pembukaan sheet tetap dimiliki Material 3 |
| `app/src/main/java/com/mankeu/presentation/components/RecordFab.kt` | Audit Reduced/Off dan hindari animasi ganda dengan perpindahan layar |
| `app/src/androidTest/java/com/mankeu/presentation/NavigationMotionTest.kt` (baru) | Workflow navigasi, interupsi, scope/mode dan state restoration |
| `app/src/androidTest/java/com/mankeu/presentation/SectionMotionTest.kt` (baru) | Tab/settings/help/forms/list/status dan semantik |
| `app/src/androidTest/java/com/mankeu/presentation/VisualSystemTest.kt` | Extend bukti resolve motion dan regresi tema/draft |
| `docs/UX_AND_ACCESSIBILITY.md` | Kontrak motion final yang benar-benar diterapkan |
| `docs/qa/2026-10-23-navigation-section-motion.md` (baru saat eksekusi) | Perangkat, skenario, hasil, link bukti, dan keterbatasan |
| `logs/ACTIVITY_LOG.md`, `logs/DECISION_LOG.md`, `logs/CHANGELOG.md` | Aktivitas, keputusan material saat diadopsi, perubahan UI saat diimplementasikan |

`AppViewModel.kt` hanya boleh disentuh jika Task 2 membuktikan emission scope/data tidak koheren; animasi tidak boleh dipakai untuk menyembunyikan mismatch. Jika perlu, perbaiki agregasi state secara terarah beserta regression test sebelum melanjutkan animasi scope.

## Task 1 — Kunci baseline dan preferensi gerak

**Files:** Theme.kt, ContentTransitions.kt, VisualSystemTest.kt, NavigationMotionTest.kt.

- [ ] Baca ulang README, PROJECT_GUIDE, PRODUCT_REQUIREMENTS, DOMAIN_MODEL, QUALITY_GATES, UX_AND_ACCESSIBILITY; gunakan AGENTS/ADR terkini saat dokumen lama berbeda. Catat Git status sebelum mulai.
- [ ] Rekam baseline tab utama, mode buku, tab Laporan/Pengaturan, overlay, dan Bantuan menggunakan data sintetis; catat Android/API, viewport, theme, font scale, dan motion mode.
- [ ] Tambahkan test resolusi motion pada VisualSystemTest, di theme fixture yang sudah ada:

```kotlin
@Test fun motionPreferenceRespectsSystemScale() {
    assertEquals(MankeuMotion.Off, MankeuMotion.resolve(MotionMode.FULL, 0f))
    assertEquals(MankeuMotion.Reduced, MankeuMotion.resolve(MotionMode.FULL, 0.5f))
    assertEquals(MankeuMotion.Off, MankeuMotion.resolve(MotionMode.OFF, 1f))
    assertFalse(MankeuMotion.Reduced.expressive)
    assertFalse(MankeuMotion.Off.expressive)
}
```

- [ ] Tambahkan fixture Compose yang memanggil helper transisi baru dengan dua target berlabel berbeda. Gunakan `mainClock.autoAdvance = false`; periksa target akhir, node keluar yang tidak actionable, dan interupsi A→B→A. Test harus gagal sebelum helper ada.
- [ ] Implementasikan helper berbasis `ContentTransform` untuk dua pola yang dipakai bersama. Fade-through memakai fase berurutan; shared-axis menerima arah dan offset piksel dari caller. Off langsung, Reduced tanpa displacement/scale. Helper tidak menyimpan data atau menjalankan coroutine bisnis.
- [ ] Jalankan focused instrumentation setelah device siap; periksa frame awal/tengah/akhir memakai Compose clock dan captureToImage. Jangan mengandalkan assert target akhir untuk membuktikan absennya slide.

**Acceptance:** semua preferensi memiliki bukti rendered-state; status lama tidak menghalangi input target dan Off tidak menyisakan konten keluar setelah idle.

## Task 2 — Benahi konsistensi state sebelum animasi scope/mode

**Files:** MankeuApp.kt, HomeScreen.kt, NavigationMotionTest.kt; AppViewModel.kt hanya bersyarat di atas.

- [ ] Tambahkan regression fixture dua unit sintetis dengan nominal berbeda, misalnya A Rp125.000 dan B Rp780.000; proyeksi dan realisasi juga memiliki nominal berbeda.
- [ ] Jalankan mode-switch dengan clock tertahan. Buktikan label dan angka setiap konten konsisten; cabang Catat harus memakai mode milik target AnimatedContent, bukan membaca mode terbaru di luar target untuk konten yang sedang keluar.
- [ ] Trace `selectBusiness`, `selectProject`, `selectBookMode`, selectedBookId, flatMapLatest, dan combine uiState. Uji bahwa pergantian scope cepat tidak menghasilkan label B dengan data A. `isLoading` saat ini hanya mewakili initial load; jangan menggunakannya sebagai bukti readiness setiap pergantian scope.
- [ ] Jika mismatch terbukti, perbaiki pemilik state lebih dahulu dengan scope-tagged aggregation; jangan copy selected ID ke snapshot yang masih membawa isi scope lama. Bila belum terbukti, jangan menambahkan model loading atau cache baru.
- [ ] Simpan outgoing presentation snapshot yang sudah koheren hanya sepanjang transisi bila diperlukan. Aksi hanya menggunakan target aktif; animasi tidak memicu save atau memilih ulang unit.
- [ ] Hilangkan slide internal kartu saldo saat parent sedang berganti mode/scope. Terapkan fade-through tab utama dan shared-axis X mode buku; scope switch fade isi tanpa menggerakkan header/navigation bar.
- [ ] Periksa tap cepat HOME→ENTRY→STOCK→REPORTS, mode bolak-balik, dan scope A→B→A sebelum animasi selesai. Target terakhir harus menang; tidak ada klik/penyimpanan ke konteks lama.

**Acceptance:** tidak ada mismatch label/nominal/scope, operasi ganda, atau slide ganda. Draft tidak berpindah pemilik scope. Nominal tetap Long dan langsung memakai hasil valid, tanpa interpolasi angka.

## Task 3 — Tab Laporan dan Pengaturan

**Files:** ReportsScreen.kt, AccountSettingsScreen.kt, SectionMotionTest.kt.

- [ ] Buat regression scenario: ubah draft profil, pindah ke Umum lalu kembali; scroll tab Laporan, pindah tab lalu kembali. Catat kegagalan awal sebelum refactor.
- [ ] Angkat state draft yang perlu bertahan ke pemilik di luar subtree transisi, atau gunakan `rememberSaveableStateHolder` dengan key SettingsTab/ReportSection; jangan menyimpan satu state scroll untuk semua tab.
- [ ] Pisahkan header/tab bar dari badan section. Gunakan helper shared-axis X yang sama untuk perpindahan isi, direction berdasarkan indeks tab, offset 16 dp.
- [ ] Pertahankan virtualisasi daftar panjang. Jangan menggabungkan seluruh laporan yang dapat membesar menjadi satu eager Column hanya untuk membungkus animasi; screen mengatur transition boundary dan LazyColumn per tab jika diperlukan.
- [ ] Pertahankan target tur dan auto-select tab dari activeTourStep. Tunggu layout target stabil sebelum spotlight berpindah; jangan menambahkan fixed delay.
- [ ] Jalankan setiap tab pada Full/Reduced/Off, lalu tes pergantian theme dan 130% font scale saat draft belum disimpan.

**Acceptance:** isi berganti mengikuti arah tab, Reduced tanpa slide, Off langsung; draft dan scroll dipulihkan; kontrol tab tetap stabil dan tidak mengubah perilaku simpan.

## Task 4 — Overlay dan Bantuan

**Files:** MankeuApp.kt, HelpUi.kt, SectionMotionTest.kt.

- [ ] Tambahkan skenario buka Data/Pengaturan/Bantuan lalu Back; buktikan destination dasar dan scroll kembali seperti sebelumnya.
- [ ] Reuse shared-axis Z overlay yang sudah ada; samakan guard Full/Reduced/Off. Jangan menambah transisi nested pada screen hanya karena overlay terbuka.
- [ ] Tempatkan transisi daftar/artikel di HelpCenter sebelum percabangan early return. State query dan scroll daftar berada di luar konten artikel yang keluar/masuk.
- [ ] Gunakan shared-axis X 24 dp, forward saat buka artikel, backward saat kembali; perpindahan artikel terkait memakai fade jika tidak memiliki urutan hierarki.
- [ ] Uji search→buka artikel→Back, related article→Back sesuai perilaku back existing, serta “Tunjukkan di aplikasi”. Fokus kembali ke pemicu; spotlight tetap mencapai target.

**Acceptance:** tidak ada kilatan background kosong, query hilang, overlay berlapis tanpa sengaja, atau fokus tertinggal di konten yang ditutup.

## Task 5 — Form, sheet, dan FAB

**Files:** EntryScreen.kt, PlanScreen.kt, MankeuSheet.kt, RecordFab.kt, SectionMotionTest.kt.

- [ ] Tambahkan skenario kategori baru, detail lanjutan, tipe biaya/aset, field umur ekonomis, validasi gagal, dan keyboard terbuka. Isi field sebelum toggle untuk menguji preservation.
- [ ] Reuse AnimatedVisibility pada kelompok field kondisional; Full expand/fade, Reduced fade tanpa expansion, Off langsung. Hindari animateContentSize parent pada subtree yang sedang di-expand oleh anak.
- [ ] Batasi animateContentSize di MankeuSheet ke perubahan konten yang memang membutuhkan resize. Material3 tetap memiliki animasi pembukaan/penutupan sheet dan IME inset.
- [ ] Audit FAB: rotasi/scale hanya pada Full, fade pada Reduced, langsung pada Off; pemilihan aksi menutup speed dial sekali dan membuka satu form.
- [ ] Field error tetap mengambil fokus dan terlihat di atas keyboard. Jangan menunda pesan validasi demi exit animation.

**Acceptance:** draft tetap benar setelah toggle, tidak ada keyboard jump/field tertutup, satu tap menghasilkan satu aksi, dan sheet tetap dapat dibatalkan melalui Back/scrim sesuai perilaku existing.

## Task 6 — Daftar dan pergantian empty/content

**Files:** HomeScreen.kt, EntryScreen.kt, PlanScreen.kt, StockSection.kt, ReportsScreen.kt, SectionMotionTest.kt.

- [ ] Identifikasi daftar yang dapat berubah karena save, reverse, archive, delete, filter atau mutasi stok. Pertahankan key entity ID dan LazyColumn; jangan memakai indeks sebagai key.
- [ ] Terapkan animateItem pada root item untuk perubahan membership/urutan. Gunakan 180/90 ms fade dan placement tween 180 ms pada Full; placement null pada Reduced; lepas modifier pada Off.
- [ ] Jangan menganimasikan seluruh hasil pencarian tiap ketikan. Gunakan placement hanya bila tidak membuat hasil sulit diikuti; hasil kosong memakai boundary empty/content yang stabil.
- [ ] Tambahkan fade saat catatan/stok/produk pertama tersimpan dan saat item terakhir benar-benar hilang. Header serta CTA tidak boleh berganda; jangan replay Lottie hanya karena item kembali masuk viewport.
- [ ] Test tambah pertama→isi tampil, tambah kedua→posisi stabil, reverse/archive/delete sesuai aksi yang tersedia→hasil/notice benar, filter tanpa hasil→clear filter. Gunakan record sintetis.

**Acceptance:** tidak ada scroll lompat ke atas tanpa niat pengguna, item melompat identitas, data menghilang sementara sebagai empty palsu, atau perubahan semantics yang menyulitkan TalkBack.

## Task 7 — Status impor, transfer, dan awal aplikasi

**Files:** MoreScreen.kt, MankeuApp.kt, HomeScreen.kt, SectionMotionTest.kt, WelcomeScreenTest.kt.

- [ ] Baca REFERENCE_WORKBOOK_CONTRACT dan IMPORT_RECONCILIATION_AND_NOTIFICATIONS sebelum menyentuh alur impor. Rules DEC-012 dalam AGENTS adalah batas wajib jika panduan lama berbeda.
- [ ] Kelompokkan tampilan berdasarkan jenis Idle/Loading/Ready/Committed/Failure; transisi dipicu perubahan jenis, bukan setiap pembaruan progress/object preview.
- [ ] Bungkus badan ImportCard dengan AnimatedContent. Resize hanya untuk kartu kecil; preview panjang tetap lazy/scrollable dan tidak diukur ulang setiap frame.
- [ ] Snapshot tampilan keluar hanya untuk render. Tombol commit/cancel/retry dimiliki state aktif; konten Ready yang sedang keluar tidak dapat mengirim commit kedua.
- [ ] Terapkan fade serupa pada status transfer/backup yang sudah ada; jangan mengubah layanan backup, jaringan, format berkas, atau menambah progress fiktif.
- [ ] Terapkan fade Welcome→shell serta loading→content pada pemilik boundary. Jangan menunggu animasi sebelum menyelesaikan welcome atau menampilkan database yang siap; lifecycle return tidak mengulang welcome.
- [ ] Uji import valid→preview→commit sekali, duplikat→warning tetap terlihat, malformed→failure lengkap, cancel saat loading, retry, dan kegagalan commit tanpa indikator sukses palsu. Domain import harus tetap atomic/reversible.

**Acceptance:** semua status tetap benar dan actionable; animasi tidak menjadi pemicu I/O, tidak mengubah hasil impor, dan tidak menampilkan sukses sebelum hasil operasi sah.

## Task 8 — Acceptance, dokumentasi, dan commit

**Files:** seluruh test terkait, docs/UX_AND_ACCESSIBILITY.md, docs/qa/2026-10-23-navigation-section-motion.md, logs.

- [ ] Jalankan Gradle secara berurutan agar cache tidak berebut. Pastikan emulator sudah boot sebelum connected tests. Perintah di bawah dijalankan dari root melalui PowerShell:

```powershell
rtk proxy .\gradlew.bat testDebugUnitTest
rtk proxy .\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.mankeu.presentation.NavigationMotionTest,com.mankeu.presentation.SectionMotionTest,com.mankeu.presentation.VisualSystemTest,com.mankeu.presentation.WelcomeScreenTest'
rtk proxy .\gradlew.bat lintDebug assembleDebug
rtk proxy python scripts/verify_visual_system.py
rtk git diff --check
```

- [ ] Expected: Gradle berakhir BUILD SUCCESSFUL, test report tidak memiliki failure, verifier PASS, diff check tanpa whitespace error. Jika test gagal, perbaiki dan rerun task terdampak; jangan mengklaim build/proses yang masih berjalan sebagai lulus.
- [ ] Jalankan regresi ContextualLottieTest dan TooltipFeedbackTest bila lifecycle/semantik ilustrasi atau feedback tersentuh. Tidak perlu mengubah atau memproses ulang aset Lottie.
- [ ] Rekam matriks phone compact dan tablet expanded, light/dark, font 100%/130%, Full/Reduced/Off. Skenario pokok: navigasi cepat, mode, scope, tab, overlay, bantuan, form+IME, daftar, impor, cold start dan resume.
- [ ] Lakukan TalkBack/fokus: konten keluar tidak actionable, heading target dapat ditemukan, modal fokus dipulihkan, tidak ada duplicate CTA. Uji scale sistem 0 dan preferensi in-app; restore pengaturan pengujian setelah selesai.
- [ ] Ukur frame timing pada build yang mendekati release, memakai device dan dataset sintetis yang sama sebelum/sesudah. Rekam 10 perpindahan setelah warm-up untuk tab utama dan Laporan; laporkan p50/p95/frame terlewat melalui Perfetto/FrameTimeline bila tersedia. Selidiki regresi berulang; debug screenshot tidak membuktikan performa.
- [ ] Isi laporan QA dengan nama test, perintah, hasil aktual, perangkat/API, screenshot/frame awal-tengah-akhir atau rekaman, serta limitasi. Jika physical device/profiling belum tersedia, tandai unknown; jangan menyebut smooth di semua perangkat.
- [ ] Update UX guide dengan kontrak yang benar-benar diterapkan. Tambah ADR ketika pola bersama diadopsi; gunakan nomor DEC berikutnya saat itu. Tambah changelog hanya untuk perubahan UI yang sudah diimplementasikan, bukan janji rencana ini.
- [ ] Tiap batch implementasi mengikutsertakan entry Activity Log dan log lain yang relevan dalam commit atomik. Stage path/hunk milik task saja; hindari `git add .`. Periksa staged diff sebelum commit dan pertahankan perubahan pengguna.

**Definition of done:** setiap baris kontrak motion memiliki bukti named workflow pada preferensi yang relevan; data/draft/scroll/aksi tetap benar; tidak ada mismatch scope atau penggandaan operasi; hasil build/lint/test dan keterbatasan perangkat dicatat. Approval plan atau screenshot statis saja bukan bukti implementasi selesai.

## Urutan eksekusi dan checkpoint

1. Task 1–2: fondasi motion dan konsistensi state; blokir perluasan scope animation jika mismatch belum selesai.
2. Task 3–4: tab dan hierarki navigasi; checkpoint review rekaman phone/tablet.
3. Task 5–7: form, daftar, status; checkpoint workflow tanpa perubahan hasil finansial.
4. Task 8: acceptance lintas preferensi, evidence dan handoff.

Perkiraan effort **proposed**: 4–6 hari kerja termasuk pengujian dan perbaikan, bergantung pada temuan state consistency serta perangkat yang tersedia. Estimasi ini bukan komitmen waktu penyelesaian.
