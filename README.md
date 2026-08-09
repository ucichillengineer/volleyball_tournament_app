# Court Balance

Kotlin Multiplatform (Compose) volleyball team balancer for **Android**, **iOS**, and **Web**.

## Live web app

https://ucichillengineer.github.io/volleyball_tournament_app/

Roster changes sync automatically to a shared cloud store. Open the site, add yourself, and others see updates after refresh / reopen (auto-pull on load, auto-push on change).

## Editable GitHub backup

All ratings are backed up to:

**[`data/roster.json`](./data/roster.json)**

### Manual edit (safe recovery / corrections)
1. Open [`data/roster.json`](https://github.com/ucichillengineer/volleyball_tournament_app/blob/main/data/roster.json) on GitHub
2. Edit ratings / players / teams in the JSON
3. Commit to `main`
4. GitHub Actions publishes that file to the live cloud automatically
5. Or in the app: **Admin → Restore from GitHub backup**

### Automatic backups
A GitHub Action runs hourly (and on manual run) to copy the live cloud roster into `data/roster.json`.

Local backup script:

```bash
./scripts/backup-roster.sh
```

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
