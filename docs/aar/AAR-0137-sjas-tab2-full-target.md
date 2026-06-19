# AAR-0137: SJAS Tab-2 Full Target

- Date: 2026-06-19
- ADR: [ADR-0137](../adr/ADR-0137-sjas-tab2-full-target.md)
- Branch: `adr-0137-sjas-tab2-full-target`

## Outcome

ADR-0137 is complete as the full generated SelfCons contradiction target stage
for the Tab-2-or-stronger Workstream B variant.

The implementation adds the target-only `:willard-sjas-tab2-boundary` profile,
with a distinct system-code tag and a generated Group-3 sentence over
`dsjas-tab2-proof/3`. The profile is intentionally not registered as an
executable proof profile. It exists to generate the target formula and code for
later boundary-failure evidence.

The implementation also adds `tab2-or-stronger-full-target-system` and
`tab2-or-stronger-full-target-report`. The report exposes the generated system
code, Group-3 SelfCons code, negated SelfCons formula, full
`AxiomConj(S_tab2_boundary) /\\ not(SelfCons(S_tab2_boundary))` target, and
target code. Constructed-certificate and proof-search synthesis obligations
remain open.

## Evidence

Initial red selectors failed as intended:

```text
sjas-tab2-full-target-names-generated-selfcons
No such var: sjas/tab2-or-stronger-full-target-system

boundary-failure-roadmap-keeps-witness-contract-open
expected :full-target-implemented, actual :reduced-witness-implemented
expected completed Tab-2 full target stage, actual only reduced witness
expected Tab-2 open obligations without full target, actual still included it
expected Tab-2 full-target metadata, actual nil
```

Focused green selectors:

```text
sjas-tab2-full-target-names-generated-selfcons
Ran 1 tests containing 17 assertions.
0 failures, 0 errors.

boundary-failure-roadmap-keeps-witness-contract-open
Ran 1 tests containing 24 assertions.
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 229 tests containing 1457 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1259 fail=0 error=0
```

## Follow-up

- Add constructed-certificate validation for the generated Tab-2 target.
- Add durable proof-search synthesis evidence before claiming final completion.
- Keep any future Tab-2 checker separate from the target-only boundary profile.
