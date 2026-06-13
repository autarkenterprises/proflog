# AAR-0097: SJAS Structural Proof-Tree Audit

- Date: 2026-06-13
- Related ADR: [ADR-0097](../adr/ADR-0097-sjas-structural-proof-tree-audit.md)
- Outcome: completed

## What Happened

Added a host-side structural proof-tree audit for the first SJAS Track 2b
correspondence fragment. The audit recognizes flat and wide formula-bearing
tableau nodes, validates formula byte payloads, recursively checks children,
and reports tree/size metrics needed by later correspondence proof work.

The first-fragment audit now admits symbol-free proof terms only when this
structural audit succeeds. Malformed symbol-free terms are reported as
`:malformed-structural-tableau`.

## What Worked

The red test failed before implementation on the missing
`audit-structural-proof-tree` var. After implementation, the focused structural
selectors and the full `proflog.sjas-correspondence-test` namespace passed.
The fast gate also passed: `lein test-proflog-fast` with 191 tests and 1009
assertions. The extended gate passed with 73 tests and 219 assertions.

The change stayed outside proof search. No SJAS proof predicate, kernel
relation, proof-code encoder, query API, or answer behavior changed.

## What Did Not Work

This still does not prove semantic rule validity. It audits proof-object shape,
finite tree structure, and size metrics only. The SJAS structural checker still
provides the operational semantic validation, and Track 2b still needs a
written proof over compatible formal semantics.

## Follow-Up

- Use the structural metrics in the first Track 2b proof artifact for finite
  tree and lower-bound proof-size obligations.
- Add an explicit common intermediate semantics for the valid structural
  tableau fragment, including root/child/rule/closure judgments.
- Keep additional legacy proof symbols outside the first fragment until they
  receive primitive, macro-expansion, erasure, or unreachability proofs.
