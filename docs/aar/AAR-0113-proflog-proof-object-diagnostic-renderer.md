# AAR-0113: Proflog Proof Object Diagnostic Renderer

- Date: 2026-06-17
- ADR: [ADR-0113](../adr/ADR-0113-proflog-proof-object-diagnostic-renderer.md)
- Branch: `adr-0113-proflog-proof-object-diagnostic-renderer`
- Status: complete

## Outcome

Added read-only diagnostic namespace `proflog.diagnostics.proof-trace` with
`proof-trace-edn`, `format-proof-trace`, and `render-proof-trace`.

## Evidence

Supported artifact shapes: kernel proof S-expressions whose tags appear in
`known-step-tags` (conj/split/close/savefml, quantifiers, equality, procedure
calls, profile markers).

Limitations: artifacts without recognized tags return `:insufficient-data`;
non-collection artifacts return `:unsupported`. Branch IDs and signed-tableau
metadata are not inferred.

Tests: `lein test proflog.diagnostics.proof-trace-test` — includes `refl-close`
label regression (`:reflexive-disequality-contradiction`). Wired into
`lein test-proflog-fast`.

## Follow-up

ADR-0114 may reuse literal extraction patterns; ADR-0115 may consume traces for
debugging but must not depend on rendering for semantic checks.
