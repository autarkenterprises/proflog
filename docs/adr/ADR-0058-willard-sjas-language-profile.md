# ADR-0058: Willard SJAS Language Profile Design

- Status: proposed
- Date: 2026-05-10
- Branch: `adr-0058-sjas-profile-design`
- AAR: pending

## Context

The nested SJAS archive now contains a public-witness corpus for Dan Willard's
self-verifying and self-justifying axiom-system work. The implementation
question is how to translate that material into a Proflog language profile
without confusing three different layers:

- the object-language arithmetic whose symbols appear in formulas;
- the deduction apparatus whose consistency is being reflected;
- the metatheorem that the constructed system is actually consistent.

The detailed extraction note is
[Willard SJAS Profile Design Notes](../log/2026-05-10-willard-sjas-profile-design.md).

The existing Proflog architecture already has a generic proof-profile dispatch
layer and a kernel-interleaved theory hook from the Robinson-Q work. That is the
right extension point for SJAS. A new SJAS profile must not be a host-side proof
checker or a whole-formula preprocessor hidden behind the query API.

## Decision

Design the first SJAS implementation target as a Type-A, semantic-tableaux,
Level-1 profile named `:willard-sjas-level1`.

This profile will target the Willard line where:

- addition is available as a total function;
- multiplication is represented as a three-place relation, not a total
  function symbol;
- the deduction apparatus is Fitting/Smullyan style semantic tableaux, plus a
  later limited Tab-1 proof-list rule if the implementation claims ISD(A) or
  IS#_D(beta);
- self-consistency is Level-1, meaning no simultaneous proofs of a Pi-star-1
  formula and its negation under the selected apparatus.

Defer the Hilbert/theta-function line. It is mathematically important, but it is
less aligned with the current Proflog kernel and includes a conjectural premise
in Willard's 2016 presentation.

## Designed Architecture

Add a public namespace such as `proflog.willard-sjas` for corpus-derived data
and formula construction:

- `u-grounding-language`;
- `level1-profile-language`;
- U-grounding term builders;
- bounded quantifier constructors;
- Delta-star-0 / Pi-star-1 / Sigma-star-1 classifiers;
- `SelfCons1` formula construction;
- finite `IS#_D(beta)`-style system construction.

Add a kernel profile namespace such as `proflog.kernel.willard-sjas-profile`:

- relational U-grounding arithmetic normalization and graph checking;
- relational proof-certificate checking for miniature semantic-tableau proofs;
- optional Tab-1 proof-list checking;
- a `willard-sjas-theory-closeo` rule bound through
  `kernel/*theory-profile-closeo*`.

Extend `proflog.proof-profile/prove-program*` with:

```clojure
(defmethod prove-program* :willard-sjas-level1
  [_profile program formula proof-limit fuel]
  (willard-sjas-profile/prove-program program formula proof-limit fuel))
```

Proof terms must expose profile use:

```clojure
(profiled willard-sjas-level1 ...)
```

The profile may use host Clojure while translating source text into a kernel
formula representation, including assigning stable codes and constructing a
fixed-point formula. After translation, it must not use host Clojure to decide
bounded arithmetic truth, formula-class membership, or proof-certificate
validity.

## Consequences

- This gives Proflog a clear route to demonstrate a nontrivial Willard-style
  self-justifying axiom system while preserving the distinction between formal
  proof execution and external metatheory.
- The first profile is intentionally modest. It should demonstrate a finite
  `IS#_D(beta)`-style system before claiming anything about full ISD(A).
- The formula classifier and proof-certificate checker are likely to be the
  main performance risks.
- Documentation must state that bounded contradiction probes do not prove
  consistency. Willard's actual consistency-preservation theorem remains a
  mathematical metatheorem unless separately mechanized.

## Test Obligations

The implementation ADR that promotes this design must start with failing tests
for each item below.

- Language tests show that the profile language contains U-grounding functions
  and `mult/3`, but no `mul/2` multiplication function.
- Classifier tests cover positive and negative Delta-star-0, Pi-star-1, and
  Sigma-star-1 examples.
- Arithmetic tests exercise forward, answer, and partial-synthesis modes for
  representative U-grounding operations and the `mult/3` graph relation.
- Proof-certificate tests accept valid miniature tableau proofs and reject
  malformed or open-branch certificates.
- If Tab-1 is claimed, proof-list tests enforce the intermediate theorem class
  restriction.
- The finite SJAS demonstrator proves its generated `SelfCons1` statement and
  at least one ordinary beta consequence through the selected profile.
- Bounded contradiction probes try to find simultaneous Pi-star-1/complement
  proofs and record their outcomes and timings.
- Source audits reject host proof checkers and whole-formula proof-time
  normalizers in the promoted profile path.

## Exit Criteria

- The design note remains linked from this ADR and from `LOG.md`.
- A future implementation ADR can follow this record without re-reviewing the
  Willard corpus from scratch.
- The first implementation target, profile name, proof route, source/proof-time
  boundary, test obligations, and known shortcomings are explicit.

## After Action Summary

Pending.
