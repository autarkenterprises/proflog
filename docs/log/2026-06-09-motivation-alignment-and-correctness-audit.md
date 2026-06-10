# Motivation Alignment And Correctness Audit

Date: 2026-06-09

## Scope

User-requested audit of the whole directory: do the constituent projects —
the Proflog implementation, the SJAS `IS#_D(beta)` internalization, and the
conference artifacts — progress in alignment with the stated motivation, and
is the work to date correct? The motivation, as restated by the user: an
executable artifact corresponding to SJAS, to demonstrate and investigate the
translation of the logical property of self-justification into the
computational domain — is there a direct computational equivalent to logical
self-justification; if so, what effect does it have on programming in an
SJAS-lang; if not, is there an equivalent computational property?

Method: full documentation review (ADR/AAR stacks, LOG, audits, the
arithmeticization specification), literature verification against the local
Willard corpus under `sjas/nachlass/papers/`, re-execution of both broad test
gates on the working tree, and git/working-tree archaeology.

## Alignment Assessment

The three strands are coherently aimed at the stated motivation.

- ADR-0073's three-track program is exactly the "what does correspondence
  require and entail" question made operational: Track 1 (arithmeticized
  literature predicate) completed 2026-06-09 per the Track 1 completion
  audit; Track 2a (relevance matrix), 2b (correspondence proof), and 2c
  (`D_Proflog` as a deductive apparatus) are the open arms that carry the
  research question proper.
- The LOPSTR/PPDP 2026 abstract (`lopstr-ppdp26/abstract.txt`) states the
  research objective in the motivation's own terms; the miniKanren 2026
  artifact (`mk2026/`) carries the Proflog-kernel half; the IU Logic Seminar
  talk (2026-05-27) carried the rationale.
- The "effect on programming in an SJAS-lang" arm is so far represented by
  the reflected/external clause boundary (Group-2b versus application layer)
  and the worked example, but has no dedicated ADR after Track 1. The
  lopstr-ppdp26 outline names it ("what programs are facilitated...") — it
  deserves its own tracked workstream once Track 2a exists.

## Correctness Findings — Semantic

1. **Level-1 Group-3 lacked the Pi-star-1 pair restriction** (defect, fixed
   by ADR-0087). Willard 2013 sentence (7) defines `Pair(x,y)` as "`x` is the
   Godel number of a Pi-star-1 sentence and `y` represents `x`'s negation";
   Definition 5.1 carries that Group-3 into `IS#_D(beta)` unchanged. The
   implemented `SelfCons1` quantified over all complementary code pairs,
   asserting full consistency under `D` — strictly stronger than the Level-1
   sentence and outside what Theorems 4.1/5.2 license. Corrected in both the
   builder and profile reconstruction templates.
2. **`Delta-star-0` classifiers were not closed under `not`/`implies`**
   (defect, fixed by ADR-0087) although both are formula-code grammar tags
   and reflected Group-2b clause formulas are built with `implies`.
3. **The builder never validated the reflected basis** (gap, fixed by
   ADR-0087): `pi-star-1?` existed, was tested, and had no caller;
   Definition 5.1's "axioms that have Pi-star-1 encodings" precondition is
   now a build-time error.
4. **Naming drift** (documentation): the ordinary-tableau MVP instance is
   the finite-basis IS(A)-style system of Willard 2001 (Group-3 target
   `0 = 1`); `IS#_D(beta)` proper, per Definition 5.1, carries the Level-1
   Group-3 and corresponds to `:willard-sjas-level1`. ADR-0058 drew the
   distinction; the arithmeticization spec and the Track 1 audits reused
   "the concrete `IS#_D(beta)` instance" for the tableau0 endpoint. Recorded
   in ADR-0087; new records must use profile-qualified naming.
5. **Apparatus strength is the sharpest open fidelity risk** (Track 2a
   obligation, not yet a classified defect). Willard 2005 defines a branch
   as closed *iff it contains a sentence and its negation*; all arithmetic
   knowledge flows through axioms. The implemented checker also closes
   branches through arithmeticized profile relations (byte reading, numeral
   arithmetic, code canonicalization), equality/disequality machinery, and
   recursive `tableau-proof`/`subst-prf`/`axiom-member` closures. The
   committed specification sanctions these as `ClosedBranch` cases, so Track
   1 is complete against the spec, but the spec's apparatus is thereby an
   extension of the literature's `D`. Relatedly, Willard's Group-1 is "a
   finite set of Pi-star-1 sentences which can prove any true Delta-star-0
   sentence", while the implementation carries a small fixed Group-1 and
   absorbs Delta-star-0 truth into apparatus closure rules. Both belong at
   the top of the ADR-0073 Track 2a relevance matrix, alongside the
   proof-size discipline (Willard's >= 5J-bit lower bound has no regression
   test yet). Until Track 2a classifies these, "ordinary semantic tableau"
   claims should be read as "tableau plus arithmeticized closure oracle".
6. **Godel-code exit criterion refreshed**: the value printed in the
   2026-06-06 note (`431159687003828162118819841327179`) predates the
   ADR-0086 `0 = 1` target. Current value for the default ordinary-tableau
   instance, printed from the internalized coding path after ADR-0086/0087
   via `lein print-sjas-selfcons-godel-code`:

   ```text
   1895911909320248794237471524907560082878513227
   ```

## Correctness Findings — Process

- **Commit discipline had lapsed** (AGENTS.md practices 4, 7, 11): twelve
  completed ADR/AAR pairs (0075-0086), eleven `docs/log/` notes, the
  load-bearing `vendor/core.logic-1.0.1/` overlay, `project.clj`'s
  source-path/alias changes, the kernel/language/subst stack-safety and
  hook code, and the final Track 1 profile slices existed only in the
  working tree, while `cljtap.run-section` was referenced by committed
  `project.clj` aliases yet untracked. Corrected as six slice commits
  (`793b25f`..`1fa3e53`), pushed.
- **A superseded durable SelfCons probe was still consuming a core** (~22
  CPU-hours, namespace header only, source predating the ADR-0085 repair;
  AAR-0086 had already recorded it as no longer completion evidence).
  Terminated; termination note appended to its log.
- **Debris removed**: extracted core.logic jar contents (`META-INF/`,
  `clojure/`, `cljs/`), 2026-02-26 root-level snapshots of legacy cljtap
  sources, GV04 debug scratch scripts, an emacs autosave of `kernel.clj`,
  empty `package.json`/`package-lock.json`, and the empty `.codex` file.
  The pre-greenfield deep-research reports moved to `docs/research/`.
  `.gitignore` now covers build output, session state, `test-runs/`, editor
  droppings, and the nested conference repositories.
- **The opaque SJAS namespace gate has not been runtime-green since
  ADR-0086** (found 2026-06-10 while verifying ADR-0087's slow selectors):
  `sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile`
  and `sjas-subst-prf-checks-selfcons-fixed-point-certificate` each exceed
  40-45 minute timeouts at commit `1fa3e53`, before ADR-0087, and were
  stopped past two CPU-hours each at `e18f7b7`. The positive fixed-point
  checks pass; the cost concentrates in whole-program queries (every query
  decomposes the full `AxiomConj`, gamma-instantiating against the enlarged
  Group-3 codes) and in negative exhaustive searches. These vars were not in
  AAR-0086's focused selector list, and the fast/extended gates do not cover
  the SJAS namespace. Disposition: AAR-0086/0087 follow-ups recorded,
  runtime rows added to `TEST_RUNTIME_BASELINE.md`, and the re-baseline and
  scheduling investigation proposed as ADR-0088.
- **Remaining manual items**: `node_modules/@openai` is root-owned (npm run
  as root on 2026-04-26) and needs `sudo rm -rf node_modules`; the git
  remote URL embeds a GitHub PAT in plaintext (`.git/config`) — move it to a
  credential helper and rotate the token.

## Conference Artifact Risk

`lopstr-ppdp26/` and `mk2026/` carry frozen copies of the Proflog source
(own git histories). Both snapshots predate the 2026-06-09 literature
corrections: the ADR-0086 Tableau-0 `0 = 1` SelfCons target and the ADR-0087
Level-1 Pi-star-1 pair restriction. The LOPSTR/PPDP system description
claims an `IS#_D(beta)` implementation, which after this audit specifically
means the corrected Level-1 Group-3. Refresh both artifact snapshots from
this branch before submission or camera-ready.

## Disposition

- ADR-0087 (with AAR-0087) corrects findings 1-3 with red/green evidence and
  both broad gates.
- Findings 4-5 are recorded here and in ADR-0087; finding 5 defines the
  opening entries of the Track 2a relevance matrix, which is the next
  ADR-0073 work item.
- Process corrections are committed and pushed; manual items are listed
  above for the user.
