# ADR-0085: SJAS Structural Quantifier Sibling Scheduling

- Status: completed
- Date: 2026-06-09
- Branch: `adr-0073-sjas-correspondence-program`
- Parent: [ADR-0073](ADR-0073-sjas-internalization-correspondence-program.md)

## Context

ADR-0073 Track 1 requires the public literature-style `tableau-proof/3(s,t,p)`
predicate to validate the ordinary-tableau Group-3 SelfCons certificate through
the arithmeticized, formula-bearing structural checker.

After ADR-0084, the proof predicate has no known proof-facing host shortcut.
The remaining public SelfCons gate is live for multiple hours. Focused probes
show the delay is already present in the lower-level structural SelfCons core:

```text
timeout 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
exit 124, elapsed 3:00.01

timeout 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau
exit 124, elapsed 3:00.00

timeout 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-tableau
exit 124, elapsed 3:00.00
```

The concrete SelfCons core proof is small: the target is 77 formula-code bytes
and the proof is 471 proof-code bytes. The delay is therefore a scheduling
problem, not merely large input size.

The structural checker originally delayed sibling formulas as raw formulas and
selected them later under the current branch environment. That made a sibling
enqueued before a quantifier vulnerable to bindings introduced while proving a
different sibling. Focused probes showed this as a timeout in the small
Group-3/negated-SelfCons core.

After the sibling-scope repair, the lower-level SelfCons core passed, but the
larger in-memory target still stalled in `AxiomConj(system-code)`
reconstruction. That was a second scheduling problem: Tableau-0 Group-3
antecedent reconstruction discarded the presented public `system-code` term
after reading its bytes, then explored both compact and U-Grounding embeddings
for the embedded self-reference. The public code term already determines which
embedding is correct.

## Decision

Schedule environment-carrying quantifier branches before direct instantiated
quantifier branches in `sjas-structural-proof-check-state-decodedo`.

Also store each formula added to the structural unexpanded agenda with the
branch environment snapshot that was in scope when it was enqueued. When the
proof-guided selector later chooses that sibling, it checks the sibling under
its own saved environment, not under environment entries introduced while proving
a different sibling.

Make branch selection itself proof-node-blind: it only enumerates agenda
candidates and their saved environments. The decoded structural checker remains
the single point that substitutes the selected formula and validates it against
the decoded proof-node formula. This avoids a duplicate large-formula
substitution/match without changing the proof relation.

For Tableau-0 Group-3 `AxiomConj(system-code)` reconstruction, add a
code-term-aware proof antecedent path. It still reads `system-code` into bytes
through object relations and rebuilds embedded public code terms relationally,
but it keeps the walked public `system-code` term so compact and U-Grounding
embeddings are selected by structure rather than by an unconstrained
alternative.

This is a scheduling repair, not a semantic shortcut:

- the checker still validates formula-bearing proof nodes;
- proof-rule tags remain rejected;
- theorem, system, and proof codes remain decoded through object relations;
- no host registry or host proof checker is introduced;
- direct instantiated branches remain available after the environment-carrying
  route for cases where they are still useful.
- agenda environments are ordinary object-level branch environments threaded
  through miniKanren data; they are not host-side side tables.
- the Group-3 code-aware path still uses byte readers and code-term builder
  relations; it does not project bytes through a host decoder or consult a
  source registry.

## Success Criteria

- The existing SelfCons structural core selector completes successfully instead
  of exceeding the 180-second red timeout.
- The in-memory and decoded SelfCons selectors complete successfully.
- The public `tableau-proof/3` SelfCons selector completes successfully, proving
  the ADR-0073 Track 1 MVP endpoint.
- Existing focused structural proof and source-audit selectors remain green.
- Fast and extended regression gates remain green before Track 1 is marked
  complete.

## Failure Criteria

- The repair accepts legacy proof-rule traces or generated host proof evidence.
- The repair changes binder semantics for quantifier siblings.
- The public SelfCons selector still fails semantically after the scheduling
  change.

## Result

The repair met the success criteria on 2026-06-09. Focused SelfCons selectors
passed on current source:

```text
sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 1:08.67 maxrss 667564KB

sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:23.34 maxrss 989076KB

sjas-proof-check-accepts-formula-bearing-selfcons-tableau
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:20.39 maxrss 1036916KB

sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:17.71 maxrss 992768KB
```

Source audits and broad gates also passed:

```text
sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 0:17.19 maxrss 239172KB

lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 1:56.85 maxrss 407724KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 4:34.15 maxrss 606528KB
```
