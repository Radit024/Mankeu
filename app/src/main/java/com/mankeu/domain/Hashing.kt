package com.mankeu.domain

import java.security.MessageDigest

/**
 * Lowercase hex SHA-256 of these bytes.
 *
 * Two identical copies of this existed — `AppViewModel.computeSha256Hex` for the hash stamped on a
 * cloud backup, and `FinanceWorkbookImporter.ByteArray.sha256()` for the workbook identity that
 * makes an import idempotent. Both are content hashes that other records are keyed by, so the two
 * must agree byte for byte forever; keeping them as separate private functions meant a change to
 * one could silently diverge from the other and orphan every hash written before it.
 */
fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
