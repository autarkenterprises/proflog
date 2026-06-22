# ADR-0142: SJAS Multiplication-Boundary Derivation via Willard Theorem 2.3

- Status: accepted
- Date: 2026-06-22
- Branch: `adr-0142-sjas-boundary-genuine-derivation`
- AAR: pending

## Context

ADR-0141's completion claim for ADR-0119 Workstream B was retracted (commit
`b16ed5b` superseded by `379031c`). The interdev review
[2026-06-22-adr-0141-completion-claim-review.md](../interdev/2026-06-22-adr-0141-completion-claim-review.md)
found the boundary contradiction was accepted by a trusted
`willard-sjas-boundary-refutation` constructor routed around the ordinary
checker (circular), the "independent" synthesis was host-seeded with the exact
expected proof bytes, and the six-of-six ledger trusted caller-supplied
metadata. The soundness fix removed the trusted constructor; Workstream B is
reopened.

A subsequent source-fidelity investigation (this ADR's preparation) established
two further facts that redirect the work:

1. **The prior boundary apparatus is misattributed.** The
   `V4`/`V5`/`willard-map`/`Upsilon`/`Paradox` axioms in
   `proflog.sjas-boundary-axioms` carry "Willard 2002 Equation (12)/(15)" labels
   that do not hold. Willard 2002 tab2
   (`willard2002_new_exceptions_tableaux_author_tab2.pdf`) Eq 11-17 are the
   `Check/omega/Top/Constraint` machinery of a *consistency-preservation* proof
   (the safe side, Theorems 1-2), proved by metalevel induction (PROBE +
   Lemmas 2-3). There is no V4 descent axiom, no `Upsilon = subst-code AND
   semprfk`, no `willard-map`, no `Paradox`, and no literal V4/V5 in
   willard2002/2005/2001. The previously planned "V4 finite descent, no
   induction required" path is therefore unsound: eliminating a bounded
   existential with the tableau delta-rule yields an opaque parameter, not a
   shrinking numeral, so without a least-number principle (which SJAS lacks by
   design, and which Willard keeps at the metalevel) the descent never closes.

2. **Multiplication-total crosses the boundary under this checker's deduction.**
   The correct operationalization is: self-justification is the tableau system's
   *failure to close* trees that would refute its SelfCons axiom (SelfCons is
   added as an axiom, so it is trivially "proved"; the content is
   non-refutability); a boundary *failure* is the *appearance* of a closing
   tableau for such a refutation. Willard 2005 jsl5 Remark 4 (lines 977-984)
   shows the theta-compactification property underwriting self-justification is
   impossible with multiplication-total "under any possible deduction method D,
   whether cut-free or otherwise" — semantic tableaux included; Willard
   deliberately drops multiplication-total to keep self-justification under
   tableaux (lines 800-826). So the configured multiplication-total system is
   genuinely on the unsafe side under the semantic-tableau deduction this
   project implements, and a closing tableau for the SelfCons-refutation exists.
   The earlier feasibility note's "search rejects 1=0 in ~18 ms" is **not**
   evidence of consistency: a genuinely inconsistent system can have its only
   bottom-proof be a large, search-inaccessible Goedel-Loeb derivation that must
   be *constructed*, not searched for.

The faithful, checkable, object-level construction is Willard 2002 jsl2 (the
semantic-tableau Second Incompleteness paper, ref [68] of willard2005,
`willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf`), **Theorem 2.3**.
Its three hypotheses map directly onto genuine apparatus already in the kernel:

| Theorem 2.3 condition | Meaning | Existing genuine piece |
|---|---|---|
| (A) `forall p. not SemPrf_alpha(BOT,p)` | SelfCons / tableau-consistency axiom (Eq 3; `BOT`=code of `0=1`) | Group-3 axiom |
| (B) `(exists y z. SemPrfK_alpha(code(DK),y,z)) => exists x. SemPrf_alpha(BOT,x)` | bounded "proof" of the diagonal implies `BOT` provable | `semprfk-alpha`/`semprf-alpha` |
| (C) `forall g h h*. Subst(g,h) AND Subst(g,h*) => h=h*` | `Subst` single-valued (Delta0-valid; Q proves it) | `subst-code` |

The diagonal is `DK = Gamma(nbar)` where
`Gamma(g) = forall h y z. Subst(g,h) => not SemPrfK_alpha(h,y,z)` (Eq 4) and
`nbar = code(Gamma(g))`; `sjas-subst-code-anyo` already computes
`Subst(nbar, code(DK))`. Expressivity enters at exactly one place: `SemPrfK`
uses `y < Log(z,K)` (Definition 2.1), whose bound arithmetic (Lemmas 3.1/3.2:
Subtraction and Log as Delta0-total functions) needs multiplication-grade
growth — which the addition-only variant cannot carry.

## Decision

Adopt the following goal and pursue it as the genuine, non-circular completion
of ADR-0119 Workstream B.

**Goal.** Demonstrate the Goedel-boundary *failure* for the SJAS apparatus by
mechanizing Willard's semantic-tableau Second Incompleteness Theorem
(willard2002 jsl2, Theorem 2.3) as an **ordinary, structural-checker-verified
closed tableau** that derives `BOT` (`0 = 1`) from the multiplication-total
system together with its own SelfCons axiom — establishing executably that
recognizing multiplication as a total function carries the system across the
boundary, from self-justifying (cannot close a SelfCons-refutation) to
inconsistent (a closing tableau appears).

**Deliverable.** A proof object, accepted step-by-step by the existing
`dsjas-subst-prf` / structural tableau checker, realizing Theorem 2.3's closure
over the exact generated multiplication-total system: conditions (A), (B), (C)
plus the diagonal `DK = Gamma(nbar)`, assembled into the six steps
`A AND B => D*` ; `alpha proves Subst(nbar,code(DK))` ; `(C) pins DK == D*` ;
`alpha proves DK` ; `alpha proves not DK` ; `close`, where
`D* = forall y z. not SemPrfK_alpha(code(DK),y,z)`.

**Success criteria (all required).**

1. Every inference is accepted by the *ordinary* checker — no trusted boundary
   constructor, no special boundary route, no proof-grammar node whose
   conclusion is the contradiction.
2. The boundary *contrast* holds and localizes: the analogous construction
   fails to close in the addition-only variant, and the failure sits precisely
   at the bound-arithmetic step (`SemPrfK`'s `y < Log(z,K)`, Lemmas 3.1/3.2)
   that needs multiplication-grade growth.
3. The contradiction is derived against the exact generated SelfCons code and
   reflected beta of that same system; the diagonal is the genuine `subst-code`
   of `Gamma(g)`, computed, not asserted.
4. Independent synthesis discovers the proof tuple by running object-level
   relations with fresh variables — no host-precomputed/seeded proof bytes;
   kept on a code path separate from construction.
5. Completion is derived from substantive verified state (not caller-supplied
   metadata); all public surfaces agree; gates stay green; durable per-variant
   records (exact codes, decoded proof tree, command, timings) preserved.

**Honesty constraints.** Construct-and-check, never search-and-trust: building
the proof object and verifying it with the ordinary checker is legitimate;
concluding `BOT` because the boundary hypotheses are present is not. Each
predicate on the closure path — notably `semprfk-alpha` (must check a decoded
bounded proof, not destructure/trust) and `subst-code` single-valuedness — is
audited for genuine checking before any closure is claimed; an over-trusting
predicate re-introduces the retracted circularity one level deeper. The
misattributed V4/V5/`willard-map`/`Upsilon`/`Paradox` apparatus is removed, not
relabeled.

**Rationale.** Theorem 2.3 is the object-level result the weak system itself
carries once multiplication-total is present; it is faithful to the implemented
semantic-tableau deduction (criterion: Willard 2005 Remark 4), its three
conditions are already realized by genuine kernel predicates, and its closure is
an ordinary tableau derivation rather than a trusted rule — so it satisfies the
review's correct-implementation criteria by construction rather than by
assertion.

## Consequences

- **Removed apparatus.** The `V4`/`V5`/`willard-map`/`Upsilon`/`Paradox` axioms
  in `proflog.sjas-boundary-axioms`, the dead boundary-refutation helpers in
  `willard_sjas_profile.clj` (`sjas-boundary-refutation-proof-bytes-coreo`,
  `sjas-boundary-profile-hypotheses-coreo`, the byte-prefix routers), and the
  `boundary-refutation-proof` emitter in `willard_sjas.clj` are deleted. The
  genuine `subst-code`, `semprf-alpha`, `semprfk-alpha`, `finax4`, `lt`, and the
  Group-3 SelfCons construction are retained.
- **Principal risk.** This is the mechanization of an incompleteness proof as a
  concrete tableau; it is a multi-session effort. The hard, gating parts are
  (a) the `SemPrfK` bound arithmetic (`Log(z,K)`, Lemmas 3.1/3.2) and (b) the
  predicate-trust audit on the closure path. If `semprfk-alpha` or
  `subst-code` functionality turns out to destructure rather than check, that
  trust must be removed first or the result is circular again. The boundary
  contrast (criterion 2) is the falsifier that guards against a vacuous closure.
- **Rejected alternatives.**
  - *Keep a trusted boundary constructor* — the retracted circular approach;
    enlarges the trusted system with the conclusion under investigation.
  - *Metalevel formalization of the full generalized Second Incompleteness*
    (definable cuts / theta-compactification) — faithful but a different,
    much larger deliverable (a proof *about* the system, not a checker-accepted
    proof object).
  - *Blind proof search for the contradiction* — the bottom-proof is
    search-inaccessible by design; construction is required.
  - *Addition-total plus a stronger deduction method* — multiplication-total is
    deduction-independent (Remark 4) and faithful to the implemented tableau
    checker, so it is the cleaner boundary to exhibit.
- **Scope.** This ADR covers Workstream B only. Workstream C (pair/list
  consistency-preservation) remains separately reopened per the review.

## Test Obligations

Red before implementation:

- `subst-code` computes and decodes `Subst(nbar, code(DK))` for the generated
  diagonal `DK = Gamma(nbar)`, and condition (C) single-valuedness is accepted
  as a Delta0-valid leaf while a non-functional witness is rejected.
- `semprfk-alpha` accepts a genuine decoded bounded proof and rejects a
  non-proof / an out-of-bound proof — an explicit trust audit, not a
  destructure.
- The assembled six-step Theorem 2.3 closure is accepted only through ordinary
  checker steps over the exact generated multiplication-total system; the
  removed boundary constructor and any symbol-headed boundary certificate are
  rejected (cleanly, without throwing).
- The same construction over the addition-only variant fails to close, and the
  failure localizes to the `SemPrfK` bound-arithmetic step.
- Each Theorem 2.3 hypothesis is necessary: omitting (A), (B), or (C), or
  using a mismatched system or diagonal, leaves the tableau open.
- Fresh-variable synthesis recovers a proof tuple and rejects supplied
  tuple/byte metadata as a substitute; a dataflow check fails if any tuple
  component is ground before the proof relation is entered.
- The Workstream B ledger reports complete only from substantive verified
  state; adversarial forged nested metadata leaves it incomplete.

Run focused red/green selectors first, then `lein test-proflog-fast` and
`lein test-proflog-extended` in parallel, followed by focused SJAS progression
and `lein test-proflog-sjas`; long-running synthesis/evidence probes last, with
durable logs to `test-runs/`.

## Exit Criteria

- The misattributed V4/V5/`willard-map`/`Upsilon`/`Paradox` apparatus and dead
  boundary routes are removed; no trusted boundary route remains and the checker
  rejects the old constructor.
- The Theorem 2.3 closure is mechanized and accepted, step-by-step, by the
  ordinary structural checker over the exact generated multiplication-total
  SelfCons code and reflected beta.
- The boundary contrast is demonstrated: the addition-only variant fails to
  close, localized to the bound-arithmetic step.
- The predicate-trust audit confirms genuine checking on every closure-path
  predicate (`semprfk-alpha`, `subst-code` functionality, the Q-disproof of the
  invalid Pi1 leaf).
- An independently synthesized proof tuple passes the same validation and has a
  durable log.
- The ledger and all public completion surfaces agree and derive from verified
  state; gates (fast, extended, SJAS) are green.
- AAR-0142 records implementation, red/green evidence, exact tuple codes, the
  decoded closing proof tree, the trust audit, synthesis logs, and final gates.
- ADR-0119 records Workstream B complete on the basis of this genuine
  derivation.
