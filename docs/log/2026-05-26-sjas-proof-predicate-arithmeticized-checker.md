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
`sjas-reflected-proof-program` bridge, and `program/call-clauseo` inside the
SJAS profile.

## Remaining Boundary

This removes the host proof-kernel validation shortcut from SJAS proof
predicates, but it is not yet a paper-grade proof that the checker is a complete
arithmetic formalization of Willard's tableau deductive apparatus. The reflected
procedure-call boundary is now system-code-driven for the currently generated
single-clause proof shapes. A stricter future stage still needs broader
coverage for grouped multi-clause alternatives and a proof that the decoded
clause-call relation preserves the relevant tableau intensional invariants.

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
- Green focused checks:
  - `sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator`
  - `sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator`
  - `sjas-proof-predicates-check-reflected-calls-from-system-code`
  - `sjas-formal-codes-are-godel-byte-terms`
  - `sjas-profile-source-audit-rejects-host-proof-checker-route`
  - structural non-generated theorem-code proof-predicate checks
  - Group-3 self-consistency substantive proof check
- `git diff --check`
- `lein test-proflog-fast`: 152 tests, 570 assertions
- `lein test-proflog-extended`: 68 tests, 203 assertions
