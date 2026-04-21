#!/usr/bin/env bash
set -e

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

have_mvn() { command -v mvn >/dev/null 2>&1; }
have_docker() { command -v docker >/dev/null 2>&1; }

run_suite_mvn() {
  local name="$1"
  local dir="$2"
  echo ""
  echo "========================================"
  echo " Running: $name"
  echo "========================================"
  if (cd "$dir" && mvn test -q 2>&1); then
    echo "[PASS] $name"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name"
    FAIL=$((FAIL + 1))
  fi
}

run_suite_docker() {
  local name="$1"
  local rel_dir="$2"
  echo ""
  echo "========================================"
  echo " Running (docker): $name"
  echo "========================================"
  if docker run --rm \
       -v "$REPO_DIR:/work" \
       -v "$HOME/.m2:/root/.m2" \
       -w "/work/$rel_dir" \
       maven:3.9.6-eclipse-temurin-17 \
       mvn test -q 2>&1; then
    echo "[PASS] $name"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name"
    FAIL=$((FAIL + 1))
  fi
}

if have_mvn; then
  run_suite_mvn "Unit Tests (service layer)" "$REPO_DIR/unit_tests"
  run_suite_mvn "API Integration Tests (MockMvc)" "$REPO_DIR/API_tests"
elif have_docker; then
  echo "Maven not found on host — running test suites in docker..."
  run_suite_docker "Unit Tests (service layer)" "unit_tests"
  run_suite_docker "API Integration Tests (MockMvc)" "API_tests"
else
  echo "ERROR: Neither Maven nor Docker is available on PATH."
  echo "       Install Java 17 + Maven, or install Docker."
  exit 2
fi

echo ""
echo "========================================"
echo " Results: $PASS passed, $FAIL failed"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
