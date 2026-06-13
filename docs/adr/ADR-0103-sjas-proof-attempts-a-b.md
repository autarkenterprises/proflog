# ADR-0103: SJAS Proof Attempts A and B

- Status: completed
- Date: 2026-06-13
- Branch: `adr-0103-sjas-proof-attempts-a-b`
- AAR: [AAR-0103](../aar/AAR-0103-sjas-proof-attempts-a-b.md)

## Context

ADR-0102 refuted ADR-0100 as stated and split the remaining work into two
corrected proof targets:

- Path A: a narrowed literal-Willard structural fragment.
- Path B: an extended selected `D_SJAS` apparatus.

Both paths need the same missing foundation: an executable inventory of every
structural checker branch, classified by whether it is a literal Willard rule,
bookkeeping lemma, truth/NNF lemma, quantifier lemma, or an excluded/extended
SJAS rule family.

## Decision

Add an executable branch-level proof audit to `proflog.sjas-correspondence`.
This audit will not change proof search or the kernel. It will expose:

- every `sjas-structural-proof-check-state-decodedo` branch by line range;
- Path A status for each branch;
- candidate `D_SJAS` rule family or families for each branch;
- open proof obligations for Path A and Path B.

Use this to advance both proof attempts:

- Path A can be pursued as a narrowed theorem over only direct/lemma branches.
- Path B can be pursued as a candidate extended apparatus, while retaining the
  unresolved obligations for `sjas-axiom` size accounting, recursive proof
  well-foundedness, and literature admissibility.

## Consequences

The audit makes proof status executable and reviewable. It does not, by itself,
complete either proof. A future ADR must still discharge the listed lemmas or
choose the Path B proof-object accounting.

After implementation, Path A is reduced to six named lemmas over the narrowed
fragment, and Path B is reduced to a candidate `D_SJAS` inventory plus the
global blockers for `sjas-axiom` size accounting, recursive proof
well-foundedness, and literature admissibility.

## Test Obligations

- Red tests must require the new Path A branch audit API.
- Red tests must require the new Path B `D_SJAS` apparatus audit API.
- The tests must verify branch coverage, excluded Path A branches, Path B
  candidate rule families, and open blockers.

## Exit Criteria

- Focused correspondence audit tests pass.
- ADR-0103 proof-attempt notes record the Path A and Path B status after the
  executable inventory is added.
- Broad fast/extended regression gates remain green before commit.
