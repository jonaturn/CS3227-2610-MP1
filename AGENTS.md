# Project context

## Session continuity

At the start of each session, read `CODEX_SESSION_HANDOFF.md` before continuing work.
Keep that handoff updated after significant decisions, approved changes, and checks,
and before ending a session or an anticipated context transition. Record pending work,
approval boundaries, and whether verification results are current or historical.
Do not depend solely on conversation compaction for continuity. The assistant cannot
manually clear conversation context or disable automatic compaction; the user can
start a fresh session, which should resume from this handoff.

During the current file-by-file code review, explain each concern and proposed fix
and wait for the user's approval before changing code, including temporary diagnostic
programs. Updating the handoff itself is authorized. Earlier feature implementation
approval does not authorize every newly proposed review fix.

This repository is a starter template for a greenfield Java project used in an introductory software engineering course in an undergraduate computer science program. Students use it as the starting point for their own projects.

# Default user context

Unless the user says otherwise, assume that you are assisting a student working on a project in this repository. If the user identifies themselves as an instructor or another project stakeholder, adapt your response to that role.

# Student profile

* Prior knowledge: Basic Java and OOP concepts.
* Level of programming experience: Fairly experienced, with multiple internships; soon-to-be graduate.
* IDE and level of expertise: Visual Studio Code; primary IDE used throughout internships and personal projects, ranging from simple applications to complex projects.

# Guidance for interacting with users

* Explain the rationale for significant actions: what you did and why.
* Keep explanations brief but instructive, supporting learning through responsible use of AI. For example:

  * When suggesting a Git command, briefly explain what it does.
  * Add explanatory Javadoc comments to all classes and to nontrivial methods and fields when their purpose or behavior is not obvious.
  * Make generated code as self-explanatory as possible, and include explanatory comments where they improve understanding.
  * When faced with a design choice, choose the simplest option that is sufficient for the requirements, while briefly explaining relevant more advanced alternatives.

# Project-specific requirements

## Java version:

Ensure that Java 25 is used when running the application or build tasks. On macOS, use `sdk use java 25.0.3.fx-zulu` to switch to Java 25 if needed.

## UI testing

Keep `test/ui-test-plan.md` synchronized with user-visible command or output changes. Interleave positive and negative cases so tests verify that rejected commands do not corrupt application state. After each code update that affects the command-line UI, invoke the project `test-ui` skill, run every applicable case, and show the console input/output record. Stop at the first failure and report the expected and actual behavior.

## Git

Use lightweight tags unless the user requests an annotated tag.
When proposing or creating a commit message, include enough detail to explain the rationale for the change.
Do not commit or push unless explicitly asked.
