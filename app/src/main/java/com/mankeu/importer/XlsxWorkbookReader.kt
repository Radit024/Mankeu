package com.mankeu.importer

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XlsxWorkbookReader(
    private val maxEntries: Int = 1_000,
    private val maxUncompressedBytes: Long = 64L * 1024L * 1024L,
) {
    fun read(input: InputStream): WorkbookSnapshot {
        val entries = readZipEntries(input)
        val workbook = entries["xl/workbook.xml"] ?: error("Workbook metadata xl/workbook.xml is missing.")
        val relationships = entries["xl/_rels/workbook.xml.rels"]
            ?: error("Workbook relationships are missing.")
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val relationTargets = parseRelationships(relationships)
        val workbookDocument = parseXml(workbook)

        val sheets = workbookDocument.elements("sheet").map { sheetElement ->
            val name = sheetElement.getAttribute("name")
            val relationId = sheetElement.attributeByLocalName("id")
            val target = relationTargets[relationId] ?: error("Sheet relationship $relationId is missing.")
            val path = if (target.startsWith('/')) normalizePath("", target) else normalizePath("xl", target)
            val sheetBytes = entries[path] ?: error("Worksheet $path is missing.")
            parseSheet(name, sheetBytes, sharedStrings)
        }
        require(sheets.isNotEmpty()) { "Workbook contains no worksheets." }
        return WorkbookSnapshot(sheets)
    }

    private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(result.size < maxEntries) { "Workbook contains too many ZIP entries." }
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    total += bytes.size
                    require(total <= maxUncompressedBytes) { "Workbook is too large after decompression." }
                    val safeName = entry.name.replace('\\', '/')
                    require(!safeName.startsWith("/") && ".." !in safeName.split('/')) { "Unsafe workbook path." }
                    result[safeName] = bytes
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun parseRelationships(bytes: ByteArray): Map<String, String> = parseXml(bytes)
        .elements("Relationship")
        .associate { it.getAttribute("Id") to it.getAttribute("Target") }

    private fun parseSharedStrings(bytes: ByteArray): List<String> = parseXml(bytes)
        .elements("si")
        .map { stringItem -> stringItem.descendantText("t") }

    private fun parseSheet(name: String, bytes: ByteArray, sharedStrings: List<String>): WorkbookSheet {
        val document = parseXml(bytes)
        val rows = document.elements("row").mapIndexed { fallbackIndex, rowElement ->
            val rowIndex = rowElement.getAttribute("r").toIntOrNull() ?: fallbackIndex + 1
            val cells = rowElement.directChildren("c").mapNotNull { cell ->
                val reference = cell.getAttribute("r").ifBlank { return@mapNotNull null }
                val type = cell.getAttribute("t")
                val raw = cell.directChildren("v").firstOrNull()?.textContent
                val inline = cell.directChildren("is").firstOrNull()?.descendantText("t")
                val formula = cell.directChildren("f").firstOrNull()?.textContent
                val text = when (type) {
                    "s" -> raw?.toIntOrNull()?.let(sharedStrings::getOrNull)
                    "inlineStr" -> inline
                    "str", "b", "e" -> raw
                    else -> null
                }
                val number = if (type.isBlank() || type == "n") raw?.toDoubleOrNull() else null
                WorkbookCell(reference, columnIndex(reference), text, number, formula)
            }
            WorkbookRow(rowIndex, cells)
        }
        return WorkbookSheet(name, rows)
    }

    private fun parseXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // Android's bundled DOM implementation may throw UnsupportedOperationException
            // for these optional JAXP capabilities even though desktop JVMs accept them.
            runCatching { isXIncludeAware = false }
            runCatching { setExpandEntityReferences(false) }
            safeFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            safeFeature("http://xml.org/sax/features/external-general-entities", false)
            safeFeature("http://xml.org/sax/features/external-parameter-entities", false)
            trySetAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
            trySetAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    private fun DocumentBuilderFactory.safeFeature(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private fun DocumentBuilderFactory.trySetAttribute(name: String, value: String) {
        runCatching { setAttribute(name, value) }
    }

    private fun Document.elements(localName: String): List<Element> {
        val nodes = getElementsByTagNameNS("*", localName)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun Element.directChildren(localName: String): List<Element> = buildList {
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            if (element.localName == localName || element.tagName == localName) add(element)
        }
    }

    private fun Element.descendantText(localName: String): String {
        val nodes = getElementsByTagNameNS("*", localName)
        return (0 until nodes.length).joinToString("") { nodes.item(it).textContent.orEmpty() }
    }

    private fun Element.attributeByLocalName(localName: String): String {
        val attributes = attributes
        for (index in 0 until attributes.length) {
            val item = attributes.item(index)
            if (item.localName == localName || item.nodeName == localName) return item.nodeValue
        }
        return ""
    }

    private fun columnIndex(reference: String): Int {
        val letters = reference.takeWhile(Char::isLetter).uppercase()
        return letters.fold(0) { value, character -> value * 26 + (character - 'A' + 1) } - 1
    }

    private fun normalizePath(base: String, target: String): String {
        val parts = (base.trimEnd('/') + "/" + target.replace('\\', '/').trimStart('/')).split('/')
        val normalized = ArrayDeque<String>()
        parts.forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (normalized.isNotEmpty()) normalized.removeLast()
                else -> normalized.addLast(part)
            }
        }
        return normalized.joinToString("/")
    }
}
