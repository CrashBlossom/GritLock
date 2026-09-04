package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Context
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.GauntletWithHabits
import com.example.fitlock.data.HabitWithDefinition
import com.example.fitlock.data.VaultItem

@Composable
fun LocksHub(
    groups: List<AppGroup>,
    vaultItems: List<VaultItem>,
    gauntlets: List<GauntletWithHabits>,
    onAddGroup: () -> Unit,
    onEditGroup: (AppGroup) -> Unit,
    onDeleteGroup: (AppGroup) -> Unit,
    onToggleGroup: (AppGroup) -> Unit,
    onAddVaultItem: (VaultItem) -> Unit,
    onDeleteVaultItem: (VaultItem) -> Unit,
    onUnlockVaultItem: (VaultItem) -> Unit,
    onAddGauntlet: () -> Unit,
    onEditGauntlet: (GauntletWithHabits) -> Unit,
    onDeleteGauntlet: (GauntletWithHabits) -> Unit,
    onToggleGauntlet: (GauntletWithHabits) -> Unit,
    onStartGauntlet: (GauntletWithHabits) -> Unit,
    onNavigateToHabitLibrary: () -> Unit = {},
    onHabitClick: (HabitWithDefinition) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fitlock_prefs", Context.MODE_PRIVATE) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Master Password state management
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    val isProtectionEnabled = prefs.getBoolean("master_password_enabled", false)
    val masterPassword = prefs.getString("master_password", "") ?: ""

    val handleProtectedAction = { action: () -> Unit ->
        if (isProtectionEnabled) {
            pendingAction = action
            showPasswordDialog = true
        } else {
            action()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("App Groups") },
                icon = { Icon(Icons.Default.Apps, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Secret Vault") },
                icon = { Icon(Icons.Default.Lock, contentDescription = null) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AppGroupsContent(
                    groups = groups,
                    onAdd = onAddGroup,
                    onEdit = { handleProtectedAction { onEditGroup(it) } },
                    onDelete = { handleProtectedAction { onDeleteGroup(it) } },
                    onToggle = { group ->
                        if (isProtectionEnabled && group.isEnabled) {
                            handleProtectedAction { onToggleGroup(group) }
                        } else {
                            onToggleGroup(group)
                        }
                    }
                )
                1 -> VaultScreen(
                    vaultItems = vaultItems,
                    onAddItem = onAddVaultItem,
                    onDeleteItem = { handleProtectedAction { onDeleteVaultItem(it) } },
                    onUnlockItem = onUnlockVaultItem
                )
            }
        }
    }

    if (showPasswordDialog) {
        MasterPasswordDialog(
            correctPassword = masterPassword,
            onDismiss = { showPasswordDialog = false },
            onSuccess = {
                showPasswordDialog = false
                pendingAction?.invoke()
                pendingAction = null
            }
        )
    }
}

@Composable
fun AppGroupsContent(
    groups: List<AppGroup>,
    onAdd: () -> Unit,
    onEdit: (AppGroup) -> Unit,
    onDelete: (AppGroup) -> Unit,
    onToggle: (AppGroup) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Group")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(groups) { group ->
                GroupItem(
                    group = group,
                    onToggle = { onToggle(group) },
                    onLongClick = { onEdit(group) },
                    onDelete = { onDelete(group) }
                )
            }
        }
    }
}
