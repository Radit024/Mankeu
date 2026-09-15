# Interaction Feedback - Tooltip dan Haptic

Cara Mankeu memberi tahu pengguna bahwa sentuhannya diterima: tooltip tekan lama dari Material 3 dan lima jenis getar. Aturannya ada di `docs/UX_AND_ACCESSIBILITY.md` ("Touch targets, tooltips and haptics") dan keputusan D-084 sampai D-087.

## 1. Dari peristiwa ke getar

```mermaid
flowchart TD
    subgraph eventSources ["Sumber peristiwa"]
        longPress["Tekan lama pada ikon, badge, atau kartu angka"]
        discrete["Ganti tab, segmen, filter, tanggal, atau tutup sheet"]
        destructive["Mulai hapus, balik, atau arsipkan"]
        formInvalid["Validasi formulir gagal"]
        vmMessage["Pesan ViewModel atau hasil ekspor dan cadangan"]
    end

    longPress --> tooltipBox["TooltipBox<br/>state.isVisible menjadi true"]
    tooltipBox --> evLong["LONG_PRESS"]
    discrete --> sameChoice{"Pilihan yang sama dengan sekarang?"}
    sameChoice -->|"ya"| noFeedback(["Tanpa getar"])
    sameChoice -->|"tidak"| evTap["TAP"]
    destructive --> evWarn["WARN"]
    formInvalid --> evReject["REJECT"]
    vmMessage --> isError{"UiText.isError atau TransferUiState.Failure?"}
    isError -->|"ya"| evReject
    isError -->|"tidak"| evConfirm["CONFIRM"]

    evLong --> feedback["MankeuFeedback"]
    evTap --> feedback
    evWarn --> feedback
    evReject --> feedback
    evConfirm --> feedback

    feedback --> hapticSwitch{"Sakelar getar di Pengaturan aktif?"}
    hapticSwitch -->|"tidak"| silent(["Tanpa getar"])
    hapticSwitch -->|"ya"| composeHaptic["LocalHapticFeedback<br/>performHapticFeedback"]
    composeHaptic --> platformHaptic["Getar platform<br/>mengikuti setelan getar ponsel"]
    feedback --> soundSwitch{"Sakelar suara aktif dan event bersuara?"}
    soundSwitch -->|"ya"| soundEffect["View.playSoundEffect"]
```

| Event | `HapticFeedbackType` | Contoh |
| --- | --- | --- |
| TAP | SegmentTick | Tab Laporan, Proyeksi atau Realisasi, filter Riwayat, tanggal di kalender |
| LONG_PRESS | LongPress | Tooltip ikon Data, badge DIBALIK, kartu Uang masuk bersih |
| WARN | GestureThresholdActivate | Ketuk ikon hapus atau balik pada transaksi |
| CONFIRM | Confirm | Transaksi tersimpan, impor dikomit, cadangan dibuat |
| REJECT | Reject | Jumlah kosong, hapus transaksi kredit yang sudah dibayar sebagian |

## 2. Siklus tooltip

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Pemilik usaha
    participant Anchor as Ikon, badge, atau kartu
    participant TipBox as TooltipBox
    participant Tip as MankeuTooltip
    participant Fb as MankeuFeedback

    Owner->>Anchor: Tekan dan tahan
    Anchor->>TipBox: Ambang tekan lama tercapai
    TipBox->>TipBox: Tampilkan di atas anchor, pindah ke bawah bila tidak muat
    TipBox-->>Tip: snapshotFlow isVisible bernilai true
    Tip->>Fb: LONG_PRESS
    Fb-->>Owner: Getar tekan lama
    Owner->>Anchor: Lepas jari
    TipBox->>TipBox: Pelepasan dikonsumsi, aksi tombol tidak ikut berjalan
    alt Jari diangkat, ketuk di luar, atau waktu habis
        TipBox->>TipBox: Tooltip ditutup
    else Pembaca layar aktif
        TipBox-->>Owner: Teks tooltip dibacakan sebagai deskripsi anchor
    end
```

## 3. Tampilan kartu transaksi

```mermaid
stateDiagram-v2
    state "Aktif, hijau atau koral" as Active
    state "Asli yang dibalik, netral dan dicoret" as ReversedOriginal
    state "Entri pembalik, netral dengan tanda pemulihan" as ReversalEntry

    [*] --> Active : transaksi tersimpan
    Active --> ReversedOriginal : reverseEvent
    ReversedOriginal --> Active : entri pembaliknya dihapus
    [*] --> ReversalEntry : dibuat oleh reverseEvent
    ReversalEntry --> [*] : dihapus

    note right of ReversalEntry
        Tanda plus bila membatalkan pengeluaran,
        minus bila membatalkan pemasukan,
        tanpa tanda bila sumbernya di luar 200 baris terakhir.
    end note
```
