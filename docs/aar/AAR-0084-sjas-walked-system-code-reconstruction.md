# AAR-0084: SJAS Walked System-Code Reconstruction

- Date: 2026-06-09
- ADR: [ADR-0084](../adr/ADR-0084-sjas-walked-system-code-reconstruction.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Closed a Track 1 relationality gap in recursive proof-predicate handling.
`tableau-proof/3` and `subst-prf/4` structural branches now reconstruct
`AxiomConj(system-code)` after reading `system-code` through branch equality
state. The `subst-prf/4` substitution-result axiom branch likewise validates
the walked system code before checking the substitution relation. Nested proof
checking receives the walked system-code term.

This makes the implemented proof predicate closer to a relation over supplied
codes rather than a top-level ground-only path.

## Evidence

The focused test failed red before implementation:

```text
sjas-proof-predicate-system-code-reconstruction-walks-equality-state
Ran 1 tests containing 5 assertions.
5 failures, 0 errors.
elapsed 0:36.78 maxrss 242168KB
```

After adding the walked helpers and updating the proof branches, the focused
test passed, including semantic checks that a system-code variable already bound
in `sigma` can be consumed:

```text
sjas-proof-predicate-system-code-reconstruction-walks-equality-state
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 0:46.47 maxrss 349264KB
```

Related focused checks passed:

```text
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
```

The post-change broad gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 3:38.29 maxrss 424912KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 8:46.17 maxrss 616812KB
```

## Follow-up

Continue monitoring the public SelfCons Track 1 MVP probe. This ADR removes a
known relationality gap; it does not by itself establish that the long concrete
SelfCons selector has completed.
