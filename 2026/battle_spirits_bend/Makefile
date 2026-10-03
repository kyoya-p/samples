# Makefile for Battle Spirits Bend Engine

SHELL := /bin/bash
export PATH := $(HOME)/.cargo/bin:$(PATH)

BEND ?= bend
HVM ?= hvm

.PHONY: all check test bench smoke-test help

all: check

check:
	@echo "=== Checking Bend syntax ==="
	@found=0; \
	for dir in src tests bench; do \
		if [ -d "$$dir" ]; then \
			for f in "$$dir"/*.bend; do \
				if [ -f "$$f" ]; then \
					echo "Checking $$f..."; \
					$(BEND) check "$$f" || exit 1; \
					found=1; \
				fi; \
			done; \
		fi; \
	done; \
	if [ "$$found" -eq 0 ]; then \
		echo "No .bend files found to check yet."; \
	else \
		echo "All Bend files passed check."; \
	fi

test:
	@echo "=== Running Tests (bend run) ==="
	@found=0; \
	if [ -d tests ]; then \
		for f in tests/test_*.bend; do \
			if [ -f "$$f" ]; then \
				echo "Testing $$f..."; \
				$(BEND) run "$$f" || exit 1; \
				found=1; \
			fi; \
		done; \
	fi; \
	if [ "$$found" -eq 0 ]; then \
		echo "No test files matching tests/test_*.bend found."; \
	else \
		echo "All tests completed."; \
	fi

bench:
	@echo "=== Running Benchmark (bend run-c) ==="
	@if [ -f bench/benchmark.bend ]; then \
		$(BEND) run-c bench/benchmark.bend; \
	else \
		echo "bench/benchmark.bend not found. Please implement benchmark first."; \
		exit 1; \
	fi

smoke-test:
	@bash ./run.sh smoke

help:
	@echo "Battle Spirits Bend Engine - Build & Test Targets"
	@echo ""
	@echo "Targets:"
	@echo "  make check      - Type and syntax check all .bend files"
	@echo "  make test       - Run all test suites via bend run (CPU interpreter)"
	@echo "  make bench      - Run benchmark via bend run-c (C multithreaded)"
	@echo "  make smoke-test - Run smoke test to verify Bend interpreter and C backend"
	@echo "  make help       - Display this help message"
