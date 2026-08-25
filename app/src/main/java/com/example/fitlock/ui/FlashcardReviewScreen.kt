package com.example.fitlock.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitlock.data.Flashcard
import com.example.fitlock.data.GritLockRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

@Composable
fun FlashcardReviewScreen(
    deckName: String,
    repository: GritLockRepository,
    onFinish: () -> Unit
) {
    val flashcards by repository.getFlashcardsByDeck(deckName).collectAsState(initial = emptyList())
    
    // The master list of cards that are due
    val dueCardsBase = remember(flashcards) { 
        flashcards.filter { it.nextReviewDate <= System.currentTimeMillis() + 60000 } // Buffer 1 min
    }
    
    // The active session queue (allows for "Again" re-insertion)
    val sessionQueue = remember { mutableStateListOf<Flashcard>() }
    
    // Sync the queue when the screen starts or when cards are imported
    LaunchedEffect(dueCardsBase) {
        if (sessionQueue.isEmpty() && dueCardsBase.isNotEmpty()) {
            sessionQueue.addAll(dueCardsBase.shuffled())
        }
    }
    
    var showAnswer by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showScribble by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var currentScribble by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anki_deck_settings", android.content.Context.MODE_PRIVATE) }
    var frontFieldsStr by remember(deckName) { mutableStateOf(prefs.getString("${deckName}_front", "0") ?: "0") }
    var backFieldsStr by remember(deckName) { mutableStateOf(prefs.getString("${deckName}_back", "1") ?: "1") }

    val scope = rememberCoroutineScope()
    val cardScrollState = rememberScrollState()

    if (sessionQueue.isEmpty()) {
        if (flashcards.isNotEmpty() && dueCardsBase.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No cards due in this deck! 🎉", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onFinish) { Text("Go Back") }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val currentCard = sessionQueue.first()
    
    val allFields = remember(currentCard) {
        try {
            val arr = JSONArray(currentCard.allFieldsJson ?: "[]")
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            if (list.isEmpty()) listOf(currentCard.frontText, currentCard.backText) else list
        } catch (e: Exception) {
            listOf(currentCard.frontText, currentCard.backText)
        }
    }

    val frontContent = remember(allFields, frontFieldsStr) {
        frontFieldsStr.split(",").mapNotNull { it.toIntOrNull() }
            .mapNotNull { allFields.getOrNull(it) }
            .filter { it.isNotBlank() }
            .joinToString("<br>")
            .ifBlank { currentCard.frontText }
    }
    
    val backContent = remember(allFields, backFieldsStr) {
        backFieldsStr.split(",").mapNotNull { it.toIntOrNull() }
            .mapNotNull { allFields.getOrNull(it) }
            .filter { it.isNotBlank() }
            .joinToString("<br>")
            .ifBlank { currentCard.backText }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reviewing: $deckName (${sessionQueue.size} left)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showScribble) {
                    IconButton(onClick = { 
                        currentScribble = "" 
                        scope.launch { 
                            repository.upsertFlashcard(currentCard.copy(drawingData = "")) 
                        }
                    }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Clear Scribble")
                    }
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Deck Settings")
                }
                IconButton(onClick = { showScribble = !showScribble }) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Toggle Scribble",
                        tint = if (showScribble) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(if (isEditing) Icons.Default.Save else Icons.Default.Edit, contentDescription = "Edit Card")
                }
            }
        }
        
        if (dueCardsBase.isNotEmpty()) {
            val total = dueCardsBase.size
            val finished = (total - sessionQueue.size).coerceAtLeast(0)
            LinearProgressIndicator(
                progress = { (finished.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                        .verticalScroll(cardScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isEditing) {
                        val editedFields = remember(currentCard) {
                            mutableStateListOf<String>().also { it.addAll(allFields) }
                        }
                        
                        editedFields.forEachIndexed { index, field ->
                            OutlinedTextField(
                                value = field,
                                onValueChange = { editedFields[index] = it },
                                label = { Text("Field $index") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val newFieldsArr = JSONArray()
                                    editedFields.forEach { newFieldsArr.put(it) }
                                    val updated = currentCard.copy(
                                        frontText = editedFields.getOrNull(0) ?: "",
                                        backText = editedFields.getOrNull(1) ?: "",
                                        allFieldsJson = newFieldsArr.toString()
                                    )
                                    repository.upsertFlashcard(updated)
                                    
                                    val idx = sessionQueue.indexOf(currentCard)
                                    if (idx != -1) sessionQueue[idx] = updated
                                    isEditing = false
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Save All Fields")
                        }
                    } else {
                        CardContent(text = frontContent)
                        
                        if (showAnswer) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
                            CardContent(text = backContent, isAnswer = true)
                        }
                    }
                }

                if (!isEditing && showScribble) {
                    ScribbleCanvas(
                        modifier = Modifier.fillMaxSize(),
                        initialScribbleData = currentCard.drawingData,
                        transparentBackground = true,
                        onScribbleUpdated = { 
                            currentScribble = it
                            scope.launch {
                                repository.upsertFlashcard(currentCard.copy(drawingData = it))
                            }
                        }
                    )
                }
            }
        }

        if (!isEditing) {
            if (!showAnswer) {
                Button(
                    onClick = { showAnswer = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Show Answer")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val buttons = listOf(
                        "Again" to 1,
                        "Hard" to 2,
                        "Good" to 3,
                        "Easy" to 4
                    )
                    
                    buttons.forEach { (label, rating) ->
                        Button(
                            onClick = {
                                scope.launch {
                                    val updatedCard = updateSM2(currentCard, rating, currentScribble)
                                    repository.upsertFlashcard(updatedCard)
                                    
                                    showAnswer = false
                                    currentScribble = ""
                                    
                                    // SESSION LOGIC: 
                                    // If rating is "Again" (1), move to the end of the queue.
                                    // Otherwise, remove from current session.
                                    sessionQueue.removeAt(0)
                                    if (rating == 1) {
                                        sessionQueue.add(updatedCard)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when(rating) {
                                    1 -> MaterialTheme.colorScheme.error
                                    4 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        ) {
                            Text(
                                text = label, 
                                fontSize = 10.sp, 
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        CardSettingsDialog(
            allFields = allFields,
            frontFieldsStr = frontFieldsStr,
            backFieldsStr = backFieldsStr,
            onDismiss = { showSettings = false },
            onSave = { front, back ->
                frontFieldsStr = front
                backFieldsStr = back
                prefs.edit().putString("${deckName}_front", front).putString("${deckName}_back", back).apply()
                showSettings = false
            }
        )
    }
}

@Composable
fun CardSettingsDialog(
    allFields: List<String>,
    frontFieldsStr: String,
    backFieldsStr: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val frontIndices = remember { frontFieldsStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableStateList() }
    val backIndices = remember { backFieldsStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableStateList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Settings") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Choose which fields to show on each side.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                allFields.forEachIndexed { index, field ->
                    val cleanLabel = field.replace(Regex("<[^>]*>"), "").take(30).ifBlank { "Field $index" }
                    Text("Field $index: $cleanLabel", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = frontIndices.contains(index),
                            onCheckedChange = { checked ->
                                if (checked) frontIndices.add(index) else frontIndices.remove(index)
                            }
                        )
                        Text("Show on Front")
                        Spacer(modifier = Modifier.width(16.dp))
                        Checkbox(
                            checked = backIndices.contains(index),
                            onCheckedChange = { checked ->
                                if (checked) backIndices.add(index) else backIndices.remove(index)
                            }
                        )
                        Text("Show on Back")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(frontIndices.joinToString(","), backIndices.joinToString(","))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CardContent(text: String, isAnswer: Boolean = false) {
    val context = LocalContext.current
    val mediaDir = remember { File(context.filesDir, "anki_media") }
    
    // Regex to find <img> tags and extract src
    val imgRegex = remember { Regex("<img [^>]*src=\"([^\"]+)\"[^>]*>") }
    
    // Replace <br> and <div> tags with newlines to preserve formatting
    val formattedText = remember(text) {
        text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<div>", RegexOption.IGNORE_CASE), "")
            .replace("&nbsp;", " ")
    }
    
    val parts = remember(formattedText) {
        val result = mutableListOf<Pair<String, String?>>()
        var lastMatchEnd = 0
        imgRegex.findAll(formattedText).forEach { match ->
            if (match.range.first > lastMatchEnd) {
                result.add(formattedText.substring(lastMatchEnd, match.range.first) to null)
            }
            result.add("" to match.groups[1]?.value)
            lastMatchEnd = match.range.last + 1
        }
        if (lastMatchEnd < formattedText.length) {
            result.add(formattedText.substring(lastMatchEnd) to null)
        }
        result
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        parts.forEach { (plainText, imgSrc) ->
            if (plainText.isNotBlank()) {
                val cleanText = plainText.replace(Regex("<[^>]*>"), "").trim()
                if (cleanText.isNotEmpty()) {
                    Text(
                        text = cleanText,
                        fontSize = if (isAnswer) 22.sp else 28.sp,
                        fontWeight = if (isAnswer) FontWeight.Medium else FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isAnswer) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = if (isAnswer) 30.sp else 36.sp
                    )
                }
            }
            if (imgSrc != null) {
                val imgFile = File(mediaDir, imgSrc)
                if (imgFile.exists()) {
                    AsyncImage(
                        model = imgFile,
                        contentDescription = "Card Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .heightIn(max = 300.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private fun updateSM2(card: Flashcard, quality: Int, drawingData: String): Flashcard {
    var n = card.repetitions
    var ef = card.easinessFactor
    var i = card.interval

    if (quality >= 3) {
        if (n == 0) {
            i = 1
        } else if (n == 1) {
            i = 6
        } else {
            i = (i * ef).toInt().coerceAtLeast(1)
        }
        n++
    } else {
        n = 0
        i = 1
    }

    ef = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
    if (ef < 1.3) ef = 1.3

    val nextReview = System.currentTimeMillis() + (i * 24L * 60L * 60L * 1000L)
    
    return card.copy(
        repetitions = n,
        easinessFactor = ef,
        interval = i,
        nextReviewDate = nextReview,
        drawingData = drawingData
    )
}
