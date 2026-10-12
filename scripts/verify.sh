#!/usr/bin/env bash
# Every check a change must pass. Contributors run it locally and the
# maintainers' CI runs the same script, so both stay in sync.
# Requires JDK 25, Node.js 24 and Docker (Testcontainers).
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> backend: unit, integration and architecture tests"
./mvnw -B -ntp -f backend verify

echo "==> frontend: install, lint, test, typecheck and build"
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run lint
npm --prefix frontend test
npm --prefix frontend run build

echo "==> all checks passed"
