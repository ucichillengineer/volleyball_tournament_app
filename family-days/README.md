# Family Days

Kotlin Multiplatform app for family birthdays and anniversaries. It includes an Android app and a browser-based web app.

## Launch the web app

### Prerequisites

- JDK 17 (`/usr/local/opt/openjdk@17` is available on this Mac)
- Gradle 8.10+ (available as `gradle`)
- A modern browser with WebAssembly support, such as Chrome, Edge, Firefox, or Safari

### Start the development server

From the project root, run:

```bash
export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
gradle :composeApp:wasmJsBrowserDevelopmentRun -Dkotlin.compiler.execution.strategy=in-process
```

Gradle downloads the required Node/Webpack tooling on the first launch. When the development server reports its address, open that local URL in a browser (normally `http://localhost:8080`).

Use `Ctrl+C` in the terminal to stop the server.

### Build a deployable web bundle

```bash
export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
gradle :composeApp:wasmJsBrowserProductionWebpack -Dkotlin.compiler.execution.strategy=in-process
```

The generated static files are placed under `composeApp/build/dist/wasmJs/productionExecutable/`. Deploy the contents of that directory to a static host such as GitHub Pages, Netlify, or Cloudflare Pages.

## Android verification

```bash
export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
gradle :composeApp:testDebugUnitTest -Dkotlin.compiler.execution.strategy=in-process
```
