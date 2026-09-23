# Staniz AI Prompt Summary Log

**Project:** Staniz MP1

**AI tools:** OpenAI Codex desktop application, and Claude Code from 22 September
2026 onward, after the Codex monthly limit was reached

**Period represented:** 28 August to 23 September 2026
**Prepared from:** The Codex and Claude Code project tasks, and the resulting Git
history

## Scope and method

This is a chronological, paraphrased summary rather than a verbatim chat export.
It records substantive requests, questions, approvals, and corrections that
affected the project. Repeated approval-only messages, tool output, and ambient
browser context have been condensed. Where several short prompts formed one
decision, they are summarized together.

## Prompt summary

1. Supplied the current CS3227 Project Duke brief and software-engineering
   references, requested a fast Level 0 plan for rename, greet, and exit, and
   required every code change to be shown as a diff for approval.
2. Chose `Staniz` as the chatbot's name.
3. Asked for a Staniz ASCII banner to be included in Level 0.
4. Approved the proposed Level 0 rename, greeting, banner, and farewell changes.
5. Asked for help filling the `[to be filled]` student-profile fields in the
   repository's Markdown instructions.
6. Asked whether the listed development environment should be Codex because
   Codex was being used to implement the project.
7. Clarified that the preferred IDE was Visual Studio Code, used extensively
   across internships and personal projects, and described the programming
   experience level as fairly experienced and soon to graduate.
8. Approved the resulting student-profile update.
9. Asked to ideate a setup in which Claude reviews every pull request.
10. Corrected the request from Claude review to native Codex code review.
11. Asked for a step-by-step walkthrough of the Codex code-review setup plan.
12. Reported that the Code review option was not visible in Codex settings.
13. Asked whether the current task was running locally rather than in Codex
    cloud and why it did not appear in the web settings.
14. Asked why the local repository was available in Codex desktop while the
    Codex web page instead prompted installation of the ChatGPT application.
15. Asked to continue with the first project setup step.
16. Asked why Java 25 was not available in the terminal environment.
17. Asked Codex to install Java 25.
18. Asked to begin Level 1.
19. Asked how to compile the application into a JAR file.
20. Asked how the course guidance says Level 1 should be tested and whether JAR
    compilation was required at that stage.
21. Asked how to remove the generated JAR after testing.
22. Confirmed Level 1 was complete and asked to proceed to the next part.
23. Asked how to carry out the first item in a previously supplied Git
    procedure.
24. Asked how to create and use the `Level-0` Git tag.
25. Asked why the commit and tag had not been pushed yet.
26. Clarified that both the commit and tag had already been pushed manually.
27. Approved the proposed Level 2 echo-loop implementation.
28. Asked why running `javac Staniz.java` directly did not work.
29. Asked what the `-d` and `-cp` options mean in the Java compilation and
    execution commands.
30. Asked how to inspect the repository's Git tags.
31. Asked to move on to Level 3.
32. Confirmed an intervening step was complete and asked to continue.
33. Approved the proposed two-file Level 3 task-list implementation.
34. Asked whether `Task.java` also had to be compiled.
35. Asked whether an incomplete task should display an empty marker instead of
    a question mark.
36. Clarified that completed tasks should display `X` and incomplete tasks a
    blank marker.
37. Approved the marker-only correction.
38. Reported that repeatedly pressing Enter added blank tasks and that `mark 1`
    did not work.
39. Approved the proposed bug fixes and Javadoc updates.
40. Asked to include separator lines around the response to the `unmark`
    command.
41. Asked why the proposed diff removed so much code and replaced direct
    `System.out.println` calls with a `printResponse` helper.
42. Approved the response-separator refactor.
43. Asked Codex to inspect the repository and report the project's current state.
44. Asked it to read the Codex session handoff file and reconcile the handoff with
   the repository.
45. Challenged the initial conclusion that Git tags were absent, explained that
   tags had been created locally, and asked whether they had not been pushed.
46. Authorized Codex to add the missing milestone tags to the corresponding
   commits.
47. Asked to continue with `A-MoreOOP`, implementing the UI separation first.
48. Asked what `TaskList` does and how it replaces the previous raw task data
   structure.
49. Asked what the parser should do, how it would be implemented, and what a
   stateful-parser alternative would look like before approving the stateless
   design.
50. Proposed dividing the new classes into `parser`, `task`, and `command`
   packages, then approved the package reorganization.
51. Asked to move to Gradle, approved the proposed setup, and asked what Gradle
   contributes to the project and how to verify it manually.
52. Reported that `bye` stopped working after the Gradle transition and asked for
    the cause to be investigated.
53. Asked to continue to `A-JUnit` and create tests for every non-trivial method
    in every class.
54. Asked whether tests should be grouped by class following the SE-EDU JUnit
    guide, what the Gradle test command does, and what successful output should
    look like.
55. Shared a successful Gradle build transcript and asked to continue to
    `A-Jar`.
56. Asked whether `A-Gradle` had produced changes and requested a summary of the
    SE-EDU Git/coding conventions and how they could be applied.
57. Corrected the planned sequence to Level 9 and Checkstyle, then asked for the
    coding-standard changes to be implemented first.
58. Asked why some classes and methods were made private while others remained
    public.
59. Established a standing instruction that every command approval request must
    explain the command's syntax, purpose, and effects.
60. Asked to move to Level 9, and established a standing release workflow: when
    moving on, stage, commit, create the correct lightweight milestone tag, and
    push the commit and tag.
61. Asked to move to Checkstyle, reviewed its proposed changes, and approved the
    implementation and publication.
62. Established a standing rule that any merge conflict must be shown and
    explicitly approved before resolution.
63. Asked why some Checkstyle changes appeared identical to the previous code,
    then approved the remaining changes.
64. Asked Codex to redo an interrupted task, locate the generated JAR, and explain
    how to run the application.
65. Questioned why an `A-Varargs` branch had been created when the trimmed course
    brief did not list that level, then supplied the current CS3227 Project Duke
    page as the authoritative brief.
66. Asked exactly what changed in `A-Varargs`, confirmed those changes could
    remain, and asked whether the JavaFX tutorial features had been implemented.
67. Directed Codex to follow the tutorial documentation more closely and pointed
    out that launching the GUI without entering commands did not test its
    behavior.
68. Asked to continue to `A-Assertions`, explicitly excluding the older course
    website and using only the current trimmed brief.
69. Approved the assertions and moved to `A-CodeQuality`, while reminding Codex
    to explain command syntax before approval requests.
70. Approved the code-quality changes and moved to `A-CI`.
71. Asked what continuous integration does, requested a detailed walkthrough of
    the YAML workflow, and asked how GitHub Actions creates and configures jobs.
72. Reported that CI had been committed and pushed, then moved to `A-BetterGui`.
73. Required consultation before decisions about visual design, including avatar
    and color choices.
74. Selected green instead of blue, retained asymmetric message bubbles, noted
    unwanted window resizing, and asked how Codex launches and interacts with a
    GUI during testing.
75. Asked whether TestFX should replace OS-level window automation.
76. Decided to omit automated GUI interaction because `A-MoreTesting` permits it,
    reported focus-related automation errors, and supplied separate user and
    Staniz avatar images.
77. Manually tested the GUI, confirmed it worked, and asked Codex to commit, tag,
    and push before moving to `A-Personality`.
78. Approved the proposed disciplined training-coach personality and requested
    the corresponding commit and push.
79. Moved to `A-MoreTesting`, asked whether function coverage was 100%, and asked
    which metrics determine sufficient test quality and how the repository rated.
80. Asked what further testing was advisable, including integration and mutation
    testing, and asked for an explanation of PIT.
81. Approved implementation of integration tests, coverage reporting, and PIT
    mutation testing.
82. Asked to commit, tag, and push `A-MoreTesting`, then move to
    `A-MoreErrorHandling`.
83. Selected error-handling proposals 1, 2, 3, and 6: flexible whitespace,
    specific no-argument errors, duplicate/misordered parameter detection, and
    safer atomic persistence.
84. Asked Codex to finish, commit, tag, and push those changes while the user was
    away, then move to `A-UserGuide`.
85. Approved preparation of the User Guide and said a GUI screenshot would be
    supplied later.
86. Supplied the full-window Staniz screenshot for `docs/Ui.png`.
87. Asked whether the documentation explains how to compile the JAR.
88. Asked for every command needed to build and run Staniz, plus the correct
    syntax for every in-app command.
89. Authorized the expanded build/run documentation to be committed, pushed, and
    associated with the `A-UserGuide` tag.
90. Moved to `A-Release` and asked how to publish Staniz as a GitHub Release.
91. Supplied an authored PDF reflection about using AI tools and requested a
    Developer Guide plus a summary log of the prompts from this conversation.
92. Asked to incorporate an earlier Codex task into the prompt log and place its
    prompts before those from the current project task. Those prompts were about
    Codex pricing and account setup, and were later removed from this log as not
    relevant to the project.
93. Asked to remove the separation between the two Codex tasks and combine all
    summarized prompts into one continuous chronological list.
94. Supplied the required submission contents and asked Codex to make the
    repository compliant: source code, a dependency-inclusive release JAR, User
    and Developer Guides, a reflection with at least three prompt examples, and
    prompt-summary logs in the prescribed paths.

95. Explained that the task-list application overlapped too closely with a prior
    semester's project and requested clarification before converting it into a
    gym workout application.
96. Defined one active split, up to seven workout days independent of weekdays,
    a starter exercise-name library, individual sets with kg and reps/ranges,
    optional performance recording, a repeating cycle, and a JavaFX GUI.
97. Accepted local JSON storage and confirmed that users should be able to skip
    workouts without completing them.
98. Requested an inspection of the repository and a concrete implementation
     roadmap, then authorized carrying out that plan.

99. Clarified that rep entry should accept arbitrary counts and ranges, rather
     than being limited to the placeholder examples. The hint was clarified and
     alternate counts/ranges were verified.
100. Confirmed that the workout functionality met expectations and requested UI/UX
     refinement because the first interface felt too bare.
101. Authorized active workout tracking, previous-performance comparisons, easier
     routine editing, history corrections/filtering, library rename/archive, and
     exercise progress views. These remain a local JavaFX desktop application.
102. Requested file-by-file code review using the supplied SE textbook guidelines
     and MP1 brief, with no fixed coverage, design-pattern, or method-length rules.
103. Asked how to enforce matching names and library IDs for planned exercises
     after the model review identified this validation gap.
104. Approved separating State construction and validation into focused private
     helpers while preserving existing behaviour.
105. Asked for clarification of first-use initialization, then approved creating
     current-version starter records directly and removing the legacy constructor
     while retaining old-save migration and backups.
106. Approved named constants for the current schema version and maximum workout
     days, including consistent UI limits and explicit migration versions.
107. Requested discussing review fixes before code changes, including temporary
     diagnostic programs, and maintaining a handoff read at the start of sessions.
108. Approved validation when converting saved JSON into Java records after
     discussing silent defaults for missing fields and truncation of fractions.
109. Approved showing meaningful model-validation explanations inside wrapped
     loading exceptions while retaining recovery guidance and debugging causes.
110. Approved extracting migration helpers within WorkoutStore while preserving
     IDs, ordering and backups, with a historical-only exercise regression test.
111. Discussed the distinction between fixed library exercise names and editable
     workout/history entries. Approved removing library renaming and asked which
     tests needed updating. Workout/history replacements and older saved snapshots
     remain supported; ID/name validation is still required at the service boundary.
112. Approved abstracting and reusing the duplicated completion logic in
     WorkoutService while retaining the distinct guards for each completion mode.
113. Preferred retaining performance sorting after discussing the latest-record
     query, then approved boundary tests for session indexes and performance order.
114. Approved organizing classes into model, service, storage and gui packages,
     with tests mirroring those packages and build/import references updated.
115. Resumed the WorkoutJsonValidation review from the handoff and approved
     renaming private helpers to distinguish validation actions from value readers.
116. Accepted history filters and selection resetting on refresh. Approved extracting
     the workout dialog from MainWindow and correcting its five-view Javadoc.
117. Approved named WorkoutDialog entry methods for plan editing, current-workout
     recording and history correction, sharing one private form implementation.
118. Approved decomposing the shared dialog setup into meaningful helpers and asked
     to add this responsibility-based refactoring guidance to the code-review skill.
119. Approved continuing the review and refactoring of WorkoutDialog, accepting the
     fix that runs the post-save callback outside the validation/save catch so a
     failure after a committed save cannot be shown as a retryable form error.
120. Asked for a fuller explanation of both WorkoutEditor problems, then approved
     fixing them: the editor now reports an unchosen exercise like a set error,
     naming, marking and focusing the row, and the invalid style is paired with
     the selector control in CSS so the mark actually renders and then clears.
121. Approved correcting the session set rows' accessible text, which used a raw
     0-based exercise index on the weight field and gave the reps field and the
     Done checkbox none, so every row announced only "Done" to a screen reader.
122. Asked for recommendations on the two remaining session concerns, then chose to
     keep the per-keystroke session save and to stop a failed write from erasing
     text the user is still typing, adding a GUI case and UI plan row 20a for it.
123. Asked to close the outstanding workout-editor coverage, so the GUI journey and
     UI plan row 4a now cover saving a row with no exercise chosen, checking the
     named message, the marked selector, and that both clear once one is picked.
124. Approved sharing MainWindow's pluralization helper so the progress view stops
     printing "1 exact recorded sessions", with a GUI case and UI plan row 13a
     covering the singular form at the point where it is provably reachable.
125. Asked for a fuller explanation of the progress selector's identity handling,
     then approved holding library entries in the selector instead of their names,
     removing the name-to-ID lookup and its unguarded orElseThrow.
126. Approved applying the same post-save callback fix to the copy-sets dialog, so
     a refresh failure after a committed copy cannot consume the event and leave
     the dialog refusing to close on every further confirmation.
127. Approved fixing the copy-sets error label, which kept stale messages, reserved
     layout space when empty and rendered unstyled because the dialog never loaded
     the stylesheet, and adding the first coverage of that dialog's failure path.
128. Asked whether long GUI methods are normal and whether to refactor them, then
     approved splitting the two recommended on evidence of interleaved concerns
     rather than length: MainWindow.currentView and RoutineDialogs.copySets.
129. Began the test-file reviews and approved tightening three GUI assertions that
     searched the whole scene for strings present regardless of behavior, proving
     by deliberate mutation that the originals could not fail.
130. Approved making GUI failures name the last completed scenario and enforcing
     that every numbered UI plan case is exercised, which also required declaring
     the plan as a Gradle task input so a plan-only edit reruns the suite.
131. Moved on to the backend tests and approved covering the model's referential
     State invariants, which no test exercised, taking the backend suite from 42
     to 46 tests and pinning each rule to its own message.
132. Approved exact message assertions throughout WorkoutServiceTest, since the GUI
     shows those messages verbatim and five different rules had been checked with
     five identical assertions that could not tell them apart.
133. Approved the same exact-message treatment for WorkoutImprovementsTest, where
     six checks of the active-session guard were indistinguishable from unrelated
     failures, cutting unasserted service messages from eleven to two.
134. Approved covering the last two service guards, for an unknown history record
     and an unknown library exercise, which turned out to have no test at all;
     every service message is now asserted somewhere.
135. Skipped a maintainability fix in the boundary test and approved strengthening
     the storage loop, so every load failure is checked for its recovery guidance
     and the three cases this project words are pinned to their explanations.
136. Approved a test proving a failed save deletes its temporary file and leaves the
     target untouched; the related ordering guarantee for the migration backup was
     left unpinned, since forcing that failure is platform-specific.
137. Reviewed styles.css and build.gradle, then approved a Java 25 toolchain, CI
     packaging the release JAR, and two dead-code removals.
138. Approved log and guide corrections for the deliverables, removed the earlier
     Codex pricing task from the prompt log, and added a reflection section on the
     workout conversion written from the user's own stated judgements.
139. Asked how the UI plan check would be moved out of the desktop suite, then
     approved splitting it so the plan-versus-mapping comparison runs with the
     backend tests while the desktop suite keeps only what a completed run proves.

## Recurring human decisions recorded in the original task-manager conversation

These historical preferences describe the earlier implementation. The workout
conversion above replaces its task commands and chat interface and authorizes
the updated GUI interaction test plan.

- Use the current trimmed CS3227 Project Duke brief as the authoritative scope.
- Explain every approval-gated command before running it.
- Ask before resolving any merge conflict.
- Consult the user on visual-design decisions.
- Keep the GUI green with asymmetric bubbles and supplied avatars.
- Do not add fragile automated GUI-driving tests; test shared backend behavior
  and perform GUI checks manually.
- Use lightweight milestone tags unless explicitly requested otherwise.
- Commit, tag, and push a completed level when the user asks to move on.
- Preserve human review over requirements, design trade-offs, visible output,
  and final release decisions.
