---
name: novel-ignite-product-rules
description: Use when changing product behavior in this repository's novel-writing app, especially around Workshop, Templates, Board, local-first persistence, and Ollama/OpenRouter routing. This skill captures repo-specific rules that generic KMP or Compose skills do not know.
---

# Novel Ignite Product Rules

## Overview

This skill defines the repo-specific product rules for Novel Ignite. Use it with generic KMP/Compose skills when a task changes what the app does for writers rather than only how the code is organized.

## Use This For

- `Workshop` writing flow, chat flow, and template-driven prompting
- `Templates` authoring, enrichment, save/delete behavior, and remix handling
- `Board` publish/remix behavior
- local persistence and restore behavior that affects writing data
- inference routing between local Ollama and relay-backed OpenRouter

Do not use this skill for generic KMP setup, Gradle conventions, or navigation-library choices unless those decisions change the product rules below.

## Product Invariants

- The core product surfaces are `Workshop`, `Templates`, `Board`, and `Library`.
- `Workshop` remains a hybrid document-plus-chat drafting flow.
- Selected templates are story operating modes, not just prompt snippets.
- Template choice should materially affect assistant behavior and scene guidance.
- `Templates` keep both structured fields and freeform prompt blocks editable.
- Enrichment may start from partial user seed input, but users keep final edit control before saving.
- `Board` references published template snapshots, not mutable private drafts.
- Local development defaults to Ollama.
- Cloud behavior must go through the relay; the app must not call OpenRouter directly.
- Writing data is local-first, so persistence changes should prefer graceful recovery over brittle failure.

## Feature Rules

### Workshop
- Keep document editing and AI interaction in one drafting workflow.
- If a template is selected, generation should use full template detail when available, not only prompt blocks.
- Missing optional template detail should degrade gracefully instead of blocking generation.
- `Workshop` assistant output should help continue the story, not read like a detached tool.

### Templates
- Structured fields include title, genre, premise, world setting, character cards, relationship notes, tone/style, banned elements, plot constraints, opening hook, and prompt blocks.
- Deleting an active template should clear the active workshop selection.
- Remix loads an editable private draft instead of mutating the published source snapshot.

### Board
- Treat `Board` as a public template catalog, not a full social network.
- Publish/remix flows must preserve the distinction between editable private templates and immutable published versions.

## Check Before Shipping

- Read [README.md](D:/kmp-llm-note/README.md) for environment and verification commands.
- Read [2026-04-18-kmp-ai-novel-app-design.md](D:/kmp-llm-note/docs/superpowers/specs/2026-04-18-kmp-ai-novel-app-design.md) for product scope and provider routing.
- Read [2026-04-19-workshop-template-story-prompt-design.md](D:/kmp-llm-note/docs/superpowers/specs/2026-04-19-workshop-template-story-prompt-design.md) when touching Workshop/template behavior.
- Re-run the narrowest relevant tests first, then `./gradlew.bat :composeApp:jvmTest` for shared/Desktop regression coverage.

When a generic KMP skill and this skill conflict, prefer this skill for product behavior and the generic skill for implementation technique.
