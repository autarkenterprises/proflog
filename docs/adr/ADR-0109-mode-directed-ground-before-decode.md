# ADR-0109: Mode-Directed Ground-Before-Decode (Width-Reduction #1)

- Status: accepted
- Date: 2026-06-14
- Branch: `adr-0107-pure-indexed-lookup` (carries ADR-0107 #2 and this ADR-0109 #1)
- AAR: [AAR-0109](../aar/AAR-0109-mode-directed-ground-before-decode.md)

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

## Exit Criteria

- The decoders are mode-directed (backward terminates — measured above); the
  behaviour test pins it; ADR-0093 + fast (198/1047/0) + SJAS not-slow
  (1060/0/0) gates green; no `project`/`conda`/host cut introduced; the
  header-before-payload guard preserved.
