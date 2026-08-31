# Staniz User Guide

Staniz is your personal task companion: a disciplined training partner that
helps you capture objectives, track important dates, and finish what you start.
You interact with Staniz by entering short commands into its chat window.

![Staniz desktop application](Ui.png)

## Quick start

### Requirements

- Java Development Kit (JDK) 25
- The `staniz.jar` application file

### Starting Staniz

1. Put `staniz.jar` in a folder where Staniz may create its `data` directory.
2. Open a terminal in that folder.
3. Run:

   ```text
   java -jar staniz.jar
   ```

4. Enter a command in the text field and press **Enter** or select **Send**.

If you are building Staniz from its source repository, run
`.\gradlew shadowJar` on Windows or `./gradlew shadowJar` on macOS/Linux. The
application will be created at `build/libs/staniz.jar`.

## Reading the task list

Staniz displays each task with a type and completion marker:

| Symbol | Meaning |
| --- | --- |
| `[T]` | To-do |
| `[D]` | Deadline |
| `[E]` | Event |
| `[ ]` | Incomplete |
| `[X]` | Completed |

For example, `[D][ ] Submit report (by: Sep 10 2026)` is an incomplete
deadline.

## Command overview

| Action | Command format |
| --- | --- |
| Add a to-do | `todo DESCRIPTION` |
| Add a deadline | `deadline DESCRIPTION /by DATE` |
| Add an event | `event DESCRIPTION /from START_DATE /to END_DATE` |
| Show all tasks | `list` |
| Search task descriptions | `find KEYWORD` |
| Mark a task completed | `mark TASK_NUMBER` |
| Mark a task incomplete | `unmark TASK_NUMBER` |
| Delete a task | `delete TASK_NUMBER` |
| Exit Staniz | `bye` |

Command words and search keywords are case-sensitive. Dates must use the
`yyyy-MM-dd` format, such as `2026-09-10`. Staniz accepts extra spaces or tabs
around command elements.

## Adding a to-do

Use `todo` for an objective without a specific date.

Format: `todo DESCRIPTION`

Example:

```text
todo read Clean Code
```

Expected result:

```text
Good. Another objective locked in:
  [T][ ] read Clean Code
```

## Adding a deadline

Use `deadline` for an objective that must be completed by a particular date.
Specify `/by` exactly once.

Format: `deadline DESCRIPTION /by DATE`

Example:

```text
deadline submit report /by 2026-09-10
```

Expected result:

```text
Good. Another objective locked in:
  [D][ ] submit report (by: Sep 10 2026)
```

## Adding an event

Use `event` for an activity with a start date and an end date. Specify `/from`
and `/to` exactly once each, in that order. The start date may be the same as the
end date, but it cannot be later.

Format: `event DESCRIPTION /from START_DATE /to END_DATE`

Example:

```text
event project retreat /from 2026-09-12 /to 2026-09-14
```

Expected result:

```text
Good. Another objective locked in:
  [E][ ] project retreat (from: Sep 12 2026 to: Sep 14 2026)
```

## Listing tasks

Use `list` to show the complete training plan in its saved order.

Format: `list`

Example result:

```text
Current training plan:
1.[T][ ] read Clean Code
2.[D][ ] submit report (by: Sep 10 2026)
3.[E][ ] project retreat (from: Sep 12 2026 to: Sep 14 2026)
```

The task numbers in this list are the numbers used by `mark`, `unmark`, and
`delete`.

## Finding tasks

Use `find` to show tasks whose descriptions contain a keyword. Matching is
case-sensitive and also finds partial words.

Format: `find KEYWORD`

Example:

```text
find report
```

Example result:

```text
Matching objectives:
1.[D][ ] submit report (by: Sep 10 2026)
```

The numbers in search results are local to those results. To change or delete a
task, use its number from `list`, not its number from `find`.

## Marking a task as completed

Use the task's number from `list`.

Format: `mark TASK_NUMBER`

Example:

```text
mark 1
```

Expected result:

```text
Strong work. One more task conquered:
  [T][X] read Clean Code
```

## Marking a task as incomplete

Use `unmark` when a completed task needs more work.

Format: `unmark TASK_NUMBER`

Example:

```text
unmark 1
```

Expected result:

```text
Reset accepted. This objective is active again:
  [T][ ] read Clean Code
```

## Deleting a task

Use the task's number from `list`. The tasks after it are renumbered
automatically.

Format: `delete TASK_NUMBER`

Example:

```text
delete 1
```

Example result:

```text
Cutting dead weight. This task is gone:
  [T][ ] read Clean Code
You have 2 objectives left in the program.
```

## Exiting Staniz

Use `bye` without any additional arguments. Staniz displays its farewell and
then closes the window.

Format: `bye`

Expected result:

```text
Session complete. Stay disciplined.
```

## Saving task data

Staniz saves changes automatically after successfully adding, marking,
unmarking, or deleting a task. It restores those tasks the next time it starts.
The data is stored at `data/staniz.txt`, relative to the folder from which the
application was launched.

Avoid editing the data file manually. Invalid saved data is reported at startup
instead of being silently discarded.

## Troubleshooting commands

If a command cannot be processed, Staniz explains what is wrong and usually
shows a valid example. Common causes include:

- leaving out a task description, keyword, date, or task number;
- entering a task number that is not present in `list`;
- using a date that is not in `yyyy-MM-dd` format;
- repeating `/by`, `/from`, or `/to`;
- placing `/to` before `/from` in an event; or
- adding arguments after `list` or `bye`.

Rejected commands do not change the saved task list. Correct the command using
the guidance in the error message and submit it again.
