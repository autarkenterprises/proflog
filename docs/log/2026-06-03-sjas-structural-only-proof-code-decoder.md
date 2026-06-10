# SJAS Structural-Only Non-Axiom Proof-Code Decoder

## Track 1 Slice

ADR-0073 Track 1, proof-code grammar and tableau proof checking.

## Problem

After the proof checker was narrowed to formula-bearing tableau trees, the
non-axiom proof-code decoder still decoded legacy symbolic proof traces. A
certificate such as `(false-close)` no longer passed the checker, but it still
decoded as a non-axiom proof object before failing later.

That left the proof-predicate boundary wider than the intended Track 1
fragment. The non-axiom proof predicate should accept structural tableau proof
trees, not symbolic kernel trace constructors.

## Red Test

`sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates` was
changed to require:

- `sjas-axiom` still decodes through the dedicated axiom citation path;
- a formula-bearing structural false-node proof decodes through the non-axiom
  path;
- a legacy list-root proof trace `(false-close)` does not decode as a
  non-axiom proof.

Before the implementation, the legacy proof trace still decoded:

```text
FAIL in (sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates)
non-axiom proof-code decoding should reject legacy proof-rule traces
expected: (empty? legacy-as-non-axiom)
actual: (not (empty? ((false-close))))
```

## Change

Added a structural-only proof-byte decoder:

- it decodes proof bytes, empty lists, short lists, and wide lists;
- it deliberately has no proof-symbol branch;
- `decode-non-sjas-axiom-proof-codeo` now uses it instead of the generic
  symbolic proof decoder.

The source audit now checks that the non-axiom proof-predicate decoder calls
`decode-structural-proof-byteso` and does not call `decode-proof-byteso`.

## Verification

Focused selectors passing after the change:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates
lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-checks-formula-bearing-tableau-nodes
lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
lein test :only proflog.willard-sjas-test/sjas-proof-checker-rejects-legacy-proof-rule-tag-certificates
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-generic-profiled-sidecar-certificates
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-answer-overlay-query-certificates
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check -- src/proflog/kernel/willard_sjas_profile.clj test/proflog/willard_sjas_test.clj LOG.md docs/log
```

## Remaining Work

The generic source-boundary proof-code utility still encodes older proof
symbols for proof reporting and correspondence inventory. The proof-predicate
non-axiom path no longer admits them, but a later cleanup should either
separate the generic proof-reporting grammar from the SJAS predicate grammar or
remove the legacy proof-symbol inventory entirely if no current reporting path
needs it.
