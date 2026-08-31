# Staniz Developer Guide

## Introduction

Staniz is a Java 25 desktop task manager with a command-driven JavaFX interface.
This guide explains the architecture and the design decisions that are not
immediately obvious from reading individual classes. For command syntax and
end-user behavior, refer to the [User Guide](UserGuide.md).

## Setting up the project

### Prerequisites

- JDK 25
- Git
- A terminal capable of running the Gradle wrapper

Confirm the active Java version:

```text
java -version
```

The reported major version must be 25. The Gradle wrapper downloads and uses the
project's pinned Gradle version, so a separate Gradle installation is not
required.

### Common development commands

| Purpose | Windows PowerShell | macOS/Linux |
| --- | --- | --- |
| Run the JavaFX application | `.\gradlew run` | `./gradlew run` |
| Compile and run all tests | `.\gradlew test` | `./gradlew test` |
| Run tests and Checkstyle | `.\gradlew check` | `./gradlew check` |
| Build coverage report | `.\gradlew jacocoTestReport` | `./gradlew jacocoTestReport` |
| Run mutation tests | `.\gradlew pitest` | `./gradlew pitest` |
| Build the executable JAR | `.\gradlew shadowJar` | `./gradlew shadowJar` |

Generated reports and artifacts are available at:

- JAR: `release/staniz.jar`
- JaCoCo: `build/reports/jacoco/test/html/index.html`
- PIT: `build/reports/pitest/index.html`
- Checkstyle: `build/reports/checkstyle/`

## Architecture

Staniz separates user-interface concerns from command processing and
persistence. Both the JavaFX and console interfaces call the same backend, so
behavior can be tested without automating the GUI.

```text
JavaFX GUI or console UI
          |
          v
 Staniz.executeCommand
    |       |       |
    |       |       +--> ResponseFormatter --> CommandResult --> UI
    |       |
    |       +----------> TaskList and Task hierarchy
    |
    +------------------> Parser and CommandType
                            |
Task-changing command -----+----> Storage --> data/staniz.txt
```

### Package responsibilities

| Package | Responsibility |
| --- | --- |
| `staniz` | Coordinates command execution and owns the active `TaskList` and `Storage`. |
| `staniz.command` | Describes supported command metadata and the result returned to a UI. |
| `staniz.parser` | Recognizes command types and validates command-specific arguments. |
| `staniz.task` | Models tasks and owns ordered task-list operations. |
| `staniz.storage` | Loads, validates, encodes, and safely replaces the persistent data file. |
| `staniz.ui` | Handles console input/output and UI-independent response formatting. |
| `staniz.gui` | Loads JavaFX views and renders user, success, and error dialog bubbles. |

JavaFX layouts, styles, and images are stored under `src/main/resources` rather
than embedded in Java classes. This keeps presentation changes separate from
command behavior.

## Command execution

`Staniz.executeCommand(String)` is the stable boundary between the user
interfaces and the backend. Processing one command follows these steps:

1. `Parser.parseCommandType` strips surrounding whitespace and maps the first
   complete keyword to a `CommandType`.
2. A command-specific parser validates the remaining arguments and converts
   dates or task numbers into typed values.
3. `Staniz` dispatches the command with a switch expression at one abstraction
   level.
4. The selected operation reads or mutates `TaskList`.
5. If `CommandType.changesTasks()` is true, `Storage` saves the complete list.
6. `ResponseFormatter` produces UI-independent response text.
7. `CommandResult` returns the response together with the `shouldExit` flag.

Keeping the exit flag out of the GUI lets both interfaces implement `bye`
consistently. The JavaFX controller displays the farewell, disables further
input, and closes after a short delay; the console loop exits immediately after
printing the same response.

### Command metadata

`CommandType` stores three pieces of behavior for each command:

- the exact user-facing keyword;
- whether arguments are accepted; and
- whether successful execution changes persistent task state.

Centralizing this metadata prevents separate command lists from drifting apart.
For example, `list` and `bye` reject unexpected arguments, while only add,
mark, unmark, and delete operations trigger a save.

## Parsing and validation

`Parser` is stateless: every method receives the complete command and returns a
validated value or throws `StanizException`. A stateless parser is sufficient
because no command depends on earlier input beyond the task list owned by
`Staniz`.

The parser deliberately performs validation before mutation. Rejected commands
therefore leave the task list unchanged. Important rules include:

- command keywords and search text are case-sensitive;
- extra leading, trailing, repeated, and tab whitespace is accepted;
- user task numbers are one-based and converted to zero-based indexes only
  after range validation;
- dates use ISO `yyyy-MM-dd` input and are represented internally by
  `LocalDate`;
- `/by`, `/from`, and `/to` are recognized only as complete whitespace-delimited
  tokens;
- deadline and event parameters must appear exactly once; and
- an event's start date cannot be later than its end date.

The private `ParameterLocation` record captures the first token location and
total occurrence count. This supports specific duplicate and ordering errors
without retaining parser state between commands.

## Task model

`Task` contains the state shared by every task: a description and completion
status. Its subclasses add only type-specific data and formatting:

| Class | Additional data | Persistent prefix |
| --- | --- | --- |
| `Todo` | None | `T` |
| `Deadline` | Due date | `D` |
| `Event` | Start and end dates | `E` |

`TaskList` owns the mutable `ArrayList<Task>` and exposes task-level operations
instead of exposing the collection itself. Constructors defensively copy input,
and `getTasks()` returns an immutable snapshot. These boundaries prevent callers
such as `Storage` from modifying the list without going through `TaskList`.

`TaskList.find` returns a separate list that retains matching task references in
their original order. Search-result numbering is local to that result, while
mutating commands use numbering from the full list; the User Guide calls out
this distinction explicitly.

## Storage design

The default file is `data/staniz.txt`, resolved relative to the directory from
which Staniz is launched. A missing file represents an empty task list. A
malformed existing file produces a line-specific `StorageException` rather than
silently discarding user data.

Each UTF-8 line contains pipe-separated fields:

```text
T | 0 | read Clean Code
D | 1 | submit report | 2026-09-10
E | 0 | project retreat | 2026-09-12 | 2026-09-14
```

The second field is `1` for completed or `0` for incomplete. Backslashes and
pipe characters in descriptions are escaped as `\\` and `\|`, respectively.
Loading rejects unknown task types, invalid completion markers, wrong field
counts, blank required values, invalid dates, reversed event ranges, and
unsupported escape sequences.

### Protecting the data file during saves

`Storage.save` does not write directly over the existing file. It:

1. serializes the complete task list;
2. writes it to a temporary sibling file;
3. atomically replaces `staniz.txt` when the file system supports atomic moves;
4. falls back to a normal replacement after the complete temporary file has
   been written; and
5. removes an incomplete temporary file if saving fails.

Using a sibling temporary file keeps source and destination on the same file
system, which increases the chance that an atomic move is supported. The
fallback preserves portability while still avoiding partial direct writes.

One known limitation is that a task has already changed in memory if the final
save operation fails. The previous on-disk file remains protected, but the
running application may differ from it until restart. A future transaction
layer could mutate a copy and publish it only after persistence succeeds.

## User interfaces

### JavaFX interface

`Launcher` is a plain Java entry point that calls `Application.launch(Main.class,
args)`. Keeping the executable entry point separate from the `Application`
subclass avoids Java launcher's special JavaFX module handling when starting the
fat JAR.

`Main` loads `MainWindow.fxml`, creates the backend, sets minimum window
dimensions, and reports startup failures with an alert. `MainWindow` handles
text-field and button events. `DialogBox` is a reusable FXML control with factory
methods for user, successful, and error messages. CSS controls colors and bubble
geometry, while avatars are loaded from classpath resources and clipped to
circles at runtime.

### Console interface

`Ui` remains available even though the packaged application starts the JavaFX
interface. It provides a low-dependency way to exercise real command input and
output, which makes regression testing more reliable than desktop automation.
`ResponseFormatter` is shared so console tests also verify the text shown in the
GUI.

## Error handling and assertions

Staniz separates expected runtime failures from internal programming errors:

- `StanizException` describes invalid user commands and contains actionable
  correction guidance.
- `StorageException` wraps unreadable, unwritable, or invalid persistent data.
- Java assertions document internal invariants after public input has already
  been validated, such as valid zero-based indexes and non-null task values.

The UIs catch the two expected exception types at their boundaries. The GUI
uses a distinct error bubble, while startup storage failures use an alert.
Assertions are enabled by Gradle's `run` and `test` tasks but are not used as a
substitute for user-input validation.

## Testing strategy

The test packages mirror the production packages. The current suite contains 69
JUnit tests with no failures and covers command metadata, parsing, task behavior,
storage, response formatting, backend integration, console I/O, and required GUI
resources.

The JavaFX package is excluded from automated coverage and mutation metrics.
This follows the project scope: backend behavior is tested through the shared
`Staniz` boundary, while GUI resources receive structural tests and the rendered
window is checked manually.

At the current release candidate, the backend reports:

| Metric | Coverage |
| --- | ---: |
| Instructions | 94.71% |
| Branches | 92.98% |
| Lines | 92.34% |
| Methods | 97.22% |
| Classes | 100.00% |

PIT generated 180 mutations: 167 were killed, 9 were in excluded or uncovered
paths, and 4 survived. The mutation score is therefore stronger evidence than
line coverage alone that assertions in the test suite detect behavioral faults.

### End-to-end console checks

`test/ui-test-plan.md` contains 13 interleaved positive and negative scenarios.
`test/run-tests.ps1` compiles the console entry point in isolation, runs each
scenario, checks expected output fragments, and prints the complete input/output
record. Negative cases are followed by state checks so a rejected command cannot
quietly corrupt later behavior.

## Continuous integration

`.github/workflows/gradle.yml` runs on pushes and pull requests across Ubuntu,
macOS, and Windows. Each matrix job:

1. checks out the repository;
2. validates the Gradle wrapper;
3. installs the Zulu JDK 25 distribution with JavaFX; and
4. runs `./gradlew check`.

The cross-platform matrix detects platform-specific build and test failures
before a JAR is released.

## Building and releasing

Run a clean release verification from the repository root:

```text
./gradlew clean check shadowJar
```

On Windows PowerShell, use `.\gradlew` instead of `./gradlew`. The Shadow plugin
creates one executable JAR at `release/staniz.jar` and bundles JavaFX runtime
dependencies for Windows, macOS, and Linux.

For a GitHub release:

1. merge the completed release branch into `master`;
2. create and push a version tag such as `v1.0.0` at that exact commit;
3. create a GitHub Release from the version tag;
4. upload only `staniz.jar` as the binary asset; and
5. download and smoke-test the public asset from a clean directory.

The released source, documentation, and JAR must all correspond to the same
commit.

## Adding a command

When extending Staniz with another command:

1. add its keyword and metadata to `CommandType`;
2. add focused argument parsing and validation to `Parser`;
3. add the operation to `Staniz.executeNonExitCommand`;
4. add domain behavior to `TaskList` or another focused class rather than the
   UI;
5. add or reuse a `ResponseFormatter` method;
6. update JUnit, mutation, and console regression tests as applicable;
7. update `test/ui-test-plan.md` for visible command/output changes; and
8. update the User Guide command reference.

This sequence keeps GUI and console behavior aligned because neither interface
implements command semantics itself.

## Acknowledgements

- The project began from the [SE-EDU Duke project](https://se-education.org/guides/tutorials/javaFxPart1.html)
  starter structure and followed its JavaFX tutorial conventions.
- The current implementation follows the trimmed
  [CS3227 Project Duke brief](https://nus-cs2103-ay2627-s1.github.io/website/projectDuke/cs3227.html).
- Build and test dependencies are provided by Gradle, OpenJFX, JUnit 5,
  Checkstyle, JaCoCo, the Shadow plugin, and PIT.
- OpenAI Codex was used extensively to implement, test, document, and review the
  project. The project author selected requirements, approved design decisions,
  supplied assets, and manually reviewed behavior.
- The two avatar source images were supplied by the project author.

See [CONTRIBUTORS.md](../CONTRIBUTORS.md) for the contributors credited by the
starter repository.
