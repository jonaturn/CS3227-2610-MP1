# Staniz

Staniz is a desktop task manager with a chat-style interface and a disciplined
training-partner personality. It tracks to-dos, deadlines, and events using short
text commands, and saves every task-changing command locally.

See the [Staniz User Guide](docs/README.md) for installation instructions and the
complete command reference.

## Building and running

Staniz requires JDK 25. Confirm that the active Java version begins with `25`:

```text
java -version
```

Run Staniz directly from its source directory:

| Platform | Command |
| --- | --- |
| Windows PowerShell | `.\gradlew run` |
| macOS/Linux | `./gradlew run` |

Build the distributable JAR from the repository root:

| Platform | Build command | Run command |
| --- | --- | --- |
| Windows PowerShell | `.\gradlew shadowJar` | `java -jar .\build\libs\staniz.jar` |
| macOS/Linux | `./gradlew shadowJar` | `java -jar build/libs/staniz.jar` |

`shadowJar` creates `build/libs/staniz.jar` with the JavaFX and runtime
dependencies included. A pre-built copy named `staniz.jar` can instead be
started from its containing directory with `java -jar staniz.jar`.

## Verification commands

| Platform | Tests | Tests and code-quality checks |
| --- | --- | --- |
| Windows PowerShell | `.\gradlew test` | `.\gradlew check` |
| macOS/Linux | `./gradlew test` | `./gradlew check` |

Task data is stored relative to the directory from which Staniz is launched, at
`data/staniz.txt`.
