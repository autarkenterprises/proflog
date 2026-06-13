# ADR-0097: SJAS Structural Proof-Tree Audit

- Status: completed
- Date: 2026-06-13
- Branch: `adr-0097-sjas-structural-proof-tree-audit`
- AAR: [AAR-0097](../aar/AAR-0097-sjas-structural-proof-tree-audit.md)

## Context

ADR-0096 separated the encoded SJAS proof-symbol alphabet from admission into
the first Track 2b correspondence fragment. That first fragment admits
formula-bearing structural tableau proof terms and the bare `sjas-axiom`
citation certificate. For structural tableau proof terms, the audit currently
checks only the absence of encoded proof-symbol tags.

Track 2b needs stronger evidence. The correspondence proof must preserve finite
tree shape, child structure, formula-code inspectability, and proof-size
accounting. A proof term that merely contains no symbols might still be
malformed as a structural tableau node: it could have a missing byte payload,
an arity mismatch between byte count and payload, an invalid byte, or an
improper child term.

The accepted structural proof-code shape is already fixed by the decoder:

- flat node: `(byte-count byte... child...)`, where `byte-count` is positive
  and exactly that many following items are formula-code bytes;
- wide node: `((byte...) child...)`, where the first item is a non-empty list
  of formula-code bytes;
- children recursively have the same structural node shape.

This ADR adds a host-side audit for that proof object shape. It is not a new
proof checker and it does not replace the arithmeticized SJAS proof predicate.
It is an executable Track 2 artifact that exposes the finite tree and size
metrics needed by later correspondence proof work.

## Decision

Add structural proof-tree summary functions to `proflog.sjas-correspondence`.
The audit will:

- recognize flat and wide formula-bearing tableau node shapes;
- reject malformed byte-count, byte-range, and child-node shapes;
- report node count, leaf count, maximum depth, formula byte count, and child
  counts;
- integrate the result into the first-fragment audit for symbol-free proof
  terms.

The implementation must remain a pure host-side inspection utility. It must not
alter SJAS proof-code encoding, decoding, proof checking, query behavior, or
kernel search.

## Consequences

- Track 2b gets concrete evidence for the first fragment's finite proof-tree
  structure and size accounting.
- Future proof-size lower-bound work can use the summary metrics instead of
  reparsing structural proof terms ad hoc.
- Malformed symbol-free terms will no longer be reported as admitted simply
  because they contain no proof-symbol tags.
- This remains weaker than a formal correspondence proof: it audits syntax and
  size shape, not semantic rule validity.

## Test Obligations

The first red tests must show:

- flat and wide structural proof trees return expected node/leaf/depth/byte
  metrics;
- malformed flat payload counts, invalid bytes, and malformed children produce
  explicit errors in the audit summary;
- the first-fragment audit admits only valid structural tableaux, not arbitrary
  symbol-free lists.

## Exit Criteria

- Focused ADR-0097 tests pass.
- `proflog.sjas-correspondence-test` passes.
- `lein test-proflog-fast` passes because the correspondence audit namespace is
  on the fast gate.
- Documentation records that this is a Track 2 audit artifact, not a semantic
  proof-checker change.
- `git diff --check` passes.
