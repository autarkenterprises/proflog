# AAR-0111: D_SJAS Counting Lemma — Deriving the Proof-Object Bit-Floor

- Date: 2026-06-15
- ADR: [ADR-0111](../adr/ADR-0111-dsjas-counting-lemma.md)
- Branch: `adr-0111-dsjas-counting-lemma` (off `adr-0109-dsjas-composite-mismatch-coverage`)

## Outcome

ADR-0111 is complete. ADR-0108's **Lemma 1** (size-to-U-height) is now **derived
from the `proflog.willard-sjas-code` byte grammar** instead of asserted as
`:proved-under-code-injectivity` prose.

The derivation reads three facts off the encoders and composes them:

- `encode-canonical-term-bytes` spends a fixed **3-byte header** (`app`-tag,
  `symbol-index`, arity) on every `(app …)` occurrence → **18 bits/occurrence**
  in a canonical formula → cited theorem-code `F` is **≥ 18J bits**.
- `proof-code-bytes` re-wraps each formula byte as `[proof-byte-tag value]` (×2)
  → **36 bits/occurrence** when proof-code-wrapped.
- each structural node is a `proof-list` over a non-empty formula sub-list →
  **≥ 4 framing bytes = 24 bits/node**.

So structural trees are **≥ 24N + 36J** bits and citations **≥ 18J** bits, both
dominating Willard's `5J`. The one remaining hypothesis is **code-injectivity**
(`:injective-public-code-reading`), now stated explicitly as Lemma 1's premise.

`dsjas-counting-lemma` (`:status :derived-from-byte-grammar`) records the derived
constants; the EA-stability `:size-to-u-height-bound` clause now sources it
(`:status :derived-from-byte-grammar`) rather than the asserted predecessor,
which is retained (`:supersedes`) for the ADR-0104 record.

The ADR-0102 counterexample is defeated **constructively** by an executable
property: encoding `eq(f^8(c), f^8(c))` (`J = 18`) yields a theorem-code of
≥ 324 bits ≫ `5J = 90`, while the bare `sjas-axiom` marker stays at a fixed
18 bits regardless of `J`.

## Evidence

Red:

```text
No such var: correspondence/audit-dsjas-counting-lemma
```

Green (focused), then the broad gates:

```text
proflog.sjas-correspondence-test   39 tests / 489 assertions, 0 failures, 0 errors
lein test-proflog-fast             <recorded at commit>
lein test-proflog-extended         <recorded at commit>
lein test-proflog-sjas             <recorded at commit>
```

## Follow-up

- The per-occurrence (3-byte/18-bit) and proof-wrapping (×2/36-bit) floors are
  pinned by an executable property; the structural `24N + 36J` node floor is
  grounded by the canonical + proof-wrapping composition. An end-to-end property
  (encode a full structural proof tree, count `N` and `J`, assert ≥ 24N + 36J)
  would strengthen the empirical check beyond the per-occurrence pieces.
- **Code-injectivity** remains the single standing hypothesis under Lemma 1,
  shared with the ADR-0089 representation-sensitivity finding; discharging or
  bounding it is the natural successor.
- Epistemic status: a grammar-level derivation with an executable floor check,
  not machine-checked — the same standing as the rest of the `D_SJAS` line.
