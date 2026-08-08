#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p dist/web

echo "==> Building Android APK"
./gradlew :composeApp:assembleDebug --quiet
cp composeApp/build/outputs/apk/debug/*.apk dist/volleyball-tournament.apk
echo "APK → dist/volleyball-tournament.apk"

echo "==> Building Web distribution"
./gradlew :composeApp:wasmJsBrowserDistribution --quiet
WEB_OUT="composeApp/build/dist/wasmJs/productionExecutable"
if [[ -d "${WEB_OUT}" ]]; then
  rm -rf dist/web
  mkdir -p dist/web
  cp -R "$WEB_OUT"/* dist/web/
  # Webpack hashed names aren't enough — runtime still fetches these original names.
  cp -f composeApp/build/compileSync/wasmJs/main/productionExecutable/optimized/VolleyballTournament-composeApp-wasm-js.wasm dist/web/ 2>/dev/null || true
  cp -f composeApp/build/compose/skiko-runtime-processed-wasmjs/skiko.wasm dist/web/ 2>/dev/null || true
  echo "Web → dist/web/"
  # Keep GitHub Pages site in sync
  rm -rf docs
  mkdir -p docs
  cp -R dist/web/* docs/
  touch docs/.nojekyll
  echo "Pages site → docs/"
  (cd dist && rm -f court-balance-web.zip && zip -qr court-balance-web.zip web)
  echo "Zip → dist/court-balance-web.zip"
  echo "Serve with:  cd dist/web && python3 -m http.server 5173"
  echo "Then open:   http://localhost:5173/"
else
  echo "Web output folder not found; open composeApp/build after the Gradle task."
fi

echo "Done. Share the APK / zip via email or WhatsApp."
