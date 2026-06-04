package com.example.pocspeed

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.io.BufferedInputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val DEBUG_PANEL_LINE_COUNT = 18
    }

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var fpsTextView: TextView
    private lateinit var modelDebugTextView: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val modelExecutor = Executors.newSingleThreadExecutor()

    private var frameCounter = 0
    private var lastFpsTimestampMs = 0L
    private val isProcessing = AtomicBoolean(false)

    private var objectDetector: ObjectDetector? = null
    @Volatile
    private var modelStatusText = "YOLO: startet"
    private val frameFailureLogged = AtomicBoolean(false)
    private val modelDebugLines = mutableListOf<String>()

    // Runtime model for TFLite Task Vision. The CI build embeds this asset into the APK.
    private val modelFileName = "yolo11n.tflite"
    private val embeddedModelAssetName = modelFileName

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                fpsTextView.text = "FPS: Kamera-Rechte fehlen"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        fpsTextView = findViewById(R.id.fpsTextView)
        modelDebugTextView = findViewById(R.id.modelDebugTextView)
        lastFpsTimestampMs = System.currentTimeMillis()

        prepareModelAndStart()
    }

    private fun prepareModelAndStart() {
        addModelDebug("Start: suche YOLO Asset '$embeddedModelAssetName'")
        modelExecutor.execute {
            val modelFile = ensureModelFile()
            objectDetector = createObjectDetector(modelFile)

            runOnUiThread {
                updateFpsText()
                if (hasCameraPermission()) {
                    startCamera()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    private fun ensureModelFile(): File? {
        val modelDir = File(filesDir, "models")
        addModelDebug("App filesDir: ${filesDir.absolutePath}")
        addModelDebug("Model-Zielordner: ${modelDir.absolutePath}")

        if (!modelDir.exists()) {
            val created = modelDir.mkdirs()
            addModelDebug("Model-Zielordner angelegt: $created")
        }

        logVisibleModelFiles(modelDir)

        val targetFile = File(modelDir, modelFileName)
        if (targetFile.exists()) {
            addModelDebug("Alte lokale Kopie wird ersetzt: ${describeFile(targetFile)}")
            targetFile.delete()
        }

        runOnUiThread {
            fpsTextView.text = "FPS: Lade eingebettetes Modell..."
        }

        return try {
            copyEmbeddedModel(targetFile)
            if (targetFile.exists() && targetFile.length() > 0L) {
                addModelDebug("Lokale YOLO-Datei bereit: ${describeFile(targetFile)}")
                addModelDebug("Lokale SHA-256: ${sha256(targetFile)}")
                addModelDebug("Datei-Header: ${fileHeaderHex(targetFile)}")
                logEmbeddedHashIfPresent()
                targetFile
            } else {
                addModelDebug("Fehler: lokale Modellkopie fehlt oder ist leer: ${describeFile(targetFile)}")
                targetFile.delete()
                runOnUiThread {
                    fpsTextView.text = "FPS: Eingebettetes Modell leer"
                }
                null
            }
        } catch (e: Exception) {
            targetFile.delete()
            addModelDebug("Fehler beim Kopieren des eingebetteten Modells: ${e.message ?: e.javaClass.simpleName}", e)
            runOnUiThread {
                fpsTextView.text = "FPS: Eingebettetes Modell fehlt (${e.message ?: "Unbekannter Fehler"})"
            }
            null
        }
    }

    private fun copyEmbeddedModel(targetFile: File) {
        val rootAssets = assets.list("")?.sorted().orEmpty()
        addModelDebug("APK-Assets sichtbar: ${rootAssets.joinToString().ifBlank { "<keine>" }}")
        addModelDebug("Versuche Asset zu kopieren: $embeddedModelAssetName -> ${targetFile.absolutePath}")

        assets.open(embeddedModelAssetName).use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun logVisibleModelFiles(modelDir: File) {
        val files = modelDir.listFiles()
            ?.sortedBy { it.name }
            ?.joinToString { describeFile(it) }
            ?: "<Ordner nicht lesbar>"
        addModelDebug("Lokale Model-Dateien vorher: ${files.ifBlank { "<keine>" }}")
    }

    private fun logEmbeddedHashIfPresent() {
        val hashAssetName = "$embeddedModelAssetName.sha256"
        try {
            assets.open(hashAssetName).bufferedReader().use { reader ->
                addModelDebug("Eingebettete SHA-Datei: ${reader.readText().trim()}")
            }
        } catch (e: Exception) {
            addModelDebug("Keine eingebettete SHA-Datei '$hashAssetName' gefunden (${e.message ?: e.javaClass.simpleName})")
        }
    }

    private fun describeFile(file: File): String {
        return "${file.name} exists=${file.exists()} size=${file.length()} path=${file.absolutePath}"
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(file.inputStream()).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun fileHeaderHex(file: File, byteCount: Int = 16): String {
        val bytes = ByteArray(byteCount)
        val read = file.inputStream().use { it.read(bytes) }
        if (read <= 0) return "<leer>"
        return bytes.take(read).joinToString(" ") { "%02x".format(it) }
    }

    private fun addModelDebug(message: String, throwable: Throwable? = null) {
        val line = "YOLO Debug: $message"
        Log.d(TAG, line, throwable)

        synchronized(modelDebugLines) {
            modelDebugLines += line
            while (modelDebugLines.size > 30) {
                modelDebugLines.removeAt(0)
            }
        }

        runOnUiThread {
            val lines = synchronized(modelDebugLines) { modelDebugLines.takeLast(DEBUG_PANEL_LINE_COUNT).toList() }
            modelDebugTextView.text = lines.joinToString("\n")
        }
    }

    private fun createObjectDetector(modelFile: File?): ObjectDetector? {
        if (modelFile == null) {
            addModelDebug("Kein ModelFile vorhanden; ObjectDetector wird nicht erstellt.")
            modelStatusText = "YOLO: Model-Datei fehlt"
            return null
        }

        return try {
            addModelDebug("Versuche ObjectDetector zu laden: ${modelFile.absolutePath}")
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(0.4f)
                .build()
            ObjectDetector.createFromFileAndOptions(this, modelFile.absolutePath, options).also {
                modelStatusText = "YOLO: geladen"
                addModelDebug("ObjectDetector erfolgreich geladen.")
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: e.javaClass.simpleName
            modelStatusText = "YOLO: Ladefehler"
            addModelDebug("ObjectDetector-Ladefehler: $errorMessage", e)
            addModelDebug("Hinweis: Datei ist da; meist passt das TFLite/Metadata-Format nicht zum Task Vision ObjectDetector.")
            runOnUiThread {
                updateFpsText()
            }
            null
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        updateFps()

        val detector = objectDetector
        if (detector == null || isProcessing.getAndSet(true)) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results = detector.detect(tensorImage)

            runOnUiThread {
                overlayView.setResults(results, bitmap.width, bitmap.height)
            }
        } catch (e: Exception) {
            if (frameFailureLogged.compareAndSet(false, true)) {
                modelStatusText = "YOLO: Inferenzfehler"
                addModelDebug("Erster Inferenzfehler: ${e.message ?: e.javaClass.simpleName}", e)
                runOnUiThread {
                    updateFpsText()
                }
            }
        } finally {
            isProcessing.set(false)
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val nv21 = yuv420888ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)

        val chromaRowStride = image.planes[1].rowStride
        val chromaPixelStride = image.planes[1].pixelStride
        var offset = ySize

        if (chromaPixelStride == 2 && chromaRowStride == image.width) {
            val vBytes = ByteArray(vSize)
            val uBytes = ByteArray(uSize)
            vBuffer.get(vBytes)
            uBuffer.get(uBytes)
            for (i in 0 until uSize step 2) {
                nv21[offset++] = vBytes[i]
                nv21[offset++] = uBytes[i]
            }
        } else {
            val width = image.width
            val height = image.height
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val uData = ByteArray(uPlane.buffer.remaining())
            val vData = ByteArray(vPlane.buffer.remaining())
            uPlane.buffer.get(uData)
            vPlane.buffer.get(vData)

            for (row in 0 until height / 2) {
                for (col in 0 until width / 2) {
                    val vuPos = row * uPlane.rowStride + col * uPlane.pixelStride
                    nv21[offset++] = vData[vuPos]
                    nv21[offset++] = uData[vuPos]
                }
            }
        }

        return nv21
    }

    private fun updateFps() {
        frameCounter++
        val nowMs = System.currentTimeMillis()
        val elapsedMs = nowMs - lastFpsTimestampMs

        if (elapsedMs >= 1000) {
            val fps = (frameCounter * 1000f) / elapsedMs
            frameCounter = 0
            lastFpsTimestampMs = nowMs

            runOnUiThread {
                updateFpsText(fps)
            }
        }
    }

    private fun updateFpsText(fps: Float? = null) {
        val fpsLine = fps?.let { String.format(Locale.US, "FPS: %.1f", it) }
            ?: fpsTextView.text.toString().lineSequence().firstOrNull()
            ?: "FPS: --"
        fpsTextView.text = "$fpsLine\n$modelStatusText"
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        modelExecutor.shutdown()
    }
}
