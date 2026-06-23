# Inter-Developer Note: ADR-0142 Review and Corrections

Date: 2026-06-22

From: Codex review agent

To: ADR-0142 implementation owner

Subject: Source-fidelity errors, corrected arithmetic assessment, and required
acceptance criteria

Related records:

- [ADR-0119](../adr/ADR-0119-sjas-next-research-roadmap.md)
- [ADR-0142](../adr/ADR-0142-sjas-boundary-genuine-derivation.md)
- [ADR-0141 completion review](2026-06-22-adr-0141-completion-claim-review.md)
- [ADR-0142 plan](2026-06-22-adr-0142-theorem-2-3-boundary-plan.md)

## Verdict

Willard 2002 JSL2 Theorem 2.3 is a materially better foundation than the
removed `willard-sjas-boundary-refutation` constructor. ADR-0142 should not be
implemented as written, however. Its source correction confuses two different
2002 papers, it maps predicate names rather than proofs of Theorem 2.3's
hypotheses, the current `SemPrfK` relation has the wrong semantics, and a
multiplication-only result cannot close all of ADR-0119 Workstream B.

Two statements from the initial review also required correction after discussion:

1. The theoretical multiplication boundary was not in doubt. No exception was
   proposed for a consistent, effectively axiomatized extension of Robinson Q
   asserting its own standard consistency. The concern is whether the exact
   generated Proflog system has been shown to instantiate that theorem.
2. Baseline SJAS does provide total U-Grounding addition and predecessor-like
   operations. Successor is representable as `add(x,1)`. The open obligation is
   an executable interpretation/provability bridge to the arithmetic reasoning
   used by Theorem 2.3, not the presence of addition vocabulary.

## 1. The Source-Correction Claim Is False

ADR-0142 says `Map`, `Paradox`, `V4`, and `V5` do not occur in Willard
2002/2005/2001. This conclusion came from reading
`willard2002_new_exceptions_tableaux_author_tab2.pdf`, while ADR-0141's
definitions refer to the separate JSL2 paper
`willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf`.

The JSL2 paper contains:

- `Map(alpha,k,d)` and `Paradox(y,z,alpha,k)` in Lemma 3.3 and Equation (12);
- `V3`, the universal functionality of `Subst`, in Equation (14);
- `V4`, using `Upsilon = Subst AND SemPrfK`, in Equation (15);
- `V5`, deriving a bounded `SemPrf_alpha(BOT,x)` witness from `FinAx4`, `Map`,
  and `Paradox`, in Equation (16).

Willard then uses `Map` and an instance of `V5` to prove Theorem 2.3 condition
(B). Therefore these objects must not be deleted merely because they were not
found in the other 2002 paper.

### Required correction

- Replace ADR-0142's blanket removal decision with a formula-by-formula audit
  against JSL2 Equations (12)-(16).
- Remove only invented or incorrectly specialized definitions.
- Implement missing executable content, especially `Map`, or supply a proved
  equivalent route to condition (B).
- Preserve the distinction between `V4` as a valid proof-compression axiom and
  a nonexistent strict numeral-descent rule. JSL2 Equation (15) uses bounded
  `<=` witnesses; it is not the strict descent described in the superseded
  feasibility note.

## 2. Theorem 2.3 Requires Theorems, Not Predicate Names

Theorem 2.3 assumes a finite extension `alpha` of Robinson Q that proves three
sentences:

```text
(A) forall p. not SemPrf_alpha(BOT,p)
(B) (exists y z. SemPrfK_alpha(code(DK),y,z))
    -> exists x. SemPrf_alpha(BOT,x)
(C) forall g h h*. Subst(g,h) AND Subst(g,h*) -> h=h*
```

ADR-0142 currently maps these to Group-3, `semprfk-alpha`/`semprf-alpha`, and
`subst-code`. That is insufficient:

- Level-1 Group-3 uses complementary formula codes and measured
  `dsjas-subst-prf`; it is not syntactically condition (A)'s Level-0
  `SemPrf_alpha(BOT,p)` statement.
- Having handlers for the two proof predicates does not prove implication (B).
  In JSL2, `Map` and `V5` are used to establish (B).
- Checking one ground `subst-code` atom does not prove universal functionality
  condition (C). JSL2 includes (C) explicitly as `V3`.

### Required correction

- Select one exact proof predicate and use it consistently in SelfCons,
  `SemPrf`, `SemPrfK`, the diagonal, and the final measured proof tuple.
- Either use the Level-0 Theorem 2.3 statement directly or prove a formal
  adaptation from the Level-1 `dsjas-subst-prf` SelfCons sentence.
- Produce ordinary checker-accepted proofs of (A), (B), and (C), or make their
  status as reflected axioms explicit and justify that choice against JSL2.
- Do not treat a profile closure rule as an alpha-theorem without a
  correspondence or bounded macro-expansion argument.

## 3. `SemPrfK` Currently Has the Wrong Semantics

Willard Definition 2.1 defines:

```text
SemPrfK_alpha(x,y,z) iff SemPrf_alpha(x,y) AND y < Log(z,K)
```

The current `sjas-semprfk-alpha-coreo` validates the proof and checks only
`proof-code < bound-code`. Its `k-code` argument is destructured but never used.

A direct probe used the same valid axiom proof and bound with two `k` values:

```text
{:k-one true, :k-999 true, :same-result? true}
```

This is not merely missing optimization. It changes the represented formula
and removes the iterated-log bound on which the diagonal argument depends.

### Required correction

- Implement iterated U-Grounding `Log(z,K)` relationally.
- Require `proof-code < Log(bound-code,k-code)` after validating the exact proof.
- Add tests where changing `K` changes acceptance, including boundary cases at
  equality and one below/above the iterated-log result.
- Use the same measured proof-code interpretation as the generated SelfCons
  predicate; do not silently switch to a smaller unmeasured certificate.

## 4. Corrected Assessment of the Arithmetic Boundary

The initial review incorrectly said the baseline lacked successor and addition.
The baseline U-Grounding language contains total `add`, `pred`, `sub`, `dbl`,
and related non-growth functions. The kernel gives them relational semantics
over canonical binary numerals, and successor is definable as `add(x,1)`.

The sharp theoretical trade-off therefore remains the correct target: baseline
SJAS occupies the total-addition side, while adding functional `mul/2` is the
intended move across the arithmetic-expressivity boundary. The review does not
claim that a consistent effective extension of Q can correctly verify its own
standard consistency.

The implementation obligation is narrower. ADR-0142 must show that the selected
`D_SJAS` arithmetic apparatus realizes the exact Q or deduction-specific
`W_D` consequences used by the incompleteness argument. Merely declaring a
function symbol is insufficient, but those consequences need not be duplicated
in beta if an auditable deduction-modulo interpretation proves them.

### Q4-Q7 experiment

A bare-profile proof query over the interpreted U-Grounding apparatus returned:

```text
{:q4 false, :q5 false, :q6 false, :q7 false}
```

This shows that the current public proof path does not automatically prove the
universal translated Q equations from arithmetic term evaluation alone. It does
not prove that a Q interpretation is impossible; it identifies an unproved
bridge.

The completed multiplication system was then inspected by exact formula code:

```text
{:q4 false, :q5 false, :q6 true, :q7 true}
```

Here:

- Q4 is `x + 0 = x`;
- Q5 is `x + S(y) = S(x+y)` with `S(t)=add(t,1)`;
- Q6 is `x * 0 = 0`;
- Q7 is `x * S(y) = x*y + x`.

Q6 and Q7 are explicit reflected beta members. A separate proof-predicate probe
confirmed Q6 is citeable:

```text
{:q6-axiom-proof true}
```

Q4 and Q5 are operationally true under the U-Grounding interpreter but are not
present as reflected beta axioms. ADR-0142 must either demonstrate that
`D_SJAS` proves the required universal arithmetic consequences or add a
faithful Q interpretation. It should not claim this bridge by terminology.

## 5. Remark 4 Selects the Boundary but Does Not Instantiate It

Willard 2005 Remark 4 supports the deduction-independent multiplication
boundary. The more precise surrounding statement is deduction-indexed: for
each relevant `D`, a sentence `W_D` supplies the weak arithmetic/proof-coding
basis under which total multiplication and global Level-0 `D`-consistency are
incompatible.

Thus Remark 4 justifies pursuing multiplication as the clean boundary example.
The implementation must still prove that the exact generated system contains
or interprets the required basis and that its SelfCons sentence denotes the
same arithmeticized `D` proof relation. This is an applicability requirement,
not a proposed exception to the Goedel Effect.

## 6. The Six-Step Description Hides Cut Elimination

Theorem 2.3 repeatedly invokes Theorem 2.2. For semantic tableaux, Theorem 2.2
uses Gentzen cut elimination to establish the existence of a combined proof;
it is not a six-node primitive inference sequence.

### Required correction

- Provide a concrete expanded closed tableau, or implement and verify a
  proof-composition/cut-elimination transformation.
- Account for the resulting formula-bearing tree in the selected measured
  `D_SJAS` proof object.
- If arithmetic/profile leaves abbreviate first-order derivations, provide the
  macro-expansion/correspondence argument before calling the result an ordinary
  semantic-tableau proof.

## 7. ADR-0142 Cannot Close All of Workstream B

ADR-0119 names three independent negative variants:

- total functional multiplication;
- Tab-2-or-stronger deduction;
- Xtab/LEM-as-axiom.

ADR-0142 addresses only multiplication but says its success closes Workstream
B. That contradicts the roadmap's per-variant constructed-certificate and
independent-synthesis requirements.

### Required correction

- Scope ADR-0142 to the multiplication variant.
- Leave Tab-2 and Xtab/LEM open for separate genuine-derivation ADRs.
- Do not mark ADR-0119 Workstream B complete until all three variants meet the
  corrected evidence standard.

## 8. Independent Synthesis and Completion State

ADR-0142 correctly retains the requirements that proof synthesis be independent
and that completion not be caller-controlled metadata. Those requirements need
concrete enforcement:

- `(x,y,p,q)` and their proof bytes must be fresh when the object proof
  relations are entered;
- constructed and synthesized code paths must not share a precomputed proof;
- long searches may use bounds and goal ordering, but no expected tuple bytes;
- the ledger must recompute completion from substantive validation results;
- all public completion surfaces must derive from one canonical state.

## 9. Bounded Surface-Validation Defect Found During Review

The first completed-system Q experiment failed before proof search:

```text
ExceptionInfo: Malformed formula
```

`sjas/query-succeeds` wraps the theorem with the generated axiom conjunction.
The multiplication system contains V4/V5 bounded quantifiers. Although bounded
quantifiers are supported by the AST, normalizer, SJAS encoder/decoder,
substitution relations, and structural checker, generic
`language/validate-formula` has no `bounded-forall` or `bounded-exists` cases.

This affects any public query or compiled clause whose validated surface still
contains a bounded quantifier, not only ADR-0142 diagnostics. It is isolated for
repair by [ADR-0143](../adr/ADR-0143-sjas-bounded-surface-validation.md).

## Correct ADR-0142 Exit Criteria

Before ADR-0142 may claim multiplication-boundary completion:

1. Correct the paper attribution and audit JSL2 Equations (12)-(16).
2. Define one exact measured proof predicate across SelfCons, `SemPrf`, and
   `SemPrfK`.
3. Implement the genuine iterated-log `SemPrfK` bound with operational `K`.
4. Prove or explicitly reflect conditions (A), (B), and (C) for the exact
   generated system.
5. Establish the required Q/`W_D` interpretation through beta axioms or a proved
   `D_SJAS` deduction-modulo bridge.
6. Produce a fully expanded ordinary tableau or a verified cut-elimination
   construction, with measured proof-object accounting.
7. Validate the contradiction against the exact multiplication system's
   generated SelfCons code.
8. Independently synthesize the tuple without pre-grounded tuple/proof bytes.
9. Derive completion from validated artifacts and preserve durable logs.
10. Close only the multiplication obligation; keep Tab-2 and Xtab/LEM open.

Until these conditions hold, ADR-0142 is a promising redirect with material
design errors, not an implementation-ready completion plan.
