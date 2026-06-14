# ADR-0108: D_SJAS Quantitative EA-Stability

- Status: completed
- Date: 2026-06-14
- Branch: `adr-0108-dsjas-ea-stability`
- AAR: [AAR-0108](../aar/AAR-0108-dsjas-quantitative-ea-stability.md)

## Context

ADR-0104 completed the selected-apparatus correspondence program for
`D_SJAS`. The next question is whether that selected apparatus has the
quantitative stability property needed by Willard's Level-1 self-justification
argument.

Willard's definitions are:

- A-stability: if a `Pi_1` theorem has proof length `Log(p) <= #(theta)+1`,
  then it is `Good(1/2 #(theta))`.
- E-stability: if a `Sigma_1` theorem has proof length
  `Log(p) <= #(theta)+1`, then it is
  `Good(1/2 floor(Log(p)) - 1)`.
- EA-stability: both A-stability and E-stability.

The ADR-0102 `sjas-axiom` counterexample still matters. If `Log(p)` is read as
the length of bare public proof-code `P`, the theorem is false: the fixed
`sjas-axiom` proof marker is 18 bits and can cite formulas whose payload is
arbitrarily large through `S` and `F`. ADR-0104 therefore selected a repaired
Track 2c proof-object measure for `D_SJAS`: structural proof trees are measured
by proof-code bytes, while citation leaves are measured by the combined
inspectable tuple `(S,F,P)`.

## Decision

Prove quantitative EA-stability for the selected `D_SJAS` measure:

```text
Log_D_SJAS(p) =
  Log(P)       for formula-bearing structural proof trees
  Log(S,F,P)   for sjas-axiom citation objects
```

This is the theorem used for `IS#_{D_SJAS}(beta)`. The proof-code-only theorem is
explicitly rejected.

With `H = #(theta)` and `L = Log_D_SJAS(p)`, the quantitative constants are:

```text
A-stability: sigma = 1, tau = 1, lambda = 1/2, mu = 0
E-stability: sigma = 1, tau = 1, lambda = 1/2, mu = -1
```

Equivalently:

```text
Pi_1:    L <= H+1  implies  Good(H/2)
Sigma_1: L <= H+1  implies  Good(floor(L)/2 - 1)
```

## Proof

The proof is a direct adaptation of Willard's Appendix D proof of Theorem D.4,
with `D_SJAS` replacing ordinary semantic tableaux and `Log_D_SJAS` replacing
bare proof-code length.

**Lemma 1: Size to U-height.** ADR-0104 proves the selected proof objects expose
the formula/function-symbol payload required by the conventional tableaux
encoding condition. Formula-bearing structural proof trees satisfy a stronger
bound than `5J`: if there are `N` proof nodes and `J` application/function
occurrences, the measured proof code has at least `24N + 36J` bits. Combined
citation objects expose at least `18J` bits of cited formula payload. Both
dominate Willard's conservative `5J` threshold.

**Lemma 2: D_SJAS Normed(a,b) open branch.** Willard's Fact D.3 extends to the
selected `D_SJAS` rules. The base tableau and quantifier families are literal
semantic-tableau rules. Branch bookkeeping and truth normalization erase to the
same formula branch. Equality/disequality is branch-local first-order equality
over decoded finite terms. Arithmetic/profile closure, syntax checks, formula
class checks, and proof-predicate atoms are bounded `Delta_0` relations over
finite code payloads. Axiom membership is an ordinary axiom leaf once `(S,F,P)`
is measured. Reflected calls are finite macro expansions from decoded
system-code. Recursive `tableau-proof/3` and `subst-prf/4` leaves use the
finite acyclic least-fixed-point semantics proved in ADR-0104. Therefore a
`D_SJAS` deduction tree over a `Normed(a,b)` basis with code below the
generalized `(a/b)^4` threshold has a contradiction-free branch.

**A-stability.** Suppose not. Then some r.e. `Pi_1` view `theta` has a
`D_SJAS` proof object `p` of a `Pi_1` theorem `Upsilon` with
`L <= #(theta)+1`, while `Upsilon` fails `Good(1/2 #(theta))`. Let
`Reverse(Upsilon)` be the prenex negation of `Upsilon`, a `Sigma_1` sentence.
Because `Upsilon` fails `Good(1/2 #(theta))`, `Reverse(Upsilon)` satisfies the
corresponding bounded-goodness condition. Form
`Z = theta union B union {Reverse(Upsilon)}`. The axioms of `Z` are
`Normed(2^H, 2^(H/2))` up to Willard's harmless finite small-proof cases.
The inequality `L <= H+1` and Lemma 1 put `p` below Lemma 2's open-branch
threshold. But `p` was a closed proof of `Upsilon`, so every branch must close.
Contradiction. Thus the A-stability clause holds.

**E-stability.** Suppose not. Then some r.e. `Pi_1` view `theta` has a
`D_SJAS` proof object `p` of a `Sigma_1` theorem `Upsilon` with
`L <= #(theta)+1`, while `Upsilon` fails
`Good(1/2 floor(L)-1)`. Then `Reverse(Upsilon)` is a `Pi_1` sentence satisfying
that bounded goodness horizon. Form
`Z = theta union B union {Reverse(Upsilon)}`. The `theta` and `B` axioms provide
the first norm component, and `Reverse(Upsilon)` provides the needed bounded
`Pi_1` component at the proof-length horizon. Lemma 1 again puts `p` below the
generalized open-branch threshold, so Lemma 2 gives a contradiction-free branch.
This contradicts the assumption that `p` is a closed proof. Thus the
E-stability clause holds.

The two clauses prove quantitative EA-stability for `D_SJAS` under
`Log_D_SJAS`. By Willard's Theorem 5.9, the Level-1 `SelfCons` construction for
`IS#_{D_SJAS}(beta)` has the required consistency-preservation consequence under
the recorded standing assumptions.

## Consequences

- The positive theorem is not a proof of the refuted proof-code-only statement.
- Future code must preserve the selected `Log_D_SJAS` measure. In particular,
  citation payloads must remain inspectable and counted.
- Proof-search optimizations do not affect this ADR unless they change the
  selected rule families, proof-object shape, code injectivity, or finite
  acyclic recursive semantics.

## Test Obligations

- Add a red test requiring an executable quantitative EA-stability audit.
- Require the audit to distinguish proof-code-only refutation from the selected
  combined-measure theorem.
- Require the audit to expose the A/E constants.
- Require every selected `D_SJAS` rule family to have a bounded-satisfaction
  preservation clause.
- Keep the ADR-0104 Track 2c correspondence tests green.

## Exit Criteria

- The quantitative theorem and proof-code-only refutation are recorded in an
  executable audit.
- The ADR records the theorem over `Log_D_SJAS` and proves the A/E clauses.
- Focused correspondence tests pass.
- Fast and extended regression gates pass before commit.
