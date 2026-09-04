package com.example.musicgenapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : ComponentActivity() {

    private val viewModel: GenerateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MelodyForgeScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MelodyForgeScreen(viewModel: GenerateViewModel) {
    var prompt by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(15f) }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Describe the music (mood, genre, instruments, tempo)") },
            placeholder = { Text("e.g. dreamy lo-fi with soft piano and rain, 80bpm") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.height(16.dp))
        Text("Duration: ${duration.toInt()}s")
        Slider(
            value = duration,
            onValueChange = { duration = it },
            valueRange = 5f..30f
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.generate(prompt, duration.toInt()) },
            enabled = state !is UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state is UiState.Loading) "Generating..." else "Generate")
        }

        Spacer(Modifier.height(24.dp))

        when (val s = state) {
            is UiState.Idle -> {}
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is UiState.Blocked -> {
                Text(
                    "Can't generate that: ${s.reason}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            is UiState.Failed -> {
                Text(
                    "Something went wrong: ${s.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            is UiState.Ready -> {
                Text("Done! Saved to app storage.")
                Spacer(Modifier.height(8.dp))
                PlaybackControls(filePath = s.filePath)
            }
        }
    }
}

@Composable
fun PlaybackControls(filePath: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(java.io.File(filePath))))
            prepare()
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Row {
        Button(onClick = { exoPlayer.play() }) { Text("Play") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = { exoPlayer.pause() }) { Text("Pause") }
    }
    Spacer(Modifier.height(8.dp))
    Text("Saved at: $filePath", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun stringResource(id: Int): String {
    return androidx.compose.ui.platform.LocalContext.current.getString(id)
}
