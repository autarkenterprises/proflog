# SJAS Large Proof Report Decoder Internalization

Date: 2026-06-01
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This slice removes a remaining host proof-code decoder from the large
`tableau-proof/3` public evidence path. The large-proof report bridge already
required semantic acceptance by `direct-negated-profile-closeo` before returning
compact public evidence, but it decoded the supplied proof-code tree for the
report with `sjas-code/proof-formal-code-term->proof`.

That host inverse did not decide proof validity, but it was still a host-side
decoder at the proof-predicate evidence boundary. The report now decodes the
proof-code tree by running the SJAS proof-code relation:

```clojure
(decode-proof-code-kindo proof-code '() sigma-out proof-bytes decoded-proof proof-kind)
```

and derives the public read marker with `code-read-marker-o`. The acceptance
gate remains unchanged: the report is built only after the SJAS proof checker
accepts the ground large `tableau-proof/3` query.

## Red Evidence

The source audit was extended to reject `proof-formal-code-term->proof` in the
SJAS profile source. Before implementation it failed:

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  FAIL: large proof reporting must decode proof-code trees through the SJAS proof-code relation
```

## Verification

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 33 assertions.
  0 failures, 0 errors.

timeout -k 5s 120s lein test :only proflog.willard-sjas-test/large-tableau-proof-zero-limit-does-not-materialize-report
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 2:03.69 maxrss 543868KB

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 598 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
