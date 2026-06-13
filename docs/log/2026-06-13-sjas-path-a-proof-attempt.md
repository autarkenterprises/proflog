# Path A Proof Attempt: Narrow Literal-Willard Fragment

Date: 2026-06-13

ADR: [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md)

Predecessor: [Path A target](2026-06-13-sjas-path-a-narrow-willard-fragment.md)

## Result

Path A has been advanced to an executable branch inventory, but the proof is not
complete.

The current narrowed theorem target remains viable:

```text
For every non-axiom formula-bearing structural proof tree P whose checker path
uses only Path-A-admitted branches:

  ProflogAccepts_A(P,S,F) iff SemPrf_D(decode(P),S,F)
```

The executable audit records 19 structural-checker branch families:

- 5 direct Willard/tableau branches;
- 8 branches admitted only with explicit lemmas;
- 6 excluded branch families.

## Direct Branches

These are already close to literal Willard `D` rules:

- complementary literal closure;
- conjunction;
- double negation and atomic/equality negation duals, modulo NNF;
- negation/de Morgan/implication branches, modulo agenda linearization;
- disjunction.

## Lemma Branches

These remain in the narrow theorem only after proof of named lemmas:

- literal save plus agenda continuation;
- `false` / `not true` truth closure;
- `not false` and `true` agenda continuation;
- universal / once-universal expansion;
- existential expansion;
- negated and bounded quantifier duals;
- additional quantifier expansion clauses.

The open Path A obligations are:

- `:agenda-ancestor-preservation`;
- `:truth-constant-semantics`;
- `:nnf-irrelevance`;
- `:quantifier-freshness`;
- `:gamma-parameter-admissibility`;
- `:bounded-guard-correctness`.

## Excluded Branches

These are outside Path A and must remain outside the narrowed theorem:

- disequality progress and storage;
- profile structural closes, including axiom membership, arithmetic/profile
  closure, recursive `tableau-proof/3`, and `subst-prf/4`;
- positive equality closures;
- equality-triggered reflected calls;
- equality agenda continuation;
- direct reflected calls.

## Executable Evidence

The audit API is:

```text
correspondence/sjas-structural-checker-rule-inventory
correspondence/audit-path-a-narrow-rule-inventory
```

The focused test is:

```text
path-a-rule-inventory-classifies-narrow-and-excluded-branches
```

It passed as part of:

```text
lein test proflog.sjas-correspondence-test
Ran 28 tests containing 397 assertions.
0 failures, 0 errors.
```

## Next Proof Work

To complete Path A, prove the six named lemmas above, then add a final
coverage theorem stating that all excluded branch families are outside the
Path-A domain. This can yield a genuine but intentionally narrow theorem.
