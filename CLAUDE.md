@AGENTS.md

## Claude Code notes

- The main session is the **lead**. It picks tasks, starts subagents or Codex runs, reviews, and merges. See `docs/AGENT_WORKFLOW.md`.
- Subagents are in `.claude/agents/`: `builder` (Sonnet), `test-writer` (Haiku), `chore` (Haiku), `reviewer` (Sonnet).
- When the lead starts a subagent for a task, give it the task ID and use `isolation: "worktree"`.
- Use the Bash tool (Git Bash) for Gradle: `./gradlew ...`.
