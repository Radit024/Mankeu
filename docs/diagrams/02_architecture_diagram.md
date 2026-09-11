# Arsitektur Sistem Mankeu (Clean and Layered Architecture)

Aplikasi Mankeu dirancang dengan prinsip **Local-First**, **Offline-First**, dan pemisahan tanggung jawab yang ketat (*Clean Architecture*). Operasi lokal tidak bergantung pada koneksi internet, akun eksternal, atau backend server.

```mermaid
flowchart TB
    subgraph PresentationLayer ["1. Presentation Layer (Jetpack Compose and M3)"]
        direction TB
        UI_Shell["MankeuApp Navigation Shell\n(TopAppBar, ScopeSummary, SegmentedBookMode)"]
        
        subgraph Screens ["Screens and Components"]
            ScreenHome["HomeScreen\n(Cash Headline, 4 Core Metrics, Feasibility Card)"]
            ScreenEntry["EntryScreen\n(Fast 5-Tap Buttons, Dynamic Modal Bottom Sheet)"]
            ScreenReports["ReportsScreen\n(Income Statement, Vico Cash Flow Chart, BEP)"]
            ScreenMore["MoreScreen / DataScreen\n(Category Management, Audit Batches, Exports)"]
            ScreenScope["ManageScopeScreen\n(Business and Project Management)"]
        end

        subgraph Visuals ["Visualization and Interaction"]
            VicoChart["Vico 3.2.3 CartesianChartHost\n(Native Horizontal Scroll and Touch Tooltips)"]
            TourEngine["TourTarget Framework\n(Interactive Onboarding Walkthroughs)"]
            FeedbackManager["LocalFeedback\n(Haptic and Audio Confirmation)"]
        end

        UI_Shell --> Screens
        ScreenReports --> VicoChart
    end

    subgraph StateLayer ["2. ViewModel and State Management"]
        AppVM["AppViewModel\n(Single Source of UI Truth)"]
        UiState["AppUiState\n(Reactive StateFlow with WhileSubscribed)"]
        AppVM --> UiState
        Screens --> AppVM
        UiState --> Screens
    end

    subgraph DomainLayer ["3. Domain Layer (Pure Kotlin - Zero Android Framework Deps)"]
        direction TB
        subgraph Calculators ["Business and Accounting Calculators"]
            CalcFeasibility["BusinessAnalysisCalculator\n(BEP, BC Ratio, Financing Gap, Net Margin)"]
            CalcCashFlow["MonthlyCashFlowCalculator\n(Chronological Net and Cumulative Flows)"]
            CalcBudgetPlan["BudgetPlanCalculator\n(Dated RAB Scheduling and Projections)"]
            CalcReport["PostingReportCalculator\n(Double-Entry Balance and Summary)"]
        end

        subgraph Policies ["Accounting and Classification Policies"]
            PolicyLedger["LedgerPolicy\n(Double-Entry Mapping and Reversal Journals)"]
            Classifier["CategoryClassifier\n(Rule-based and Semantic Tagging)"]
        end

        subgraph DomainModels ["Domain Models and Invariants"]
            ModelEvent["MoneyEvent and CashFlowRecord"]
            ModelJournal["JournalEntry and Posting"]
            ModelObligation["Obligation and Settlement"]
            ModelAnalysis["BusinessAnalysis and AnalysisAssumptions"]
        end
    end

    subgraph DataLayer ["4. Data and Persistence Layer (Room SQLite)"]
        direction TB
        Repo["FinanceRepository\n(Single Funnel for All Writes and Transactions)"]
        Dao["FinanceDao\n(Room DAO with Narrow SQL Projections and Limits)"]
        RoomDB["AppDatabase Schema 4\n(Room SQLite, WAL Mode, Foreign Key Cascades)"]
        
        Repo --> Dao
        Dao --> RoomDB
    end

    subgraph Subsystems ["5. Specialized Subsystems"]
        direction TB
        subgraph ImportExport ["Workbook and Report Engine"]
            Importer["FinanceWorkbookImporter\n(Multi-sheet Koordinatif, Atomic Commit)"]
            ExporterXlsx["FinanceWorkbookExporter\n(Auditable Multi-sheet XLSX)"]
            ExporterPdf["PdfReportExporter\n(Printable Financial Statements)"]
            ProvenanceEngine["Provenance Engine\n(Notice Tracking, Inference Confidence)"]
        end

        subgraph SecurityBackup ["Zero-Knowledge Backup and Cloud Sync"]
            BackupManager["EncryptedBackupManager\n(AES-GCM-256 with PBKDF2 Password Key)"]
            Codec["PortableBackupCodec\n(Portable Binary Database Snapshot)"]
            CloudClient["GoogleDriveBackupClient\n(Optional Cloud Sync via Drive AppDataFolder)"]
            AuthManager["FirebaseAuthManager\n(Optional Google Sign-In)"]
        end
    end

    %% Wiring between layers
    AppVM --> Repo
    AppVM --> DomainLayer
    Repo --> DomainLayer
    Repo --> Subsystems
    AppVM --> Subsystems

    %% Styling
    classDef pres fill:#E3F2FD,stroke:#1E88E5,stroke-width:2px;
    classDef state fill:#E8EAF6,stroke:#3F51B5,stroke-width:2px;
    classDef domain fill:#E8F5E9,stroke:#43A047,stroke-width:2px;
    classDef data fill:#FFF3E0,stroke:#FB8C00,stroke-width:2px;
    classDef subsys fill:#F3E5F5,stroke:#8E24AA,stroke-width:2px;

    class PresentationLayer,UI_Shell,Screens,Visuals pres;
    class StateLayer,AppVM,UiState state;
    class DomainLayer,Calculators,Policies,DomainModels domain;
    class DataLayer,Repo,Dao,RoomDB data;
    class Subsystems,ImportExport,SecurityBackup subsys;
```

## Prinsip Desain Utama

1. **Unidirectional Data Flow (UDF)**:
   - UI hanya membaca immutable `AppUiState`.
   - Perubahan data hanya terjadi melalui pemanggilan fungsi suspend pada `AppViewModel` yang mendelegasikan ke `FinanceRepository`.
2. **Kemandirian Domain (Domain Purity)**:
   - Modul kalkulasi kelayakan (`BusinessAnalysisCalculator`), jadwal kas (`MonthlyCashFlowCalculator`), dan aturan buku besar (`LedgerPolicy`) tidak mengimpor satupun paket Android (`androidx.*`). Semuanya adalah fungsi pure Kotlin yang 100% *unit-testable*.
3. **Penyimpanan Uang Presisi Tinggi**:
   - Nilai uang disimpan secara eksklusif dalam tipe integer `Long` Rupiah.
   - Perhitungan rasio dan margin menggunakan `java.math.BigDecimal` dengan `RoundingMode.HALF_UP` untuk menghindari kesalahan komputasi desimal floating-point.
4. **Keamanan Data (Zero-Knowledge Backup)**:
   - Cadangan data (`.aabk`) dienkripsi menggunakan AES-256 dalam mode GCM dengan otentikasi data (AAD). Kunci diturunkan dari kata sandi pengguna via PBKDF2 (310.000 iterasi). Server atau pihak ketiga tidak dapat membaca isi keuangan.
