#!/usr/bin/env bash
# run.sh — Helper script to run Bend and HVM commands on WSL2
set -euo pipefail

export PATH="$HOME/.cargo/bin:$PATH"

if ! command -v bend >/dev/null 2>&1; then
    echo "Error: 'bend' is not installed or not in PATH ($PATH)." >&2
    echo "Install via: cargo install bend-lang hvm" >&2
    exit 1
fi

SUBCOMMAND="${1:-help}"

case "$SUBCOMMAND" in
    run)
        shift
        bend run "$@"
        ;;
    run-c)
        shift
        bend run-c "$@"
        ;;
    run-cu)
        shift
        bend run-cu "$@"
        ;;
    check)
        shift
        bend check "$@"
        ;;
    test)
        shift
        echo "=== Running Tests (bend run) ==="
        found=0
        if [ -d tests ]; then
            for f in tests/test_*.bend; do
                if [ -f "$f" ]; then
                    echo "--- Testing $f ---"
                    bend run "$f" "$@"
                    found=1
                fi
            done
        fi
        if [ "$found" -eq 0 ]; then
            echo "No test files matching tests/test_*.bend found."
        fi
        ;;
    bench)
        shift
        echo "=== Running Benchmark (bend run-c) ==="
        if [ -f "bench/benchmark.bend" ]; then
            bend run-c bench/benchmark.bend "$@"
        else
            echo "bench/benchmark.bend not found."
            exit 1
        fi
        ;;
    smoke|smoke-test)
        echo "=== Running Smoke Test (Bend Interpreter & C Backend) ==="
        tmp_bend=$(mktemp --suffix=.bend)
        cat << 'EOF' > "$tmp_bend"
def main():
  bend x = 0:
    when x < 10:
      val = fork(x + 1) + fork(x + 1)
    else:
      val = 1
  return val
EOF
        echo "Testing 'bend run' (Interpreter)..."
        res_interp=$(bend run "$tmp_bend")
        echo "$res_interp"
        echo "Testing 'bend run-c' (C multithreaded backend)..."
        res_c=$(bend run-c "$tmp_bend")
        echo "$res_c"
        rm -f "$tmp_bend"
        if [ "$res_interp" = "Result: 1024" ] && [ "$res_c" = "Result: 1024" ]; then
            echo "Smoke test passed successfully!"
        else
            echo "Smoke test unexpected result!" >&2
            exit 1
        fi
        ;;
    version|--version|-v)
        echo "Bend version: $(bend --version)"
        if command -v hvm >/dev/null 2>&1; then
            echo "HVM version:  $(hvm --version)"
        fi
        ;;
    help|--help|-h)
        echo "Usage: ./run.sh <command> [args...]"
        echo ""
        echo "Commands:"
        echo "  run <file.bend>       Run Bend file with interpreter"
        echo "  run-c <file.bend>     Run Bend file with C multithreaded backend"
        echo "  run-cu <file.bend>    Run Bend file with CUDA backend (requires NVIDIA GPU)"
        echo "  check <file.bend>     Type and syntax check a Bend file"
        echo "  test                  Run all tests in tests/test_*.bend"
        echo "  bench [args]          Run benchmark in bench/benchmark.bend"
        echo "  smoke                 Run parallel tree smoke test"
        echo "  version               Print bend and hvm versions"
        ;;
    *)
        bend "$SUBCOMMAND" "$@"
        ;;
esac
