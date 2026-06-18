# AAR-0121: SJAS Tab-1 Entry Validation

- Date: 2026-06-18
- ADR: [ADR-0121](../adr/ADR-0121-sjas-tab1-entry-validation.md)
- Branch: `adr-0121-sjas-tab1-entry-validation`

## Outcome

ADR-0121 is complete.

The implementation turns the ADR-0120 Tab-1 surface into an executable entry
validator without claiming full theorem reuse:

- `:willard-sjas-tab1` now dispatches through the SJAS proof profile instead of
  throwing the ADR-0120 deferred exception.
- Tab-1 system-code headers with profile tag `35` are accepted by the kernel
  system-code reader.
- `tab1-proof/3` decodes `H` as a `tab1-proof-list-object`, requires a
  non-empty proof list, validates each `(theorem, proof)` entry through the
  existing arithmeticized tableau proof-byte checker, and requires the final
  entry theorem bytes to match the public target `F`.
- `dsjas-tab1-proof/3` decodes measured `(S,F,H)` proof objects, rejects
  embedded system/theorem mismatches, and delegates to the same proof-list
  validator.
- Intermediate proof-list entries are restricted to public `Pi*_1` or
  `Sigma*_1` formulas before recursion continues.
- Formula-bearing structural proof nodes can close both public and measured
  Tab-1 proof leaves through the same core relation.
- `willard-sjas-tab1-proof-check` is appended to the proof-symbol table and
  classified as relevant profile evidence.

Theorem-reuse proof search remains open: this slice validates entries against
the selected system with the existing tableau proof predicate, but it does not
yet extend later entry proofs with earlier `t_j` assumptions.

## Evidence

Initial red selectors failed as intended:

```text
tab1-proof-accepts-single-entry-axiom-citation
Tab-1 proof search is deferred after ADR-0120

dsjas-tab1-proof-accepts-and-checks-measured-proof-lists
Tab-1 proof search is deferred after ADR-0120

tab1-proof-rejects-entry-whose-proof-does-not-prove-theorem
Tab-1 proof search is deferred after ADR-0120

tab1-proof-list-accounting-records-entry-validation
expected :entry-validation-implemented, actual :deferred-to-adr-0121
```

Focused green selectors:

```text
tab1-proof-accepts-single-entry-axiom-citation
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

dsjas-tab1-proof-accepts-and-checks-measured-proof-lists
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

tab1-proof-rejects-entry-whose-proof-does-not-prove-theorem
Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

tab1-proof-list-accounting-records-entry-validation
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.
```

Nearby invariants passed:

```text
sjas-tab1-profile-group-three-uses-tab1-proof-list-vocabulary
tab1-proof-list-object-encodes-theorem-proof-pairs
dsjas-tableau-proof-accepts-and-checks-composite-axiom-citations
dsjas-subst-prf-accepts-and-checks-composite-axiom-citations
proof-symbol-audit-classifies-every-encoded-certificate-symbol
implemented-sjas-profile-layer-markers-are-classified
```

Final gates:

```text
lein test-proflog-fast
Ran 219 tests containing 1360 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1099 fail=0 error=0
```

## Follow-up

- ADR-0122 or a later Tab-1 ADR should implement theorem-reuse proof search:
  each later `p_i` must be checked from beta plus earlier proof-list theorems
  `t_j`, not only against the base system.
- Add multi-entry examples that exercise the `Pi*_1` / `Sigma*_1` intermediate
  restriction with a genuine reusable theorem once theorem reuse is available.
- Workstream B and Workstream C from ADR-0119 remain open.
