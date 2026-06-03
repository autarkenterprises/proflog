# SJAS Structural Positive Reflected Calls

Date: 2026-06-03
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing structural positive calls now expand reflected clauses decoded
from `system-code` without a `pos-call` proof-rule tag.

The red test used a reflected zero-arity procedure:

```text
positive-demo :- false.
```

The proof node for `positive-demo` is encoded with canonical formula-code
syntax for the user relation. Decoding recovers the structural `(sym idx)`
relation head expected by the proof checker. The structural checker then scans
the encoded reflected Group-2b records, recovers the matching body from
`system-code`, and checks the child formula-bearing subtree for `false`.

The proof object carries only formula nodes; it does not contain `pos-call` or
any compiled reflected-program table reference.

## Verification

Focused red selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-positive-reflected-calls

FAIL in (sjas-proof-check-accepts-formula-bearing-positive-reflected-calls)
formula-bearing positive calls should recover reflected bodies from encoded system-code
actual: (not (successful? ()))
```

Focused green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-positive-reflected-calls
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
git diff --check
```
