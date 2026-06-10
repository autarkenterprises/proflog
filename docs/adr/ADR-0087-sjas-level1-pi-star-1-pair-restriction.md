# ADR-0087: SJAS Level-1 Pi-star-1 Pair Restriction And Basis Classification

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- AAR: [AAR-0087](../aar/AAR-0087-sjas-level1-pi-star-1-pair-restriction.md)

## Context

A literature audit of the SJAS implementation against the local Willard
corpus found one semantic deviation and two supporting classification gaps in
the Level-1 `IS#_D(beta)` line. All three are ADR-0073 Track 1 obligations:
they concern accurate executable formation of the literature axiom basis and
SelfCons sentence, not a deliberately modified Track 2 apparatus.

1. **The Level-1 Group-3 pair restriction is missing.** Willard 2013
   (`sjas/nachlass/papers/willard2013_significance_self_justifying_axiom_systems_arxiv_1307.0150.pdf`,
   Section 4, sentence (7)) defines the `ISD(A)` Group-3 axiom as

   ```text
   forall x forall y forall p forall q
     not [ Pair(x,y) /\ Prf(x,p) /\ Prf(y,q) ]
   ```

   "where `Pair(x,y)` is a `Delta*0` formula indicating `x` is the Godel
   number of a `Pi*1` sentence and `y` represents `x`'s negation."
   Definition 5.1 keeps this Group-3 for the finite `IS#_D(beta)` variant,
   changing only the "I am" fragment to reflect the finite Group-2. The
   implemented `selfcons1-formula` and the profile's
   `level1-selfcons-internal-formula` template instead quantify over **all**
   complementary code pairs:

   ```text
   forall x y p q .
     not neg-pair(x,y) \/ not subst-prf(s,g,x,p) \/ not subst-prf(s,g,y,q)
   ```

   Without the `Pi*1` restriction on `x`, the generated axiom asserts full
   consistency of the system under `D`, which is strictly stronger than
   Willard's Level-1 consistency. The distinction is not cosmetic: the
   Level-1 restriction is part of why Theorem 4.1/5.2 consistency
   preservation holds for tableau-style `D`, and the stronger sentence is
   not licensed by the cited theorems.

2. **The `Delta*0` classifiers are not closed under `not` and `implies`.**
   Both connectives are first-class formula-code tags (`formula-not-tag` 9,
   `formula-implies-tag` 10), and `clause->formula` builds reflected Group-2b
   formulas as `forall params (implies body head)`. The literature's
   `Delta*0` class is closed under propositional connectives. The host
   classifier `delta-star-0?` and the relational
   `sjas-delta-star-0-formulao` both reject `not` and `implies` outright, so
   no Group-2b clause formula could ever classify as `Pi*1`.

3. **The builder never validates the reflected basis.** Definition 5.1
   requires `beta` to be a finite set of axioms with `Pi*1` encodings, and
   the ADR-0058 design note records that reflected Group-2b clauses play the
   Group-2 role "subject to the same truth/formula-class restrictions".
   `proflog.willard-sjas/system` accepts arbitrary formulas silently; the
   `pi-star-1?` classifier exists and is tested but has no caller.

A related naming drift is corrected in documentation: the ordinary-tableau
`:willard-sjas-tableau0` instance is the finite-basis IS(A)-style system of
Willard 2001 (Group-3 target `0 = 1`), while `IS#_D(beta)` proper, per
Definition 5.1, carries the Level-1 Group-3 and corresponds to
`:willard-sjas-level1`. ADR-0058 drew this distinction correctly; later
documents (the arithmeticization specification and the Track 1 audits) called
the tableau0 MVP "the concrete `IS#_D(beta)` instance" without the
qualification. Records that reuse that phrase should be read with, and new
records must use, the profile-qualified naming.

## Decision

1. **Restrict the Level-1 pair.** Add `not pi-star-1-code(x)` as the first
   disjunct of the Level-1 Group-3 matrix, in both the builder
   (`selfcons1-formula`) and the profile reconstruction template
   (`level1-selfcons-internal-formula`, shared by the proof-bearing and
   proof-free Group-3 relations). This encodes Willard's single `Delta*0`
   `Pair(x,y)` as the conjunction of the two existing reserved vocabulary
   atoms `pi-star-1-code(x)` and `neg-pair(x,y)`; Willard 2013 presents (7)
   as "one encoding" of the Group-3 declaration, so the decomposition into
   two object atoms is a permitted encoding choice and is recorded here.
   The skeleton/diagonal fixed-point mechanism (ADR-0065) is unchanged; the
   skeleton simply carries the additional literal.

2. **Close `Delta*0` under `not` and `implies`.** Extend `delta-star-0?`
   (host, source-boundary classification) and `sjas-delta-star-0-formulao`
   (object-language relation) with structurally recursive `not` and
   `implies` cases, matching the formula-code grammar and the literature
   class.

3. **Validate the reflected basis at build time.** `system` rejects, with a
   diagnostic `ex-info` naming the group and the offending canonical
   formula, any `beta` member or reflected Group-2b clause formula that is
   not `Pi*1`-encodable, where `Pi*1`-encodable means `delta-star-0?` (a
   degenerate universal closure) or `pi-star-1?`. External clauses are not
   reflected and are not validated. Truth of `beta` in the standard model
   remains, as before, Willard's external consistency-preservation premise
   and is not decided by the builder.

## Consequences

- The generated Level-1 Group-3 formula, its code, and the embedded skeleton
  code change. System codes do not change: the system descriptor encodes
  profile, beta, and reflected source, and Group-3 is reconstructed from it.
- The Level-1 demonstration no longer claims a stronger-than-literature
  consistency axiom; the artifact's "programming-language analogue" of
  `IS#_D(beta)` asserts the same Level-1 sentence the literature licenses.
- Reflected-basis validation makes the Definition 5.1 precondition a
  build-time error instead of a silent semantic drift; programs in SJAS-lang
  are now syntactically held to the class discipline their self-consistency
  axiom presupposes.
- Existing systems in tests and examples (beta `1 = 1`, reflected
  `demo(x) :- x = 1`) remain buildable because the classifier extension
  makes their canonical forms classify.

## Test Obligations

Red before implementation, then green; focused selectors first, then both
broad gates.

- `sjas-level1-group-three-restricts-pair-to-pi-star-1`: the Level-1
  Group-3 formula and its skeleton must contain the `pi-star-1-code`
  restriction alongside `neg-pair` and `subst-prf`.
- `sjas-formula-classifiers-close-delta-star-0-under-connectives`: host
  `delta-star-0?`/`pi-star-1?` accept `not`/`implies` matrices; unbounded
  quantifiers under the new connectives still reject.
- `sjas-syntax-class-predicates-accept-implies-codes`: the object-language
  `delta-star-0-code` predicate closes on an implies-bearing formula code.
- `sjas-system-rejects-non-pi-star-1-reflected-basis`: a `beta` member or a
  reflected clause whose formula is not `Pi*1`-encodable is rejected with a
  diagnostic error; the worked-example shapes still build.
- Existing Level-1 regressions must pass through the revised shape:
  `sjas-level1-group-three-uses-substitution-proof-vocabulary`,
  `sjas-level1-group-three-uses-selfcons-skeleton-code`,
  `sjas-tableau-proof-cites-level1-group-three-from-system-code`, and the
  slow `subst-prf` fixed-point certificate selectors.
- `lein test-proflog-fast` and `lein test-proflog-extended`.

## Exit Criteria

- Both Group-3 template sites (builder and profile) emit the restricted
  matrix and agree, as witnessed by the citation selector passing through
  profile reconstruction.
- Classifier closure and basis validation are in place with positive and
  negative tests.
- The AAR records the revised Level-1 Group-3 shape and any timing change in
  the affected selectors, and the documentation naming clarification is
  recorded in the ADR/AAR indexes and LOG.
