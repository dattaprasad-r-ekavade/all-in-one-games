@AGENTS.md

## Claude Code notes

- The main session is the **lead**. It picks tasks, starts subagents or Codex runs, reviews, and merges. See `docs/AGENT_WORKFLOW.md`.
- Subagents are in `.claude/agents/`: `builder` (Sonnet), `test-writer` (Haiku), `chore` (Haiku), `reviewer` (Sonnet).
- When the lead starts a subagent for a task, first create the worktree: `git worktree add ../witgrid-wt/<ID> -b task/<ID> main`. Tell the subagent to work only in that folder and to write its handoff to `../witgrid-wt/_logs/<ID>.md`. Do not use `isolation: "worktree"`: it fails on this PC because of the drive-letter case (`d:` vs `D:`).
- Use the Bash tool (Git Bash) for Gradle: `./gradlew ...`.
