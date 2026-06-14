# ADR-0110: Mode-Directed Ground-Before-Decode (Width-Reduction #1)

- Status: accepted
- Date: 2026-06-14
- Branch: `adr-0107-pure-indexed-lookup` (carries ADR-0107 #2 and this ADR-0110 #1)
- AAR: [AAR-0110](../aar/AAR-0110-mode-directed-ground-before-decode.md)

## Context

[ADR-0106](ADR-0106-sjas-search-width-reduction.md) §C identified **mode-directed
/ ground-before-decode evaluation** as the *highest-leverage* width-reduction
lever, and named "forward-mode reformulation of the hot decode path" as the
successor implementation ADR. The corrected diagnosis: the subst-prf negative
grinds in `decode_formula_byteso` / `decode_embedded_code_bodyo` /
`static_table_entryo` over **variable-dense** intermediate terms; ground decode
is ~1 ms (≈1000× faster). The §D purity constraint is binding: the speed must be
an emergent property of a *pure* relation given ground inputs — **no
`project`/`conda`/`condu`/host cut**.

## The defect (mechanism)

Every branch of the formula/term byte decoders placed the **constructor
unification last**, after the recursive byte-decodes:

```clojure
;; before
[(fresh [left right after-tag after-left]
   (== (lcons formula-eq-tag after-tag) bytes)
   (decode-term-byteso prog after-tag after-left left)   ; runs with `left` free
   (decode-term-byteso prog after-left rest right)        ; runs with `right` free
   (== (list 'eq left right) formula))]                    ; connects formula LAST
```

core.logic conjunction is sequential per answer (only `mplus` interleaves), so a
**ground** `formula` could not reach `left`/`right` until *after* the recursive
decodes had already enumerated every sub-term/byte encoding. The decoder was
therefore mode-directed in the *forward* direction (ground bytes → the tag byte
discriminates the branch) but **degenerate in the backward direction** (ground
formula → bytes): a `run 1` backward decode of even `0 = 0` did **not** complete
in 70 s (timed probe killed it). This is the variable-dense grind in miniature:
when the proof checker holds a ground target but the decode does not propagate
that groundness, decode enumerates instead of computing.

## Decision

Reorder each decoder branch so the **constructor `==` runs first** (pure
conjunction reordering — answer-set-identical by construction, since the decoders
use only `==`/`lcons`/recursive relational calls; no `project`/`conda`). A ground
output then drives the decode forward by unification; a free output leaves a
single cheap binding and the prior enumeration is unchanged. Sites:

- `decode-formula-byteso` — all 15 branches (`true`/`false`/`pos`/`neg`/`eq`/
  `neq`/`and`/`or`/`not`/`implies`/`forall`/`once-forall`/`exists`/
  `bounded-forall`/`bounded-exists`): constructor `==` moved ahead of the tag
  match and recursive decodes.
- `decode-term-byteso` (`var`/`par`) and `decode-app-termo`: term `==` first.
- `parse-code-payload-byteso` / `parse-term-list-byteso`: the output cons
  (`(== (lcons … ) payload|terms)`) bound **before** the recursion, so a ground
  payload/term fails a wrong-length arm in O(depth-to-mismatch) instead of after
  building a full depth-`remaining` structure.
- `decode-natural-bodyo` / `decode-embedded-code-bodyo`: term `==` moved ahead of
  the payload parse, **inside** the existing `(fresh [payload] …)` — the two
  header-length checks still run first, so the forward-mode length rejection
  guarded by `sjas-embedded-payload-decoders-check-header-before-payload-fresh`
  is preserved (see Test Obligations).

This is purely conjunction ordering inside existing relations: **no new operator,
no committed choice, no host lookup** — exactly ADR-0106 §D #1 ("pure by
conjunction ordering").

## Test Obligations (TDD)

- New fast-gate `proflog.decode-mode-directed-test`: forward decode unchanged;
  **backward** decode (ground formula → bytes) now *terminates deterministically*
  with the unique encoding (the RED before the reorder was a 70 s non-completion,
  recorded above); round-trip agreement on a recursive `(and …)` formula.
- The existing source-structure guard
  `sjas-embedded-payload-decoders-check-header-before-payload-fresh` must stay
  green — the reorder keeps the length headers ahead of `(fresh [payload]`. (An
  initial hoist of the term `==` above the header checks tripped this guard; the
  fix narrows the reorder to *inside* the payload `fresh`, honouring the
  forward-mode optimisation the guard codifies.)
- Full ADR-0093 canonical suite + fast gate + SJAS not-slow gate green
  (answer-set preservation across the whole decoder-driven proof path).

## Honest scope

This makes the formula/term **decoders** mode-directed (demonstrated: backward
decode collapses from non-terminating to deterministic). Whether the *full*
subst-prf negative wall collapses additionally requires the **proof checker** to
propagate the ground target down to each node's formula *before* that node's
bytes are decoded — a further goal-ordering question on
`sjas-proof-check-programo` / `decode-structural-proof-bytes-coreo`. That, plus
the parallel `decode-syntax-*` family (same mechanical transform, deferred here
to keep the change reviewable and the measurement clean), is the named
follow-up. #1 here is the enabling, correctness-preserving reformulation of the
decode primitives; the grind-level payoff is measured in the successor.

## Measurement

Backward decode (ground formula → bytes), `run*`, warmed:

| query | before | after |
|---|---|---|
| `(eq (num ()) (num ()))` | not complete in 70 s | **0.33 ms**, 1 answer |
| `(and (eq 0 0) (neg (var 1)))` | (not run — same regime) | **1.19 ms**, 1 answer |
| forward `(5 25 0 0 25 0 0)` → formula | ~0.94 ms | ~0.94 ms (unchanged) |

The backward direction collapses from non-termination to deterministic
milliseconds; forward is unaffected. This is the decoder-level demonstration of
the ground-decode regime (ADR-0106's ~1 ms ground path) the negative wall must be
driven into.

### Whole-gate before/after (the honest workload impact)

SJAS not-slow, 145 timed vars, one clean contention-free run each (HEAD vs the
pre-optimisation profile at `128e819`), same `pass=1060 fail=0`:

- **Overall ≈ 1.01×** (summed per-var time 138.0 s → 136.3 s) — *flat*.

The effect is **localised to paths where decode already carries ground
structure** (reader rejections, certificate checks):

| test | OLD → NEW ms | ratio |
|---|---|---|
| `compact-code-reader-rejects-arity-mismatched-terms` | 244 → 59 | **4.15×** |
| `correspondence-anti-compression-rejects-skeletal-certificate` | 369 → 106 | **3.47×** |
| `axiom-member-query-ignores-injected-generated-facts` | 1154 → 714 | 1.62× |
| `proof-predicate-system-code-reconstruction-walks-equality-state` | 2192 → 1711 | 1.28× |
| `composite-examples-distinguish-beta-axioms-from-reflected-procedures` | 2012 → 1596 | 1.26× |

**The deep proof-check tests are unchanged** — and they dominate the gate's
absolute time and are the closest not-slow analogue of the negative wall:

| test | OLD → NEW ms | ratio |
|---|---|---|
| `level1-group-three-rejects-wrong-public-code-representation` | 29592 → 28628 | 1.03× |
| `proof-check-accepts-…-distinct-nested-existential-parameters` | 26446 → 26303 | 1.01× |
| `proof-check-accepts-…-equality-triggered-literal-closures` | 12565 → 14050 | 0.89× |

They do **not** improve because the proof checker decodes each node's formula
*while it is still free* — `formula-bearing-proof-nodeo` runs the decode, and the
reconciliation with the ground branch formula (`sjas-acyclic-unifyo
visible-formula …`) happens *after* — so the mode-directed decoder never enters
the ground regime. A few construction-heavy tests regress slightly
(`arithmetic-…-synthesis-modes` 0.77×, `subst-prf-reconstructs-axiom-basis`
0.80×), attributable to #2's lookup constant (see ADR-0107 measurement).

**Net:** #1 is correctness-preserving with real localised wins where ground
structure is present, but it does **not** by itself move the proof-check critical
path. This empirically motivates the proposal below: the checker must ground each
node's formula *before* decoding it.

## Proposal: proof-checker ground-target propagation (the deeper #1)

**Where the grind is.** In `sjas-proof-check-stateo` (and recursively in
`sjas-structural-proof-check-state-decodedo`) the order is:

```clojure
(formula-bearing-proof-nodeo prog proof node-formula children)  ; decode node-formula FROM the (free) proof
(sjas-proof-guided-selecto node-formula env (lcons fml unexpanded)
                           selected selected-env remaining)      ; select a branch formula -- PROOF-NODE-BLIND
;; … later, the structural checker unifies `selected` (ground, from the target) with `node-formula`
```

In the **negative** (no proof exists) `proof` is the search variable, so
`node-formula` is free at decode time and `decode-proof-formula-byteso`
(→ `decode-formula-byteso`) **enumerates all formulas** — the wall — and the
ground branch formula only filters them afterwards. `sjas-proof-guided-selecto`
is *deliberately* proof-node-blind today (its first arg is ignored) to avoid
re-matching a large formula before every rule check — a choice that optimises the
**positive** case (ground proof → one decode) at the negative case's expense.

**The change.** Bind `node-formula` from the ground target *before* decoding it:

```clojure
(sjas-proof-guided-selecto node-formula env (lcons fml unexpanded)
                           selected selected-env remaining)
(== node-formula selected)                                      ; ground node-formula from the target
(formula-bearing-proof-nodeo prog proof node-formula children) ; decode now runs FORWARD (mode-directed, #1)
```

Now the decode runs with `node-formula` ground, so #1's reorder makes it compute
the node's formula-bytes *forward* (ground formula → its unique encoding) and
**reject** any proof whose bytes don't encode a branch formula — O(branch) ground
candidates instead of enumerating the formula language. This is why #1 is the
**prerequisite**: without it, grounding `node-formula` first would still
enumerate (constructor `==` was last); with it, ground drives forward.

**Recursive propagation.** Willard's `D` rules are *functional top-down*: a ground
conclusion formula plus the rule determines the premise (child) formulas. So once
the root node-formula is ground (from the target), each rule application grounds
its children's sub-targets, which ground their node-formulas before *their*
decodes — the ground target flows down the entire proof tree, and the
mode-directed decode fires at every node. The wall's combinatorial decode becomes
a ground, deterministic descent.

**The tension to measure (not hand-wave).** Select-before-decode adds an
O(branch-size) factor to the **positive** case: each branch candidate is unified
into `node-formula` and the (ground) proof re-decoded forward (~ms each) rather
than decoded once. The positive proof-check tests above (2–26 s) are the
regression risk; the negatives (`rejects-*` + the `^:slow` wall) are the gain.
The successor ADR must report both. If the positive regresses unacceptably, the
pure mitigations (no `project`/`conda`) are: (a) a **relational skeleton/header
prefilter** — unify only the top constructor / byte-length of `node-formula`
against the disjunction of branch formulas before the full decode, pruning
candidates cheaply; (b) post the membership `node-formula ∈ branch-formulas` as a
constraint and let the *decode* (now mode-directed) be the one pass that both
verifies and selects. Both keep the negative win while bounding the positive cost.

**Scope of the change.** `sjas-proof-check-stateo` and the recursive
`sjas-structural-proof-check-state-decodedo` (the `child-formula` decode at the
same decode-then-select pattern); make `sjas-proof-guided-selecto` formula-aware
(use its currently-ignored first arg) so the unify is structural, not a separate
goal. The `decode-syntax-*` family gets the same #1 reorder it still lacks.
Gate with ADR-0093 + an answer-set agreement test + the §D purity constraint, and
measure the positive/negative trade above.

## Exit Criteria

- The decoders are mode-directed (backward terminates — measured above); the
  behaviour test pins it; ADR-0093 + fast (198/1047/0) + SJAS not-slow
  (1060/0/0) gates green; no `project`/`conda`/host cut introduced; the
  header-before-payload guard preserved.
