# KMP Templates List And Editor Separation Design

Date: 2026-04-19
Status: Draft approved in conversation, written for implementation planning

## 1. Goal

Clean up the `Templates` UX by separating browsing and editing into distinct flows.

The current screen mixes:

- new template creation
- existing template editing
- template list browsing
- workshop selection state

This creates a cluttered experience and makes it hard for the writer to tell whether they are selecting, creating, or editing.

The design should make `Templates` feel like a personal catalog of story modes, while preserving fast `Use in Workshop` actions and local-first behavior.

## 2. Product Alignment

This design follows the product rules for this repository:

- `Templates` is a core product surface
- selected templates act like story modes, not just prompt snippets
- local template authoring remains editable and local-first
- remix creates an editable private draft rather than mutating a published source

This change is a UX restructuring of the existing `Templates` product surface. It does not change template data semantics or workshop prompt behavior.

## 3. Problem Statement

The current `Templates` screen stacks multiple high-weight contexts in one column:

- a full new-template editor
- a full detail editor for the selected template
- the template list

This causes four UX problems:

1. The screen does not establish a primary job.
2. Users cannot easily distinguish list browsing from editor mode.
3. New and existing template forms can both compete for attention.
4. Selection for `Workshop` is visually mixed with editing actions.

The result is a messy experience, especially because the editor card is dense and carries many inputs.

## 4. Design Principles

The redesign should follow these principles:

1. The default `Templates` experience is list-first.
2. Browsing and editing happen in separate screens, not in parallel on one screen.
3. `Create` and `Edit` reuse the same editor layout.
4. `Use in Workshop` remains a fast action from the list.
5. Dangerous actions remain available but visually secondary to editing and saving.
6. Save, delete, remix, and navigation feedback should always make the current state obvious.

## 5. Information Architecture

`Templates` is split into two screens:

1. `Templates List`
2. `Template Editor`

### Templates List

The list screen is the default entry point for the `Templates` top-level area.

Its job is to help the writer:

- scan available templates
- open a template for editing
- quickly bind a template to `Workshop`
- create a new template

### Template Editor

The editor screen is used for both:

- creating a new template
- editing an existing template

Its job is to focus on authoring only. The list is not shown alongside the editor.

## 6. Screen Design

### 6.1 Templates List

The list screen contains:

- top app title: `Templates`
- one primary CTA: `New Template`
- short helper copy explaining that templates are reusable story modes for `Workshop`
- a compact `Workshop active` banner when a template is currently selected
- a vertically scrolling list of template cards

Each template card shows:

- title
- genre
- premise summary
- subtle state badge when active, using copy such as `Active in Workshop`

Each template card supports:

- card click: open the template in the editor screen
- `Use in Workshop`: select the template without leaving the list

The card click behavior must always mean `open for editing`.

The screen must not show any large editor form inline.

### 6.2 Empty State

When there are no local templates, the list screen shows an empty state instead of an empty card list.

The empty state contains:

- a message explaining that there are no local templates yet
- a clear `New Template` CTA
- supportive copy that templates can be used as story modes in `Workshop`

### 6.3 Template Editor

The editor screen reuses a single layout for `New Template` and `Edit Template`.

The editor header contains:

- back navigation to `Templates`
- screen title: `New Template` or `Edit Template`
- supportive copy describing the template as a story mode for `Workshop`
- header-level `Delete` action only when editing an existing template

The editor body contains:

- title field
- genre field
- premise field
- prompt block input and add action
- editable prompt block list
- enrich action
- save action

The list is not visible on this screen.

### 6.4 Version History

Version history remains available when editing an existing template, but it appears below the editor as secondary information.

It should never outrank the main authoring form in the visual hierarchy.

### 6.5 Remix Entry

When the writer enters from a `Board` remix action:

- skip the list screen
- open the editor directly with a new editable draft
- show a small remix source banner near the top of the editor

The remix source is informational only. The resulting save still creates or updates a private local draft according to existing product rules.

## 7. Interaction Model

### 7.1 Core Navigation

The expected state transitions are:

- opening `Templates` -> `Templates List`
- tapping `New Template` -> `Template Editor` in create mode
- tapping a template card -> `Template Editor` in edit mode
- remix from `Board` -> `Template Editor` in create mode with remix banner

### 7.2 Save

On save success:

- return to the list screen
- refresh the template list
- subtly highlight the saved template card
- preserve or update `Workshop active` label if the saved template is the active one

On save failure:

- stay on the editor screen
- preserve all entered values
- show a clear inline error near the top or near the save action

### 7.3 Delete

Delete is available in the editor header for existing templates.

Delete is not shown on the list card. Destructive behavior is centralized in the editor so the list can stay focused on scanning, opening, and workshop selection.

Delete behavior:

- pressing `Delete` opens a confirmation dialog
- the dialog includes the template title
- if the template is currently active in `Workshop`, the dialog explains that workshop selection will also be cleared

Delete confirmation copy should be explicit and calm, for example:

`Delete template?`

`'Noir Tracker' will be removed. If it is active in Workshop, that selection will also be cleared.`

On delete success:

- return to the list screen
- remove the deleted template from the list
- clear active workshop selection when applicable
- show short feedback such as `Template deleted`

### 7.4 Back Navigation

If the editor has unsaved changes, back navigation should show a discard confirmation before leaving the screen.

If there are no unsaved changes, back returns directly to the list.

### 7.5 Workshop Selection

`Use in Workshop` remains on the list card because it is a quick selection action, not an editing action.

When used:

- the list screen stays visible
- the active workshop banner updates immediately
- the selected card shows an `Active in Workshop` style cue

This keeps the list as the fastest place to assign a story mode.

## 8. Visual Hierarchy Rules

The redesign should make the current mode obvious at a glance.

Hierarchy rules:

- only one heavy authoring form is visible at a time
- list browsing and editing are never shown as peer surfaces on the same screen
- card click and card button actions must feel different in meaning
- destructive actions use lower visual emphasis than save or primary navigation
- workshop selection state is shown as status, not as competing editor content

## 9. Common Pattern Guidance

This design intentionally follows a common `collection + editor` pattern used by note apps, CMS tools, issue trackers, and content editors:

- collection screen for scanning and selecting
- dedicated editor screen for focused changes
- card click means open
- button actions mean direct operations such as use or delete
- create and edit share one editor layout

This is recommended because it matches user expectations and reduces ambiguity.

## 10. Data And State Considerations

This UX change should not require a change to template persistence format.

The main state changes are UI-level:

- explicit navigation state between list and editor
- editor mode: create vs edit
- dirty-state tracking for discard confirmation
- saved-template return highlighting
- remix-entry metadata for banner display

The design should preserve:

- local-first template saving
- active workshop template persistence
- existing rule that deleting an active template clears workshop selection

## 11. Error Handling

The UX must keep writers oriented when operations fail.

Expected handling:

- save failure: inline error, keep editor values
- enrich failure: inline error inside the editor, keep current draft
- delete failure: keep editor open and show clear error feedback
- template load failure: fail gracefully to the list or show recoverable inline messaging

Error copy should be short, specific, and non-destructive.

## 12. Testing Focus

Implementation planning should verify:

- `Templates` opens on the list screen by default
- `New Template` opens the shared editor in create mode
- card click opens the shared editor in edit mode
- `Use in Workshop` works from the list without navigation
- save returns to the list and highlights the saved card
- delete confirms before execution and clears active workshop state when needed
- remix opens the editor directly with source context shown
- back navigation warns on unsaved changes
- empty state appears correctly when there are no templates

## 13. Out Of Scope

This design does not add:

- template search or filter as a required MVP change
- new template fields
- new board publishing behavior
- changes to workshop prompt construction
- changes to persistence schema
- a new navigation library decision

These may be considered later, but they are not part of this UX cleanup.

## 14. Planning Recommendation

The implementation plan should treat this as a focused UX restructuring of `Templates`.

Suggested planning slices:

1. introduce list-vs-editor navigation state
2. convert current inline editors into one reusable full-screen editor flow
3. move delete to editor header with confirmation behavior
4. preserve remix, active workshop, and save/delete feedback behaviors
5. add regression coverage for navigation and destructive action rules
