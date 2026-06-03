# SJAS Formula-Bearing Tableau Nodes

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

ADR: [ADR-0073: SJAS Internalization Correspondence Program](../adr/ADR-0073-sjas-internalization-correspondence-program.md)

## Track 1 Slice

The SJAS-side proof predicate should not require Proflog proof-trace evidence
when the semantic-tableau proof tree itself supplies enough structure. A proof
trace is not cost-free: in Proflog it is another value returned from
`core.logic/run`, effectively a sequence/tree of evidence. If that trace is
made part of the SJAS proof predicate, the trace must be Godel-encoded, decoded,
traversed, and related arithmetically to the tableau. That additional
arithmeticization is expensive and unnecessary whenever the tableau tree alone
determines the local deduction or closure rule.

This slice adds the first formula-bearing tableau proof node shape:

```clojure
(byte-count byte... child...)
```

The byte payload is the encoded formula at that tableau node. The remaining
items are child proof nodes. No local rule name such as `conj`, `skip-true`, or
`false-close` is present in this proof fragment. Instead, the checker decodes
the node formula and infers the admissible local rule from the parent formula,
child formulas, and branch state.

## Implemented Fragment

The first structural fragment covers:

- false leaf closure;
- conjunction expansion into one child branch whose agenda receives the right
  conjunct;
- true skip to the next formula on the current branch.

For example, a closed tableau for:

```clojure
(and true false)
```

can now be encoded as a formula-bearing root node for `(and true false)`, a
formula-bearing child node for `true`, and a formula-bearing leaf node for
`false`, without any Proflog proof-rule tags.

## Result

This does not complete formula-bearing tableau checking. Equality, arithmetic
closure, disjunction, quantifiers, reflected calls, and substitution/proof
predicate calls still use the older proof-directed certificate branches. It
does establish the preferred Track 1 direction: proof codes should encode the
semantic tableau directly, and proof traces should be excluded, macro-expanded,
or justified only when they are genuinely required.

## Verification

Focused selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-codes-encode-formula-bearing-tableau-nodes-without-rule-tags
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-classifies-every-encoded-certificate-symbol
git diff --check
```

All passed after implementation.
