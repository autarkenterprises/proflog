# ADR-0147 stage 4: numeral representation is a boundary parameter -- diagnostic only

Date: 2026-07-03. Branch `adr-0147-theorem23-bot-closure`.

Stage 3 root-caused the real-diagonal step-5 not-Dk check as *memory-bound*: the
mid-scale tree closes, but the real diagonal OOM'd. Stage 4 asked whether the
DEFAULT `:compact` code format -- which halves the embedded-code mass -- lifts
the wall. The answer, and the reason it must NOT be adopted for the demonstration,
are both recorded here.

## The embedded code, not the tree, is the mass (diagnostic)

The step-5 target is `Subst(nbar, code(Dk)) /\ SemPrf^k(code(Dk), cert, bound)
/\ Dk`. Its size is dominated by the embedded Goedel codes. Measured AST node
counts for the total-multiplication (+`pow`) diagonal:

| term                | `:u-grounding` | `:compact` |
|---------------------|---------------:|-----------:|
| system-code         |            276 |        112 |
| nbar (skeleton)     |            500 |        247 |
| code(Dk)            |          1 010 |        498 |
| **target total**    |      **6 812** |  **3 372** |

Under a fixed 3.9 GB heap the u-grounding check OOM'd. The compact check does NOT
OOM but does NOT close: at `-Xmx5g` it GC-thrashes with heap pinned ~4.8-4.9 GB
from ~+150 s onward (heartbeat: heap 3838 MB @169 s -> 4869 MB @424 s, still
running). So the memory saved by halving per-state mass is re-consumed by the
search's memory growth: the live frontier does not plateau at the ~11.6 K states
the tiny pin finishes in; memory grows with WORK done, not frontier width.
**Compact only delays the wall; it does not remove it.** The remaining lever is
therefore a STATE-COUNT reduction (deterministic ground-mode dispatch / tabling
with a partition proof), which is representation-invariant -- not more per-state
shrinkage.

## Why compact must NOT become the demonstration's representation (the confound)

Even had compact closed, adopting it would be METHODOLOGICALLY INVALID. The
executable demonstration's explanatory value rests on varying exactly ONE
parameter -- `mul`-as-relation vs `mul`-as-total-function -- and showing that this
single change flips BOT-closure (self-justifying -> non-self-justifying).
Willard's Self-Justification property is sensitive to THREE things: the deduction
method, the arithmetic expressivity of the language, and *the numeral
representation of numbers* -- because the representation controls the size of the
proofs that build Goedel codes, and the boundary is fundamentally a
representational-efficiency threshold.

Directional asymmetry:
- On the SELF-JUSTIFYING side (addition-only), a more efficient numeral
  representation can *increase total representational efficiency past the
  threshold*, crossing the boundary and forfeiting self-justification. That would
  destroy the standing falsifier (addition-only non-closure) by the representation
  change rather than by `mul`-totality -- a confound.
- On the NON-self-justifying side (mul-total), further efficiency gains do not
  change the non-SJ status, so compact is safe there in ABSOLUTE terms -- but the
  CONTRAST is between the two sides, so using compact on only one side varies two
  parameters at once, which is equally confounding.

This is not merely theoretical here. The ADR-0111 counting lemma -- the >= 5J
anti-compression floor that underwrites the correspondence and the boundary --
is `:derived-from-byte-grammar` and reasons explicitly about how compact code
terms and binary numerals normalize into the canonical encoding. The pinned
ADR-0146 boundary contrast fixes `:u-grounding` on BOTH sides
(`sjas_boundary_contrast_test.clj`), varying only `mul`-as-X. Changing that
representation would require (a) re-deriving the size floor for compact and
(b) proving the addition-only side STILL fails to close under compact (SJ
preserved). Absent both proofs, the numeral change is forbidden.

## Decision

Hold the numeral representation FIXED at `:u-grounding` (the pinned choice) for
the demonstration. Treat `:compact` in `proflog.sjas-not-dk-probe` strictly as a
memory DIAGNOSTIC (it isolated the wall as per-state formula mass), never as the
demonstration path. Attack the real-mass wall only with representation-INVARIANT
state-count reduction, which cannot confound the single-parameter attribution.

The `proflog.sjas-not-dk-probe` default is `:u-grounding`; the `compact` flag is
retained with an in-code warning that it is diagnostic and boundary-unfaithful
without the two proofs above. A watchdog (MemAvailable floor -> hard-exit) is
baked in so no detached run can lock the host.
