package com.hllous.plantravel.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.ui.components.SectionCard
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView

@Composable
fun QrScannerScreen(viewModel: MainViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var memberName by rememberSaveable { mutableStateOf("") }
    var scannedText by rememberSaveable { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Escanear QR", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }
        OutlinedTextField(value = memberName, onValueChange = { memberName = it }, label = { Text("Tu nombre") }, modifier = Modifier.fillMaxWidth(), colors = travelTextFieldColors())
        if (!hasCameraPermission) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) {
                Text("Permitir camara")
            }
        } else {
            QrCameraPreview(onQrDetected = { payload ->
                scope.launch {
                    if (scannedText.isNotBlank()) return@launch
                    scannedText = payload
                    val code = payload.removePrefix("PLANTRAVEL|").substringAfterLast("/")
                    viewModel.consumeInvite(code = code, memberName = memberName)
                    onDone()
                }
            })
        }
        if (scannedText.isNotBlank()) {
            SectionCard(title = "QR detectado") {
                Text(scannedText)
            }
        }
    }
}

@Composable
private fun QrCameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val barcodeView = remember {
        CompoundBarcodeView(context).apply {
            barcodeView.decoderFactory = com.journeyapps.barcodescanner.DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        }
    }
    DisposableEffect(Unit) {
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val text = result?.text ?: return
                onQrDetected(text)
            }
        }
        barcodeView.decodeContinuous(callback)
        barcodeView.resume()
        onDispose {
            barcodeView.pause()
        }
    }
    AndroidView(
        factory = { barcodeView },
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    )
}
