# SJAS Correspondence Fragment Audit

Date: 2026-06-13

ADR: [ADR-0096](../adr/ADR-0096-sjas-correspondence-fragment-audit.md)

Branch: `adr-0096-sjas-correspondence-fragment-audit`

## Context

Track 2b needs a proof-object correspondence boundary, not only a list of
encoded proof symbols. Before this slice, `proflog.sjas-correspondence`
classified every symbol in `sjas-code/proof-symbols` as relevant or excluded,
but it did not distinguish "encoded and classified" from "admitted into the
first correspondence fragment."

That distinction became sharper after Track 1 moved non-`sjas-axiom`
`tableau-proof/3` certificates to formula-bearing structural tableau nodes.
Those accepted proof-code trees do not use legacy proof-rule symbols such as
`conj`, `split`, `false-close`, `eq-step`, `pos-call`, or `neg-call`. The
symbols remain useful as public proof evidence and audit material, but they are
not admitted to the first literature-predicate proof-code fragment by this
Track 2 slice.

## Change

Added `proof-symbol-fragment-boundaries`,
`classify-proof-symbol-fragment`, and
`audit-first-correspondence-fragment` to
`proflog.sjas-correspondence`.

The first fragment is intentionally conservative:

- formula-bearing structural tableau proof terms are inside the fragment when
  their decoded proof term contains no encoded proof-symbol tags;
- the bare `sjas-axiom` symbol is inside the citation fragment;
- every other encoded proof symbol is outside the first fragment unless it is
  already classified as excluded, in which case the fragment audit reports it
  as excluded as well.

This changes no kernel, proof-checker, encoder, query, or answer behavior. It
only exposes a stricter Track 2 audit surface.

## Red/Green Evidence

Red selector before implementation:

```text
lein test :only proflog.sjas-correspondence-test/proof-symbol-fragment-boundary-covers-every-encoded-symbol
Syntax error compiling at (proflog/sjas_correspondence_test.clj:265:33).
No such var: correspondence/proof-symbol-fragment-boundaries
Tests failed.
```

Green focused selectors after implementation:

```text
lein test :only proflog.sjas-correspondence-test/proof-symbol-fragment-boundary-covers-every-encoded-symbol
Ran 1 tests containing 235 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/first-correspondence-fragment-admits-structural-tableaux-and-axiom-citations
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/legacy-proof-rule-tags-are-classified-but-not-admitted-to-first-fragment
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.

lein test :only proflog.sjas-correspondence-test/sidecar-and-answer-overlay-evidence-remain-outside-first-fragment
Ran 1 tests containing 5 assertions.
0 failures, 0 errors.

lein test proflog.sjas-correspondence-test
Ran 16 tests containing 343 assertions.
0 failures, 0 errors.

lein test-proflog-fast
Ran 187 tests containing 992 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 73 tests containing 219 assertions.
0 failures, 0 errors.
```

## Track 2 Result

This does not prove the Track 2b correspondence theorem. It makes a narrower
claim: the executable audit now separates the first admitted proof-code
fragment from the broader encoded proof evidence alphabet. Later Track 2b work
must still prove primitive status, bounded macro expansion, erasure, or
unreachability before admitting additional proof symbols into a correspondence
fragment.
