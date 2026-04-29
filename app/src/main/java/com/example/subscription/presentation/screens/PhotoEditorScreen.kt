package com.example.subscription.presentation.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscription.presentation.PhotoEditorState
import com.example.subscription.presentation.PhotoEditorViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PhotoEditorScreen(
    viewModel: PhotoEditorViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val bitmap by viewModel.filteredBitmap.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("Опиши это изображение кратко") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            bmp?.let { viewModel.setOriginal(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AI Photo Editor",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image preview
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Selected photo",
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Text(
                text = "Выберите фото для редактирования",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Photo picker button
        Button(
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выбрать фото")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local filters
        if (bitmap != null) {
            Text(
                text = "Локальные фильтры",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterButton("Grayscale") { viewModel.applyGrayscale() }
                FilterButton("Sepia") { viewModel.applySepia() }
                FilterButton("Bright") { viewModel.applyBrightness() }
                FilterButton("Contrast") { viewModel.applyContrast() }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.reset() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сбросить фильтры")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Section
        if (bitmap != null) {
            Text(
                text = "Gemini AI",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Промпт для Gemini") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.analyzeWithAi(prompt) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отправить в Gemini")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI State
        when (val s = state) {
            is PhotoEditorState.Loading -> {
                CircularProgressIndicator()
            }
            is PhotoEditorState.AiResult -> {
                Text(
                    text = "Ответ Gemini:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = s.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            is PhotoEditorState.Error -> {
                Text(
                    text = "Ошибка: ${s.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun FilterButton(
    label: String,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
