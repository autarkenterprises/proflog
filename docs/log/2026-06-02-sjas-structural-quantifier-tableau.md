# SJAS Structural Quantifier Tableau Nodes

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

Formula-bearing tableau nodes now support the core quantifier expansion
constructors without Proflog proof-trace tags:

- existential expansion;
- universal expansion;
- once-universal expansion.

The proof code supplies the quantified formula node and a single child node.
The checker infers the local tableau rule from the decoded formula:

- `exists` introduces a fresh rigid parameter in the branch environment;
- `forall` introduces a fresh proof variable in the branch environment and
  proof-variable set;
- `once-forall` uses the same one-step instantiation discipline without
  carrying a `once-univ` proof tag.

The first regression uses closed bodies (`false`) so that this slice proves the
formula-tree route for quantifier nodes without introducing a separate
term-payload grammar for explicit witnesses. More expressive formula-bearing
proofs with visible instantiated child terms remain a later Track 1 slice.

## Verification

Focused red/green selector:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-quantifier-expansions
```

Regression selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disjunction-tableaux
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
git diff --check
```
