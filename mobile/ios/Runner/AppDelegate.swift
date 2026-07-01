import Flutter
import UIKit
import Vision

@main
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GeneratedPluginRegistrant.register(with: self)
    
    let controller = window?.rootViewController as! FlutterViewController
    let channel = FlutterMethodChannel(
      name: "com.fisbu/ocr",
      binaryMessenger: controller.binaryMessenger
    )
    
    channel.setMethodCallHandler { call, result in
      if call.method == "recognizeText" {
        guard let args = call.arguments as? [String: Any],
              let imagePath = args["imagePath"] as? String else {
          result(FlutterError(code: "INVALID_ARGS", message: "imagePath gerekli", details: nil))
          return
        }
        
        guard let image = UIImage(contentsOfFile: imagePath),
              let cgImage = image.cgImage else {
          result(FlutterError(code: "INVALID_IMAGE", message: "Görüntü yüklenemedi", details: nil))
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
    
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
}
