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
