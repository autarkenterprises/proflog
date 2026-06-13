# AAR-0102: SJAS Counterexample and Corrected Proof Targets

- Date: 2026-06-13
- ADR: [ADR-0102](../adr/ADR-0102-sjas-counterexample-proof-targets.md)
- Branch: `adr-0102-sjas-counterexample-proof-targets`

## Outcome

ADR-0100 as stated is explicitly refuted. The accepted `sjas-axiom` citation
counterexample demonstrates a fixed-size proof code with 18 bits accepted for a
large beta formula requiring 90 bits under ADR-0100's own `5J` formulation.

Path A and Path B were pursued to precise theorem targets:

- Path A narrows to a literal-Willard structural fragment and excludes the
  extended SJAS machinery.
- Path B defines the candidate extended `D_SJAS` apparatus needed for the full
  self-referential proof predicate.

Neither Path A nor Path B is complete yet. Path A needs a domain audit helper
and a formal branch coverage matrix. Path B needs a chosen `sjas-axiom`
size-accounting repair and a proof that the extended apparatus is admissible
for Willard-style SJAS arguments.

## Evidence

Focused selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-axiom-citation-counterexamples-adr-0100-size-claim
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-correspondence-per-rule-witnesses
Ran 1 tests containing 36 assertions.
0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-correspondence-anti-compression-rejects-skeletal-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 25 tests containing 386 assertions.
0 failures, 0 errors.
```

Regression gates:

```text
lein test-proflog-fast
Ran 196 tests containing 1035 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.

git diff --check
passed
```

## Follow-up

- Implement Path A's narrow-fragment audit and coverage matrix.
- Decide Path B's `sjas-axiom` proof-object accounting.
- Only after that decision, extend the operational suite for Path B primitives
  and recursive proof predicates.
