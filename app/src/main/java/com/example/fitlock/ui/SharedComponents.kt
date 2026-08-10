package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.ExerciseRequirement
import com.example.fitlock.exercise.ExerciseType
import com.example.fitlock.utils.AppInfoFetcher

@Composable
fun RequirementRow(index: Int, req: ExerciseRequirement, onUpdate: (ExerciseRequirement) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }
    val allApps = remember { AppInfoFetcher.getInstalledApps(context) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            var expanded by remember { mutableStateOf(false) }
            
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(req.type, color = Color.White, fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = expanded, 
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1C1C21))
                ) {
                    ExerciseType.entries.forEach { ex ->
                        DropdownMenuItem(
                            text = { Text(ex.name, color = Color.White) },
                            onClick = {
                                onUpdate(req.copy(type = ex.name))
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = req.count.toString(),
                onValueChange = { onUpdate(req.copy(count = it.toIntOrNull() ?: 0)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                label = { Text(if (req.type == "APP_USAGE") "Seconds" else "Reps", fontSize = 8.sp) }
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }

        if (req.type == "APP_USAGE") {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                val appCount = req.targetPackageNames.size
                Text(
                    text = if (appCount == 0) "Select Apps to Use" else "$appCount Apps Selected",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Select Apps", color = Color.White) },
            text = {
                var appSearch by remember { mutableStateOf("") }
                val filtered = allApps.filter { it.name.contains(appSearch, ignoreCase = true) }
                
                Column {
                    OutlinedTextField(
                        value = appSearch,
                        onValueChange = { appSearch = it },
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filtered) { app ->
                            val isSelected = req.targetPackageNames.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newList = if (isSelected) {
                                            req.targetPackageNames - app.packageName
                                        } else {
                                            req.targetPackageNames + app.packageName
                                        }
                                        onUpdate(req.copy(targetPackageNames = newList))
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD0BCFF))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(app.name, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(app.packageName, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAppPicker = false }) {
                    Text("Done")
                }
            },
            containerColor = Color(0xFF1C1C21)
        )
    }
}
