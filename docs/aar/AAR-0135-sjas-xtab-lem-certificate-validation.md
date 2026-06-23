# AAR-0135: SJAS Xtab / LEM Certificate Validation

- Date: 2026-06-18
- ADR: [ADR-0135](../adr/ADR-0135-sjas-xtab-lem-certificate-validation.md)
- Branch: `adr-0135-sjas-xtab-lem-certificate-validation`

## Outcome

ADR-0135 is complete as the constructed-certificate proof-validation bridge for
the Xtab/LEM-as-axiom Workstream B target.

The implementation adds `xtab-lem-constructed-certificate-validation`, which
builds the ADR-0133 reduced-witness system, derives the ADR-0134 generated
target, classifies a supplied proof code, and checks it through
`tableau-proof/3` against the generated Group-3 theorem code.

The Workstream B audit now advertises the generic
`verify-boundary-constructed-certificate` verifier and
`xtab-lem-constructed-certificate-validation` helper for
`:xtab-or-lem-axiom` constructed-certificate candidates.

This is not a constructed certificate. The roadmap still leaves
`:constructed-certificate` and `:proof-search-synthesis` open for Xtab/LEM.

## Evidence

Initial red selectors failed as intended:

```text
sjas-xtab-lem-certificate-validation-targets-generated-selfcons
No such var: sjas/xtab-lem-constructed-certificate-validation

xtab-lem-certificate-validation-recorded-for-generated-target
expected :implemented, actual nil
expected xtab-lem-constructed-certificate-validation, actual nil
```

Focused green selectors:

```text
sjas-xtab-lem-certificate-validation-targets-generated-selfcons
Ran 1 tests containing 15 assertions.
0 failures, 0 errors.

xtab-lem-certificate-validation-recorded-for-generated-target
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 21 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 229 tests containing 1454 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1227 fail=0 error=0
```

## Follow-up

- Build actual constructed certificates for generated Workstream B targets.
- Add proof-search synthesis evidence with durable logs for long-running
  probes.
- Add the reduced reflected-beta witness for the Tab-2-or-stronger variant.
