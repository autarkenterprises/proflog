# AAR-0103: SJAS Proof Attempts A and B

- Date: 2026-06-13
- ADR: [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md)
- Branch: `adr-0103-sjas-proof-attempts-a-b`

## Outcome

ADR-0103 is complete after the follow-up proof work requested by the user.

Path A is proved for the narrowed literal-Willard structural fragment. The
executable proof audit discharges all six named lemma obligations and proves
that every admitted branch is either a direct Willard tableau rule or a
discharged bookkeeping/truth/NNF/quantifier case. The excluded SJAS-extension
branches remain outside the theorem domain.

Path B is completed negatively for Track 2b over the current accepted domain.
The current implementation cannot be claimed to correspond to literal Willard
`D`, because it admits extended equality/profile/reflected/proof-predicate rule
families and still has the ADR-0102 fixed-size `sjas-axiom` citation
counterexample. A positive `D_SJAS` theorem is a Track 2c task, not a completed
literal-Willard proof.

## Evidence

Initial red test before the inventory implementation:

```text
lein test proflog.sjas-correspondence-test
Syntax error compiling at (proflog/sjas_correspondence_test.clj:385:17).
No such var: correspondence/audit-path-a-narrow-rule-inventory
Tests failed.
```

Inventory green:

```text
lein test proflog.sjas-correspondence-test
Ran 28 tests containing 397 assertions.
0 failures, 0 errors.
```

Follow-up red test before the proof-status implementation:

```text
lein test proflog.sjas-correspondence-test
Syntax error compiling at (proflog/sjas_correspondence_test.clj:458:17).
No such var: correspondence/audit-path-a-narrow-correspondence-proof
Tests failed.
```

Proof-status green:

```text
lein test proflog.sjas-correspondence-test
Ran 30 tests containing 407 assertions.
0 failures, 0 errors.
```

Final broad gates before commit:

```text
lein test-proflog-fast
Ran 201 tests containing 1056 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Follow-up

- Track 2c: define `D_SJAS` as a selected deductive apparatus, choose the
  proof-object accounting repair, then prove correspondence for that apparatus.
- A later mechanized proof assistant version could replace the direct-
  examination Path A proof, but it is not required for this ADR's current
  direct-examination standard.
