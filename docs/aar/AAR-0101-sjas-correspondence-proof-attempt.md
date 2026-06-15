# AAR-0101: SJAS Correspondence Proof Attempt Audit

- Date: 2026-06-13
- ADR: [ADR-0101](../adr/ADR-0101-sjas-correspondence-proof-attempt.md)
- Branch: `adr-0101-sjas-correspondence-proof-audit`

## Outcome

The attempted whole proof of ADR-0100 did not close.

The proof attempt corroborates a substantial core: non-axiom formula-bearing
structural proofs that use ordinary tableau rules can be mapped to Willard's
semantic-tableau method with explicit agenda, NNF, freshness, and environment
lemmas. The proof-size lower bound can also be repaired for genuine
formula-bearing structural proof trees because the proof code carries every
node's formula bytes.

The full ADR-0100 theorem is still not demonstrated as written:

- several accepted checker branches are equality/arithmetic/profile/reflected
  or recursive proof-predicate steps, not literal Willard `D` rules;
- the bare `sjas-axiom` citation is fixed-size while the cited formula can be
  large through `S` and `F`, so `size(P) >= 5J` fails as a statement about `P`
  alone over ADR-0100's covered domain.

## Evidence

The proof attempt is recorded in
[2026-06-13-sjas-correspondence-proof-attempt.md](../log/2026-06-13-sjas-correspondence-proof-attempt.md).
It contains the exhaustive checker-branch audit, the partial proof that works,
the repaired anti-compression subargument for structural trees, and the
blocking `sjas-axiom` / extended-apparatus issues.

Focused regressions were rerun after the documentation update:

```text
lein test :only proflog.willard-sjas-test/sjas-correspondence-per-rule-witnesses
Ran 1 tests containing 36 assertions.
0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-correspondence-anti-compression-rejects-skeletal-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 25 tests containing 386 assertions.
0 failures, 0 errors.

git diff --check
passed
```

## Effect on ADR-0100

ADR-0100 should be treated as a useful proof scaffold and partial
corroboration, not a completed Track 2b proof. Its AAR now has an erratum
pointing to this result.

ADR-0102 strengthens this from "not demonstrated" to "refuted as stated" by
adding an executable fixed-size `sjas-axiom` citation counterexample.

## Follow-up

- Choose the revised proof target: narrowed literal-Willard fragment, extended
  selected apparatus, or combined proof-object encoding.
- If the bare `sjas-axiom` citation remains admissible, define exactly what
  proof object the `5J` lower bound ranges over.
- Add a clause/test coverage matrix only after the proof target is fixed.
