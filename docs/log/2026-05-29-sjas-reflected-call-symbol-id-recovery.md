# SJAS Reflected Call Symbol-ID Recovery

Date: 2026-05-29

## Context

ADR-0073 Track 1 requires reflected procedure-call proof reconstruction to come
from encoded `system-code`, not from runtime clause tables or source-side
registries. Earlier slices removed the symbol table from syntax predicates,
Group-2 beta membership, and reflected Group-2b axiom membership, but reflected
`pos-call`/`neg-call` proof checking still resolved relation indexes through
`:sjas/symbol-index-entries`.

The user also clarified that reading `system-code` bytes is itself an
internalization obligation. The acceptable final state is object-language byte
reading through `sjas-formal-code-byteso` and its compact/U-Grounding relations,
not a host projection such as `code-term-bytes`. This slice is narrower: it
removes the source symbol-codebook dependency from reflected call recovery once
the byte stream has been exposed.

## Change

Added a focused regression,
`sjas-proof-predicates-check-reflected-calls-without-symbol-registry`, that:

- generates the ordinary demo proof certificate for `demo(1)`;
- removes runtime clause tables from the compiled program;
- removes `:sjas/registry`, including `:sjas/symbol-index-entries`;
- redefines `kernel/prove-programo` to throw if reached;
- requires `tableau-proof(system-code, theorem-code, proof-code)` to validate
  the reflected `neg-call` certificate anyway.

The proof profile now has a proof-facing formula decoder that first attempts
the existing host-symbol path, then falls back to the syntax decoder that keeps
application heads as `(sym n)`. Reflected call lookup similarly keeps the old
symbol path when available but can match a focused theorem atom headed by
`(sym relation-index)` directly against a reflected clause record
`[34 relation-index arity+1 body-bytes...]`.

Proof-antecedent reconstruction also uses the proof-facing fallback decoder, so
the reconstructed axiom basis can be built when the source symbol table is
absent.

## Verification

Red:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
FAIL: reflected procedure calls must compare encoded symbol ids from system-code, not host symbol names
```

Green and regression checks:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code
timeout -k 5s 150s lein test :only proflog.willard-sjas-test/sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry
timeout -k 5s 130s lein test :only proflog.willard-sjas-test/sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry
timeout -k 5s 160s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator
timeout -k 5s 140s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes
timeout -k 5s 360s lein test :only proflog.willard-sjas-test/sjas-subst-prf-checks-structural-non-generated-theorem-codes
timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-ignore-external-runtime-clauses
lein test-proflog-fast
lein test-proflog-extended
```

All listed green checks passed. The slow structural selectors were rerun
serially after a parallel run produced weak evidence: two selectors were killed
without test output and one timed out under load.

## Remaining Boundary

This slice does not finish Track 1. In particular:

- generic proof-facing formula decoding still prefers host symbol names when
  the source codebook is present;
- arithmetic/profile relation interpretation still expects built-in host
  relation names such as `lt`, `leq`, `mult`, `tableau-proof`, and `subst-prf`;
- compact system-code reads use proof-evidence summarization for tractability
  in several long-code paths, even though the semantic byte relation remains
  `sjas-formal-code-byteso`;
- the direct object-level tableau checker is still a covered subset of the
  currently generated certificate grammar, not a complete arithmetized
  semantic-tableau proof predicate.
