# SJAS Walked System-Code Reconstruction

## Context

While auditing ADR-0073 Track 1 independently of the long SelfCons runtime
probe, the proof predicate showed an equality-state asymmetry: `proof-code` and
`theorem-code` were read through the current branch `sigma`, but structural
`AxiomConj(system-code)` reconstruction and one `subst-prf/4` system-code
validation branch still read `system-code` from empty equality state.

That was not a host shortcut for top-level ground calls, but it was incomplete
relational handling for nested proof predicates under equality.

## Change

- Added ADR-0084.
- Added proof-free walked system-code helpers.
- Updated `sjas-tableau-proof-coreo` and `sjas-subst-prf-coreo` structural
  branches to reconstruct `AxiomConj` through branch equality state.
- Updated the `subst-prf/4` substitution-result axiom branch to validate walked
  system-code.
- Passed the walked system-code term into nested structural proof checking.

## Evidence

Red:

```text
sjas-proof-predicate-system-code-reconstruction-walks-equality-state
Ran 1 tests containing 5 assertions.
5 failures, 0 errors.
elapsed 0:36.78 maxrss 242168KB
```

Green:

```text
sjas-proof-predicate-system-code-reconstruction-walks-equality-state
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 0:46.47 maxrss 349264KB

sjas-subst-prf-substitution-axiom-branch-validates-system-code
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 0:36.28 maxrss 245656KB

sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 0:32.84 maxrss 286780KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 0:52.22 maxrss 439012KB

lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 3:38.29 maxrss 424912KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 8:46.17 maxrss 616812KB
```

## Notes

This is a Track 1 correctness/internalization repair. It is not an optimization
of the long public SelfCons probe.
