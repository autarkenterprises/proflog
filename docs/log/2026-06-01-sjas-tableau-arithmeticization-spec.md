# SJAS Tableau Arithmeticization Specification

Date: 2026-06-01

## Context

The current work had drifted toward small proof-checker fixes and performance
probes. The user refocused the goal on the system-description requirement: the
paper needs a semantically sound in-principle specification of arithmeticized
semantic-tableau proofs for `IS#_D(beta)`, even if evaluating that relation is
impractical for large examples.

## Result

Added the normative Track 1 specification:

- [SJAS Tableau Proof Arithmeticization Specification](../SJAS_TABLEAU_ARITHMETIZATION_SPEC.md)

The specification defines `TabPrf_beta(s,t,p)` as a bounded object-language
relation over system, theorem, and proof codes. It states that the system code
decodes to the finite SJAS axiom basis, the theorem code decodes to a formula,
and the proof code decodes to a finite closed semantic tableau for:

```text
AxiomConj(s) /\ not(Formula(t)).
```

It also specifies the required subrelations: byte/code reading, syntax,
system-code reconstruction, structural axiom membership, proof-tree navigation,
local tableau rule checking, branch closure, substitution/fixed-point checking,
and reflected procedure-call expansion from decoded system clauses.

## Design Position

This artifact separates three claims that had been easy to conflate:

1. The Track 1 semantic target is an arithmeticized proof predicate over codes.
2. The current Proflog implementation is an executable approximation and
   growing instantiation of that target.
3. Any direct call to the Proflog proof kernel remains a Track 2 bridge unless
   a correspondence theorem proves that it preserves the relevant tableau-tree,
   closure, and proof-size invariants.

The specification explicitly permits runtime performance shortcuts only when
they instantiate the same bounded relation over the accepted proof-code
fragment. It does not permit host theorem registries, generated axiom-member
facts, nominal symbol tables, or proof-target tables to serve as semantic
authorities.

## Paper Update

The LOPSTR/PPDP system-description paper now includes a concise subsection on
the in-principle arithmeticization claim. The paper version states the same
core relation, its decomposition into bounded object-language checks, the
ordinary tableau soundness argument, the encoded-tableau representability
claim, and the reason theorem-level equivalence is too weak for SJAS.

## Verification

- `git diff --check`
- `make paper` in `lopstr-ppdp26`
