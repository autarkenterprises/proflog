# AAR-0107: Pure Indexed Relational Lookup (Width-Reduction #2)

- Date: 2026-06-14
- ADR: [ADR-0107](../adr/ADR-0107-pure-indexed-relational-lookup.md)
- Branch: `adr-0107-pure-indexed-lookup`

## Outcome

Added a **pure relational indexed lookup** to the vendored core.logic overlay and
re-expressed the largest integer-keyed SJAS static table over it, with zero
regression and an answer-set agreement test — discharging ADR-0106 §C/§D #2
without any `project`/`conda`/host cut.

- **The primitive.** `clojure.core.logic.index/int-indexo` (new namespace, depends
  on `clojure.core.logic.fd`): a fixed finite table with non-negative integer
  keys, compiled once into a perfect bit-trie of ground logic terms. A ground key
  descends the trie **deterministically by unification** (the wrong child fails at
  the discriminating `==`, no cut); a free key/value enumerates the leaves. One
  relation, deterministic-forward and enumerating-backward, **no groundness
  inspection and no double-count** — structural determinism, not committed choice.
  This is the "more expressive data structure added *inside* core.logic" of
  ADR-0106 §D.

- **Contract (TDD, isolation-first).** `proflog.core-logic-indexed-lookup-test`
  (fast gate, 8 assertions): forward (ground key → its value, exactly one
  answer), absent key fails, backward (value → key), free enumeration (every
  entry exactly once), and agreement with a linear `or*` baseline in every mode.
  Red before the primitive existed, green after.

- **Integration.** `code-constructor-buildo` (byte-count → constructor) was a
  linear `or*` over the **4096-entry** `code-functions` table (keys 0..4095, all
  distinct). Re-expressed as a single `int-indexo` over a byte-count-keyed index:
  an O(12) trie descent on a ground byte-count instead of an O(4096) scan.

- **Negative result (measured).** The re-expression is **~2× slower**, not faster
  (int-indexo 27.2 ms/op vs linear 12.3 ms/op on a forced, warmed ground-key
  lookup). The `fd` constant outweighs the asymptotic win at N=4096, and a ground
  key already fails the linear `or*`'s wrong branches at the first `==` (no
  residual choice points for the trie to remove). The ADR-0106 §C #2 premise — that
  a ground-key linear lookup "opens a choice point per entry" — was wrong; the
  table scan was never the cost. So #2 stands as a **correct, pure, but
  non-performant** primitive; an actual win needs a non-`fd` trie (cut the
  per-step constant) or a much larger table. See ADR-0107 Measurement.

## Evidence

- `proflog.code-constructor-index-test` (extended gate, 19 assertions): the
  re-expressed relation has **exactly the same answer set** as a faithful linear
  baseline over the same source table — forward (sampled keys, single answer, no
  double-count), absent keys, backward, and the full 4096-entry free enumeration
  (sound + complete). Green.
- ADR-0093 canonical suite + fast gate green **with the re-expression** (197→198
  tests counting the new fast tests, 0 failures); SJAS not-slow gate green
  end-to-end (`pass=1060 fail=0 error=0`) — the constructor lookup is exercised by
  the code-reconstruction / ADR-0095 synthesis paths.

## Follow-up

- **Reconsider #2.** As built it is a measured ~2× regression with no current
  benefit (the lookup was never the bottleneck). Options: (a) re-implement the
  trie with **plain unification over a bit/digit decomposition, no `fd`**, to cut
  the per-step constant and see if it then beats the linear scan; (b) **revert**
  the `code-constructor-buildo` re-expression and keep `int-indexo` only as a
  vendored primitive for a future large/sparse table; (c) leave it (correct,
  pure, negligible gate cost) pending a table where the asymptotics matter.
  Decision deferred to the user.
- The real tractability lever is the free-key **decode enumeration**, addressed by
  #1 and the proof-checker proposal in
  [ADR-0109](../adr/ADR-0109-mode-directed-ground-before-decode.md) — not the
  table lookup.
- The primitive is already correct for non-contiguous / sparse keys; only the leaf
  count is `2^width` (assumes a small max key).
