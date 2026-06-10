# ADR-0084: SJAS Walked System-Code Reconstruction

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0084](../aar/AAR-0084-sjas-walked-system-code-reconstruction.md)

## Context

ADR-0073 Track 1 requires `tableau-proof/3` and `subst-prf/4` to be relations
over the supplied public codes. In recursive proof-predicate leaves those codes
may appear under branch equality state. The proof predicate already read
`proof-code` and `theorem-code` through `sigma`, and axiom-member citation used
a walked wrapper for `system-code` and `formula-code`.

The structural proof branches still reconstructed `AxiomConj(system-code)` and
validated `system-code` through helpers that read public code from an empty
equality state. That was harmless for top-level ground calls, but it was not a
fully relational Track 1 implementation for nested proof predicates whose
`system-code` argument has been bound by equality before the proof predicate
leaf is closed.

## Decision

Add proof-free walked system-code helpers:

- `sjas-system-code-valid-walked-coreo`;
- `sjas-system-axiom-formula-walked-coreo`.

These helpers read `system-code` through the current equality substitution,
return the walked system-code term, and reconstruct or validate the decoded
finite system from the resulting bytes.

Use the walked helpers in:

- the structural branch of `sjas-tableau-proof-coreo`;
- the structural branch of `sjas-subst-prf-coreo`;
- the substitution-result axiom branch of `sjas-subst-prf-coreo`.

Nested structural proof checking now receives the walked system-code term.

## Consequences

- Public top-level ground proof-predicate calls keep the same behavior.
- Recursive proof-predicate leaves no longer lose equality bindings on their
  `system-code` argument.
- The change is a correctness/internalization repair, not a SelfCons runtime
  optimization.

## Test Obligations

- Add a red focused test that proves the walked helpers are present, are used by
  `tableau-proof/3` and `subst-prf/4`, and can consume a system-code variable
  bound in `sigma`.
- Keep the existing `subst-prf` system-code validation audit green.
- Keep the proof-profile source audit green.
- Keep a representative public `tableau-proof/3` formula-bearing certificate
  selector green.

## After Action

Completed on 2026-06-09. See the AAR for red/green evidence.
