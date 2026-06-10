# SJAS Wide Formula-Bearing Proof Nodes

Date: 2026-06-03

## Context

The first formula-bearing structural node shape encoded a formula as:

```text
(byte-count byte... child...)
```

That works only while the local formula code is shorter than one proof byte.
Full public targets such as `AxiomConj(system-code) /\\ not(Formula(t))` can be
much larger, so Track 1 needs a proof-code representation that is not bounded
by the initial one-byte formula-length fragment.

## Change

Added two compatible extensions:

- proof-code lists can now use a wide list-count tag when a proof list has 63
  or more items;
- formula-bearing structural nodes may carry their formula bytes as a proof
  byte-list payload:

```text
((formula-byte...) child...)
```

The old flat node shape remains accepted for compact formulas. The new
byte-list shape is a data representation, not a proof-rule tag; the structural
checker still infers local tableau rules from decoded formulas and children.

The relational wide-list decoder uses a recursive high/low count decrement
relation rather than host projection or a 4096-way committed scan.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-byte-list-formula-bearing-false-nodes`
- `lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-checks-formula-bearing-tableau-nodes`
- `lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-formula-bearing-tableau-nodes-without-rule-tags`
- `lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-has-no-proof-rule-tag-shortcuts`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

This removes the immediate structural proof-node size ceiling. A positive
public `tableau-proof/3` theorem proof with a formula-bearing structural
certificate is still needed before the legacy non-axiom certificate surface can
be narrowed safely.
