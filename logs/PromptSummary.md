# Staniz AI Prompt Summary Log

**Project:** Staniz MP1

**AI tool:** OpenAI Codex desktop application

**Period represented:** 28 August to 1 September 2026
**Prepared from:** The `Explain payment and pricing` Codex task, the active Codex
project task, and resulting Git history

## Scope and method

This is a chronological, paraphrased summary rather than a verbatim chat export.
It records substantive requests, questions, approvals, and corrections that
affected the project. Repeated approval-only messages, tool output, and ambient
browser context have been condensed. Where several short prompts formed one
decision, they are summarized together.

## Prompt summary

1. Asked for an explanation of Codex payment and pricing before beginning the
   project.
2. Asked whether NUS would provide students with a free ChatGPT plan from 31
   August onward.
3. Supplied the current CS3227 Project Duke brief and software-engineering
   references, requested a fast Level 0 plan for rename, greet, and exit, and
   required every code change to be shown as a diff for approval.
4. Chose `Staniz` as the chatbot's name.
5. Asked for a Staniz ASCII banner to be included in Level 0.
6. Approved the proposed Level 0 rename, greeting, banner, and farewell changes.
7. Asked for help filling the `[to be filled]` student-profile fields in the
   repository's Markdown instructions.
8. Asked whether the listed development environment should be Codex because
   Codex was being used to implement the project.
9. Clarified that the preferred IDE was Visual Studio Code, used extensively
   across internships and personal projects, and described the programming
   experience level as fairly experienced and soon to graduate.
10. Approved the resulting student-profile update.
11. Asked to ideate a setup in which Claude reviews every pull request.
12. Corrected the request from Claude review to native Codex code review.
13. Asked for a step-by-step walkthrough of the Codex code-review setup plan.
14. Reported that the Code review option was not visible in Codex settings.
15. Asked whether the current task was running locally rather than in Codex
    cloud and why it did not appear in the web settings.
16. Asked why the local repository was available in Codex desktop while the
    Codex web page instead prompted installation of the ChatGPT application.
17. Asked to continue with the first project setup step.
18. Asked why Java 25 was not available in the terminal environment.
19. Asked Codex to install Java 25.
20. Asked to begin Level 1.
21. Asked how to compile the application into a JAR file.
22. Asked how the course guidance says Level 1 should be tested and whether JAR
    compilation was required at that stage.
23. Asked how to remove the generated JAR after testing.
24. Confirmed Level 1 was complete and asked to proceed to the next part.
25. Asked how to carry out the first item in a previously supplied Git
    procedure.
26. Asked how to create and use the `Level-0` Git tag.
27. Asked why the commit and tag had not been pushed yet.
28. Clarified that both the commit and tag had already been pushed manually.
29. Approved the proposed Level 2 echo-loop implementation.
30. Asked why running `javac Staniz.java` directly did not work.
31. Asked what the `-d` and `-cp` options mean in the Java compilation and
    execution commands.
32. Asked how to inspect the repository's Git tags.
33. Asked to move on to Level 3.
34. Confirmed an intervening step was complete and asked to continue.
35. Approved the proposed two-file Level 3 task-list implementation.
36. Asked whether `Task.java` also had to be compiled.
37. Asked whether an incomplete task should display an empty marker instead of
    a question mark.
38. Clarified that completed tasks should display `X` and incomplete tasks a
    blank marker.
39. Approved the marker-only correction.
40. Reported that repeatedly pressing Enter added blank tasks and that `mark 1`
    did not work.
41. Approved the proposed bug fixes and Javadoc updates.
42. Asked to include separator lines around the response to the `unmark`
    command.
43. Asked why the proposed diff removed so much code and replaced direct
    `System.out.println` calls with a `printResponse` helper.
44. Approved the response-separator refactor.
45. Asked Codex to inspect the repository and report the project's current state.
46. Asked it to read the Codex session handoff file and reconcile the handoff with
   the repository.
47. Challenged the initial conclusion that Git tags were absent, explained that
   tags had been created locally, and asked whether they had not been pushed.
48. Authorized Codex to add the missing milestone tags to the corresponding
   commits.
49. Asked to continue with `A-MoreOOP`, implementing the UI separation first.
50. Asked what `TaskList` does and how it replaces the previous raw task data
   structure.
51. Asked what the parser should do, how it would be implemented, and what a
   stateful-parser alternative would look like before approving the stateless
   design.
52. Proposed dividing the new classes into `parser`, `task`, and `command`
   packages, then approved the package reorganization.
53. Asked to move to Gradle, approved the proposed setup, and asked what Gradle
   contributes to the project and how to verify it manually.
54. Reported that `bye` stopped working after the Gradle transition and asked for
    the cause to be investigated.
55. Asked to continue to `A-JUnit` and create tests for every non-trivial method
    in every class.
56. Asked whether tests should be grouped by class following the SE-EDU JUnit
    guide, what the Gradle test command does, and what successful output should
    look like.
57. Shared a successful Gradle build transcript and asked to continue to
    `A-Jar`.
58. Asked whether `A-Gradle` had produced changes and requested a summary of the
    SE-EDU Git/coding conventions and how they could be applied.
59. Corrected the planned sequence to Level 9 and Checkstyle, then asked for the
    coding-standard changes to be implemented first.
60. Asked why some classes and methods were made private while others remained
    public.
61. Established a standing instruction that every command approval request must
    explain the command's syntax, purpose, and effects.
62. Asked to move to Level 9, and established a standing release workflow: when
    moving on, stage, commit, create the correct lightweight milestone tag, and
    push the commit and tag.
63. Asked to move to Checkstyle, reviewed its proposed changes, and approved the
    implementation and publication.
64. Established a standing rule that any merge conflict must be shown and
    explicitly approved before resolution.
65. Asked why some Checkstyle changes appeared identical to the previous code,
    then approved the remaining changes.
66. Asked Codex to redo an interrupted task, locate the generated JAR, and explain
    how to run the application.
67. Questioned why an `A-Varargs` branch had been created when the trimmed course
    brief did not list that level, then supplied the current CS3227 Project Duke
    page as the authoritative brief.
68. Asked exactly what changed in `A-Varargs`, confirmed those changes could
    remain, and asked whether the JavaFX tutorial features had been implemented.
69. Directed Codex to follow the tutorial documentation more closely and pointed
    out that launching the GUI without entering commands did not test its
    behavior.
70. Asked to continue to `A-Assertions`, explicitly excluding the older course
    website and using only the current trimmed brief.
71. Approved the assertions and moved to `A-CodeQuality`, while reminding Codex
    to explain command syntax before approval requests.
72. Approved the code-quality changes and moved to `A-CI`.
73. Asked what continuous integration does, requested a detailed walkthrough of
    the YAML workflow, and asked how GitHub Actions creates and configures jobs.
74. Reported that CI had been committed and pushed, then moved to `A-BetterGui`.
75. Required consultation before decisions about visual design, including avatar
    and color choices.
76. Selected green instead of blue, retained asymmetric message bubbles, noted
    unwanted window resizing, and asked how Codex launches and interacts with a
    GUI during testing.
77. Asked whether TestFX should replace OS-level window automation.
78. Decided to omit automated GUI interaction because `A-MoreTesting` permits it,
    reported focus-related automation errors, and supplied separate user and
    Staniz avatar images.
79. Manually tested the GUI, confirmed it worked, and asked Codex to commit, tag,
    and push before moving to `A-Personality`.
80. Approved the proposed disciplined training-coach personality and requested
    the corresponding commit and push.
81. Moved to `A-MoreTesting`, asked whether function coverage was 100%, and asked
    which metrics determine sufficient test quality and how the repository rated.
82. Asked what further testing was advisable, including integration and mutation
    testing, and asked for an explanation of PIT.
83. Approved implementation of integration tests, coverage reporting, and PIT
    mutation testing.
84. Asked to commit, tag, and push `A-MoreTesting`, then move to
    `A-MoreErrorHandling`.
85. Selected error-handling proposals 1, 2, 3, and 6: flexible whitespace,
    specific no-argument errors, duplicate/misordered parameter detection, and
    safer atomic persistence.
86. Asked Codex to finish, commit, tag, and push those changes while the user was
    away, then move to `A-UserGuide`.
87. Approved preparation of the User Guide and said a GUI screenshot would be
    supplied later.
88. Supplied the full-window Staniz screenshot for `docs/Ui.png`.
89. Asked whether the documentation explains how to compile the JAR.
90. Asked for every command needed to build and run Staniz, plus the correct
    syntax for every in-app command.
91. Authorized the expanded build/run documentation to be committed, pushed, and
    associated with the `A-UserGuide` tag.
92. Moved to `A-Release` and asked how to publish Staniz as a GitHub Release.
93. Supplied an authored PDF reflection about using AI tools and requested a
    Developer Guide plus a summary log of the prompts from this conversation.
94. Asked to incorporate the earlier `Explain payment and pricing` task into the
    prompt log and place its prompts before those from the current project task.
95. Asked to remove the separation between the two Codex tasks and combine all
    summarized prompts into one continuous chronological list.
96. Supplied the required submission contents and asked Codex to make the
    repository compliant: source code, a dependency-inclusive release JAR, User
    and Developer Guides, a reflection with at least three prompt examples, and
    prompt-summary logs in the prescribed paths.

## Recurring human decisions recorded in the conversation

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
