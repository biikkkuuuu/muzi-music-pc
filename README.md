# Muzi Desktop (PC Version)

Muzi Music player for Windows PC built with **Kotlin** and **Compose Multiplatform (Desktop)**.

## Features (Exact Muzi Aesthetic):
- **Pitch Black Dark Theme (`#000000`)** with smooth Material 3 aesthetics.
- **Left Navigation Rail:** Quick switching between Home, Search, Saved/Library, and Settings.
- **Home Dashboard:**
  - Header Mood Filter Chips (*Podcasts, Work out, Feel good, Romance, Party, Energise, Relax...*).
  - Browse Charts carousel (Spotify Top 50, Hot 100, Billboard 200...).
  - Quick picks 4-column song grid with one-click play.
- **Now Playing & Lyrics View:**
  - Split layout with large album artwork and playback controls (Seekbar, Play/Pause, Next, Prev, Shuffle, Repeat).
  - Synchronized scrolling lyrics with real-time active line highlighting (LrcLib).
  - Adaptive album color backdrop glow.
- **Search Screen:** Minimal rounded search pill with instantaneous music results.
- **Library / Saved Screen:** Favourites, Downloads, and History cards.
- **Floating Mini Player:** Persistent bottom dock across screens.

## How to Run
```bash
./gradlew run
```

## How to Package for Windows (.exe / .msi)
```bash
./gradlew packageDistributionForCurrentOS
```
