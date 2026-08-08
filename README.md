# Court Balance

Kotlin Multiplatform (Compose) volleyball team balancer for **Android**, **iOS**, and **Web**.

Seeded from `players.md` with Praveen Sanigepalli and Vikas Yadlapalli.

## Live web app

Published on GitHub Pages from the `docs/` folder:

https://ucichillengineer.github.io/volleyball_tournament_app/

## Features

- Add yourself, or admin-add members
- Rate setter / lifter / spiker / all-rounder as Beginner · Medium · Advanced
- Players or admin can update ratings
- Admin login (default `admin` / `volleyball`) stored in synced JSON on Google Drive
- Create balanced teams with captains
- One pending team switch at a time, then confirm to reshuffle
- Share team sheet via WhatsApp / system share

## Quick start

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

# Android APK
./gradlew :composeApp:assembleDebug

# Local web
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Refresh shareables + docs/ (GitHub Pages site)
./scripts/package-shareables.sh
```

## Google Drive sync

1. Paste `scripts/GoogleDriveSync.gs` into [Google Apps Script](https://script.google.com)
2. Deploy as Web App (Execute as Me, Anyone)
3. In the app: **Admin → unlock → paste Apps Script URL → Push**

## Default admin

- Username: `admin`
- Password: `volleyball`
