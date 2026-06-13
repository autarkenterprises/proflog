# ADR-0100: SJAS Tableau Correspondence Proof (Track 2b, First Fragment)

- Status: accepted
- Date: 2026-06-13
- Branch: `adr-0100-sjas-correspondence-proof`
- AAR: [AAR-0100](../aar/AAR-0100-sjas-correspondence-proof.md)

## Context

ADR-0073 Track 2b asks for a *proof-object correspondence* between Proflog proof
acceptance and Willard's semantic-tableau proof predicate, to the standard set
in the [correspondence proof criteria](../log/2026-05-26-sjas-correspondence-proof-criteria.md)
(9 criteria). That note recorded Track 2b as *not complete*, blocked on: a
common formal semantics; the equality / procedure-call / quantifier / profile
constructors not yet discharged; skeletal `univ`/`witness` certificates; and an
independent SJAS-side tableau semantics.

Track 2a has since discharged the constructor blockers. ADR-0096 fixed the
**first correspondence fragment** (formula-bearing structural tableau
certificates, which carry no proof-symbol tags, plus the bare `sjas-axiom`
citation); ADR-0097 audits its tree shape and proof-size; and
[ADR-0098](ADR-0098-sjas-equality-fragment-reachability.md) /
[ADR-0099](ADR-0099-sjas-track2a-completion.md) proved the equality, procedure-
call, and quantifier constructors **unreachable** in that fragment (criterion 1,
route 3: "prove it is unreachable in the covered domain"). This enables the
criteria doc's route 1: *prove the biconditional over a sharply bounded fragment
that excludes all unresolved/unencodable constructors.*

## Scope (honest and bounded)

- **Covered domain (criterion 1):** the first correspondence fragment —
  formula-bearing structural tableau certificates and the bare `sjas-axiom`
  citation — over both `:willard-sjas-tableau0` and `:willard-sjas-level1`
  profiles and the compact base-64 code format. Every other proof-certificate
  constructor is **out of domain by Track 2a unreachability**, not by silent
  omission. U-Grounding code format and full skeletal-tag admission are *not*
  claimed.
- **Beta validity is a stated trust boundary** (criterion 1): Willard's `D`
  takes the axiom system α as given; the correspondence likewise takes beta
  membership / formula-class validity as the axiom basis, not a discharged
  obligation (ADR-0072).

## Decision

1. **Medium (criterion 2):** direct structural semantics by direct examination —
   the route the criteria doc explicitly permits over a proof assistant. State
   Willard's deduction method `D` precisely from the source
   (willard2001 §"semantic tableaux proof", 8 rules + closure + prenex* root;
   the Conventional Tableaux Encoding Requirement for size), define the Proflog
   side as the inductive structural checker relation
   `sjas-structural-proof-check-state-decodedo`, and match every checker clause
   to a `D` rule. This is **not** machine-checked; its soundness rests on the
   faithfulness of the `D` transcription (verified against the extracted source)
   and the per-rule matching.
2. **Proof artifact:** a correspondence-proof document containing the theorem,
   the per-rule correspondence table, the soundness and completeness arguments
   by structural induction, the anti-compression (5J) lemma, the irrelevance
   lemmas, and the trust boundary.
3. **Operational tests (criterion 9):** per-rule correspondence witnesses (each
   `D` rule exercised by an accepted formula-bearing, in-fragment certificate),
   the existing positive/negative acceptance and rejection selectors, and a
   proof-size regression — consolidated as the Track 2b operational suite.

## Theorem (target)

For every covered system code `S`, theorem code `F`, and first-fragment proof
certificate `P`:

```
ProflogAccepts(P, S, F)  iff  SemPrf_D(decode(P), S, F)
```

where `SemPrf_D` is Willard's semantic-tableau proof predicate for the selected
`D`, `decode(P)` is the formula-bearing tableau tree the certificate encodes,
and the encoding of `P` satisfies the Conventional Tableaux Encoding Requirement
(≥ 5J bits for J function-symbol occurrences).

## Test Obligations

- Per-rule witnesses green; existing acceptance/rejection selectors and both
  broad gates stay green; a proof-size regression shows a fixed-size certificate
  cannot validate an arbitrarily large formula-bearing tree.

## Exit Criteria

- The bounded biconditional is proved by direct examination with every Proflog
  checker clause matched to a `D` rule (or to a Track 2a unreachability result),
  the anti-compression lemma stated, irrelevance lemmas recorded, and the trust
  boundary named. Residual (full-domain admission, U-Grounding, machine-checked
  mechanization) is recorded as follow-up in AAR-0100. This completes Track 2b
  **over the first fragment**; it does not claim the unbounded-domain theorem.
