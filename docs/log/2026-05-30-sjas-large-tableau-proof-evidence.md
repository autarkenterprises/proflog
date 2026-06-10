# SJAS Large Tableau Proof Evidence

Date: 2026-05-30
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 slice removes the large non-axiom `tableau-proof/3` reporting
shortcut and resolves the resulting public proof-materialization timeout. The
previous implementation checked a large Group-3 theorem-code proof through the
SJAS close relation in truth mode, but returned a synthetic
`(profiled willard-sjas-proof-check)` marker instead of the decoded proof tree
that was supplied by the proof code.

That was semantically better than the old `kernel/prove-programo` validator
shortcut because acceptance still came from the SJAS proof-check relation, but
it was not a concrete implementation of the proof predicate's proof-object
surface. The public proof result now reports the decoded non-axiom proof tree
for large theorem codes. For large direct `tableau-proof/3` queries, semantic
acceptance still comes from the SJAS proof-check relation in truth mode; the
returned proof report is then built from the checked proof-code bytes so public
callers do not force `core.logic` to reify the internal proof-search state.

## Red/Green Evidence

The source audit now rejects:

```text
large-non-axiom-tableau-proof-query
large-tableau-proof-summary
reported-decoded-proof-o
```

Before the implementation, the audit failed on all three names. After removing
the shortcut it passed 32 assertions.

## Runtime Boundary

The focused selector
`sjas-tableau-proof-checks-structural-non-generated-theorem-codes` passes with
full proof evidence.

The larger selector
`sjas-selfcons-demonstration-uses-substantive-proof-targets` initially exceeded
a 900s envelope after this change. Focused probes separated the phases:

- Group-3 certificate generation completed in roughly two minutes.
- Proof-code decoding completed in a few seconds.
- The full SJAS semantic proof check completed in truth mode in roughly
  90-110 seconds.
- Asking `core.logic` to return the public proof term for the same accepted
  direct `tableau-proof/3` query did not complete inside 900 seconds.

The timeout was therefore proof-report reification, not proof-predicate
acceptance. A right-conjunct-focused checker was added for the legitimate
tableau state obtained after expanding `(and system-axioms neg-theorem)`: it
focuses `neg-theorem` while leaving `system-axioms` pending. This is the same
branch selection the generic scheduler could make, but it was not sufficient by
itself to make public reification tractable.

The final fix keeps acceptance relational and avoids reification only at the
reporting boundary. For a large ground non-axiom `tableau-proof/3` query, the
profile first runs `direct-negated-profile-closeo` in truth mode. Only after
that SJAS relation accepts does it decode the supplied proof-code term through
the source-boundary proof-code inverse and return the small public evidence
term:

```clojure
(profiled willard-sjas-proof-check
          proof-code-read-marker
          theorem-code-read-marker
          decoded-certificate-proof)
```

This is not a semantic acceptance shortcut and does not call
`kernel/prove-programo` for the proof predicate. It is, however, a report
construction bridge: the proof tree returned to the public API is decoded from
the already-checked proof code rather than reified from the internal
miniKanren search state.

## Verification

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Red before implementation: 3 failures on the large proof-summary names.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 5s 360s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 900s lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Timed out while materializing full proof evidence.

timeout -k 10s 900s lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  After the report bridge:
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-round-trips-byte-payload-evidence
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-round-trips-equality-triggered-atom-closure-evidence
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-u-grounding-canonical-byte-evidence
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 5s 360s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes
  Ran 1 tests containing 4 assertions.
  0 failures, 0 errors.

git diff --check
  clean.

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 594 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
