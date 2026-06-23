# ADR-0142: SJAS Multiplication-Boundary Derivation via Willard Theorem 2.3

- Status: accepted (revised 2026-06-22 after review)
- Date: 2026-06-22
- Branch: `adr-0142-sjas-boundary-genuine-derivation`
- AAR: pending

## Revision note (2026-06-22)

This ADR was revised after
[the Codex review](../interdev/2026-06-22-adr-0142-review-and-corrections.md)
and [the owner's reply](../interdev/2026-06-22-adr-0142-review-reply.md).

The first draft claimed the `V3/V4/V5/Map/Paradox/FinAx4` apparatus was
misattributed and should be removed. **That claim was wrong and is withdrawn.**
The apparatus is genuine Willard 2002 JSL2 §3.2 (Equations 12-16); the first
draft read the wrong 2002 paper (tab2) and stopped before JSL2 §3.2. Theorem 2.3
is the *conclusion* those axioms feed, not a replacement for them. The apparatus
is therefore retained, audited against JSL2, and the one genuinely-missing piece
(`Map`) is implemented. The corrections below also fix the `SemPrfK` semantics,
confront the cut-elimination obligation, add the arithmetic-interpretation
bridge, and scope this ADR to the multiplication variant only.

## Context

ADR-0141's completion claim for ADR-0119 Workstream B was retracted (commit
`b16ed5b` superseded by `379031c`) because the boundary contradiction was
accepted by a trusted `willard-sjas-boundary-refutation` constructor routed
around the ordinary checker, the synthesis was host-seeded, and the ledger
trusted forgeable metadata. **That retraction stands and is unaffected by this
ADR**: it concerned the trusted constructor, not the source axioms.

The faithful, object-level inconsistency result for a Q-extension that recognizes
its own tableau consistency is Willard 2002 JSL2
(`willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf`), **Theorem 2.3**:
a finite extension `alpha` of Robinson Q is inconsistent if it proves

```text
(A) forall p. not SemPrf_alpha(BOT, p)                       -- tableau-consistency (Eq 3)
(B) (exists y z. SemPrfk_alpha(code(DK), y, z))
        -> exists x. SemPrf_alpha(BOT, x)                    -- bounded-proof-of-diagonal => BOT
(C) forall g h h*. Subst(g,h) AND Subst(g,h*) -> h = h*      -- Subst single-valued
```

with diagonal `DK = Gamma(nbar)`, `Gamma(g) = forall h y z. Subst(g,h) =>
not SemPrfk_alpha(h,y,z)` (Eq 4), `nbar = code(Gamma(g))`. JSL2 §3.2 then defines
the concrete system `FinAx5(alpha) = Q + V1..V5` that proves (A), (B), (C)
(Theorem 3.5, lines 513-549). The boundary apparatus already in
`proflog.sjas-boundary-axioms` corresponds to that system:

| JSL2 object | Equation | Role | Kernel status |
|---|---|---|---|
| `Map(alpha,k,d)` | Lemma 3.3 | diagonal proof-code locator | **missing** (no kernel handler) |
| `Paradox(y,z,alpha,k) = exists d<z {Map AND SemPrfk}` | Eq (12) | antecedent of V5 | depends on `Map` |
| `V3 = forall g h h*. Subst(g,h) AND Subst(g,h*) => h=h*` | Eq (14) | **is** condition (C) | present as axiom |
| `V4` with `Upsilon = Subst AND SemPrfk` | Eq (15) | proof-compression axiom (provable from Q; bounded `<=`) | present as axiom |
| `V5 = [FinAx4 AND k>=alpha AND Paradox] => exists x<z SemPrf(BOT,x)` | Eq (16) | establishes condition (B) | present as axiom; route needs `Map` |
| `Subst(g,h)` | §2 / Eq 4 | Goedel diagonal | present (`subst-code`, faithful) |

`V4` is **not** a strict numeral-descent rule: it uses bounded `<=`, is provable
from Q, and is included only as a redundant proof-compression axiom (JSL2 line
478-479; it appears *inside* the bounded proof object, line 733). The
inconsistency closure is Theorem 2.3's diagonal clash, established through
Theorem 2.2.

**Operationalization.** Self-justification is the tableau system's *failure to
close* a SelfCons-refutation (SelfCons is added as an axiom, so it is trivially
"proved"; the content is non-refutability); a boundary *failure* is the
*appearance* of a closing tableau for that refutation. Willard 2005 jsl5 Remark 4
(lines 977-984) shows multiplication-total destroys the theta-compactification
property underwriting self-justification "under any possible deduction method D"
— semantic tableaux included. Remark 4 *selects* multiplication as the clean
boundary; it does not by itself *instantiate* the result for our generated
system, which remains an obligation. Non-closure under bounded search is not
evidence of consistency: the bottom-proof is large and search-inaccessible and
must be constructed.

## Decision

Pursue the genuine, non-circular completion of the **multiplication** variant of
ADR-0119 Workstream B by mechanizing JSL2 Theorem 2.3 over the exact generated
multiplication-total system.

**Goal.** Exhibit a proof object, accepted step-by-step by the existing
`dsjas-subst-prf` / structural tableau checker, that closes the SelfCons
refutation (derives `BOT = 0=1`) from `{Q + V1..V5 + multiplication-total +
SelfCons axiom}`, and show the analogous construction *fails to close* in the
addition-only variant — demonstrating executably that recognizing multiplication
as a total function carries the system across the boundary.

**Plan.**

1. **Audit, don't delete.** Audit `sjas-boundary-axioms` formula-by-formula
   against JSL2 Eqs (12)-(16). Keep `V3/V4/V5/Paradox/FinAx4`; remove only
   genuinely-invented or incorrectly-specialized definitions. Implement the
   missing `Map(alpha,k,d)` as a genuine checked relation (or supply a proved
   equivalent route to condition (B)).
2. **One measured proof predicate.** Use a single exact proof-code
   interpretation consistently across SelfCons, `SemPrf`, `SemPrfk`, the
   diagonal, and the final measured proof tuple.
3. **Fix `SemPrfk` semantics.** Implement `SemPrfk_alpha(x,y,z) iff
   SemPrf_alpha(x,y) AND y < Log(z,k)` (Definition 2.1) with operational `k`:
   relational iterated U-Grounding `Log(z,k)` and `proof < Log(bound, k)` after
   validating the exact proof. The current `sjas-semprfk-alpha-coreo` checks only
   `proof < bound` and ignores `k`; that is wrong.
4. **Discharge (A), (B), (C) for the exact generated system.** (C) is the V3
   axiom. (B) follows from the V5 instance plus `Map(alpha,k,code(DK))` via
   Theorem 2.2 (JSL2 footnote 1). (A) is the generated SelfCons; either use the
   Level-0 `SemPrf_alpha(BOT,p)` statement directly or prove the correspondence
   from the Level-1 measured `dsjas-subst-prf` SelfCons sentence. Prove these as
   checker-accepted derivations, or make their status as reflected axioms
   explicit and justify it against JSL2.
5. **Confront cut-elimination.** Theorem 2.3's combinations invoke Theorem 2.2,
   which for semantic tableaux is Gentzen cut-elimination / model-completeness,
   not a fixed-size inference. Provide a concrete expanded closed tableau, or
   implement and verify a proof-composition / cut-elimination transformation,
   and account for the resulting formula-bearing tree in the measured proof
   object. Profile/arithmetic leaves that abbreviate first-order derivations
   require a macro-expansion / correspondence argument before being called an
   ordinary tableau proof.
6. **Arithmetic interpretation bridge.** Show the selected `D_SJAS` apparatus
   realizes the exact Q / deduction-specific `W_D` consequences used by the
   argument — via reflected beta axioms or an auditable deduction-modulo
   interpretation. Declaring a function symbol is insufficient; e.g. the
   universal addition laws `x+0=x`, `x+S(y)=S(x+y)` are operationally true under
   the interpreter but are not currently reflected beta members.
7. **Construct + check, contrast, synthesize.** Assemble the closing tableau;
   verify each step with the ordinary checker; show non-closure (localized to the
   `SemPrfk` / `Log` bound step) in the addition-only variant; then perform
   genuinely independent synthesis (fresh `(x,y,p,q)` and proof bytes; no
   host-seeded tuple; constructed and synthesized code paths separate).

**Dependency.** This ADR depends on bounded-quantifier surface validation, which
is handled separately by **ADR-0143** (the generated multiplication system
contains V4/V5 bounded quantifiers that `language/validate-formula` must accept).
ADR-0143 is kept distinct and is not absorbed here.

**Honesty constraints.** Construct-and-check, never search-and-trust. Each
predicate on the closure path — `Map`, `semprfk-alpha` (must check a decoded
bounded proof with the real `Log` bound, not destructure/trust), `subst-code`
single-valuedness, the Q-disproof of the invalid Pi1 leaf — is audited for
genuine checking before any closure is claimed. The trusted
`willard-sjas-boundary-refutation` constructor stays removed.

## Consequences

- **Apparatus retained.** `V3/V4/V5/Map/Paradox/FinAx4`, `subst-code`,
  `semprf-alpha`, `semprfk-alpha`, `finax4`, `lt`, and the Group-3 SelfCons
  construction are kept. Only the trusted boundary-refutation constructor and any
  genuinely-invented/mis-specialized definitions found by the audit are removed.
- **Principal risks.** (a) Implementing `Map` and the genuine `Log`-bounded
  `SemPrfk` is real proof-coding work; (b) the cut-elimination obligation (step
  5) is the deepest part — a fully expanded tableau may be very large, and a
  verified composition transformation is itself substantial; (c) the
  predicate-trust audit gates any completion claim. The addition-only
  non-closure contrast is the falsifier against a vacuous closure.
- **Rejected alternatives.** Reinstating a trusted constructor (circular);
  deleting the JSL2 V-axioms (they are faithful and needed for (B)/(C));
  metalevel-only formalization (a different deliverable); blind search
  (inaccessible); a strict-descent reading of V4 (not Willard's construction).
- **Scope.** Multiplication variant only. Tab-2 and Xtab/LEM remain open for
  separate genuine-derivation ADRs. ADR-0119 Workstream B is not closed by this
  ADR alone. Workstream C remains separately reopened.

## Test Obligations

Red before implementation:

- `subst-code` computes and decodes `Subst(nbar, code(DK))` for the generated
  diagonal; condition (C) is accepted via the V3 axiom and a non-functional
  `Subst` witness is rejected.
- `Map(alpha,k,d)` accepts the genuine diagonal locator and rejects a non-locator
  `d`; `Paradox` and the V5 route establish (B) only with a valid `Map` witness.
- `SemPrfk` acceptance changes with `k`: tests at the iterated-log boundary
  (equality, one below, one above `Log(bound,k)`) pass/fail correctly, and a
  valid proof above the bound is rejected.
- The assembled Theorem 2.3 closure is accepted only through ordinary checker
  steps over the exact generated multiplication-total system; the removed
  constructor and any symbol-headed boundary certificate are rejected cleanly.
- Each hypothesis is necessary: omitting (A), (B), or (C), or using a mismatched
  system or diagonal, leaves the tableau open.
- The cut-elimination / proof-composition step is verified (expanded tableau
  checks, or the transformation is tested against its specification).
- The addition-only variant fails to close, localized to the `SemPrfk`/`Log`
  bound step.
- Fresh-variable synthesis recovers a proof tuple and rejects supplied
  tuple/byte metadata; a dataflow check fails if any tuple component is ground
  before the proof relation is entered.
- The Workstream B ledger reports the multiplication obligation complete only
  from substantive verified state; forged nested metadata leaves it incomplete.

Run focused red/green selectors first, then `lein test-proflog-fast` and
`lein test-proflog-extended` in parallel, then focused SJAS progression and
`lein test-proflog-sjas`; long-running synthesis/evidence probes last, with
durable logs to `test-runs/`.

## Exit Criteria

1. Paper attribution corrected and `sjas-boundary-axioms` audited against JSL2
   Equations (12)-(16); only invented/mis-specialized definitions removed.
2. One exact measured proof predicate used across SelfCons, `SemPrf`, `SemPrfk`,
   the diagonal, and the final tuple.
3. Genuine iterated-log `SemPrfk` bound with operational `k` implemented.
4. `Map` implemented (or a proved equivalent route to (B)); conditions (A), (B),
   (C) proved or explicitly reflected for the exact generated system, with the
   Level-0/Level-1 SelfCons correspondence settled.
5. Required Q / `W_D` interpretation established via beta axioms or a proved
   `D_SJAS` deduction-modulo bridge.
6. A fully expanded ordinary tableau or a verified cut-elimination / composition
   construction, with measured proof-object accounting.
7. The contradiction validated against the exact multiplication system's
   generated SelfCons code and reflected beta.
8. The tuple independently synthesized without pre-grounded tuple/proof bytes,
   with durable logs.
9. Completion derived from validated artifacts; all public completion surfaces
   agree.
10. Only the multiplication obligation closed; Tab-2 and Xtab/LEM left open.
    Gates (fast, extended, SJAS) green; AAR-0142 records implementation,
    red/green evidence, exact tuple codes, the decoded closing proof tree, the
    trust audit, synthesis logs, and final gates.

Until these hold, ADR-0142 is an accepted plan, not a completion claim.
