# AGENTS.md

This repository keeps its project-specific and KMP-related skills under `.codex/skills/`. Prefer these project-local skills over any similarly named global skills when working in this repo.

## Skill Priority

Use these skills when their scope matches the task:

- `$novel-ignite-product-rules`
  - Use when changing user-visible product behavior in `Workshop`, `Templates`, `Board`, local-first persistence, or Ollama/OpenRouter routing.
- `$kmp-architecture`
  - Use for shared KMP structure, source-set boundaries, expect/actual, and general shared-module design.
- `$kmp-architect`
  - Use for broader KMP architecture setup and feature/module shaping.
- `$kmp-navigation`
  - Use when navigation strategy or multiplatform navigation behavior changes.
- `$decompose`
  - Use only when the task explicitly uses or introduces Decompose patterns.

## Repo Rules

- Treat `Workshop` as a hybrid document-plus-chat writing flow.
- Treat selected templates as story modes, not just prompt snippets.
- Keep local development on Ollama and cloud traffic behind the relay.
- Favor graceful persistence and restore behavior for writer data.

## Source Control

- Keep `.codex/skills/` tracked in git for this repository.
- If a skill changes, update its `agents/openai.yaml` to stay aligned with `SKILL.md`.
