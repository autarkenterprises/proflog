# Interdev Handoff: Proof-Checker Ground-Target Propagation

- Date: 2026-06-14
- From: SJAS tractability line (ADR-0105/0106/0107/0110)
- To: the parallel agent picking up SJAS tractability
- Prerequisite branch: `adr-0107-pure-indexed-lookup` (pushed; **merge to main
  first** — it carries #1 / ADR-0110, the prerequisite, and the #2 revert)
- Full design: ADR-0110 "Proposal: proof-checker ground-target propagation"

## Why this is *the* lever (state of play)

The subst-prf negative-exhaustion wall grinds in **free-key formula decode
enumeration inside the proof checker** (ADR-0106 corrected diagnosis).

- **#1 (ADR-0110) — done, on this branch.** Made the formula/term decoders
  *mode-directed*: with a ground formula the decode runs forward (constructor `==`
  moved to the front of every branch). Backward decode of `0=0` went from
  non-terminating (>70 s) to ~0.3 ms.
- **But the heavy proof-check tests did not move** (measured, clean): e.g.
  `level1-group-three-rejects-wrong-public-code` 1.03×,
  `proof-check-…-distinct-nested-existential` 1.01×. Reason: the checker decodes
  each node's formula **while it is still free**, so the mode-directed decoder
  never enters the ground regime.
- **#2 (ADR-0107, indexed lookup) — reverted, dead end.** Measured ~2× *slower*
  than the linear `or*`; the ground-key lookup was never the cost. **Do not
  revisit table-lookup determinisation.**

So the one change the data says will move the wall: **make the checker ground each
node's formula from the (ground) target *before* decoding it**, so #1's
mode-directed decoder fires.

## The defect (exact locations, `src/proflog/kernel/willard_sjas_profile.clj`)

- `sjas-proof-check-stateo` (**:7192**) does, in order:
  1. `formula-bearing-proof-nodeo prog proof node-formula children` — decodes
     `node-formula` **from the (free, in the negative) proof**.
  2. `sjas-proof-guided-selecto node-formula …` (**:7165**) — selects a branch
     formula but is **proof-node-blind** (its first arg is ignored, by design,
     "to avoid re-matching a large formula before every rule check").
  3. The ground branch formula reconciles with the decoded `node-formula` only
     *later*, inside `sjas-structural-proof-check-state-decodedo` (**:6172**) via
     `(sjas-acyclic-unifyo visible-formula …)`.
- `formula-bearing-proof-nodeo` (**:5668**) decodes via
  `decode-proof-formula-byteso` (**:1994**) → `decode-formula-byteso` (the
  #1-reordered decoder). **The mode-directed decoder is already wired in here**;
  it just runs on a free `node-formula`.
- The recursive child case repeats the same decode-then-select-blind pattern in
  `sjas-structural-proof-check-state-decodedo` (~:6187–6193).

In the negative, step 1 enumerates the formula language; the ground target only
filters afterward. That is the wall.

## The change

Select the branch formula **first** (grounding `node-formula` from the target),
**then** decode:

```clojure
(sjas-proof-guided-selecto node-formula env (lcons fml unexpanded)
                           selected selected-env remaining)
(== node-formula selected)                                      ; ground node-formula from the target
(formula-bearing-proof-nodeo prog proof node-formula children) ; now decodes FORWARD (mode-directed, #1)
```

Either make `sjas-proof-guided-selecto` formula-aware (use its currently-ignored
first arg to unify `selected` with `node-formula` structurally) or add the
explicit `(== node-formula selected)` between select and decode. Now
`formula-bearing-proof-nodeo` decodes with `node-formula` ground → #1 runs it
forward (ground formula → its unique byte encoding) and **rejects** any proof
whose bytes don't encode a branch formula: O(branch) ground candidates instead of
enumerating the formula language. Apply the same reorder to the recursive child
decode in `sjas-structural-proof-check-state-decodedo`.

**Why #1 is the prerequisite:** without it (constructor `==` last) a ground
`node-formula` still enumerates; with it, ground drives forward. Merge ADR-0110
first.

**Recursive propagation (why it compounds):** Willard's `D` rules are *functional
top-down* — a ground conclusion formula + the rule determines the premise (child)
formulas. So once the root `node-formula` is ground from the target, each rule
application grounds its children's sub-targets → grounds their node-formulas
before *their* decodes → the ground target flows down the entire proof tree.

## The tradeoff to MEASURE (do not skip)

Select-before-decode adds an **O(branch-size)** factor to the **positive** case
(each branch candidate unified into `node-formula`, the ground proof re-decoded
forward ~ms each). Report **both** directions:

- **Positive regression risk** (current HEAD = #1 only, clean ms):
  - `proof-check-accepts-…-distinct-nested-existential-parameters` ≈ 26.3 s
  - `…-equality-triggered-literal-closures` ≈ 14.0 s
  - `…-equality-triggered-positive/negative-calls` ≈ 7.9 s
- **Negative / heavy gain target** (these should drop a lot — they are the wall):
  - `level1-group-three-rejects-wrong-public-code-representation` ≈ 28.6 s
    (a rejection; the closest *not-slow* wall analogue — the key test to move)
  - `^:slow sjas-subst-prf-rejects-selfcons-complement-axiom-certificate`
    (negative) — **does not complete in 600 s** (the wall).
  - `^:slow sjas-subst-prf-checks-selfcons-fixed-point-certificate` (positive) —
    **also does not complete in 600 s under the linear/reverted code** (it timed
    out at 240 s under int-indexo, but linear at 600 s confirms it is *inherently*
    heavy, near/at the wall — **not** a #2 artefact). A success criterion for the
    propagation is bringing this positive under the envelope.

**Pure mitigations** if the positive regresses past baseline (no `project`/`conda`/
`condu`/host cut — §D is binding):
- (a) a relational **skeleton/header prefilter**: unify only the top constructor /
  byte-length of `node-formula` against the disjunction of branch formulas before
  the full decode;
- (b) post `node-formula ∈ branch-formulas` as a membership constraint and let the
  single mode-directed decode both verify and select.

## Test obligations

- ADR-0093 canonical suite green; fast + SJAS not-slow + extended green.
- An answer-set **agreement** test: the reordered checker accepts/rejects exactly
  the same (proof, target) pairs as before (with/without).
- §D purity: no `project`/`conda`/`condu`/host cut.
- Measurement note (positive/negative before/after) in the successor AAR.

## Also outstanding (smaller)

- The parallel **`decode-syntax-*`** family (`decode-syntax-formula-byteso` etc.,
  ~:1700–1900) still has constructor `==` last; apply the same #1 reorder
  (mechanical, ADR-0110-style) to complete the decoder work.

## Do NOT

- Revisit #2 / indexed-lookup table determinisation (ADR-0107/AAR-0107 reverted —
  the fd-trie is a measured dead end; only a *non-fd* trie on a table ≫4096 could
  ever pay off, and the lookup was never the bottleneck anyway).
- Introduce `project`/`conda`/`condu`/host cuts.

## Pointers

- Proposal in full: `docs/adr/ADR-0110-mode-directed-ground-before-decode.md`
  ("Proposal" section).
- #1 outcome: `docs/aar/AAR-0110-…`. #2 negative result: `docs/adr/ADR-0107-…`,
  `docs/aar/AAR-0107-…`.
- Measurement method: SJAS not-slow via the focused runner prints per-var
  `:DONE <name> <ms>`; for before/after, `git checkout <ref> -- <profile>` then
  restore. Slow vars run individually under a `timeout` so the never-completing
  negatives don't block the rest.
