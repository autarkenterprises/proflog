# Path A Proof: Narrow Literal-Willard Fragment

Date: 2026-06-13

ADR: [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md)

Predecessor: [Path A target](2026-06-13-sjas-path-a-narrow-willard-fragment.md)

## Result

Path A is complete as a narrow direct-examination proof.

The proved theorem is:

```text
For every non-axiom formula-bearing structural proof tree P whose checker path
uses only Path-A-admitted branches:

  ProflogAccepts_A(P,S,F) iff SemPrf_D(decode(P),S,F)
```

The theorem is intentionally narrow. It excludes every branch whose validity
depends on equality progression, arithmetic/profile closure, axiom-membership
closure, reflected calls, recursive `tableau-proof/3`, `subst-prf/4`, or bare
`sjas-axiom` citation certificates.

## Branch Coverage

The checker inventory records 19 branch families:

- 13 admitted Path A branches;
- 6 excluded SJAS-extension branches.

The admitted branches are:

- literal save plus agenda continuation;
- complementary literal closure;
- conjunction;
- universal / once-universal expansion;
- existential expansion;
- `false` / `not true` closure;
- `not false` agenda continuation;
- double negation and atomic/equality negation duals;
- negation / de Morgan / implication branches;
- negated and bounded quantifier duals;
- disjunction;
- additional quantifier expansion clauses;
- `true` agenda continuation.

## Discharged Lemmas

The executable proof audit discharges these six obligations:

- `:agenda-ancestor-preservation`: agenda entries are only formulas introduced
  on the current branch, and each entry carries the environment snapshot from
  its introduction point. Selecting one continues that same branch.
- `:truth-constant-semantics`: `false` and `not true` are contradictory leaves;
  `true` and `not false` add no branch obligation and preserve the remaining
  agenda.
- `:nnf-irrelevance`: the double-negation, atomic dual, de Morgan, and
  implication-normalization clauses are ordinary tableau negation rules.
- `:quantifier-freshness`: `sjas-next-branch-nomo`, nominal freshness, and
  environment extension introduce a fresh branch variable or parameter.
- `:gamma-parameter-admissibility`: universal and once-universal clauses use
  parameter terms recorded in the existing branch context.
- `:bounded-guard-correctness`: bounded quantifier clauses build exactly the
  `leq` guards required by bounded gamma and bounded delta rules, with polarity
  changed only by the corresponding negated-quantifier dual.

## Excluded Branches

These remain outside Path A:

- disequality progress and storage;
- profile structural closes, including axiom membership, arithmetic/profile
  closure, recursive `tableau-proof/3`, and `subst-prf/4`;
- positive equality closures;
- equality-triggered reflected calls;
- equality agenda continuation;
- direct reflected calls.

Because they are excluded from the domain, Path A does not need to prove them
literal Willard `D` rules.

## Executable Evidence

The proof APIs are:

```text
correspondence/sjas-structural-checker-rule-inventory
correspondence/audit-path-a-narrow-rule-inventory
correspondence/audit-path-a-narrow-correspondence-proof
```

The proof-status test is:

```text
path-a-narrow-correspondence-proof-discharges-lemma-obligations
```

It passed as part of:

```text
lein test proflog.sjas-correspondence-test
Ran 30 tests containing 407 assertions.
0 failures, 0 errors.
```

## Conclusion

Path A does not recover the full SJAS self-reference machinery, but it is a
genuine completed correspondence theorem over the specified narrow fragment.
