# ADR-0095: SJAS Proof Synthesis

- Status: accepted
- Date: 2026-06-10
- Branch: `adr-0095-sjas-proof-synthesis`
- AAR: [AAR-0095](../aar/AAR-0095-sjas-proof-synthesis.md)

## Context

The [SelfCons execution discussion](../log/2026-06-10-selfcons-execution-discussion.md)
identified the remaining gap in the self-verification claim: the artifact
*validates* supplied proof codes of its own consistency sentence; it has
not yet been shown to *generate* them. Because the arithmeticized
`tableau-proof/3` is a pure relation over codes, miniKanren mode
polymorphism should close the gap: running the same relation with the
proof-code argument a fresh variable turns the checker into a prover.

Two regimes are distinguished. **Citation synthesis**: when the theorem
code is an axiom of the system, the certificate branch should bind a fresh
proof variable to the `sjas-axiom` citation certificate once
`axiom-member(s,t)` reconstructs — for `t` the Group-3 code, this is the
runtime generating the Henkin proof of its own consistency. **Structural
synthesis**: a fresh proof variable against the structural branch makes
the node-by-node validator enumerate proof trees — genuine tableau proof
search through the arithmetized apparatus, whose tractability is unknown
and whose products are native artifacts of the implemented `D` (direct
Track 2a/2b comparison material).

The dual-probe hotspot review (84 stack samples per probe) found
ADR-0090/0094 have exhausted the constant-factor levers: keyword-lookup
frames fell from 58/84 samples to 0, `LVar.equals` from 20 to 0, the
ADR-0090 scanner's deep descent appears in only 2/84 samples, and the
residual profile is walk/occurs over variable-dense terms — structural
search width. Synthesis is therefore the next *semantic* frontier rather
than a further micro-optimization target.

Whether citation synthesis already works is an empirical question: the
byte relations accept non-ground codes (the bound-code-decoding selectors
prove the decode direction), but the generate direction has never been
exercised. The new tests are capability probes in the red/green sense
that they fail or diverge exactly when the capability is absent; any such
failure drives mode-completeness repairs to the certificate/byte
relations inside this ADR.

## Decision

1. Add citation-synthesis regressions: query `tableau-proof(s, t, P)`
   with `P` a fresh object variable through `sjas/query-answers`, for
   (a) a beta axiom code and (b) the Tableau-0 Group-3 SelfCons code,
   asserting the synthesized binding equals
   `(sjas/proof-certificate 'sjas-axiom)`.
2. Extend `proflog.sjas-runtime-probe` with a `synthesis` case that runs
   the same fresh-variable query under a bounded fuel and prints elapsed
   time and the synthesized certificate (or its absence) — execution-
   behavior data per the 2026-06-10 doctrine, ahead of any structural-
   synthesis attempt.
3. Repair relational mode gaps surfaced by the tests, red/green, without
   weakening the checking direction or the source audit. The localized
   repairs (see the [interdev review](../interdev/2026-06-13-adr-0095-proof-synthesis-review.md),
   with which this ADR concurs) are:
   a. **Construct, do not read backward.** The synthesis branch builds the
      `sjas-axiom` certificate from its fixed bytes through the canonical
      compact builder `sjas-internal-code-termo`, not by running the
      presented-code reader `sjas-public-code-byteso` backward. The reader
      accepts non-canonical byte numerals (`code-byte-termo` reads numerals
      arithmetically), so it is deliberately many-to-one and not a bijection;
      run backward it yields a non-canonical, non-admissible term that answer
      export silently drops. The builder uses `code-byte-build-termo` (the
      canonical 64-entry table) and is verified to reproduce
      `(sjas/proof-certificate 'sjas-axiom)` exactly and deterministically.
   b. **Harden the reader's forward direction.** `code-argso` /
      `code-args-coreo` decoded a `code-N` term without relating the declared
      constructor `byte-count` to the actual argument count, so a malformed
      `(app code-2 b0 b1 b2)` read as valid. The count is now threaded through
      the single decoding walk (perf-neutral on the forward path), so
      arity-mismatched terms are rejected.
   c. **Share the destructure preamble.** Extract
      `sjas-tableau-proof-destructureo` (the negated-atom walk to the three
      code arguments) from `sjas-tableau-proof-callo` and reuse it in the
      synthesis branch, so the single proof-code position does not drift
      between the checking and synthesizing branches.

## Test Obligations

- The two citation-synthesis selectors, red (failing or diverging) before
  any repair this ADR introduces, green after; existing citation,
  ADR-0087, source-audit selectors and both broad gates stay green.
- Probe `synthesis` case committed with at least one recorded run.

## Exit Criteria

- The runtime synthesizes the citation certificate for both targets,
  including the system's own SelfCons sentence; timings recorded in
  AAR-0095; structural-synthesis tractability assessed (probe data), with
  any pursuit of it deferred to a successor ADR.
