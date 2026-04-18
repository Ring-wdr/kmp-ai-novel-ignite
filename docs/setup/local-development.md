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
- `./gradlew.bat :composeApp:jvmTest`
- `./gradlew.bat :relay:test`

Task 12's original plan referred to the desktop verification step as `desktopTest`, but this wizard scaffold wires the `desktopTest` source directory into `:composeApp:jvmTest`. Use the command below for actual desktop smoke verification in this branch:

```bash
./gradlew.bat :composeApp:jvmTest
```
