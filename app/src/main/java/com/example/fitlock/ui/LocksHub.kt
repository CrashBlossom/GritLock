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
import androidx.compose.ui.unit.dp
import com.example.fitlock.data.AppGroup
import com.example.fitlock.data.VaultItem

@Composable
fun LocksHub(
    groups: List<AppGroup>,
    vaultItems: List<VaultItem>,
    onAddGroup: () -> Unit,
    onEditGroup: (AppGroup) -> Unit,
    onDeleteGroup: (AppGroup) -> Unit,
    onToggleGroup: (AppGroup) -> Unit,
    onAddVaultItem: (VaultItem) -> Unit,
    onDeleteVaultItem: (VaultItem) -> Unit,
    onUnlockVaultItem: (VaultItem) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
            if (selectedTab == 0) {
                AppGroupsContent(
                    groups = groups,
                    onAdd = onAddGroup,
                    onEdit = onEditGroup,
                    onDelete = onDeleteGroup,
                    onToggle = onToggleGroup
                )
            } else {
                VaultScreen(
                    vaultItems = vaultItems,
                    onAddItem = onAddVaultItem,
                    onDeleteItem = onDeleteVaultItem,
                    onUnlockItem = onUnlockVaultItem
                )
            }
        }
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
