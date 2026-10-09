# NagpurPulse AI Workflow — How 5 Claude accounts work as one team

**Problem:** each Claude account has its own usage limit and none of them share memory.
**Solution:** the repo is the shared memory. Every chat starts by pasting the same short
briefing (`AI_CONTEXT.md`), works on ONE task from `TASK_BOARD.md`, outputs ONE patch, and ends by
producing a 5-line handoff that you paste into `HANDOFF_LOG.md` / `AI_CONTEXT.md`.

## Folder map
| File | Purpose | You paste it into Claude? |
|---|---|---|
| `AI_CONTEXT.md` | The shared brain (<150 lines). Architecture, rules, done/in-progress | **Always, first message** |
| `TASK_BOARD.md` | Every task with ID, account, files to attach, status | No — you read it |
| `HANDOFF_LOG.md` | Append-only log of what each chat changed | Only last 5 entries |
| `prompts/00-session-templates.md` | Start / end / review / recovery prompts | Copy from it |
| `prompts/account-N-*.md` | Ready-to-paste task prompts for each account | Copy from it |
| `../../tools/ai-pack.sh` | Bundles the files for a task into one paste-able text | You run it |

## The 5 accounts
| # | Role | Connectors / tools | Typical chats |
|---|---|---|---|
| 1 | Backend, DB, Security (Supabase) | **Supabase connector (staging project)** | ~16 |
| 2 | UI / UX, screen by screen | none needed | ~24 |
| 3 | Play Store & compliance | web search on | ~14 |
| 4 | Performance, scale, reliability | none needed | ~13 |
| 5 | QA, integration, release | none needed | ~12 |

## One work session (same for every account)
1. `git checkout -b ai/<TASK-ID>` from the `ai/integration` branch.
2. Run `tools/ai-pack.sh <TASK-ID> <files...>` → paste `AI_CONTEXT.md` + the task prompt + the pack.
3. Claude returns **one patch**. Save it as `docs/ai/patches/<TASK-ID>.patch`, then `git apply --check` and `git apply`.
4. Build + run the app. If broken, paste the build error back into the SAME chat (cheapest fix).
5. Last message of the chat: send the **END prompt**. Paste its answer into `HANDOFF_LOG.md`, update the
   status in `TASK_BOARD.md`, commit, merge into `ai/integration`.
6. Account 5 reviews `ai/integration` before anything reaches `main`.

## Hard rules
- Never paste secrets: no `SUPABASE_SERVICE_ROLE_KEY`, keystore, passwords, FCM server keys, `.env`. The
  anon key and `google-services.json` are public-client values, but still prefer not to paste them.
- Connect the Supabase connector to a **staging** project, never production. Production changes go through
  migration files in git, reviewed by you.
- One task per chat. If a task will not fit, split it before you start.
- A chat that hits its limit is fine **only if** `HANDOFF_LOG.md` is current. Ask for the handoff early
  (around 70% of your budget), not at the very end.
- Never let two accounts edit the same file in parallel. The task board's "Files" column prevents this.
