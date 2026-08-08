# Court Balance

Kotlin Multiplatform (Compose) volleyball team balancer for **Android**, **iOS**, and **Web**.

## Live web app

https://ucichillengineer.github.io/volleyball_tournament_app/

Roster changes sync automatically to a shared cloud store. Open the site, add yourself, and others see updates after refresh / reopen (auto-pull on load, auto-push on change).

## Features

- Add yourself, or admin-add members
- Rate setter / lifter / spiker / all-rounder as Beginner · Medium · Advanced
- Create balanced teams with captains
- One pending team switch at a time
- Share team sheet via WhatsApp
- Admin login (default `admin` / `volleyball`)

## Quick start

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew :composeApp:assembleDebug
./scripts/package-shareables.sh
```
