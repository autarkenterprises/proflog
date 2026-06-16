# ADR-0113: Proflog Proof Object Diagnostic Renderer

- Status: accepted
- Date: 2026-06-16
- Branch: `adr-0113-proflog-proof-object-diagnostic-renderer`
- AAR: [AAR-0113](../aar/AAR-0113-proflog-proof-object-diagnostic-renderer.md)

## Context

Review of `bradleypallen/tableaux` showed that branch IDs, closure reasons, and
step-by-step construction records are useful for humans inspecting a tableau
run. Proflog already has its own proof-search and proof-object representations;
the useful improvement is a read-only diagnostic renderer that makes those
artifacts easier to inspect.

The renderer must be profile-transparent. It may display whatever proof-search
or proof-object structure a profile exposes, but it must not encode special
behavior for one profile or affect proof acceptance.

## Decision

Add a diagnostic renderer/replayer for Proflog proof objects or proof-search
artifacts. The public surface should be a small diagnostic namespace, for
example `proflog.diagnostics.proof-trace`, with pure functions that:

- produce a structured EDN trace from an existing artifact;
- format that trace as stable human-readable text;
- report rule-application traces, pending-goal traces, branch creation, and
  closure explanations when the source artifact contains enough information;
- report explicit `:unsupported` or `:insufficient-data` diagnostics when the
  source artifact does not contain enough information.

The renderer is not part of proof search. It must consume completed artifacts
after the fact, perform no proof acceptance, and introduce no host-side shortcut
that changes query answers.

## Consequences

- Long or subtle Proflog runs become easier to audit without changing their
  semantics.
- Future tests can compare stable diagnostic output for small known examples.
- The renderer may expose missing metadata in older proof artifacts; those
  cases should be reported as diagnostic limitations, not repaired by guessing.
- Text formatting must be deterministic enough for regression tests, while the
  structured EDN trace remains the preferred machine-facing interface.

## Test Obligations

- Red tests must require a known proof artifact to render a stable structured
  trace with rule applications and closure explanation.
- Red tests must require a human-readable formatter to produce deterministic
  output for the same trace.
- Red tests must require malformed or incomplete artifacts to return explicit
  diagnostic statuses rather than throwing incidental host exceptions.
- Red tests must show that invoking the renderer does not change proof
  acceptance, query answers, or reified result order for the inspected example.

## Exit Criteria

- A pure diagnostic namespace exists with structured and formatted trace entry
  points.
- The renderer handles at least one closed example and one open or incomplete
  example.
- Unsupported artifact shapes are documented and covered by tests.
- Relevant fast tests pass, and any extended renderer examples are marked with
  expected duration.
- AAR-0113 records the supported artifact shapes and known limitations.
