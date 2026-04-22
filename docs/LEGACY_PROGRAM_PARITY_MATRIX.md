# Legacy Program Parity Matrix

Date: 2026-04-22
Branch: `adr-0009-legacy-program-closure`
Related ADRs:

- `docs/adr/ADR-0008-test-gap-closure.md`
- `docs/adr/ADR-0009-legacy-program-closure.md`

This matrix tracks end-to-end legacy program families against the current
greenfield suite. It deliberately excludes the legacy micro-regression sections
that are about raw tableau or equality rule behavior rather than named program
families.

## Status Key

- `Comparable`: a greenfield family exists at roughly the same semantic layer.
- `Partial`: a greenfield family exists, but legacy still covers materially
  deeper forward, failure, inverse, or recursive behavior.
- `Absent`: no comparable greenfield family exists yet.
- `Deferred`: explicitly tracked as future experimental work, not yet promoted.

## Matrix

| Legacy family | Legacy refs | Greenfield refs | Status | Current note |
|---|---|---|---|---|
| Fitting `P1` even/odd | [X01-X05](../test/cljtap/alphaleantap_ep_test.clj) | [query_test.clj](../test/proflog/query_test.clj), [quantified_programs_test.clj](../test/proflog/quantified_programs_test.clj) | Comparable | Greenfield now covers direct and deeper quantified execution for the original `forall`-based odd clause. |
| Inline Nim `P2` | [J-family / nim-program](../test/cljtap/alphaleantap_ep_test.clj), [MV05-MV06](../test/cljtap/alphaleantap_ep_test.clj) | [query_test.clj](../test/proflog/query_test.clj), [answers_test.clj](../test/proflog/answers_test.clj), [nim_synthesis_test.clj](../test/proflog/nim_synthesis_test.clj) | Comparable | Greenfield covers direct truth/falsity, bounded answer behavior, and deeper winning-move witnesses. |
| Factored `move` warning | [move-program / MV01-MV06](../test/cljtap/alphaleantap_ep_test.clj) | [query_test.clj](../test/proflog/query_test.clj) | Comparable | Greenfield now distinguishes decidable ground `move/2` from the unresolved factored `win/1` cases, and contrasts both against the inline Nim program. |
| `member` list relation | [Q04-Q07](../test/cljtap/alphaleantap_ep_test.clj) | [list_programs_test.clj](../test/proflog/list_programs_test.clj) | Comparable | Greenfield now covers direct hit, recursive hit, empty-list failure, and non-member failure, which meets or exceeds the legacy semantic surface for `member`. |
| `append` / `reverse` list relations | [Y01-Y12](../test/cljtap/alphaleantap_ep_test.clj) | [list_programs_test.clj](../test/proflog/list_programs_test.clj), [synthesis_modes_test.clj](../test/proflog/synthesis_modes_test.clj), [answers_test.clj](../test/proflog/answers_test.clj) | Partial | Legacy still completes `Y10` and `Y12` in about `27-28 s`. Greenfield ground checks are fine, but diagnostics now show the open-query gap precisely: `reverse([a,b], r)` exports only the first symbolic frontier at `call-depth 1` (`~1.8 s`) and yields no raw proof at `call-depth 2` after `~54.7 s`; `append(a,b,[a,b,c])` reaches the base split at `~28.9 s` and the first recursive split at `~41.6 s`, but not the deeper splits. |
| Nested and deep `append` list families | [Y13-Y15](../test/cljtap/alphaleantap_ep_test.clj), [Z01-Z04](../test/cljtap/alphaleantap_ep_test.clj) | [list_programs_test.clj](../test/proflog/list_programs_test.clj), [synthesis_modes_test.clj](../test/proflog/synthesis_modes_test.clj) | Partial | Legacy still completes `Y15` (`17.76 s`) and `Z04` (`17.06 s`) on the current machine. Greenfield currently covers only the first nested forward and suffix answers; inverse nested splits remain beyond parity, and the depth-3 forward answer family is still unresolved after a `>360000 ms` probe. |
| Transitive closure `tc` | [TC01-TC06](../test/cljtap/alphaleantap_ep_test.clj) | [integration_families_test.clj](../test/proflog/integration_families_test.clj) | Comparable | Greenfield now covers direct edges, the recursive `a -> c` path, and the simple no-path cases on the inline small graph. |
| Peano `plus` | [PA01-PA23](../test/cljtap/alphaleantap_ep_test.clj) | [integration_families_test.clj](../test/proflog/integration_families_test.clj), [synthesis_modes_test.clj](../test/proflog/synthesis_modes_test.clj) | Comparable | Greenfield now combines multi-step ground truth/falsity checks with symbolic open and partial answer-family coverage. |
| Quantified singleton and mixed clause bodies | [X-family](../test/cljtap/alphaleantap_ep_test.clj), quantified spec families below | [quantified_programs_test.clj](../test/proflog/quantified_programs_test.clj) | Comparable | Greenfield now executes direct quantified clause bodies and includes finite-domain specification families such as `sorted2`, `subset`, and `acyclic`. |
| Sortedness `sorted2` | [SO01-SO05](../test/cljtap/alphaleantap_ep_test.clj) | [quantified_programs_test.clj](../test/proflog/quantified_programs_test.clj) | Comparable | Greenfield now covers the legacy empty, singleton, sorted, unsorted, and two-element sorted cases for `sorted2`. |
| Subset relations | [SS01-SS03](../test/cljtap/alphaleantap_ep_test.clj) | [quantified_programs_test.clj](../test/proflog/quantified_programs_test.clj) | Comparable | Greenfield now covers the legacy true, false, and reflexive subset cases over the finite domain `{a, b, c}`. |
| Graph properties `acyclic` | [GP01-GP03](../test/cljtap/alphaleantap_ep_test.clj) | [quantified_programs_test.clj](../test/proflog/quantified_programs_test.clj) | Comparable | Greenfield now covers the acyclic `a→b→c` case and the two cyclic counterexamples `a→b→a` and `a→b→c→a`. |
| Group verifier `GV` | [GV01-GV09](../test/cljtap/alphaleantap_ep_test.clj) | none | Deferred | ADR-0008 treats this as a future greenfield experiment, not current baseline parity work. |
| Finite-domain reasoning `FD` | [FD01-FD07](../test/cljtap/alphaleantap_ep_test.clj) | none | Deferred | ADR-0008 treats this as a future greenfield experiment, not current baseline parity work. |

## Immediate Closure Order

1. Produce worked examples for the greenfield families already present:
   `P1`, inline Nim, quantified clause bodies, `tc`, `plus`, list families, and
   structured answer-mode families.
2. Deepen the `Partial` rows that are already in greenfield:
   `member`, `append`/`reverse`, nested/deep `append`, `tc`, `plus`, and richer
   quantified specification programs.
3. Build the `Absent` rows in mission-relevant order:
   `move` warning, `sorted2`, `subset`, and `acyclic`.
4. Revisit the `Deferred` rows only after the earlier closures expose whether
   they are semantic work, performance work, or both.
