# ADR-0140: SJAS Boundary Proof-Route Validation

- Status: completed
- Date: 2026-06-19
- Branch: `adr-0140-sjas-boundary-proof-route-validation`
- AAR: [AAR-0140](../aar/AAR-0140-sjas-boundary-proof-route-validation.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) requires Workstream B to
demonstrate each negative variant against that variant's own generated SelfCons
statement. ADR-0127 rejects direct Group-3 citation and the known ordinary
SelfCons tableau, but ADR-0131's verifier still trusts the candidate field
`:uses-reduced-witness?`. ADR-0132, ADR-0135, and ADR-0138 then validate a proof
of the positive Group-3 theorem rather than a counterexample to the proof
predicate quantified by SelfCons.

That is not rigorous boundary evidence. Group 3 is itself an axiom, so both an
`sjas-axiom` citation and a structural path that selects Group 3 prove
`SelfCons(S)` without using the negative variant. Renaming or wrapping that
ordinary path does not turn it into evidence that SelfCons fails.

For a Level-1 system, the relevant counterexample is a concrete tuple
`(x,y,p,q)` satisfying the positive body of `not(SelfCons(S))`:

```text
PiStar1Code(x)
/\ NegPair(x,y)
/\ DSubstPrf(S,g,x,p)
/\ DSubstPrf(S,g,y,q)
```

For the Tab-2 boundary profile the corresponding body omits the Level-1 class
guard and uses `DTab2Prf`. The current Tab-2 target names that relation but does
not implement it, so Tab-2 evidence cannot honestly pass until a later ADR adds
the checker.

## Decision

Replace metadata-based Workstream B route claims with a derived validation
contract:

- candidate intake no longer treats `:uses-reduced-witness?` as evidence;
- a constructed or synthesized candidate must carry the concrete theorem-code,
  complement-code, and measured proof-object tuple required by its generated
  SelfCons formula;
- target-specific validation must run the applicable class, complement, and
  measured proof predicates through the SJAS kernel against the exact generated
  system and fixed-point substitution code;
- the underlying proof certificates must decode successfully;
- at least one kernel-accepted structural proof route must select an exact
  reduced-witness formula as a formula-bearing node;
- no accepted route may select the generated Group-3 formula as its closing
  premise;
- proof-route facts are derived from decoded proof objects and exact formula
  bytes, never copied from candidate booleans;
- the legacy positive-Group-3 validation helpers remain available as diagnostic
  compatibility APIs, but their results cannot complete a Workstream B evidence
  obligation.

The route requirement is intentionally stronger than system membership. Merely
embedding the reduced witness in `system-code`, or placing its code in candidate
metadata, does not show that a proof used it. A formula-bearing node in a proof
tree accepted by the kernel is an executed tableau selection, so exact node
matching provides inspectable proof-route evidence.

ADR-0140 hardens the contract and records unavailable proof relations. It does
not claim any of the six final evidence obligations. Later certificate and
synthesis ADRs must satisfy this contract with actual counterexample tuples.

## Consequences

- Ordinary SelfCons citations, ordinary structural SelfCons tableaux, and
  renamed equivalents remain insufficient.
- Existing single-proof validation reports are explicitly diagnostic rather
  than evidence-completing.
- Total-multiplication and Xtab/LEM counterexamples can use the implemented
  Level-1 `dsjas-subst-prf/4` relation.
- Tab-2 remains blocked on an arithmeticized `dsjas-tab2-proof/3` checker; the
  validator must report that fact rather than validate through Tableau-0.
- Final evidence is larger than one proof code because SelfCons itself quantifies
  over a complementary theorem pair and two measured proof objects.

## Test Obligations

Red first:

- candidate screening still trusts `:uses-reduced-witness?`;
- the generic verifier accepts a successful positive-SelfCons validation record;
- no proof-route report derives witness use from decoded proof objects;
- Tab-2 validation can still present its Tableau-0 bridge as boundary evidence.

Green after implementation:

- changing only `:uses-reduced-witness?` never changes acceptance;
- a legacy positive-SelfCons validation report completes no obligation;
- direct Group-3 citation and the ordinary structural Group-3 path are rejected;
- route reports distinguish an exact reduced-witness node from system-code
  membership and from unrelated structural nodes;
- malformed, mismatched, or unreadable measured proof objects are rejected;
- Level-1 counterexample validation checks class, complement, both measured
  proofs, exact system/fixed-point codes, and derived witness-route use;
- Tab-2 reports `:proof-relation-unavailable` until its checker exists;
- all new code paths have focused tests;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- `lein test-proflog-sjas`.

## Exit Criteria

- Workstream B verification no longer trusts candidate route metadata.
- No proof of the positive Group-3 theorem can complete a boundary obligation.
- Public validation reports expose kernel counterexample checks and derived
  route checks separately.
- The roadmap records the Tab-2 checker as a prerequisite for final evidence.
- No final Workstream B evidence obligation is closed by this ADR.
