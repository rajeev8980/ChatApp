package com.example.chatapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatapp.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onGroupCreated: (String, String) -> Unit,
    onBack: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val users by vm.users.collectAsState()
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("Select members (${selected.size})")
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(users, key = { it.uid }) { user ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clickable {
                                selected = if (selected.contains(user.uid)) selected - user.uid
                                else selected + user.uid
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected.contains(user.uid),
                            onCheckedChange = {
                                selected = if (it) selected + user.uid else selected - user.uid
                            }
                        )
                        Text(user.displayName.ifBlank { user.email })
                    }
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val id = vm.createGroup(name, selected.toList())
                            onGroupCreated(id, name)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && name.isNotBlank() && selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator() else Text("Create group")
            }
        }
    }
}
