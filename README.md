# POC: Realtime Car Detection für Android

## Ziel
Wir entwickeln hier eine **Android-App**, die wir während der Entwicklung **schnell auf echte Handys bringen und direkt testen** können.

Der Fokus für den POC ist:
- **Realtime Car Detection** aus dem **Kamera-Livebild** (Video-Stream)
- Kurze Iterationszyklen: bauen, installieren, testen, verbessern
- Eine technische Basis, auf der später z. B. Speed-/Flow-Messung erweitert werden kann

## Was die App im POC können soll
1. Kamera-Vorschau auf dem Android-Gerät anzeigen
2. In Echtzeit Fahrzeuge im Bild erkennen (Bounding Boxes + Confidence)
3. Solide FPS auf Mid-Range-Geräten erreichen
4. Ergebnisse lokal visualisieren (Overlay)

## Entwicklungs-Konzept: schnell aufs Handy bringen
Damit wir beim Entwickeln nicht ausgebremst werden, setzen wir auf einen schnellen Device-Loop:

1. **Einheitliches Android-Projekt** (Kotlin, Android Studio)
2. **Debug-Builds direkt auf USB-Gerät** installieren
3. Optional **ADB über WLAN** für kabelloses Iterieren
4. Hot-Loop mit kleinen Inkrementen:
   - Modell/Preprocessing anpassen
   - Build + Deploy
   - Direkt im realen Kameraszenario testen
5. Messbare Checkpoints pro Sprint:
   - Startzeit der App
   - Inference-Latenz
   - FPS
   - Akku-/Thermal-Verhalten

## Technische Richtung (POC)
- **Kamera**: CameraX
- **Inference**: TensorFlow Lite oder ONNX Runtime Mobile
- **Modelle**: leichte Detectoren (z. B. YOLO-Nano/Small Varianten)
- **Rendering**: Overlay auf Preview (Bounding Boxes, Labels, Confidence)

## Warum direkt auf dem Handy testen?
- Reale Kamera-Bedingungen (Licht, Winkel, Bewegungsunschärfe)
- Echte Performance-/Thermal-Effekte
- Schnelleres Feedback als rein im Emulator

## Nächste Schritte
- [ ] Android-App-Grundgerüst anlegen
- [ ] CameraX Preview integrieren
- [ ] Erstes On-Device-Detektionsmodell einbinden
- [ ] Overlay für Boxen und Klassen anzeigen
- [ ] Mess-Logging für FPS/Latenz ergänzen

---
Wenn gewünscht, kann ich als Nächstes auch direkt eine konkrete Projektstruktur (Module, Packages, MVP-Architektur) in dieses Repo schreiben.

## No-cost distribution path for private users (Android)

You can distribute and test the Android app privately without Google Play.

- **No Play Store required**.
- Build the APK with **GitHub Actions**.
- Download the generated artifact and install it manually on Android (**sideloading**).

### Step-by-step: install Android artifact

1. Open the relevant **GitHub Actions** workflow run.
2. Download the APK artifact named **`app-debug-apk`**.
3. Transfer the APK to your phone (for example via Drive, USB, email, or messaging app).
4. On Android, enable **Install unknown apps / unknown sources** for the app you use to open the APK (file manager or browser).
5. Open the APK and install the app.
6. **Updates:** repeat the same process and install the newer APK over the existing app.
