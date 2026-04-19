# Project Skills

All KMP-related skills for this repository live under `.codex/skills/` in the project root so they travel with the repo.

## Installed Project-Local Skills

### Upstream Skills

- `kmp-architecture`
  - Source: `pddhkt/kotlin-kmp-skills`
  - Installed from: `plugins/kotlin-kmp/skills/kmp`
- `kmp-navigation`
  - Source: `ahmed3elshaer/everything-claude-code-mobile`
  - Installed from: `skills/kmp-navigation`
- `kmp-architect`
  - Source: `andvl1/claude-plugin`
  - Installed from: `skills/kmp`
- `decompose`
  - Source: `andvl1/claude-plugin`
  - Installed from: `skills/decompose`

### Repo-Local Skill

- `novel-ignite-product-rules`
  - Purpose: capture Novel Ignite business rules that generic KMP skills do not know

## Not Installed

- `kotlin-multiplatform-reviewer`
  - Intended source: `physics91/claude-vibe`
  - Status: not installed because the upstream GitHub repo was not resolvable during installation

## Pairing Guidance

- Use `kmp-architecture` or `kmp-architect` for generic implementation technique.
- Use `kmp-navigation` only when navigation behavior is in scope.
- Use `decompose` only if the task explicitly touches Decompose.
- Use `novel-ignite-product-rules` whenever a change affects user-visible Novel Ignite behavior.
