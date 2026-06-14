# AAR-0109: Mode-Directed Ground-Before-Decode (Width-Reduction #1)

- Date: 2026-06-14
- ADR: [ADR-0109](../adr/ADR-0109-mode-directed-ground-before-decode.md)
- Branch: `adr-0107-pure-indexed-lookup`

## Outcome

Made the SJAS formula/term byte decoders **mode-directed** by a pure conjunction
reorder, so a ground formula drives the decode forward instead of enumerating —
ADR-0106 §C/§D #1, with no `project`/`conda`/host cut.

- **Defect.** Every decoder branch placed the constructor `==` *last*, after the
  recursive byte-decodes. Since core.logic conjunction is sequential per answer,
  a ground `formula` could not constrain the sub-decodes until after they had
  enumerated. Backward decode (ground formula → bytes) of even `0 = 0` did **not**
  complete in 70 s.

- **Fix.** Constructor `==` moved to the front of all 15 `decode-formula-byteso`
  branches, the `decode-term-byteso` var/par branches, and `decode-app-termo`;
  the output cons bound before recursion in `parse-code-payload-byteso` /
  `parse-term-list-byteso`; term `==` ahead of the payload parse (inside the
  existing payload `fresh`) in `decode-natural-bodyo` / `decode-embedded-code-bodyo`.
  All pure reordering — answer-set-identical by construction.

- **Result.** Backward decode now terminates **deterministically** with the
  unique encoding (`proflog.decode-mode-directed-test`, fast gate: forward
  unchanged, backward terminates, round-trip agrees). Measured: backward
  `(eq 0 0)` **0.33 ms** and `(and (eq 0 0) (neg (var 1)))` **1.19 ms** (both
  were non-terminating before — a 70 s `run 1` was killed); forward unchanged at
  ~0.94 ms.

## What broke and how it was fixed (honest)

The first attempt **hoisted** the term `==` above the two header-length checks in
the `*-bodyo` decoders. That tripped the source-structure guard
`sjas-embedded-payload-decoders-check-header-before-payload-fresh` (2 failures,
SJAS pass=1058) — a deliberate forward-mode invariant: reject wrong length
headers *before* allocating payload state. The reorder was narrowed to *inside*
the payload `fresh` (term `==` before parse, headers still first), restoring the
guard while keeping the backward win. Lesson: a "pure reorder" can still violate
a **codified ordering invariant**; the regression suite (not just my reasoning)
is what caught it.

## Evidence

- `proflog.decode-mode-directed-test` green (RED was the 70 s non-completion).
- Fast gate + ADR-0093 canonical green (198 tests / 1047 assertions / 0).
- SJAS not-slow gate: `pass=1060 fail=0 error=0` after the guard fix (restored
  from the transient `pass=1058 fail=2`).

## Follow-up

- The full subst-prf negative-wall payoff needs the **proof checker** to ground
  each node's target formula before decoding its bytes
  (`sjas-proof-check-programo` / `decode-structural-proof-bytes-coreo` goal
  order) — the named successor.
- Apply the same mechanical reorder to the parallel `decode-syntax-*` family
  (deferred here to keep the change reviewable).
- Quantify the grind-level effect on a tractable proxy (bounded negative /
  positive proof-check) once the checker propagates groundness.
