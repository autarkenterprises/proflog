# Path A: Narrow Literal-Willard Fragment

Date: 2026-06-13

ADR: [ADR-0102](../adr/ADR-0102-sjas-counterexample-proof-targets.md)

## Goal

Prove a corrected literal-Willard correspondence theorem by narrowing the
domain until every accepted checker step is either a direct Willard
semantic-tableau rule or a proved irrelevance/bookkeeping lemma.

This path deliberately does **not** try to cover the full SJAS self-reference
machinery. It is the conservative fragment proof.

## Proposed Theorem

For every covered system code `S`, formula code `F`, and non-axiom
formula-bearing structural proof tree `P` in the narrow fragment:

```text
ProflogAccepts_A(P,S,F) iff SemPrf_D(decode(P),S,F)
```

and the proof code of `P` satisfies the Conventional Tableaux Encoding lower
bound for every explicit application/function-symbol occurrence in
`decode(P)`.

## Narrow Fragment

The domain excludes:

- bare `sjas-axiom` citation certificates;
- equality/disequality theory progression and closure;
- arithmetic/profile relation closure;
- reflected procedure-call expansion;
- `axiom-member/2` leaf closure;
- recursive `tableau-proof/3`;
- `subst-prf/4`;
- any proof branch whose closure depends on decoded system-code membership
  rather than ordinary tableau ancestry.

The domain includes:

- formula-bearing structural proof nodes;
- truth constants, literals, conjunction, disjunction, implication, negation;
- first-order universal/existential expansion;
- bounded universal/existential expansion;
- same-branch agenda continuation;
- literal branch closure by explicit complementary branch literals.

## Required Lemmas

1. **Node decoding.** Every proof node in the fragment decodes to exactly one
   formula-bearing tableau sentence and its child nodes.

2. **Agenda linearization.** Same-branch agenda continuation preserves the
   Willard ancestor relation. A child selected from `unexpanded` is a descendant
   of the logical parent that introduced it.

3. **NNF and truth irrelevance.** Double negation, de Morgan, implication
   rewriting, `true` skip, `not false` skip, `false` closure, and `not true`
   closure correspond to Willard's listed negation rules plus standard tableau
   truth constants.

4. **Quantifier freshness.** `sjas-next-branch-nomo` and `env` extension provide
   the fresh parameter required for delta rules and the parameter term required
   for gamma rules.

5. **Bounded guard correctness.** The generated `leq` guard formulas are
   exactly the bounded gamma/delta guards in Willard's rules.

6. **Closure soundness.** Literal leaves close only when branch state contains
   explicit complementary branch literals representing `Y` and `not Y`.

7. **Completeness by replay.** Every narrow `D` tableau can be encoded as a
   formula-bearing proof tree whose child order and agenda linearization are
   accepted by the checker.

8. **Structural size lower bound.** For formula-bearing structural proof trees,
   proof-code bytes grow at least linearly with the formula bytes at every
   node, and explicit application/function-symbol occurrences are not hidden
   outside the proof object.

## Implementation Tasks

- Add a path-A domain audit helper that rejects the excluded branch families or
  classifies a supplied proof as outside the narrow fragment.
- Add a branch coverage matrix for the included checker clauses.
- Consolidate existing positive tests for alpha, beta, negation, implication,
  quantifiers, bounded quantifiers, agenda continuation, truth constants, and
  literal closure under a path-A selector.
- Add negative tests showing that `sjas-axiom`, equality, arithmetic,
  reflected-call, `tableau-proof`, and `subst-prf` examples are outside Path A.

## Status

Pursued to a precise theorem target and proof obligation list. Not yet complete:
the domain audit helper and the formal branch coverage matrix still need to be
implemented before this can be claimed as a proved fragment.
