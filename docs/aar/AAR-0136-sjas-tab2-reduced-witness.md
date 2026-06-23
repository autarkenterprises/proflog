# AAR-0136: SJAS Tab-2 Reduced Witness

- Date: 2026-06-19
- ADR: [ADR-0136](../adr/ADR-0136-sjas-tab2-reduced-witness.md)
- Branch: `adr-0136-sjas-tab2-reduced-witness`

## Outcome

ADR-0136 is complete as the reduced witness stage for the
Tab-2-or-stronger Workstream B variant.

The implementation adds `tab2-rank2-witness-formula` and
`tab2-or-stronger-reduced-witness-report`. The report records the
`forall x. exists y. true` witness formula, codes it against the implemented
`:willard-sjas-tab1` baseline, proves that it is outside the Tab-1
`Pi*_1`/`Sigma*_1`/`pi-star-1-encodable?` intermediate classes, and validates a
structural proof code through the existing ordinary Tableau-0 `tableau-proof/3`
path.

The first attempted witness shape, `forall x. exists y. y = x`, had the right
classifier status but needed a more involved proof-variable/parameter closure
than this ADR required. The accepted witness keeps the same `forall` then
`exists` boundary while yielding a compact structural certificate.

The Workstream B roadmap now marks only
`:reduced-reflected-beta-witness` complete for `:tab-2-or-stronger`; the full
generated SelfCons target, constructed certificate, and proof-search synthesis
obligations remain open.

## Evidence

Initial red selectors failed as intended:

```text
sjas-tab2-reduced-witness-exhibits-non-tab1-intermediate
No such var: sjas/tab2-or-stronger-reduced-witness-report

boundary-failure-roadmap-keeps-witness-contract-open
expected :reduced-witness-implemented, actual :surface-implemented
expected completed Tab-2 reduced witness stage, actual nil
expected Tab-2 open obligations without reduced witness, actual still included it
expected Tab-2 reduced-witness metadata, actual nil
```

Focused green selectors:

```text
sjas-tab2-reduced-witness-exhibits-non-tab1-intermediate
Ran 1 tests containing 15 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 23 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 229 tests containing 1456 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1242 fail=0 error=0
```

## Follow-up

- Build the Tab-2-or-stronger full generated SelfCons contradiction target.
- Add constructed-certificate validation once that generated target exists.
- Keep proof-search synthesis evidence distinct from constructed certificates.
