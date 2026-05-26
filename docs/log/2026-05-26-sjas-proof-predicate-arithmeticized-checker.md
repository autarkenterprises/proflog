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

The checker currently covers the proof constructors reached by existing SJAS
certificate generation:

- top-level `conj` followed by direct SJAS arithmetic closure for ground
  arithmetic theorems such as `1 = 1` and `lt(1,2)`;
- recursive tableau `conj` and `split`;
- `univ`, `once-univ`, and `witness`;
- complementary literal `close` and `savefml`;
- reflected `pos-call` and `neg-call` procedure calls, evaluated against the
  reflected-only program boundary rather than external runtime clauses.

Two no-kernel regressions guard the main accepted paths by redefining
`kernel/prove-programo` to throw:

- simple arithmetic certificates for `tableau-proof/3` and identity
  `subst-prf/4`;
- reflected-clause `neg-call` certificates such as `demo(1)`.

A source audit also rejects reintroducing the exact
`kernel/prove-programo target` proof-predicate route.

## Remaining Boundary

This removes the host proof-kernel validation shortcut from SJAS proof
predicates, but it is not yet a paper-grade proof that the checker is a complete
arithmetic formalization of Willard's tableau deductive apparatus. In
particular, reflected procedure calls still use the reflected-only compiled
program as the operational clause boundary; a stricter future stage can replace
that with clause lookup decoded from the `system-code` reflected-clause bytes.

Operationally, the current checker is proof-directed and rejects unsupported
proof constructors instead of falling back to the host kernel.

## Verification

- Red regression observed before implementation:
  `sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator`
  failed because both proof predicates reached `kernel/prove-programo`.
- Red regression observed before procedure-call extension:
  `sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator`
  failed because `neg-call` was not yet handled by the local checker.
- Green focused checks:
  - `sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator`
  - `sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator`
  - `sjas-profile-source-audit-rejects-host-proof-checker-route`
  - structural non-generated theorem-code proof-predicate checks
  - Group-3 self-consistency substantive proof check
- `git diff --check`
- `lein test-proflog-fast`: 152 tests, 570 assertions
- `lein test-proflog-extended`: 68 tests, 203 assertions
