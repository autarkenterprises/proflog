# SJAS Proof Predicate Shortcut Excision

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice tightens ADR-0073 Track 1 from "marker evidence is acceptable when
the checked relation is object-level" to the stricter endpoint now required for
an in-principle arithmeticized proof predicate: proof-facing paths must return
the actual object-level code-reader and proof-check evidence they consume.

Removed proof-code and public-code marker bridges:

- `sjas-decode-proof-formula-code-markero`
- `decode-proof-code-kindo`
- `code-read-marker-o`
- `sjas-public-code-bytes-markero`

`tableau-proof/3`, `subst-prf/4`, public system-code reads, and theorem-code
reads now carry the full `sjas-formal-code-byteso` derivation instead of
collapsing it to `(sjas-code-bytes)` or `(sjas-ug-code-bytes)` marker evidence.

Removed host-ground proof-predicate entrypoints and scheduling bridges:

- `ground-negated-subst-code-args`
- `ground-negated-app-args`
- `ground-negated-relation`
- `direct-negated-profile-closeo`
- `hide-sjas-clauses-from-generic-sidecars`

Public SJAS proof search now enters through `kernel/prove-programo` with the
SJAS theory rule bound, rather than selecting a proof-profile atom through a
host-ground relation-symbol inspection or hiding host clause metadata to steer
generic sidecar scheduling.

Removed compact-code reader host scheduling:

- `compact-code-bytes-no-walko`
- `compact-code-term-byte-count`
- host `seq?` dispatch over byte terms
- host `symbol?` and `integer?` dispatch over compact code constructors

Compact code reading now uses the same relational path in all modes:
`equality/walko`, finite `code-constructoro`, object-level byte numeral
decoding, and recursive `code-argso` evidence.

Removed proof-directed tableau shortcuts:

- `sjas-ground-structural-negated-theorem-proofo`
- `sjas-arithmetic-branch-closeo`
- `sjas-top-conj-*`
- `sjas-negated-theorem-branch-proof-checko`

`sjas-proof-check-programo` now calls `sjas-proof-check-stateo` directly from
the initial tableau state. `tableau-proof/3` reconstructs
`and(system-axioms, negated-theorem)` and validates the decoded proof against
that state instead of first trying special cases that focus the negated theorem
or close a top-level arithmetic branch.

The public profile still calls `kernel/prove-programo` as the outer Proflog
search engine, with `willard-sjas-theory-closeo` bound as the theory rule. The
source audit now counts only call forms and permits exactly those two public
entrypoint calls. It does not permit `kernel/prove-programo` inside
`tableau-proof/3`, `subst-prf/4`, or `sjas-proof-check-programo`.

Removed the `compact-false-formula-code` special case from `axiom-member`.
False-formula beta membership now goes through the same structural system-code
membership relation as every other formula code rather than comparing the whole
formula-code term to a precomputed host constant.

After the host `seq?` byte-reader branch was removed, the first current-source
public axiom-citation selector ran for more than four hours and then failed
with `java.lang.OutOfMemoryError` in `bits->canonical-termo`. The fix was not
to restore host shape inspection. Instead, `code-byte-termo` now places the
finite `byte-bitso` relation first and uses committed object-level parse
evidence when the supplied term already reads as that byte. Canonical term
generation remains available only after the finite byte value is fixed.

## Red Evidence

The source audit was extended before implementation and failed on the intended
shortcuts:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  FAIL sjas-public-code-bytes-markero
  FAIL sjas-decode-proof-formula-code-markero
  FAIL decode-proof-code-kindo
  FAIL code-read-marker-o

lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  FAIL defn- ground-negated-subst-code-args
  FAIL defn- ground-negated-app-args
  FAIL defn- ground-negated-relation
  FAIL direct-negated-profile
  FAIL hide-sjas-clauses-from-generic-sidecars

lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  FAIL compact-code-bytes-no-walko
  FAIL compact-code-term-byte-count

lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  FAIL sjas-ground-structural-negated-theorem-proofo
  FAIL sjas-arithmetic-branch-closeo
  FAIL sjas-top-conj-
  FAIL sjas-negated-theorem-branch-proof-checko

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-axiom-citation-certificates
  ERROR java.lang.OutOfMemoryError: Java heap space
  stack included bits->canonical-termo
```

## Verification

Completed current-source checks:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 65 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-false-close-certificates
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-axiom-citation-certificates
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

lein test-proflog-fast
  Ran 164 tests containing 653 assertions.
  0 failures, 0 errors.

lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.

lein test-proflog-sjas-focused
  :SUMMARY pass=469 fail=0 error=0
  Note: this focused-suite process was started before the shortcut-excision
  edits and is retained as baseline productive-run evidence, not as
  current-source proof-heavy confirmation.
```

Long proof-predicate checks are still being allowed to run to completion as
soundness evidence, rather than being stopped merely because the full
object-evidence path is expensive.
