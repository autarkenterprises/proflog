# SJAS Structural Proof-Tree Audit

Date: 2026-06-13

ADR: [ADR-0097](../adr/ADR-0097-sjas-structural-proof-tree-audit.md)

Branch: `adr-0097-sjas-structural-proof-tree-audit`

## Context

ADR-0096 made the first Track 2b correspondence fragment explicit:
formula-bearing structural tableaux and bare `sjas-axiom` citations are
admitted, while encoded legacy proof-rule traces remain outside the first
fragment. That was still too weak for the structural side because an arbitrary
symbol-free list could look admitted even when it did not have the
formula-bearing tableau node shape consumed by the SJAS structural checker.

The accepted structural proof-code shape is:

- flat node: `(byte-count byte... child...)`, where `byte-count` is positive
  and exactly that many following items are formula-code bytes;
- wide node: `((byte...) child...)`, where the first item is a non-empty byte
  list;
- children recursively have the same shape.

## Change

Added `audit-structural-proof-tree` to `proflog.sjas-correspondence`. It
checks the host-side proof object syntax for the first structural fragment and
reports:

- validity;
- node count;
- leaf count;
- maximum depth;
- total formula-byte count;
- preorder child counts;
- formula-byte payloads;
- structured errors and error reasons for malformed nodes.

`audit-first-correspondence-fragment` now calls this parser before admitting a
symbol-free proof term as `:formula-bearing-tableau`. Malformed symbol-free
terms are classified as `:malformed-structural-tableau`.

No kernel, proof checker, proof-code encoder, query, or answer behavior was
changed.

## Red/Green Evidence

Red selector before implementation:

```text
lein test :only proflog.sjas-correspondence-test/structural-proof-tree-audit-reports-flat-node-size-and-shape
Syntax error compiling at (proflog/sjas_correspondence_test.clj:325:17).
No such var: correspondence/audit-structural-proof-tree
Tests failed.
```

Green focused selectors after implementation:

```text
lein test :only proflog.sjas-correspondence-test/structural-proof-tree-audit-reports-flat-node-size-and-shape
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/structural-proof-tree-audit-reports-wide-node-size-and-shape
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/structural-proof-tree-audit-rejects-malformed-symbol-free-terms
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/first-correspondence-fragment-requires-valid-structural-tableaux
Ran 1 tests containing 5 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 20 tests containing 360 assertions.
0 failures, 0 errors.

lein test-proflog-fast
Ran 191 tests containing 1009 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Track 2 Result

This is a proof-object audit artifact, not a formal correspondence theorem.
It strengthens the first fragment boundary by making finite tree shape and
proof-size metrics executable. Later Track 2b proof work can use these metrics
when stating the structural proof-tree and anti-compression obligations.
