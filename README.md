# Staniz

Staniz is a desktop task manager with a chat-style interface and a disciplined
training-partner personality. It tracks to-dos, deadlines, and events using short
text commands, and saves every task-changing command locally.

See the [Staniz User Guide](docs/README.md) for installation instructions and the
complete command reference.

## Development setup

Staniz requires JDK 25. From the repository root, use the Gradle wrapper so that
the project uses its pinned Gradle version:

```powershell
.\gradlew run
```

Useful development commands are:

```powershell
.\gradlew test
.\gradlew check
.\gradlew shadowJar
```

- `test` runs the JUnit test suite.
- `check` runs the verification lifecycle, including tests and Checkstyle.
- `shadowJar` creates the distributable `build/libs/staniz.jar` with its runtime
  dependencies included.

Run the packaged application with:

```powershell
java -jar build/libs/staniz.jar
```

Task data is stored relative to the directory from which Staniz is launched, at
`data/staniz.txt`.
