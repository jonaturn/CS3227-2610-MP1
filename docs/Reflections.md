# Reflection on Using AI Tools

**Jonathen Cheng Yuzhe - A0273210W**

I built Staniz using Codex as the primary implementation tool across the
incremental levels of the project brief. This reflection describes where that
helped, where it did not, how prompting affected the engineering process, and
what judgement I still had to supply myself.

## Where the AI clearly helped

The biggest win was persistent context. I gave Codex the project brief and the
relevant documentation chapters once, and it carried that context across much
of the work instead of making me re-explain conventions at every step.
Recording durable instructions in `AGENTS.md` and a session handoff file helped
those decisions survive across sessions too.

The largest overall gain was not having to read documentation down to the last
implementation detail. I could read at a high level for understanding, then use
that understanding to check what the AI produced. Reversing the usual order -
understanding first and requesting details on demand - cut my development time
by a wide margin.

Project setup and configuration, which I have always found painful,
essentially stopped being a concern. Adding libraries, wiring them into the
Gradle build, and reorganising the code into packages went from an afternoon of
fighting build files to a short exchange. The agent could also explain why a
layout or dependency scope was appropriate. Even when configuring Java 25, I
could ask Codex to guide me through switching from Java 21.

Testing was the other major benefit. I used to find it tedious enough to
postpone, but with the agent handling implementation, I stayed disciplined
about testing every increment and started earlier. Usually I would begin
testing only after writing a group of features; this time I implemented tests
as I tackled each increment. My role shifted to supplying boundary cases,
checking that tests were meaningful rather than tautological, and monitoring
runs. The project ended with JUnit tests spanning the parser, storage, task,
command, formatting, and integration layers, plus a thirteen-case end-to-end
console plan. That is far more coverage than I would have written by hand.

The GUI work was easier than expected, and the output went beyond what I asked.
When building the JavaFX interface, the agent configured circular clipping for
the avatar images I supplied so they matched the convention of familiar chat
applications. That was an extra step I would probably have skipped.

## Interesting prompt examples

### 1. Turning the project brief into an approval-gated Level 0 plan

**Prompt (abridged):** "This is my project briefing. I want to finish this
project as soon as possible. Start with Level 0 - rename, greet, exit. List the
tasks and the steps for each subtask. Every code change should show a diff and
ask for approval. Follow the supplied code-quality, refactoring,
documentation, and error-handling guidance."

This prompt was effective because it supplied four kinds of context together:
the authoritative requirements, the immediate scope, the process constraint,
and the engineering standards. Codex responded with a small Level 0 plan
instead of attempting the entire project. Requiring a proposed diff before
each edit gave me a review checkpoint where I could confirm both the code and
the interpretation of the brief.

The important lesson was that a good implementation prompt does not need to
dictate the Java statements. It should instead establish what source is
authoritative, what outcome is required, and how risky decisions are approved.
That kept the AI useful without transferring final design responsibility to it.
The same pattern later became a standing workflow for incremental changes.

### 2. Exploring stateless and stateful parser designs before approval

**Prompts (abridged):** "What will the parser do and how will you implement
it?" followed by "Approved, although I am curious what a stateful parser would
look like."

These prompts were valuable because they delayed implementation until I
understood the responsibility of the proposed class. Codex explained how a
stateless parser receives a complete command, validates it, and returns typed
values without remembering previous commands. It also described the more
complex alternative: an object that retains partial input or parsing state
between calls.

The comparison made the design decision clearer. Staniz commands are complete
in one line, so retained parsing state would create lifecycle and testing
complexity without solving a real requirement. I approved the stateless design
after understanding the rejected alternative, rather than accepting the first
suggestion merely because it worked. This was an example of using an LLM as a
design explainer as well as a code generator.

### 3. Expanding testing beyond a raw coverage percentage

**Prompts (abridged):** "What metrics determine code coverage and whether
there are sufficient tests?", "What would you suggest next - integration
testing, mutation testing, and so on?", "What is PIT?", and finally "Let's
implement all three."

This sequence shows how short follow-up prompts can progressively refine a
technical decision when the conversation retains context. The first question
challenged the assumption that 100% line coverage alone means the tests are
good. The follow-ups led to integration tests, JaCoCo reporting, and PIT
mutation testing. PIT deliberately changes program behaviour and checks
whether the test suite detects the change, which provided evidence that the
tests asserted meaningful outcomes instead of merely executing lines.

The final prompt, "implement all three", would be poor in isolation because
"three" is ambiguous. It worked only because the previous messages had defined
the exact scope. This taught me that conversational prompting is efficient, but
important decisions should still be restated in durable documentation or a
handoff before changing sessions.

### 4. Correcting plausible but non-compliant visible output

**Prompts (abridged):** "Shouldn't an incomplete task show an empty marker
rather than `?`?" and "Done uses `X`; undone is blank."

Codex had produced status symbols that looked reasonable but did not match the
brief's required `[X]` and `[ ]` convention. My correction was short because it
referred to a precise visible mismatch. The agent then proposed a marker-only
change, which I could review independently from the rest of the Level 3 code.

This was one of the most important interactions because the incorrect output
was plausible. Compilation and ordinary unit tests would not necessarily reveal
that it violated the product specification. It demonstrated why I had to run
the application, compare output with the brief, and treat user-visible wording
and formatting as testable requirements.

## Where the AI fell short

The most persistent issue was hallucination of small details. Even with the
project brief available, Codex once rendered alternative symbols in the list
output instead of the `[X]` and `[ ]` marked and unmarked convention the brief
specifies. The output looked perfectly plausible, which is exactly what makes
this failure mode dangerous. I caught it only by running the application and
comparing the output line by line against the sample in the brief. From that
point on, I specifically told it to run tests and show me the output for each
case so I could check it manually.

Some tasks also sat at the edge of what the agent could do well. When the
project gained a JavaFX GUI, automated GUI testing became a problem. Codex
produced a Python script that drove the application through Windows SendKeys.
It passed, but was fragile and machine-dependent. TestFX would have been the
proper approach, but it was not worth the additional complexity for this
trimmed project, and the briefing confirmed that GUI automation was not
required. That trade-off was an engineering decision the agent could not make
for me.

Long sessions degraded noticeably. As context grew, Codex forgot standing
instructions, such as my requirement that it break down the syntax and purpose
of a command before asking for permission to run it. I had to keep restating
rules, and eventually moved the durable ones into `AGENTS.md` and the handoff
file so they were re-read rather than remembered.

It also got sidetracked on sources. The trimmed brief links to the main course
website, and I caught Codex treating the main site as the absolute source of
truth despite my repeated instruction to follow the trimmed version. If left
unchecked, it would have implemented requirements outside the intended scope,
so I had to verify which page its plans actually came from.

Finally, the structure of the assignment capped the throughput I could get from
the tool. Because the project was a strict sequence of incremental levels, each
increment depended on the last, so I could point the agent at only one feature
at a time. In a codebase with more loosely coupled work, I could have run
several agent instances in parallel and spent my own time reviewing their
output rather than waiting on it, a pattern I picked up during a previous
internship. The bottleneck here was the sequential shape of the project rather
than the tool, but it meant I never used the AI at the rate it was capable of.

## What I would do differently

Two changes would have saved me the most time. First, I would write the standing
rules into `AGENTS.md` at the start rather than discovering them through
repeated correction, including an explicit instruction that the trimmed brief
is the authoritative source and that linked pages are not. This is necessarily
iterative because some useful rules become apparent only during the work.

Second, I would treat every user-visible output format as something to verify
against the brief, as that is precisely the category the AI gets confidently
wrong. Overall, it was better to keep decision-making responsibility myself
while handing technical work to the agent and manually checking the result for
correctness.
