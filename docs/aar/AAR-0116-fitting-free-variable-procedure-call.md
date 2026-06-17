# AAR-0116: The Core Procedure Call Rule is Fitting's §8 Free-Variable Extension

- Status: accepted
- Date: 2026-06-17
- ADR: [ADR-0116](../adr/ADR-0116-fitting-free-variable-procedure-call.md)

## Outcome

The audit's headline §6 question — does the core fire procedure calls only on
*ground* atoms of `L` (Fitting §6), or on variable-bearing atoms (§8) — is
resolved from source: the core admits free proof variables and rejects only delta
parameters (`l-ground-termo`, `kernel_support.clj:313`), so it implements
Fitting's **§8 free-variable Prolog-style call**, with `l-ground` as his
"keep-the-unifier-in-`L`" mechanism. Classified `➕ extension-beyond-Fitting`,
pinned by `sec6-l-ground-guard-admits-variables-but-rejects-parameters` (green).

## What worked

- Reading the guard definition settled the question definitively where an earlier
  automated pass had only asserted "✓ compliant." Evidence over assertion.
- A two-line unit test on `l-ground-termo` (var admitted, par rejected) is a
  sharper and cheaper witness than any end-to-end query.

## What surprised / was corrected

- My own prior hypothesis (kernel = ground-only §6, overlay = free-variable §8)
  was **wrong**: the free-variable extension is in the *core*. The overlay only
  adds residuals/call-depth/answer-export above an already-free-variable rule.
- The implementation lands precisely in the design space Fitting flagged as
  "future work … a serious complication" — the project quietly solved his
  keep-in-`L` requirement with a structural `par`-rejecting guard.

## Follow-ups

- Soundness of the free-variable call rule vs. the supervaluation semantics is
  *characterized*, not *proved* — a mechanized argument (and an optional
  strict-§6 mode) is deferred.
- At audit close, reconcile with `origin/main` ADR-0114 (open-branch witness
  extraction) and ADR-0112 (literature golden suite), which touch the same
  open-branch / free-variable boundary.
