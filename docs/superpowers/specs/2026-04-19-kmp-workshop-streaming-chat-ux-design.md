# KMP Workshop Streaming Chat UX Design

Date: 2026-04-19
Status: Draft approved in conversation, written as a focused UX contract for planning

## 1. Purpose

This document defines the core streaming chat UX for the `Workshop` area of the KMP AI novel-writing app.

It exists as a companion to the broader product design document:

- `docs/superpowers/specs/2026-04-18-kmp-ai-novel-app-design.md`

The purpose of this document is to make the intended streaming assistant experience explicit before implementation planning.

The app already defines a provider-agnostic inference layer and a hybrid document-plus-chat writing flow. This document narrows that broad direction into a concrete user experience contract for streaming chat behavior inside `Workshop`.

## 2. Scope

This document includes:

- the user-visible streaming chat flow in `Workshop`
- message lifecycle rules for user and assistant turns
- minimal user-facing state transitions
- abort behavior
- error and recovery behavior
- persistence and restoration rules for streaming chat state
- testable success criteria for the streaming experience

This document does not include:

- Compose or platform API choices
- ViewModel, Flow, coroutine, or DI implementation details
- concrete transport protocol choices
- visual styling details such as animation polish or exact placeholder presentation
- reuse rules for `Templates`, `Board`, or `Library`

Implementation strategy belongs in the planning phase, not in this document.

## 3. Product Intent

The `Workshop` assistant must feel like an active drafting partner, not a batch job that returns a full answer at the end.

The core experience is:

- the user sends a prompt or scene action
- the user turn appears immediately
- a new assistant turn begins in a pending state
- the latest assistant turn grows progressively while generation is in progress
- the user can interrupt the current generation
- completion produces a stable assistant turn that remains in conversation history
- failure does not destroy the surrounding session or manuscript context

The user should always be able to tell whether the assistant is idle, currently generating, or recovering from a failed generation.

## 4. UX Principles

The streaming chat UX in `Workshop` should follow these principles:

- streaming is the default experience for interactive assistant output
- the user sees progress on the current assistant turn instead of waiting for a fully buffered response
- only one active assistant generation may exist at a time
- the newest assistant turn is the only turn that may be progressively updated
- interruption must be safe and predictable
- failures must preserve the rest of the writing session
- only stable, completed assistant turns become long-lived conversation history

These principles intentionally prioritize clarity and resilience over visual flourish.

## 5. User Experience Flow

The expected flow for a normal streaming response is:

1. The user sends freeform chat input or triggers a scene action such as continue, rewrite, or next-beat suggestion.
2. The new user turn is added to the visible conversation immediately.
3. The assistant enters a generation phase and creates a new in-progress assistant turn.
4. The in-progress assistant turn updates progressively as text arrives.
5. The conversation view keeps the latest assistant turn visible as it grows.
6. When generation completes, the in-progress assistant turn becomes a stable completed turn.
7. The app returns to a ready state for the next interaction.

The expected flow for interruption is:

1. The user stops the active generation while the assistant turn is still in progress.
2. The current in-progress assistant turn is discarded rather than preserved as final conversation history.
3. The conversation returns to a ready state without losing the previous completed turns or the latest user turn.

The expected flow for failure is:

1. A generation fails due to connection, provider, parsing, or other inference problems.
2. The app enters a brief recovery phase.
3. The failed in-progress assistant turn is cleaned up.
4. The user can see that generation failed.
5. The rest of the writing session remains intact and the app returns to a ready state for retry.

## 6. User-Facing State Model

The streaming chat experience should expose three minimal user-facing states:

- `idle`
  The assistant is ready for a new request. No generation is active.
- `streaming`
  The latest assistant turn is actively being generated and updated.
- `recovering`
  The app is cleaning up after a failed or abnormal generation and is preparing to return to `idle`.

Required transition rules:

- `idle -> streaming` when the user sends a new request
- `streaming -> idle` on successful completion
- `streaming -> idle` on user-initiated abort after cleanup
- `streaming -> recovering` on generation failure or abnormal termination
- `recovering -> idle` once cleanup is complete

Required invariants:

- there is never more than one active generation
- progressive updates apply only while the state is `streaming`
- `recovering` is a short-lived cleanup state, not a long-running mode
- the user must be able to tell whether generation is active or not

## 7. Message Lifecycle Contract

The conversation model should distinguish between stable turns and the current in-progress assistant turn.

### User turn rules

- a user turn is created immediately when the user submits input
- the user turn is treated as durable conversation state
- the user turn remains visible even if the next assistant generation fails or is aborted

### Assistant turn rules

- a new assistant turn is created when streaming starts
- this assistant turn is initially considered in-progress
- only the latest in-progress assistant turn may receive progressive updates
- once generation completes, the turn becomes a stable completed assistant turn
- once a turn is completed, it must not receive further text updates

### Conversation integrity rules

- the UI should present the experience as one assistant answer growing over time
- internal transport details must not leak into user-facing conversation structure
- completed turns are immutable from the user's perspective

## 8. Event Semantics

The implementation may use any concrete transport mechanism that fits KMP, but the UX contract should preserve the following event meanings:

- `Start`
  Begin a new in-progress assistant turn
- `TextDelta`
  Add new text to the current in-progress assistant turn
- `Complete`
  Finalize the current assistant turn
- `Abort`
  Cancel the current generation and discard the in-progress assistant turn
- `Error`
  Mark the current generation as failed and enter recovery

Required event rules:

- `TextDelta` always targets the latest in-progress assistant turn
- `Complete` ends progressive mutation for the current assistant turn
- `Abort` and `Error` both terminate the active generation
- `Abort` and `Error` never delete prior completed turns
- the transport layer may emit finer-grained signals, but the UI layer should only depend on these semantics

## 9. Abort and Error Handling

### Abort behavior

- the user may stop generation only while the assistant is in `streaming`
- abort cancels the current generation attempt, not the entire chat session
- the in-progress assistant turn is removed from durable conversation history
- after abort cleanup, the app returns to `idle`

The default rule is conservative: preserve the session, discard the unfinished answer.

### Error behavior

Failures may include:

- local Ollama connectivity failure
- model unavailability
- relay failure
- malformed streamed content
- final response mapping failure
- other inference-layer exceptions

When an error occurs:

- the in-progress assistant turn must not be promoted to a stable history item
- the app should preserve prior completed conversation turns
- the manuscript editor state must remain intact
- the user should be informed that the generation failed
- the app should return to a retryable ready state quickly

## 10. Persistence and Restoration Rules

Streaming chat must behave safely across app recomposition, screen recreation, and app restart.

Required persistence rules:

- user turns are persisted when submitted
- completed assistant turns are persisted after completion
- failed or aborted in-progress assistant turns must not be restored as if they were completed answers
- the app must avoid reviving broken partial assistant text as stable history after restart

The minimum safe persistence rule for MVP is:

- only completed assistant turns are durable

If later planning chooses to persist partial progress for resilience, that strategy must still clearly distinguish unfinished content from completed history and must not degrade restart safety.

## 11. Success Criteria

This streaming chat UX is successful if all of the following are true:

1. A user can submit a request in `Workshop` and see the latest assistant turn grow progressively before completion.
2. The app never shows more than one active assistant generation at the same time.
3. The user can abort the current generation and the unfinished assistant turn does not remain as a completed message.
4. A failed generation does not erase earlier conversation turns or current manuscript state.
5. Only completed assistant turns are reused as stable conversation context.
6. The same high-level streaming UX contract works for both local and cloud inference modes.

## 12. Validation Expectations

Planning and implementation should include verification for at least these cases:

- normal streaming completion with visible incremental growth
- user abort during streaming
- provider or transport failure during streaming
- malformed partial output that still requires safe cleanup
- restart or restoration behavior after an interrupted generation
- prevention of multiple concurrent active streams

These checks matter because the core value of streaming chat is not only speed perception, but trust in the stability of the writing session.

## 13. Deferred Decisions For Planning

The following items are intentionally deferred to the implementation plan:

- exact transport representation for streaming events
- KMP-specific state holder structure
- Compose UI decomposition
- visual treatment for pending content
- whether partial text is ever checkpointed before completion
- analytics or debugging shape for generation traces

Those decisions should preserve this UX contract rather than redefine it.
