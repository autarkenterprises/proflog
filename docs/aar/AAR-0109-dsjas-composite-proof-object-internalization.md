# AAR-0109: D_SJAS Composite Proof-Object Internalization

- Date: 2026-06-14
- ADR: [ADR-0109](../adr/ADR-0109-dsjas-composite-proof-object-internalization.md)
- Branch: `adr-0109-dsjas-composite-proof-object`

## Outcome

ADR-0109 is complete.

The implemented `IS#_{D_SJAS}(beta)` SelfCons sentences now quantify over the
same composite proof objects used by the quantitative EA-stability theorem:

```text
dsjas-tableau-proof(S,F,C)      where C decodes as (S,F,P)
dsjas-subst-prf(S,G,F,C)        where C decodes as (S,G,F,P)
```

The public compatibility predicates `tableau-proof/3` and `subst-prf/4` remain
available. Generated SelfCons and reconstructed Group-3 formulas use the
measured `dsjas-*` predicates, so the proof variables in SelfCons now range over
the object measured by `Log_D_SJAS` rather than bare public proof-code `P`.

The correspondence audit was also updated: the new proof-code payload symbols
`dsjas-tableau-proof-object` and `dsjas-subst-prf-object` are classified, and the
EA-stability accounting distinguishes tableau `(S,F,P)` objects from
substitution `(S,G,F,P)` objects.

## Evidence

Initial red tests:

```text
No such var: sjas/dsjas-tableau-proof-object

FAIL proof-symbol-audit-classifies-every-encoded-certificate-symbol
actual: #{dsjas-subst-prf-object dsjas-tableau-proof-object}
```

Focused measured-object selectors passed:

```text
dsjas-composite-tableau-proof-object-carries-measured-components
dsjas-tableau-proof-accepts-and-checks-composite-axiom-citations
dsjas-composite-subst-prf-object-carries-measured-components
dsjas-subst-prf-accepts-and-checks-composite-axiom-citations
```

Final gates:

```text
lein test-proflog-fast
Ran 208 tests containing 1115 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1078 fail=0 error=0
```

An all-vars `lein test-proflog-sjas-focused` run was started during verification
and reached the known over-envelope slow selector
`sjas-subst-prf-checks-selfcons-fixed-point-certificate`. It crossed 15 minutes,
was recorded in `LOG.md`, and was later interrupted after exceeding the
historical 45-minute envelope. The branch's SJAS gate is the not-slow
`lein test-proflog-sjas` run above.

## Follow-up

- Keep `Log_D_SJAS` accounting synchronized with any later proof-object
  compression or proof-predicate synthesis changes.
- Slow fixed-point probes remain useful, but they should be launched through the
  documented slow/durable lane rather than through the all-vars focused gate.
