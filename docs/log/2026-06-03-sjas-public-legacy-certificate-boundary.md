# SJAS Public Legacy Certificate Boundary

Date: 2026-06-03

## Context

ADR-0073 Track 1 now has a formula-bearing structural checker whose proof
nodes are decoded from proof code and audited not to match legacy Proflog proof
rule tags. The public `tableau-proof/3` and `subst-prf/4` compatibility paths,
however, still accept many older proof-trace certificate shapes for non-axiom
proofs.

## Assessment

It would be unsound engineering to simply make `tableau-proof/3` reject all
legacy non-axiom certificates before adding at least one positive public
structural theorem proof that exercises the same public surface. The direct
structural checker and the proof-code decoder are covered, but an end-to-end
public structural theorem proof still needs to be constructed for the full
target:

```text
AxiomConj(system-code) /\ not(Formula(theorem-code)).
```

Until that exists, the final public-surface narrowing remains a Track 1
boundary:

- structural formula-bearing proof checking is implemented and decoded;
- the structural checker is isolated from legacy proof-rule tags;
- legacy non-axiom proof-trace certificates are still accepted by the broader
  compatibility checker and public tests;
- the final SJAS self-consistency code must not be printed as complete until
  this boundary is retired or explicitly justified.

## Required Next Step

Construct a positive public `tableau-proof/3` test whose proof certificate is a
formula-bearing structural tree for a concrete theorem. Then narrow the
non-`sjas-axiom` public path to the structural checker, or record the proof
obligation if a compatibility surface is intentionally retained for Track 2.
