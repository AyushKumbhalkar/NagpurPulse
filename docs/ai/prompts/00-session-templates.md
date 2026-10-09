# Session templates (same for all 5 accounts)

Order of messages in a new chat: **(1) AI_CONTEXT.md → (2) task prompt → (3) file pack**.
You can send 1+2+3 in a single message to save budget.

## START prompt (put above AI_CONTEXT.md)
```
You are a senior Android/Supabase engineer on the NagpurPulse project. The text below is the shared project
context; treat it as the source of truth and follow its conventions. Work only on the task I give next.
Rules: analyze the supplied files first, then output ONE git-apply-ready unified diff. After the patch give
max 5 bullets of risks. No long explanations. If a file you need is missing, ask for it ONCE before coding.
Budget: I have limited usage, so keep answers tight and do not repeat my code back.
--- CONTEXT START ---
<paste AI_CONTEXT.md here>
--- CONTEXT END ---
```

## END prompt (send when ~70% of your budget is used, or when the task is done)
```
Stop coding. Give me exactly this, nothing else:
1) Status of task <ID>: DONE / PARTIAL / BLOCKED (one line).
2) A HANDOFF entry in this format:
### [<ID>] · Account <N> · <date>
Changed: files…
Patch: docs/ai/patches/<ID>.patch
Decisions: …
Risks / follow-ups: …
Next task: <ID>
3) Up to 5 lines to append to AI_CONTEXT.md (only durable facts or decisions).
4) If PARTIAL: the exact remaining steps as a checklist so a fresh chat can finish them.
```

## CONTINUE prompt (new chat finishing a PARTIAL task)
```
Continue task <ID>. Previous chat status: PARTIAL. Remaining steps: <paste checklist>. Already applied patch:
<paste or attach>. Do NOT redo finished work. Output one patch for the remaining steps only.
```

## BUILD-FIX prompt (cheapest way to fix compile errors)
```
The patch compiled with these errors. Fix only these, output a minimal diff against the current state:
<paste Gradle errors, max 40 lines>
Relevant current file(s): <paste only the failing file(s)>
```

## REVIEW prompt (use in Account 5, or any account to double-check another)
```
Review this patch as a skeptical senior engineer. Check: compile errors, wrong imports/APIs for the
versions in the context, behavior regressions, security (RLS/secrets/logging), Play policy impact,
performance (recomposition, N+1 queries), accessibility. Output: BLOCKERS / SHOULD-FIX / NITS, then a
corrected diff for BLOCKERS only.
<paste patch>
```

## SUMMARIZE prompt (when a chat gets long)
```
Summarize this chat in ≤ 20 lines so I can start a fresh chat: goal, what is applied, what is pending,
decisions, open errors. Then stop.
```

## CONTEXT-COMPRESS prompt (Account 5, every ~10 tasks)
```
Here is AI_CONTEXT.md and the last 10 HANDOFF_LOG entries. Produce a new AI_CONTEXT.md under 150 lines:
merge durable decisions, move finished work into one "Done" line each, delete stale findings.
Keep conventions unchanged. Output the full file.
```

## Patch hygiene
- Save each patch as `docs/ai/patches/<ID>.patch` and commit it with the code.
- Always run `git apply --check <patch>` before `git apply`.
- If a patch fails to apply, send Claude: the error + the current version of the failing file, and use BUILD-FIX.
