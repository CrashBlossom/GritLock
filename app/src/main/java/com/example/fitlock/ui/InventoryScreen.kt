/**
 * InventoryScreen handles the Gold economy and the Item Shop.
 * It's where users spend Gold earned from workouts and quests.
 */
package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.InventoryItem
import com.example.fitlock.data.UserStats

@Composable
fun InventoryScreen(
    userStats: UserStats?,
    inventory: List<InventoryItem>,
    onBuyItem: (InventoryItem) -> Unit,
    onUseItem: (InventoryItem) -> Unit
) {
    var showShop by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1. Header: Gold and Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (showShop) "General Store" else "Your Inventory",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Text(
                        " ${userStats?.gold ?: 0} Gold",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Toggle between Inventory and Shop
            Button(
                onClick = { showShop = !showShop },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showShop) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (showShop) Icons.Default.Info else Icons.Default.ShoppingCart, null)
                Text(if (showShop) " INVENTORY" else " SHOP")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. The Grid (Shop or Inventory)
        val displayList = if (showShop) getShopItems() else inventory.filter { it.quantity > 0 }

        if (displayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (showShop) "Store is out of stock!" else "Your bag is empty.",
                    color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayList) { item ->
                    ItemCard(
                        item = item,
                        isShop = showShop,
                        canAfford = (userStats?.gold ?: 0) >= item.price,
                        onClick = { if (showShop) onBuyItem(item) else onUseItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: InventoryItem, isShop: Boolean, canAfford: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C21)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(item.description, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.height(40.dp))
            
            Spacer(modifier = Modifier.height(12.dp))

            if (isShop) {
                Button(
                    onClick = onClick,
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) Color(0xFFFFD700) else Color.Gray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("${item.price} G", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("USE (${item.quantity})", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Hardcoded shop items for now.
 */
fun getShopItems(): List<InventoryItem> {
    return listOf(
        InventoryItem("pomo_potion", "Focus Potion", "Reduces your next Pomodoro by 5 mins.", 500, com.example.fitlock.data.ItemType.FOCUS_POTION),
        InventoryItem("discipline_weights", "Weights of Discipline", "Double STR XP, but workouts are harder.", 2000, com.example.fitlock.data.ItemType.WEIGHTS_OF_DISCIPLINE),
        InventoryItem("bypass_scroll", "Bypass Scroll", "Instantly unlock one app group.", 5000, com.example.fitlock.data.ItemType.BYPASS_SCROLL)
    )
}
