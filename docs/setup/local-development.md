# Local Development

## App

- Start from official KMP wizard scaffold.
- Use Desktop first.
- Keep Ollama running locally before testing local inference.

Run the desktop app with:

```bash
./gradlew.bat :composeApp:run
```

This scaffold keeps desktop JVM tests under `composeApp/src/desktopTest/kotlin`, but Gradle executes them through `:composeApp:jvmTest`.

## Relay

- Set `OPENROUTER_API_KEY`.
- Run `./gradlew.bat :relay:run`.

Example PowerShell session:

```powershell
$env:OPENROUTER_API_KEY="your-key"
./gradlew.bat :relay:run
```

## Verification

- `./gradlew.bat :composeApp:allTests`
- `./gradlew.bat :composeApp:desktopTest`
- `./gradlew.bat :relay:test`

Wizard scaffold adaptation note: `:composeApp:desktopTest` is not a generated Gradle task in this branch. The `desktopTest` source directory is wired into `:composeApp:jvmTest`, so use the command below for actual desktop smoke verification:

```bash
./gradlew.bat :composeApp:jvmTest
```
