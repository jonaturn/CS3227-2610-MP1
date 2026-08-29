#!/usr/bin/env python3
"""Compile Staniz with Java 25 and verify one command-line UI test case."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


def parse_arguments() -> argparse.Namespace:
    """Return validated command-line arguments for a single UI test case."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--case", required=True, help="descriptive test case name")
    parser.add_argument(
        "--command",
        action="append",
        dest="commands",
        required=True,
        help="command to send to Staniz; repeat to supply a list",
    )
    parser.add_argument(
        "--expect",
        action="append",
        dest="expected_fragments",
        required=True,
        help="output fragment expected in order; repeat to supply a list",
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


def resolve_java_tool(tool_name: str, java_home: Path | None) -> str:
    """Resolve a Java executable from an override, JAVA_HOME, or PATH."""
    executable_name = f"{tool_name}.exe" if os.name == "nt" else tool_name
    candidate_home = java_home or (Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else None)
    if candidate_home:
        candidate = candidate_home / "bin" / executable_name
        if candidate.is_file():
            return str(candidate)

    executable = shutil.which(tool_name)
    if executable:
        return executable

    raise RuntimeError(f"Could not find {tool_name}. Install Java 25 or pass --java-home.")


def require_java_25(java_executable: str) -> None:
    """Fail when the selected runtime is not Java 25."""
    result = subprocess.run(
        [java_executable, "--version"],
        capture_output=True,
        text=True,
        check=False,
    )
    version_output = (result.stdout + result.stderr).strip()
    first_line = version_output.splitlines()[0] if version_output else "unknown version"
    if result.returncode != 0 or not first_line.startswith(("java 25", "openjdk 25")):
        raise RuntimeError(f"Java 25 is required, but the selected runtime reports: {first_line}")


def compile_sources(javac_executable: str, source_dir: Path, classes_dir: Path) -> None:
    """Compile every Java source file into the temporary classes directory."""
    sources = sorted(source_dir.glob("*.java"))
    if not sources:
        raise RuntimeError(f"No Java sources found in {source_dir.resolve()}")

    result = subprocess.run(
        [javac_executable, "-d", str(classes_dir), *(str(source) for source in sources)],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        diagnostics = (result.stdout + result.stderr).strip()
        raise RuntimeError(f"Compilation failed:\n{diagnostics}")


def run_application(
    java_executable: str,
    classes_dir: Path,
    main_class: str,
    commands: list[str],
) -> tuple[int, str, str]:
    """Run the application with the supplied commands and capture its output."""
    command_input = "\n".join(commands) + "\n"
    result = subprocess.run(
        [java_executable, "-cp", str(classes_dir), main_class],
        input=command_input,
        capture_output=True,
        text=True,
        check=False,
    )
    return result.returncode, result.stdout.replace("\r\n", "\n"), result.stderr.replace("\r\n", "\n")


def first_missing_fragment(output: str, expected_fragments: list[str]) -> str | None:
    """Return the first expected fragment absent from its required order."""
    search_from = 0
    for fragment in expected_fragments:
        fragment_index = output.find(fragment, search_from)
        if fragment_index == -1:
            return fragment
        search_from = fragment_index + len(fragment)
    return None


def print_transcript(commands: list[str], output: str, error_output: str) -> None:
    """Print the exact console interaction for human review."""
    print("--- INPUT ---")
    for command in commands:
        print(f"> {command}")
    print("--- OUTPUT ---")
    print(output, end="" if output.endswith("\n") else "\n")
    if error_output:
        print("--- ERROR OUTPUT ---")
        print(error_output, end="" if error_output.endswith("\n") else "\n")


def main() -> int:
    """Compile, run, verify, and report one UI test case."""
    arguments = parse_arguments()

    try:
        java_executable = resolve_java_tool("java", arguments.java_home)
        javac_executable = resolve_java_tool("javac", arguments.java_home)
        require_java_25(java_executable)

        with tempfile.TemporaryDirectory(prefix="staniz-ui-test-") as temporary_directory:
            classes_dir = Path(temporary_directory)
            compile_sources(javac_executable, arguments.source_dir, classes_dir)
            return_code, output, error_output = run_application(
                java_executable,
                classes_dir,
                arguments.main_class,
                arguments.commands,
            )
    except RuntimeError as error:
        print(f"FAIL [{arguments.case}]", file=sys.stderr)
        print(error, file=sys.stderr)
        return 1

    print_transcript(arguments.commands, output, error_output)

    if return_code != 0:
        print(f"FAIL [{arguments.case}]", file=sys.stderr)
        print(f"Expected exit code 0, actual exit code {return_code}.", file=sys.stderr)
        return 1

    missing_fragment = first_missing_fragment(output, arguments.expected_fragments)
    if missing_fragment is not None:
        print(f"FAIL [{arguments.case}]", file=sys.stderr)
        print("First missing expected output fragment:", file=sys.stderr)
        print(missing_fragment, file=sys.stderr)
        return 1

    print(f"PASS [{arguments.case}]")
    return 0


if __name__ == "__main__":
    sys.exit(main())
