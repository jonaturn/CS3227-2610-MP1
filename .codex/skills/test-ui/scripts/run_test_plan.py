#!/usr/bin/env python3
"""Run every UI test case documented in a Staniz Markdown test plan."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import re
import sys
import tempfile

from run_ui_tests import (
    compile_sources,
    first_missing_fragment,
    print_transcript,
    require_java_25,
    resolve_java_tool,
    run_application,
)


CASE_PATTERN = re.compile(
    r"^## Case \d+: (?P<name>[^\n]+)\n\n"
    r"Aim: (?P<aim>[^\n]+)\n\n"
    r"Input:\n\n```text\n(?P<commands>.*?)\n```\n\n"
    r"Expected output:\n\n```text\n(?P<expected>.*?)\n```",
    re.MULTILINE | re.DOTALL,
)


@dataclass(frozen=True)
class UiTestCase:
    """Contains one console interaction and its expected output fragments."""

    name: str
    aim: str
    commands: list[str]
    expected_fragments: list[str]


def parse_arguments() -> argparse.Namespace:
    """Return command-line settings for running a complete test plan."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--plan",
        type=Path,
        default=Path("test/ui-test-plan.md"),
        help="Markdown test plan to execute",
    )
    parser.add_argument(
        "--source-dir",
        type=Path,
        default=Path("src/main/java"),
        help="directory containing the Java sources",
    )
    parser.add_argument(
        "--main-class",
        default="Staniz",
        help="fully qualified application entry point",
    )
    parser.add_argument(
        "--java-home",
        type=Path,
        help="Java 25 installation directory when it is not active on PATH",
    )
    return parser.parse_args()


def parse_test_plan(plan_path: Path) -> list[UiTestCase]:
    """Parse all cases from the documented Markdown test-plan structure."""
    if not plan_path.is_file():
        raise RuntimeError(f"Test plan not found: {plan_path.resolve()}")

    plan_text = plan_path.read_text(encoding="utf-8").replace("\r\n", "\n")
    declared_case_count = len(re.findall(r"^## Case \d+:", plan_text, re.MULTILINE))
    test_cases = [
        UiTestCase(
            name=match.group("name").strip(),
            aim=match.group("aim").strip(),
            commands=match.group("commands").splitlines(),
            expected_fragments=match.group("expected").splitlines(),
        )
        for match in CASE_PATTERN.finditer(plan_text)
    ]

    if not test_cases:
        raise RuntimeError(f"No test cases found in {plan_path.resolve()}")
    if len(test_cases) != declared_case_count:
        raise RuntimeError(
            "The test plan contains a malformed case. Each case needs a heading, "
            "one-line aim, input text block, and expected-output text block."
        )
    return test_cases


def run_test_case(
    test_case: UiTestCase,
    java_executable: str,
    classes_dir: Path,
    main_class: str,
) -> bool:
    """Execute one case, print its transcript, and report whether it passed."""
    print(f"=== {test_case.name} ===")
    print(f"Aim: {test_case.aim}")
    return_code, output, error_output = run_application(
        java_executable,
        classes_dir,
        main_class,
        test_case.commands,
    )
    print_transcript(test_case.commands, output, error_output)

    if return_code != 0:
        print(f"FAIL [{test_case.name}]", file=sys.stderr)
        print(f"Expected exit code 0, actual exit code {return_code}.", file=sys.stderr)
        return False

    missing_fragment = first_missing_fragment(output, test_case.expected_fragments)
    if missing_fragment is not None:
        print(f"FAIL [{test_case.name}]", file=sys.stderr)
        print("First missing expected output fragment:", file=sys.stderr)
        print(missing_fragment, file=sys.stderr)
        return False

    print(f"PASS [{test_case.name}]\n")
    return True


def main() -> int:
    """Compile once and execute the complete UI plan with fail-fast behavior."""
    arguments = parse_arguments()

    try:
        test_cases = parse_test_plan(arguments.plan)
        java_executable = resolve_java_tool("java", arguments.java_home)
        javac_executable = resolve_java_tool("javac", arguments.java_home)
        require_java_25(java_executable)

        with tempfile.TemporaryDirectory(prefix="staniz-ui-suite-") as temporary_directory:
            classes_dir = Path(temporary_directory)
            compile_sources(javac_executable, arguments.source_dir, classes_dir)
            for test_case in test_cases:
                if not run_test_case(
                    test_case,
                    java_executable,
                    classes_dir,
                    arguments.main_class,
                ):
                    return 1
    except RuntimeError as error:
        print("FAIL [UI test plan]", file=sys.stderr)
        print(error, file=sys.stderr)
        return 1

    print(f"PASS [UI test suite: {len(test_cases)}/{len(test_cases)} cases]")
    return 0


if __name__ == "__main__":
    sys.exit(main())
