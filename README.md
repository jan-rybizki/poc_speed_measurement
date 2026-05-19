# POC: Realtime Car Detection für Android

## Ziel
Dieses Repository enthält jetzt ein erstes **Android-Grundgerüst** mit:
- Live-Kamera-Vorschau (CameraX)
- FPS-Einblendung direkt im Bild
- GitHub-Action, die automatisch ein **Debug-APK als Artifact** bereitstellt

## Enthalten im aktuellen Stand
- Android-App-Modul `app`
- `MainActivity` mit CameraX Preview + `ImageAnalysis` für FPS-Messung
- Overlay-`TextView` für aktuelle FPS
- GitHub Workflow `.github/workflows/android-artifact.yml` zum Bauen und Hochladen von `app-debug-apk`

## Lokal starten (Android Studio)
1. Projekt in Android Studio öffnen.
2. Gradle Sync durchführen.
3. App auf ein Gerät mit Kamera installieren/starten.
4. Beim ersten Start Kameraberechtigung erlauben.

## APK über GitHub Actions herunterladen
1. In GitHub zum Tab **Actions** gehen.
2. Workflow **Android Debug Artifact** auswählen.
3. Einen Run öffnen (z. B. nach Push oder manuell via `workflow_dispatch`).
4. Artifact **`app-debug-apk`** herunterladen.
5. APK auf Android-Gerät sideloaden.
