package com.mankeu.importer

data class WorkbookSnapshot(val sheets: List<WorkbookSheet>)

data class WorkbookSheet(
    val name: String,
    val rows: List<WorkbookRow>,
)

data class WorkbookRow(
    val index: Int,
    val cells: List<WorkbookCell>,
) {
    fun cellAt(columnIndex: Int): WorkbookCell? = cells.firstOrNull { it.columnIndex == columnIndex }
}

data class WorkbookCell(
    val reference: String,
    val columnIndex: Int,
    val text: String?,
    val number: Double?,
    val formula: String?,
) {
    val display: String get() = text ?: number?.toString().orEmpty()
}
