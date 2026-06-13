# ADR-0096: SJAS Correspondence Fragment Audit

- Status: completed
- Date: 2026-06-13
- Branch: `adr-0096-sjas-correspondence-fragment-audit`
- AAR: [AAR-0096](../aar/AAR-0096-sjas-correspondence-fragment-audit.md)

## Context

ADR-0073 Track 2 asks for proof-object correspondence between Proflog proof
acceptance and the selected SJAS semantic-tableau proof predicate. The existing
`proflog.sjas-correspondence` namespace classifies the encoded SJAS proof
symbol alphabet, but classification alone is not enough for Track 2b. A symbol
can be encoded for inspection and still be outside the first correspondence
fragment.

This distinction matters because Track 1 now validates non-`sjas-axiom`
`tableau-proof/3` certificates as formula-bearing tableau nodes rather than as
legacy kernel proof traces. Legacy tags such as `conj`, `split`, `eq-step`,
`pos-call`, and `neg-call` are still useful evidence symbols in public proof
output and historical audits, but accepting them as proof-code input to the
literature predicate would be a different proof-object relation.

The Track 2 artifact needed here is therefore a small executable boundary:
state which proof terms are inside the first correspondence fragment, which
terms are explicit axiom citations, and which encoded symbols remain outside
the fragment pending a separate primitive, macro-expansion, erasure, or
exclusion proof.

## Decision

Extend the SJAS correspondence audit with a fragment-level classifier separate
from the existing symbol relevance classifier.

The first fragment admitted by this ADR is deliberately narrow:

- formula-bearing structural tableau certificates, recognized by having no
  encoded proof-symbol tags in the decoded proof term;
- the bare `sjas-axiom` citation certificate, whose soundness is discharged by
  axiom-membership checking rather than by a non-axiom tableau tree.

Every other encoded proof symbol remains outside that first fragment, even when
its Track 2a classification is `:relevant`. This is not a claim that the symbol
is unsound. It is a statement that Track 2b has not yet admitted it as a
primitive, bounded macro, wrapper erasure, or proved-unreachable constructor in
the first proof-object correspondence theorem.

This ADR must not change the kernel, proof checker, proof-code encoder, or
query behavior.

## Consequences

- Future Track 2b work gets a sharper executable boundary than
  "all symbols are classified."
- New proof-symbol alphabet entries will fail tests until they receive both a
  Track 2a relevance classification and a fragment-boundary classification.
- Legacy and sidecar proof evidence remains encodable for audit/debugging
  without silently entering the literature proof-predicate fragment.
- The result is intentionally conservative. It may cause future correspondence
  work to add explicit admissions for constructors that are later proved
  primitive, macro-expandable, erasable, or unreachable.

## Test Obligations

The first red tests must show:

- formula-bearing proof terms and bare `sjas-axiom` citations are inside the
  first fragment;
- legacy kernel proof-rule traces such as `(conj (false-close))` are outside
  the first fragment even though their symbols are encoded and classified;
- generic sidecars and answer-overlay evidence stay outside the first fragment;
- every symbol in `sjas-code/proof-symbols` has an explicit fragment boundary.

## Exit Criteria

- `proflog.sjas-correspondence-test` passes with the new fragment-boundary
  tests.
- `lein test-proflog-fast` passes because the new audit namespace is on the
  fast gate.
- The ADR index, `LOG.md`, and a focused `docs/log/` note record the change.
- `git diff --check` passes.
