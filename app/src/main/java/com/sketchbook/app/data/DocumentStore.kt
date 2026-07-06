package com.sketchbook.app.data

import android.content.Context
import com.sketchbook.app.model.SketchDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Loads and saves `.sketch` documents (JSON) in the app's private files directory.
 * Mirrors the iOS DocumentStore (which uses iCloud Documents; Android stores locally
 * and relies on Android Auto Backup for cloud restore).
 */
class DocumentStore(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dir: File = File(context.filesDir, "sketches").apply { mkdirs() }

    suspend fun loadAll(): List<SketchDocument> = withContext(Dispatchers.IO) {
        val docs = dir.listFiles { f -> f.extension == "sketch" }
            ?.mapNotNull { file ->
                runCatching { json.decodeFromString<SketchDocument>(file.readText()) }.getOrNull()
            } ?: emptyList()
        docs.sortedWith(
            compareByDescending<SketchDocument> { it.isFavorite }
                .thenByDescending { it.modifiedAt }
        )
    }

    suspend fun save(document: SketchDocument, bumpModified: Boolean = true): SketchDocument =
        withContext(Dispatchers.IO) {
            val doc = if (bumpModified) document.copy(modifiedAt = System.currentTimeMillis()) else document
            val file = File(dir, doc.fileName)
            val tmp = File(dir, doc.fileName + ".tmp")
            tmp.writeText(json.encodeToString(SketchDocument.serializer(), doc))
            tmp.renameTo(file)
            doc
        }

    suspend fun delete(document: SketchDocument) = withContext(Dispatchers.IO) {
        File(dir, document.fileName).delete()
    }

    suspend fun duplicate(document: SketchDocument): SketchDocument {
        val now = System.currentTimeMillis()
        val copy = document.copy(
            id = UUID.randomUUID().toString(),
            title = document.title + " copy",
            createdAt = now,
            modifiedAt = now,
        )
        return save(copy, bumpModified = false)
    }
}
