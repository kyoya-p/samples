# DISPATCH — Worker (Milestone 1: Environment & Tooling Setup)

## Task Description
Set up Bend and HVM execution environment in WSL2 (Ubuntu 24.04), create runner script and Makefile in `battle_spirits_bend`, and verify smoke execution.

## Identity & Working Directory
- Identity: teamwork_preview_worker_m1
- Working Directory: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_worker_m1
- Target Codebase: C:\Users\kyoya\home26\works\samples\2026\battle_spirits_bend

## Inputs
- ORIGINAL_REQUEST.md: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md
- PROJECT.md: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\orchestrator_1\PROJECT.md
- Explorer 1 Handoff: C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_explorer_survey_1\handoff.md

## File Write Ownership
You exclusively own:
- `battle_spirits_bend/run.sh`
- `battle_spirits_bend/Makefile`
- `battle_spirits_bend/README.md`
Do NOT write to other files yet.

## Scope & Instructions
1. In WSL2 (`Ubuntu-24.04`), ensure `bend` and `hvm` are installed and runnable.
   - Run command in WSL: `export PATH="$HOME/.cargo/bin:$PATH"`
   - If not installed, install via: `wsl bash -c "export PATH=\$HOME/.cargo/bin:\$PATH && cargo install bend-lang hvm"`
   - Note: cargo build may take a few minutes. Check `bend --version` and `hvm --version`.
2. Create `battle_spirits_bend/run.sh` and `battle_spirits_bend/Makefile` providing convenient test and bench commands:
   - `make test` -> runs test suite via `bend run`
   - `make bench` -> runs benchmark via `bend run-c`
   - `make check` -> syntax checks
3. Execute a smoke test with a simple `.bend` file (e.g. testing `bend run` and `bend run-c`) in WSL2 to prove the compiler and C parallel backend work properly.
4. Record build and execution output in your handoff report at `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_worker_m1\handoff.md`.

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.


## 2026-10-03T04:12:53Z
You are teamwork_preview_worker_m1. Your working directory is C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_worker_m1. Read your task assignment at C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_worker_m1\DISPATCH.md and C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md. Set up Bend and HVM on WSL2 (Ubuntu 24.04), create run.sh and Makefile in battle_spirits_bend, verify execution with a smoke test in bend run and bend run-c, write handoff.md in your working directory and notify the orchestrator.
MANDATORY INTEGRITY WARNING: DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
