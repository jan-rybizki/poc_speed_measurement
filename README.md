# POC: Realtime Object Detection für Android

## Ziel
Dieses Repository enthält ein Android-POC mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- Realtime Object Detection über TensorFlow Lite (YOLO-kompatibles `.tflite` Modell)
- Bounding Boxes + Klassenlabel + Confidence im Overlay

Die App erkennt alle Klassen, die das eingebettete Modell liefert; sie filtert
die Ergebnisse derzeit nicht auf Autos. Trotz des Repository-Namens misst der
aktuelle Stand noch keine Geschwindigkeit: Objekt-Tracking, Kamera- bzw.
Perspektivkalibrierung und die Umrechnung einer Bewegung in km/h oder m/s sind
nicht implementiert.

## Voraussetzungen für YOLO
- Die CI-Pipeline exportiert YOLO11n vor dem APK-Build nach TensorFlow Lite und bettet `yolo11n.tflite` direkt in die APK ein.
- Beim App-Start kopiert die App das eingebettete Modell aus den APK-Assets nach `files/models/yolo11n.tflite` und lädt genau diese lokale Datei für TensorFlow Lite Task Vision.
- Dadurch ist die Debug-APK nicht von einem kurzlebigen GitHub-Actions-Artifact-Link oder einem Runtime-Download abhängig.
- Bei jedem Start wird die lokale Kopie aus dem eingebetteten APK-Asset neu geschrieben. Damit nutzt eine aktualisierte App auch wirklich das Modell, das mit dieser APK gebaut wurde.
- Nicht jede durch einen YOLO-Exporter erzeugte `.tflite`-Datei ist automatisch mit dem TensorFlow Lite Task Vision `ObjectDetector` kompatibel. Das Modell muss dessen erwartete Ein-/Ausgaben und Metadaten besitzen. Die aktuelle Pipeline bettet das Exportergebnis ein, führt aber noch keinen Laufzeittest mit `ObjectDetector` aus.

## Lokal starten (Android Studio)
1. Projekt in Android Studio öffnen.
2. Für lokale Builds zuerst ein kompatibles TFLite-Modell unter `app/src/main/assets/yolo11n.tflite` bereitstellen. Die Modelldatei wird absichtlich nicht eingecheckt; ein frischer Checkout kann daher zwar gebaut werden, besitzt ohne diesen Schritt aber keine funktionierende Objekterkennung. Die CI erzeugt und kopiert die Datei automatisch; lokal kannst du die Befehle aus dem Abschnitt **YOLO11n lokal exportieren und einbetten** nutzen.
3. Gradle Sync durchführen.
4. App auf ein Gerät mit Kamera installieren/starten.
5. Beim ersten Start Kameraberechtigung erlauben.

Die binäre `gradle-wrapper.jar` wird in diesem Repository nicht versioniert.
Android Studio bringt die nötige Gradle-Unterstützung mit. Wer `./gradlew`
außerhalb von Android Studio verwendet, benötigt deshalb eine systemweit
installierte `gradle`-Version; das Skript fällt automatisch darauf zurück.

## Aktueller Pipeline-Flow
- GitHub Actions lädt `yolo11n.pt`, exportiert es nach `.tflite` und kopiert das Ergebnis vor dem Android-Build nach `app/src/main/assets/yolo11n.tflite`.
- Die APK enthält dadurch das TFLite-Modell.
- Beim Start kopiert die App das Modell aus den Assets nach `files/models/yolo11n.tflite`.
- CameraX Preview + `ImageAnalysis`
- Pro zur Inferenz angenommenem Frame: Konvertierung `ImageProxy -> Bitmap`
- Inferenz mit TFLite Task Vision `ObjectDetector`
- Zeichnen von Bounding Boxes und Klassen in `OverlayView`

CameraX verwirft bei Rückstau ältere Frames, und während einer laufenden
Inferenz wird kein zweiter Frame verarbeitet. Die eingeblendete FPS-Zahl zählt
die Aufrufe des Image-Analyzers; sie ist deshalb keine Messung der erfolgreich
abgeschlossenen YOLO-Inferenzen pro Sekunde.


## Stabiler Dev-Install-Workflow (verhindert Install-Fehler bei Updates)
Für den reinen Dev-Workflow gibt es ein Install-Skript, das zuerst ein normales Update versucht (`adb install -r`) und bei typischen Update-Konflikten automatisch deinstalliert und neu installiert.

1. USB-Debugging aktivieren und Gerät verbinden.
2. Im Projektroot ausführen:
   `./scripts/dev-install.sh`

Das Skript baut und installiert die APK, verändert dabei aber keine
Repository-Dateien. Bei `INSTALL_FAILED_UPDATE_INCOMPATIBLE` oder
`INSTALL_FAILED_VERSION_DOWNGRADE` deinstalliert es die vorhandene App und
versucht danach eine frische Installation. Andere Fehler, beispielsweise eine
ungültige APK, werden ausgegeben, ohne die installierte App zu entfernen.

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
Die CI stellt die Debug-APK als zeitlich begrenztes GitHub-Actions-Artifact
bereit. Den jeweils verfügbaren Build findest du in den
[Workflow-Läufen von Android Debug Artifact](https://github.com/jan-rybizki/poc_speed_measurement/actions/workflows/android-artifact.yml).
Ein bestimmter Artifact-Link ist nicht dauerhaft und wird deshalb hier nicht
als stabiler „Latest“-Download veröffentlicht.


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

> **Kompatibilitätshinweis:** Ein erfolgreicher `yolo export` und Android-Build
> beweisen noch nicht, dass Task Vision das Modell laden kann. Prüfe nach dem
> Export auf einem Gerät das Debug-Panel auf `ObjectDetector erfolgreich
> geladen.`. Bei `ObjectDetector-Ladefehler` muss das Modell in einem für Task
> Vision geeigneten Format samt Metadaten bereitgestellt werden.

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
