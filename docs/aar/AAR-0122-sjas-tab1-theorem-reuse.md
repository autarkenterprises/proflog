# AAR-0122: SJAS Tab-1 Theorem Reuse

- Date: 2026-06-18
- ADR: [ADR-0122](../adr/ADR-0122-sjas-tab1-theorem-reuse.md)
- Branch: `adr-0122-sjas-tab1-theorem-reuse`

## Outcome

ADR-0122 is complete.

The Tab-1 checker now carries earlier validated theorem bytes while walking a
proof list:

- intermediate entries enter the reusable list only after proof validation and
  `Pi*_1` / `Sigma*_1` classification;
- a later `sjas-axiom` entry may cite either a finite-system axiom or an earlier
  theorem byte string;
- structural later-entry proofs conjoin decoded earlier theorem antecedents
  with `AxiomConj(S)` before checking the negated current theorem;
- public `tab1-proof/3` accepts a multi-entry proof list where the final entry
  cites an earlier non-system theorem;
- the roadmap audit records theorem reuse as implemented and clears the
  Workstream A Tab-1 validation deferred obligation.

The test fixture uses a Tableau-0 SJAS system to exercise the public
`tab1-proof/3` relation with a small structural first entry. ADR-0121 still
covers the `:willard-sjas-tab1` profile dispatch and measured Tab-1 object
validation.

## Evidence

Initial red selectors failed as intended:

```text
tab1-prior-theorem-member-core-recognizes-earlier-theorem-by-bytes
Unable to resolve var: sjas-profile/sjas-tab1-prior-theorem-member-coreo

tab1-roadmap-audit-reconciles-rank1-terminology
expected :implemented, actual nil
expected #{}, actual #{:proof-search-theorem-reuse}
```

Focused green selectors:

```text
tab1-prior-theorem-member-core-recognizes-earlier-theorem-by-bytes
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.

tab1-proof-reuses-earlier-pi-star-1-theorem-as-axiom-citation
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.

tab1-roadmap-audit-reconciles-rank1-terminology
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
```

ADR-0121 regression selectors also passed:

```text
tab1-proof-accepts-single-entry-axiom-citation
dsjas-tab1-proof-accepts-and-checks-measured-proof-lists
tab1-proof-rejects-entry-whose-proof-does-not-prove-theorem
tab1-proof-list-accounting-records-entry-validation
```

Final gates:

```text
lein test-proflog-fast
Ran 219 tests containing 1361 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1107 fail=0 error=0
```

## Follow-up

- Workstream A's positive Tab-1 proof-list validation is now covered at the
  entry-validation and theorem-reuse levels; future Tab-k work should be
  explicit about whether it generalizes beyond `k=1` or optimizes the current
  relational path.
- Workstream B negative variants and Workstream C self-extension remain open
  from ADR-0119.
