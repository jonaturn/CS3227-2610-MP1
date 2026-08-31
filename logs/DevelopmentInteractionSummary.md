# Staniz Development Interaction Summary

## Purpose

This log complements [PromptSummary.md](PromptSummary.md). The prompt summary
records the chronological user requests, while this file records the principal
decisions, implementation outcomes, corrections, and verification performed
during AI-assisted development.

## Initial setup and Levels 0-3

- The user established the trimmed CS3227 Project Duke brief as the
  authoritative requirements source and required proposed diffs before code
  changes.
- The placeholder Duke application was renamed to Staniz and given its banner,
  greeting, and farewell.
- Java 25 was configured and the user learned how source compilation, classpath
  selection, JAR creation, Git commits, and lightweight milestone tags work.
- Level 1 added the input loop and `bye`; Level 2 echoed commands; Level 3 added
  tasks and the initial task list.
- Manual output review found a plausible but incorrect incomplete-task marker.
  The user corrected it to the required `[X]` and `[ ]` convention.
- Empty input and command-recognition bugs were found during manual use and
  corrected before proceeding.

## Object-oriented restructuring

- UI, parsing, command metadata, task-domain behavior, and task-list ownership
  were separated into focused classes.
- The user requested explanations of `TaskList`, parser responsibilities, and
  the stateless-versus-stateful parser trade-off before approving the design.
- Classes were organized into packages such as `staniz.parser`,
  `staniz.command`, and `staniz.task`, later supplemented by storage, UI,
  exception, and GUI packages.
- Public visibility was limited to cross-package contracts; implementation
  details remained private or package-private.

## Build, packaging, and code standards

- Gradle became the repeatable build interface for compilation, tests, running,
  quality checks, coverage, mutation testing, and JAR packaging.
- JUnit tests were grouped by production class and package, following the
  course tutorial's structure.
- The Shadow plugin produced one executable JAR containing runtime and JavaFX
  dependencies.
- Coding-standard and Checkstyle changes were reviewed carefully because some
  proposed edits differed only in whitespace or access modifiers.
- A standing rule was established that approval requests for commands must
  explain their syntax, purpose, and effect, and that merge-conflict resolutions
  require explicit user approval.

## JavaFX GUI and personality

- The initial JavaFX interface followed the course tutorial structure, with
  FXML controllers and a separate launcher suitable for a fat JAR.
- The user selected a green color scheme, asymmetric bubbles, stable window
  sizing, and separate supplied avatars for the user and Staniz.
- GUI automation through operating-system input was explored but found fragile.
  The user chose manual GUI verification because the trimmed brief did not
  require automated GUI testing.
- A disciplined training-partner personality was applied to greetings,
  confirmations, errors, and the farewell while keeping command semantics
  unchanged.

## Reliability and testing

- Assertions were added only for internal invariants, not as replacements for
  user-input validation.
- Code-quality refactoring kept command dispatch at one abstraction level and
  centralized response formatting.
- GitHub Actions was configured to run Gradle checks on supported operating
  systems with Java 25.
- Tests expanded to cover individual classes, backend integration, console I/O,
  storage failures, and command recovery after rejected input.
- JaCoCo measured structural coverage and PIT mutation testing checked whether
  tests detected behavioral changes. GUI implementation classes were excluded
  from these automated metrics according to the agreed scope.
- The console test plan interleaved valid and invalid commands so rejected input
  could be checked for unintended state changes.

## Error handling and persistence

- Parser errors were made specific for missing arguments, malformed numbers,
  invalid dates, duplicate parameters, and misordered event parameters.
- Flexible whitespace was accepted without weakening command-keyword or
  parameter-token boundaries.
- Storage validation reports malformed saved data rather than silently dropping
  it.
- Saving uses a temporary sibling file and atomic replacement when available,
  reducing the risk of corrupting the previous data file.

## Documentation and submission preparation

- The User Guide was checked against the implemented commands, response text,
  date format, persistence behavior, and Java 25 build/run instructions.
- The Developer Guide documented architecture, parsing, the task model,
  persistence, GUI and console boundaries, error handling, testing, CI, and the
  release process. It includes an acknowledgements section for reused sources,
  dependencies, supplied assets, and AI assistance.
- The authored reflection was converted to the required Markdown path and
  expanded with four detailed prompt examples.
- Prompt summaries from the earlier `Explain payment and pricing` task and the
  current project task were combined into one chronological list.
- Gradle was configured to generate `release/staniz.jar`. The final compliance
  build used Java 25.0.4.1, passed all 69 JUnit tests and both Checkstyle tasks,
  and produced a JAR containing the Staniz launcher, JavaFX classes and native
  libraries, FXML, CSS, and avatar resources.

## Human oversight retained

The user remained responsible for selecting requirements, approving design and
visual choices, correcting specification mismatches, deciding the scope of GUI
testing, manually checking visible behavior, and authorizing Git operations.
Codex supplied implementation, explanations, automated verification, and
documentation support within those decisions.
