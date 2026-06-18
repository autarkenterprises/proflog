# ADR-0121: SJAS Tab-1 Entry Validation

- Status: completed
- Date: 2026-06-18
- Branch: `adr-0121-sjas-tab1-entry-validation`
- AAR: [AAR-0121](../aar/AAR-0121-sjas-tab1-entry-validation.md)

## Context

[ADR-0119](ADR-0119-sjas-next-research-roadmap.md) defines the next SJAS
research phase around Tab-k proof lists, with Tab-1 as the first positive
implementation target. [ADR-0120](ADR-0120-sjas-tab1-proof-list-surface.md)
created the Tab-1 profile identity, public `tab1-proof/3` and
`dsjas-tab1-proof/3` relation builders, proof-list object syntax, measured
`(S,F,H)` proof objects, and generated Tab-1 SelfCons relation symbols.

That surface is intentionally not yet a checker. Querying `tab1-proof/3` should
not be treated as Tab-1 evidence until the relation decodes the proof-list
object and validates its entries against the arithmeticized SJAS proof
predicate.

ADR-0121 is the next Workstream A slice. Its purpose is to make the public
Tab-1 relations reject malformed or mismatched measured proof-list objects and
accept the smallest meaningful Tab-1 list: one theorem/proof pair where the
theorem is an encoded system axiom and the proof is the fixed `sjas-axiom`
certificate.

## Decision

Implement entry validation for the public Tab-1 proof-list predicates:

- `tab1-proof(S,F,H)` decodes `H` as a `tab1-proof-list-object`;
- a non-empty proof list is required;
- each list entry is decoded as a theorem-code byte payload paired with a
  proof-code byte payload;
- the final entry theorem bytes must match the public target theorem `F`;
- each entry proof is validated by the existing arithmeticized
  `tableau-proof` byte checker against the public system `S` and that entry's
  theorem;
- every intermediate entry, once multi-entry lists are admitted, must pass the
  public `Pi*_1` or `Sigma*_1` formula classifier before it can be reused as a
  Tab-1 theorem;
- `dsjas-tab1-proof(S,F,C)` decodes measured object `C`, checks that its
  embedded system and theorem bytes match the public `S` and `F`, extracts the
  embedded proof-list bytes, then delegates to the same entry validator.

This ADR does not complete theorem-reuse proof search. The accepted entry
checker may validate each entry against the base system first; extending a later
entry's tableau basis by earlier proof-list theorems remains a later ADR unless
this branch explicitly closes it.

## Consequences

- The `:willard-sjas-tab1` profile becomes queryable for single-entry axiom
  citations and can reject mismatched target/system/proof-list payloads.
- Generated Tab-1 SelfCons has a live measured relation predicate, rather than
  only a relation symbol and proof-object codec.
- The `D_SJAS` accounting audit can advance from "deferred to ADR-0121" to
  entry validation implemented, while still recording theorem reuse as open.
- Full Tab-1 completion remains a separate milestone until proof search can
  validate `p_i` from beta plus earlier `t_j` and the classifier restriction is
  exercised on genuine multi-entry reuse.

## Test Obligations

Red first:

- `tab1-proof-accepts-single-entry-axiom-citation` fails because the
  `:willard-sjas-tab1` profile still has no proof-search semantics.
- `dsjas-tab1-proof-accepts-and-checks-measured-proof-lists` fails because the
  measured Tab-1 object is not decoded by the kernel.
- `tab1-proof-rejects-entry-whose-proof-does-not-prove-theorem` fails for the
  same missing predicate reason.
- `tab1-proof-list-accounting-records-entry-validation` fails because the
  correspondence audit still marks entry validation as deferred.

Green after implementation:

- the focused selectors above;
- the ADR-0120 Tab-1 surface selectors;
- nearby `D_SJAS` composite proof-object selectors;
- `lein test-proflog-fast`;
- `lein test-proflog-extended`;
- focused SJAS selectors or `lein test-proflog-sjas` if the runtime is
  acceptable.

## Exit Criteria

- A single-entry Tab-1 proof list citing a beta axiom through `sjas-axiom`
  succeeds through both `tab1-proof/3` and `dsjas-tab1-proof/3`.
- The same proof list is rejected when queried for a different target theorem.
- A measured Tab-1 object is rejected when its embedded system or theorem bytes
  do not match the public call.
- A proof-list entry is rejected when its proof bytes do not prove its entry
  theorem.
- The correspondence audit records the implemented entry-validation slice and
  leaves theorem-reuse completion as a deferred obligation.
