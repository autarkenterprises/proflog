# AAR-0138: SJAS Tab-2 Certificate Validation

- Date: 2026-06-19
- ADR: [ADR-0138](../adr/ADR-0138-sjas-tab2-certificate-validation.md)
- Branch: `adr-0138-sjas-tab2-certificate-validation`

## Outcome

ADR-0138 is complete as the proof-validation bridge for Tab-2-or-stronger
constructed-certificate candidates.

The implementation adds
`tab2-or-stronger-constructed-certificate-validation`, which derives the
ADR-0137 target-only system, classifies supplied proof codes, and validates
readable candidates through ordinary `tableau-proof/3` against the generated
Tab-2 Group-3 theorem code. The returned validation record matches the existing
total-multiplication and Xtab/LEM certificate-validation shape.

The kernel profile now recognizes the target-only
`:willard-sjas-tab2-boundary` system tag for Group-3 axiom membership and proof
antecedent reconstruction. The `dsjas-tab2-proof/3` symbol is handled as a
profile-local coding reservation: Tab-2 targets receive a stable code index,
while ordinary systems keep that index available for user relations when the
Tab-2 relation is not declared.

The Workstream B roadmap now advertises the Tab-2 validation helper through the
generic constructed-certificate verifier metadata. This remains intermediate
evidence only: the constructed-certificate and proof-search synthesis
obligations are still open.

## Evidence

Initial red selectors failed as intended:

```text
sjas-tab2-certificate-validation-targets-generated-selfcons
No such var: sjas/tab2-or-stronger-constructed-certificate-validation

tab2-certificate-validation-recorded-for-generated-target
expected verifier metadata for :tab-2-or-stronger, actual nil
```

An intermediate green attempt exposed an implementation regression:

```text
lein test-proflog-sjas
:SUMMARY pass=1265 fail=9 error=0
```

Those failures were reflected-call proof checks caused by making
`dsjas-tab2-proof` a globally decoded reserved symbol. The final implementation
keeps the reservation profile-local, and the affected focused regression
returned green:

```text
sjas-procedure-call-expansion-is-formula-bearing-and-tag-free
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
```

Focused green selectors:

```text
sjas-tab2-certificate-validation-targets-generated-selfcons
Ran 1 tests containing 15 assertions.
0 failures, 0 errors.

tab2-certificate-validation-recorded-for-generated-target
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 26 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 230 tests containing 1465 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1274 fail=0 error=0
```

## Follow-up

- Add an actual Tab-2 constructed-certificate candidate before claiming the
  `:constructed-certificate` obligation.
- Add durable proof-search synthesis evidence before claiming final Workstream B
  completion for the Tab-2-or-stronger variant.
- Keep any future executable Tab-2 checker separate from the target-only
  boundary profile unless a later ADR explicitly changes that boundary.
