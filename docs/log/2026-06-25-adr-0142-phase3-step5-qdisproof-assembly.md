# ADR-0142 Phase 3 (step 5 Q-disproof): the false-Pi1-instance refutation assembles

Date: 2026-06-25
Builds on: [Phase 3 pow-vocabulary](2026-06-25-adr-0142-phase3-pow-vocabulary-decoded-semprfk-closure.md),
[Phase 3 baseline](2026-06-23-adr-0142-phase3-construct-and-check-baseline.md)

This session advances Theorem 2.3 step 5 (`not Dk`) by building the structural
machinery for Willard's **Q-disproof of the false Pi1 instance** and verifying it
assembles over the real multiplication system. It also pins down precisely what
the remaining not-Dk obstruction is — a checker capability, not just unfinished
tree assembly. No BOT derivation is claimed; step 5 stays `:partial`.

## The instance and its refutation

The diagonal is `Dk = forall h y z. Subst(nbar,h) => not SemPrf^k(sys,k,h,y,z)`.
Step 5 instantiates it at the diagonal witnesses to get the Pi1 instance

    Subst(nbar, code(Dk)) => not SemPrf^k(sys, k, code(Dk), p, 2^(p+1))

which is FALSE: `Subst(nbar, code(Dk))` holds by Goedel diagonalization (Eq 7) and
the bounded proof `SemPrf^k(code(Dk), p, 2^(p+1))` holds, so the consequent fails.
Refuting the implication is the beta-rule — branch into `not antecedent` and
`consequent`, both must close — and the two leaves are the two halves of the
Q-disproof.

## Delivered (checker-verified)

- **`sjas-subst-code-structural-closeo`** (new kernel relation), wired into the
  structural close disjunction next to the SemPrf^k V-route. It closes
  `(neg Subst(s,t))` by EVALUATING the genuine `subst-code` gate
  (`sjas-subst-code-any-coreo`: decode the source formula-code, substitute its
  distinguished free variable, compare modulo alpha-renaming), reusing the same
  core as the search-layer `sjas-subst-code-closeo` minus the proof marker. This
  is the construct-and-check analog of the SemPrf^k V-route, and the missing
  Q-disproof half: before it, `(neg Subst)` could only close by a pos/neg clash
  (the arithmetic-only `sjas-relation-holds-coreo` does not evaluate subst-code),
  while `(neg SemPrf^k)` already self-closed.
- **`proflog.sjas-not-dk-qdisproof-test`** (new):
  - `subst-qdisproof-closes-the-real-diagonal-locator` (fast): `(neg Subst(nbar,
    code(Dk)))` self-closes over the REAL diagonal locator, and a wrong target
    stays open (the gate is genuinely evaluated).
  - `false-pi1-instance-refutation-assembles-both-q-disproof-branches`
    (`^:slow`, ~1 min: decodes the giant `code(Dk)` numeral through both leaves):
    refuting `Subst(nbar,code(Dk)) => not SemPrf^k(...)` closes the negated
    antecedent by subst eval and the consequent by the V-route.
- **Ledger** (`theorem23-closure-status`): `:phase3-qdisproof` recorded under
  `:resolved-since-aar`; `:step5-subst-qdisproof-closes-real-diagonal` and
  `:step5-false-pi1-instance-refutation-assembles` added to `:checker-accepted`;
  step 5 note updated; the open boundary refined (below).

## Finding: the remaining not-Dk obstruction is witness-providing gamma

Refuting the universal `Dk` directly does NOT work, and the reason is precise:
the checker's `forall` rule (`sjas-proof-check-stateo`, the `(forall (tie ...))`
branch) instantiates a universal with a **fresh branch variable**
(`sjas-next-branch-nomo`), never a chosen ground witness; and the SemPrf^k V-route
cannot resolve a *free* proof/bound variable (the symbolic `pow` bound matches the
literal `(app pow base exp)`, not a logic variable). Empirically, a ground-witness
gamma child is rejected and a fresh-variable gamma child cannot close the
bounded-proof leaf. So Willard's "instantiate Dk with (p,q,r)" — deriving the
ground instance from `Dk` (`Dk |- instance`) — needs a **witness-providing
gamma-instantiation** the checker does not yet have. This replaces the vaguer
"not-Dk-tree-assembly" boundary item.

## Honest scope

The assembled refutation's consequent leaf uses a real route-axiom bounded proof
as the SemPrf^k witness. In the real instance the theorem is `code(Dk)`, whose
bounded proof is the still-open step-4 combination tree; the checker closes each
leaf independently, so this validates the beta-split topology and both Q-disproof
rules while the coupling (same `code(Dk)` in both leaves) and the real proof `p`
remain documented residuals.

The open boundary is now two items: (1) the cut-free combination trees for steps
1/3/4/B; (2) witness-providing gamma-instantiation (`Dk |- ground-instance`).

## Gates

No-regression check for adding `sjas-subst-code-structural-closeo` to the
structural close disjunction:

- SJAS not-slow: **pass=1447 fail=0 error=0** (1445 + the 2 new subst-qdisproof
  assertions; the new ns runs in the focused SJAS gate, the `^:slow` beta-split
  excluded there and covered by `test-proflog-sjas-slow`).
- fast: green (the closure-status ledger test updated in lockstep).
- the `^:slow` beta-split passes in the slow context (fuel 50, ~80s).
