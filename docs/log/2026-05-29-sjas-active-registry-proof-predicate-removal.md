# SJAS Active Registry Proof-Predicate Removal

Date: 2026-05-29
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This is a Track 1 system-code reconstruction slice for ADR-0073. It removes
the proof-predicate guard that required `tableau-proof/3` and `subst-prf/4`
calls to use the active `:sjas/system-code` stored in the source-preprocessing
registry.

The removed guard was narrower than the finite symbol-table boundary: it did
not decode formulas or clauses, but it still made proof predicates consult a
host registry before accepting the supplied `system-code`. That is not
object-language axiom membership. For fixed axioms, the supplied code already
contains enough structural information to validate membership through the
system-code decoders.

## Change

The focused regression
`sjas-proof-predicates-do-not-require-source-preprocessing-registry` now drops
`:sjas/registry` entirely from a compiled SJAS program and proves a Group-0
fixed axiom through both:

```clojure
(tableau-proof system-code fixed-axiom-code sjas-axiom-code)
(subst-prf system-code fixed-axiom-code fixed-axiom-code sjas-axiom-code)
```

The test also audits `willard_sjas_profile.clj` so the removed
`sjas-active-systemo` guard cannot be reintroduced silently. The proof
predicate still checks the supplied `system-code` structurally through
`sjas-axiom-membero`; it no longer asks whether the code equals a registry
entry.

This does not finish the broader finite signature boundary. Formula-code symbol
indexes still resolve through the source-preprocessing symbol table for clauses
and formulas that contain application symbols. That remains a Track 1
internalization target: either encode the signature/codebook into `system-code`
and check it structurally, or redesign the proof checker to operate directly on
internal numeric symbol identifiers rather than host AST symbols.

## Verification

```text
timeout -k 5s 90s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-do-not-require-source-preprocessing-registry
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-kernel-certificates
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-reconstructs-axiom-basis-without-system-registry
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 140s lein test :only proflog.willard-sjas-test/sjas-subst-prf-checks-identity-substitution-certificates
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 160s lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

git diff --check -- src/proflog/kernel/willard_sjas_profile.clj test/proflog/willard_sjas_test.clj LOG.md docs/log/2026-05-29-sjas-active-registry-proof-predicate-removal.md
  0 errors.

lein test-proflog-fast
  Ran 159 tests containing 594 assertions.
  0 failures, 0 errors.
  fast_elapsed_s=103.86

lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
  extended_elapsed_s=234.02
```

`lein test-proflog-sjas-focused` was also started for broad progress-visible
coverage. It advanced through the first three tests, then spent about five
minutes in
`sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures` and
was stopped for exact-selector investigation rather than left as an opaque long
run. The exact selectors directly covering this slice and the adjacent
proof-predicate paths are the green checks above.
