# ADR-0090: Core.logic Ground-Term Walk Fast Path

- Status: in progress
- Date: 2026-06-10
- Branch: `adr-0088-sjas-runtime-rebaseline`
- AAR: pending

## Context

ADR-0088's bisect probe (`proflog.sjas-runtime-probe`) attributed the SJAS
whole-program query grind precisely: `beta` queries through the full
generated program complete in seconds (tableau0 `4.8 s`, level1 `3.0 s`
query time), while raw `axiom-member(s, group3-code)` citations at fuel 64
exceed the 15-minute probe cap for both profiles. JVM stack samples of the
grinding probes show the main thread inside core.logic's `walk-term` over
`ISeq` (logic.clj `doall`/`map` rebuild, per-node protocol dispatch through
`MethodImplCache`), and the earlier 137-CPU-minute `subst-prf` negative was
hot in `LVar.hashCode` under substitution lookups. The workload shape is
characteristic: SJAS code terms are large, fully ground structures
(base-64 byte lists whose bytes are binary numeral terms — thousands of
nodes), bound once and then re-walked at every decode step, unification,
and closure attempt. Each `walk*` rebuilds the entire structure with fresh
sequence cells even though nothing can change, and each occurs check
rescans it even though a ground term can contain no variable.

The 2026-05-01 ADR-0032 probe (`adr32/core-logic-walk-reify-memo`) showed
that *identity-memoizing* `walk*` regresses list-family workloads — cache
overhead without avoided work — and explicitly did not retry structural
sharing. This ADR takes the complementary approach: avoid the work itself
for terms that provably cannot change.

Per the ADR-0088 doctrine, the remedy belongs at the core.logic layer
rather than as more complex SJAS/Proflog code, must stay elegant and
preserve miniKanren's clean semantics, and as a core.logic patch requires
this dedicated ADR and AAR (the rule recorded since the equality research
summary and honored by ADR-0075).

## Decision

Patch the vendored `core.logic-1.0.1` overlay (mirrored to the opt-in
`core.logic-1.1.1` profile copy, as ADR-0075 did) with three coordinated,
semantics-preserving changes:

1. **Ground-term recognition.** A private worklist scanner
   `-ground-term?` decides whether a fully walked term is ground. It is
   deliberately conservative: logic variables fail it; `LCons`, `ISeq`,
   and persistent vectors recurse; `nil`, booleans, characters, numbers,
   strings, keywords, and symbols pass; every other type — maps, sets,
   records, and in particular nominal `tie`/`nom`/`susp` structures —
   fails it, so nominal machinery is never tagged or skipped. A term whose
   metadata carries `::ground` is accepted without descent, so scans
   amortize across shared subtrees.
2. **Tag-on-bind and tagged short-circuits.** `ext` tags a ground value
   with `::ground` metadata before the occurs check and extension, so the
   one groundness scan replaces the occurs-check scan it makes
   unnecessary; `occurs-check-worklist` skips `::ground`-tagged subtrees
   (a ground term cannot contain the variable); `walk*` returns
   `::ground`-tagged terms immediately and tags its own result when that
   result is ground and can carry metadata. Because substitution values
   are stored tagged, every later walk of the same binding is constant
   time.
3. **Copy-on-write rebuilds.** The `ISeq`, `IPersistentVector`, and
   `LCons` `walk-term` implementations return the original object when
   every child walks to an `identical?` child, eliminating the per-walk
   reallocation of unchanged structure. Results are `=`-identical to the
   previous implementation in all cases.

Soundness rests on one invariant: a ground term is a fixed point of
`walk*` under every substitution and can never come to contain a
variable, so returning it unscanned, unrebuilt, and unsearched is
observationally identical to the previous behavior. Metadata does not
participate in Clojure equality or hashing, so tagging cannot affect
unification, disequality, tabling keys, or reification. Types that cannot
carry metadata (numbers, strings, keywords) are trivially ground leaves
and need no tag.

## Consequences

- Repeated walks and occurs checks over bound SJAS code terms drop from
  linear in term size to constant time after the first binding; first
  walks lose the rebuild allocation churn.
- All other workloads see at most one cheap identity comparison per
  rebuilt node and one abort-on-first-variable scan per extension, in
  exchange for the removed occurs-check scan when the value is ground;
  the ADR-0032 memoization regression mode (cache without avoided work)
  is structurally avoided.
- The patch stays inside the two vendored overlay files; upstream
  core.logic semantics, the nominal subsystem, and the constraint store
  are untouched.

## Test Obligations

- New focused regression `proflog.core-logic-ground-walk-test`, red
  before the patch: repeated `walk*` of a bound ground tree returns the
  identical object; copy-on-write returns identical objects for unchanged
  children; unification, disequality, and the `x = f(x)` occurs rejection
  behave unchanged on tagged terms; a term containing an unbound variable
  is never tagged.
- The ADR-0088 bisect probe is the runtime evidence: both `axiom-member`
  cases must fall from exceeding the 15-minute cap to ordinary focused
  runtimes, with `proofs=1` preserved, and the `beta` cases must not
  regress.
- Semantic regression: the ADR-0075 occurs-check selector, the ADR-0087
  selectors, Level-1/tableau0 citation and fixed-point selectors, the
  128-assertion profile source audit, `lein test-proflog-fast`, and
  `lein test-proflog-extended`.
- The ADR-0088 namespace sweep then re-baselines every SJAS var on the
  patched tree.

## Exit Criteria

- Probe and selector evidence recorded in AAR-0090; no semantic selector
  changes behavior.
- Both vendored overlays carry the same patch.
- ADR-0088's baseline tables are produced on the patched state.
