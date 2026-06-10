# ADR-0089: SJAS Group-3 Presented Code Representation

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0087-sjas-selfcons-fixedpoint-basis` (named before the audit's
  ADR-0087 landed; renumbered to 0089 on adoption because 0087 and 0088 were
  already assigned)
- AAR: [AAR-0089](../aar/AAR-0089-sjas-group3-presented-code-representation.md)

## Context

This slice was executed by a parallel agent in a separate worktree while the
main directory was under the 2026-06-09 audit, and was reviewed and adopted
by merge during that audit. The detailed slice note is
[SJAS Group-3 Presented Code Representation](../log/2026-06-09-sjas-group3-presented-code-representation.md).

ADR-0073 Track 1 fixed-point reconstruction requires Group-3 to refer to the
public system code `s` selected by the proof predicate, not merely to an
equivalent byte payload. ADR-0071 made the distinction observable: a
`:u-grounding` system excludes compact `code-N` constructors from its
language and presents codes as binary U-Grounding numerals. The proof-free
`AxiomConj(s)` path already used the walked public `system-code` for
Tableau-0, but the axiom-member Group-3 relations and Level-1 Group-3
reconstruction accepted either internal code-term shape after decoding the
same bytes, so a U-Grounding system could cite a compact-embedded variant of
its own self-consistency sentence.

## Decision

Thread the public code representation kind, inferred relationally by the
existing object-level code readers, through Group-3 reconstruction:

- `sjas-public-code-byteso` and `sjas-public-code-bytes-coreo` expose the
  representation kind; `code-kind-internal-termo` relates it to internal
  code terms.
- Tableau-0 and Level-1 Group-3 reconstruction use the presented kind in
  both the proof-producing `axiom-member` path and the proof-free core path.
- Level-1 `AxiomConj(s)` reconstruction applies the same presented
  representation to the fixed-point skeleton check.
- Proof-free system-code validation calls the presented-code `AxiomConj`
  arity after walking equality state, so validation and reconstruction
  agree.

No source registry, host code-format lookup, or host byte projector is
added; the kind is propagated as a logic value.

## Consequences

- A U-Grounding system no longer accepts a compact `code-N` variant of its
  fixed-point SelfCons sentence merely because the byte payload matches;
  the self-reference of Group-3 is anchored to the presented public code.
- ADR-0073's slice 3 (system-code and fixed-point axiom reconstruction) is
  correspondingly strengthened, recorded in the ADR-0073 text.
- The slice independently re-validated the ADR-0087 Pi-star-1 Group-3 shape:
  its focused selectors ran green on top of that correction.

## Test Obligations

Recorded with red/green evidence in the slice note and AAR-0089: the new
Tableau-0 and Level-1 wrong-representation rejection selectors red before
implementation and green after, the affected Group-3/U-Grounding and walked
system-code selectors green, and both broad gates green.
