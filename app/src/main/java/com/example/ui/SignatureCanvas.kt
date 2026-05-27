package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun SignatureCanvas(
    label: String,
    onSignatureChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDrawingStateChanged: (Boolean) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    var signatureBase64 by remember { mutableStateOf("") }
    
    // Line path details for Jetpack Compose UI rendering
    val currentPath = remember { mutableStateOf<ComposePath?>(null) }
    val pathsList = remember { mutableStateListOf<ComposePath>() }
    // Trigger to force recomposition elegantly without setting null
    var redrawTrigger by remember { mutableStateOf(0) }

    // Width and height of the signature canvas in pixels
    val canvasWidthPx = 400
    val canvasHeightPx = 180

    // Off-screen Bitmap and Canvas to render the high quality PNG
    val signatureBitmap = remember {
        Bitmap.createBitmap(canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888)
    }
    val androidCanvas = remember {
        Canvas(signatureBitmap).apply {
            drawColor(android.graphics.Color.WHITE) // White backdrop for signatures
        }
    }
    val androidPaint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
            strokeWidth = 6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
    val androidPath = remember { Path() }

    fun exportToBase64() {
        // Redraw full path once on offscreen bitmap before exporting
        androidCanvas.drawColor(android.graphics.Color.WHITE)
        androidCanvas.drawPath(androidPath, androidPaint)

        val bitmapCopy = try {
            signatureBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            null
        }
        if (bitmapCopy == null) return

        coroutineScope.launch {
            val base64Uri = withContext(Dispatchers.IO) {
                try {
                    val stream = ByteArrayOutputStream()
                    bitmapCopy.compress(Bitmap.CompressFormat.PNG, 90, stream)
                    val byteArray = stream.toByteArray()
                    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    "data:image/png;base64,$base64"
                } catch (e: Exception) {
                    ""
                } finally {
                    try {
                        bitmapCopy.recycle()
                    } catch (swallowed: Exception) {}
                }
            }
            if (base64Uri.isNotEmpty()) {
                signatureBase64 = base64Uri
                onSignatureChanged(base64Uri)
            }
        }
    }

    fun clearSignature() {
        pathsList.clear()
        currentPath.value = null
        androidPath.reset()
        androidCanvas.drawColor(android.graphics.Color.WHITE)
        signatureBase64 = ""
        onSignatureChanged("")
        redrawTrigger++
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
        ) {
            ComposeCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        try {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                onDrawingStateChanged(true)
                                try {
                                    view.parent?.requestDisallowInterceptTouchEvent(true)
                                } catch (t: Throwable) {}

                                val componentWidth = size.width.toFloat()
                                val componentHeight = size.height.toFloat()
                                val scaleX = if (componentWidth > 0f) canvasWidthPx.toFloat() / componentWidth else 1f
                                val scaleY = if (componentHeight > 0f) canvasHeightPx.toFloat() / componentHeight else 1f

                                val path = ComposePath().apply {
                                    moveTo(down.position.x, down.position.y)
                                }
                                currentPath.value = path
                                androidPath.moveTo(down.position.x * scaleX, down.position.y * scaleY)
                                redrawTrigger++

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.firstOrNull { it.id == down.id }
                                    if (dragChange == null || !dragChange.pressed) {
                                        break
                                    }

                                    val endOffset = dragChange.position
                                    currentPath.value?.let { p ->
                                        p.lineTo(endOffset.x, endOffset.y)
                                        redrawTrigger++
                                    }

                                    androidPath.lineTo(endOffset.x * scaleX, endOffset.y * scaleY)
                                    dragChange.consume()
                                }

                                try {
                                    view.parent?.requestDisallowInterceptTouchEvent(false)
                                } catch (t: Throwable) {}

                                currentPath.value?.let { p ->
                                    pathsList.add(p)
                                }
                                currentPath.value = null
                                exportToBase64()
                                onDrawingStateChanged(false)
                                redrawTrigger++
                            }
                        } catch (ce: java.util.concurrent.CancellationException) {
                            throw ce
                        } catch (t: Throwable) {
                            try {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            } catch (extra: Throwable) {}
                            onDrawingStateChanged(false)
                        }
                    }
            ) {
                // Register dependency on redrawTrigger to force ComposeCanvas update
                val trigger = redrawTrigger
                
                // Render finished paths
                pathsList.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color(0xFF1E293B),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                // Render current dragging path
                currentPath.value?.let { path ->
                    drawPath(
                        path = path,
                        color = Color(0xFF1E293B),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                
                // Overlay friendly guide to draw sign
                if (pathsList.isEmpty() && currentPath.value == null) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "Firme aquí",
                        size.width / 2f - 40f,
                        size.height / 2f + 5f,
                        Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 34f
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Floating Clean Canvas button
            IconButton(
                onClick = { clearSignature() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(100.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpiar Firma",
                    tint = Color(0xFF475569)
                )
            }
        }
    }
}
