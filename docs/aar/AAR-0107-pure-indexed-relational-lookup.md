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
  O(12) trie descent on a ground byte-count instead of an O(4096) scan, **no
  choice point** on the ground key. The other call sites (`code-constructoro`
  forward is symbol-keyed) are left for a follow-up code-N↔N variant.

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

- The grind-level payoff of #2 is realised only once #1
  ([ADR-0109](../adr/ADR-0109-mode-directed-ground-before-decode.md)) makes the
  table keys ground at lookup time; #2 here is the enabling, correctness-preserving
  O(N)→O(log N) infrastructure on ground-key lookups.
- A symbol-keyed (`code-N` ↔ N) variant to also determinise `code-constructoro`
  forward and the other `static-table-entryo` call sites.
- Extend the trie primitive to non-contiguous / sparse key sets if a future table
  needs it (the current build assumes a small max key; it is already correct for
  sparse keys, only the leaf count is `2^width`).
