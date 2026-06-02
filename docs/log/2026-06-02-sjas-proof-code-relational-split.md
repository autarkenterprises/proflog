# SJAS Proof-Code Relational Split

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

`tableau-proof/3` distinguished the special `sjas-axiom` certificate from
substantive proof-code trees with `conda`. In ground axiom-citation mode this
was operationally convenient, but it made proof-code classification a
committed-choice scheduler inside the proof predicate. ADR-0073 Track 1 asks
for a relation over proof codes, not a search-control shortcut.

The split now proceeds through proof-code byte relations:

- `decode-sjas-axiom-proof-codeo` constrains the decoded public proof-code
  bytes to the fixed proof-grammar byte stream for `sjas-axiom`, then verifies
  those bytes through `sjas-formal-code-byteso`.
- `decode-non-sjas-axiom-proof-codeo` decodes the public proof bytes through
  `sjas-formal-code-byteso`, requires the proof-list tag at the root, and then
  decodes the proof tree with `decode-proof-byteso`.

The axiom route is intentionally byte-defined rather than canonical-code-term
defined. A noncanonical U-Grounding numeral that decodes to the same proof
bytes should still be accepted as the `sjas-axiom` certificate. The non-axiom
route is positively identified as a list-root proof tree rather than by a
large disequality constraint against the axiom proof bytes.

The same split is used by `tableau-proof/3` and `subst-prf/4`, so the
substitution proof predicate no longer relies on a delayed disequality over a
decoded proof object either.

## Red Evidence

The source audit was extended before implementation and failed against the
previous `tableau-proof/3` committed choice:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route

FAIL in (sjas-profile-source-audit-rejects-host-proof-checker-route)
tableau-proof proof-code classification must be a relation, not a committed-choice scheduler
expected: (not (re-find #"(?s)defn- sjas-tableau-proof-closeo.*?\(conda" profile-source))
  actual: ... "(conda"
```

An intermediate plain-`conde` implementation without a structural proof-code
discriminator left axiom-citation selectors wandering through non-axiom proof
grammar branches. That implementation was not retained.

## Verification

Focused green checks:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.
  elapsed 1:02.83 maxrss 265080KB

lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 68 assertions.
  0 failures, 0 errors.
  elapsed 0:23.44 maxrss 237336KB

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-false-close-certificates
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.
  elapsed 2:08.01 maxrss 324572KB

lein test-proflog-fast
  Ran 165 tests containing 654 assertions.
  0 failures, 0 errors.
  elapsed 2:06.78 maxrss 477876KB

lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
  elapsed 4:39.57 maxrss 576456KB

git diff --check
  clean.
```

`sjas-subst-prf-reconstructs-axiom-basis-without-system-registry` did not
complete in this turn. Stack samples showed full CPU use in `core.logic`
result reification/walking, not in proof-code classification. The first
intermediate run with residual proof-code disequalities was stopped after
`16:21.06` while reifying. After replacing the non-axiom discriminator with a
positive proof-list-root check, the rerun remained full-CPU with stable memory
through repeated samples and was stopped after `1:22:43` with no pass/fail
output. This is recorded as unresolved proof-materialization behavior, not as a
semantic rejection by `subst-prf/4`.
