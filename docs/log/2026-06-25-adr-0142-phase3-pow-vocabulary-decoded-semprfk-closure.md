# ADR-0142 Phase 3 (pow vocabulary): a decoded SemPrf^k node closes in construct-and-check mode

Date: 2026-06-25
Builds on: [Phase 3 construct-and-check baseline](2026-06-23-adr-0142-phase3-construct-and-check-baseline.md),
[Phase 0 checker characterization](2026-06-23-adr-0142-phase0-checker-characterization.md)

This session discharges the third (and last) item that the Phase 3 baseline left
on the Theorem 2.3 `:open-boundary`: the **pow-vocabulary encode/decode for
encoded proof trees**. With it, the step-5 bounded-proof witness no longer closes
only through the kernel SEARCH/query path — a fully *decoded* `(neg SemPrf^k)`
interior node now closes inside a construct-and-check tableau tree over the real
multiplication system. No closure step is promoted to a BOT derivation; nothing is
overclaimed. Step 5 stays `:partial` and two genuine tree-assembly items remain.

## Problem (from the Phase 3 baseline)

Putting the step-5 witness `SemPrf^k(code(Dk), p, 2^(p+1))` inside an *encoded*
proof tree needs two things the baseline did not yet have:

1. **Decode by name.** The structural checker matches an interior node by decoding
   it and comparing to the literal branch formula (`sjas-proof-node-formula-matcho`).
   Before this change `semprfk-alpha` was profile-local and `pow` was not reserved
   at all, so both decoded to `(sym n)` and the node could never match its literal
   branch atom. The precise risk the baseline flagged: the **encoder** assigns each
   system a *compacted* index over `[declared-reserved ++ sorted-user]`, while the
   **proof-facing decoder** resolves *global* reserved indexes — appending `pow`
   must keep those two views in agreement.
2. **A structural closure rule.** The bounded `SemPrf^k` V-route interpretation
   lived only in the kernel SEARCH hook (`sjas-semprfk-alpha-closeo`), not in the
   `structural-proof-valid?` disjunction the construct-and-check path uses.

## Delivered (checker-verified, gates green)

- **`pow` appended to `sjas-code/reserved-coding-symbols`** and **`semprfk-alpha`
  promoted out of `profile-local-reserved-symbols`** so both recover a semantic
  name in proof-facing mode. To keep the encoder's compacted index equal to the
  decoder's global reserved index, the boundary cluster
  (`mul finax4 willard-map semprfk-alpha semprf-alpha pow`) is kept **contiguous**
  and `dsjas-tab2-proof` is moved **last** — so a multiplication system (which
  declares the cluster but not `dsjas-tab2-proof`) has no compaction gap below the
  cluster. `tab2-boundary-proof-symbol` is reworked to use the *compacted* index a
  Tab-2 system actually carries (prefix + 1, skipping the undeclared cluster), via
  the new `tab2-multiplication-cluster-symbols` set.
- **`sjas-semprfk-alpha-structural-closeo`** (new kernel relation), wired into the
  structural close disjunction. It is the construct-and-check counterpart of
  `sjas-semprfk-alpha-closeo`: it reuses the **same** executable bounded-proof core
  (`sjas-semprfk-alpha-coreo` — proof-code validation through the decoded
  structural checker plus the Definition 2.1 `Log` bound), omitting only the
  search-layer `(profiled ...)` proof marker. As a leaf-closing disjunct it can
  only close *more* leaves; it cannot make a previously-closing leaf fail.
- **`proflog.sjas-tree-builder/decode-proof-facing`** (new helper): encodes a
  formula to a system's public code and reads it back through the proof-facing
  decoder, to characterize which symbols a constructed node presents to the checker.
- **`proflog.sjas-semprfk-tree-closure-test`** (new, 3 tests / 4 assertions, green):
  - `semprfk-alpha` and `pow` decode **by name** inside an encoded `SemPrf^k`
    formula (no `(sym n)`);
  - a decoded `(neg SemPrf^k)` single-node tree with the symbolic `2^(p+1)` bound
    is **checker-accepted** via the structural V-route;
  - a **too-small** `2^p` bound gives `Log(2^p,1)=p`, so `proof<p` fails and the
    negation does **not** close — the bound stays genuinely checked, not assumed.
- **`proflog.sjas-tree-builder-test`** updated: a `semprfk-alpha` pos/neg clash now
  closes (it is a named primitive), while a still-profile-local relation
  (`willard-map`) does not — replacing the prior assertion that `semprfk-alpha`
  closed only by interpretation.

## Gates (the no-mis-decode falsifier)

The global-naming change is exactly the kind that can silently shift a user
relation's index into a reserved slot. The falsifier is the full SJAS gate:

- SJAS not-slow: **pass=1445 fail=0 error=0**
- fast: **273 tests / 2343 assertions, 0 failures**
- targeted: `sjas-semprfk-tree-closure-test` + `sjas-tree-builder-test` +
  `sjas-theorem23-closure-test` green.

Green confirms the encoder's compacted view and the proof-facing decoder's global
view agree across every exercised system.

## Honest status

`theorem23-closure-status` now records `:phase3-pow-vocabulary` under
`:resolved-since-aar`, adds
`:step5-decoded-semprfk-node-closes-construct-and-check` to `:checker-accepted`,
and drops `:pow-vocabulary-encode-decode-for-encoded-proof-trees` from
`:open-boundary :remaining`. Step 5 remains `:partial`. The open boundary is down
to two **pure tree-assembly** items, with no new trusted rule and no bound
obstruction:

1. the cut-free combination trees for steps 1/3/4 and condition B;
2. the not-Dk tree with its Q-disproof of the false Pi1 sub-formula.

## Next

Build the not-Dk tree (step 5) end-to-end with `proflog.sjas-tree-builder`: its
hard sub-part — the decoded bounded-proof leaf — is now checker-accepted, so what
remains is assembling the surrounding `(neg Dk)` instantiation and the Q-disproof
branch. Then the steps 1/3/4/B combination trees.
