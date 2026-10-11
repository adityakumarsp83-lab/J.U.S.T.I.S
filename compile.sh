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
echo "╚══════════════════════════════════════════════════╝"
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

# Find all .java files
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
if [ "$1" = "test" ]; then
    echo ""
    echo "🧪 Running Phase 2 Smoke Test..."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    java -cp "$OUT_DIR" com.justis.Phase2SmokeTest
elif [ "$1" = "run" ] || [ -z "$1" ]; then
    echo ""
    echo "🚀 Running J.U.S.T.I.S...."
    echo "─────────────────────────────────────────────────"
    cd "$PROJECT_DIR"
    java -cp "$OUT_DIR" com.justis.Main 2>/dev/null || echo "   ℹ️  Main.java not yet implemented. Use './compile.sh test' to run tests."
fi

echo ""
