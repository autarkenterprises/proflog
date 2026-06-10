# SJAS Profile Marker Audit Closure

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice closes a stale executable-audit gap for encoded SJAS profile-layer
proof symbols.

The checker already consumes or returns explicit SJAS profile/code evidence
markers for arithmetic closure, code decoding, axiom membership, theorem-code
reading, proof-predicate checking, and substitution checking. However, the
correspondence audit still classified several of those markers as
`:unresolved`, alongside obsolete generated-host evidence tags.

That was no longer accurate Track 1 bookkeeping. Implemented markers are now
classified as relevant SJAS proof evidence:

```clojure
profiled
willard-sjas-tableau0
willard-sjas-level1
willard-sjas-arithmetic
willard-sjas-code
willard-sjas-axiom-member
willard-sjas-theorem-code
willard-sjas-proof-check
willard-sjas-subst-code
willard-sjas-subst-proof-check
```

Obsolete generated-host markers are classified as excluded:

```clojure
willard-sjas-fact
sjas-generated-axiom-member
```

This does not assert a Track 2 correspondence theorem. It makes the executable
Track 1 audit match the current implementation: all encoded proof symbols are
now explicitly relevant or excluded rather than left in a stale unresolved
bucket.

## Red Evidence

The new implemented-evidence audit regression failed before the classification
change because real SJAS proof-check evidence still reported unresolved
symbols:

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/proof-term-audit-has-no-unresolved-markers-for-implemented-sjas-evidence
  unresolved: #{willard-sjas-arithmetic willard-sjas-theorem-code profiled willard-sjas-proof-check}
```

## Verification

```text
timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/implemented-sjas-profile-layer-markers-are-classified
  Ran 1 tests containing 12 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.sjas-correspondence-test/proof-term-audit-has-no-unresolved-markers-for-implemented-sjas-evidence
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test proflog.sjas-correspondence-test
  Ran 12 tests containing 93 assertions.
  0 failures, 0 errors.

lein with-profile +test run -m clojure.main -e '<proof-symbol status probe>'
  :unresolved ()
  :excluded (first-order guarded-call-seq-defer lem-close propositional query-neg-call query-neg-call-guarded-alt query-pos-call sjas-generated-axiom-member skolemized willard-sjas-fact)

git diff --check
  clean.

timeout -k 10s 900s lein test-proflog-fast
  Ran 164 tests containing 653 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
