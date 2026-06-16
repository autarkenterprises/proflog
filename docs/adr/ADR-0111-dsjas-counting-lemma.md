# ADR-0111: D_SJAS Counting Lemma — Deriving the Proof-Object Bit-Floor

- Status: completed
- Date: 2026-06-15
- Branch: `adr-0111-dsjas-counting-lemma` (off `adr-0109-dsjas-composite-mismatch-coverage`)
- AAR: [AAR-0111](../aar/AAR-0111-dsjas-counting-lemma.md)

## Context

ADR-0108's EA-stability proof rests on **Lemma 1** (size-to-U-height): a measured
`D_SJAS` proof object exposes enough bits per function/application occurrence to
dominate Willard's conservative `5J` Conventional-Tableaux-Encoding threshold.
ADR-0104 recorded this as `dsjas-combined-size-lower-bound` with
`:status :proved-under-code-injectivity` — **prose** per-kind arguments ("every
function/application occurrence is in the measured payload"), not a derivation.
The rule-family soundness interdev note (2026-06-14) isolated Lemma 1 as the
remaining asserted gap: "Lemma 1 reduces to a counting lemma over the byte
grammar (provable, finite) plus code-injectivity (a stated assumption)."

This ADR discharges that counting lemma.

## Decision

Derive the per-occurrence and per-node bit-floor directly from the
`proflog.willard-sjas-code` byte grammar, record it as `dsjas-counting-lemma`
(`:status :derived-from-byte-grammar`) with code-injectivity as the single
explicit hypothesis, and source the EA-stability `:size-to-u-height-bound` clause
from this derived lemma instead of the asserted `dsjas-combined-size-lower-bound`.

## Derivation

Bytes are six-bit base-64 digits (`byte-base` = 64).

1. **Per-occurrence floor (canonical).** `encode-canonical-term-bytes` encodes
   every ordinary canonical `(app head args...)` term as a three-byte header —
   the `app` term-tag, the `symbol-index`, and the one-byte arity — emitted
   *before* the argument encodings. So each ordinary canonical
   function/application occurrence costs **≥ 3 bytes = 18 bits**, independent of
   its arguments. Compact code terms and binary numerals are normalized to
   first-class `code` / `num` payload terms before this ordinary-application
   branch; `var`/`par` cost 2 bytes; every formula node costs ≥ its 1 tag byte.
   Hence a canonical formula with `J` ordinary application occurrences encodes
   to **≥ 18J bits**.

2. **Per-occurrence floor (proof-wrapped).** `proof-code-bytes` re-wraps each
   canonical formula byte as a two-byte `[proof-byte-tag value]` proof datum, so
   the same occurrence costs **≥ 6 bytes = 36 bits** inside a structural proof code.

3. **Per-node floor.** Each structural proof node is a `proof-list`
   (≥ `proof-list-tag` + count) in one of two accepted formula-bearing shapes.
   The compact flat-prefix shape carries a wrapped formula byte-count before the
   formula bytes; the wide shape carries a non-empty formula-byte sub-list. Both
   shapes contribute **≥ 4 framing bytes = 24 bits** per node before the formula
   bytes themselves.

Therefore a cited theorem-code `F` with `J` occurrences is **≥ 18J bits**, and an
`N`-node structural proof tree with `J` occurrences is **≥ 24N + 36J bits**. Both
dominate `5J` (18 ≥ 5, 36 ≥ 5).

**Hypothesis (code-injectivity).** The floor bounds the *measured* object only
because distinct formulas decode from distinct codes (`:injective-public-code-reading`):
the `J` occurrences cannot be compressed below the per-occurrence header cost.
This is the same standing assumption used across the SJAS coding ADRs (the
ADR-0089 representation-sensitivity finding); ADR-0111 makes it Lemma 1's
explicit hypothesis.

## Consequence: the ADR-0102 counterexample is defeated constructively

ADR-0102 refuted measuring a bare `sjas-axiom` citation by `P` alone: the marker
is a fixed 3 bytes (18 bits) no matter how large the cited formula is, so it
cannot satisfy `size ≥ 5J` as `J` grows. The repaired `(S,F,P)` / `(S,G,F,P)`
measure (ADR-0104) counts `F`, whose encoding grows as **≥ 18J**. The executable
property encodes `eq(f^8(c), f^8(c))` (`J = 18`) and shows `F ≥ 324` bits ≫
`5J = 90`, while the bare marker stays at 18 bits.

## Consequences

- ADR-0108 Lemma 1 / the `:size-to-u-height-bound` clause is now **derived**, not
  asserted; `dsjas-combined-size-lower-bound` is retained (marked `:supersedes`)
  for the ADR-0104 record.
- Encoder changes that lower any per-constructor byte count, or that change the
  compact-code / binary-numeral normalization boundary, would weaken the floor;
  the executable properties pin the public formula-byte floor and both accepted
  structural node shapes.
- The only standing assumption remaining under Lemma 1 is code-injectivity.

## Test Obligations

- A red test requiring `audit-dsjas-counting-lemma` with the derived constants.
- An executable property: public `canonical-formula-code-bytes` spend ≥ 3 bytes
  per ordinary app-occurrence (and proof-wrapping ≥ 6 bytes), over representative
  formulas including quantifiers, bounded quantifiers, compact embedded code
  terms, and the ADR-0102 counterexample.
- An executable structural property: encode a full structural proof tree using
  both accepted node shapes, count `N` and `J`, and assert the **24N + 36J** bit
  floor.
- The EA-stability `:size-to-u-height-bound` clause sources the derived lemma.
- Existing ADR-0104/0108 size and EA-stability tests stay green.

## Exit Criteria

- The counting lemma is recorded as an executable audit with derived constants.
- The per-occurrence / per-node floors are checked by public encoder and
  structural proof-tree property tests.
- Focused correspondence tests pass; fast / extended / SJAS gates pass before commit.

## Evidence

Red:

```text
Syntax error compiling at (proflog/sjas_correspondence_test.clj).
No such var: correspondence/audit-dsjas-counting-lemma
```

Green selectors:

```text
dsjas-counting-lemma-derives-per-occurrence-bit-floor
dsjas-counting-lemma-encoder-floor-property
dsjas-counting-lemma-structural-proof-tree-floor-property
proflog.sjas-correspondence-test: 40 tests, 506 assertions, 0 failures, 0 errors
```

Final gates are recorded in [AAR-0111](../aar/AAR-0111-dsjas-counting-lemma.md).
