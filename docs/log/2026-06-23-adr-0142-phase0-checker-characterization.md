# ADR-0142 Phase 0: SJAS checker lemma-composition characterization

Date: 2026-06-23
Plan: `~/.claude/plans/velvet-conjuring-frost.md` (overcoming the two ADR-0142
closure obstructions)

Phase 0 cheaply characterizes how the SJAS structural checker establishes
`alpha |- Xi`, to decide whether Obstruction 1 (the Theorem 2.2 combination
steps) requires a new cut rule. **It does not** — and this avoids enlarging the
trusted base.

## Probes (read-only, REPL)

1. `query/query-succeeds` over a *clause program* does not prove
   `(forall p. not S(p)) => not S(c)` (universal instantiation of an assumed
   hypothesis), though it proves propositional modus tollens. → lemmas are not
   usable as goal hypotheses.
2. The checker does **not** prove the diagonal `DK` over the multiplication
   system (false in 53 ms — the search exhausts immediately, not a fuel limit).
   → the checker does not *search* for the alternating-quantifier combination.
3. The structural proof grammar (`decode-structural-proof-byteso`,
   `sjas-structural-proof-check-state-decodedo`) consumes **formula-bearing
   tableau trees** (nodes = formula bytes + child nodes). Its docstring is
   explicit: "Symbolic Proflog proof-rule tags are deliberately excluded here;
   the checker infers rules from formulas and branch state." There is **no rule
   tag and no cut tag**.
4. The checker has builders (`structural-proof-formula-nodes`) and many passing
   construct-and-check tests for closed tableaux over both propositional
   connectives (`...accepts-formula-bearing-{and-true-false,disjunction,double-negation,negated-conjunction}-tableaux`)
   and **quantifiers** (`...accepts-formula-bearing-{quantifier-expansions,
   bounded-quantifier-expansions,negated-quantifier-expansions,
   distinct-nested-existential-parameters}`, `sjas-quantifier-instantiation-is-formula-bearing-and-tag-free`).

## Conclusion

The SJAS checker is a **validator of constructed cut-free tableau trees**, not a
search-complete prover. This matches ADR-0142's "construct-and-check, never
search-and-trust" mandate.

**Obstruction 1 (decision: do NOT add a cut rule).** Because the checker accepts
constructed cut-free trees with full propositional + quantifier rule support, the
three Theorem 2.2 combination steps (Eqs 6/8/9) are overcome by **constructing
their cut-free tableau trees** and validating them with the existing checker. The
super-exponential cut-elimination blow-up is Willard's *worst-case* bound; these
combinations are the shallow, "immediate" implications between `A`, `B`, `D*`,
`Subst`, `C`, so their cut-free trees are constructible. This **avoids the
trusted-base enlargement** a cut/lemma rule would require (the Phase-2 fallback is
therefore not expected to be needed). `proflog.sjas-cut-composition` remains a
useful *with-cut* reference and size-accounting model, but the landed proof
objects will be cut-free trees the ordinary checker already accepts.

**Obstruction 2 (unchanged).** The `SemPrf^k` bound step still needs symbolic
closed-form arithmetic (Phase 1): the bound `q` with `Log(q,K) > p` is a tower,
infeasible to materialize as a bit-list, so `sjas-iterated-logo` must reduce
`Log(tower(K,m),K) -> m` algebraically (Willard Lemma 3.2), gated on the
reflected V1/V2 totality so the addition-only variant fails the same step.

## Next

Proceed to Phase 1 (symbolic arithmetic). The tree-construction work for
Obstruction 1 reuses `structural-proof-formula-nodes` / `dsjas-tableau-proof-object`
and the existing checker; it is construct-and-check over the real generated
system, with the addition-only non-closure as the falsifier.
