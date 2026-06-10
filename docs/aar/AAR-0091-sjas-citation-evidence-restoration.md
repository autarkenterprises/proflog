# AAR-0091: SJAS Tableau-Proof Citation Evidence Restoration

- Date: 2026-06-10
- ADR: [ADR-0091](../adr/ADR-0091-sjas-citation-evidence-restoration.md)
- Branch: `adr-0088-sjas-runtime-rebaseline`

## Outcome

Public `tableau-proof/3` citation answers are evidence-bearing again. The
closure was refactored around a shared object preamble
(`sjas-tableau-proof-callo`) with split proof-free branches
(`sjas-tableau-proof-certificate-coreo`, `sjas-tableau-proof-structural-coreo`;
`sjas-tableau-proof-coreo` remains their disjunction for recursive
proof-predicate leaves). The closure's certificate branch validates
membership through the proof-bearing `sjas-walked-axiom-membero` and nests
its evidence inside the profile wrapper, reversing the `e248c8b` marker
summary now that ADR-0090 pays the reification cost. Structural
certificates keep the plain wrapper; their inspectable evidence is the
decoded proof tree itself.

The profile source audit evolved with the design while preserving every
intent: the core-source extraction window now starts at the shared
preamble, so the read-once, certificate-recognition, structural-decode,
negated-theorem, and walked-reconstruction patterns are asserted across
the split relations, and the closure is asserted to destructure through
the preamble, validate citations through the proof-bearing object
relation, nest the membership evidence, and delegate structural answers to
the proof-free object relation.

## Evidence

Red (first full-namespace run since the ADR-0086-era slowdown, ADR-0088
sweep): `sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures`
and `sjas-tableau-proof-cites-fixed-axiom-groups-from-system-code` failed
with bare-marker proofs lacking `sjas-system-reflected-axiom`,
`sjas-system-group-zero-axiom`, and `sjas-code-arg` steps; the identical
failure reproduces at `97f70a7` (pre-ADR-0090), and `git log -S`
attributes the marker to `e248c8b`.

Green after the refactor (with ADR-0092 in the same tree):

```text
lein test :only <the two failing vars>
                proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies
                proflog.willard-sjas-test/sjas-system-rejects-non-pi-star-1-reflected-basis
Ran 4 tests containing 29 assertions.
0 failures, 0 errors.
elapsed 0:56.72 maxrss 368360KB

semantic batch (ADR-0087 selectors, both Group-3 citations, evolved
131-assertion source audit, injected-fact guards):
Ran 8 tests containing 147 assertions.
0 failures, 0 errors.
elapsed 1:03.61 maxrss 375288KB
```

Broad gates (shared with ADR-0092): `lein test-proflog-fast` Ran 171 tests containing 679 assertions. 0 failures, elapsed 3:40.68; `lein test-proflog-extended` Ran 68 tests containing 203 assertions. 0 failures, elapsed 8:58.27.

## Follow-up

- `sjas-subst-prf-closeo` retains its summarized wrapper by decision; its
  contract is defined by the current subst-prf regressions and any
  widening goes through the Track 2a relevance matrix.
