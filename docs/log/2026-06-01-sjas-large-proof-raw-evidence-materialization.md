# SJAS Large Proof Raw Evidence Materialization

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Question

The earlier message

```text
Timed out while materializing full proof evidence.
```

needed a fresh assessment after the large proof report decoder and guarded
proof-checking slices. The public `tableau-proof/3` path completed, but the
private/raw `direct-negated-profile-closeo` path still needed to be forced with
its proof variable reified.

## Finding

The public checked proof path still completes:

```text
timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:10.63 maxrss 630396KB
```

The corrected raw probe, using the negated branch literal seen by the kernel,
timed out after the Group-3 certificate was already available:

```text
timeout -k 10s 480s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein with-profile +test run -m clojure.main -e '<raw direct proof probe>'
  :group3-proof-ms 27337.302265
  timed out while forcing raw direct proof evidence
```

A focused regression was then added and confirmed red by timeout:

```text
timeout -k 10s 180s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/large-tableau-proof-raw-direct-evidence-materializes
  timed out
```

## Cause

The remaining timeout was avoidable evidence materialization, not semantic
failure. The large theorem-code decoder selected compact read evidence only
when its input sigma was literally the host value `'()`. In the raw direct
proof-predicate branch, proof-code decoding starts from empty sigma but returns
`sigma-proof` as a logic output. Even when that output is eventually empty, the
host-side check did not see a literal empty list at relation construction time,
so theorem decoding took the detailed per-byte read-proof path.

That made raw reification try to materialize a large theorem-code reader proof
instead of the compact theorem-code marker already used by the accepted public
report path.

## Fix

The ground direct `tableau-proof/3` branch now uses
`sjas-ground-structural-negated-theorem-proofo` for non-axiom certificates. For
large compact theorem codes, this helper requires the proof-code reader to leave
sigma empty and then calls the existing structural theorem decoder with a
host-ground empty sigma. This lets the same object-level byte decoder produce
the theorem formula while returning compact read evidence:

```clojure
(profiled willard-sjas-proof-check
          proof-code-read-marker
          theorem-code-read-marker
          decoded-certificate-proof)
```

The fix does not skip formula decoding, certificate decoding, or proof checking.
It only bounds the public/private evidence term for a large code-reader proof.

## Green Evidence

The new focused regression now materializes raw evidence:

```text
timeout -k 10s 300s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/large-tableau-proof-raw-direct-evidence-materializes
  Ran 1 tests containing 5 assertions.
  0 failures, 0 errors.
  elapsed 1:11.81 maxrss 715820KB
```

The same raw probe now reports one materialized proof:

```text
:group3-proof-ms 25711.073703
:raw-count 1
:raw-ms 31988.798049
:first-pr-str-len 178
elapsed 1:10.30 maxrss 593896KB
```

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/large-tableau-proof-zero-limit-does-not-materialize-report
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 33 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:08.10 maxrss 626220KB

git diff --check
  clean.

timeout -k 10s 900s lein test-proflog-fast
  Ran 162 tests containing 638 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.

timeout -k 10s 3600s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test-proflog-sjas-focused
  :SUMMARY pass=457 fail=0 error=0
  elapsed 12:12.09 maxrss 611800KB
```
