# AAR-0118: Fitting-Fidelity Audit — Secondary Findings and Dispositions

- Status: accepted
- Date: 2026-06-17
- ADR: [ADR-0118](../adr/ADR-0118-fitting-audit-secondary-findings.md)

## Outcome

Every remaining fidelity-matrix row is now dispositioned: ⊥-vs-`:unresolved`
(⚖️ over-approximation), the γ-instantiation envelope (➕ §8 compromise),
Substitutivity-via-σ (⚖️ representational divergence), and answer-overlay
soundness (⚖️ faithful mirror). None is a defect.

## What worked

- Consolidating the four smaller findings into one disposition ADR avoided ADR
  sprawl while keeping the matrix fully accounted for.
- The propositional differential test gave a crisp, defensible "complete here"
  boundary for the γ-envelope discussion (finding 2).

## What was learned

- The recurring shape of this audit: the implementation is *faithful in meaning*
  but *divergent in presentation* (σ-substitution, multi-clause sugar) or
  *extended into Fitting's own §8 space* (free-variable calls/γ). Very little is a
  genuine departure; the value is in naming each precisely and testing it.

## Follow-ups (deferred, named)

- Precise `⊥` would need the `Φ_P` fixpoint (not an operational concern).
- First-order/equality differential soundness (no propositional-style oracle yet).
- Quantify the first-order γ envelope.
- Line-level diff of `answer_overlay.clj` vs the kernel rules.
- Reconcile finding (1) with `origin/main` ADR-0114 at audit close.
