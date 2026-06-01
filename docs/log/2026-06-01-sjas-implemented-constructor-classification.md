# SJAS Implemented Constructor Classification

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice aligns the executable correspondence audit with the Track 1 proof
checker implementation. Several proof constructors were still classified as
`:unresolved` even though earlier Track 1 slices had added object-level SJAS
checker support for them:

```clojure
eq-step
eq-triggered-call
eq-triggered-neg-call
eq-refl
eq-bind
par-bind
pos-call
neg-call
neg-call-alt
neg-call-guarded-alt
alt
guarded-alt
guarded-neg-alt
guarded-neg-alt-saturated
guarded-seq-step
guarded-seq-last
guarded-call-seq-step
guarded-residual-seq-step
guarded-residual-seq-last
guarded-scope-exists
guarded-scope-done
guarded-seq-done
guarded-call-seq-done
guarded-residual-seq-done
guard-saturation-done
guard-eq
```

These constructors are now classified as relevant SJAS proof-checker structure,
not stale unresolved gaps. This does not claim a Track 2 correspondence theorem;
it records that Track 1 has concrete checker clauses and focused regressions for
these proof constructors. Remaining unresolved layer/profile symbols should now
be easier to inspect because the audit no longer conflates implemented
object-level machinery with unimplemented bridges.

## Red Evidence

The new audit regression initially failed for every listed constructor because
the classification still returned `:unresolved`:

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/implemented-proof-checker-constructors-are-relevant
  26 failures: implemented equality/procedure/guarded constructors were :unresolved
```

After moving those constructors into relevant equality/procedure groups, stale
expectations in the older correspondence tests also failed and were updated.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/implemented-proof-checker-constructors-are-relevant
  Ran 1 tests containing 26 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 22 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/proof-term-audit-reports-obligations-for-actual-proof-trees
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test proflog.sjas-correspondence-test
  Ran 10 tests containing 78 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 900s lein test-proflog-fast
  Ran 162 tests containing 638 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
