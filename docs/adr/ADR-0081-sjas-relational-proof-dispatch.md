# ADR-0081: SJAS Relational Proof Dispatch

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0081](../aar/AAR-0081-sjas-relational-proof-dispatch.md)

## Context

ADR-0073 Track 1 requires proof-predicate application to be an object relation
over encoded systems, theorem formulas, substitutions, and proof trees. Earlier
slices removed generated registries, host proof-checker calls, staged byte
projectors, and summary evidence. The remaining audit found committed-choice
`conda` dispatch in two proof-facing areas:

- generic kernel dispatch between an installed theory-profile close relation
  and ordinary kernel closure;
- SJAS public-code, axiom-membership, `tableau-proof/3`, and `subst-prf/4`
  branch classification.

These choices are not arithmetic rules. They are implementation scheduling.
Where alternatives are already distinguished by structural tags, byte-list
roots, proof-code roots, or explicit nil/non-nil guards, committed choice is
unnecessary and weakens the claim that the predicate is an ordinary relation.

## Decision

Replace proof-facing `conda` dispatch with `conde` guarded by structural
relations:

- the kernel theory-profile hook is selected by explicit nil/non-nil dynamic
  var guards, with ordinary kernel closure still available when a profile is
  installed but does not own the current formula;
- compact and U-Grounding code readers are ordinary alternatives over disjoint
  public code constructors;
- axiom-membership groups are ordinary finite alternatives over decoded system
  structure;
- axiom-citation and structural proof-code paths are selected by decoded proof
  bytes, not committed choice.

This change is a correctness/internalization cleanup, not a performance
optimization. It may expose additional valid relational answers if two finite
structural branches overlap; such overlap should be treated as semantic
evidence rather than hidden by committed choice.

## Consequences

- SJAS proof-facing code no longer imports or calls `conda`.
- The generic kernel no longer uses committed choice to mediate theory-profile
  closure.
- If a future profile requires committed scheduling for tractability, it must
  be isolated outside the arithmeticized proof predicate or justified by a
  separate correspondence argument.

## Test Obligations

- Add a red source audit rejecting `conda` import/calls in the SJAS proof
  profile and generic kernel profile-dispatch path.
- Keep the focused U-Grounding formula-bearing proof selector green.
- Run `lein test-proflog-fast` and `lein test-proflog-extended` before closing
  the ADR.
