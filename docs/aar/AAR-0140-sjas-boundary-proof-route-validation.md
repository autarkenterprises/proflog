# AAR-0140: SJAS Boundary Proof-Route Validation

- Date: 2026-06-19
- ADR: [ADR-0140](../adr/ADR-0140-sjas-boundary-proof-route-validation.md)
- Branch: `adr-0140-sjas-boundary-proof-route-validation`

## Outcome

ADR-0140 corrected the Workstream B evidence contract without closing any final
evidence obligation.

The audit confirmed two false-positive paths. `screen-boundary-evidence`
trusted the candidate field `:uses-reduced-witness?`, and the three legacy
target validators checked a proof of the positive Group-3 theorem. Since Group
3 is an axiom, that validation establishes ordinary self-justification rather
than a failure of the generated SelfCons predicate.

The implementation now requires `:selfcons-counterexample` validation. For the
Level-1 variants, target-specific validators check the theorem class,
complement relation, and both measured `dsjas-subst-prf/4` objects through the
SJAS kernel. The generic verifier also binds all four tuple components between
the candidate and validation report.

`boundary-proof-route-report` decodes the measured proof objects, verifies their
embedded system bytes, and inspects exact formula-bearing proof nodes. A valid
route must select a designated reduced-witness formula and must not select the
generated Group-3 formula. It rejects unreadable objects, invalid measured
object arities, wrong embedded systems, unrelated proof nodes, and ordinary
Group-3 routes.

The Tab-2 validator now reports `:proof-relation-unavailable`; ADR-0138's
Tableau-0 bridge remains a diagnostic compatibility API and cannot validate the
`dsjas-tab2-proof/3` atoms quantified by the Tab-2 SelfCons sentence. ADR-0139's
positive SelfCons synthesis probe is likewise labeled diagnostic-only.

## Red-Green Evidence

Initial red selectors demonstrated the prior behavior:

```text
boundary-evidence-screen-rejects-trivial-selfcons-evidence
expected #{:ordinary-selfcons-citation}
actual #{:ordinary-selfcons-citation :missing-reduced-witness}

sjas-boundary-proof-route-is-derived-from-measured-proof-objects
No such var: sjas/boundary-proof-route-report

boundary-constructed-certificate-verifier-requires-screen-and-proof-validation
expected :wrong-counterexample-tuple
actual no rejection reason

boundary-proof-search-synthesis-probe-exposes-all-workstream-b-variants
expected diagnostic-only fields; actual nil
```

Focused green selectors:

```text
lein test-vars proflog.sjas-correspondence-test
:SUMMARY pass=610 fail=0 error=0

lein test :only proflog.willard-sjas-test/sjas-boundary-proof-route-is-derived-from-measured-proof-objects
Ran 1 tests containing 24 assertions.
0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-boundary-counterexample-validation-does-not-relabel-positive-selfcons
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.

lein test-vars :not-slow proflog.sjas-boundary-synthesis-probe-test
:SUMMARY pass=51 fail=0 error=0
```

Final gates:

```text
lein test-proflog-fast
Ran 231 tests containing 1496 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1359 fail=0 error=0
```

## Coverage Boundary

All rejection, decoding, exact-system, tuple-binding, and route-classification
paths introduced by this ADR have focused coverage. The successful Level-1
counterexample branch is intentionally not fabricated: exercising it requires
the actual complementary theorem/proof tuple that remains the open Workstream B
constructed-certificate obligation. A mocked success would recreate the trust
problem ADR-0140 removes. The kernel predicate calls and route audit are covered
independently until a real certificate ADR supplies that tuple.

## Follow-up

- Implement the arithmeticized `dsjas-tab2-proof/3` relation before attempting
  Tab-2 final evidence.
- Add public coding for complete counterexample certificate tuples.
- Construct and kernel-check the first Level-1 counterexample tuple, including
  a proof route that selects the reduced witness.
- Replace the positive SelfCons synthesis query with joint synthesis of the
  generated counterexample tuple and preserve its durable search log.
