# POC: Realtime Car Detection für Android

## Ziel
Dieses Repository enthält ein Android-POC mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- Realtime Object Detection über TensorFlow Lite (YOLO-kompatibles `.tflite` Modell)
- Bounding Boxes + Klassenlabel + Confidence im Overlay

## Voraussetzungen für YOLO
- Die CI-Pipeline exportiert YOLO11n vor dem APK-Build nach TensorFlow Lite und bettet `yolo11n.tflite` direkt in die APK ein.
- Beim App-Start kopiert die App das eingebettete Modell aus den APK-Assets nach `files/models/yolo11n.tflite` und lädt genau diese lokale Datei für TensorFlow Lite Task Vision.
- Dadurch ist die Debug-APK nicht von einem kurzlebigen GitHub-Actions-Artifact-Link oder einem Runtime-Download abhängig.
- Bei jedem Start wird die lokale Kopie aus dem eingebetteten APK-Asset neu geschrieben. Damit nutzt eine aktualisierte App auch wirklich das Modell, das mit dieser APK gebaut wurde.

## Lokal starten (Android Studio)
1. Projekt in Android Studio öffnen.
2. Für lokale Builds zuerst ein TFLite-Modell unter `app/src/main/assets/yolo11n.tflite` bereitstellen. Die CI erledigt diesen Schritt automatisch; lokal kannst du die Befehle aus dem Abschnitt **YOLO11n lokal exportieren und einbetten** nutzen.
3. Gradle Sync durchführen.
4. App auf ein Gerät mit Kamera installieren/starten.
5. Beim ersten Start Kameraberechtigung erlauben.

## Aktueller Pipeline-Flow
- GitHub Actions lädt `yolo11n.pt`, exportiert es nach `.tflite` und kopiert das Ergebnis vor dem Android-Build nach `app/src/main/assets/yolo11n.tflite`.
- Die APK enthält dadurch das TFLite-Modell.
- Beim Start kopiert die App das Modell aus den Assets nach `files/models/yolo11n.tflite`.
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

## YOLO-Fehlerlog finden
Wenn die App unten im Kamerabild `Model konnte nicht geladen werden` oder andere YOLO-Debug-Zeilen anzeigt, gibt es zwei einfache Wege an die Details zu kommen:

### Direkt in der App
- Unten im Kamerabild steht ein schwarzes **YOLO Debug**-Panel.
- Dort zeigt die App die letzten Diagnosezeilen an, z. B.:
  - welche APK-Assets sichtbar sind (`APK-Assets sichtbar: ...`)
  - wohin `yolo11n.tflite` kopiert wird (`files/models/yolo11n.tflite`)
  - ob die Datei existiert und wie groß sie ist (`exists=true size=...`)
  - SHA-256 und Datei-Header der kopierten Datei
  - den konkreten `ObjectDetector`-Ladefehler
- Wenn dort `APK-Assets sichtbar` **kein** `yolo11n.tflite` enthält, wurde das Modell wahrscheinlich nicht in die APK eingebettet.
- Wenn `exists=true size=...` angezeigt wird, aber danach `ObjectDetector-Ladefehler` kommt, ist die Datei vorhanden; dann ist sehr wahrscheinlich das TFLite-/Metadata-Format nicht mit TensorFlow Lite Task Vision `ObjectDetector` kompatibel.

### Über Android Studio Logcat
1. Gerät per USB verbinden und App starten.
2. In Android Studio unten **Logcat** öffnen.
3. Als Prozess/Package `com.example.pocspeed` auswählen.
4. Nach `YOLO Debug` oder nach dem Tag `MainActivity` filtern.

### Über adb im Terminal
Mit angeschlossenem Gerät kannst du das YOLO-Log auch direkt im Terminal lesen:

```bash
adb logcat -c
adb shell am force-stop com.example.pocspeed
adb shell monkey -p com.example.pocspeed 1
adb logcat -s MainActivity
```

Falls dein Gerät/Terminal den Tag-Filter anders behandelt, funktioniert als Alternative meistens:

```bash
adb logcat | grep "YOLO Debug"
```

Hilfreiche Zeilen zum Kopieren/Teilen sind besonders:
- `APK-Assets sichtbar: ...`
- `Versuche Asset zu kopieren: ...`
- `Lokale YOLO-Datei bereit: ... size=... path=...`
- `Lokale SHA-256: ...`
- `ObjectDetector-Ladefehler: ...`
- `Erster Inferenzfehler: ...`



## Download
<!-- AUTO-APK-LINK --> [Latest Debug APK](https://github.com/jan-rybizki/poc_speed_measurement/actions/runs/26973548613/artifacts/7420720341)


## Wichtiger Hinweis zu `.pt`
Die Android-App nutzt TensorFlow Lite Task Vision. Ein YOLO-`.pt`-Checkpoint kann nicht direkt mit `ObjectDetector` geladen werden.
Darum wird der `.pt`-Checkpoint in GitHub Actions vor dem APK-Build nach `.tflite` exportiert. Die App lädt zur Laufzeit keinen `.pt`-Checkpoint und konvertiert auch nicht auf dem Handy.


## YOLO11n automatisch in GitHub Actions exportieren und in die APK einbetten
Der Workflow **Android Debug Artifact** baut jetzt App und Modell zusammen:

1. `yolo11n.pt` wird heruntergeladen.
2. YOLO11n wird nach TensorFlow Lite exportiert.
3. Das erzeugte `.tflite` wird als `app/src/main/assets/yolo11n.tflite` in die Android-App kopiert.
4. Danach wird die Debug-APK gebaut.
5. Die Debug-APK enthält damit genau das Modell aus demselben CI-Lauf.

Datei: `.github/workflows/android-artifact.yml`.

Der separate Workflow **Build YOLO11n TFLite** bleibt nützlich, wenn du nur den YOLO-Export testen oder die Modell-Dateien separat als Artifact/Release erzeugen möchtest.

## YOLO11n lokal exportieren und einbetten
Für einen lokalen Android-Studio-Build kannst du das Modell ebenfalls lokal erzeugen:

```bash
python -m pip install --upgrade pip
pip install ultralytics tensorflow
wget -O yolo11n.pt https://github.com/ultralytics/assets/releases/download/v8.3.0/yolo11n.pt
yolo export model=yolo11n.pt format=tflite
mkdir -p app/src/main/assets
model_file="$(find . -path './app/*' -prune -o -name '*.tflite' -print | sort | head -n 1)"
cp "$model_file" app/src/main/assets/yolo11n.tflite
```

`app/src/main/assets/yolo11n.tflite` ist absichtlich in `.gitignore` eingetragen, damit das große generierte Modell nicht ins Repository committed wird.
