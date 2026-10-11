#!/bin/bash
# ═══════════════════════════════════════════════════
#  J.U.S.T.I.S. — Build & Run Script
#  Judicial Unified Search, Triaging & Intelligent Scheduling
# ═══════════════════════════════════════════════════

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║     J.U.S.T.I.S. — Build System                 ║"
╚══════════════════════════════════════════════════╝"
echo ""

# ─── Clean ─────────────────────────────────────────
if [ "$1" = "clean" ]; then
    echo "🧹 Cleaning build output..."
    rm -rf "$OUT_DIR"
    echo "   Done."
    exit 0
fi

# ─── Compile ───────────────────────────────────────
echo "🔨 Compiling Java sources..."
mkdir -p "$OUT_DIR"

JAVA_FILES=$(find "$SRC_DIR" -name "*.java")
FILE_COUNT=$(echo "$JAVA_FILES" | wc -l | tr -d ' ')
echo "   Found $FILE_COUNT source files."

javac -d "$OUT_DIR" $JAVA_FILES

if [ $? -eq 0 ]; then
    echo "   ✅ Compilation successful!"
else
    echo "   ❌ Compilation failed!"
    exit 1
fi

# ─── Run ───────────────────────────────────────────
if [ "$1" = "web" ]; then
    echo ""
    echo "🌐 Launching J.U.S.T.I.S. Web Application at http://localhost:8080..."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    (sleep 1 && open "http://localhost:8080" 2>/dev/null || true) &
    java -cp "$OUT_DIR" com.justis.web.WebServer
elif [ "$1" = "test" ]; then
    echo ""
    echo "🧪 Running Comprehensive Test Suite..."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    java -cp "$OUT_DIR" com.justis.ComprehensiveTestSuite
elif [ "$1" = "test-phase2" ]; then
    echo ""
    echo "🧪 Running Phase 2 Smoke Test..."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    java -cp "$OUT_DIR" com.justis.Phase2SmokeTest
elif [ "$1" = "demo" ]; then
    echo ""
    echo "🚀 Running J.U.S.T.I.S. Automated Verification Demo..."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    java -cp "$OUT_DIR" com.justis.Main --demo
elif [ "$1" = "run" ] || [ -z "$1" ]; then
    echo ""
    echo "🚀 Launching J.U.S.T.I.S. Interactive Terminal..."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    java -cp "$OUT_DIR" com.justis.Main
fi

echo ""
