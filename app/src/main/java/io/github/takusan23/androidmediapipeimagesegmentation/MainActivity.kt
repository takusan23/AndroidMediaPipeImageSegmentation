package io.github.takusan23.androidmediapipeimagesegmentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import io.github.takusan23.androidmediapipeimagesegmentation.ui.theme.AndroidMediaPipeImageSegmentationTheme
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidMediaPipeImageSegmentationTheme {
                ImageSegmentationScreen()
            }
        }
    }
}

@Composable
private fun ImageSegmentationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaPipeImageSegmentation = remember { MediaPipeImageSegmentation(context) }

    val inputBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    val segmentedBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            // 推論はコルーチンでやる
            scope.launch {
                // Bitmap を取得。Glide や Coil が使えるならそっちで取得したほうが良いです
                val bitmap = context.contentResolver.openInputStream(uri)
                    .use { BitmapFactory.decodeStream(it) }

                // 推論
                val resultBitmap: Bitmap
                val time = measureTimeMillis {
                    resultBitmap = mediaPipeImageSegmentation.segmentation(bitmap)
                }
                println("time = $time")

                // UI に表示
                inputBitmap.value = bitmap.asImageBitmap()
                segmentedBitmap.value = resultBitmap.asImageBitmap()
            }
        }
    )

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            Button(onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(text = "写真を選ぶ") }

            if (inputBitmap.value != null) {
                Image(
                    bitmap = inputBitmap.value!!,
                    contentDescription = null
                )
            }
            if (segmentedBitmap.value != null) {
                Image(
                    bitmap = segmentedBitmap.value!!,
                    contentDescription = null
                )
            }
        }
    }
}