---
name: test-ui
description: Compile and test the Staniz command-line UI using command lists and expected output fragments. Use after changes that affect user-visible console behavior.
---

# Test the Staniz UI

Use `scripts/run_test_plan.py` to run the complete documented UI suite. It compiles all Java sources once in a temporary directory, requires Java 25, executes each case from `test/ui-test-plan.md`, checks expected output fragments in order, and prints every input/output transcript.

```powershell
python .codex/skills/test-ui/scripts/run_test_plan.py
```

Run every applicable case in `test/ui-test-plan.md` after a code update that affects console behavior. Stop at the first failing case and report:

- the case and its aim;
- the commands entered;
- the first expected fragment that was not found;
- the actual console transcript.

Use `scripts/run_ui_tests.py` when an ad hoc or single-case check is more appropriate:

```powershell
python .codex/skills/test-ui/scripts/run_ui_tests.py `
  --case "Create and list all task types" `
  --command "todo borrow book" `
  --command "list" `
  --command "bye" `
  --expect "added: [T][ ] borrow book" `
  --expect "1.[T][ ] borrow book"
```

If `python` is unavailable, use the Codex bundled Python runtime. If Java 25 is installed but is not the active terminal runtime, pass its installation directory with `--java-home`.

Keep `test/ui-test-plan.md` synchronized whenever commands or visible output change. Expected fragments should be specific enough to prove the behavior while excluding decorative separators and the banner unless those are the behavior under test.
