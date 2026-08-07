#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.local/bin:$PATH"

REVIEW_FILE="/tmp/claude-review-${PR_NUMBER}.json"

PROMPT_FILE=$(mktemp /tmp/claude-review-prompt.XXXXXX.txt)
trap 'rm -f "$PROMPT_FILE"' EXIT

cat > "$PROMPT_FILE" <<PROMPT_EOF
You are reviewing PR #${PR_NUMBER} in the sandbox-load-test repository.

Steps:
1. Read ALL CLAUDE.md/AGENTS.md files to understand project conventions.
2. Run: git diff origin/${BASE_REF}...HEAD --name-only  (get changed files)
3. Run: git diff origin/${BASE_REF}...HEAD  (get the full diff with line numbers)
4. Read the changed files to understand context.
5. Write a thorough code review in Korean.

6. Write the review to ${REVIEW_FILE} in this exact JSON format.
   The 'body' field must use this exact markdown structure:

{
  "body": "## 🤖 eottabom-claude-review\n\n### 📊 종합 평가\n\n| 항목 | 평가 |\n|------|------|\n| ✅ Correctness | <가능 / 불가 + 한 줄 이유> |\n| 🔁 Regression Risk | <🔴 매우 높음 / 🟡 보통 / 🟢 낮음 + 한 줄 이유> |\n| ⚠️ Risky Changes | <있음: 내용 요약 / 없음> |\n| 🧪 Missing Tests | <있음: 내용 요약 / 없음> |\n\n### 📝 상세 리뷰\n\n<상세 리뷰 내용을 마크다운으로 작성. 파일명, 라인, 근거를 구체적으로>",
  "event": "COMMENT",
  "comments": [
    {
      "path": "relative/path/to/file.java",
      "line": <line number in the new version of the file>,
      "side": "RIGHT",
      "body": "<specific inline comment in Korean>"
    }
  ]
}

7. Post the review:
   gh api /repos/eottabom/sandbox-load-test/pulls/${PR_NUMBER}/reviews --method POST --input ${REVIEW_FILE}

Rules:
- Only add inline comments on lines that exist in the diff (added or changed lines on the RIGHT side).
- The 'line' must be the actual line number in the current file, not the diff position.
- If there are no specific inline findings, use an empty array for 'comments'.
- If there are no findings at all, still post a summary saying so.
- Write everything in Korean.
PROMPT_EOF

claude --dangerously-skip-permissions --verbose --output-format stream-json < "$PROMPT_FILE"
