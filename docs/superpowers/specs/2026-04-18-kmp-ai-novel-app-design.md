# KMP AI Novel App Design

Date: 2026-04-18
Status: Draft approved in conversation, written for implementation planning

## 1. Goal

Build a Kotlin Multiplatform AI novel-writing app for Android and Desktop.

The product should help solo writers:

- create and manage long-lived writing projects
- use AI for drafting, scene exploration, and rewrite assistance
- build reusable novel templates
- publish templates to a public board
- copy and remix templates shared by other users

The MVP should prioritize fast iteration, reliable local writing workflows, and a clean path to later Android on-device LLM support.

## 2. Product Direction

### Primary persona

The primary user is a solo writer who wants to manage their own worldbuilding, characters, and long-running writing sessions, while using AI as a drafting partner.

### Platform priority

The initial target platforms are:

- Android
- Desktop

This supports short-form mobile writing and deeper long-form writing/editing on desktop.

### Writing experience

The writing experience is hybrid:

- document-first writing for the actual manuscript
- chat-style interaction for scene experiments, idea exploration, and rewrite assistance

### Auth model

Authentication is optional.

- local writing and personal template use must work without login
- login is required for template publishing and cloud-connected features

### Template philosophy

Templates are hybrid assets:

- structured fields for reusable writing context
- freeform prompt blocks for creative flexibility

## 3. Architecture Overview

The app should use a single Kotlin Multiplatform codebase with Compose Multiplatform UI.

### High-level layers

1. Client app
   Android + Desktop app built with KMP and Compose Multiplatform
2. Shared domain layer
   Writing workflows, template logic, prompt construction, persistence interfaces, generation use cases
3. Storage layer
   Local persistence with SQLDelight backed by SQLite
4. Cloud backend layer
   BaaS for auth, public template metadata, and public board features
5. AI relay layer
   Separate lightweight API that proxies production requests to OpenRouter

### Key architectural rules

- writing workflows must not depend directly on a specific model vendor
- local-first operation is the default for draft work
- public sharing and account features are additive, not foundational
- Android on-device inference must be possible later without refactoring the core writing flows

## 4. Client Information Architecture

The MVP should use four top-level areas:

- Workshop
- Templates
- Board
- Library

### Workshop

The main writing space.

Includes:

- manuscript editor
- side or bottom AI panel
- project-scoped template attachment
- scene-level AI actions such as continue, rewrite, dialogue polish, next-beat suggestion

### Templates

Personal template management.

Includes:

- create template
- edit structured fields
- edit freeform prompt blocks
- save private templates
- attach templates to projects

### Board

Public template catalog, not a full social network.

Includes:

- browse public templates
- view template details
- like template
- copy/remix template into private collection
- publish personal template

### Library

A home for user-owned assets.

Includes:

- recent projects
- recent sessions
- saved generations
- favorite templates

## 5. Core Data Model

Local storage should use SQLDelight with SQLite.

### Core entities

- `Project`
  Represents a novel or writing project
- `DraftSession`
  Represents a writing session tied to a project
- `Template`
  Private reusable writing template
- `TemplateVersion`
  Immutable snapshot of a template used for publishing or generation traceability
- `TemplatePost`
  Public metadata for a published template
- `GenerationRun`
  Tracks generation requests and outputs for debugging and reproducibility
- `UserProfile`
  User account metadata for optional login features

### Recommended storage boundaries

Stored locally by default:

- manuscript text
- local draft sessions
- unsynced template edits
- recent generations
- project metadata

Stored remotely:

- user account profile
- public template posts
- public template metadata
- likes
- remix/copy metrics

Deferred from MVP:

- full manuscript cloud sync
- collaborative editing
- real-time shared state

## 6. AI Integration Architecture

The AI integration layer must be provider-agnostic.

### Shared contract

Define a common interface such as `InferenceEngine`.

Recommended operations:

- `generate`
- `streamGenerate`
- `abort`
- `listModels`
- `capabilities`

The UI and use cases should operate only against this contract.

### Request model

Use a normalized request model such as `GenerationRequest` containing:

- project context
- selected template data
- freeform prompt blocks
- recent manuscript excerpt
- action type
- model parameters

### Response model

Use a normalized event/result model such as:

- `StreamingToken`
- `StructuredSuggestion`
- `FinalText`
- `GenerationError`

### Environment-specific engines

#### `LocalOllamaEngine`

Used for local development and local runtime mode.

- primary development baseline
- available for Desktop and Android app builds
- works without login

#### `RemoteOpenRouterEngine`

Used for production cloud inference.

- app must not call OpenRouter directly
- requests go through a lightweight relay API
- relay handles secrets, allowlist, usage policy, and shared wrapping rules

#### `AndroidOnDeviceEngine`

Not part of MVP implementation, but part of MVP architecture.

- define the interface slot now
- implement later in `androidMain`
- keep shared domain logic unchanged when added later

## 7. Environment Behavior

### Local mode

- default development path
- uses Ollama
- supports writing without login
- stores all writing data locally

### Cloud mode

- login-gated
- uses relay API backed by OpenRouter
- enables publish-to-board behavior
- keeps the same user-facing writing flow as local mode

### Error handling expectations

All inference failures should map into a unified error model.

Examples:

- Ollama connection unavailable
- model not installed locally
- relay unavailable
- auth failure
- quota or usage limit hit

The UI should show environment-appropriate guidance while preserving a shared error contract.

## 8. Template Model

Templates should combine structured and flexible input.

### Structured fields

Recommended MVP fields:

- title
- genre
- premise
- world setting
- character cards
- relationship notes
- tone/style
- banned elements
- plot constraints
- opening hook

### Flexible fields

- custom prompt blocks
- optional scene instructions
- optional writing rules

### Publish model

Publishing should create a stable snapshot.

- private editable template stays editable
- published version is stored as `TemplateVersion`
- board references the published snapshot, not the mutable private draft

## 9. MVP Scope

### In scope

- KMP Android + Desktop app
- document + chat hybrid writing workflow
- local persistence with SQLDelight + SQLite
- local inference via Ollama
- production inference via relay API + OpenRouter
- optional login
- private template creation/editing
- public template board with browse, detail, publish, like, and remix/copy

### Out of scope

- comments
- follows
- direct messaging
- collaborative writing
- full manuscript cloud sync
- billing
- advanced recommendation feed
- Android on-device LLM implementation

## 10. Success Criteria

The MVP is successful if:

1. A user can create a writing project and draft with Ollama without logging in.
2. Draft state and recent generations persist locally across app restarts.
3. A logged-in user can publish a template to the public board.
4. Another user can browse a public template and remix it into a private template.
5. The same writing use case works through both local and cloud inference modes.
6. The architecture cleanly supports a future Android on-device engine without redesigning shared writing flows.

## 11. Implementation Guidance

Implementation should favor mature, stable, widely adopted libraries whenever they fit the requirements well.

### Library selection principle

If there is a stable, well-supported, widely used library that solves a feature cleanly, prefer using it over building custom infrastructure.

Apply this principle especially to:

- navigation
- DI
- persistence
- auth integration
- HTTP clients
- serialization
- image loading
- markdown or rich text support
- logging
- testing

Custom implementation should be reserved for:

- product-specific writing workflows
- template composition logic
- generation orchestration
- domain rules that are central to product differentiation

### Stability preference

When choosing between alternatives, prefer:

- proven production usage
- active maintenance
- strong documentation
- multiplatform support
- predictable upgrade path

Avoid introducing niche or experimental dependencies for MVP-critical paths unless there is a clear technical reason.

## 12. Recommended Technical Bias

The implementation plan should strongly consider proven KMP-friendly libraries where appropriate.

Examples of the kind of stack to evaluate first:

- Compose Multiplatform
- SQLDelight
- Ktor client/server where applicable
- kotlinx.serialization
- a stable DI solution already common in KMP ecosystems
- a stable BaaS SDK or thin HTTP integration if SDK quality is weak

These are examples, not final lock-ins. Final library choices should follow the stability principle above.

## 13. Open Decisions For Planning Phase

These should be resolved in implementation planning, not in this design:

- exact BaaS choice
- exact relay API stack
- exact DI library
- exact navigation library
- exact editor strategy for long-form manuscript editing
- exact board ranking and discovery heuristics

## 14. Planning Recommendation

Implementation should proceed in phases:

1. Core writing shell and local persistence
2. Ollama integration and hybrid writing flow
3. Template authoring
4. Board browsing and publishing
5. Cloud inference relay and optional auth
6. Architecture slot preparation for Android on-device engine

This keeps the first deliverable focused on writer value while preserving the future path toward on-device Android inference.
