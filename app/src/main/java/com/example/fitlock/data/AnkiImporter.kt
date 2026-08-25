package com.example.fitlock.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class AnkiImporter(private val context: Context, private val repository: GritLockRepository) {

    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        android.util.Log.d("AnkiImporter", "Starting import from URI: $uri")
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        
        val mediaDir = File(context.filesDir, "anki_media")
        if (!mediaDir.exists()) mediaDir.mkdirs()
        
        val tempFiles = mutableListOf<File>()
        val mediaMapRaw = mutableMapOf<String, String>()
        var dbFile2: File? = null
        var dbFile21: File? = null

        try {
            while (entry != null) {
                val fileName = entry.name
                val outFile = File(context.cacheDir, "anki_tmp_$fileName")
                try {
                    FileOutputStream(outFile).use { output ->
                        zipInputStream.copyTo(output)
                    }
                    
                    if (fileName == "collection.anki2") {
                        dbFile2 = outFile
                    } else if (fileName == "collection.anki21") {
                        dbFile21 = outFile
                    } else if (fileName == "media") {
                        try {
                            val mediaJson = outFile.readText()
                            if (mediaJson.trim().startsWith("{")) {
                                val json = JSONObject(mediaJson)
                                json.keys().forEach { key ->
                                    mediaMapRaw[key] = json.getString(key)
                                }
                                android.util.Log.d("AnkiImporter", "Successfully parsed media map with ${mediaMapRaw.size} entries")
                            } else {
                                android.util.Log.w("AnkiImporter", "Skipping 'media' file - not a JSON object")
                            }
                        } catch (e: Exception) { 
                            android.util.Log.e("AnkiImporter", "Error reading 'media' file: ${e.message}")
                        }
                    } else {
                        tempFiles.add(outFile)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AnkiImporter", "Error extracting $fileName: ${e.message}")
                }
                
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        } finally {
            zipInputStream.close()
        }

        // Rename temp media files to their actual names in the permanent media dir
        tempFiles.forEach { file ->
            val key = file.name.removePrefix("anki_tmp_")
            val realName = mediaMapRaw[key]
            if (realName != null) {
                val destFile = File(mediaDir, realName)
                file.renameTo(destFile)
            } else {
                file.delete()
            }
        }

        val file = dbFile21 ?: dbFile2
        if (file != null) {
            try {
                android.util.Log.d("AnkiImporter", "Opening database: ${file.name}")
                val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                
                val deckNames = mutableMapOf<Long, String>()
                try {
                    val colCursor = db.rawQuery("SELECT decks FROM col", null)
                    if (colCursor.moveToFirst()) {
                        val decksJson = colCursor.getString(0)
                        val json = JSONObject(decksJson)
                        json.keys().forEach { key ->
                            val deck = json.getJSONObject(key)
                            deckNames[key.toLong()] = deck.getString("name")
                        }
                    }
                    colCursor.close()
                } catch (e: Exception) {
                    android.util.Log.e("AnkiImporter", "Error parsing decks: ${e.message}")
                }

                val cursor = db.rawQuery("""
                    SELECT n.flds, c.did 
                    FROM notes n 
                    JOIN cards c ON n.id = c.nid
                """.trimIndent(), null)

                val flashcardsToImport = mutableListOf<Flashcard>()
                
                // Get existing cards to prevent duplicates and preserve progress
                val existingCardKeys = repository.allFlashcards.first().map { 
                    "${it.deckName}|${it.frontText}|${it.backText}" 
                }.toSet()
                
                android.util.Log.d("AnkiImporter", "Database opened. Cursor count: ${cursor.count}. Existing cards: ${existingCardKeys.size}")
                
                if (cursor.moveToFirst()) {
                    do {
                        val flds = cursor.getString(0)
                        val deckId = cursor.getLong(1)
                        val fields = flds.split("\u001f")
                        
                        if (fields.size >= 2) {
                            val deckName = deckNames[deckId] ?: "Imported Deck"
                            val front = processText(fields[0], mediaDir)
                            val back = processText(fields[1], mediaDir)
                            
                            // Store all fields for flexible display settings
                            val fieldsArray = JSONArray()
                            fields.forEach { fieldsArray.put(it) }
                            val fieldsJson = fieldsArray.toString()

                            // 1. Check ALL fields for boilerplate
                            var isBoilerplate = false
                            val boilerplateKeywords = listOf(
                                "update to", "latest anki", "anki version", "ankidroid", 
                                "ankimobile", "anki desktop", "ankiweb", "import .apkg", 
                                ".colpkg", "no cards found", "please download", "syncing with anki",
                                "please update to the latest"
                            )

                            for (field in fields) {
                                val clean = stripHtml(field).lowercase()
                                if (boilerplateKeywords.any { clean.contains(it) }) {
                                    isBoilerplate = true
                                    android.util.Log.d("AnkiImporter", "Skipping boilerplate found in field: $clean")
                                    break
                                }
                            }
                            
                            if (isBoilerplate) continue
                            
                            // 2. Prevent duplicates
                            val cardKey = "$deckName|$front|$back"
                            if (existingCardKeys.contains(cardKey)) {
                                android.util.Log.d("AnkiImporter", "Skipping duplicate card: $front")
                                continue
                            }

                            flashcardsToImport.add(Flashcard(
                                deckName = deckName,
                                frontText = front,
                                backText = back,
                                allFieldsJson = fieldsJson
                            ))
                        } else {
                            android.util.Log.d("AnkiImporter", "Skipping note with ${fields.size} fields (expected >= 2)")
                        }
                    } while (cursor.moveToNext())
                }
                cursor.close()
                db.close()

                if (flashcardsToImport.isNotEmpty()) {
                    repository.upsertFlashcards(flashcardsToImport)
                    android.util.Log.d("AnkiImporter", "SUCCESS: Imported ${flashcardsToImport.size} cards")
                } else {
                    android.util.Log.w("AnkiImporter", "WARNING: No new valid cards found after filtering.")
                }
            } catch (e: Exception) {
                android.util.Log.e("AnkiImporter", "DATABASE ERROR: ${e.message}", e)
            } finally {
                dbFile2?.delete()
                dbFile21?.delete()
            }
        } else {
            android.util.Log.e("AnkiImporter", "ERROR: Could not find collection.anki2 or .anki21 in .apkg")
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()
    }

    private fun processText(html: String, mediaDir: File): String {
        // We'll leave the HTML tags for the UI to parse.
        // The UI's CardContent will look for <img> tags.
        return html.replace("&nbsp;", " ").trim()
    }
}
