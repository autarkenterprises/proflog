# AAR-0086: SJAS Tableau-0 Zero-One SelfCons Target

- Date: 2026-06-09
- ADR: [ADR-0086](../adr/ADR-0086-sjas-tableau0-zero-one-selfcons-target.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Completed the literature-compliance correction for ordinary Tableau-0
SelfCons. The minimal contradiction target is now the code of `0 = 1`, not the
code of primitive `false`.

The change touched both sides that must agree:

- the public SJAS builder emits `:contradiction-code` for `(eq 0 1)`;
- Tableau-0 Group-3 formula construction uses that code;
- kernel Group-3 axiom membership and proof-side `AxiomConj(s)` reconstruction
  embed the same formula-code bytes;
- the inconsistent-beta control now adds `0 = 1` to beta when it wants the
  contradiction target to be citable by `sjas-axiom`.

The descriptor/fixed-point interpretation of `s` was preserved. The byte
payload of `s` still records the finite profile, beta, and reflected source.
`AxiomConj(s)` reconstructs Group-3 from the profile and the same public code
instead of requiring a literal recursive SelfCons formula in the beta payload.

## Evidence

Red evidence before implementation:

```text
sjas-tableau0-selfcons-targets-zero-equals-one
Ran 1 tests containing 4 assertions.
3 failures, 0 errors.

sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
Ran 1 tests containing 3 assertions.
1 failures, 0 errors.
```

Focused selectors passed after implementation:

```text
sjas-tableau0-selfcons-targets-zero-equals-one
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.

sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

sjas-tableau-proof-cites-tableau0-group-three-from-system-code
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

sjas-tableau-proof-ignores-injected-generated-axiom-member-facts
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

sjas-axiom-member-query-ignores-injected-generated-facts
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

sjas-selfcons-demonstration-uses-substantive-proof-targets
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.

sjas-tableau-proof-cites-level1-group-three-from-system-code
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 8:29.61 maxrss 1961424KB
```

Broad gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 6:07.70 maxrss 463668KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 14:02.94 maxrss 576208KB

sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.

git diff --check
clean
```

## Follow-up

The public SelfCons selector is slower with the `0 = 1` target than it was with
primitive `false`; the test metadata now records an expected duration of about
8m30s for this selector. This is not a reason to revert the target. Further
performance work should remain subordinate to preserving the arithmeticized,
formula-bearing proof predicate.

The fixed-point `AxiomConj(s)` construction belongs to Track 1. If later review
finds that the executable relation does not accurately form the literature
axiom basis and SelfCons sentence for the selected `IS#_D(beta)` apparatus, that
is a Track 1 defect. Track 2 is for explicitly modified deductive apparatuses
or variants and the proof that they define a different acceptable SJAS.
