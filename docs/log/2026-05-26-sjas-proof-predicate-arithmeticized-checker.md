# SJAS Proof-Predicate Arithmeticized Checker

Date: 2026-05-26

## Context

ADR-0073 Track 1 called for closing the SJAS proof-predicate internalization gap
by replacing the non-`sjas-axiom` shortcut through `kernel/prove-programo`.
That shortcut decoded a proof-code tree and then asked the host Proflog proof
kernel to validate the decoded tree against
`(and system-axioms negated-theorem)`. It was useful as an executable
correspondence experiment, but it collapsed the intended SJAS virtualization of
the deductive apparatus.

## Change

`tableau-proof/3` and `subst-prf/4` now validate non-`sjas-axiom` certificates
through `sjas-proof-check-programo`, a local SJAS proof-check relation over
decoded formula and proof constructors. The proof-predicate paths no longer
call `kernel/prove-programo target ... decoded-proof`.

Compact theorem-code decoding inside `tableau-proof/3` and `subst-prf/4` now
uses the same object code-byte relation used by syntax predicates. Accepted
compact proof-predicate certificates therefore expose `sjas-code-arg` evidence
inside `willard-sjas-theorem-code` rather than the previous staged
`(sjas-code-bytes)` marker. Compact substitution source and target decoding
also use object byte reads.

The checker currently covers the proof constructors reached by existing SJAS
certificate generation:

- top-level `conj` followed by direct SJAS arithmetic closure for ground
  arithmetic theorems such as `1 = 1` and `lt(1,2)`;
- recursive tableau `conj` and `split`;
- `univ`, `once-univ`, and `witness`;
- complementary literal `close` and `savefml`;
- reflected `pos-call` and `neg-call` procedure calls, decoded from the
  `system-code` reflected-clause bytes rather than from compiled clause lists.

Two no-kernel regressions guard the main accepted paths by redefining
`kernel/prove-programo` to throw:

- simple arithmetic certificates for `tableau-proof/3` and identity
  `subst-prf/4`;
- reflected-clause `neg-call` certificates such as `demo(1)`.
- reflected-clause `neg-call` certificates with `:clause-list`,
  `:alternative-clause-list`, `:guarded-clause-list`, and
  `:sjas/reflected-program` removed, forcing procedure-call evidence to come
  from encoded `system-code`.

A source audit also rejects reintroducing the exact
`kernel/prove-programo target` proof-predicate route, the removed
`sjas-reflected-proof-program` bridge, `program/call-clauseo` inside the SJAS
profile, and the old staged compact theorem/substitution decoder names.

## Remaining Boundary

This removes the host proof-kernel validation shortcut from SJAS proof
predicates, but it is not yet a paper-grade proof that the checker is a complete
arithmetic formalization of Willard's tableau deductive apparatus. The reflected
procedure-call boundary is now system-code-driven for the currently generated
single-clause proof shapes. A stricter future stage still needs broader
coverage for grouped multi-clause alternatives and a proof that the decoded
clause-call relation preserves the relevant tableau intensional invariants.

The finite symbol-index table remains a source-preprocessing boundary. Formula
and system codes store relation/function indexes, not full structural names or
declarations. This is acceptable only if symbol identity is proven irrelevant
up to a fixed injective coding of the finite language, or if the language
signature itself is internalized into the system code. The current code treats
that table as nominal preprocessing metadata, so it remains a Track-1/Track-2b
obligation rather than completed U-Grounding internalization. The current
justification is recorded in
[SJAS Symbol-Table Isomorphism Justification](2026-05-26-sjas-symbol-table-isomorphism-justification.md):
the table is admissible as a fixed codebook only up to kind/arity-preserving
signature isomorphism, not as a freestanding nominal fact.

Large U-Grounding Level-1 substitution still has a narrower performance
boundary: compact substitution codes are read through object byte relations,
but the Level-1 fixed-point check keeps an isolated ground-byte projection for
large U-Grounding substitution-side formula codes. A direct relational attempt
failed at the previous fuel bound and then overflowed core.logic's occurs check
when the large substituted theorem numeral was walked to formula comparison.
That bridge is documented as a tractability boundary, not a final
internalization result.

Operationally, the current checker is proof-directed and rejects unsupported
proof constructors instead of falling back to the host kernel.

## Verification

- Red regression observed before implementation:
  `sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator`
  failed because both proof predicates reached `kernel/prove-programo`.
- Red regression observed before procedure-call extension:
  `sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator`
  failed because `neg-call` was not yet handled by the local checker.
- Red regression observed before system-code reflected-call lookup:
  `sjas-proof-predicates-check-reflected-calls-from-system-code` failed when
  compiled clause lists and the reflected-program registry entry were removed.
- Red regressions observed before compact theorem-code staging removal:
  `sjas-tableau-proof-checks-kernel-certificates` and
  `sjas-subst-prf-checks-identity-substitution-certificates` accepted proof
  predicates without `sjas-code-arg` evidence under `willard-sjas-theorem-code`.
- Red source audit observed before substitution staging cleanup:
  `sjas-profile-source-audit-rejects-host-proof-checker-route` found the old
  staged compact theorem and substitution decoder names.
- U-Grounding substitution boundary probe:
  direct object decoding of the Level-1 fixed-point substitution source failed
  at fuels 240, 500, 1000, and 2000; after restoring only the isolated
  U-Grounding substitution-side byte projection, target walking through the
  object relation produced a core.logic `StackOverflowError`, so that target
  side was isolated under the same U-Grounding boundary.
- Green focused checks:
  - `sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator`
  - `sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator`
  - `sjas-proof-predicates-check-reflected-calls-from-system-code`
  - `sjas-tableau-proof-checks-kernel-certificates`
  - `sjas-subst-prf-checks-identity-substitution-certificates`
  - `sjas-subst-code-relates-structural-substitution-codes`
  - `sjas-u-grounding-subst-code-computes-level1-fixed-point`
  - `sjas-symbol-table-is-irrelevant-up-to-signature-isomorphism`
  - `sjas-formal-codes-are-godel-byte-terms`
  - `sjas-profile-source-audit-rejects-host-proof-checker-route`
  - structural non-generated theorem-code proof-predicate checks
  - Group-3 self-consistency substantive proof check
- `git diff --check`
- `lein test-proflog-fast`: 152 tests, 570 assertions
- `lein test-proflog-extended`: 68 tests, 203 assertions
