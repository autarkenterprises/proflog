# AAR-0108: D_SJAS Quantitative EA-Stability

- Date: 2026-06-14
- ADR: [ADR-0108](../adr/ADR-0108-dsjas-quantitative-ea-stability.md)
- Branch: `adr-0108-dsjas-ea-stability`

## Outcome

ADR-0108 is complete.

The branch proves quantitative EA-stability for the selected `D_SJAS`
proof-object measure `Log_D_SJAS`, not for bare public proof-code `P`.
Structural proof trees keep the ordinary proof-code measure; `sjas-axiom`
citation leaves use ADR-0104's combined inspectable `(S,F,P)` measure. This
keeps the theorem compatible with ADR-0102's counterexample instead of
reintroducing the false proof-code-only claim.

The executable audit records the quantitative constants:

```text
A-stability: sigma = 1, tau = 1, lambda = 1/2, mu = 0
E-stability: sigma = 1, tau = 1, lambda = 1/2, mu = -1
```

It also records the rule-family preservation clauses needed to generalize
Willard's Appendix D Normed(a,b) open-branch lemma from ordinary semantic
tableaux to the selected `D_SJAS` apparatus.

## Evidence

Initial red test:

```text
lein test proflog.sjas-correspondence-test
Syntax error compiling at (proflog/sjas_correspondence_test.clj:609:17).
No such var: correspondence/audit-dsjas-quantitative-ea-stability
Tests failed.
```

Focused green after the theorem audit implementation:

```text
lein test proflog.sjas-correspondence-test
Ran 37 tests containing 454 assertions.
0 failures, 0 errors.
```

Final broad gates:

```text
lein test-proflog-fast
Ran 208 tests containing 1103 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Follow-up

- Treat `Log_D_SJAS`, not bare `P`, as the stability measure for
  `IS#_{D_SJAS}(beta)`.
- Any later proof-object compression or citation optimization must preserve the
  combined measured payload or update this ADR with a new quantitative proof.
