# ADR-0118: Fitting-Fidelity Audit — Secondary Findings and Dispositions

- Status: accepted
- Date: 2026-06-17
- Branch: `fitting-fidelity-audit`
- AAR: [AAR-0118](../aar/AAR-0118-fitting-audit-secondary-findings.md)
- Audit: [docs/FITTING_FIDELITY_AUDIT.md](../FITTING_FIDELITY_AUDIT.md)

## Context

The audit's two headline results have their own ADRs — the §6 free-variable
procedure call ([ADR-0116](ADR-0116-fitting-free-variable-procedure-call.md)) and
quorum proof-checking + proof-term adequacy
([ADR-0117](ADR-0117-quorum-proof-checking.md)). This ADR records the **secondary
findings** so every row of the fidelity matrix is dispositioned. None is a defect;
each is a faithful realization, a justified representational divergence, or a
§8-sanctioned compromise.

## Findings and dispositions

### 1. ⊥ vs `:unresolved` (§3) — ⚖️ faithful with documented over-approximation

The implementation computes Fitting's *operational* valuation `t_P` (Definition
7.1) via two tableau searches; it does **not** compute the denotational `Φ_P` /
smallest supervaluation model `s_P`. Genuine semantic `⊥` ("undefined") is
reported as `:unresolved`, which is *budget-exhausted* and therefore
**over-approximates** `⊥`: the system reports `succeeds`/`fails` only on an actual
closure, and everything else as "don't know." This is sound as a *reporting*
discipline (no spurious definite verdict) but does not decide `⊥`.

Evidence: `fitting-fidelity-test/sec3-occurs-…` (existential occurs stays open)
and `…/sec3-p1-even-or-odd-is-undefined` (`(∀x)(even∨odd)` is `:unresolved`).
Deciding true `⊥` would require the `Φ_P` fixpoint, which is out of scope (and not
what an operational LP language computes). Deferred.

### 2. γ-instantiation envelope (§4/§8) — ➕ §8-sanctioned compromise

Fitting's γ rule admits *any* closed term of `L^par`. The core bounds this via
fresh-variable instantiation (`kernel.clj:893`), a closed-term candidate path
(`:919`, `gamma.clj`), and fair re-enqueue (ADR-0016). This is the §8 free-variable
γ, paired with the keep-in-`L` `l-ground` guard ([ADR-0116](ADR-0116-fitting-free-variable-procedure-call.md)).
The completeness envelope is therefore Fitting's §8 compromise: incomplete for
arbitrary instantiation, but with the sources of incompleteness understood. The
propositional fragment is differentially complete
(`sec7-propositional-validity-matches-truth-tables`). Deferred: a quantitative
characterization of the first-order γ envelope.

### 3. Substitutivity via an explicit σ (§5) — ⚖️ representational divergence

Fitting states Reflexivity and Substitutivity as branch-extension rules. The core
realizes them with an explicit equality substitution `sigma` plus
`equality/unify-termo`/`walko` rather than rule applications: walking a term
through `sigma` and unifying is exactly the closure under substitutivity. Justified
and tested: `equality-test/equality-bindings-compose-through-transitive-chains`
and `…-atom-congruence-on-the-branch` exhibit left-right replacement and
congruence; Free-Closure and One-One are the `eq-contradictiono`/decompose paths
(§5 matrix rows). Faithful in meaning; divergent only in presentation.

### 4. Answer-overlay soundness (§8 layer) — ⚖️ faithful mirror

`answer_overlay.clj` mirrors the kernel's tableau rules (`:690-870` vs kernel
`:885-1045`) with added `residuals` / `call-depth` / `existentials-as-vars?`
threading and identical proof tags. Unresolved calls are exported as
distinctly-tagged residual obligations (`eq-triggered-residual-call`, `:147`),
**never folded into a closure**, so an answer with residuals is reported with its
open frontier rather than claimed proven. Parameters are kept out of exported
answers by `answers/admissible-term?` / `ground-no-par?` (`answers.clj:127,540`).
The ADR-0117 quorum accepts P2 `win(4)` certificates, exercising the shared rule
grammar. Deferred: a line-level diff of all ~2.5k LOC (highest-residual-risk
surface, but no rule weakening found by inspection).

## Reconciliation note (parallel work)

`origin/main` ADR-0114 (open-branch witness extraction) is adjacent to finding (1)
(the `⊥`/`:unresolved` / open-branch boundary); reconcile at audit close.

## Exit criteria

- Every fidelity-matrix row is dispositioned (faithful / representational-
  divergence / extension / finding), with an ADR for each non-faithful row.
- Deferrals named explicitly: precise `⊥`, first-order γ envelope quantification,
  full answer-overlay line diff, and the FO/equality differential soundness test.
