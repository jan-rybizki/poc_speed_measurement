# POC: Realtime Car Detection für Android

## Ziel
Dieses Repository enthält ein Android-POC mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- Realtime Object Detection über TensorFlow Lite (YOLO-kompatibles `.tflite` Modell)
- Bounding Boxes + Klassenlabel + Confidence im Overlay

## Voraussetzungen für YOLO
- Beim ersten Start lädt die App ein TFLite-Laufzeitmodell nach `files/models/yolo11n.tflite` herunter.
- Standard-URL im Code: `MainActivity.modelDownloadUrl` (angefragte `.pt`-Quelle). Für die App-Laufzeit wird automatisch eine kompatible `.tflite`-Datei genutzt.
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
<!-- AUTO-APK-LINK --> [Latest Debug APK](https://github.com/jan-rybizki/poc_speed_measurement/actions/runs/26845439793/artifacts/7368798728)


## Wichtiger Hinweis zu `.pt`
Die Android-App nutzt TensorFlow Lite Task Vision. Ein YOLO-`.pt`-Checkpoint kann nicht direkt mit `ObjectDetector` geladen werden.
Darum mappt die App die angefragte YOLO11-`.pt`-URL intern auf den offiziellen YOLO11n-`float32.tflite`-Export. Du musst **nicht** lokal auf dem Handy konvertieren.


**Nur Handy / kein Python lokal?** Kein Problem: Gib einfach einen Online-`.tflite`-Link (oder die bekannte YOLO11n-`.pt`-URL, die intern gemappt wird) in den Code ein und starte die App.


## YOLO11n automatisch in GitHub Actions exportieren
Du kannst die Konvertierung komplett online in GitHub Actions laufen lassen (kein lokales Python nötig):

1. Öffne **Actions** → **Build YOLO11n TFLite**.
2. Starte den Workflow per **Run workflow**.
3. Optional: `create_release=true`, dann wird zusätzlich ein GitHub Release mit den Artefakten erstellt.

Der Workflow erzeugt:
- `*.tflite` Export(e)
- `SHA256SUMS.txt`
- Original `yolo11n.pt`

Datei: `.github/workflows/build-yolo11n-tflite.yml`.
