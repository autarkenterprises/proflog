# ADR-0112: Proflog Literature Tableau Golden Suite

- Status: accepted
- Date: 2026-06-16
- Branch: `adr-0112-proflog-literature-tableau-golden-suite`
- AAR: [AAR-0112](../aar/AAR-0112-proflog-literature-tableau-golden-suite.md)

## Context

Review of `bradleypallen/tableaux` found a useful external corpus of tableau
tests and examples, but not an implementation architecture that should be
imported into Proflog. The useful part is the test discipline: examples are
grouped around classical tableaux, weak Kleene behavior, signed tableaux,
first-order syntax, optimization envelopes, and literature-attributed examples
from Smullyan, Fitting, Priest, Ferguson, and handbook-style tableau sources.

As of reviewed upstream commit `fa5a736090465d0ddf35362a6271d4298d668d42`,
the active `tableaux` test corpus contains 65 active Python test functions,
plus a setup smoke script and CLI formula examples. Proflog should not accept
those results merely because the Python tests assert them. Each supported
expectation must be checked through Proflog itself, and any disagreement among
the Python result, external source expectation, and Proflog result must be
recorded for reconciliation.

## Decision

Add a Proflog-level literature tableau golden suite. The suite must start by
cataloging every active test in the reviewed `tableaux` corpus and then either
incorporate it directly, translate it into a Proflog analog, or mark it
unsupported with a specific reason. The planning inventory is seeded at
[tableaux-test-inventory](../planning/proflog-tableau-improvements/tableaux-test-inventory.md).

The implementation must include:

- a source-test inventory keyed by upstream file, test name, source commit, and
  Proflog disposition;
- a reconciliation ledger for differences among upstream `tableaux`, external
  source expectations, and Proflog observations;
- fast tests for small semantic examples whose expected runtime is modest;
- extended tests for larger branch-growth, performance-envelope, or
  source-confirmation examples;
- source notes for any additional examples imported from Smullyan, Fitting,
  Priest, Ferguson, D'Agostino, or other tableau literature.

No upstream test result is authoritative by itself. A test becomes part of the
golden suite only after its expected result is independently confirmed by
Proflog or explicitly recorded as an unresolved reconciliation item.

## Consequences

- Proflog gains a portable, literature-oriented tableau conformance layer
  rather than a clone of another project's Python API.
- The suite can expose gaps where Proflog intentionally lacks a connective,
  sign discipline, parser feature, or model/witness facility. Those gaps must
  be documented rather than silently omitted.
- Some upstream tests are implementation-shape tests for `tableaux`; these must
  be translated into semantic Proflog analogs or classified as non-portable.
- Performance claims must be paired with semantic preservation tests, not only
  elapsed-time checks.

## Test Obligations

- Red tests must fail when any active upstream `tableaux` test is missing from
  the source inventory or lacks a Proflog disposition.
- Red tests must require at least one small closure example, one open-branch
  example, one alpha/beta expansion example, one contradiction example, and one
  branch-growth envelope before the suite can be called present.
- Red tests must require the reconciliation ledger to record disagreements
  instead of overwriting expected results.
- Fast-suite examples must be added to `lein test-proflog-fast`; slower source
  confirmation or branch-growth examples must be marked and placed in
  `lein test-proflog-extended`.
- Any added external literature example must include enough citation context to
  let a later developer find and verify the source.

## Exit Criteria

- Every active `tableaux` test at commit `fa5a736090465d0ddf35362a6271d4298d668d42`
  is represented in the inventory with a direct, analog, performance,
  source-confirmation, or unsupported disposition.
- All supported dispositions have Proflog-observed results recorded.
- All disagreements among upstream, source, and Proflog expectations are
  resolved or explicitly deferred with rationale.
- The fast and extended test gates pass with the new suite included.
- AAR-0112 records the final inventory counts, reconciliations, runtime
  placement, and remaining unsupported surface.

## References

- `bradleypallen/tableaux`: https://github.com/bradleypallen/tableaux
- Raymond M. Smullyan, *First-Order Logic*.
- Melvin Fitting, *First-Order Logic and Automated Theorem Proving*.
- Graham Priest, *An Introduction to Non-Classical Logic*.
- M. D'Agostino, D. M. Gabbay, R. Hahnle, and J. Posegga, *Handbook of Tableau
  Methods*.
- T. M. Ferguson, "Tableaux and Restricted Quantification for Systems Related
  to Weak Kleene Logic."
