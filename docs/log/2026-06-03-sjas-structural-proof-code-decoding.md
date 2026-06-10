# SJAS Structural Proof-Code Decoding

Date: 2026-06-03

## Context

Formula-bearing structural tableau tests had been using in-memory proof lists.
The public proof predicate, however, receives a proof-code term. For Track 1,
structural proof nodes must survive the public proof-code decoder and be
checked by the SJAS proof predicate as object-level proof data.

## Finding

Added focused coverage showing that a compact proof certificate for a
formula-bearing structural tableau node decodes relationally through
`decode-non-sjas-axiom-proof-codeo` and is then accepted by
`sjas-proof-check-programo`.

The proof tree contains no symbolic proof-rule tags; it is a list-root proof
node whose first item is the formula byte count, followed by formula bytes and
child proof nodes.

## Verification

Focused selectors:

- `lein test :only proflog.willard-sjas-test/sjas-proof-code-decoder-checks-formula-bearing-tableau-nodes`
- `lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-formula-bearing-tableau-nodes-without-rule-tags`
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions`
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
- `lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol`
- `git diff --check`

All passed.

## Remaining Work

This verifies that structural proof-code decoding is not an in-memory-only path.
It does not by itself narrow the legacy proof-code grammar; public proof
predicates still accept older proof-trace certificates until the final Track 1
surface is retired, narrowed, or explicitly justified.
