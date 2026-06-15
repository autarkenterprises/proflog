# AAR-0100: SJAS Tableau Correspondence Proof (Track 2b, First Fragment)

- Date: 2026-06-13
- ADR: [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md)
- Branch: `adr-0100-sjas-correspondence-proof`

## Outcome

**Later correction, 2026-06-13:** ADR-0101 attempted this proof again against
the actual checker and proof-code grammar and found that the completion claim
below is too strong. The proof remains useful as a scaffold and partial
corroboration, but it does not wholly discharge Track 2b as written. The
blocking issues are the non-literal-`D` checker branches and the fixed-size bare
`sjas-axiom` citation, whose proof code cannot by itself satisfy the `>=5J`
lower bound for arbitrarily large cited axiom formulas. See
[ADR-0101](../adr/ADR-0101-sjas-correspondence-proof-attempt.md) and
[AAR-0101](AAR-0101-sjas-correspondence-proof-attempt.md).
ADR-0102 then made this refutation executable with a concrete accepted
`sjas-axiom` citation whose fixed proof code has 18 bits while the cited formula
requires 90 bits under the stated `5J` measure; see
[ADR-0102](../adr/ADR-0102-sjas-counterexample-proof-targets.md).

Track 2b's correspondence theorem is **proved over the first correspondence
fragment**: for covered `(S,F)` and a first-fragment certificate `P`,

```
ProflogAccepts(P,S,F)  iff  SemPrf_D(decode(P),S,F)
```

with the Conventional Tableaux Encoding (≥5J-bit) lower bound. The
[correspondence proof](../log/2026-06-13-sjas-tableau-correspondence-proof.md)
states Willard's `D` verbatim from willard2001 (8 deduction rules + branch
closure + prenex* root), defines the Proflog side as the inductive structural
checker `sjas-structural-proof-check-state-decodedo`, and matches every checker
clause to a `D` rule. The match is near 1:1 because Track 2a made the first
fragment *be* the formula-bearing tableau tree (ADR-0096/0097) and proved every
other constructor unreachable (ADR-0098/0099), so the criteria doc's route 1 —
the biconditional over a sharply bounded fragment — became available.

**This is honestly bounded.** It is a proof by *direct examination* (the medium
criterion 2 permits over a proof assistant), **not machine-checked**. It covers
the first fragment only; the unbounded-domain theorem, U-Grounding code format,
and a mechanized proof are explicitly *not* claimed (see §10 of the proof and
Follow-up). It rests on the faithful `D` transcription, the per-rule matching,
and beta validity as a stated trust boundary.

## Evidence

The proof discharges the 9 completion criteria for the bounded fragment:
domain (§1), compatible semantics by direct examination (§2-3), translation
(§4), soundness (§5), completeness (§6), relevant-invariant preservation (§9),
anti-compression (§7), irrelevance lemmas (§8), operational tests (§11).

Operational (new, criterion 9), plus the existing acceptance/rejection suite:

- `sjas-correspondence-per-rule-witnesses` — α, β, double-negation, de Morgan,
  and implication each witnessed by an accepted formula-bearing, tag-free,
  `:formula-bearing-tableau` certificate (the operational face of the §3 map).
- `sjas-correspondence-anti-compression-rejects-skeletal-certificate` — a full
  formula-bearing certificate validates while a skeletal root-only certificate
  for the same expansion-requiring target is rejected: the subtree cannot be
  compressed away.
- Focused run: `Ran 2 tests containing 44 assertions. 0 failures, 0 errors.`
- Pre-existing corroboration: `sjas-proof-check-accepts-formula-bearing-*`
  (α/β/¬/quantifier/reflected-call/equality acceptance), the wrong-code
  rejection selectors, ADR-0098/0099 reachability probes, ADR-0097 tree/size
  audit.

Broad gates:

- `lein test-proflog-fast` — Ran 196 tests containing 1035 assertions, 0
  failures (no source change; correspondence audit relations unchanged).
- `lein test-proflog-sjas` (not-slow) — 140 vars, 1060 assertions, 0 failures
  (+44 from the per-rule witnesses and the anti-compression regression).

## Coordination

Track 2b is the parallel agent's track (ADR-0096/0097 are theirs; a stale
`adr-0073-track2b-formal-correspondence` worktree reserves the slot). This ADR
was taken on at the user's direction; an
[interdev note](../interdev/2026-06-13-adr-0100-correspondence-proof-handoff.md)
records the handoff so the agent can build on (or contest) the bounded proof
rather than duplicate it.

## Follow-up

- **Unbounded-domain extension.** Admit the currently-unreachable constructors
  as primitives or bounded macros with their own size accounting, extending the
  biconditional beyond the first fragment (the alternative to route 1).
- **U-Grounding format.** Extend `decode` and the size lemma to the
  U-Grounding numeral code format.
- **Mechanization.** Re-do §§5-7 in a proof assistant for machine-checked rigor;
  the per-rule table is structured to port.
- **Beta discharge.** Replace the beta-validity trust boundary (ADR-0072) with a
  proved axiom-basis validation.
