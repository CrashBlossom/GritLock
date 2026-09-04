package com.example.fitlock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlock.data.*

@Composable
fun AtlasScreen(
    quests: List<QuestWithBlocks>,
    goals: List<GoalWithProjects>,
    onAddRoutine: () -> Unit,
    onEditQuest: (QuestWithBlocks) -> Unit,
    onDeleteQuest: (QuestWithBlocks) -> Unit,
    onToggleQuest: (QuestWithBlocks) -> Unit,
    onStartQuest: (QuestWithBlocks) -> Unit,
    onUpsertGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onUpsertProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onUpsertMilestone: (Milestone) -> Unit,
    onDeleteMilestone: (Milestone) -> Unit,
    onUpsertProjectTask: (ProjectTask) -> Unit,
    onDeleteProjectTask: (ProjectTask) -> Unit,
    title: String = "Atlas"
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Routines", "Projects", "Goals")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> RoutinesContent(
                    routines = quests.filter { it.quest.type == QuestType.ROUTINE },
                    onAdd = onAddRoutine,
                    onEdit = { onEditQuest(it) },
                    onDelete = { onDeleteQuest(it) },
                    onToggle = { onToggleQuest(it) },
                    onStart = { onStartQuest(it) }
                )
                1 -> ProjectsContent(
                    goals = goals,
                    onUpsertProject = onUpsertProject,
                    onDeleteProject = onDeleteProject,
                    onUpsertMilestone = onUpsertMilestone,
                    onDeleteMilestone = onDeleteMilestone,
                    onUpsertProjectTask = onUpsertProjectTask,
                    onDeleteProjectTask = onDeleteProjectTask
                )
                2 -> GoalsContent(
                    goals = goals,
                    onUpsertGoal = onUpsertGoal,
                    onDeleteGoal = onDeleteGoal
                )
            }
        }
    }
}

@Composable
fun GoalsContent(
    goals: List<GoalWithProjects>,
    onUpsertGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    var showAddDialog by remember { mutableStateOf<Goal?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No goals set. Define your North Star!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(goals) { goalWithProjects ->
                    val goal = goalWithProjects.goal
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showAddDialog = goal },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(goal.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(goal.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            IconButton(onClick = { onDeleteGoal(goal) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = Goal(title = "") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Goal")
        }
    }

    showAddDialog?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = { showAddDialog = null },
            onConfirm = { 
                onUpsertGoal(it)
                showAddDialog = null
            }
        )
    }
}

@Composable
fun ProjectsContent(
    goals: List<GoalWithProjects>,
    onUpsertProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onUpsertMilestone: (Milestone) -> Unit,
    onDeleteMilestone: (Milestone) -> Unit,
    onUpsertProjectTask: (ProjectTask) -> Unit,
    onDeleteProjectTask: (ProjectTask) -> Unit
) {
    var showAddDialog by remember { mutableStateOf<Project?>(null) }
    var showMilestoneDialog by remember { mutableStateOf<Milestone?>(null) }
    var showTaskDialog by remember { mutableStateOf<ProjectTask?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Create a Goal first to start projects!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                goals.forEach { goalWithProjects ->
                    item {
                        Text(goalWithProjects.goal.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    items(goalWithProjects.projects) { projectWithMilestones ->
                        val project = projectWithMilestones.project
                        var expanded by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(project.title, fontWeight = FontWeight.Bold)
                                        Text("${projectWithMilestones.milestones.size} milestones", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    IconButton(onClick = { onDeleteProject(project) }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                    }
                                }
                                
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Milestones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    projectWithMilestones.milestones.forEach { milestone ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                                            Checkbox(checked = milestone.isCompleted, onCheckedChange = { onUpsertMilestone(milestone.copy(isCompleted = it)) })
                                            Text(milestone.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { onDeleteMilestone(milestone) }) {
                                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    TextButton(onClick = { showMilestoneDialog = Milestone(projectId = project.id, title = "") }, modifier = Modifier.padding(start = 8.dp)) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Text("Add Milestone", fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Task Backlog", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    projectWithMilestones.tasks.forEach { task ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                                            Icon(Icons.Default.Assignment, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(task.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { onDeleteProjectTask(task) }) {
                                                Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    TextButton(onClick = { showTaskDialog = ProjectTask(projectId = project.id, title = "") }, modifier = Modifier.padding(start = 8.dp)) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Text("Add Task to Backlog", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (goals.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = Project(goalId = goals.first().goal.id, title = "") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Project")
            }
        }
    }

    showAddDialog?.let { project ->
        EditProjectDialog(
            project = project,
            goals = goals.map { it.goal },
            onDismiss = { showAddDialog = null },
            onConfirm = { 
                onUpsertProject(it)
                showAddDialog = null
            }
        )
    }

    showMilestoneDialog?.let { milestone ->
        EditMilestoneDialog(
            milestone = milestone,
            onDismiss = { showMilestoneDialog = null },
            onConfirm = {
                onUpsertMilestone(it)
                showMilestoneDialog = null
            }
        )
    }

    showTaskDialog?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { showTaskDialog = null },
            onConfirm = {
                onUpsertProjectTask(it)
                showTaskDialog = null
            }
        )
    }
}

@Composable
fun EditTaskDialog(task: ProjectTask, onDismiss: () -> Unit, onConfirm: (ProjectTask) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Backlog Task") },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Description") })
        },
        confirmButton = {
            Button(onClick = { onConfirm(task.copy(title = title)) }, enabled = title.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditMilestoneDialog(milestone: Milestone, onDismiss: () -> Unit, onConfirm: (Milestone) -> Unit) {
    var title by remember { mutableStateOf(milestone.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Milestone") },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Milestone Title") })
        },
        confirmButton = {
            Button(onClick = { onConfirm(milestone.copy(title = title)) }, enabled = title.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditGoalDialog(goal: Goal, onDismiss: () -> Unit, onConfirm: (Goal) -> Unit) {
    var title by remember { mutableStateOf(goal.title) }
    var category by remember { mutableStateOf(goal.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal.id.isEmpty()) "New Goal" else "Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(goal.copy(title = title, category = category)) }, enabled = title.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditProjectDialog(project: Project, goals: List<Goal>, onDismiss: () -> Unit, onConfirm: (Project) -> Unit) {
    var title by remember { mutableStateOf(project.title) }
    var selectedGoalId by remember { mutableStateOf(project.goalId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title") })
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        val g = goals.find { it.id == selectedGoalId }
                        Text("Goal: ${g?.title ?: "Select..."}")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        goals.forEach { goal ->
                            DropdownMenuItem(text = { Text(goal.title) }, onClick = { selectedGoalId = goal.id; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(project.copy(title = title, goalId = selectedGoalId)) }, enabled = title.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RoutinesContent(
    routines: List<QuestWithBlocks>,
    onAdd: () -> Unit,
    onEdit: (QuestWithBlocks) -> Unit,
    onDelete: (QuestWithBlocks) -> Unit,
    onToggle: (QuestWithBlocks) -> Unit,
    onStart: (QuestWithBlocks) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add Routine")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (routines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No routines found. Forge your first routine!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routines) { routine ->
                    QuestItem(
                        quest = routine,
                        onToggle = { onToggle(routine) },
                        onEdit = { onEdit(routine) },
                        onDelete = { onDelete(routine) },
                        onStart = { onStart(routine) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestItem(
    quest: QuestWithBlocks,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quest.quest.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${quest.blocks.size} blocks | ${quest.quest.triggerType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onStart) {
                    Icon(Icons.Default.PlayArrow, "Start", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                }
            }
        }
    }
}
