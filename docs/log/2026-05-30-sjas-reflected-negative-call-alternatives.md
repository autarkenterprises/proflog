# SJAS Reflected Negative Call Alternatives

Date: 2026-05-30
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 slice internalizes the multi-clause reflected negative-call
constructor used by Proflog's procedure-call rule. The SJAS checker already
validated single-clause `neg-call` certificates by decoding a reflected
Group-2b clause from `system-code`; it did not validate `neg-call-alt`
certificates for relations with multiple reflected clauses. The proof-code
alphabet also omitted the inner `alt` constructor emitted by the kernel for
alternative selection.

The new relation scans the reflected Group-2b records inside the encoded
system, reconstructs every matching negated alternative for the focused
ground call, and checks the supplied `(neg-call-alt (alt ...))` proof against
one reconstructed alternative. It does not consult compiled clause tables,
alternative tables, guarded tables, or a reflected runtime program registry.

This is still Track 1, not a Track 2 correspondence claim: the implementation
adds object-level checking for a concrete proof constructor family rather than
justifying a host-kernel shortcut.

## Red/Green Evidence

Two focused tests captured the prior boundary:

- Encoding `(neg-call-alt (alt (refl-close)))` failed because `alt` was not in
  the SJAS proof-code alphabet.
- The SJAS-local proof checker rejected a decoded multi-clause reflected
  negative-call target even with the intended proof tree.

After implementation, `alt` is an encoded proof symbol classified with the
procedure-call expansion obligations, the direct proof checker accepts the
multi-clause negative call from `system-code`, and public `tableau-proof/3`
accepts an encoded `neg-call-alt` certificate even when the compiled clause
tables are stripped from the program.

## Verification

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-negative-call-alternative-evidence
  Red before implementation:
  ERROR, Unsupported proof symbol in SJAS certificate {:symbol alt}.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-reflected-negative-call-alternatives-from-system-code
  Red before implementation:
  FAIL, expected successful proof-check result but got ().

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-negative-call-alternative-evidence
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 240s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-reflected-negative-call-alternatives-from-system-code
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 300s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-reflected-negative-call-alternative-certificates
  Ran 1 tests containing 3 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 20 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 10s 900s lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 596 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
