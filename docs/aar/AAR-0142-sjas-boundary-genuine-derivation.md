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
lein test-proflog-extended   (recorded on completion)
lein test-proflog-sjas       (focused not-slow; new willard-sjas-test vars green individually)
```

All ADR-0142 focused tests are green individually (see Red-Green Evidence). The
fast gate, which includes `sjas-correspondence-test` and the four new namespaces,
is green.

## Follow-ups (separate ADRs)

- Tab-2-or-stronger and Xtab/LEM variants remain open (criterion 10).
- The cut-free expansion and the tower-bounded `¬Dk` step are the two research
  problems that a future ADR must solve to close the multiplication obligation.
