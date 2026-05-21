# POC: Realtime Car Detection für Android

## Ziel
Dieses Repository enthält ein Android-POC mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- Realtime Object Detection über TensorFlow Lite (YOLO-kompatibles `.tflite` Modell)
- Bounding Boxes + Klassenlabel + Confidence im Overlay

## Voraussetzungen für YOLO
- Beim ersten Start lädt die App das Modell automatisch nach `files/models/yolo11n.tflite` herunter.
- Standard-URL im Code: `MainActivity.modelDownloadUrl` (Hugging Face).
- Für produktive Nutzung sollte die Datei aus einer eigenen, stabilen Quelle geladen werden und `modelSha256` im Code gesetzt werden (Integritätsprüfung per SHA-256).

## Lokal starten (Android Studio)
1. Projekt in Android Studio öffnen.
2. Gradle Sync durchführen.
3. App auf ein Gerät mit Kamera installieren/starten.
4. Beim ersten Start Kameraberechtigung erlauben.
5. Beim ersten Start mit Internetverbindung wird das Modell automatisch heruntergeladen.

## Aktueller Pipeline-Flow
- CameraX Preview + `ImageAnalysis`
- Pro Frame: Konvertierung `ImageProxy -> Bitmap`
- Inferenz mit TFLite Task Vision `ObjectDetector`
- Zeichnen von Bounding Boxes und Klassen in `OverlayView`
