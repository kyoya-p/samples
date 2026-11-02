# Battle Spirits Bend Engine

Massively parallel game engine and simulator for the Battle Spirits trading card game in the Bend programming language.

## Prerequisites

- **WSL2** (Ubuntu 24.04 or POSIX Linux)
- **Rust toolchain** (1.74+)
- **GCC** (13.3.0+)
- **Bend & HVM**:
  ```bash
  cargo install bend-lang hvm
  ```

## Development & Execution

Helper commands via `make` or `./run.sh`:

- **Syntax & Type Check**:
  ```bash
  make check
  # or ./run.sh check <file.bend>
  ```
- **Run Tests (Interpreter)**:
  ```bash
  make test
  # or ./run.sh test
  ```
- **Run Benchmark (C Multithreaded)**:
  ```bash
  make bench
  # or ./run.sh bench
  ```
- **Direct Execution**:
  ```bash
  ./run.sh run <path_to_file.bend>     # CPU Interpreter
  ./run.sh run-c <path_to_file.bend>   # C Multithreaded Backend
  ```
