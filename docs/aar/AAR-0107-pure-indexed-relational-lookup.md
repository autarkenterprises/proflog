# AAR-0107: Pure Indexed Relational Lookup (Width-Reduction #2)

- Date: 2026-06-14
- ADR: [ADR-0107](../adr/ADR-0107-pure-indexed-relational-lookup.md)
- Branch: `adr-0107-pure-indexed-lookup`
- **Status: REVERTED** — built, measured, found to give no speedup (a regression),
  reverted. Kept as a recorded negative result so this line is not re-attempted.

## TL;DR (why reverted)

`int-indexo` is correct and pure but provides **no speedup** — it is a regression:

- **Microbench:** ~2× slower than the linear `or*` on a ground-key lookup
  (27.2 vs 12.3 ms/op).
- **Extended gate:** no improvement; construction-heavy tests regressed ~0.8×.
- **Slow suite:** no improvement (8/10 pass at 1.6–7 s under int-indexo, same as
  baseline). The two heaviest (`subst-prf-checks-selfcons-fixed-point-certificate`,
  `…-rejects-selfcons-complement-axiom-certificate`) **do not complete under either
  version** — int-indexo timed out at 240 s, but the reverted/linear code also
  times out at 600 s, so they are *inherently* near/at the wall and **not** a #2
  artefact. (The revert thus rests on the microbench + extended, not the slow
  timeouts.)

The ADR-0106 §C #2 premise was wrong: a *ground*-key linear `or*` already fails
every wrong entry at its first `==`, so it never "opens a choice point per entry"
and was never the cost. The real grind is the *free*-key decode enumeration,
addressed by #1 / the [ADR-0110](../adr/ADR-0110-mode-directed-ground-before-decode.md)
proof-checker proposal — not the table lookup. The `code-constructor-buildo`
re-expression, the `int-indexo` primitive, the contract test, the agreement test,
and the project.clj entries were all reverted; #1 (ADR-0110) is independent and
retained.

## Outcome (as built, before revert)

Added a **pure relational indexed lookup** to the vendored core.logic overlay and
re-expressed the largest integer-keyed SJAS static table over it, with zero
*correctness* regression and an answer-set agreement test — discharging ADR-0106
§C/§D #2's purity bar (no `project`/`conda`/host cut) but **not** its performance
goal.

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

- **#2 was reverted** (decision made 2026-06-14). The `code-constructor-buildo`
  re-expression, the `int-indexo` primitive, both tests, and the project.clj
  entries were removed; the linear `or*` restored; gates re-run green. If the
  indexed-lookup idea is ever revisited, the *only* viable form is a **non-`fd`
  trie** (plain unification over a bit/digit decomposition, to cut the per-step
  constant) **and** a table far larger than 4096 where the asymptotics dominate —
  the `fd` version measured here is a dead end and should not be rebuilt.
- The real tractability lever is the free-key **decode enumeration**, addressed by
  #1 and the proof-checker proposal in
  [ADR-0110](../adr/ADR-0110-mode-directed-ground-before-decode.md) — not the
  table lookup.
- The primitive is already correct for non-contiguous / sparse keys; only the leaf
  count is `2^width` (assumes a small max key).
