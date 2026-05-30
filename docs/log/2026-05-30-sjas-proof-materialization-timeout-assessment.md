# SJAS Proof Materialization Timeout Assessment

Date: 2026-05-30
Branch: `adr-0073-sjas-correspondence-program`

## Question

The phrase "timed out while materializing full proof evidence" needed a fresh
assessment because it can describe two different failures:

1. the SJAS proof predicate does not accept the encoded certificate in time; or
2. the predicate accepts, but `core.logic` cannot reify the full proof stream in
   time.

Only the first case would be a semantic failure of the arithmeticized proof
checker. The second is a reporting/runtime materialization boundary.

## Current Evidence

The focused public selector completed under the current code:

```text
timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:38.72 maxrss 613904KB
```

A phase probe of the same path measured:

```text
:system 35.4ms
:group3-proof 41011.7ms
:certificate 0.7ms
:tableau-proof-public 45178.5ms
:group3-proof-pr-str-len 86
:certificate-pr-str-len 1595
:tableau-proof-pr-str-len 211
:contains-witness true
:contains-once-univ true
```

This shows that the public `tableau-proof/3` path completes and returns proof
evidence containing the relevant decoded certificate constructors.

The raw direct relation was also probed by bypassing the public report bridge
and forcing the `core.logic` result. After the Group-3 proof was available, the
lazy relation call returned immediately, but forcing the result did not finish
inside a 360 second envelope:

```text
:group3-proof 37424.5ms
:direct-reified 0.8ms
timeout after 360s while forcing the returned proof stream
```

The timeout is therefore still specifically materialization/reification of the
raw miniKanren proof stream, not SJAS proof-predicate acceptance.

## Boundary

The current public large-proof path is a reporting optimization, not a semantic
acceptance shortcut. It first requires the SJAS proof-check relation to accept
the encoded theorem/proof pair. Only an accepted large direct `tableau-proof/3`
query may return the compact public report:

```clojure
(profiled willard-sjas-proof-check
          proof-code-read-marker
          theorem-code-read-marker
          decoded-certificate-proof)
```

The returned report summarizes code-byte reader subproofs with format markers,
but it does not summarize the decoded proof certificate itself; the proof
constructor tree remains visible and is checked by the SJAS relation before the
report is returned.

## Improvement

The report decoder used to be prepared before checking whether the caller had
requested any proof evidence. A new focused regression showed this by rebinding
the reporting-side proof decoder to throw and then running a large
`tableau-proof/3` query with `proof-limit 0`; the test failed red because the
report decoder still ran.

The fix moves large-report construction behind the proof-limit and SJAS
acceptance gate. With `proof-limit 0`, the query now returns no proofs without
running the report materializer. With a positive proof limit, the report is
constructed only after the object-level proof checker accepts.

Large `sjas-axiom` citations are deliberately not handled by the non-axiom
report bridge. A focused regression confirmed that Tableau-0 Group-3 axiom
citations still use the ordinary object-level axiom-member path.

## Verification

```text
timeout -k 5s 120s lein test :only proflog.willard-sjas-test/large-tableau-proof-zero-limit-does-not-materialize-report
  Red before implementation: reporting-side decoder was invoked despite proof-limit 0.
  Green after implementation: Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.
  elapsed 1:38.72 maxrss 613904KB

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-cites-tableau0-group-three-from-system-code
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 596 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```

## Conclusion

The old timeout should not be interpreted as failure to prove the large Group-3
certificate. It is a `core.logic` evidence-materialization limitation for the
raw relation. The public API now runs the accepted SJAS proof check to
completion and returns compact but constructor-bearing evidence; the latest
change also prevents unnecessary report materialization when no proof is
requested.
