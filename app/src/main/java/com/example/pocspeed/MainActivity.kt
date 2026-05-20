package com.example.pocspeed

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.widget.TextView
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
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var fpsTextView: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var frameCounter = 0
    private var lastFpsTimestampMs = 0L
    private val isProcessing = AtomicBoolean(false)

    private var objectDetector: ObjectDetector? = null

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
        lastFpsTimestampMs = System.currentTimeMillis()

        objectDetector = createObjectDetector()

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createObjectDetector(): ObjectDetector? {
        return try {
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(0.4f)
                .build()
            ObjectDetector.createFromFileAndOptions(this, "yolo11n.tflite", options)
        } catch (_: Exception) {
            runOnUiThread {
                fpsTextView.text = "FPS: -- (Model yolo11n.tflite fehlt in app/src/main/assets)"
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
        } catch (_: Exception) {
            // Ignore single frame failures.
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
                fpsTextView.text = String.format(Locale.US, "FPS: %.1f", fps)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
