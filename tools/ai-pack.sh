#!/usr/bin/env bash
# Bundle files into one paste-able text file for a Claude chat.
# Usage: tools/ai-pack.sh UI-05 app/src/main/java/.../HomeScreen.kt app/src/main/java/.../PostCard.kt
# Output: ai-pack-<TASK>.txt in the repo root (git-ignored suggestion: add 'ai-pack-*.txt' to .gitignore)
set -euo pipefail
TASK="${1:?task id, e.g. UI-05}"; shift
[ "$#" -ge 1 ] || { echo "give at least one file"; exit 1; }
OUT="ai-pack-${TASK}.txt"
: > "$OUT"
for f in "$@"; do
  [ -f "$f" ] || { echo "missing: $f" >&2; exit 1; }
  { echo; echo "===== FILE: $f ($(wc -l < "$f") lines) ====="; cat "$f"; } >> "$OUT"
done
chars=$(wc -c < "$OUT")
echo "Wrote $OUT — $chars chars ≈ $((chars / 4)) tokens (rough estimate)."
if [ "$chars" -gt 60000 ]; then echo "WARNING: pack is large (>~15k tokens). Send fewer files or only the relevant functions."; fi
