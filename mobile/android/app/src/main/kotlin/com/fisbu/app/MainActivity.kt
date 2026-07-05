package com.fisbu.app

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File

class MainActivity : FlutterActivity() {
    private val channelName = "com.fisbu/ocr"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                if (call.method == "recognizeText") {
                    val imagePath = call.argument<String>("imagePath")
                    val file = imagePath?.let { File(it) }
                    if (file == null || !file.exists()) {
                        result.error("INVALID_ARGS", "Geçersiz argüman", null)
                        return@setMethodCallHandler
                    }

                    val image = try {
                        InputImage.fromFilePath(applicationContext, Uri.fromFile(file))
                    } catch (e: Exception) {
                        result.error("INVALID_ARGS", "Geçersiz argüman", null)
                        return@setMethodCallHandler
                    }

                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        .process(image)
                        .addOnSuccessListener { visionText -> result.success(visionText.text) }
                        .addOnFailureListener { e ->
                            result.error("OCR_ERROR", e.localizedMessage, null)
                        }
                } else {
                    result.notImplemented()
                }
            }
    }
}
