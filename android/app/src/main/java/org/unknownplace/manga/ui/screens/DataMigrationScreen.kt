package org.unknownplace.manga.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable

@Serializable
object DataMigration

@Composable
fun DataMigrationScreen(
    viewModel: DataMigrationViewModel = viewModel(),
    onFinishMigration: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(true) {
        viewModel.migrate()
    }
    
    LaunchedEffect(uiState.finished) {
        if (uiState.finished) {
            onFinishMigration()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (uiState.migrating) {
            CircularProgressIndicator()
            Text(text = "Upgrading database...")
        }
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { /*TODO*/ },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetAndMigrate()
                    }) {
                        Text(text = "Reset database")
                    }
                },
                title = {
                    Text(text = "Data migration error")
                },
                text = {
                    Text(text = "Failed to upgrade database. Need to reset.")
                },
            )
        }
    }
}