# POC: Realtime Object Detection für Android

## Ziel
Dieses Repository enthält ein Android-POC mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- Realtime Object Detection über TensorFlow Lite Task Vision (EfficientDet Lite0)
- Bounding Boxes + Klassenlabel + Confidence im Overlay

Die App erkennt alle Klassen, die das eingebettete Modell liefert; sie filtert
die Ergebnisse derzeit nicht auf Autos. Trotz des Repository-Namens misst der
aktuelle Stand noch keine Geschwindigkeit: Objekt-Tracking, Kamera- bzw.
Perspektivkalibrierung und die Umrechnung einer Bewegung in km/h oder m/s sind
nicht implementiert.

## Voraussetzungen für das Erkennungsmodell
- Die CI-Pipeline lädt das bereits für TensorFlow Lite Task Vision aufbereitete EfficientDet-Lite0-Modell und bettet `efficientdet-lite0.tflite` direkt in die APK ein.
- Beim App-Start kopiert die App das eingebettete Modell zu Diagnosezwecken aus den APK-Assets nach `files/models/efficientdet-lite0.tflite` und prüft dort Größe, Header und SHA-256. TensorFlow Lite Task Vision lädt das Modell über seinen APK-Asset-Namen, wie von `ObjectDetector.createFromFileAndOptions` erwartet.
- Dadurch ist die Debug-APK nicht von einem kurzlebigen GitHub-Actions-Artifact-Link oder einem Runtime-Download abhängig.
- Bei jedem Start wird die lokale Kopie aus dem eingebetteten APK-Asset neu geschrieben. Damit nutzt eine aktualisierte App auch wirklich das Modell, das mit dieser APK gebaut wurde.
- Der vorherige YOLO11n-Export wurde bewusst entfernt: Er installierte bei jedem APK-Build eine große Python-/TensorFlow-Toolchain und sein rohes Exportformat war nicht verlässlich mit `ObjectDetector` kompatibel. EfficientDet Lite0 enthält die von Task Vision erwarteten Metadaten.

## Lokal starten (Android Studio)
1. Projekt in Android Studio öffnen.
2. Für lokale Builds zuerst das TFLite-Modell unter `app/src/main/assets/efficientdet-lite0.tflite` bereitstellen. Die Modelldatei wird absichtlich nicht eingecheckt; ein frischer Checkout kann daher zwar gebaut werden, besitzt ohne diesen Schritt aber keine funktionierende Objekterkennung. Die CI lädt und kopiert die Datei automatisch; lokal kannst du die Befehle aus dem Abschnitt **Modell lokal herunterladen und einbetten** nutzen.
3. Gradle Sync durchführen.
4. App auf ein Gerät mit Kamera installieren/starten.
5. Beim ersten Start Kameraberechtigung erlauben.

Die binäre `gradle-wrapper.jar` wird in diesem Repository nicht versioniert.
Android Studio bringt die nötige Gradle-Unterstützung mit. Wer `./gradlew`
außerhalb von Android Studio verwendet, benötigt deshalb eine systemweit
installierte `gradle`-Version; das Skript fällt automatisch darauf zurück.

## Aktueller Pipeline-Flow
- GitHub Actions lädt EfficientDet Lite0 direkt als `.tflite`, prüft Mindestgröße und TFLite-Dateikennung und legt es vor dem Android-Build unter `app/src/main/assets/efficientdet-lite0.tflite` ab.
- Die APK enthält dadurch das TFLite-Modell.
- Beim Start prüft die App das Modell über eine Kopie unter `files/models/efficientdet-lite0.tflite` und übergibt anschließend den APK-Asset-Namen an Task Vision.
- CameraX Preview + `ImageAnalysis`
- Pro zur Inferenz angenommenem Frame: Konvertierung `ImageProxy -> Bitmap`
- Inferenz mit TFLite Task Vision `ObjectDetector`
- Zeichnen von Bounding Boxes und Klassen in `OverlayView`

CameraX verwirft bei Rückstau ältere Frames, und während einer laufenden
Inferenz wird kein zweiter Frame verarbeitet. Die eingeblendete FPS-Zahl zählt
die Aufrufe des Image-Analyzers; sie ist deshalb keine Messung der erfolgreich
abgeschlossenen Modell-Inferenzen pro Sekunde.


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

## Modell-Fehlerlog finden
Wenn die App unten im Kamerabild `Model konnte nicht geladen werden` oder andere Modell-Debug-Zeilen anzeigt, gibt es zwei einfache Wege an die Details zu kommen:

### Direkt in der App
- Unten im Kamerabild steht ein schwarzes **Detektor Debug**-Panel.
- Dort zeigt die App die letzten Diagnosezeilen an, z. B.:
  - welche APK-Assets sichtbar sind (`APK-Assets sichtbar: ...`)
  - wohin `efficientdet-lite0.tflite` kopiert wird (`files/models/efficientdet-lite0.tflite`)
  - ob die Datei existiert und wie groß sie ist (`exists=true size=...`)
  - SHA-256 und Datei-Header der kopierten Datei
  - den konkreten `ObjectDetector`-Ladefehler
- Wenn dort `APK-Assets sichtbar` **kein** `efficientdet-lite0.tflite` enthält, wurde das Modell wahrscheinlich nicht in die APK eingebettet.
- Wenn `exists=true size=...` angezeigt wird, aber danach `ObjectDetector-Ladefehler` kommt, ist die Datei vorhanden; dann ist sehr wahrscheinlich das TFLite-/Metadata-Format nicht mit TensorFlow Lite Task Vision `ObjectDetector` kompatibel.
- Das Modell muss bei einer über GitHub Actions erzeugten APK nicht händisch auf das Android-Gerät kopiert werden. Die Zeile `APK-Assets sichtbar: efficientdet-lite0.tflite` bestätigt, dass es bereits Bestandteil der APK ist.

### Über Android Studio Logcat
1. Gerät per USB verbinden und App starten.
2. In Android Studio unten **Logcat** öffnen.
3. Als Prozess/Package `com.example.pocspeed` auswählen.
4. Nach `Detektor Debug` oder nach dem Tag `MainActivity` filtern.

### Über adb im Terminal
Mit angeschlossenem Gerät kannst du das Modell-Log auch direkt im Terminal lesen:

```bash
adb logcat -c
adb shell am force-stop com.example.pocspeed
adb shell monkey -p com.example.pocspeed 1
adb logcat -s MainActivity
```

Falls dein Gerät/Terminal den Tag-Filter anders behandelt, funktioniert als Alternative meistens:

```bash
adb logcat | grep "Detektor Debug"
```

Hilfreiche Zeilen zum Kopieren/Teilen sind besonders:
- `APK-Assets sichtbar: ...`
- `Versuche Asset zu kopieren: ...`
- `Lokale Modell-Datei bereit: ... size=... path=...`
- `Lokale SHA-256: ...`
- `ObjectDetector-Ladefehler: ...`
- `Erster Inferenzfehler: ...`



## Download
Die CI stellt die Debug-APK als zeitlich begrenztes GitHub-Actions-Artifact
bereit. Den jeweils verfügbaren Build findest du in den
[Workflow-Läufen von Android Debug Artifact](https://github.com/jan-rybizki/poc_speed_measurement/actions/workflows/android-artifact.yml).
Ein bestimmter Artifact-Link ist nicht dauerhaft und wird deshalb hier nicht
als stabiler „Latest“-Download veröffentlicht.


## Modell automatisch in GitHub Actions einbetten
Der Workflow **Android Debug Artifact** baut App und Modell zusammen:

1. Das veröffentlichte EfficientDet-Lite0-Modell mit Task-Vision-Metadaten wird direkt heruntergeladen.
2. Die Pipeline verwirft leere, zu kleine oder nicht als TFLite erkennbare Downloads.
3. Das Modell wird als `app/src/main/assets/efficientdet-lite0.tflite` in die Android-App gelegt.
4. Danach wird die Debug-APK gebaut.
5. Die APK und Modell-Prüfsumme werden als Artifacts bereitgestellt.

Datei: `.github/workflows/android-artifact.yml`.

Der separate Workflow **Download Object Detection Model** lädt und validiert dasselbe Modell, ohne eine APK zu bauen.

## Modell lokal herunterladen und einbetten
Für einen lokalen Android-Studio-Build kannst du dasselbe Modell direkt herunterladen:

```bash
mkdir -p app/src/main/assets
curl --fail --location --retry 3 --retry-all-errors \
  --output app/src/main/assets/efficientdet-lite0.tflite \
  'https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1?lite-format=tflite'
sha256sum app/src/main/assets/efficientdet-lite0.tflite \
  > app/src/main/assets/efficientdet-lite0.tflite.sha256
```

`app/src/main/assets/efficientdet-lite0.tflite` ist absichtlich in `.gitignore` eingetragen, damit das große Modell nicht ins Repository committed wird.
