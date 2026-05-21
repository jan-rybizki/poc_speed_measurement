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


## Stabiler Dev-Install-Workflow (verhindert Install-Fehler bei Updates)
Für den reinen Dev-Workflow gibt es ein Install-Skript, das zuerst ein normales Update versucht (`adb install -r`) und bei typischen Update-Konflikten automatisch deinstalliert und neu installiert.

1. USB-Debugging aktivieren und Gerät verbinden.
2. Im Projektroot ausführen:
   `./scripts/dev-install.sh`

Das reduziert "Install failed" im Alltag deutlich, auch wenn z. B. eine inkompatible Alt-Installation auf dem Gerät liegt.


## Download
<!-- AUTO-APK-LINK --> [Latest Debug APK](https://github.com/jan-rybizki/poc_speed_measurement/actions/runs/26243711187/artifacts/7143414783)
