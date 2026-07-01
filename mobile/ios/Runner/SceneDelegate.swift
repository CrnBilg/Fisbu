import Flutter
import UIKit
import Vision

class SceneDelegate: FlutterSceneDelegate {
  override func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
    super.scene(scene, willConnectTo: session, options: connectionOptions)
    
    guard let flutterVC = window?.rootViewController as? FlutterViewController else { return }
    
    let channel = FlutterMethodChannel(
      name: "com.fisbu/ocr",
      binaryMessenger: flutterVC.binaryMessenger
    )
    
    channel.setMethodCallHandler { call, result in
      if call.method == "recognizeText" {
        guard let args = call.arguments as? [String: Any],
              let imagePath = args["imagePath"] as? String,
              let image = UIImage(contentsOfFile: imagePath),
              let cgImage = image.cgImage else {
          result(FlutterError(code: "INVALID_ARGS", message: "Geçersiz argüman", details: nil))
          return
        }
        
        let request = VNRecognizeTextRequest { request, error in
          if let error = error {
            result(FlutterError(code: "OCR_ERROR", message: error.localizedDescription, details: nil))
            return
          }
          let observations = request.results as? [VNRecognizedTextObservation] ?? []
          let text = observations.compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
          result(text)
        }
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["tr", "en"]
        request.usesLanguageCorrection = true
        
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        DispatchQueue.global(qos: .userInitiated).async {
          do {
            try handler.perform([request])
          } catch {
            result(FlutterError(code: "OCR_ERROR", message: error.localizedDescription, details: nil))
          }
        }
      } else {
        result(FlutterMethodNotImplemented)
      }
    }
  }
}
