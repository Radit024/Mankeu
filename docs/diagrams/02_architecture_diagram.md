# Arsitektur Sistem Mankeu

Aplikasi native Android yang **offline-first**: semua data keuangan tinggal di Room SQLite di perangkat. Firebase Auth dan Google Drive hanya dipakai untuk cadangan terenkripsi opsional.

## 1. Lapisan dan dependensi

Dependensi dirakit secara manual di `MankeuApplication` (pola *manual dependency injection* yang direkomendasikan Android untuk satu grafik kecil) dan diteruskan ke `AppViewModel` lewat `viewModelFactory { initializer { } }` dari Jetpack Lifecycle.

```mermaid
flowchart TB
    subgraph wiring ["Perakitan aplikasi"]
        appContainer["MankeuApplication<br/>kontainer lazy"]
        mainActivity["MainActivity<br/>viewModelFactory dan initializer"]
    end

    subgraph presentation ["Presentasi: Jetpack Compose dan Material 3"]
        shell["MankeuApp<br/>collectAsStateWithLifecycle"]
        screens["Beranda, Catat, Riwayat, Laporan<br/>Overlay Data dan Pengaturan"]
        vicoChart["CashFlowChart<br/>Vico 3.2.3"]
    end

    subgraph stateLayer ["State"]
        viewModel["AppViewModel<br/>uiState StateFlow AppUiState<br/>isSaving dan mutating"]
    end

    subgraph domainLayer ["Domain: Kotlin murni, tanpa androidx"]
        ledgerPolicy["LedgerPolicy<br/>jurnal dan pembalik seimbang"]
        calculators["PostingReportCalculator<br/>MonthlyCashFlowCalculator<br/>BudgetPlanCalculator<br/>BusinessAnalysisCalculator"]
        txCalc["TransactionCalculation<br/>kuantitas BigDecimal, rupiah Long"]
    end

    subgraph dataLayer ["Data: Room"]
        repository["FinanceRepository<br/>satu corong semua penulisan<br/>aturan bisnis di withTransaction"]
        dao["FinanceDao<br/>proyeksi sempit dan agregasi SQL"]
        database[("AppDatabase schema 5")]
    end

    subgraph ioLayer ["Subsistem I/O"]
        importer["FinanceWorkbookImporter<br/>XlsxWorkbookReader: DOM platform,<br/>XXE mati, ZIP dibaca terbatas"]
        exporters["FinanceWorkbookExporter<br/>FinancePdfExporter<br/>MsmeTemplateExporter"]
        backupManager["EncryptedBackupManager<br/>PBKDF2-HMAC-SHA256 dan AES-256-GCM"]
        driveClient["GoogleDriveBackupClient<br/>HttpURLConnection dan org.json"]
        authManager["FirebaseAuthManager"]
    end

    appContainer --> mainActivity
    mainActivity --> viewModel
    shell --> screens
    screens --> vicoChart
    shell --> viewModel
    viewModel --> repository
    viewModel --> importer
    viewModel --> backupManager
    viewModel --> driveClient
    viewModel --> authManager
    viewModel --> calculators
    repository --> ledgerPolicy
    repository --> calculators
    repository --> txCalc
    repository --> exporters
    repository --> dao
    dao --> database
    backupManager --> database
```

## 2. Pohon komponen Compose

```mermaid
flowchart TD
    setContent["MainActivity.setContent"] --> mankeuApp["MankeuApp"]
    mankeuApp --> mankeuTheme["MankeuTheme<br/>token warna tanpa animasi"]
    mankeuTheme --> overlayContent["AnimatedContent overlay"]
    overlayContent -->|"DATA"| dataOverlay["OverlayScaffold<br/>DataScreen: impor, ekspor, cadangan, kategori"]
    overlayContent -->|"SETTINGS"| settingsOverlay["OverlayScaffold<br/>SettingsScreen"]
    overlayContent -->|"NONE"| widthCheck{"Lebar layar 840dp atau lebih?"}
    widthCheck -->|"ya"| appRail["AppRail"]
    widthCheck -->|"ya"| mainScaffoldWide["MainScaffold"]
    widthCheck -->|"tidak"| mainScaffold["MainScaffold<br/>NavigationBar 4 destinasi"]
    mainScaffold --> scopeHeader["ScopeHeader<br/>ScopeSummary dan tombol mode buku"]
    mainScaffold --> screenContent["ScreenContent"]
    mainScaffoldWide --> screenContent
    screenContent --> homeScreen["HomeScreen"]
    screenContent --> entryScreen["EntryScreen<br/>formulir saja"]
    screenContent --> ledgerScreen["LedgerScreen<br/>cari dan saring"]
    screenContent --> reportsScreen["ReportsScreen"]
    homeScreen --> homeCard["EventCard<br/>balik"]
    homeScreen --> metricCards["SimpleMetricCard<br/>read-out tanpa klik"]
    ledgerScreen --> ledgerCard["EventCard<br/>balik dan hapus, 48dp"]
    reportsScreen --> monthlyChart["MonthlyCashFlowChart"]
    reportsScreen --> statusBadge["Badge status piutang"]
    mankeuApp --> scopeSheet["ManageScopeSheet"]
    mankeuApp --> restoreDialog["RestorePreviewDialog"]
    mankeuApp --> spotlightTour["SpotlightTour"]
    mankeuApp --> feedbackProvider["CompositionLocal LocalFeedback<br/>MankeuFeedback"]

    subgraph tooltipLayer ["MankeuTooltip: TooltipBox dan PlainTooltip"]
        tooltipAnchor["Anchor tekan lama"]
    end
    scopeHeader --> tooltipAnchor
    homeCard --> tooltipAnchor
    ledgerCard --> tooltipAnchor
    metricCards --> tooltipAnchor
    statusBadge --> tooltipAnchor
    tooltipAnchor -.->|"LONG_PRESS"| feedbackProvider
```

## 3. Aliran data reaktif

```mermaid
flowchart LR
    roomTables[("Tabel Room")] -->|"invalidation"| daoFlows["Flow FinanceDao<br/>observeEventsForBook LIMIT 200<br/>observeCashFlowRows<br/>observeCategoryTotals<br/>observePostingRows"]
    daoFlows --> repoFlows["FinanceRepository<br/>pemetaan ke model domain"]

    subgraph selectionChain ["Rantai pemilihan: SharingStarted.Eagerly"]
        selBusinesses["businesses"] --> selProjects["projects"]
        selProjects --> selBooks["books"]
        selBooks --> selBookId["selectedBookId"]
    end

    subgraph bookQueries ["Kueri per buku: WhileSubscribed 5000 ms"]
        perBook["events, eventCount, cashFlowRecords,<br/>categoryTotals, budgetItems, obligations,<br/>summary, assumptions, importBatches"]
    end

    repoFlows --> selBusinesses
    selBookId -->|"flatMapLatest"| perBook
    repoFlows --> perBook
    perBook --> combined["combine bertingkat"]
    selBookId --> combined
    combined --> uiState["uiState: StateFlow AppUiState"]
    uiState -->|"collectAsStateWithLifecycle"| composeUi["Compose UI"]
    composeUi -->|"aksi pengguna"| mutatingGuard["mutating<br/>isSaving compareAndSet"]
    mutatingGuard --> repoWrite["FinanceRepository<br/>withTransaction"]
    repoWrite --> roomTables
```

## Prinsip desain

1. **Satu sumber kebenaran.** UI hanya membaca `AppUiState`. Setiap penulisan melewati `AppViewModel.mutating` lalu `FinanceRepository`, dan setiap aturan bisnis ditegakkan di repository di dalam transaksi yang sama dengan penulisannya.
2. **Domain murni.** Kalkulator dan `LedgerPolicy` tidak mengimpor paket Android sehingga seluruhnya diuji dengan unit test JVM.
3. **Uang presisi.** Rupiah disimpan sebagai `Long` bulat. Rasio, margin, dan kuantitas memakai `BigDecimal` dengan `RoundingMode.HALF_UP`.
4. **Primitif standar.** `org.json` bawaan Android untuk JSON Drive, `viewModelFactory` dari Jetpack, parser DOM platform untuk XLSX, dan `NumberFormat` yang di-cache per thread (D-082). Hilt, Apache POI, DataStore, dan AppCompat dipertimbangkan lalu ditolak dengan alasan di D-082.
5. **Cadangan tanpa server.** Berkas `.aabk` dienkripsi AES-256-GCM dengan kunci PBKDF2-HMAC-SHA256 310.000 iterasi. Drive hanya menyimpan ciphertext di `appDataFolder`.
