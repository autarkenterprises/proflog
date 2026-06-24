# AAR-0142: SJAS Multiplication-Boundary Derivation via Willard Theorem 2.3

- Date: 2026-06-23
- ADR: [ADR-0142](../adr/ADR-0142-sjas-boundary-genuine-derivation.md)
- Branch: `adr-0142-sjas-mul-boundary-derivation`
- Reviews honored: [Codex review](../interdev/2026-06-22-adr-0142-review-and-corrections.md),
  [owner reply](../interdev/2026-06-22-adr-0142-review-reply.md)

## Outcome (honest summary)

ADR-0142 is **substantially implemented but not closed.** Eight of the revised
exit criteria are delivered as ordinary-checker-verified artifacts with red/green
tests; the full Theorem 2.3 closing derivation of `BOT` remains an explicitly
documented **open research boundary** — exactly the part the review's emendation
flagged as requiring novel research (cut-free expansion + tower-sized `SemPrf^k`
witnesses). **No end-to-end checker-accepted `BOT` derivation is claimed.**
ADR-0119 Workstream B (multiplication variant) is **advanced, not closed.**

This deliberately does not repeat the retracted ADR-0141 pattern: nothing is
accepted by a trusted boundary constructor, no synthesis is host-seeded, and the
completion surface (`boundary-evidence-ledger`) is left to report incomplete
because no genuine closing certificate exists.

## Per-criterion status

| # | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Audit JSL2 Eqs 12-16 | **done** | `docs/log/2026-06-23-adr-0142-jsl2-axiom-audit.md`; repaired Paradox `∃d<z`; `sjas-adr0142-paradox-bounds-witness-below-z` |
| 2 | One measured proof predicate | **done** | `sjas-adr0142-semprf-and-semprfk-share-one-proof-predicate` (k=0 degeneration shows the shared core) |
| 3 | Iterated-log `SemPrf^k`, operational k | **done** | `sjas-iterated-logo` + `sjas-semprfk-bound-holdso`; `proof < Log(bound,k)`; `sjas-adr0142-iterated-log-matches-definition-2-1`, `...-semprfk-bound-is-operational-in-k` |
| 4 | `Map` / discharge A,B,C | **partial** | `theorem23-diagonal` (Dk shape) + `Subst(n̄,⌜DK⌝)` via `subst-code` (= Map locator); A,C reflected; **B rests on criterion 6** |
| 5 | Q/`W_D` bridge | **done** | `total-multiplication-translated-q-axioms` + deduction-modulo: interpreter decides Q4-Q7 instances; `sjas-adr0142-q-interpretation-bridge` |
| 6 | Cut-elimination / composition | **partial** | verified `compose-by-cut` (Theorem 2.2 with-cut); cut-free expansion = documented boundary; `sjas-cut-composition-test` |
| 7 | Validate `BOT` vs exact system | **partial** | step 2 (`Subst(n̄,⌜DK⌝)`) validated live; full `BOT` closure open |
| 8 | Independent synthesis | **partial** | `sjas-synthesis-guard` dataflow guard (the ADR-0141 lesson); full closing-tuple synthesis awaits a checker-complete closure |
| 9 | Completion from validated state | **done (honest)** | ledger derives `:complete?` from kernel verification; left incomplete, no false flip |
| 10 | Scope; gates; AAR | **done** | multiplication only; Tab-2/Xtab-LEM untouched; gates below; this AAR |

## What was built (checker-verified)

- **`Paradox` repair (C1).** Eq (12) is `∃d<z`; the kernel had it unbounded. Now
  `bounded-exists d z + lt(d,z)`, mirroring V5's faithful `∃x<z`. Full audit
  confirms V3/V4/V5/Υ/FinAx4/Subst are genuine JSL2 §3.2 apparatus (the revised
  ADR's withdrawal of the blanket-removal claim is correct).
- **Iterated-log `SemPrf^k` (C3).** `sjas-iterated-logo` computes
  `Log(x,k)` = k-fold floor-log (`Log(x,0)=x`). `sjas-semprfk-alpha-coreo` now
  checks `proof < Log(bound,k)` (Definition 2.1) instead of the k-ignoring
  `lt(proof,bound)`. `k` is operational: `2 < Log(16,1)=4` holds, `2 < Log(16,2)=2`
  fails. The prior valid certificate (code ~16.6M) with `bound=proof+1` is now
  correctly rejected (its code exceeds `Log(bound,1)`), while unbounded `SemPrf`
  still accepts it — Definition 2.1's exact distinction.
- **Theorem 2.3 diagonal + Map (C4).** `theorem23-diagonal` builds
  `Dk(α)=Γ(n̄)` with `Γ(g)=∀h y z. Subst(g,h)⇒¬SemPrf^k_α(h,y,z)`. The relational
  `subst-code` decides `Subst(n̄,⌜DK⌝)=TRUE` (Eq 7 and, read as a locator,
  `Map(α,k,⌜DK⌝)` — Lemma 3.3), rejects non-diagonals, and is operational in `k`.
- **Q/`W_D` bridge (C5).** The U-Grounding interpreter decides every ground
  instance of the translated Q4-Q7 — which is precisely the instance-level
  Q-reasoning Theorem 2.3 uses (Q decides `Δ0` validity / `Π1` invalidity). Q6/Q7
  are reflected beta; Q4/Q5 are interpreter-realized (matching review §4).
- **Theorem 2.2 composition (C6).** `compose-by-cut` realizes the cut-elimination
  corollary: from kernel entailments of `Λ`, `Θ`, `Λ∧Θ⇒Ξ` it builds a verified
  with-cut proof of `Ξ`. Leaf sub-proofs are genuine kernel entailments
  (re-derived during validation, never trusted); the only added inference is the
  analytic cut (excluded middle). Rejects open leaves and malformed implication
  shapes; the conclusion is cross-confirmed by the kernel.
- **Closure assembler (C6b).** `theorem23-closure-status` lays out the six steps
  over the exact generated system and tags each: A/C reflected, B/step1/step3/step4
  `:cut-composition`, step2 `:checker-accepted` (validated live), step5
  `:open-boundary`. A test guards against ever reporting a uniformly
  checker-accepted status.
- **Dataflow guard (C8).** `assert-dataflow-independent!` enforces that synthesis
  tuple components are fresh lvars at proof entry — the reusable ADR-0141 lesson.

## The open research boundary (criteria 6/7, and B in 4)

Two genuinely open problems block an end-to-end `BOT` closure; both are faithful
manifestations of Willard's mathematics, not implementation gaps:

1. **Cut-free expansion (Theorem 2.2).** The three Theorem 2.2 steps (Eqs 6, 8, 9)
   are realized *with cut*. Gentzen guarantees a cut-free tableau exists, but its
   size is super-exponential in the leaf sizes (JSL2 sketch). It is not
   materialized; `proflog.sjas-cut-composition/cut-free-expansion-boundary`
   records this.
2. **Tower-sized `SemPrf^k` witness (step 5, Eq 11).** `¬Dk` needs witnesses
   `(p,q,r)` with `Log(q,K) > p`, where `p` is the proof code of `Dk`. Real proof
   codes are ~10^7; so `q` must be a tower (`q ≈ 2↑↑K (p)`) — astronomically large
   to materialize as a numeral. This is exactly Willard's bound-arithmetic
   obstruction (Definition 2.1) made operational, and exactly why the bottom-proof
   is "search-inaccessible and must be constructed" (ADR-0142 context).

Condition (B)'s discharge depends on (1) (it is a Theorem 2.2 application), so it
inherits the same boundary.

## Red-green evidence (focused)

```text
sjas-adr0142-paradox-bounds-witness-below-z         red(3 fail) -> green(4)
sjas-adr0142-iterated-log-matches-definition-2-1    green(5)
sjas-adr0142-semprfk-bound-is-operational-in-k      green(5)
sjas-adr0141-...-v-route-predicates-execute         updated: now asserts rejection, green(3)
sjas-adr0142-q-interpretation-bridge                green(8)
sjas-adr0142-semprf-and-semprfk-share-...           green(3)
sjas-adr0142-theorem23-diagonal-and-map-locator     green(4)
proflog.sjas-cut-composition-test                   green(4 tests / 12)
proflog.sjas-theorem23-closure-test                 green(2 tests / 11)
proflog.sjas-synthesis-guard-test                   green(2 tests / 5)
```

## Gates

```text
lein test-proflog-fast       Ran 267 tests / 2323 assertions, 0 failures, 0 errors (exit 0)
                             (includes the 4 new ADR-0142 test namespaces)
lein test-proflog-extended   Ran 92 tests / 971 assertions, 0 failures, 0 errors (exit 0)
lein test-proflog-sjas       :SUMMARY pass=1429 fail=0 error=0 (exit 0)
                             focused not-slow gate (the ^:slow fixed-point outlier
                             is excluded); confirms the SemPrf^k change broke
                             nothing across the full not-slow SJAS suite
```

All three gates are green. The SJAS gate (1429 passing) is the authoritative
check that the Definition-2.1 `SemPrf^k` rewrite did not regress any existing
boundary/correspondence test.

## Phase 3 baseline update (2026-06-23, same day)

Following the obstruction-overcoming plan's Phase 0/1
(checker is a cut-free-tree validator; symbolic `SemPrf^k` bound done), this
session landed the Phase 3 construction **infrastructure and a committed
baseline**, without promoting any closure step (no overclaim):

- **`proflog.sjas-tree-builder`** (new): the formula-bearing node builders
  (`flex-tableau-node` narrow/wide auto-select, `canonical-flex-tableau-node`,
  `valid-tree?`) promoted out of the test ns into reusable construction
  primitives.
- **`proflog.sjas-tree-builder-test`** (new, 3 tests / 12 assertions): the
  construct-and-check baseline over the **exact generated multiplication-total
  system** (reflexive/conjunction/double-negation closures; narrow+wide node
  shapes) — Phase 0's result promoted from the demo system to the real one.
- **Closing-rule characterization (finding).** A constructed `(pos A)`/`(neg A)`
  clash closes **iff** `A`'s relation decodes to a *named* symbol. Reserved
  U-Grounding primitives (`subst-code`, `lt`, `leq`, `axiom-member`) close;
  profile-local and user relations decode to `(sym n)` (source table removed) and
  close by **interpretation** instead, never by raw clash. This is not an
  obstruction: the Theorem 2.3 diagonal path closes via `subst-code` (named) plus
  the `semprf`/`semprfk` profile interpretation.
- **Refined `pow`-vocabulary risk.** Recorded in `theorem23-closure-status`
  `:open-boundary`: the encoder assigns per-system *compacted* reserved indexes
  while the proof-facing decoder resolves *global* reserved indexes; appending
  `pow` to `reserved-coding-symbols` (kept out of `profile-local-reserved-symbols`
  so it decodes by name) must keep those two views in agreement, with the full
  SJAS gate as the no-mis-decode falsifier.

Detail: [Phase-3 baseline note](../log/2026-06-23-adr-0142-phase3-construct-and-check-baseline.md).

Re-confirmed gates (with the new tree-builder test added to the fast gate):

```text
lein test-proflog-fast       Ran 270 tests / 2338 assertions, 0 failures, 0 errors
lein test-proflog-extended   Ran 92 tests / 971 assertions, 0 failures, 0 errors
lein test-proflog-sjas       :SUMMARY pass=1445 fail=0 error=0
```

Per-criterion status is unchanged (4/6/7/8 stay partial; nothing promoted to
`:checker-accepted`). What changed is that the construction infrastructure and a
checker-verified baseline over the real system are now committed, and the two
remaining research problems are more precisely localized.

## Follow-ups (separate ADRs)

- Tab-2-or-stronger and Xtab/LEM variants remain open (criterion 10).
- The cut-free expansion and the tower-bounded `¬Dk` step are the two research
  problems that a future ADR must solve to close the multiplication obligation.
- Next concrete step: build the step trees test-first with
  `proflog.sjas-tree-builder` in dependency order (steps 1→3→4 and B; then step 5
  once `pow` is in the coding vocabulary; then step 6), promoting each
  `theorem23-closure-status` tag to `:checker-accepted` only as it lands.
