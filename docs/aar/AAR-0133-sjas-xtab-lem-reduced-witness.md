# AAR-0133: SJAS Xtab / LEM Reduced Witness

- Date: 2026-06-18
- ADR: [ADR-0133](../adr/ADR-0133-sjas-xtab-lem-reduced-witness.md)
- Branch: `adr-0133-sjas-xtab-lem-reduced-witness`

## Outcome

ADR-0133 is complete as the reduced reflected-beta witness stage for the
Xtab/LEM-as-axiom Workstream B variant.

The implementation adds:

- `xtab-lem-relations`, declaring `xtab-lem-demo/1`;
- `xtab-lem-witness-axioms`, generating the finite universal seed
  `forall x. xtab-lem-demo(x) or not xtab-lem-demo(x)`;
- `xtab-lem-reduced-witness-options` and
  `xtab-lem-reduced-witness-system`;
- Workstream B audit updates marking `:reduced-reflected-beta-witness`
  complete for `:xtab-or-lem-axiom`.

The witness beta record is Pi*1-admissible, changes encoded system identity and
regenerated Group-3/SelfCons code, decodes through `axiom-member/2`, and is
citeable through `tableau-proof/3` with the existing `sjas-axiom` certificate.

This is not a full LEM schema, a generated SelfCons contradiction target, a
constructed certificate, or proof-search synthesis evidence. Those obligations
remain open for later Workstream B ADRs.

## Evidence

Initial red selectors failed as intended:

```text
sjas-xtab-lem-reduced-witness-installs-reflected-lem-seed
No such var: sjas/xtab-lem-relations

xtab-lem-reduced-witness-records-reflected-beta-stage
expected :reduced-witness-implemented, actual :surface-implemented
expected completed reduced witness stage, actual nil
```

Focused green selectors:

```text
sjas-xtab-lem-reduced-witness-installs-reflected-lem-seed
Ran 1 tests containing 11 assertions.
0 failures, 0 errors.

xtab-lem-reduced-witness-records-reflected-beta-stage
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 18 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 227 tests containing 1439 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1199 fail=0 error=0
```

## Follow-up

- Add the generated SelfCons contradiction target for the Xtab/LEM witness
  system.
- Add a reduced reflected-beta witness for the Tab-2-or-stronger variant.
- Build constructed certificates and proof-search synthesis evidence for
  Workstream B variants.
