# Fitting-core defect probes (2026-11-07)

Evidence for [ADR-0149](../../adr/ADR-0149-fitting-core-extraction.md).
Each script was run against `main` at `849fd55` from a checkout root:

```sh
lein run -m clojure.main docs/log/2026-11-07-fitting-core-probes/<probe>.clj
```

Scripts that test the generic kernel alone replace
`proflog.kernel/profiled-closeo` with `fail` so no sidecar can intervene.

| Script | Defect | Observed on `849fd55` |
|---|---|---|
| `d1-d2-root-formulas.clj` | D1, D2 | `∀x∃y(y≠x)`, `∀x∃y(Px↔¬Py)`, `∃y((y=0∧0=s0)∨y≠0)` all close (all satisfiable); control `∃x(x≠x)` closes |
| `d1-d2-generic-and-query.clj` | D1, D2 | generic kernel closes the D1 formulas; `query-status t(0)` = `:fails` (⊥ expected); `query-fails r(0)` returns a disproof (`r(0)` true) |
| `d3-kernel-forall-vs-once-forall.clj` | D3 | `forall y(a(y)∧¬a(s y))` closes; same body under `once-forall` does not |
| `d3-query-paired.clj` | D3 | `r(0)` (negative call, `once-forall`) `:unresolved`, expected `:succeeds`; `r3(0)` (positive call, `forall`) `:fails` as expected |
| `d4-true-false.clj` | D4 | query level correct only through the sidecar; generic kernel cannot close `false` |
| `d5-closed-term-gamma.clj` | D5 | `q0(0)` `:unresolved` (expected `:fails`); `q(0)` times out |
| `d5-unrelated-clause.clj` | D5 | `q0(0)` is `:fails` alone but `:unresolved` once unrelated clauses are added |

Design probe (2026-11-08). It needs only core.logic, and its output is the
same on stock 1.0.1 and on the vendored copy:

| Script | Question | Observed |
|---|---|---|
| `binder-escape.clj` | Can unification of quantified formulas bind a free (§8) variable to a bound occurrence? | Named `==`: `v := (var y)`. Nominal without `hash`: `v := a_0`. Nominal with `a # v`: blocked. Atomic literals: no hazard. |

These are probes, not tests. ADR-0149's test obligations turn them into red
tests in the `fitting/` subproject.
