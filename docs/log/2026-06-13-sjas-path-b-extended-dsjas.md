# Path B: Extended `D_SJAS` Apparatus

Date: 2026-06-13

ADR: [ADR-0102](../adr/ADR-0102-sjas-counterexample-proof-targets.md)

## Goal

Define the actual selected SJAS deductive apparatus implemented by the
arithmetized proof machinery, then prove correspondence against that apparatus
rather than against literal Willard `D` alone.

This path is the plausible route for the full self-reference machinery, but it
is also the higher-risk route. It must show that the extended apparatus remains
within the kind of semantic-tableau deductive method for which Willard's SJAS
results apply, or else explicitly move to a Track 2c theorem about
`IS#_{D_SJAS}(beta)`.

## Candidate Apparatus

`D_SJAS` should contain these rule families:

1. **Base tableau rules.** Willard's alpha, beta, negation, implication, gamma,
   delta, bounded gamma, bounded delta, and branch closure.

2. **Truth constants.** Closure for `false` / `not true`, and skip/identity
   treatment for `true` / `not false`.

3. **Equality and free-constructor theory.** Reflexive disequality closure,
   rigid disequality continuation, disequality storage, equality progression,
   positive equality contradiction, stored-disequality violation, and
   complementary literals after equality substitution.

4. **Arithmetic relation closure.** Object-language evaluation of the selected
   SJAS arithmetic/code relations, with polarity-correct closure when a
   relation is true under a negated literal or false under a positive literal.

5. **Axiom membership.** `axiom-member(S,F)` closes only by decoded system-code
   membership in Group-0, Group-1, Group-2 beta, Group-2b reflected clauses, or
   Group-3.

6. **Reflected procedure calls.** A positive reflected atom expands to the
   decoded reflected clause body; a negative reflected atom expands to the
   decoded negated body or guarded alternatives. Equality-triggered reflected
   calls use the same rule after equality walking.

7. **`tableau-proof/3`.** A negative proof-predicate atom closes when the
   supplied proof code decodes either to:
   - an axiom citation whose theorem code is in the decoded axiom basis; or
   - a formula-bearing structural proof tree of `AxiomConj(S) and not F` that
     is accepted recursively by `D_SJAS`.

8. **`subst-prf/4`.** A negative substitution-proof atom closes when the
   substitution relation is valid and the supplied proof code validates the
   substituted theorem under the extended axiom basis, again by the selected
   proof relation.

## Proof Obligations

1. **Formal rule specification.** Write `D_SJAS` as a mathematical relation,
   not as "whatever the implementation accepts."

2. **Well-founded recursive proof predicate.** Define a measure for nested
   `tableau-proof/3` and `subst-prf/4` calls, most likely over decoded proof-code
   byte payloads plus fuel as an implementation bound, so recursive closure is
   not circular.

3. **System-code soundness.** Prove every axiom/reflected/profile fact used by
   `D_SJAS` is reconstructed from `S`, not from host registries or generated
   side facts.

4. **Macro or primitive accounting.** For each non-Willard rule, choose one:
   primitive rule of `D_SJAS`, or bounded macro expansion into more elementary
   tableau/arithmetic rules.

5. **Size lower bound.** Define exactly what proof object the `5J` measure ranges
   over. Bare citations require either formula-bearing replacement or combined
   `(S,F,P)` proof-object accounting.

6. **Literature applicability.** Prove or cite why Willard's self-verification
   argument applies to `IS#_{D_SJAS}(beta)`. If this cannot be shown, Path B
   becomes a Track 2c result about a different deductive apparatus, not a proof
   of correspondence to literal Willard `D`.

7. **Completeness.** Show every `D_SJAS` proof object has an accepted Proflog
   certificate and every accepted certificate decodes to a `D_SJAS` proof.

## First Implementation Tasks

- Add a `D_SJAS` rule inventory table keyed to
  `sjas-structural-proof-check-state-decodedo` branch lines.
- Add executable classification of each branch as base tableau, bookkeeping,
  equality, arithmetic, axiom membership, reflected call, recursive proof, or
  substitution proof.
- Decide the `sjas-axiom` repair: exclude it from the size theorem, replace it
  with a formula-bearing axiom leaf, or count the combined proof object.
- Add tests that force large axiom citations and reflected-call bodies through
  the chosen size-accounting rule.

## Status

Pursued to a candidate apparatus and obligation list. Not yet complete: the
apparatus has not been proven literature-admissible, and the `sjas-axiom`
proof-size repair has not been chosen.
