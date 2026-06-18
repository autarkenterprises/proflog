# ADR-0120: SJAS Tab-1 Proof-List Surface

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0120-sjas-tab1-proof-list`
- AAR: [AAR-0120](../aar/AAR-0120-sjas-tab1-proof-list-surface.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) made Tab-k/Tab-1 the first
positive SJAS research workstream. It also required future Tab-k ADRs to start
red on proof-list predicates/builders, classifier restrictions, and generated
SelfCons relation-symbol changes.

The existing implementation deliberately stops before Tab-1. It has ordinary
semantic-tableau proof-code bytes, public `tableau-proof/3` and `subst-prf/4`,
measured `D_SJAS` objects for the generated SelfCons predicates, and Level-1
`Pi*_1` / `Sigma*_1` classifiers. It does not yet have a selected
`:willard-sjas-tab1` apparatus, proof-list theorem-reuse object, or generated
SelfCons sentence that quantifies a Tab-1 proof-list object.

ADR-0120 is the first implementation slice. It establishes the public surface
and coding boundary only. Arithmeticized validation of each list entry and proof
search over reusable intermediate theorems belongs to a later ADR.

## Decision

Add the Tab-1 proof-list surface without claiming a completed proof checker:

- add a `:willard-sjas-tab1` profile identity to the SJAS source/system code;
- add public relation builders for `tab1-proof/3` and measured
  `dsjas-tab1-proof/3`;
- add a `tab1-proof-list-object` proof-code payload whose entries are
  theorem/proof-code byte pairs, matching ADR-0119's
  `H = [(t1,p1), ..., (tn,pn)]`;
- add a measured `dsjas-tab1-proof-object` payload that embeds `(S,F,H)`, where
  `S` is system code, `F` is target theorem code, and `H` is the public Tab-1
  proof-list object code;
- generate Tab-1 Group-3 with `dsjas-tab1-proof`, not
  `dsjas-subst-prf`, when `:profile :willard-sjas-tab1` is selected;
- record a correspondence audit that maps Willard Rank-1* / `U1*`
  terminology to the existing local `Pi*_1` / `Sigma*_1` classifiers and marks
  ADR-0092's `pi-star-1-encodable?` as host-side basis admission, not a public
  Tab-1 intermediate-theorem classifier.

The new proof-code symbols must be appended to stable tables rather than
inserted ahead of existing entries, preserving earlier code indices.

## Consequences

- A Tab-1 system can be constructed and inspected, but proof search through
  `tab1-proof/3` is not yet complete.
- Generated Tab-1 SelfCons has the intended measured relation symbol, which
  lets later ADRs focus on arithmeticized entry validation.
- The selected size measure extends ADR-0108/0109 accounting with a Tab-1 list
  object: structural tableau entries still measure proof-code bytes, while a
  generated Tab-1 SelfCons proof object measures `(S,F,H)`.
- Tab-2 and stronger variants remain outside the positive implementation path.

## Test Obligations

Red first:

- `sjas-tab1-profile-group-three-uses-tab1-proof-list-vocabulary` fails because
  `:willard-sjas-tab1` is unsupported and no Tab-1 SelfCons relation exists.
- `tab1-proof-list-object-encodes-theorem-proof-pairs` fails because the public
  proof-list object builders are missing.
- `tab1-roadmap-audit-reconciles-rank1-terminology` fails because no executable
  Tab-1 terminology audit exists.
- `tab1-proof-list-accounting-records-measured-object` fails because
  `Log_D_SJAS` accounting has no Tab-1 measured-object clause.

Green after implementation:

- focused selectors for the tests above;
- existing `D_SJAS` composite proof-object tests;
- `lein test-proflog-fast`;
- `lein test-proflog-extended` if the changes touch proof search behavior.

## Exit Criteria

- The Tab-1 profile system builds and its Group-3 formula quantifies
  `dsjas-tab1-proof` proof objects for both sides of a `Pi*_1` complement pair.
- Public proof-list and measured Tab-1 proof-object builders round-trip through
  the existing proof-code byte decoder.
- The correspondence/accounting audit records the classifier reconciliation,
  current ADR scope, and deferred arithmeticized validation obligations.
- No test or documentation claims that Tab-1 proof checking is complete.
