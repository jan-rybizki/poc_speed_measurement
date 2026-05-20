# POC: Realtime Car Detection für Android

## Ziel
Dieses Repository enthält ein Android-POC mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- Realtime Object Detection über TensorFlow Lite (YOLO-kompatibles `.tflite` Modell)
- Bounding Boxes + Klassenlabel + Confidence im Overlay

## Voraussetzungen für YOLO
Lege dein TFLite-Modell unter folgendem Pfad ab:
- `app/src/main/assets/yolo11n.tflite`

> Hinweis: Das Modell ist **nicht** im Repo enthalten. Ohne diese Datei läuft die Kamera weiter, aber ohne Detections.

## Lokal starten (Android Studio)
1. Projekt in Android Studio öffnen.
2. Gradle Sync durchführen.
3. Falls noch nicht vorhanden: `yolo11n.tflite` nach `app/src/main/assets/` kopieren.
4. App auf ein Gerät mit Kamera installieren/starten.
5. Beim ersten Start Kameraberechtigung erlauben.

## Aktueller Pipeline-Flow
- CameraX Preview + `ImageAnalysis`
- Pro Frame: Konvertierung `ImageProxy -> Bitmap`
- Inferenz mit TFLite Task Vision `ObjectDetector`
- Zeichnen von Bounding Boxes und Klassen in `OverlayView`
