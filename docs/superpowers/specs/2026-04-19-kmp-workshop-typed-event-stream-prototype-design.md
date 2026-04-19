# KMP Workshop Typed Event Stream Prototype Design

Date: 2026-04-19
Status: Draft approved in conversation, written for implementation planning

## 1. Goal

Define a prototype for `Workshop` streaming chat that keeps the richer localchat-gemma-style UI while replacing the current text-only assistant streaming contract with a strict typed event stream inside the KMP app.

This prototype is for manual validation first. It should prove that the app can render progressively updated markdown responses plus follow-up choices from a typed event pipeline without breaking the existing `Workshop` writing flow.

## 2. Relationship To Existing Specs

This document narrows the implementation direction for:

- `docs/superpowers/specs/2026-04-18-kmp-ai-novel-app-design.md`
- `docs/superpowers/specs/2026-04-19-kmp-workshop-streaming-chat-ux-design.md`

Those documents define product scope and the user-facing streaming UX contract. This document defines the prototype architecture for a stricter assistant stream model that still honors those rules.

## 3. Product Constraints

The prototype must preserve these repo-specific product rules:

- `Workshop` remains a hybrid document-plus-chat drafting flow.
- The assistant should feel like a drafting partner, not a detached tool.
- Only one assistant generation may be active at a time.
- Writer state must remain local-first and resilient.
- Failed or aborted in-progress assistant turns must not become durable history.
- The same high-level contract should remain compatible with both local Ollama development and later relay-backed cloud inference.

## 4. Prototype Scope

The prototype includes:

- localchat-gemma-style assistant presentation in `Workshop`
- markdown-style assistant body rendering
- visible follow-up choices rendered from typed state
- a strict typed event stream contract inside shared KMP code
- a reducer or equivalent state application layer that consumes only typed stream events
- a fixture-based structured stream for manual verification
- an adapter path that maps the current Ollama text stream into the same typed event contract

The prototype does not include:

- a final production transport protocol for cloud inference
- a guarantee that Ollama itself emits schema-valid structured events
- new top-level product areas outside `Workshop`
- full design polish beyond what is needed for manual validation
- persistence of partial assistant turns

## 5. Architecture Decision

The prototype adopts a fully typed event stream inside the app boundary.

The transport itself may still begin as a text stream in some environments. The important boundary is:

- transport layer may be flexible
- `WorkshopViewModel` and UI state application layer must consume strict typed events only

This means the app should no longer treat assistant output as raw string chunks after the adapter boundary. Instead, it should work with a small `@Serializable` event contract that describes assistant turn lifecycle and renderable updates.

This approach is preferred over:

- text-only streaming with app-generated fallback choices
- text streaming with one final typed payload only

because the prototype goal is specifically to validate whether KMP shared code can support a strict event-stream contract for progressive markdown plus choice updates.

## 6. Event Model

The prototype should use a compact event family for assistant streaming.

Recommended shape:

- `AssistantStreamEvent.Start`
  Starts a new assistant turn and fixes identifiers such as `requestId` and `messageId`.
- `AssistantStreamEvent.MarkdownDelta`
  Appends markdown text to the active in-progress assistant turn.
- `AssistantStreamEvent.ChoicesReplace`
  Replaces the current follow-up choice set for the active in-progress assistant turn.
- `AssistantStreamEvent.MetadataPatch`
  Applies small display metadata updates such as labels, badges, or surface hints.
- `AssistantStreamEvent.Complete`
  Marks the assistant turn as stable and complete.
- `AssistantStreamEvent.AbortAck`
  Acknowledges cancellation for the active assistant turn.
- `AssistantStreamEvent.Error`
  Marks the active stream as failed and provides a user-facing failure message.

### Event rules

- `Start` must occur before any other event for a given active turn.
- `MarkdownDelta`, `ChoicesReplace`, and `MetadataPatch` target only the latest active assistant turn.
- `ChoicesReplace` replaces the full visible choice set. It is not a diff.
- `Complete`, `AbortAck`, and `Error` end the active generation.
- Once a turn is completed, aborted, or failed, it must not accept further content updates.
- At most one assistant turn may be active at a time.

## 7. Message State Model

The UI should render assistant state from a stable view model shape rather than directly from transport details.

Recommended assistant render state:

- `renderedMarkdown: String`
- `choices: List<WorkshopChoice>`
- `metadata: WorkshopAssistantMetadata`
- `phase: streaming | completed | failed`
- `isStreamingCaretVisible: Boolean`
- `failureMessage: String?`

Recommended choice shape:

- `id: String`
- `label: String`
- `prompt: String`
- `style: primary | secondary`

### State invariants

- completed turns are immutable from the user perspective
- failed turns are not promoted to durable assistant history
- aborted in-progress turns are removed from durable conversation history
- choice surfaces belong only to assistant turns
- the latest in-progress assistant turn is the only target for progressive updates

## 8. Adapter Strategy

The prototype should validate two input paths that feed the same typed event contract.

### A. Fixture structured stream

A fake or fixture engine emits ideal typed events directly. This path is the reference implementation for tests and manual verification.

Its role is to prove:

- reducer correctness
- UI correctness
- recovery behavior
- choice replacement behavior
- persistence safety

### B. Ollama text-stream adapter

The current local Ollama integration continues to receive text chunks, but those chunks are mapped into typed app-internal events.

For the prototype, the adapter may be conservative:

- emit `Start`
- convert incoming text chunks into `MarkdownDelta`
- synthesize `ChoicesReplace` once enough content exists or at completion
- emit `Complete` or `Error`

The adapter does not need to prove that Ollama can natively produce strict structured events. It only needs to prove that the app can standardize a weaker upstream source into the same internal contract.

## 9. UI Contract

The visual goal is to keep the richer localchat-gemma-style interaction model while staying inside current `Workshop` product boundaries.

The prototype UI should provide:

- assistant markdown rendering instead of plain text-only bubbles
- a visibly streaming latest assistant turn
- follow-up choice buttons attached to the latest assistant answer
- disabled or inactive choices for non-latest assistant turns if needed
- clear idle, streaming, recovering, and failure states

The UI does not need to expose transport implementation details.

The user should experience:

- one assistant answer that grows over time
- optional choice updates while streaming
- a stable final answer with actionable next-turn choices

## 10. Persistence Rules

Prototype persistence must follow the existing streaming UX safety contract.

Required rules:

- user turns are persisted when submitted
- completed assistant turns are persisted after completion
- in-progress assistant turns are not restored as completed history after restart
- aborted or failed assistant turns are not restored as completed history
- choice data is persisted only for completed assistant turns

For MVP safety, only completed assistant turns are durable.

## 11. Manual Validation Scenarios

The prototype is successful only if manual validation can confirm all of the following:

1. Normal completion
   The assistant markdown grows progressively, completes successfully, and ends with visible follow-up choices.

2. Mid-stream choice replacement
   A choice set appears or updates during streaming, and the latest replacement is the one that remains visible.

3. Abort
   The user aborts streaming, the unfinished assistant turn does not remain in durable history, and the rest of the conversation and manuscript stay intact.

4. Malformed or invalid event handling
   A bad event causes safe failure, cleanup, and return to an idle retryable state rather than a broken chat surface.

5. Restore safety
   After restart or restoration, completed turns remain, but interrupted partial assistant turns do not reappear as stable completed responses.

## 12. Testing Expectations

Implementation planning should include tests for at least:

- reducer application of each event type
- prevention of invalid event ordering
- single-active-stream enforcement
- choice replacement semantics
- abort cleanup
- error cleanup
- persistence snapshot behavior for completed versus in-progress turns
- desktop UI rendering for markdown, streaming, and choices

Shared behavior should be tested in `commonTest` where possible. Manual visual confirmation should be centered on Desktop.

## 13. Recommended Prototype Boundaries In Code

The implementation should keep shared logic in `commonMain` where possible:

- typed stream DTOs
- state reducer or event application logic
- `WorkshopViewModel` stream handling
- persistence rules for typed assistant turns

Desktop-focused work is acceptable for:

- manual validation helpers
- fixture launch modes if needed
- visual smoke verification

This keeps the prototype aligned with KMP goals while still making manual verification practical.

## 14. Success Decision

This prototype should be considered successful if:

- the app can run `Workshop` with the richer chat UI
- the app internally consumes only typed assistant stream events after the adapter boundary
- markdown, choice rendering, abort, error cleanup, and restart safety can all be demonstrated manually
- the resulting architecture is clean enough to support later provider-specific adapters without rewriting the `Workshop` UI contract

If the prototype succeeds, the next implementation plan should treat the typed event stream as the primary `Workshop` assistant contract.
