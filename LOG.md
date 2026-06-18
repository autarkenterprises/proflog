# Development Log

This log is the project timeline: inclusive process notes, links to durable
records, exploratory turns, dead ends, backtracks, and decision context that may
not belong in a polished README, Memory, Lessons, ADR, or AAR.

It does not supersede specialized records. Instead, it is the spine from which
those views branch:

- [MEMORY.md](MEMORY.md) keeps high-priority facts that should remain present
  in working context.
- [LESSONS.md](LESSONS.md) captures lessons learned during the work.
- [ADR records](docs/adr/README.md) capture feature-sized decisions before
  implementation.
- [AAR records](docs/aar/README.md) capture post-implementation outcomes.
- `docs/log/` contains longer notes linked from dated entries here.

This file was introduced on 2026-04-29 after the project was already mature.
Entries before that date are reconstructed from git history and existing
documentation, so they intentionally summarize rather than pretend to be a
complete contemporaneous transcript.

## 2026-06-18

- Spawned [ADR-0127](docs/adr/ADR-0127-sjas-boundary-evidence-screen.md)
  from ADR-0119 Workstream B on branch
  `adr-0127-sjas-boundary-evidence-screen`. This slice adds an executable
  evidence screen so ordinary Group-3 SelfCons citations and structural
  SelfCons tableaux cannot satisfy the remaining total-multiplication
  boundary-failure obligations. Completed with
  [AAR-0127](docs/aar/AAR-0127-sjas-boundary-evidence-screen.md); gates:
  `lein test-proflog-fast` 223/1393 green,
  `lein test-proflog-extended` 78/277 green, and `lein test-proflog-sjas`
  `pass=1154 fail=0 error=0`.
- Spawned [ADR-0126](docs/adr/ADR-0126-sjas-total-mul-full-target.md)
  from ADR-0119 Workstream B on branch
  `adr-0126-sjas-total-mul-full-target`. This slice targets the full
  generated SelfCons contradiction target for the total-multiplication reduced
  witness system, while deliberately keeping constructed-certificate and
  proof-search synthesis evidence open. Completed with
  [AAR-0126](docs/aar/AAR-0126-sjas-total-mul-full-target.md); gates:
  `lein test-proflog-fast` 221/1377 green,
  `lein test-proflog-extended` 78/277 green, and `lein test-proflog-sjas`
  `pass=1154 fail=0 error=0`.
- Spawned [ADR-0125](docs/adr/ADR-0125-sjas-total-mul-reduced-witness.md)
  from ADR-0119 Workstream B on branch
  `adr-0125-sjas-total-mul-reduced-witness`. This slice targets the reduced
  reflected-beta squaring-chain witness for the total-multiplication negative
  variant, leaving the full SelfCons contradiction target and synthesis
  evidence for later Workstream B ADRs. Completed with
  [AAR-0125](docs/aar/AAR-0125-sjas-total-mul-reduced-witness.md); gates:
  `lein test-proflog-fast` 221/1377 green,
  `lein test-proflog-extended` 78/277 green, and `lein test-proflog-sjas`
  `pass=1142 fail=0 error=0`.
- Spawned [ADR-0124](docs/adr/ADR-0124-sjas-boundary-variant-surface.md)
  from ADR-0119 Workstream B on branch
  `adr-0124-sjas-boundary-variant-surface`. This slice adds the first
  total-multiplication negative-variant surface and executable witness contract
  without claiming the reduced/full SelfCons contradiction witnesses. Completed
  with [AAR-0124](docs/aar/AAR-0124-sjas-boundary-variant-surface.md); gates:
  `lein test-proflog-fast` 221/1376 green,
  `lein test-proflog-extended` 78/277 green, and `lein test-proflog-sjas`
  `pass=1130 fail=0 error=0`. Reduced/full SelfCons witnesses, constructed
  certificates, and proof-search synthesis evidence remain open for Workstream
  B completion.
- Spawned [ADR-0123](docs/adr/ADR-0123-sjas-self-extension-pair-survey.md)
  from ADR-0119 Workstream C on branch
  `adr-0123-sjas-self-extension-pair-survey`. This slice records the required
  beta-axiomatizable data-encoding survey and selects reflected pair projection
  axioms as the first self-extension demo before list axioms. Completed with
  [AAR-0123](docs/aar/AAR-0123-sjas-self-extension-pair-survey.md); gates:
  `lein test-proflog-fast` 220/1368 green,
  `lein test-proflog-extended` 78/277 green, and `lein test-proflog-sjas`
  `pass=1119 fail=0 error=0`. List recursion and Workstream B negative
  boundary variants remain open.
- Spawned [ADR-0122](docs/adr/ADR-0122-sjas-tab1-theorem-reuse.md) from
  ADR-0119 Workstream A on branch `adr-0122-sjas-tab1-theorem-reuse`. This
  slice targets the remaining Tab-1 proof-list obligation from ADR-0121:
  validating later entries against beta plus earlier reusable `Pi*_1` /
  `Sigma*_1` theorem entries, both for `sjas-axiom` citation and structural
  tableau antecedents. Completed with
  [AAR-0122](docs/aar/AAR-0122-sjas-tab1-theorem-reuse.md); gates:
  `lein test-proflog-fast` 219/1361 green, `lein test-proflog-extended` 78/277
  green, and `lein test-proflog-sjas` `pass=1107 fail=0 error=0`.
- Spawned [ADR-0121](docs/adr/ADR-0121-sjas-tab1-entry-validation.md) from
  ADR-0119/ADR-0120 Workstream A on branch
  `adr-0121-sjas-tab1-entry-validation`. This slice targets executable
  `tab1-proof/3` and `dsjas-tab1-proof/3` entry validation: proof-list object
  decoding, measured `(S,F,H)` payload checks, and arithmeticized validation of
  each theorem/proof entry through the existing SJAS tableau proof predicate.
  Completed with [AAR-0121](docs/aar/AAR-0121-sjas-tab1-entry-validation.md);
  gates: `lein test-proflog-fast` 219/1360 green,
  `lein test-proflog-extended` 78/277 green, and `lein test-proflog-sjas`
  `pass=1099 fail=0 error=0`. Theorem-reuse proof search remains the next
  Workstream A obligation.
- Spawned [ADR-0120](docs/adr/ADR-0120-sjas-tab1-proof-list-surface.md) from
  ADR-0119 Workstream A on branch `adr-0120-sjas-tab1-proof-list`. This first
  Tab-1 implementation slice is intentionally limited to profile identity,
  proof-list object coding, measured `(S,F,H)` accounting, terminology
  reconciliation, and generated SelfCons relation symbols; arithmeticized
  proof-list validation is deferred to a later ADR. Completed with
  [AAR-0120](docs/aar/AAR-0120-sjas-tab1-proof-list-surface.md); gates:
  `lein test-proflog-fast` 219/1357 green, `lein test-proflog-extended` 78/277
  green, and `lein test-proflog-sjas` `pass=1092 fail=0 error=0`.
- Added [ADR-0119](docs/adr/ADR-0119-sjas-next-research-roadmap.md) on branch
  `adr-0119-sjas-next-research-roadmap` as a planning/control ADR for future
  SJAS `/goal` loops. It preserves three spawnable workstreams: Tab-k/Tab-1
  proof-list reuse, programmatized Goedel-boundary failures, and
  self-interpretation/self-extension via reflected beta changes. This branch is
  docs-only and does not add an AAR unless implementation begins here.

## 2026-06-17

- Fitting-fidelity audit of the greenfield Proflog core, on branch
  `fitting-fidelity-audit` (independent of `origin/main` by request; reconcile at
  close). Anchors the core to Fitting's *Tableaus for Logic Programming*
  (`LPTableaus.pdf`) §2–§8 — the project had a Willard-D correspondence but no
  systematic Fitting anchor. Matrix: [docs/FITTING_FIDELITY_AUDIT.md](docs/FITTING_FIDELITY_AUDIT.md).
- Method discipline: every verdict is backed by a test, not an assertion (an
  early automated exploration had over-asserted "compliant" and hallucinated
  kernel line numbers, so all anchors were re-verified against source).
- Interrogation suite `proflog.fitting-fidelity-test` (fast gate, 8 tests / 222
  assertions): §3 supervaluation occurs subtlety (ground/var close, existential
  ⊥, Fitting p.6); §3 P1 tautology-vs-⊥; §4 NNF negation duals (incl.
  ¬∃→once-forall); §6 `l-ground` guard; §2 one-clause-per-relation; and a §7
  propositional differential (kernel validity == truth-table tautology over 200
  random formulas — no spurious closure, and complete, for the propositional core).
- Phase 2b quorum `proflog.proof-quorum-test` (extended gate, 5 tests / 58
  assertions): kernel-as-prover → kernel-as-checker (the same relation with the
  `proof` bound) → independent non-relational `src/proflog/proof_check.clj`. All
  agree on genuine certificates (incl. P2 `win(4)`) and reject mutants.
- Findings: [ADR-0116](docs/adr/ADR-0116-fitting-free-variable-procedure-call.md)
  — the core Procedure Call Rule is Fitting's §8 free-variable call, not the §6
  ground-only rule, with `l-ground-termo` as his keep-in-L mechanism (corrected an
  earlier core/overlay hypothesis: the extension is in the core).
  [ADR-0117](docs/adr/ADR-0117-quorum-proof-checking.md) — quorum + proof-term
  adequacy: certificates are pure tag trees recording the rule per node but no
  witnesses, so the independent oracle is structural-only and kernel-as-checker
  supplies the semantic re-validation (check-determinism).
  [ADR-0118](docs/adr/ADR-0118-fitting-audit-secondary-findings.md) — secondary
  dispositions (⊥-vs-`:unresolved`, γ-envelope, Substitutivity-via-σ,
  answer-overlay soundness). Numbered 0116+ to clear the parallel agent's pushed
  0111–0115.
- Gates: fast 217/1341 green (fidelity suite included); quorum 5/58 green.
- Deferred: precise `⊥`; first-order/equality differential soundness; first-order
  γ-envelope quantification; line-level `answer_overlay` diff; reconcile with
  `origin/main` (ADR-0112 golden suite / 0113 renderer / 0114 open-branch witness).

## 2026-06-16

- LOPSTR+PPDP, miniKanren 2026, and Clojure/Conj submissions completed.
- Expanded conference search: [docs/conference/expanded-venue-map.md](docs/conference/expanded-venue-map.md)
  and [open-by-date.md](docs/conference/open-by-date.md) (open CFPs ranked by nearest conf date, verified 2026-06-20).
- Implemented the conference / CFP mapping plan (plan file left unchanged in
  `.cursor/plans/`). Added [docs/conference/](docs/conference/) navigation,
  [deadline-verification.md](docs/conference/deadline-verification.md) (Tier
  A/B deadlines verified from official sites), and
  [csl-2027-outline.md](docs/conference/csl-2027-outline.md) for the ADR-0100+
  correspondence track.
- Added submission checklists: [lopstr-ppdp26/SUBMISSION.md](lopstr-ppdp26/SUBMISSION.md),
  [mk2026/SUBMISSION.md](mk2026/SUBMISSION.md).
- Verified both paper artifacts build and pass their gates: LOPSTR+PPDP system
  description (9 pp), miniKanren kernel paper (7 pp, including core.logic host
  engine and reproducibility sections). Fixed mk2026 LaTeX build failure caused
  by a line-broken `\texttt{q,` / `\texttt{run}` pair in the layer diagram.
- Added [docs/conference/us-speaking-opportunities.md](docs/conference/us-speaking-opportunities.md):
  US seminar and colloquium outreach map (NYC CTS tier, logic colloquia, PL
  seminars, MAMLS/NJPLS/NEPLS, virtual proof-theory series, pitch template).

## 2026-06-14

- Completed [ADR-0110](docs/adr/ADR-0110-mode-directed-ground-before-decode.md)
  (width-reduction #1, mode-directed ground-before-decode), the ADR-0106
  highest-leverage lever. The formula/term byte decoders placed the constructor
  `==` *last*, so a ground formula could not drive the recursive byte-decodes
  (conjunction is sequential per answer) — backward decode of even `0 = 0` did
  not complete in 70 s. Reordered the constructor `==` to the front of all 15
  `decode-formula-byteso` branches, `decode-term-byteso` var/par,
  `decode-app-termo`, and bound the output cons before recursion in
  `parse-code-payload-byteso` / `parse-term-list-byteso` and ahead of the parse
  (inside the payload `fresh`) in the two `*-bodyo` decoders. Pure conjunction
  reordering — answer-set-identical. Backward decode now **0.33–1.19 ms**
  (deterministic) vs non-terminating; forward unchanged (~0.94 ms). An initial
  hoist above the header-length checks tripped the
  `sjas-embedded-payload-decoders-check-header-before-payload-fresh` guard
  (transient 1058/2); narrowed to inside the payload `fresh` to honour that
  forward-mode invariant. Gates: fast 198/1047/0 (incl. ADR-0093 canonical +
  new `proflog.decode-mode-directed-test`), SJAS not-slow 1060/0/0.
  **Whole-gate before/after (vs pristine 128e819, clean contention-free runs) ≈
  1.01× overall (flat)** — localised wins where decode carries ground structure
  (`rejects-arity-mismatched` 4.15×, `anti-compression…skeletal` 3.47×,
  `axiom-member-query…` 1.62×), but the heavy proof-check tests are **unchanged**
  (`rejects-wrong-public-code` 1.03×, `distinct-nested-existential` 1.01×) because
  the checker decodes each node formula while it is still free — the empirical
  case for the proof-checker ground-target propagation (detailed as ADR-0110's
  "Proposal" section, the named successor). Also defers the `decode-syntax-*`
  family. See [AAR-0110](docs/aar/AAR-0110-mode-directed-ground-before-decode.md).
- Completed [ADR-0109](docs/adr/ADR-0109-dsjas-composite-proof-object-internalization.md)
  on `adr-0109-dsjas-composite-proof-object`: generated Tableau-0 and Level-1
  SelfCons now quantify measured `D_SJAS` composite proof-object codes through
  `dsjas-tableau-proof/3` and `dsjas-subst-prf/4`, with payloads `(S,F,P)` and
  `(S,G,F,P)` decoded through the arithmeticized public byte/proof-code
  relations. Public `tableau-proof/3` and `subst-prf/4` remain compatible. The
  correspondence audit now classifies the composite proof-object symbols and
  extends `Log_D_SJAS` accounting to the substitution object. Final gates passed:
  `lein test-proflog-fast` with 208 tests / 1115 assertions,
  `lein test-proflog-extended` with 73 tests / 219 assertions, and
  `lein test-proflog-sjas` with `:SUMMARY pass=1078 fail=0 error=0`. See
  [AAR-0109](docs/aar/AAR-0109-dsjas-composite-proof-object-internalization.md).
- During ADR-0109 focused SJAS verification on
  `adr-0109-dsjas-composite-proof-object`, the selector
  `proflog.willard-sjas-test/sjas-subst-prf-checks-selfcons-fixed-point-certificate`
  crossed 15 minutes at 2026-06-14T19:23:04Z inside
  `lein test-proflog-sjas-focused`. The all-vars focused run was later
  interrupted after the selector exceeded its historical 45-minute envelope; the
  branch's SJAS gate evidence is the not-slow `lein test-proflog-sjas` run, with
  slow fixed-point probes remaining in the recorded slow-suite lane.
- Completed [ADR-0108](docs/adr/ADR-0108-dsjas-quantitative-ea-stability.md)
  on `adr-0108-dsjas-ea-stability`: proved quantitative EA-stability for the
  selected `D_SJAS` proof-object measure `Log_D_SJAS`, while preserving the
  ADR-0102 refutation of the proof-code-only statement. The theorem keeps
  Willard's A/E constants (`sigma=1`, `tau=1`, `lambda=1/2`, with `mu=0` for
  A-stability and `mu=-1` for E-stability), but uses ADR-0104's combined
  `(S,F,P)` measure for `sjas-axiom` citation leaves. Red->green: the focused
  correspondence namespace first failed on missing
  `audit-dsjas-quantitative-ea-stability`, then passed with 37 tests / 454
  assertions after adding the executable proof audit. Final broad gates passed:
  `lein test-proflog-fast` with 208 tests / 1103 assertions and
  `lein test-proflog-extended` with 73 tests / 219 assertions. See
  [AAR-0108](docs/aar/AAR-0108-dsjas-quantitative-ea-stability.md).
- Completed [ADR-0107](docs/adr/ADR-0107-pure-indexed-relational-lookup.md)
  (width-reduction #2, pure indexed relational lookup). Added
  `clojure.core.logic.index/int-indexo` to the vendored overlay: a fixed
  integer-keyed table compiled into a perfect bit-trie of ground terms, descended
  **deterministically by unification** on a ground key (no cut, no groundness
  inspection, no double-count) and enumerating on a free key — the "richer
  structure inside core.logic" of ADR-0106 §D. TDD isolation-first contract
  (`proflog.core-logic-indexed-lookup-test`, 8 assertions) red→green, then
  re-expressed `code-constructor-buildo` (the 4096-entry `code-functions` table)
  over it. Answer-set agreement vs a faithful linear baseline in every mode
  (`proflog.code-constructor-index-test`, 19 assertions, incl. the full 4096-entry
  enumeration); no regression in correctness (fast 198/1047/0, SJAS not-slow
  1060/0/0). **NEGATIVE RESULT (measured): int-indexo is ~2× SLOWER than the
  linear `or*` on a ground-key lookup (27.2 vs 12.3 ms/op).** The `fd` constant
  outweighs the O(N)→O(log N) win at N=4096, and a ground key already fails the
  linear scan's wrong branches at the first `==` (no choice points for the trie to
  remove) — the ADR-0106 §C #2 premise (ground lookup "opens a choice point per
  entry") was wrong; the table scan was never the cost. So #2 is correct + pure but
  **not a performance win** as built; an actual win needs a non-`fd` trie or a
  larger table. **REVERTED 2026-06-14** after confirming no speedup on the extended
  gate and the slow suite (8/10 slow pass at 1.6–7 s, no improvement; the two
  heaviest don't complete under *either* version — int-indexo 240 s, linear 600 s —
  so they are inherently near/at the wall, not a #2 artefact). Reverted
  `code-constructor-buildo` to the linear `or*`, deleted the
  `int-indexo` primitive + its two tests + the project.clj entries; retested green.
  #1 (ADR-0110) is independent and retained. Kept ADR-0107/AAR-0107 as the recorded
  negative result so the fd-trie lookup line is not re-attempted.
  See [AAR-0107](docs/aar/AAR-0107-pure-indexed-relational-lookup.md).

## 2026-06-13

- Completed [ADR-0106](docs/adr/ADR-0106-sjas-search-width-reduction.md) on
  `adr-0106-sjas-width-reduction`, successor to ADR-0105. Researched the
  miniKanren parallelism literature: implicit OR-parallelism
  ([concurrentKanren, 2025](https://arxiv.org/html/2510.04994)) is sound (immutable
  substitutions, bounded worker pool) but ≤#cores and modest/variable, with
  constraints unaddressed; interleaving-search scheduling is itself a cost
  ([Rozplokhas & Boulytchev, FLOPS 2022](https://arxiv.org/abs/2202.08511)). So
  parallelism is a constant factor, wrong for the critical path. **Corrected the
  diagnosis:** ground decode is ~1 ms (not ~7.5 s) and the both-ground
  `subst-code-any` check fails fast; a direct jstack of the full grind puts the
  time in proof-facing formula/embedded-code decoding + static-table enumeration
  over **variable-dense intermediate terms** (the original 84-sample finding) —
  correcting ADR-0105's per-op-cost inference (its re-derivation verdict stands).
  Elaborated the width-reduction design space, highest-leverage = mode-directed
  ground-before-decode evaluation (make codes ground at decode time so the ~1 ms
  ground path + O(1) table lookups apply), plus static-table determinisation on
  ground keys (ADR-0078 line), goal ordering / early failure, relevance prefilter,
  and decision-engine offload, **under a binding purity constraint** (no
  project/conda/host cuts; richer structure added inside core.logic).
  Diagnosis experiments were scratch evals over existing relations (no kernel
  change), removed after measuring; methodology documented for reproducibility.
  See [AAR-0106](docs/aar/AAR-0106-sjas-search-width-reduction.md).
- Completed [ADR-0105](docs/adr/ADR-0105-sjas-substate-tabling-investigation.md)
  on `adr-0105-sjas-substate-tabling`, a tractability investigation into tabling
  for the subst-prf negative-exhaustion wall. Surveyed both tabling facilities
  (core.logic `l/tabled`, constraint-store-unaware; `proflog.tabling`, ADR-0017
  canonical-state, kernel-only); the SJAS profile search is untabled. Measured
  re-derivation with a conservative reify-keyed probe: the probed relation
  `decode-syntax-formula-byteso` is **1.00×** (12 calls, 12 distinct); the
  top-level code reader is 4.33× (13/3). **Verdict: tabling is not the systemic
  fix** — the wall is a wide search over distinct intermediate terms; the lever
  is search-width reduction, not memoization. (Per-op cost corrected in ADR-0106:
  the work is variable-dense decode + table enumeration, ground decode ~1 ms —
  not "expensive decodes"; the re-derivation verdict is unaffected.) Decided
  *before* building, per the ADR-0100 lesson. See
  [measurement note](docs/log/2026-06-13-sjas-substate-tabling-measurement.md)
  and [AAR-0105](docs/aar/AAR-0105-sjas-substate-tabling-investigation.md).
- Completed [ADR-0104](docs/adr/ADR-0104-dsjas-track2c.md) on
  `adr-0104-dsjas-track2c` for the full Track 2c `D_SJAS` objective. The branch
  defines selected apparatus `D_SJAS`, repairs `sjas-axiom` proof-object
  accounting with the combined `(S,F,P)` measure, proves the combined size lower
  bound for citation and formula-bearing structural proof objects, proves
  recursive `tableau-proof/3` and `subst-prf/4` well-foundedness by least fixed
  point over finite acyclic proof-call graphs, and proves literature
  admissibility for the explicitly labeled selected variant
  `IS#_{D_SJAS}(beta)`. It does not identify `D_SJAS` with literal Willard `D`.
  Red->green milestones: missing `audit-dsjas-track2c-specification`, missing
  `audit-dsjas-combined-size-lower-bound`, insufficient recursive
  `:measure-specified`, and insufficient literature `:in-progress` all failed
  before implementation. Final focused correspondence tests passed with 35 tests
  / 436 assertions; final broad gates passed with `lein test-proflog-fast` at
  206 tests / 1085 assertions and `lein test-proflog-extended` at 73 tests / 219
  assertions. See
  [D_SJAS Track 2c Kickoff](docs/log/2026-06-13-dsjas-track2c-kickoff.md).
- Completed [ADR-0103](docs/adr/ADR-0103-sjas-proof-attempts-a-b.md)
  on `adr-0103-sjas-proof-attempts-a-b`: completed the corrected Path A/Path B
  work from ADR-0102. Path A is now a proved narrow literal-Willard theorem over
  non-axiom formula-bearing structural proof trees whose checker path uses only
  admitted branches; its six agenda/truth/NNF/quantifier/bounded-guard
  obligations are discharged by executable proof-audit clauses. Path B is now
  completed negatively for literal Track 2b: the current accepted domain cannot
  be literal Willard `D` because it includes non-Willard extended rule families
  and the ADR-0102 fixed-size `sjas-axiom` citation counterexample. Positive
  `D_SJAS` correspondence is therefore a Track 2c theorem after a proof-object
  accounting repair. Red->green: the inventory API was first absent, then the
  proof-status API was absent; after implementation
  `lein test proflog.sjas-correspondence-test` passed with 30 tests / 407
  assertions. Final gates: `lein test-proflog-fast` passed with 201 tests /
  1056 assertions, and `lein test-proflog-extended` passed with 73 tests / 219
  assertions. See [Path A proof](docs/log/2026-06-13-sjas-path-a-proof-attempt.md),
  [Path B verdict](docs/log/2026-06-13-sjas-path-b-proof-attempt.md), and
  [AAR-0103](docs/aar/AAR-0103-sjas-proof-attempts-a-b.md).
- Completed [ADR-0102](docs/adr/ADR-0102-sjas-counterexample-proof-targets.md)
  on `adr-0102-sjas-counterexample-proof-targets`: wrote an executable
  counterexample to ADR-0100 as stated. The focused test constructs a Tableau-0
  system with beta axiom `(= (f^8 1) (f^8 1))`, proves `tableau-proof(s,f,p)`
  with the fixed compact `sjas-axiom` certificate, and verifies the certificate
  has 3 base-64 proof bytes = 18 bits while `J=18`, so the claimed `>=5J`
  requirement is 90 bits. This refutes ADR-0100's proof-size claim over its
  own covered domain. Also pursued the two corrected tracks: Path A narrows to
  a literal-Willard structural fragment excluding SJAS extensions, while Path B
  defines the candidate extended `D_SJAS` apparatus needed for full
  self-reference. See the
  [counterexample](docs/log/2026-06-13-adr0100-axiom-citation-counterexample.md),
  [Path A](docs/log/2026-06-13-sjas-path-a-narrow-willard-fragment.md),
  [Path B](docs/log/2026-06-13-sjas-path-b-extended-dsjas.md), and
  [AAR-0102](docs/aar/AAR-0102-sjas-counterexample-proof-targets.md).
- Completed [ADR-0101](docs/adr/ADR-0101-sjas-correspondence-proof-attempt.md)
  on `adr-0101-sjas-correspondence-proof-audit`: independently attempted to
  prove ADR-0100's Track 2b correspondence claim against the actual structural
  checker and proof-code grammar. The proof does **not** close as written.
  Ordinary non-axiom formula-bearing structural proof trees still have a
  credible Willard-tableau correspondence and a repairable size lower bound,
  but the full ADR-0100 domain includes non-literal-`D` checker branches
  (equality/arithmetic/profile/reflected/proof-predicate closure) and the bare
  fixed-size `sjas-axiom` citation. The latter cannot satisfy `size(P) >= 5J`
  as a property of `P` alone when the cited axiom formula grows via `S` and
  `F`. Updated AAR-0100 with an erratum. See the
  [proof attempt](docs/log/2026-06-13-sjas-correspondence-proof-attempt.md) and
  [AAR-0101](docs/aar/AAR-0101-sjas-correspondence-proof-attempt.md).
- Completed [ADR-0100](docs/adr/ADR-0100-sjas-correspondence-proof.md) on
  `adr-0100-sjas-correspondence-proof`: proved the Track 2b correspondence
  theorem over the first fragment —
  `ProflogAccepts(P,S,F) ⟺ SemPrf_D(decode(P),S,F)` with the ≥5J-bit
  anti-compression bound. Stated Willard's `D` verbatim from willard2001 (8
  deduction rules + branch closure + prenex* root), defined the Proflog side as
  the inductive structural checker, and matched every checker clause to a `D`
  rule (near 1:1, because Track 2a made the first fragment the formula-bearing
  tree and proved all other constructors unreachable — the criteria doc's route
  1). Proof by direct examination, honestly bounded: first fragment only, not
  machine-checked, beta validity a stated trust boundary; unbounded-domain,
  U-Grounding, and mechanization are named follow-ups. Added the
  [proof document](docs/log/2026-06-13-sjas-tableau-correspondence-proof.md)
  (discharging all 9 completion criteria for the fragment), per-rule
  correspondence-witness tests, and an anti-compression regression. Audit/proof/
  tests only. `lein test-proflog-fast` (`196`/`1035`), SJAS not-slow (`1060`
  assertions), focused (`2`/`44`). Track 2b is the parallel agent's track
  (ADR-0096/0097); taken on at the user's direction with an
  [interdev handoff](docs/interdev/2026-06-13-adr-0100-correspondence-proof-handoff.md).
  See [AAR-0100](docs/aar/AAR-0100-sjas-correspondence-proof.md).
- Completed [ADR-0099](docs/adr/ADR-0099-sjas-track2a-completion.md) on
  `adr-0099-sjas-track2a-completion`, closing Track 2a of ADR-0073. Resolved the
  two remaining high-risk relevance-matrix rows via the same unreachability
  route as ADR-0098: reflected procedure-call expansion is a formula-bearing
  clause-body child (no `pos-call`/`neg-call`/`alt`/guarded tags), and
  quantifier instantiation introduces a `par-term`/witness formula-bearing child
  (no `univ`/`once-univ`/`witness` tags) carrying the instantiation explicitly so
  its size is accounted by ADR-0097. Generalized the audit to
  `audit-fragment-reachability` + `fragment-reachability-constructor-sets`
  (equality / procedure-call / quantifier) in `proflog.sjas-correspondence`;
  added a reflected-call probe, a quantifier probe, per-aspect audit unit tests,
  and a capstone asserting no proof symbol remains `:unresolved`. The
  [relevance-matrix completion note](docs/log/2026-06-13-sjas-track2a-relevance-matrix-completion.md)
  records every row's final disposition. Audit only. Red→green;
  `lein test-proflog-fast` (`196`/`1035`), SJAS not-slow (`1016` assertions),
  focused (`4`/`34`). Residual correspondence-theorem + proof-medium obligations
  handed to Track 2b (parallel agent). See
  [AAR-0099](docs/aar/AAR-0099-sjas-track2a-completion.md).
- Completed [ADR-0098](docs/adr/ADR-0098-sjas-equality-fragment-reachability.md)
  on `adr-0098-sjas-equality-relevance`, the first Track 2a relevance-matrix
  slice: resolved the high-risk "equality and disequality profile rules" row via
  the unreachability route. The SJAS structural proof checker closes every
  equality/disequality case formula-bearing (reflexive same-term, rigid-different
  progression, disequality storage + neq-violated recheck, positive-equality
  unification, and the highest-risk equality-triggered positive/negative calls),
  driven by the `(eq …)`/`(neq …)` formulas and branch state, with the decoded
  proof being the tree shape — it never consumes the equality-extension or
  disequality-closure tags, so they are unreachable in accepted first-fragment
  certificates. Added `audit-equality-reachability` +
  `equality-disequality-constructor-symbols` to `proflog.sjas-correspondence`,
  audit unit tests, and an end-to-end probe closing `(neq one one)` through the
  structural checker with a formula-bearing, tag-free, `:formula-bearing-tableau`
  certificate. Audit only. Red→green; `lein test-proflog-fast` (`193`/`1016`),
  SJAS not-slow (`1000` assertions), focused (`3`/`14`). Non-colliding with the
  parallel agent's Track 2b (ADR-0096/0097). See
  [equality fragment reachability](docs/log/2026-06-13-sjas-equality-fragment-reachability.md)
  and [AAR-0098](docs/aar/AAR-0098-sjas-equality-fragment-reachability.md).
- Completed [ADR-0095](docs/adr/ADR-0095-sjas-proof-synthesis.md) on
  `adr-0095-sjas-proof-synthesis`: citation synthesis works — a fresh-variable
  `tableau-proof/3` query through `query-answers` (deferral disabled) binds the
  proof code to the canonical `sjas-axiom` certificate, generating the Henkin
  proof of the system's own SelfCons sentence rather than checking it.
  Concurring with the [interdev review](docs/interdev/2026-06-13-adr-0095-proof-synthesis-review.md),
  three localized repairs: construct via the canonical builder
  `sjas-internal-code-termo` rather than the presented-code reader run backward
  (the reader reads numerals arithmetically and is deliberately not a
  bijection — see [the discussion](docs/log/2026-06-13-arithmetic-numeral-reader-and-bijection.md));
  harden the compact reader's forward direction by tying the `code-N`
  argument count to the declared byte-count; extract
  `sjas-tableau-proof-destructureo` for reuse across the checking and
  synthesizing branches. Red/green: reader-rejection unit test red→green,
  plus a canonical-builder contract test and two `^:slow` end-to-end synthesis
  selectors (beta + SelfCons). Gates: `lein test-proflog-fast` (`175`/`691`),
  SJAS not-slow (`140` vars, `993` assertions), slow lane one-JVM-per-var all
  green; probe synthesis case `certificate-match=true` for both profiles
  (`tableau0` 4.9 s, `level1` 11.9 s). The two `subst-prf` negative selectors
  remain pre-existing envelope-exceeders, unchanged by this ADR; their
  tractability (subgoal tabling / Track 2a relevance prefilter) is a successor
  concern. See [AAR-0095](docs/aar/AAR-0095-sjas-proof-synthesis.md).
- Completed [ADR-0097](docs/adr/ADR-0097-sjas-structural-proof-tree-audit.md)
  on `adr-0097-sjas-structural-proof-tree-audit` as the next Track 2 proof-
  object audit. `proflog.sjas-correspondence` now parses first-fragment
  formula-bearing structural tableau proof terms, validates flat and wide
  formula-byte payloads, rejects malformed symbol-free lists, and reports
  node/leaf/depth/formula-byte metrics needed by Track 2b tree and
  anti-compression obligations. Red: the first selector failed on missing
  `audit-structural-proof-tree`. Green: all four new focused selectors, full
  `proflog.sjas-correspondence-test` (`20` tests, `360` assertions), and
  `lein test-proflog-fast` (`191` tests, `1009` assertions); the extended gate
  also passed (`73` tests, `219` assertions). See
  [SJAS Structural Proof-Tree Audit](docs/log/2026-06-13-sjas-structural-proof-tree-audit.md)
  and [AAR-0097](docs/aar/AAR-0097-sjas-structural-proof-tree-audit.md).
- Completed [ADR-0096](docs/adr/ADR-0096-sjas-correspondence-fragment-audit.md)
  on `adr-0096-sjas-correspondence-fragment-audit` as an independent Track 2
  slice while ADR-0095 remains with the other agent. The correspondence audit
  now distinguishes encoded/classified SJAS proof symbols from admission into
  the first Track 2b correspondence fragment: formula-bearing structural
  tableau proof terms and bare `sjas-axiom` citations are admitted, while
  legacy proof-rule traces, sidecars, and answer-overlay evidence stay outside
  pending a primitive/macro/erasure/unreachability proof. Red:
  `proof-symbol-fragment-boundary-covers-every-encoded-symbol` failed on the
  missing audit var. Green: all four new focused selectors, full
  `proflog.sjas-correspondence-test` (`16` tests, `343` assertions),
  `lein test-proflog-fast` (`187` tests, `992` assertions), and
  `lein test-proflog-extended` (`73` tests, `219` assertions). See
  [SJAS Correspondence Fragment Audit](docs/log/2026-06-13-sjas-correspondence-fragment-audit.md)
  and [AAR-0096](docs/aar/AAR-0096-sjas-correspondence-fragment-audit.md).
- Merged ADR-0093's canonical miniKanren/core.logic regression suite into
  `main` after ADR-0094 had already landed. The conflict resolution preserved
  both fast-gate additions, `proflog.core-logic-lvar-equality-test` and
  `proflog.core-logic-canonical-test`, plus the extended
  `proflog.core-logic-canonical-extended-test`. Combined verification passed:
  `lein test-proflog-fast` (`183` tests, `744` assertions, `2:03.07`) and
  `lein test-proflog-extended` (`73` tests, `219` assertions, `5:19.35`).
  The separate ADR-0095 worktree remains in-progress/red and was not merged.

## 2026-06-10

- Logged verbatim the SelfCons execution discussion — whether the artifact
  has executed self-verification, the concrete 27/60/180-byte fixed-point
  sentences, the exact reconstruction relations and rules, and the
  synthesis-mode elaboration — as
  [SelfCons Execution Discussion](docs/log/2026-06-10-selfcons-execution-discussion.md).
- Reviewed the dual subst-prf probes at 9h07m (pre-0094) and 1h30m
  (post-0094) elapsed, 84 stack samples each: keyword-lookup frames fell
  from 58/84 samples to 0 and `LVar.equals` from 20 to 0 (ADR-0094
  confirmed at scale); the ADR-0090 scanner's deep descent appears in only
  2/84 samples, so its speculative lazy-worklist refinement is not
  justified; the residual profile is walk/occurs over variable-dense terms
  — structural search width, with constant-factor levers exhausted. Both
  probes still running, logs durable.
- Opened [ADR-0095](docs/adr/ADR-0095-sjas-proof-synthesis.md): citation
  proof synthesis via fresh-variable `tableau-proof/3` queries (the
  runtime generating the Henkin proof of its own consistency rather than
  checking it), plus a structural-synthesis behavior probe.
- Started [ADR-0093](docs/adr/ADR-0093-core-logic-canonical-regression-suite.md)
  on `adr-0093-core-logic-canonical-regressions` after reviewing the
  ADR-0090 ground-term fast path. The objective is a canonical core.logic
  regression suite, derived from the miniKanren/core.logic literature, that
  checks core miniKanren semantics, cKanren-style constraints, alphaKanren
  nominal behavior, tabling, CLP(FD), and modest performance canaries before
  future core.logic changes can perturb SJAS proof machinery. Survey note:
  [Core.logic Canonical Regression Suite Survey](docs/log/2026-06-10-core-logic-canonical-regression-suite.md).
  Completed it with [AAR-0093](docs/aar/AAR-0093-core-logic-canonical-regression-suite.md):
  the new fast-gate namespace covers core miniKanren semantics, classic list
  relations, cKanren-style constraints, alphaKanren nominal behavior, tabling,
  CLP(FD), a tagged-ground walk performance canary, and a tiny
  literature-derived relational interpreter/quine example. Evidence: all eight
  vars green individually; default and 1.1.1-overlay namespace runs green
  (`8` tests, `53` assertions); `lein test-proflog-fast` green (`179` tests,
  `732` assertions, `5:33.00`); `lein test-proflog-extended` green (`68`
  tests, `203` assertions, `15:02.76`).
- Extended ADR-0093 after user review clarified that the follow-up suite itself
  needed to be written and run inside the same ADR. Added
  `proflog.core-logic-canonical-extended-test` to `lein test-proflog-extended`
  with quine/twine relational-interpreter pearls, SEND+MORE=MONEY, all 92
  8-queens FD solutions, and backward binary multiplication factorization of
  30. Evidence: the extended namespace passed focused (`4` tests, `10`
  assertions), all four vars passed individually, the 1.1.1 source-overlay run
  passed, `lein test-proflog-fast` passed (`179` tests, `732` assertions,
  `9:46.96` while run concurrently), and `lein test-proflog-extended` passed
  (`72` tests, `213` assertions, `22:46.99`). A direct raw
  `(run 1 [q] (evalo q '() q))` probe against the tiny interpreter exceeded a
  90-second bounded run, so the committed test uses exact generated quine/twine
  shapes rather than a nonterminating raw search.
- Ran the raw `evalo` quine experiment to completion by adapting the paper's
  extended `eval-expo` (`absento closure` plus relational `proper-listo`) rather
  than the tiny fast-suite interpreter. The durable run
  `test-runs/raw-evalo-quine-faithful-20260610T191619Z.log` returned the
  canonical quine with residual `(!= (_0 list))`, `(!= (_0 quote))`,
  `symbolo`, and `(absento closure _0)` in `0:53.53` maxrss `217532KB`. The raw
  query is now promoted into `proflog.core-logic-canonical-extended-test` as
  `raw-evalo-quine-generation-completes`. Post-promotion gates passed:
  `lein test-proflog-fast` (`179` tests, `732` assertions, `8:01.09`),
  `lein test-proflog-extended` (`73` tests, `219` assertions, `3:04.35`),
  `lein with-profile +core-logic-source-overlay test-proflog-fast` (`179`
  tests, `732` assertions, `1:05.68`), and `lein with-profile
  +core-logic-source-overlay test-proflog-extended` (`73` tests, `219`
  assertions, `3:11.65`).
- Stack-analyzed the running `subst-prf` negative-exhaustion durable probe
  at the user's request: three samples localized the cost to
  `occurs-check-worklist` substitution lookups whose `LVar.equals` reads
  fields through keyword-lookup indirection. Logged the question, the
  original-code walkthrough, and the proposed type-hinted fast path in
  [LVar Equality Fast Path: Stack Analysis And Proposal](docs/log/2026-06-10-lvar-equality-fast-path-analysis.md),
  and opened [ADR-0094](docs/adr/ADR-0094-core-logic-lvar-equality-fast-path.md)
  (ADR-0093 is claimed by the parallel core.logic canonical-regression-suite
  agent). The durable probe continues in parallel, reniced to priority 19.
- Completed ADR-0094. Both overlays carry the type-hinted LVar-vs-LVar
  equality fast path with the IVar keyword branch kept verbatim; the
  equality contract is pinned by `proflog.core-logic-lvar-equality-test`
  (green on original and patched code, as the change is equivalence-
  preserving) and red/green takes the performance-evidence form:
  back-to-back baseline/patched passes under identical load show 1.23x to
  2.05x on the bisect probes and three whole-program vars, with proofs and
  assertions identical; fast gate `3:28.73` and extended gate `8:32.50`,
  both 0 failures and faster than their predecessors. See
  [AAR-0094](docs/aar/AAR-0094-core-logic-lvar-equality-fast-path.md).
- Executed ADR-0088 to completion on `adr-0088-sjas-runtime-rebaseline`.
  The bisect probe attributed the whole-program grind to `axiom-member`
  citations (beta queries run in seconds); stack samples placed the cost in
  core.logic `walk-term` rebuild churn and occurs rescans over large ground
  code terms. [ADR-0090](docs/adr/ADR-0090-core-logic-ground-term-walk-fast-path.md)
  added a ground-term fast path to both vendored overlays (tag-on-bind,
  tagged walk*/occurs short-circuits, copy-on-write rebuilds): the probe
  cases fell from 15-minute caps to `21.4 s`/`34.7 s`, the 137-var bulk
  sweep ran in `13:35.26`, the formerly multi-CPU-hour
  `query-generated-axioms` passes in `1:03.39`, and both broad gates got
  faster. The sweep — the first full SJAS namespace run since ADR-0086 —
  surfaced two latent defects, repaired red/green as
  [ADR-0091](docs/adr/ADR-0091-sjas-citation-evidence-restoration.md)
  (e248c8b marker summaries dropped citation evidence; the public
  tableau-proof closure now nests proof-bearing membership evidence) and
  [ADR-0092](docs/adr/ADR-0092-sjas-nnf-pi-star-1-encodability.md)
  (Pi*1-encodability now classified on NNF so antecedent existentials
  prenex as Definition 5.1 permits). The default `test-proflog-sjas` gate
  is partitioned to the not-slow namespace via the focused runner (lein
  cannot scope a selector keyword to one namespace); the two `subst-prf`
  negative-exhaustion probes remain `^:slow` with an uncapped durable run
  establishing their true envelopes. Per-var tables:
  [SJAS_RUNTIME_BASELINE_2026-06-10](docs/SJAS_RUNTIME_BASELINE_2026-06-10.md).
- Resolved the parallel-agent ADR numbering collision found by scanning
  worktrees and remote branches: branch
  `adr-0087-sjas-selfcons-fixedpoint-basis` carried one slice built on the
  audit's ADR-0087 commit, with no competing ADR document. Reviewed its
  red/green and gate evidence, merged it into
  `adr-0073-sjas-correspondence-program`, assigned it
  [ADR-0089](docs/adr/ADR-0089-sjas-group3-presented-code-representation.md)
  with [AAR-0089](docs/aar/AAR-0089-sjas-group3-presented-code-representation.md)
  (0087 and 0088 were already taken), merged the consolidated program
  branch to `main`, and pushed. The source branch is left in place for its
  agent; its name predates the renumbering.
- Logged the audit's research-question assessment and the
  `project_landscape.txt` (Autarkic Systems) commentary as
  [Computational Self-Justification: Assessment Against The Artifact](docs/log/2026-06-10-computational-self-justification-assessment.md).
- Recorded the user's performance doctrine in
  [ADR-0088](docs/adr/ADR-0088-sjas-whole-program-query-runtime.md): very
  long-running tests are acceptable when they evidence correct semantics;
  prefer optimizing at the core.logic layer (ADR-0075 occurs-check
  precedent) over complexifying SJAS/Proflog, while preserving miniKanren's
  clean semantics.
- Attributed the ADR-0087 slow-probe grind via differential runs: both
  `sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile`
  and `sjas-subst-prf-checks-selfcons-fixed-point-certificate` exceed
  40/45-minute timeouts at `1fa3e53`, before ADR-0087, so the opaque SJAS
  namespace gate has not been runtime-green since ADR-0086. The positive
  fixed-point checks pass on the corrected shape; the cost concentrates in
  whole-program `AxiomConj` decomposition and negative exhaustive searches.
  Recorded follow-ups in AAR-0086 and AAR-0087, runtime rows in
  `TEST_RUNTIME_BASELINE.md`, re-ran the 128-assertion profile source audit
  green on the ADR-0087 code, and proposed
  [ADR-0088](docs/adr/ADR-0088-sjas-whole-program-query-runtime.md) for the
  re-baseline and scheduling investigation.

## 2026-06-09

- Continued ADR-0073 Track 1 in a separate worktree/branch while the main
  directory was under audit. Group-3 reconstruction now preserves the public
  code representation selected by the presented `system-code` term: a
  U-Grounding `s` no longer accepts a compact `code-N` variant of its
  fixed-point SelfCons sentence merely because the byte payload is the same.
  The fix threads the object code-reader's representation kind through
  Tableau-0 and Level-1 Group-3 reconstruction, axiom-member, `AxiomConj(s)`,
  and proof-free system-code validation without adding source-registry or host
  byte projectors. Red: the new Tableau-0 malformed-representation selector
  failed before implementation. Green: the new Tableau-0 and Level-1 selectors,
  the walked system-code validator selectors, affected Group-3/U-Grounding
  focused selectors, `lein test-proflog-fast` in `10:12.39`, and `lein
  test-proflog-extended` in `22:34.93`. See
  [SJAS Group-3 Presented Code Representation](docs/log/2026-06-09-sjas-group3-presented-code-representation.md).
- Completed ADR-0087, the Level-1 literature-fidelity correction from the
  directory audit. The Level-1 Group-3 matrix now opens with the
  `pi-star-1-code(x)` restriction required by Willard 2013 sentence (7), in
  both the builder and the profile reconstruction template; `Delta-star-0`
  classification is closed under `not`/`implies` on both the host and
  relational sides; and `system` rejects reflected basis formulas without
  `Pi*1` encodings. Red evidence: 8 failures across the four new selectors.
  Green: the same four selectors (11 assertions), the affected Level-1 and
  tableau0 regressions including
  `sjas-tableau-proof-cites-level1-group-three-from-system-code`, and both
  broad gates. The slow `subst-prf` fixed-point certificate selectors and a
  supplementary Level-1 coverage batch run as durable detached probes whose
  logs, stack-sample interpretation, and expected envelope are recorded in
  [AAR-0087](docs/aar/AAR-0087-sjas-level1-pi-star-1-pair-restriction.md),
  to be updated with final numbers.
- Performed a user-requested motivation-alignment and correctness audit of
  the whole directory against the local Willard corpus and AGENTS.md
  methodology. Findings and dispositions, including the tableau0/level1
  naming clarification, the Track 2a apparatus-extension obligations
  (Willard 2005 closes branches only on complementary sentence pairs), the
  refreshed SelfCons Godel code
  `1895911909320248794237471524907560082878513227` for the `0 = 1` target,
  six restorative slice commits for previously uncommitted completed work,
  termination of the superseded 22-CPU-hour SelfCons probe, and working-tree
  hygiene, are recorded in
  [Motivation Alignment And Correctness Audit](docs/log/2026-06-09-motivation-alignment-and-correctness-audit.md).
- Completed ADR-0086. The ordinary-tableau Group-3 target is now the code of
  `0 = 1`, not primitive `false`; the builder, axiom-member path, and
  `AxiomConj(s)` reconstruction agree on the revised target. The public
  formula-bearing SelfCons selector passed in `8:29.61`, `lein
  test-proflog-fast` passed in `6:07.70`, and `lein test-proflog-extended`
  passed in `14:02.94`. `s` remains a finite descriptor whose `AxiomConj(s)`
  reconstruction includes Group-3 by fixed-point/profile semantics. Follow-up
  clarification: accurate formation of this fixed-point axiom basis is a Track
  1 obligation; Track 2 is for explicitly modified deductive apparatuses or
  variants, not for excusing an incomplete literature proof predicate. See
  [SJAS Tableau-0 Zero-One SelfCons Target](docs/log/2026-06-09-sjas-tableau0-zero-one-selfcons-target.md).
- Completed ADR-0081 as a Track 1 relational proof-dispatch cleanup, not a
  SelfCons optimization pass. The generic kernel and SJAS proof profile no
  longer use committed-choice `conda` dispatch in proof-facing paths; branch
  classification is now expressed through structural `conde` alternatives.
  Focused red/green evidence and the post-change fast/extended gates are
  recorded in
  [SJAS Relational Proof Dispatch](docs/log/2026-06-09-sjas-relational-proof-dispatch.md).
- Completed ADR-0082 as the follow-on Track 1 proof-hook cleanup. Generic
  kernel recursive and theory-profile hooks now have callable default
  relations instead of host optional nil dispatch, and `close-agendao` tries the
  profile hook and ordinary closure as ordinary `conde` alternatives. Focused
  hook, tabling, Robinson-Q, and SJAS selectors passed, followed by the fast and
  extended gates. See
  [Kernel Callable Proof Hooks](docs/log/2026-06-09-kernel-callable-proof-hooks.md).
- Completed ADR-0083 as a Track 1 public compact-code reader repair. Public
  compact-code readers now parse presented byte numerals with `code-byte-termo`
  instead of using byte-first reconstruction; embedded payload reconstruction
  keeps the byte-first builder. Focused selectors passed, followed by
  `lein test-proflog-fast` in `3:41.77` and `lein test-proflog-extended` in
  `8:40.19`. See
  [SJAS Public Compact Byte Reader](docs/log/2026-06-09-sjas-public-compact-byte-reader.md).
- Started a current-source durable Track 1 MVP probe for the public
  `tableau-proof/3` SelfCons certificate:
  `test-runs/selfcons-public-track1-current-20260609T014154Z.log`, wrapper PID
  `34144`. The selector is
  `proflog.willard-sjas-test/sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate`;
  it had emitted the namespace header and was live at launch verification. See
  [ADR-0073 Track 1 Audit](docs/log/2026-06-09-adr0073-track1-audit.md).
- Recorded that public SelfCons Track 1 MVP probe after it exceeded the
  requested 15-minute milestone. At `2026-06-09T01:59:31Z`, wrapper PID `34144`
  was still live at `17:36` elapsed with only the namespace header emitted and
  no exit file; it was not killed. See
  [ADR-0073 Track 1 Audit](docs/log/2026-06-09-adr0073-track1-audit.md).
- Added a requirement-by-requirement ADR-0073 Track 1 completion audit. At that
  point, current source and focused evidence supported the arithmeticized
  object-relation implementation slices, but the audit deliberately kept Track
  1 incomplete until the live public SelfCons `tableau-proof/3(s,t,p)` probe
  produced endpoint evidence. See
  [ADR-0073 Track 1 Completion Audit](docs/log/2026-06-09-adr0073-track1-completion-audit.md).
- Completed ADR-0084 as a Track 1 relationality repair. Structural
  `tableau-proof/3` and `subst-prf/4` branches now read `system-code` through
  branch equality state before reconstructing `AxiomConj` or validating the
  system record, and nested structural proof checking receives the walked
  system-code term. Focused red/green evidence is recorded in
  [SJAS Walked System-Code Reconstruction](docs/log/2026-06-09-sjas-walked-system-code-reconstruction.md).
  The post-change `lein test-proflog-fast` gate passed in `3:38.29`, and
  `lein test-proflog-extended` passed in `8:46.17`.
- Monitored the current public SelfCons Track 1 MVP probe without killing it.
  At `2026-06-09T06:28:49Z`, wrapper PID `34144` was still live at `4:45:03`
  elapsed, with only the namespace header emitted and no exit file. The older
  post-ADR-0079 core SelfCons probe
  `test-runs/selfcons-core-post-adr79-20260609T002713Z.log` exited `1` after
  `2:08:37` with `Java heap space` `OutOfMemoryError`, max RSS `4447844KB`.
  A follow-up audit found no additional proof-code reader correctness gap: the
  remaining empty-state public-code byte readers are top-level entries or are
  reached after walked-code wrappers. See
  [ADR-0073 Track 1 Audit](docs/log/2026-06-09-adr0073-track1-audit.md).
- Saved a live JVM diagnostic for the same public SelfCons Track 1 probe in
  `test-runs/selfcons-public-track1-current-20260609T063029Z-diagnostic.log`.
  At `2026-06-09T06:30:47Z`, the test JVM PID `34205` was active at `94.6%`
  CPU with RSS `351216KB`; the classpath used
  `vendor/core.logic-1.0.1/src` before the core.logic jar, heap use was
  `182163K` of `243712K`, and the main stack was in repeated core.logic
  `walk*` traversal. This records that the live public probe is using the
  revised core.logic path and is currently search/traversal bound rather than
  heap-exhausted.
- Accepted [ADR-0085](docs/adr/ADR-0085-sjas-structural-quantifier-sibling-scheduling.md)
  to complete ADR-0073 Track 1 by repairing structural proof-checker
  quantifier scheduling. Red evidence: the core, in-memory, and decoded
  SelfCons selectors each exceeded a `timeout 180s` focused run. The concrete
  core proof is small (`77` target bytes, `471` proof-code bytes), so the
  remaining issue is branch scheduling rather than input size.
- Completed ADR-0085 and ADR-0073 Track 1. The structural checker now preserves
  delayed agenda sibling environments, avoids duplicate proof-node formula
  matching before decoded rule validation, and reconstructs Tableau-0 Group-3
  proof antecedents with the walked public `system-code` term still available.
  The current-source public
  `sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate` selector
  passed with 8 assertions in `2:17.71`. The focused core, in-memory, and
  decoded SelfCons selectors also passed (`1:08.67`, `2:23.34`, `2:20.39`),
  followed by the SJAS source audit, `lein test-proflog-fast` in `1:56.85`,
  `lein test-proflog-extended` in `4:34.15`, and clean `git diff --check`.
  See [AAR-0085](docs/aar/AAR-0085-sjas-structural-quantifier-sibling-scheduling.md)
  and
  [ADR-0073 Track 1 Completion Audit](docs/log/2026-06-09-adr0073-track1-completion-audit.md).

## 2026-06-08

- Closed two host-stack blockers for ADR-0073 Track 1 large U-Grounding proof
  terms. ADR-0075 vendors core.logic 1.0.1 by default and makes the occurs check
  worklist-based; ADR-0076 makes `proflog.language/validate-term`
  worklist-based. Focused red/green failures, the successful public
  U-Grounding proof selector, host audit, AARs, and final fast/extended gate
  evidence are recorded in
  [Stack-Safe Large Proof Terms](docs/log/2026-06-08-stack-safe-large-proof-terms.md).
- Recorded the long SelfCons core proof-check milestone requested during the
  stack-safety investigation. The durable `setsid` run
  `test-runs/selfcons-core-no-timeout-20260608T213315Z.log` was still alive at
  `2026-06-08T21:49:15Z` with wrapper PID `224039` at `15:59` elapsed; it was
  not killed. A JVM stack sample showed the remaining hot path in
  `core.logic/occurs-check-worklist` via `membero`, motivating ADR-0078's
  finite-table scheduling cleanup.
- Completed ADR-0077 and ADR-0078 scheduling cleanups for formula-bearing SJAS
  proof checking. ADR-0077 removes duplicate/subsumed structural alternatives;
  ADR-0078 replaces fixed `membero` table scans with explicit finite
  alternatives. Focused red/green selectors, semantic proof-check selectors,
  and the loaded fast/extended gate results are recorded in
  [Stack-Safe Large Proof Terms](docs/log/2026-06-08-stack-safe-large-proof-terms.md).
- Completed ADR-0079 after the latest-source SelfCons stack sample moved the
  hot path to embedded payload length decoding. Embedded code and natural
  payload decoders now reject mismatched low/high length headers before
  allocating `payload` state. Clean post-change gates passed:
  `lein test-proflog-fast` in `1:34.24` and `lein test-proflog-extended` in
  `3:48.10`.
- Recorded the post-ADR-0079 SelfCons core proof-check probe once it exceeded
  the requested 15-minute milestone. The durable run
  `test-runs/selfcons-core-post-adr79-20260609T002713Z.log` was still alive at
  `2026-06-09T00:42:45Z` with wrapper PID `12980` at `15:20` elapsed; it was
  not killed and had not yet produced pass/fail or `/usr/bin/time` output.
- Completed ADR-0080, the final optimization opened during the stack-safety
  thread. Application-term decoders now destructure the encoded arity byte once
  before dispatching over finite argument counts. Focused selectors and the
  post-change fast/extended gates are recorded in
  [Stack-Safe Large Proof Terms](docs/log/2026-06-08-stack-safe-large-proof-terms.md);
  follow-on work returns to ADR-0073 Track 1 arithmeticization before any more
  proof-predicate optimization.
- Arithmeticized the public formula-bearing proof path for `tableau-proof/3`.
  Substantive structural proof certificates now decode from U-Grounding
  numerals through the SJAS object-code relation, while compact code handling
  and fixed axiom membership remain relational and source-registry-free.
  Focused red/green evidence, kernel scheduling notes, and broad gate results
  are recorded in
  [SJAS U-Grounding Formula-Bearing Proof Path](docs/log/2026-06-08-sjas-u-grounding-proof-path.md).
- Advanced ADR-0073 Track 1 from a core SelfCons tableau closure to a public
  formula-bearing `tableau-proof/3` certificate for the Tableau-0 Group-3
  self-consistency statement. The proof checker now accepts formula-bearing
  branch nodes through exact, binder-renamed, compound, and alpha-equivalent
  formula matching, and the SelfCons fixture mirrors the proof-antecedent
  shape reconstructed from public system code. Focused red/green evidence,
  public selector timings, source-audit repair, and the residual focused-SJAS
  beta axiom-member timeout are recorded in
  [SJAS SelfCons Formula-Bearing Proof](docs/log/2026-06-08-sjas-selfcons-formula-bearing-proof.md).

## 2026-06-06

- Added the ADR-0073 Track 1 self-consistency code output path. The public
  SJAS builder now reports and prints the concrete ordinary-tableau Group-3
  self-consistency Godel code by decoding the generated formal formula-code
  term to its byte string, not by consulting proof-predicate shortcuts. Focused
  red/green evidence and the printed decimal value are recorded in
  [SJAS SelfCons Godel Code Output](docs/log/2026-06-06-sjas-selfcons-godel-code-output.md).
- Repaired the SJAS source audit after the embedded-code reconstruction helper
  was renamed to the counted byte-first builder. The audit now explicitly
  requires `code-args-build-counto` and rejects the public code-reader relation
  in that path; the focused source-audit selector passed.

## 2026-06-05

- Clarified ADR-0073 Track 1's MVP: the public arithmeticized proof predicate
  must accept the concrete `IS#_D(beta)` system code, the code of the Group-3
  consistency statement, and a formula-bearing semantic-tableau proof code for
  that statement, without `sjas-axiom` citation standing in for the full
  tableau evidence. Added structural proof-code scaffolding for that SelfCons
  certificate; an initial foreground focused selector exceeded the short-run
  envelope and was stopped so future runs can use durable `test-runs/` logging.
- Added the missing positive-false arithmetic closure direction to the SJAS
  structural proof checker. Formula-bearing tableau leaves now close positive
  `leq`, `lt`, and `mult` atoms when the interpreted arithmetic relation is
  false, using proof-free structural closure inside proof codes and preserving
  proof-producing wrappers only on the public answer path. Focused red/green
  selectors passed under niceness.
- Repaired fixed axiom-basis reconstruction for the SJAS proof predicate.
  `AxiomConj(s)` now reconstructs all fixed Group-0 and Group-1 antecedents
  through the proof-predicate relation instead of hard-coding only the two
  Group-0 formulas, and the generated theorem antecedent no longer filters out
  Group-1. Focused red/green selectors passed; broader gates were run under
  niceness to reduce machine pressure.

## 2026-06-04

- Removed host runtime fuel consumption from local formula-bearing SJAS tableau
  proof-tree validation. Fixed proof-code checking now descends through the
  supplied finite proof tree without charging `support/step-fuelo`, so valid
  structural certificates are not rejected merely because the external Proflog
  evaluator fuel is zero. Focused regressions and the fast/extended gates
  passed.

- Added proof-free finite-system-code validation to the `subst-prf/4`
  substitution-result axiom branch. Even when `Subst(g,t)` supplies the
  temporary added axiom, the proof predicate now requires the supplied
  `system-code` to parse as a complete SJAS system record before accepting the
  branch. Focused `subst-prf` regressions and the fast/extended gates passed.

- Split recursive `tableau-proof/3` and `subst-prf/4` SJAS predicate checking
  into proof-free core relations plus thin public answer wrappers. Structural
  formula-bearing tableau leaves now call the core relations directly, so the
  SJAS-side tableau proof predicate does not materialize an extra Proflog
  answer-proof marker around recursive proof-predicate leaves. Focused
  regressions and the fast/extended gates passed.

- Added structural leaf closures for `axiom-member/2`, `tableau-proof/3`, and
  `subst-prf/4` inside the SJAS formula-bearing tableau checker. These leaves
  now route through decoded system-code membership, the arithmeticized proof
  predicate, and the arithmeticized substitution proof relation rather than
  falling through to generic arithmetic closure or the Proflog kernel. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Added negated atomic and equality dual rules to the SJAS structural proof
  checker. Formula-bearing proof nodes now validate surface negation over
  positive/negative atoms and equality/disequality by local tableau dualization,
  without host normalization or proof-rule tags. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Added negated quantifier dual rules to the SJAS structural proof checker.
  Formula-bearing proof nodes now validate negated universal, existential,
  once-universal, and bounded quantifier expansions through decoded branch
  environments and guard formulas. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Added implication and negated-implication local tableau rules to the SJAS
  structural proof checker. Formula-bearing proof nodes now validate implication
  branching and negated-implication same-branch expansion from decoded formula
  structure, without host normalization or proof-rule tags. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Added bounded-quantifier expansion to the SJAS structural proof checker.
  Formula-bearing proof nodes now expand bounded existential and universal
  formulas through their decoded `leq` guard formulas, with binder-aware
  substitution support for bounded forms. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Added ordinary semantic-tableau negation cases to the SJAS structural proof
  checker. Formula-bearing proof nodes now validate double-negation removal,
  negated-conjunction branching, and negated-disjunction same-branch expansion
  directly from decoded formula structure, without proof-rule trace tags. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Split compact-code byte handling in the SJAS proof predicate. Presented
  public code terms now parse their numeral bits before finite byte lookup,
  while embedded decoded-code payload reconstruction uses a separate byte-first
  builder path. This preserves the object-level arithmetic reader for public
  proof-predicate inputs without reintroducing generated formula registries or
  host byte decoders. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Removed committed-choice search control from the remaining SJAS proof
  machinery relations. Syntax skipping, beta axiom scans, guarded conjunction
  flattening, and guarded existential scope stripping now use ordinary
  structural relations with explicit nonmatching cases. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Removed remaining proof-evidence and materialization overhead from the
  reflected-call portion of the SJAS proof predicate path. Application arity
  parsing no longer uses committed-choice recursion, compact byte decoding no
  longer builds unused canonical numeral proof evidence, beta-block scans skip
  structurally instead of decoding discarded formulas, and reflected procedure
  call resolvers read system-code bytes through proof-free object relations.
  See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

- Removed auxiliary proof-trace evidence from the SJAS `tableau-proof/3` and
  `subst-prf/4` proof-predicate path. The predicates now use proof-free core
  relations for public code reading, proof-code decoding, theorem-code
  negation, finite-system axiom membership, axiom-conjunction reconstruction,
  and substitution antecedent recovery, while the returned Proflog proof is only
  the proof-predicate closure marker. This keeps the supplied formula-bearing
  tableau proof code as the relevant SJAS evidence rather than adjoining a
  second proof trace. See
  [SJAS Proof Predicate Core Evidence Removal](docs/log/2026-06-04-sjas-proof-predicate-core-evidence-removal.md).

## 2026-06-03

- Narrowed the SJAS proof-predicate non-axiom proof-code decoder to
  formula-bearing structural tableau trees. Legacy symbolic proof-rule traces
  such as `(false-close)` no longer decode as substantive non-axiom SJAS proof
  certificates; only the dedicated `sjas-axiom` citation remains a bare symbol
  path. See
  [SJAS Structural-Only Non-Axiom Proof-Code Decoder](docs/log/2026-06-03-sjas-structural-only-proof-code-decoder.md).

- Removed the legacy SJAS proof-trace checker from the Track 1 proof-predicate
  path. `sjas-proof-check-stateo` now preserves tableau agenda selection while
  delegating only to formula-bearing structural proof checking; generated
  kernel trace reinjection tests were removed or rewritten to structural
  certificates. See
  [SJAS Structural-Only Proof Predicate](docs/log/2026-06-03-sjas-structural-only-proof-predicate.md).

- Added wide formula-bearing SJAS proof nodes. Proof-code lists now support a
  wide count tag, and structural nodes can carry formula bytes as a proof
  byte-list payload, removing the one-byte formula-length ceiling from the
  initial formula-bearing proof-node fragment. See
  [SJAS Wide Formula-Bearing Proof Nodes](docs/log/2026-06-03-sjas-wide-formula-bearing-proof-nodes.md).

- Recorded the remaining public legacy certificate boundary for SJAS proof
  internalization. The structural checker is decoded and isolated from proof
  tags, but `tableau-proof/3`/`subst-prf/4` still accept legacy non-axiom
  certificate shapes until a positive public structural theorem proof supports
  narrowing that surface. See
  [SJAS Public Legacy Certificate Boundary](docs/log/2026-06-03-sjas-public-legacy-certificate-boundary.md).

- Added a source audit proving the formula-bearing SJAS structural checker does
  not match legacy proof-rule tags. The broader compatibility checker still
  accepts old certificate shapes, but `sjas-structural-proof-check-stateo` now
  has a regression preventing `conj`, `witness`, `eq-step`, procedure-call, and
  guarded-call proof tags from re-entering the structural path. See
  [SJAS Structural Checker Proof-Rule Tag Audit](docs/log/2026-06-03-sjas-structural-no-proof-rule-tags.md).

- Added focused public-boundary coverage for formula-bearing SJAS proof-code
  decoding. Compact structural proof certificates now have a regression showing
  they decode through the proof-code relation and are consumed by the SJAS proof
  checker without symbolic proof-rule tags. See
  [SJAS Structural Proof-Code Decoding](docs/log/2026-06-03-sjas-structural-proof-code-decoding.md).

- Fixed formula-bearing SJAS structural binder naming for nested branch
  quantifiers. Structural `forall`, `once-forall`, and `exists` payloads now use
  branch-environment depth rather than proof-variable depth, so nested
  existential parameters receive distinct canonical names such as `par v0` and
  `par v1`. See
  [SJAS Structural Branch Binder Names](docs/log/2026-06-03-sjas-structural-branch-binder-names.md).

- Added formula-bearing SJAS coverage for guarded-shaped reflected negative
  bodies without guarded proof constructors. The structural path now explicitly
  closes decoded negated reflected bodies with ordinary disjunction and
  quantifier rules instead of `neg-call-guarded-alt`, guarded sequence, or
  `guarded-scope-exists` tags for these fragments. See
  [SJAS Structural Guarded Reflected Bodies](docs/log/2026-06-03-sjas-structural-guarded-reflected-bodies.md).

- Extended formula-bearing SJAS equality nodes to reflected calls triggered by
  unification. Saved positive and negative calls now open reflected bodies from
  encoded `system-code` using proof-free structural equality progression, with
  the reflected body represented as a child formula-bearing node rather than an
  `eq-triggered-call` or `eq-triggered-neg-call` proof constructor. See
  [SJAS Structural Equality-Triggered Reflected Calls](docs/log/2026-06-03-sjas-structural-equality-triggered-reflected-calls.md).

- Added focused coverage showing formula-bearing SJAS negative reflected-call
  alternatives do not require `neg-call-alt` or `alt` proof constructors. The
  structural reflected-call relation ranges over encoded matching clauses, and
  the child formula code selects the negated reflected body. See
  [SJAS Structural Negative Reflected Alternatives](docs/log/2026-06-03-sjas-structural-negative-reflected-alternatives.md).

- Extended formula-bearing SJAS negative reflected calls to structural
  procedure expansion. Single-clause negative calls now recover reflected
  clause bodies from encoded `system-code` and check the child formula-bearing
  subtree without a `neg-call` proof constructor. See
  [SJAS Structural Negative Reflected Calls](docs/log/2026-06-03-sjas-structural-negative-reflected-call.md).

- Extended formula-bearing SJAS positive reflected calls to structural
  procedure expansion. Positive calls now recover reflected clause bodies from
  encoded `system-code` and check the child formula-bearing subtree without a
  `pos-call` proof constructor. See
  [SJAS Structural Positive Reflected Calls](docs/log/2026-06-03-sjas-structural-positive-reflected-call.md).

- Extended formula-bearing SJAS equality leaves to saved-literal closure after
  equality. Equality now closes structurally when proof-free unification makes
  saved positive and negative literals complementary, without `savefml`,
  `eq-step`, or `close` proof constructors. See
  [SJAS Structural Equality-Triggered Literal Closure](docs/log/2026-06-03-sjas-structural-equality-triggered-literal-closure.md).

- Extended formula-bearing SJAS equality leaves to stored-disequality closure.
  Equality now closes structurally when proof-free unification makes a stored
  disequality false, without `eq-step` or `neq-close` proof constructors. See
  [SJAS Structural Stored Disequality Closure](docs/log/2026-06-03-sjas-structural-stored-disequality-closure.md).

- Extended formula-bearing SJAS disequality nodes to unresolved disequality
  storage. Structural `exists` expansion can now use a canonical parameter
  payload, and a formula-bearing disequality node can store unresolved
  parameter constraints and continue without a `neq-store` proof-rule tag. See
  [SJAS Structural Disequality Storage](docs/log/2026-06-03-sjas-structural-disequality-storage.md).

- Extended formula-bearing SJAS quantifier nodes to canonical child payloads
  that mention introduced proof variables. Structural node comparison now uses
  the branch-visible formula after applying the quantifier environment, and
  `forall`/`once-forall` expansion chooses canonical `v0`, `v1`, ... noms so
  child proof nodes have stable formula-code bytes. See
  [SJAS Canonical Quantifier Child Nodes](docs/log/2026-06-03-sjas-canonical-quantifier-children.md).

## 2026-06-02

- Switched formula-bearing SJAS structural arithmetic closure to proof-free
  arithmetic readers and relation checks. Structural arithmetic leaves now
  close through dedicated proof-free cores rather than returning local
  arithmetic read/relation proof payloads. See
  [SJAS Proof-Free Structural Arithmetic Closure](docs/log/2026-06-02-sjas-proof-free-structural-arithmetic.md).

- Switched formula-bearing SJAS complementary literal closure to proof-free
  atom unification. The structural checker now closes complementary saved
  literals through `sjas-atom-unify-coreo` rather than the kernel's
  proof-producing `equality/atom-unifyo`. See
  [SJAS Proof-Free Complementary Literal Closure](docs/log/2026-06-02-sjas-proof-free-complementary-literals.md).

- Switched formula-bearing SJAS equality progression to proof-free unification.
  The structural checker now computes equality branch substitutions with
  `sjas-unify-termo-coreo` rather than the kernel's proof-producing
  `equality/unify-termo`, closing another local proof-trace boundary in Track
  1. See
  [SJAS Proof-Free Equality Progression](docs/log/2026-06-02-sjas-proof-free-equality-progression.md).

- Extended formula-bearing SJAS tableau proof leaves to equality contradiction
  closure. The checker now closes impossible equality leaves such as `0 = 1`
  by structural branch-state analysis, without requiring `free-close`,
  `occurs-close`, or `decompose` proof-trace constructors in the encoded proof
  term. See
  [SJAS Structural Equality Contradiction Closure](docs/log/2026-06-02-sjas-structural-equality-contradiction.md).

- Extended formula-bearing SJAS tableau proof nodes to rigid disequality
  progression. The checker now continues structurally when a disequality's
  terms are rigidly different under the branch state, without a `neq-rigid`
  proof-rule tag. See
  [SJAS Structural Rigid Disequality Progression](docs/log/2026-06-02-sjas-structural-rigid-disequality.md).

- Extended formula-bearing SJAS tableau proof nodes to equality progression.
  Equality nodes now update branch substitution and continue structurally,
  without an `eq-step` proof-rule tag in the proof certificate for the covered
  fragment. See
  [SJAS Structural Equality Progression](docs/log/2026-06-02-sjas-structural-equality-progression.md).

- Extended formula-bearing SJAS tableau proof leaves to arithmetic/profile
  closure. The checker can now close structural arithmetic leaves by evaluating
  the SJAS arithmeticized closure cores internally, without requiring either
  `(arith-close)` or a profiled arithmetic proof trace in the proof certificate.
  See
  [SJAS Structural Arithmetic Closure](docs/log/2026-06-02-sjas-structural-arithmetic-closure.md).

- Extended formula-bearing SJAS tableau proof leaves to reflexive disequality
  closure. The checker now closes structural disequality leaves when both sides
  are equal in the branch state, without requiring a `refl-close` proof-rule
  tag. See
  [SJAS Structural Reflexive Disequality Closure](docs/log/2026-06-02-sjas-structural-reflexive-disequality.md).

- Extended formula-bearing SJAS tableau proof nodes to structural quantifier
  expansion for `exists`, `forall`, and `once-forall`. The checker now infers
  quantifier expansion from decoded formula nodes and branch state without
  `witness`, `univ`, or `once-univ` proof-rule tags for the covered fragment.
  See
  [SJAS Structural Quantifier Tableau Nodes](docs/log/2026-06-02-sjas-structural-quantifier-tableau.md).

- Extended formula-bearing SJAS tableau proof nodes to literal continuation
  and complementary literal closure. Literal nodes can now save branch context
  and close against saved complementary literals without `savefml` or `close`
  proof-rule tags; formula-bearing node decoding now converts decoded SJAS
  formula-code syntax into AST form before branch-state comparison. See
  [SJAS Structural Literal Closure](docs/log/2026-06-02-sjas-structural-literal-closure.md).

- Extended formula-bearing SJAS tableau proof nodes to structural disjunction.
  The checker now infers the `or` branching rule from the decoded formula node
  and validates two child branches without a Proflog `split` proof-rule tag,
  while preserving sibling-local equality and disequality state. See
  [SJAS Structural Disjunction Tableau Nodes](docs/log/2026-06-02-sjas-structural-disjunction-tableau.md).

- Added the first formula-bearing SJAS tableau proof-node fragment. Proof nodes
  can now carry encoded formula bytes and children, while the checker infers
  `and`, `true`, and `false` tableau behavior from formula/tree structure
  rather than from Proflog proof-rule tags. This records the design objection
  that trace evidence is an additional Godel-encoded structure whose
  arithmeticized manipulation should be avoided unless correctness requires it.
  See
  [SJAS Formula-Bearing Tableau Nodes](docs/log/2026-06-02-sjas-formula-bearing-tableau-nodes.md).

- Added a minimal SJAS `(arith-close)` tableau leaf certificate so arithmetic
  branch closure need not be encoded as a full Proflog-style arithmetic proof
  trace. The checker now evaluates the arithmetic closure relation internally
  for this leaf, while the correspondence audit classifies the new proof symbol.
  See
  [SJAS Minimal Arithmetic Close Certificate](docs/log/2026-06-02-sjas-minimal-arithmetic-close.md).

- Replaced the proof-facing committed-choice `sjas-axiom-membero` dispatcher
  with ordinary relational disjunction across the finite encoded-system axiom
  classes. The source audit now rejects `conda` in that dispatcher. Additional
  focused axiom/proof-predicate selectors are still running as proof-search
  evidence. See
  [SJAS Axiom-Member Relational Dispatch](docs/log/2026-06-02-sjas-axiom-member-relational-dispatch.md).

- Replaced committed-choice reflected negative-call alternative collection with
  explicit encoded-clause match/nonmatch relations. Reflected alternatives are
  now included when relation index and arity byte match the focused call, and
  skipped only when a finite nonmatch relation proves the header differs. See
  [SJAS Reflected Alternative Explicit Nonmatch](docs/log/2026-06-02-sjas-reflected-alternative-explicit-nonmatch.md).

- Removed the source-only `subst-prf/4` substitution witness shortcut from the
  SJAS proof predicate path. `subst-prf` now computes the diagonal substituted
  source sentence as an explicit proof antecedent and includes that sentence in
  the non-axiom proof-check target. Current-source long `subst-prf` selectors
  are running durably under `test-runs/` with timestamp `20260602T230615Z`. See
  [SJAS SubstPrf Explicit Source Result](docs/log/2026-06-02-sjas-substprf-explicit-source-result.md).

- Fixed a Track 1 semantic-tableau proof-predicate bug in the SJAS `split`
  rule. The local checker no longer threads equality substitutions or
  disequality stores from the left disjunct into the right disjunct; sibling
  branches now close from the same incoming branch state, so one branch cannot
  close by borrowing a unification produced only in its sibling. Added a
  focused red/green regression for the invalid proof and updated the checker
  documentation to state the complete NNF tableau fragment it implements. See
  [SJAS Split Branch Independence](docs/log/2026-06-02-sjas-split-branch-independence.md).

- Tightened equality walking for bound rigid parameters while investigating
  the long SJAS self-consistency proof run. `walko` now leaves `(par p)` rigid
  only when `p` is unbound in the equality substitution, matching proof-variable
  walking and removing an unsound extra search alternative where a bound
  parameter could ignore its binding. See
  [Equality Bound Parameter Walk](docs/log/2026-06-02-equality-bound-parameter-walk.md).

- Removed a redundant empty-substitution equality walk from the SJAS compact
  byte reader while the long self-consistency validation continued running.
  Compact code bytes still decode through the object-level U-Grounding numeral
  relation and finite byte relation, but the parser no longer calls the full
  equality walker where no substitution state can affect the result; canonical
  byte generation also rejects mismatched public roots before recursive numeral
  construction. See
  [SJAS Compact Byte Empty Walk Removal](docs/log/2026-06-02-sjas-compact-byte-empty-walk-removal.md).

- Recorded that multi-hour SJAS proof probes must use durable `test-runs/`
  logs, saved PIDs, and detachable runners (`nohup`/`tmux`) so pass/fail output
  survives session boundaries. The same requirement is now in
  [AGENTS.md](AGENTS.md).

- Replaced the committed-choice `tableau-proof/3` split between `sjas-axiom`
  and substantive proof certificates with a proof-code byte discriminator shared
  by `tableau-proof/3` and `subst-prf/4`. The source audit now rejects `conda`
  in `sjas-tableau-proof-closeo`. See
  [SJAS Proof-Code Relational Split](docs/log/2026-06-02-sjas-proof-code-relational-split.md).

## 2026-06-01

- Removed proof-facing marker evidence, direct host-ground SJAS profile
  entrypoints, generic sidecar hiding, and compact-code reader host scheduling
  from the Track 1 proof predicate path. `tableau-proof/3` and `subst-prf/4`
  now carry full object-level code-reader evidence, public proof search routes
  through the ordinary kernel with the SJAS theory rule bound, and compact code
  bytes use the finite constructor/byte relations in all modes. See
  [SJAS Proof Predicate Shortcut Excision](docs/log/2026-06-01-sjas-proof-predicate-shortcut-excision.md).

- Removed the remaining `project`/`lvar?` compact-code reader bridge from the
  SJAS proof profile. Compact byte terms now decode through the object-level
  U-grounding numeral reader and finite byte relation; generated embedded code
  bytes still use canonical numeral generation with a noncanonical fallback.
  Focused tests show the expected tractability cost in deep system-code proof
  checks, but fast and extended gates passed. See
  [SJAS Compact Code Projector Removal](docs/log/2026-06-01-sjas-compact-code-projector-removal.md).

- Reframed large public system/formula code evidence as uniform code-reader
  marker evidence. The old `sjas-public-code-bytes-summaryo` name is gone;
  system-code and large formula-code proof paths still check
  `sjas-formal-code-byteso`, but public evidence now makes explicit that it is
  recording the checked code-reader kind rather than a separate semantic
  summary relation. See
  [SJAS Public Code Marker Evidence](docs/log/2026-06-01-sjas-public-code-marker-evidence.md).

- Removed the size-dependent theorem-code decoder inside SJAS proof
  predicates. `tableau-proof/3` and `subst-prf/4` theorem-code reads now always
  consume public codes through `sjas-formal-code-byteso` and return a uniform
  code-reader marker (`sjas-code-bytes` or `sjas-ug-code-bytes`) rather than
  switching at a host byte-count threshold. See
  [SJAS Theorem-Code Uniform Reader Marker](docs/log/2026-06-01-sjas-theorem-code-uniform-reader-marker.md).

- Removed the proof-facing source-signature codebook bridge from the SJAS
  profile. The decoder now maps fixed SJAS vocabulary indexes to semantic
  symbols and user indexes to structural `(sym n)` ids, so reflected
  procedure-call proof checking matches calls by encoded symbol index rather
  than reconstructed host relation names. See
  [SJAS Proof Decoder Structural Symbols](docs/log/2026-06-01-sjas-proof-decoder-structural-symbols.md).

- Removed the large `tableau-proof/3` public proof-report shortcut from the
  SJAS profile. Large direct proof-predicate queries now have to reify the
  proof evidence produced by `direct-negated-profile-closeo` itself instead of
  first checking truth and then returning a synthetic compact report. This
  favors Track 1 correctness and full proof-evidence internalization over the
  earlier runtime escape hatch. See
  [SJAS Large Proof Report Shortcut Removal](docs/log/2026-06-01-sjas-large-proof-report-shortcut-removal.md).

- Refocused ADR-0073 Track 1 on the paper-grade semantic target rather than
  further small checker fixes. Added a normative in-principle arithmeticization
  specification for `TabPrf_beta(system-code,theorem-code,proof-code)` as a
  bounded object-language relation over decoded finite systems, theorem codes,
  proof trees, tableau local rules, branch closure, substitution, and reflected
  clause expansion. The LOPSTR/PPDP system-description paper now summarizes
  this specification as the semantic basis for `IS#_D(beta)` internalization.
  See [SJAS Tableau Arithmeticization Specification](docs/log/2026-06-01-sjas-tableau-arithmeticization-spec.md).

- Closed the stale unresolved bucket for implemented SJAS profile/code proof
  evidence. The executable correspondence audit now classifies `profiled`,
  Willard profile markers, arithmetic/code/axiom/theorem/proof-check markers,
  and substitution proof markers as relevant SJAS proof evidence, while
  obsolete generated-host markers such as `willard-sjas-fact` and
  `sjas-generated-axiom-member` are explicitly excluded. See
  [SJAS Profile Marker Audit Closure](docs/log/2026-06-01-sjas-profile-marker-audit-closure.md).
  The proof-symbol status probe now reports no unresolved encoded proof symbols;
  `lein test-proflog-fast` passed 653 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Resolved the remaining raw large proof-evidence materialization timeout for
  direct SJAS `tableau-proof/3` checks. The public proof path already completed,
  but forcing the private `direct-negated-profile-closeo` proof stream still
  timed out because a logic-valued empty sigma made the theorem-code decoder
  select detailed per-byte evidence instead of the compact large-code marker.
  The ground direct branch now requires empty proof-code sigma before decoding
  large theorem codes with compact read evidence, preserving object-level byte
  decoding and proof checking while making raw evidence reifiable. See
  [SJAS Large Proof Raw Evidence Materialization](docs/log/2026-06-01-sjas-large-proof-raw-evidence-materialization.md).
  The new regression passed in 1:11.81 after timing out red at 180s; `lein
  test-proflog-fast`, `lein test-proflog-extended`, and the focused SJAS runner
  all passed.

- Aligned the executable correspondence audit with implemented Track 1
  proof-checker constructors. Equality, equality-triggered saved calls,
  reflected procedure calls, guarded alternatives, guarded scope, guarded call
  sequences, residual sequences, and equality guard saturation constructors
  that the SJAS checker now consumes object-level are classified as relevant
  proof-checker structure rather than stale unresolved gaps. See
  [SJAS Implemented Constructor Classification](docs/log/2026-06-01-sjas-implemented-constructor-classification.md).
  `lein test-proflog-fast` passed 638 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Closed the answer-overlay proof-constructor boundary for SJAS theorem proof
  predicates by explicit exclusion. `query-pos-call`, `query-neg-call`,
  `query-neg-call-guarded-alt`, and `guarded-call-seq-defer` remain encodable
  proof evidence for answer export, but the correspondence audit marks them
  excluded from SJAS proof-predicate certificates and `tableau-proof/3` rejects
  query-entry certificates rather than treating them as theorem proofs. See
  [SJAS Answer Overlay Exclusion](docs/log/2026-06-01-sjas-answer-overlay-exclusion.md).
  `lein test-proflog-fast` passed 610 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Closed the generic optimized sidecar boundary for SJAS proof predicates by
  explicit exclusion. `sjas/proof-certificate` now erases only outer
  `willard-sjas-tableau0` and `willard-sjas-level1` annotations, preserving
  generic `(profiled propositional ...)` and `(profiled first-order ...)`
  wrappers in proof-code trees so `tableau-proof/3` rejects them rather than
  silently accepting the wrapped subproof. The correspondence audit now marks
  `lem-close`, `skolemized`, `propositional`, and `first-order` as excluded
  from SJAS proof-predicate certificates. See
  [SJAS Generic Sidecar Exclusion](docs/log/2026-06-01-sjas-generic-sidecar-exclusion.md).
  `lein test-proflog-fast` passed 604 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Replaced the large `tableau-proof/3` public report's host proof-code inverse
  with the SJAS proof-code decoding relation. Large proof reports still run
  only after the SJAS checker accepts the proof-predicate query, but the decoded
  certificate tree returned in compact public evidence is now produced through
  `decode-proof-code-kindo` and `code-read-marker-o`, not
  `proof-formal-code-term->proof`. See
  [SJAS Large Proof Report Decoder Internalization](docs/log/2026-06-01-sjas-large-proof-report-decoder-internalization.md).
  `lein test-proflog-fast` passed 598 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Internalized recursive guarded-call sequence checking for saturated reflected
  negative-call alternatives. Guarded alternatives reconstructed from
  `system-code` now preserve decoded guard, recursive-call, residual, and
  fallback partitions; the proof checker consumes `guarded-call-seq-step` by
  resolving the nested negated call from encoded reflected Group-2b records
  rather than compiled guarded-clause tables. `guarded-call-seq-defer`,
  answer-overlay query constructors, non-equality guard saturation, and generic
  optimized layer/profile wrappers remain Track 1 gaps. See
  [SJAS Recursive Guarded Call Sequence Internalization](docs/log/2026-06-01-sjas-recursive-guarded-call-sequence-internalization.md).
  `lein test-proflog-fast` passed 598 assertions and `lein
  test-proflog-extended` passed 203 assertions.

## 2026-05-31

- Reassessed the large SJAS proof-materialization timeout after the guarded
  proof-checking slices. The current public `tableau-proof/3` path no longer
  reproduces the timeout: the substantive self-consistency selector completed in
  1:10.65 with 6 assertions, and the adjacent structural theorem-code selector
  completed in 0:41.11 with 4 assertions. The remaining timeout diagnosis is the
  same boundary as before: raw miniKanren proof-state reification is much larger
  than SJAS proof-predicate acceptance and public checked certificate evidence.
  See
  [SJAS Proof Materialization Current Assessment](docs/log/2026-05-31-sjas-proof-materialization-current-assessment.md).

- Internalized leading existential guarded scope for reflected guarded
  negative-call proof checking. Guarded alternatives reconstructed from
  `system-code` now preserve decoded scope, and the SJAS checker consumes
  `guarded-scope-exists` evidence to extend the branch environment before
  checking fallback or saturated guarded paths. Other leading quantifier forms,
  recursive guarded call sequences, non-equality guards, and answer-overlay
  guarded/query constructors remain Track 1 gaps. See
  [SJAS Existential Guarded Scope Internalization](docs/log/2026-05-31-sjas-existential-guarded-scope-internalization.md).
  `lein test-proflog-fast` passed 598 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Internalized non-empty equality guard saturation for saturated guarded
  negative-call proof checking. Reflected guarded alternatives reconstructed
  from `system-code` now preserve guard and residual partitions, and the SJAS
  checker consumes explicit `guard-eq` evidence before closing the residual
  sequence, without consulting compiled guarded-clause tables. Non-empty
  guarded call sequences, existential guarded scope, non-equality guards, and
  answer-overlay guarded/query constructors remain Track 1 gaps. See
  [SJAS Guard Equality Saturation Internalization](docs/log/2026-05-31-sjas-guard-equality-saturation-internalization.md).
  `lein test-proflog-fast` passed 598 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Internalized the no-scope/no-guard/no-recursive-call saturated guarded
  negative-call proof path. The SJAS checker now accepts
  `guarded-neg-alt-saturated` certificates whose guard saturation and guarded
  call sequence are empty and whose residual sequence closes the decoded
  reflected body reconstructed from `system-code`. Recursive guarded
  call-sequence, non-empty guard saturation, existential guarded scope, and
  answer-overlay guarded variants remain Track 1 gaps. See
  [SJAS Saturated Guarded Negative Call Internalization](docs/log/2026-05-31-sjas-saturated-guarded-negative-call-internalization.md).
  `lein test-proflog-fast` passed 598 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Internalized the fallback guarded negative-call proof path for reflected
  multi-clause procedure calls. The SJAS proof-code alphabet now includes the
  guarded terminal markers needed by kernel evidence, and the SJAS proof
  checker validates encoded `neg-call-guarded-alt` certificates by
  reconstructing guarded alternatives from reflected Group-2b records in
  `system-code`, with compiled clause tables stripped in the public regression.
  The saturated guard-first and answer-overlay guarded variants remain future
  Track 1 slices. See
  [SJAS Guarded Negative Call Internalization](docs/log/2026-05-31-sjas-guarded-negative-call-internalization.md).
  `lein test-proflog-fast` passed 598 assertions and `lein
  test-proflog-extended` passed 203 assertions.

## 2026-05-30

- Reassessed the large SJAS proof-materialization timeout. The public
  self-consistency selector now completes under a 1200s envelope, while a raw
  direct-relation probe still times out when forcing the miniKanren proof
  stream, confirming the issue is evidence reification rather than SJAS
  proof-predicate acceptance. Added a focused red/green regression proving
  large `tableau-proof/3` queries with `proof-limit 0` do not materialize the
  reporting-side proof decoder, and moved report construction behind the
  proof-limit and acceptance gate. See
  [SJAS Proof Materialization Timeout Assessment](docs/log/2026-05-30-sjas-proof-materialization-timeout-assessment.md).
  `lein test-proflog-fast` passed 596 assertions and `lein
  test-proflog-extended` passed 203 assertions.

- Internalized `neg-call-alt` proof checking for multi-clause reflected
  negative calls. The SJAS proof-code alphabet now includes the inner `alt`
  constructor, the proof checker reconstructs matching negated alternatives
  from encoded reflected Group-2b records in `system-code`, and public
  `tableau-proof/3` validates an encoded `neg-call-alt` certificate with
  compiled clause tables stripped. `lein test-proflog-fast` passed 596
  assertions and `lein test-proflog-extended` passed 203 assertions. See
  [SJAS Reflected Negative Call Alternatives](docs/log/2026-05-30-sjas-reflected-negative-call-alternatives.md).

- Internalized the ordinary semantic-tableau truth/falsehood proof
  constructors in the SJAS proof checker. `false-close` now closes an explicit
  falsehood branch, `skip-true` advances past a truth formula, public
  `tableau-proof/3` accepts encoded `false-close` certificates without
  reaching `kernel/prove-programo`, and the correspondence audit now classifies
  `skip-true` as relevant tableau structure. `lein test-proflog-fast` passed
  595 assertions and `lein test-proflog-extended` passed 203 assertions. See
  [SJAS Truth and Falsehood Proof Constructors](docs/log/2026-05-30-sjas-truth-falsehood-proof-constructors.md).

- Resolved the large Group-3 `tableau-proof/3` public proof-materialization
  timeout. The root cause was not certificate generation or semantic proof
  acceptance: focused probes showed the SJAS relation accepting the certificate
  in truth mode, while `core.logic` did not finish reifying the public proof
  term inside a 900s envelope. The profile now gates the large direct
  `tableau-proof/3` report with the same SJAS proof-check relation in truth
  mode, then builds the returned report from the checked proof-code bytes so
  the public evidence still contains the decoded certificate constructors. The
  self-consistency selector now passes with proof-shape assertions for
  `witness` and `once-univ`; the source audit remains green. `lein
  test-proflog-fast` passed 594 assertions and `lein test-proflog-extended`
  passed 203 assertions. See [SJAS Large Tableau Proof Evidence](docs/log/2026-05-30-sjas-large-tableau-proof-evidence.md).

- Removed the large non-axiom `tableau-proof/3` proof-output summary shortcut.
  Large theorem-code proofs now report the decoded proof tree instead of a
  synthetic `(profiled willard-sjas-proof-check)` marker. The source audit
  passed 32 assertions and the structural non-generated theorem-code selector
  passed with full proof evidence; this exposed the later Group-3 public proof
  materialization boundary recorded above. `lein test-proflog-fast` passed 594
  assertions and `lein test-proflog-extended` passed 203 assertions. See
  [SJAS Large Tableau Proof Evidence](docs/log/2026-05-30-sjas-large-tableau-proof-evidence.md).

## 2026-05-29

- Removed the compact public-code host argument deconstructor and addressed
  the resulting large semantic boundary. Compact `code-N` terms now bind their
  constructor arity without enumerating the full signature, byte terms still
  pass through the object byte relation, embedded `code(...)` payloads can
  regenerate canonical byte numerals, reflected host-AST call checks use a
  narrow signature-isomorphism bridge, and, at that stage, large non-axiom
  Group-3 tableau-proof reports used compact proof summaries after the SJAS
  close relation succeeded. The negative `false` axiom-citation case now fails through
  the SJAS axiom branch itself, with structural non-reifying beta-record skips,
  rather than falling through to non-axiom proof-tree checking. The 1500s SJAS
  focused-suite envelope advanced to the final alphabetic block with no
  reported failures; the exact remaining tail then passed 110 assertions in one
  JVM. `lein test-proflog-fast` passed 594 assertions and `lein
  test-proflog-extended` passed 203 assertions.
  See
  [SJAS Compact Code Without Host Argument Projection](docs/log/2026-05-29-sjas-compact-code-no-host-args.md).
- Replaced the compact public-code byte-term host lookup table with an
  arithmetic U-Grounding numeral decoder. `code-1(add(dbl(0),1))` now decodes
  as the one-byte formula code for `true`, while the source audit rejects the
  old 64-entry generated byte-term table. See
  [SJAS Compact Byte Arithmetic Decoder](docs/log/2026-05-29-sjas-compact-byte-arithmetic-decoder.md).
- Removed the generated finite symbol table from compiled SJAS program
  registries and from proof-profile formula-code decoding. Semantic SJAS
  primitives are now recovered only through reserved numeric ids; user symbols
  are compared structurally as numeric `(sym n)` ids in the proof predicate.
  See
  [SJAS Symbol-Table Registry Removal](docs/log/2026-05-29-sjas-symbol-table-registry-removal.md).
- Removed source symbol-registry dependence from `subst-code/2` over
  user-symbol formulas. Substitution now decodes source and target formula
  codes through the syntax/numeric-symbol decoder, so `demo(v0)` structurally
  substitutes to `demo(code(demo(v0)))` with `:sjas/registry` removed while the
  unsubstituted open formula is still rejected. See
  [SJAS Subst-Code Symbol-ID Decoder](docs/log/2026-05-29-sjas-subst-code-symbol-id-decoder.md).
- Reserved fixed numeric symbol ids for the SJAS semantic vocabulary and taught
  proof-facing formula decoding to recover those symbols without the generated
  source registry. `tableau-proof/3` and `subst-prf/4` now validate the
  non-generated arithmetic theorem `lt(1, 2)` with `:sjas/registry` removed,
  so branch closure for fixed U-Grounding/profile relations no longer depends
  on host-side symbol-table lookup. User symbols remain conventional codebook
  entries justified up to signature isomorphism. The attempted removal of the
  compact ground code reader was recorded as a tractability boundary after
  focused selectors exceeded their timeouts. See
  [SJAS Reserved Semantic Symbols](docs/log/2026-05-29-sjas-reserved-semantic-symbols.md).
- Removed source symbol-table lookup from reflected procedure-call proof
  reconstruction. `tableau-proof/3` now validates the demo reflected
  `neg-call` certificate with runtime clause tables and `:sjas/registry`
  removed, by decoding theorem and reflected-clause application heads as
  structural numeric `(sym n)` ids and comparing those ids against the
  reflected records in `system-code`. The user-facing caveat remains that
  system-code byte reading is only fully satisfactory when backed by
  `sjas-formal-code-byteso` rather than a host byte projection; this slice
  removes the symbol-codebook lookup from reflected calls, not every remaining
  proof-predicate shortcut. See
  [SJAS Reflected Call Symbol-ID Recovery](docs/log/2026-05-29-sjas-reflected-call-symbol-id-recovery.md).
- Removed source symbol-table lookup from reflected Group-2b axiom membership.
  Reflected clause axiom citation now reconstructs and compares formula trees
  using structural numeric `(sym n)` relation heads, while reflected
  procedure-call recovery remains on the proof-facing symbol path. See
  [SJAS Reflected Axiom Symbol-ID Membership](docs/log/2026-05-29-sjas-reflected-axiom-symbol-id-membership.md).
- Removed source symbol-table lookup from Group-2 beta axiom membership.
  `axiom-member(system, formula)` now scans beta formula byte boundaries with
  the syntax-only decoder, so application-bearing beta formulas such as
  `lt(1, 2)` can be cited with `:sjas/registry` removed. Reflected clauses and
  proof-facing AST conversion still remain as symbol-code boundaries. See
  [SJAS Beta Byte Membership Without Symbol Registry](docs/log/2026-05-29-sjas-beta-byte-membership-without-symbol-registry.md).
- Added a syntax-only formula-code decoder that keeps application heads as
  structural numeric `(sym n)` terms instead of resolving symbol indexes
  through `:sjas/symbol-index-entries`. `wff` now succeeds for an
  application-bearing formula code with `:sjas/registry` removed, while the
  remaining proof-facing symbol table boundary is left explicit. See
  [SJAS Syntax Symbol-ID Decoder](docs/log/2026-05-29-sjas-syntax-symbol-id-decoder.md).
- Removed the remaining `:sjas/code-format` source-registry read from SJAS
  syntax-code predicate closure. The proof profile now lets the object-level
  code reader infer compact versus U-Grounding representation from the supplied
  code term. This narrows the remaining source-preprocessing boundary to the
  finite symbol-index table. See
  [SJAS Code-Format Registry Removal](docs/log/2026-05-29-sjas-code-format-registry-removal.md).
- Removed the active source-registry authorization guard from SJAS proof
  predicates as an ADR-0073 Track 1 system-code reconstruction slice.
  `tableau-proof/3` now validates a fixed Group-0 axiom certificate with
  `:sjas/registry` absent, `subst-prf/4` validates the corresponding identity
  certificate the same way, and the focused regression audits that
  `sjas-active-systemo` is not reintroduced. `lein test-proflog-fast` and
  `lein test-proflog-extended` passed; `lein test-proflog-sjas-focused` was
  stopped for exact-selector investigation after the composite reflected/beta
  example exceeded the focused-run envelope. The broader finite symbol-table
  boundary remains a Track 1 internalization target. See
  [SJAS Active Registry Proof-Predicate Removal](docs/log/2026-05-29-sjas-active-registry-proof-predicate-removal.md).

## 2026-05-28

- Repaired the `test-proflog-fitting-programs` gate after the LOPSTR+PPDP
  artifact run exposed two stale assumptions. `query-status` now accepts an
  optional structural `:max-fuel` bound so unresolved catalog rows can stop
  before a known expensive next slice, and the finite-domain
  `fd-unknown-total-unresolved` row uses that bound at fuel `1`. The
  `append-inverse-flat` list matrix row now uses raw answer limit `10`, the
  first current core.logic answer limit that exposes all four closed split
  targets. Parent verification: `lein test-proflog-fitting-programs` passed 6
  tests / 81 assertions in `72.23 s`, `lein test-proflog-fast` passed 159
  tests / 594 assertions in `98.39 s`, and `lein test-proflog-extended` passed
  68 tests / 203 assertions in `247.66 s`.
- Logged the current ADR-0073 proof-machinery internalization process as eight
  logical slices: code format, syntax, system-code reconstruction, proof-code
  grammar, U-Grounding arithmetic, substitution/fixed-point machinery, tableau
  proof checking, and reflected procedure-call recovery. The goal is not merely
	  to construct Godel codes for dependencies of `tableau-proof/3`; each
	  dependency must become an object-language relation over those codes. The
	  then-open idea of moving a missing dependency into correspondence work was
	  later narrowed by the ADR-0086 clarification: for the literature predicate,
	  a missing dependency is Track 1 incomplete; Track 2 is for modified
	  deductive apparatuses or variants. Updated ADR-0073 to make these
	  subelements explicit. See
	  [SJAS Proof-Machinery Internalization Slices](docs/log/2026-05-28-sjas-proof-machinery-internalization-slices.md).
- Added SJAS proof-code support for occurs-check equality closure.
  `occurs-close` is now an encodable and decoded proof symbol, classified as
  relevant closure evidence, and public `tableau-proof/3` validates the
  certificate for `exists x. x != f(x)` without calling the host proof
  validator. See
  [SJAS Occurs-Check Proof Code](docs/log/2026-05-28-sjas-occurs-check-proof-code.md).
- Added SJAS-local proof checking for equality-triggered negative reflected
  calls. `(eq-step step-proof (eq-triggered-neg-call subproof))` now wakes saved
  negative atoms after equality makes them object-language ground, decodes the
  matching reflected clause from `system-code`, and checks the NNF negation of
  the reflected body without using `kernel/prove-programo` or runtime clause
  lookup. See
  [SJAS Equality-Triggered Negative Call Proof Check](docs/log/2026-05-28-sjas-equality-triggered-negative-call-proof-check.md).
- Added SJAS-local proof checking for equality-triggered positive reflected
  calls. `(eq-step step-proof (eq-triggered-call subproof))` now wakes saved
  positive atoms after equality makes them object-language ground, resolves the
  procedure body from reflected clauses decoded from `system-code`, and checks
  the subproof without consulting the runtime clause table or
  `kernel/prove-programo`. See
  [SJAS Equality-Triggered Positive Call Proof Check](docs/log/2026-05-28-sjas-equality-triggered-positive-call-proof-check.md).
- Added SJAS-local proof checking for equality-triggered saved atom closure.
  `(eq-step step-proof (atom-close arg-proof))` now closes saved complementary
  atoms through the local proof checker, and `atom-close`/`eq-refl` are now
  encodable proof-code symbols. The focused checker regression saves
  `color(x)` and `not color(0)`, then closes them after `x = 0`, with
  `kernel/prove-programo` disabled. See
  [SJAS Equality-Triggered Atom Closure Proof Check](docs/log/2026-05-28-sjas-equality-triggered-atom-closure-proof-check.md).
- Added SJAS-local proof checking for stored disequality closure. `(neq-store
  subproof)` now records a delayed branch disequality, and the saved-disequality
  form of `(eq-step step branch-proof)` closes when relational equality
  progress collapses that stored obligation to `(neq-close)`. The public
  `tableau-proof/3` regression validates the encoded certificate for
  `forall x. x = 0 or x != 0` without calling the host proof validator. See
  [SJAS Stored Disequality Proof Check](docs/log/2026-05-28-sjas-stored-disequality-proof-check.md).
- Added SJAS-local proof checking for proof-variable disequality closure.
  `(neq-close step-proof)` now closes selected negative equalities when
  relational unification binds only gamma-introduced proof variables, and
  `eq-bind` is now part of the proof-code alphabet. The public regression uses
  an inert `f/1` symbol to validate `exists x. f(x) = f(0)` without routing the
  branch through U-grounding arithmetic. See
  [SJAS Proof-Variable Disequality Proof Check](docs/log/2026-05-28-sjas-proof-variable-disequality-proof-check.md).

## 2026-05-27

- Advanced ADR-0073 Track 1 proof-predicate internalization by teaching the
  SJAS-local proof checker to consume decoded free-constructor equality closure
  evidence. `tableau-proof/3` now validates an encoded `(conj (free-close))`
  certificate for the SJAS theorem `neq(0,1)` without delegating to
  `kernel/prove-programo`, and the executable correspondence audit now treats
  `free-close` as relevant closure evidence while leaving richer equality
  constructors unresolved. See
  [SJAS Free Equality Proof-Check Closure](docs/log/2026-05-27-sjas-free-equality-proof-check.md).
- Extended the SJAS proof-code grammar for nested free-constructor equality
  closure evidence by appending `decompose` and `args` to the stable proof
  alphabet. `tableau-proof/3` now validates a nested encoded equality
  certificate over preserved `code-2` constructor terms, and the audit
  classifies these tags as relevant closure/tree evidence. See
  [SJAS Nested Equality Proof-Code Coverage](docs/log/2026-05-27-sjas-nested-equality-proof-code.md).
- Advanced the SJAS-local proof checker through positive equality progress.
  `par-bind` is now encodable in proof certificates, and `(eq-step step
  subproof)` can update the branch equality substitution through
  `equality/unify-termo` and continue inside the SJAS checker. The public
  `tableau-proof/3` regression validates the theorem
  `forall x. x != 0 or x != 1` with an encoded witness/equality-step
  certificate without calling the host proof validator. See
  [SJAS Positive Equality-Step Proof Check](docs/log/2026-05-27-sjas-positive-equality-step-proof-check.md).
- Added SJAS-local proof checking for reflexive disequality closure.
  `(refl-close)` now closes selected `neq` formulas whose terms are already
  identical under the branch equality substitution, and `tableau-proof/3`
  validates the encoded certificate for `eq(code-1(0), code-1(0))` without
  delegating to the host kernel. See
  [SJAS Reflexive Disequality Proof Check](docs/log/2026-05-27-sjas-reflexive-disequality-proof-check.md).
- Added SJAS-local proof checking for rigid disequality progress.
  `(neq-rigid subproof)` now discharges constructor disequalities that are
  already true in the free term algebra and continues with the remaining branch
  work. The public proof-predicate path also gained an object-level
  top-conjunction focus check so certificates that close the negated theorem do
  not have to explore the reconstructed axiom basis first. See
  [SJAS Rigid Disequality Proof Check](docs/log/2026-05-27-sjas-rigid-disequality-proof-check.md).
- Fixed the nominal lookup/hash guard bug in the legacy αleanTAP and
  greenfield nominal environment lookup relations. Core.logic's current
  `nom/hash` behavior passed the delayed-aliasing regression under the default
  1.0.1 dependency, the 1.1.1 profile, and the vendored source overlay; the
  local bug was missing use of that freshness guard when recursing past nominal
  environment keys. See [ADR-0074](docs/adr/ADR-0074-nominal-lookup-hash-guard.md),
  [AAR-0074](docs/aar/AAR-0074-nominal-lookup-hash-guard.md), and
  [Nominal Lookup Hash Guard](docs/log/2026-05-27-nominal-lookup-hash-guard.md).
- Added IULS 2026 Proflog talk materials under `iuls2026/`: the reviewed
  intent note, the sample ODP style source, a generated ODP slide deck, a
  Markdown slide source, lecture notes, and a small generator that preserves
  the sample deck's visual frame while replacing the content with a talk on
  Fitting tableaus, Procedure Call, the current Clojure/core.logic
  implementation, P1/P2 demonstrations, SJAS motivation, and future work.
  Revised the demo section into worked examples with paper-equivalent P1/P2
  Proflog snippets, concrete `evaluate-case` outputs, and proof-step traces
  captured from the current fitting-program catalog.
- Removed the remaining deterministic public-code byte projectors from the SJAS
  profile. Compact and U-Grounding public code reads now pass through
  `sjas-formal-code-byteso`; long system/Group-3 proof evidence is summarized
  only after that relation succeeds. Fixed U-Grounding sentinel handling so
  byte `63` is accepted inside payloads, and replaced the last U-Grounding
  substitution-side byte shortcut with relation-backed decoding plus fused
  substitution/alpha comparison. The minimal Level-1 U-Grounding fixed-point
  check now succeeds without the old host projector; the larger reflected demo
  path remains a core.logic runtime tractability boundary. See
  [SJAS Public Code Byte Internalization](docs/log/2026-05-27-sjas-public-code-byte-internalization.md).
- Logged the Track 2 clarification that Proflog may be studied both as a
  candidate implementation bridge for Willard's semantic-tableau `D` and,
  speculatively, as a formally specified deductive apparatus `D_Proflog` for
  `IS#_{D_Proflog}(beta)`. Added Track 2c to the ADR-0073 goal prompt:
  formalize the Proflog kernel sufficiently to determine whether Willard-style
  SJAS results can be adapted to it. Recorded why theorem-level equivalence is
  too weak and which proof-object, tree, closure, encoding, size, substitution,
  quantifier, equality, procedure-call, and axiom-basis invariants must be
  conserved or explicitly proved irrelevant. See
  [Proflog as a Candidate SJAS Deductive Apparatus](docs/log/2026-05-27-proflog-as-sjas-deductive-apparatus.md).
- Closed a proof-certificate alphabet gap for reachable SJAS proof evidence.
  Proof codes now have an explicit byte-payload tag, so evidence such as
  `(sjas-code-arg 1 sjas-code-args-end)` can be encoded and decoded without
  escaping the certificate grammar. Added `sjas-code-arg`,
  `sjas-code-args-end`, `sjas-ug-code-canonical-byte`, and `free-close` to the
  declared proof-symbol alphabet. The correspondence audit classifies the
  compact and U-Grounding code-reader tags as relevant inspectable code
  evidence and classifies `free-close` as encodable but still unresolved
  pending an equality/free-constructor closure proof, macro expansion,
  reachability exclusion, or fragment exclusion. See
  [SJAS Proof-Code Byte Payloads](docs/log/2026-05-27-sjas-proof-code-byte-payloads.md).
- Closed the corresponding syntax-predicate proof-evidence alphabet gap.
  `willard-sjas-code`, `wff`, the formula-class predicate tags, `neg-pair`, and
  `sjas-neg-pair-structural` are now encoded and classified as relevant
  code/syntax evidence. The existing syntax predicate regression now also
  audits representative `wff`, `delta-star-0-code`, and `neg-pair` proofs for
  unencodable/unclassified symbols and verifies proof-certificate encoding for
  successful syntax evidence. See
  [SJAS Syntax Proof Evidence Alphabet](docs/log/2026-05-27-sjas-syntax-proof-evidence-alphabet.md).

## 2026-05-26

- Adopted focused, progress-visible testing as the default practice for SJAS
  and other resource-heavy semantic suites. Added `proflog.focused-test-runner`,
  `lein test-vars <namespace>`, and `lein test-proflog-sjas-focused`, and
  documented the workflow in `AGENTS.md`, `README.md`, and
  [Focused Testing Practice for Resource-Heavy Suites](docs/log/2026-05-26-focused-testing-practice.md).
  The full opaque `lein test-proflog-sjas` namespace gate remains available,
  but the default active-development path is exact selectors followed by
  var-by-var timing so slow tests are visible and debuggable.
- Removed the staged compact theorem-code reader from SJAS proof-predicate
  validation. `tableau-proof/3` and `subst-prf/4` now expose compact
  theorem-code constructor-byte reads as `sjas-code-arg` evidence, and compact
  substitution source/target codes use object byte decoding instead of the old
  staged helper. The direct U-Grounding Level-1 substitution attempt failed at
  fuels 240, 500, 1000, and 2000, and then overflowed core.logic's occurs check
  on the large substituted theorem numeral, so an isolated U-Grounding
  substitution-side byte projection remains documented as a tractability
  boundary. Recorded the related finite symbol-table boundary: current codes
  still rely on source-preprocessing symbol indexes unless a later proof shows
  nominal identity is irrelevant up to fixed injective coding. Added
  [SJAS Symbol-Table Isomorphism Justification](docs/log/2026-05-26-sjas-symbol-table-isomorphism-justification.md)
  and updated
  [SJAS Proof-Predicate Arithmeticized Checker](docs/log/2026-05-26-sjas-proof-predicate-arithmeticized-checker.md).
- Removed the remaining reflected compiled-program side table from SJAS
  proof-predicate validation. Reflected `pos-call`/`neg-call` proof steps now
  decode the active `system-code` reflected-clause bytes, bind canonical
  reflected parameters to the focused call arguments, and expose body/negated
  body formulas to the local proof checker. Added a regression that strips the
  compiled clause lists and `:sjas/reflected-program` registry entry before
  validating a reflected `neg-call` certificate, plus source/registry audits
  rejecting the stale bridge. Updated
  [SJAS Proof-Predicate Arithmeticized Checker](docs/log/2026-05-26-sjas-proof-predicate-arithmeticized-checker.md).
- Replaced the non-`sjas-axiom` SJAS proof-predicate shortcut through
  `kernel/prove-programo` with a local proof-directed checker over decoded
  proof constructors. `tableau-proof/3` and `subst-prf/4` now validate the
  currently generated arithmetic, reflected `neg-call`/`pos-call`, quantifier,
  literal-saving, and literal-closure certificate shapes without delegating
  decoded proof trees back to the host proof kernel. Added no-kernel
  regressions with `kernel/prove-programo` redefined to throw, and a source
  audit rejecting the old `kernel/prove-programo target` route. See
  [SJAS Proof-Predicate Arithmeticized Checker](docs/log/2026-05-26-sjas-proof-predicate-arithmeticized-checker.md).
- Completed executable regressions for the SJAS external-clause separation and
  self-consistency negating witness. `tableau-proof/3` and `subst-prf/4` now
  validate non-`sjas-axiom` decoded certificates against a reflected-only
  compiled program, so runtime-only `external` clauses remain queryable by the
  host but cannot count as SJAS proof-predicate evidence. Added a Tableau-0
  self-consistency witness showing that the generated SJAS system rejects
  `tableau-proof(S,false-code,P0)` while ordinary Proflog accepts the same atom
  if `tableau-proof/3` is supplied as an external runtime procedure. Added a
  Level-1 complement-certificate rejection probe for
  `subst-prf(S,skeleton-code,code(not Group3),sjas-axiom)`. See
  [SJAS Self-Consistency Negating Witness Regressions](docs/log/2026-05-26-sjas-selfcons-negating-witness-regressions.md).
- Recorded a concrete SJAS/Proflog separation witness. The formula
  `external-demo(0)` is definable in an SJAS language that declares
  `external-demo/1`, but the corresponding `system-code` contains no beta axiom
  or reflected Group-2b clause for it. Proper SJAS deduction should therefore
  leave the branch open. Direct Proflog closes it with an external runtime
  clause `external-demo(x) :- x = 0`, and the current `tableau-proof/3`
  shortcut accepts the theorem certificate because `kernel/prove-programo` sees
  that full runtime program. The stronger SJAS-base formula is
  `tableau-proof(S0, code(external-demo(0)), P0)`, where `P0` is the proof code
  for `(conj (neg-call ...))`; it should fail under proper SJAS proof checking
  but currently closes through the shortcut. See
  [SJAS/Proflog Separation Witness](docs/log/2026-05-26-sjas-proflog-separation-witness.md).
- Attempted an informal proof, grounded in Willard's SJAS/analytic-tableaux
  papers and Fitting's Proflog/tableaux-for-logic-programming paper, of whether
  the current Proflog kernel can serve as an SJAS proof-predicate shortcut. The
  proof attempt gives a restricted positive result for ordinary tableau-shaped
  kernel derivations, and a full-domain negative result for the current SJAS
  shortcut: `kernel/prove-programo` is operationally defensible and probably
  sound over a bounded fragment, but it does not yet preserve all requisite
  SJAS invariants because skeletal certificates lack a proved formula-bearing
  expansion and proof-size lower bound, and because procedure-call, generic
  equality, guarded/profile, unencodable-tag, and quantifier-witness obligations
  remain open. See
  [Proflog Kernel as an SJAS Proof-Predicate Shortcut: Proof Attempt](docs/log/2026-05-26-proflog-kernel-sjas-shortcut-proof-attempt.md).
- Logged explicit completion criteria for ADR-0073 Track 2b. A correspondence
  proof is complete only after fixing the covered domain, formalizing compatible
  Proflog and SJAS tableau semantics, defining the proof-object translation,
  proving soundness and completeness, preserving every Track 2a relevant
  invariant, proving proof-size lower-bound/anti-compression, supplying
  irrelevance lemmas, and backing the result with operational tests. The same
  note records that full Track 2b is not currently complete: unencodable
  reachable proof tags, unresolved equality/procedure/profile constructors,
  skeletal quantifier certificates, the remaining `kernel/prove-programo`
  bridge, and the absence of a common formal semantics block a truthful
  completion claim. See
  [SJAS Correspondence Proof Criteria](docs/log/2026-05-26-sjas-correspondence-proof-criteria.md).
- Continued ADR-0073 Track 2a in single-threaded mode after parking the Track 1
  and Track 2b subagent worktrees in `MEMORY.md`. The new proof-size note
  refines the relevance classification for Willard's Conventional Tableaux
  Encoding Requirement: proof-size growth and byte-string inspectability are
  relevant, exact curly-brace byte layout is irrelevant under a bounded
  injective translation, and Proflog's current `proof-code-bytes` sanity check
  is not yet a full size proof because it encodes proof skeletons rather than
  formula-bearing tableau trees. Track 2b must either expand skeletal
  certificates into formula-bearing trees with preserved size accounting, prove
  the skeleton grammar is itself an admissible non-compressing tableau encoding,
  or treat the kernel certificate path as an implementation-stage shortcut. See
  [SJAS Proof-Size Relevance](docs/log/2026-05-26-sjas-proof-size-relevance.md).
- Refined ADR-0073 Track 2a classification for procedure-call proof
  constructors after the reflected-clause reachability probe exposed `neg-call`
  in an SJAS certificate. Procedure calls are now classified as a relevant macro
  layer whose primitive status remains unresolved: Track 2b must either name
  Fitting/Proflog procedure calls as part of the selected SJAS deduction
  apparatus, prove bounded expansion over reflected Group-2b axiom applications
  and ordinary tableau rules, or exclude reflected-clause proofs using
  `pos-call`, `neg-call`, guarded-call, or equality-triggered call constructors
  from the correspondence fragment. See
  [SJAS Procedure-Call Relevance](docs/log/2026-05-26-sjas-procedure-call-relevance.md).
- Refined ADR-0073 Track 2a equality classification. SJAS arithmetic equality
  proof symbols `sjas-equal` and `sjas-eq-progress` are now classified as
  relevant object-language arithmetic evidence, while generic Proflog
  free-constructor equality and disequality constructors remain unresolved
  proof-system extensions. The focused audit test first failed with
  `sjas-equal` still classified as unresolved; the green change moves SJAS
  arithmetic equality into the relevant code/arithmetic classification without
  changing `eq-step`, `neq-close`, `neq-rigid`, `neq-store`, `refl-close`, or
  equality-triggered calls. See
  [SJAS Equality and Disequality Relevance](docs/log/2026-05-26-sjas-equality-relevance.md).
- Probed generic equality/disequality reachability in current SJAS theorem
  proofs. The non-arithmetic reflexive equality theorem `mark(1)=mark(1)`
  reaches `refl-close`, confirming generic equality is not merely an encodable
  possibility. The constructor-clash disequality theorem
  `mark(1)!=other(1)` reaches `free-close`, which the current SJAS proof-code
  encoder rejects as an unsupported certificate symbol. This exposes a
  proof-certificate grammar gap: Track 2b must not claim correspondence over
  all current SJAS theorem proofs until reachable generic equality tags are
  encoded and classified, represented by existing certificate constructors, or
  excluded by a stated fragment boundary. See
  [SJAS Equality Constructor Reachability](docs/log/2026-05-26-sjas-equality-reachability.md).
- Inventoried current proof-producing tags against the SJAS certificate
  alphabet. The gap is broader than `free-close`: equality internals,
  guarded-call helper tags, compact/U-Grounding code-reader evidence such as
  `sjas-code-arg`, and profile-header proof tags can be emitted by kernel or
  SJAS profile code without being listed in `proof-symbols`. The executable
  correspondence audit now reports `:unencodable-symbols` separately from
  `:unclassified-symbols`; the red test first showed that
  `(sjas-code-arg 1 sjas-code-args-end)` and `(free-close)` had no explicit
  unencodable report. See
  [SJAS Proof-Tag Inventory](docs/log/2026-05-26-sjas-proof-tag-inventory.md).
- Recorded the lower-bound orientation for Track 2b macro-expansion proofs.
  The correspondence proof does not need byte-for-byte or size equality between
  Proflog and SJAS proof objects; it must show Proflog does not accept a more
  compressed proof object than the SJAS semantic-tableau proof predicate would
  allow under Willard's anti-compression requirement. This makes bounded macro
  expansion preferable to fragment exclusion where viable: equality or
  procedure-call constructors that expand to ordinary tableau work, increase
  tree size, or act only at branch tips may be adequate if they preserve the
  relevant lower bound. See
  [SJAS Macro Expansion and Lower-Bound Adequacy](docs/log/2026-05-26-sjas-macro-expansion-lower-bound.md).
- Classified ADR-0073 Track 2a quantifier and witness policy. Quantifier child
  formulas, existential fresh-parameter witnesses, universal instantiated
  parameter terms, bounded-quantifier side conditions, and ordinary gamma
  repeatability are relevant. Runtime ordering of admissible instantiations is
  probably irrelevant only under a proof that accepted certificates and size
  accounting are unchanged. Current `univ`, `once-univ`, and `witness` proof
  nodes are skeletal because their witness terms live in branch state rather
  than proof-code payloads, so Track 2b must reconstruct formula-bearing
  quantifier steps or enrich/canonicalize certificates. See
  [SJAS Quantifier and Witness Relevance](docs/log/2026-05-26-sjas-quantifier-witness-relevance.md).
- Inventoried ADR-0073 Track 2a profile-specific SJAS branch rules. Arithmetic
  equality/relation closure, syntax-code predicates, substitution-code, and
  axiom membership are relevant object-language predicate work that may be
  admitted as bounded macros or internalized directly. `tableau-proof/3` and
  `subst-prf/4` wrappers remain high-risk bridges because their non-`sjas-axiom`
  paths still call `kernel/prove-programo`; they need direct object-level
  checking or the full Track 2b proof-and-test correspondence. See
  [SJAS Profile Theory Rule Inventory](docs/log/2026-05-26-sjas-profile-theory-rule-inventory.md).
- Summarized ADR-0073 Track 2a coverage after the single-threaded relevance
  slices. The status note consolidates the relevant, probably irrelevant, and
  macro-expandable aspects; records evidence-backed risks such as reachable
  `neg-call`, `refl-close`, `free-close`, missing code-reader proof tags, and
  skeletal quantifier witnesses; and narrows the remaining Track 2a gaps to
  full reachability coverage, generic sidecar exclusion/expansion, Level-1
  `subst-prf`, axiom-basis boundaries, beta validity, and Track 2b proof-medium
  selection. See
  [SJAS Track 2a Coverage Status](docs/log/2026-05-26-sjas-track2a-coverage-status.md).

## 2026-05-25

- Logged the refined ADR-0072 internalization/correspondence program after the
  discussion of whether `kernel/prove-programo` necessarily violates the SJAS
  self-justification invariant. The note records the exchange verbatim,
  summarizes the distinction between direct arithmeticization and theorem-backed
  correspondence, and preserves a future goal prompt for the three coordinated
  tracks: arithmeticization, relevance analysis for tableau intensional
  features, and correspondence between the SJAS-specified deductive apparatus
  and the Proflog kernel. See
  [SJAS Internalization and Proflog Correspondence Program](docs/log/2026-05-25-sjas-internalization-correspondence-program.md).
- Started [ADR-0073](docs/adr/ADR-0073-sjas-internalization-correspondence-program.md)
  on branch `adr-0073-sjas-correspondence-program` to coordinate the refined
  three-track program. Track 1 continues direct arithmeticization of SJAS proof
  machinery; Track 2a classifies the tableau intensional measures relevant to
  self-justification; Track 2b requires both a proper correspondence proof and
  operational tests. The initial Track 2a matrix classifies tableau tree
  structure, rule-induced branching, branch closure, inspectable code encoding,
  and proof-size discipline as relevant; rule scheduling and runtime mechanics
  as probably irrelevant subject to proof; and equality, procedure-call/profile
  rules, quantifier policy, and Proflog proof-certificate layout as unresolved.
  See
  [SJAS Tableau Relevance Matrix](docs/log/2026-05-25-sjas-tableau-relevance-matrix.md).
- Continued ADR-0073 by adding an executable proof-symbol classification audit
  for the encoded SJAS certificate alphabet. The red test first failed because
  the audit namespace did not exist; the green implementation classifies every
  symbol that `proflog.willard-sjas-code` can encode as relevant or unresolved
  under the current Track 2a matrix, and reports unresolved obligations for
  equality, procedure-call/profile, and optimized-layer constructors appearing
  in actual proof terms. The ADR and relevance matrix now also specify that
  Track 2b's "proper proof" requires compatible formal semantics for both the
  Proflog kernel and the SJAS tableau apparatus, or a third-party/common
  intermediate formalization capable of rigorous equivalence checking.
  Verification: focused `proflog.sjas-correspondence-test` passed 3 tests /
  9 assertions; `lein test-proflog-fast` passed 149 tests / 562 assertions;
  `lein test-proflog-extended` passed 68 tests / 203 assertions.
- Deepened ADR-0073 Track 2a relevance analysis on branch
  `adr-0073-track2a-relevance`. The follow-up note corroborates the user
  hypothesis conditionally: tableau tree shape, rule-induced child structure,
  closure, inspectable coding, substitution vocabulary, axiom basis, and proof
  size growth are relevant, while rule scheduling and runtime mechanics are
  irrelevant only after proving they preserve accepted proof trees and size
  bounds. It leaves equality, procedure calls, profiled closure, quantifier
  witness policy, beta validation, and proof-certificate constructor layout as
  explicit Track 2b proof obligations. See
  [SJAS Tableau Relevance Deep Dive](docs/log/2026-05-25-sjas-tableau-relevance-deep-dive.md).
- Added a Track 2a runtime reachability audit for representative
  non-`sjas-axiom` SJAS certificates. The probe shows the current Tableau-0
  beta, arithmetic theorem, and Group-3 certificates use a smaller constructor
  set than the full encodable alphabet, but `profiled`,
  `willard-sjas-tableau0`, `willard-sjas-arithmetic`, and `sjas-equal` are
  actually reachable, not merely theoretical. Procedure-call and general
  equality/disequality constructors were not reached by this probe. See
  [SJAS Proof-Constructor Reachability Audit](docs/log/2026-05-25-sjas-proof-constructor-reachability-audit.md).
- Refined the ADR-0073 Track 2a classification of `profiled` proof wrappers.
  The outer `(profiled willard-sjas-tableau0 p)` and
  `(profiled willard-sjas-level1 p)` wrappers are probably irrelevant
  annotations if wrapper erasure and profile selection are proven. By contrast,
  `willard-sjas-arithmetic`, code, proof-check, and subst-proof-check wrappers
  mark relevant object-language or bridge work. Generic `(profiled
  propositional p)` and `(profiled first-order p)` sidecars appear excluded
  from ordinary SJAS profile proofs because the profile hides `:clauses` before
  kernel search. See
  [SJAS Profile Wrapper Relevance](docs/log/2026-05-25-sjas-profile-wrapper-relevance.md).
- Continued the profile-wrapper refinement by making the executable
  `proflog.sjas-correspondence` audit path-sensitive for concrete
  `(profiled kind subproof)` forms. The red test first failed because
  `classify-profile-form` did not exist; the green implementation classifies
  outer SJAS profile annotations as probably irrelevant, arithmetic/code/proof
  wrappers as relevant, and generic propositional/first-order sidecars as
  probably excluded. Verification: focused
  `profile-wrapper-audit-is-path-sensitive` passed 1 test / 3 assertions;
  `proflog.sjas-correspondence-test` passed 4 tests / 12 assertions;
  `lein test-proflog-fast` passed 150 tests / 565 assertions; `lein
  test-proflog-extended` passed 68 tests / 203 assertions.
- Probed Level-1 proof-constructor reachability for Track 2a and did not
  retain it as a verifier. The broad command attempted to synthesize Level-1
  beta and Group-3 theorem proofs through `sjas/query-succeeds`, then audit
  their proof terms; it produced no constructor inventory after several
  minutes. The follow-up note records this as an impractical evidence path and
  narrows the next requirement to auditing supplied Level-1 certificates or
  adding an explicit slow reachability test, rather than conflating constructor
  reachability with expensive theorem search. See
  [SJAS Level-1 Proof-Constructor Reachability Probe Boundary](docs/log/2026-05-25-sjas-level1-reachability-probe-boundary.md).
- Followed the narrowed Level-1 path by auditing a supplied `sjas-axiom`
  `subst-prf/4` validation proof under a `240s` shell timeout. The direct
  validation probe returned one proof containing `(profiled
  willard-sjas-level1 (profiled willard-sjas-subst-proof-check
  (sjas-code-bytes) (willard-sjas-subst-code) sjas-axiom))`, confirming that
  Level-1 substitution proof-check and substitution-code wrappers are reachable
  in proof-predicate validation. The executable profile-form classifier now
  accepts relation-specific profiled payload arities after a red test exposed
  that the earlier three-element wrapper assumption missed
  `willard-sjas-subst-proof-check`. Verification:
  `proflog.sjas-correspondence-test` passed 5 tests / 14 assertions; `lein
  test-proflog-fast` passed 151 tests / 567 assertions; `lein
  test-proflog-extended` passed 68 tests / 203 assertions.
- Ran a bounded reflected-clause reachability probe for Track 2a. Proving
  `demo(1)` in the Tableau-0 demo system returned a certificate containing
  `neg-call`, so procedure-call constructors are reachable in current SJAS
  proof certificates covering reflected clauses. This converts procedure-call
  treatment from a speculative Track 2b risk into a concrete obligation:
  either primitive SJAS rule status, bounded macro expansion through reflected
  Group-2b axioms, or an explicit fragment exclusion. The result is recorded in
  [SJAS Proof-Constructor Reachability Audit](docs/log/2026-05-25-sjas-proof-constructor-reachability-audit.md).
- Classified finite `fuel` for ADR-0073 Track 2a. Fuel is not a Willard
  proof-object feature, but it is an operational Proflog evaluator bound that
  can affect whether a supplied certificate validates at a particular finite
  bound. Track 2b must therefore state either an unbounded/existentially
  sufficient-fuel correspondence theorem, or an explicit bounded-search theorem
  with its own size relation. See
  [SJAS Fuel Relevance](docs/log/2026-05-25-sjas-fuel-relevance.md).

## 2026-05-23

- Continued [ADR-0072](docs/adr/ADR-0072-sjas-object-level-proof-machinery.md)
  by removing the generated proof-antecedent registry. The red tests required
  the SJAS registry to omit `:sjas/system-entries` and required
  `tableau-proof/3` and `subst-prf/4` non-`sjas-axiom` certificates to keep
  working after that key was removed manually. The green implementation makes
  theorem-query antecedents use canonical code-nom binders at source
  compilation, decodes `system-code` during proof-predicate application, maps
  the decoded theorem axioms to the double-negated antecedent shape used by the
  kernel refutation, and left-conjoins the reconstructed list in builder order.
  The direct proof-predicate paths now reject active `system-code` terms as
  ill-typed theorem/source formula codes before expensive certificate decoding
  or antecedent reconstruction. Deferred boundary: arbitrary bad decoded kernel
  proof terms and Level-1 beta-style proof-mismatch checks still need the
  planned proof-code tree checker instead of `kernel/prove-programo`.
  The bounded contradiction timing probe now uses an `sjas-axiom` contradiction
  citation so it records the same not-found timing shape without entering that
  deferred generic proof-term checker path. Verification: focused no-registry
  tableau/subst regressions, generated theorem-code proof checks, structural
  non-generated theorem-code checks, the source audit, and the contradiction
  timing probe passed under 600 second bounds; `lein test-proflog-fast` passed
  145 tests / 548 assertions; `lein test-proflog-extended` passed 68 tests /
  203 assertions; and `lein test-proflog-sjas` passed 44 tests / 251 assertions.
- Continued ADR-0072 by making compact formula-code syntax predicates expose
  constructor-byte proof evidence. The red test required successful `wff/1`
  proofs over compact codes to contain `sjas-code-arg` rather than only the
  bare staged `sjas-code-bytes` marker. The green implementation threads proof
  evidence through the compact code-argument relation and removes the
  `ground-formal-code-term` shortcut from the syntax predicate decoder. The
  earlier failed attempt showed that applying the same compact relational reader
  to generic non-`sjas-axiom` proof-certificate theorem targets makes the
  substantive self-consistency demonstration impractical, so that path now has
  an explicit compact-only staged decoder and substitution target decoding keeps
  its existing ground-code staging. Verification: focused syntax, structural
  non-generated syntax, U-Grounding byte-reader, substitution, and substantive
  self-consistency regressions passed under 600 second bounds; `lein
  test-proflog-fast` passed 145 tests / 548 assertions; `lein
  test-proflog-extended` passed 68 tests / 203 assertions; and `lein
  test-proflog-sjas` passed 44 tests / 253 assertions.
- Continued ADR-0072 by exposing compact beta axiom theorem-code bytes through
  the object code-byte relation. The red test required the beta
  `sjas-axiom` proof branch to include `sjas-code-arg`; before the change it
  returned only `(sjas-system-code-bytes (sjas-code-bytes))` for both system and
  formula code reads. Reading both compact `system-code` and theorem code
  relationally exceeded the 600 second focused bound, so the green
  implementation keeps compact `system-code` staged and routes the smaller beta
  theorem-code argument through `sjas-object-code-byteso`. Focused verification:
  `sjas-tableau-proof-accepts-axiom-citation-certificates` passed 1 test /
  4 assertions under the 600 second bound. Full verification: `lein
  test-proflog-fast` passed 145 tests / 548 assertions; `lein
  test-proflog-extended` passed 68 tests / 203 assertions; and `lein
  test-proflog-sjas` passed 44 tests / 254 assertions.
- Continued ADR-0072 by exposing compact fixed-axiom theorem-code bytes through
  the object code-byte relation. The red test required Group-0 and Group-1
  `sjas-axiom` citation proofs to contain `sjas-code-arg`; before the change
  both formula-code reads used the staged `(sjas-code-bytes)` marker. The green
  implementation keeps compact `system-code` staged but routes the cited fixed
  axiom formula code through `sjas-object-code-byteso`. Focused verification:
  `sjas-tableau-proof-cites-fixed-axiom-groups-from-system-code` passed 1 test /
  8 assertions under the 600 second bound. Full verification: `lein
  test-proflog-fast` passed 145 tests / 548 assertions; `lein
  test-proflog-extended` passed 68 tests / 203 assertions; and `lein
  test-proflog-sjas` passed 44 tests / 256 assertions.
- Probed three additional ADR-0072 internalization moves and did not retain
  their code. Reading reflected axiom theorem-code bytes through the object
  compact-code relation exceeded the focused reflected citation bound. Replacing
  the compact byte-term lookup with constructor-bit peeling made focused syntax
  and citation tests green, but the full SJAS suite stalled in the structural
  non-generated theorem-code negative check. Returning even a bounded
  proof-code byte-read summary made focused proof checks green, but the full
  SJAS suite stalled in the compact kernel-certificate proof check. These
  remain real ADR-0072 boundaries: reflected formula-code reads, compact byte
  argument arithmetic, and proof-code read evidence need a different
  proof-search strategy rather than simply returning larger relational proof
  trees.
- Continued ADR-0072 by removing stale side-table placeholders from compiled
  SJAS program maps. The red tests required generated SJAS programs to omit
  top-level `:sjas/system-code`, `:sjas/fact-atoms`, and `:sjas/proof-targets`
  keys, and required the generic procedure-call relation to keep resolving
  clauses for a registry-only profile-bearing compiled program. The green
  implementation leaves live source-preprocessing metadata under
  `:sjas/registry` and removes the old nil/empty top-level compatibility slots
  from both the SJAS builder and the program-view relation. Focused
  verification: `call-clauseo-accepts-registry-only-profile-metadata` passed 1
  test / 5 assertions, and `sjas-formal-codes-are-godel-byte-terms` passed 1
  test / 11 assertions. Full verification: `lein test-proflog-fast` passed 146
  tests / 553 assertions; `lein test-proflog-extended` passed 68 tests / 203
  assertions; and `lein test-proflog-sjas` passed 44 tests / 256 assertions.
- Continued ADR-0072 by removing the matching top-level source-metadata
  fallbacks from the SJAS kernel profile. The red test manually dropped
  `:sjas/registry`, reintroduced `:sjas/system-code`, `:sjas/code-format`, and
  `:sjas/symbol-index-entries` on the compiled program map, and showed that a
  Group-0 `sjas-axiom` citation still succeeded. The green implementation makes
  active-system, code-format, and symbol-index lookup ignore stale top-level
  program keys, leaving the source-preprocessing registry as the only accepted
  host metadata path. Focused verification: the new registry-requirement
  regression passed 1 test / 2 assertions; fixed-axiom citation passed 1 test /
  8 assertions; beta citation passed 1 test / 4 assertions; the
  beta/reflected composite check passed 1 test / 9 assertions; and syntax-code
  decoding passed 1 test / 8 assertions. Full verification: `lein
  test-proflog-fast` passed 146 tests / 553 assertions; `lein
  test-proflog-extended` passed 68 tests / 203 assertions; and `lein
  test-proflog-sjas` passed 45 tests / 258 assertions.
- Probed the next possible ADR-0072 byte-reader slice for `subst-code/2` source
  formula codes and did not retain it. The red test required the
  `subst-code(selfcons-skeleton-code, group3-code)` proof to expose
  `sjas-code-arg` evidence instead of the bare `(profiled
  willard-sjas-subst-code)` marker. Removing the source-side
  `ground-formal-code-term` branch and threading the object byte-read proof made
  the focused Level-1 substitution regression run for roughly eight minutes
  without completing, so the code and test changes were backed out. This remains
  a real boundary, but it needs a bounded source-code reader or a proof shape
  that avoids re-walking the compact skeleton before it can be made green.

## 2026-05-21

- Completed OCR, assessment, and organization of Dan Willard's collected
  nachlass scans in the nested `sjas/` repo:
  [`collected_dew_materials/README.md`](sjas/nachlass/collected_dew_materials/README.md).
  Eighteen unique PDF witnesses (225 pages) now have merged OCR text under
  `ocr/text/`, a `manifest.tsv` inventory, topic indexes, and reproducible
  scripts. One exact duplicate alias was skipped. Quality is mostly
  `needs_review` (ocrad fallback on typewriter/fax scans); the 2020
  incompleteness lecture notes and 2008 ZCF/ZF drafts are grep-searchable but
  not formula-authoritative. Dec 2025 scans include additional incompleteness
  and Hilbert-program drafts plus a Trivers-Willard biology article. See
  [`sjas/nachlass/LOG.md`](sjas/nachlass/LOG.md).
- Started [ADR-0072](docs/adr/ADR-0072-sjas-object-level-proof-machinery.md)
  on branch `adr-0072-sjas-object-proof-machinery`. The active goal is to
  internalize SJAS proof machinery so that arithmetic coding and decoding
  needed while applying predicates such as `tableau-proof/3` and `subst-prf/4`
  happens at the kernel/object-code level. The first implementation slice
  targets theorem-code decoding inside proof predicates; later slices must
  remove generated system/axiom registries and eventually replace decoded
  Proflog proof-term validation with code-level tableau proof-tree checking.
- During the first ADR-0072 slice, rejected a ground host-side theorem-code
  decoder as insufficient: it decoded actual bytes rather than using the
  formula registry, but still materialized proof targets in Clojure during
  `tableau-proof/3`. The pure relational fix was to make structural code-to-AST
  translation construct variable and quantifier nodes with concrete shared
  code-nom constants instead of calling nominal constructors on logic variables.
- ADR-0072 first-slice verification before commit `b8615cf`: focused
  theorem-code proof checks passed (`sjas-tableau-proof-checks-kernel-certificates`,
  `sjas-subst-prf-checks-identity-substitution-certificates`,
  `sjas-tableau-proof-checks-structural-non-generated-theorem-codes`,
  `sjas-subst-prf-checks-structural-non-generated-theorem-codes`, plus the
  self-consistency certificate regression). `lein test-proflog-fast` passed
  145 tests / 548 assertions; `lein test-proflog-extended` passed 68 tests /
  203 assertions; `lein test-proflog-sjas` passed 35 tests / 223 assertions.
  The full SJAS namespace run was approximately 29 minutes after removing the
  theorem-target registry shortcut.
- Continued ADR-0072 by removing generated formula syntax registries from
  SJAS predicate application. The red test first asserted that the active SJAS
  registry no longer carries `:sjas/formula-entries`,
  `:sjas/formula-negation-entries`, `:sjas/formula-class-entries`, or
  `:sjas/neg-pair-entries`; it failed while those tables were still generated.
  The green implementation makes `wff/1`, formula-class predicates, and
  `neg-pair/2` use structural formula-code decoding rather than finite formula
  lookup tables. Focused verification: `sjas-syntax-predicates-decode-formula-godel-codes`
  passed 1 test / 7 assertions, and
  `sjas-structural-code-predicates-accept-non-generated-formula-codes` passed
  1 test / 5 assertions. Isolated fresh-JVM timings for the smallest
  `true/false` code path were approximately 10.2s for `wff/1`, 10.5s for
  `delta-star-0-code/1`, and 29.4s for `neg-pair/2`. This removes the generated
  formula registry but does not yet eliminate `ground-formal-code-term`; that
  deterministic byte extractor is host-side computation and remains an
  ADR-0072 boundary to remove or strictly isolate from the semantic
  proof-predicate path. Regression verification after the change:
  `lein test-proflog-fast` passed 145 tests / 548 assertions,
  `lein test-proflog-extended` passed 68 tests / 203 assertions, and
  `lein test-proflog-sjas` passed 35 tests / 224 assertions in approximately
  30.5 minutes.
- Attempted the next ADR-0072 slice: removing compact `code-N` terms from the
  deterministic formula/substitution-code byte extractor. A red test required
  `wff/1` proof evidence to include `sjas-code-arg`, distinguishing relational
  byte-argument parsing from host byte-vector extraction. The focused syntax,
  tableau-proof, substitution-proof, general substitution, and fixed-point
  certificate probes could be made green, but the substantive self-consistency
  demonstration did not complete after roughly 30 minutes in isolation, and the
  full SJAS namespace was stopped twice at roughly 56-58 minutes. The attempted
  code change was not retained; compact and U-Grounding ground byte extraction
  remain an explicit ADR-0072 boundary pending a better object-level code
  reader.
- Continued ADR-0072 by moving Group-2 beta axiom citation off generated
  `axiom-member/2` metadata. The red test required
  `sjas-tableau-proof-accepts-axiom-citation-certificates` to include
  `sjas-system-beta-axiom` evidence; before the change it returned only
  `(willard-sjas-axiom-member (sjas-generated-axiom-member))`. The green
  implementation decodes the beta formula byte section of `system-code` and
  compares it to the theorem-code bytes before falling back to generated
  metadata for non-beta axiom groups. This is progress but not completion:
  Group-0, Group-1, reflected Group-2b, and Group-3 membership still fall back
  to generated facts, and the beta path still uses `ground-formal-code-term` to
  expose already-ground byte strings. Verification: focused axiom-citation,
  kernel-certificate, substitution-certificate, and substantive self-consistency
  tests passed; `lein test-proflog-fast` passed 145 tests / 548 assertions;
  `lein test-proflog-extended` passed 68 tests / 203 assertions; and
  `lein test-proflog-sjas` passed 35 tests / 225 assertions in approximately
  37 minutes.
- Continued ADR-0072 by moving reflected Group-2b axiom citation off generated
  `axiom-member/2` metadata. The red test used a `composite/1` reflected clause
  and required the `tableau-proof(system-code, reflected-clause-code,
  sjas-axiom-code)` proof to contain `sjas-system-reflected-axiom` evidence;
  before the change it closed through the generated axiom-member fallback. The
  green implementation reads the reflected-clause section of `system-code`,
  reconstructs `forall x1 ... forall xn. body -> R(x1, ..., xn)`, and compares
  that formula against the theorem code modulo alpha-equivalence. This is
  progress but not completion: Group-0, Group-1, and Group-3 membership still
  fall back to generated facts, and the reflected path still uses
  `ground-formal-code-term` to expose already-ground byte strings. Verification:
  `lein test-proflog-sjas` passed 35 tests / 227 assertions in approximately
  44 minutes.
- Continued ADR-0072 by moving fixed Group-0 and Group-1 axiom citation off the
  generated `axiom-member/2` fallback. The red test required `sjas-axiom`
  certificates for representative Group-0 and Group-1 records to contain
  `sjas-system-group-zero-axiom` or `sjas-system-group-one-axiom`, and to omit
  `sjas-generated-axiom-member`; before the change both closed through the
  generated fallback. The green implementation validates the encoded system
  header, decodes the theorem formula code, and matches fixed axiom formulas in
  the compact decoded representation. This is progress but not completion:
  Group-3 membership still falls back to generated facts, the fixed path still
  uses `ground-formal-code-term`, and proof predicates still validate decoded
  proof terms by invoking the kernel. Verification: focused fixed-axiom,
  beta-axiom, reflected-clause, and substantive self-consistency tests passed;
  `lein test-proflog-fast` passed 145 tests / 548 assertions;
  `lein test-proflog-extended` passed 68 tests / 203 assertions; and
  `lein test-proflog-sjas` passed 36 tests / 233 assertions in approximately
  41 minutes.
- Continued ADR-0072 by moving Tableau-0 Group-3 axiom citation off generated
  `axiom-member/2` metadata. The red test required a `sjas-axiom` certificate
  for the Tableau-0 self-consistency axiom to contain
  `sjas-system-group-three-axiom` and omit `sjas-generated-axiom-member`; before
  the change it used the generated fallback. The green implementation validates
  the Tableau-0 system-code header, decodes the theorem formula, and reconstructs
  `forall p. not tableau-proof(system-code, false-code, p)` using either compact
  embedded code terms or U-Grounding sentinel numerals. This is progress but not
  completion: Level-1 Group-3 still falls back to generated facts, all current
  axiom-group decoders still use `ground-formal-code-term`, and proof predicates
  still validate decoded proof terms by invoking the kernel. Verification:
  focused Tableau-0 Group-3, fixed-axiom, beta-axiom, reflected-clause, and
  substantive self-consistency tests passed; `lein test-proflog-fast` passed
  145 tests / 548 assertions; `lein test-proflog-extended` passed 68 tests /
  203 assertions; and `lein test-proflog-sjas` passed 37 tests / 236 assertions
  in approximately 50 minutes.
- Continued ADR-0072 by moving Level-1 Group-3 axiom citation off generated
  `axiom-member/2` metadata. The red test required a `sjas-axiom` certificate
  for the Level-1 self-consistency axiom to contain
  `sjas-system-level1-group-three-axiom` and omit
  `sjas-generated-axiom-member`; before the change it used the generated
  fallback. The green implementation validates the Level-1 system-code header,
  decodes the final Group-3 formula, extracts and decodes the embedded
  substitution-code term, and checks that the embedded code denotes the expected
  fixed-point skeleton over the same system-code term. This is progress but not
  completion: standard axiom citations no longer require generated facts, but
  the generated fallback branch remains, all current axiom-group decoders still
  use `ground-formal-code-term`, and proof predicates still validate decoded
  proof terms by invoking the kernel. Verification: focused Level-1 Group-3,
  Tableau-0 Group-3, fixed-axiom, beta-axiom, reflected-clause, and substantive
  self-consistency tests passed; `lein test-proflog-fast` passed 145 tests /
  548 assertions; `lein test-proflog-extended` passed 68 tests / 203 assertions;
  and `lein test-proflog-sjas` passed 38 tests / 239 assertions in
  approximately 49 minutes.
- Continued ADR-0072 by removing the generated `axiom-member/2` fallback from
  `sjas-axiom` proof-certificate checking. The red test injected a bogus
  generated membership fact for the contradiction code and showed that
  `tableau-proof(system-code, false-code, sjas-axiom-code)` incorrectly trusted
  it. Removing the fallback exposed a legitimate relational `subst-prf` path
  where code terms are bound by core.logic rather than immediately visible to
  host ground extraction; the fix walks code terms through equality sigma before
  structural axiom membership, preserves sigma in ground substitution-source
  checks, and lets the staged byte reader fall back to the structural code
  relation when host extraction fails. This is progress but not completion:
  ordinary `axiom-member/2` queries still close from generated facts,
  `ground-formal-code-term` remains as the fast path for already-ground codes,
  and proof predicates still validate decoded proof terms by invoking the
  kernel. Verification: the injected-fact test, all focused axiom-citation
  tests, substantive self-consistency, and the subst-prf independence regression
  passed; `lein test-proflog-fast` passed 145 tests / 548 assertions;
  `lein test-proflog-extended` passed 68 tests / 203 assertions; and
  `lein test-proflog-sjas` passed 39 tests / 240 assertions in approximately
  56 minutes.
- Continued ADR-0072 by removing the generated `axiom-member/2` fact path from
  ordinary SJAS predicate evaluation. The red test injected a bogus generated
  membership fact for the contradiction code and required
  `axiom-member(system-code, false-code)` to keep failing. The green
  implementation routes ordinary `axiom-member/2` closure through the same
  decoded system-code membership relation used by `sjas-axiom` proof
  certificates, and adds `axiom-member/2` to the direct SJAS profile route so
  ground code queries avoid generic agenda walking before structural decoding.
  This is progress but not completion: builder-generated `axiom-member/2`
  clauses and registry fact metadata still exist as artifacts, current
  membership decoders still use `ground-formal-code-term` for already-ground
  public codes, and proof predicates still validate decoded proof terms by
  invoking the kernel. Verification so far: the injected ordinary-query test
  passed in 19 seconds; focused positive builder, composite beta/reflected,
  cross-profile generated-axiom, axiom-citation, fixed-axiom, Tableau-0
  Group-3, Level-1 Group-3, and injected proof-certificate tests passed;
  `lein test-proflog-fast` passed 145 tests / 548 assertions;
  `lein test-proflog-extended` passed 68 tests / 203 assertions; and
  `lein test-proflog-sjas` passed 40 tests / 241 assertions in approximately
  63 minutes.
- Continued ADR-0072 by removing the stale generated `axiom-member/2` builder
  metadata. The red test required the SJAS registry to omit `:sjas/fact-atoms`
  and the compiled program to omit generated `axiom-member` clauses; it failed
  while the registry still stored the finite generated fact table. The green
  implementation removes the old fact-table builder and leaves
  `axiom-member/2` membership solely to decoded system-code membership in the
  proof profile. Also tightened `sjas-axiom` proof-certificate dispatch so a
  failed axiom citation cannot fall through into generic kernel proof search.
  An inherited full-SJAS process from the interrupted session was stopped after
  more than two hours; its stack was in a slow `subst-prf` path that later
  passed as a focused test. Verification: the artifact-removal test passed; the
  injected ordinary-query and proof-certificate regressions passed after
  manually injecting stale registry facts; the axiom-citation no-fallthrough
  regression passed; `sjas-subst-prf-uses-substitution-code-independently-of-theorem-code`
  passed in about 150 seconds; `lein test-proflog-fast` passed 145 tests / 548
  assertions; `lein test-proflog-extended` passed 68 tests / 203 assertions;
  and a clean `lein test-proflog-sjas` passed 41 tests / 243 assertions in
  about 35 minutes.
- Continued ADR-0072 by isolating the U-Grounding axiom-citation byte decoder
  from the deterministic host shortcut. The red test required a ground
  U-Grounding `tableau-proof/3` citation to include `sjas-ug-code-byte-cons`
  evidence for system/theorem code decoding; before the change it succeeded
  with only `sjas-system-code-bytes (sjas-ug-code-bytes)`. A blunt removal of
  the shortcut timed out after materializing the complete bit list for a large
  system numeral, so the green implementation uses a bounded kernel relation
  that peels six canonical `0`/`1`/`dbl`/`add(_,1)` bits per byte and records a
  summarized fixed-radix byte-cons proof. Compact `code-N` constructor decoding
  remains on the legacy shortcut path for now, and other
  `ground-formal-code-term` uses remain in formula/substitution decoding.
  Verification: the red U-Grounding proof-predicate test passed after the
  object decoder in about 45 seconds; U-Grounding syntax, bound-code, and
  subst-code focused tests passed; compact axiom-citation and ordinary
  `axiom-member/2` regressions passed after restoring the compact-only shortcut;
  `lein test-proflog-fast` passed 145 tests / 548 assertions;
  `lein test-proflog-extended` passed 68 tests / 203 assertions; and a clean
  `lein test-proflog-sjas` passed 41 tests / 246 assertions in about 33
  minutes.

## 2026-05-20

- Logged Willard's semantic-tableaux proof-encoding requirements and linked
  them to Proflog's current proof-code encoder. Willard does not prescribe one
  unique byte layout; the binding constraint is the Conventional Tableaux
  Encoding Requirement, while the 2001 paper supplies a concrete base-64
  byte-string/tree example. Proflog's `proof-code-bytes` is therefore recorded
  as the selected ordinary-tableau kernel proof-term byte layout, not as a
  byte-for-byte reproduction of every historical Willard proof-list notation.
  See
  [Willard Tableau Proof Encoding](docs/log/2026-05-20-willard-tableau-proof-encoding.md).
- Logged a literature survey and conceptual clarification on Hilbert–Bernays
  derivability conditions, Kleene (1943), and the relationship between HB
  conditions and Lawvere-style fixed points. The SJAS corpus cites Hilbert &
  Bernays (1939) substantively in several papers (especially 2001 and 2011) but
  never cites Kleene (1943); only Kleene (1938) on ordinal notation /
  fixed-point self-reference appears. HB conditions are not themselves a Lawvere
  fixed point — they are axioms on a provability predicate used together with a
  separate diagonal/fixed-point construction in Löb and Second Incompleteness
  proofs. See
  [SJAS HB/Kleene Literature and Lawvere Framing](docs/log/2026-05-20-sjas-hb-kleene-literature-and-lawvere-framing.md).
- Evaluated Willard's diagonalization-preclusion argument for correctness. The
  durable conclusion is conditional: the argument is coherent for the specific
  Type-A U-Grounding/semantic-tableaux/conventional-proof-encoding profile, but
  it should not be overstated as a general impossibility of diagonalization.
  The strongest counterarguments are scope counterarguments around proof-object
  compression, cut-like reuse, hidden numeral compression, non-multiplicative
  fast-growth primitives, and unsafe finite beta extensions. See
  [SJAS Diagonalization Preclusion Evaluation](docs/log/2026-05-20-sjas-diagonalization-preclusion-evaluation.md).
- Traced Willard's exact additive-versus-multiplicative diagonalization
  mechanism. The growth illustration is in the 2011 paper's comparison of
  `x_i = x_{i-1}+x_{i-1}` with `y_i = y_{i-1}*y_{i-1}`. The exact collapse point
  in the semantic-tableaux second-incompleteness proof is Lemma 4.7 in the 2002
  JSL paper: total multiplication builds the short squaring-chain fragments
  reused by Lemma 4.8; when multiplication is only a relation, Lemma 4.7 fails
  and the proof collapses. The positive Type-A mechanism is Fact D.3 in the
  2011 paper, where small U-Grounding deduction trees must have an unclosed
  branch unless total multiplication is added. See
  [SJAS Diagonalization Mechanism Trace](docs/log/2026-05-20-sjas-diagonalization-mechanism-trace.md).
- Logged a verbatim research note on the relationship between Willard's
  U-Grounding expressivity, proof-object coding, deductive apparatus choice,
  and the proposed "arithmetic efficiency" explanation of why diagonalization
  can fail in Type-A/tableau SJAS settings. See
  [SJAS Diagonalization Efficiency Question](docs/log/2026-05-20-sjas-diagonalization-efficiency-question.md).

## 2026-05-19

- Logged a second decidable-SJAS research pass focused on whether a decidable
  language could equal, or preserve the introspection-relevant fragment of,
  Willard's U-Grounding language. The note records a screening lemma: full
  unrestricted first-order equivalence to standard U-Grounding should be
  undecidable because U-Grounding has total addition and a `Delta*0`
  definition of multiplication's graph. The strongest candidates are therefore
  weaker readings: single-base Buchi/automatic arithmetic, WS1S proof
  intervals, WS2S/tree-automata proof trees, and controlled MSO+BAPA/Parikh
  counting. See
  [Decidable SJAS Candidates Around U-Grounding Expressivity](docs/log/2026-05-19-decidable-sjas-u-grounding-candidates.md).
- Started the decidable-fragment SJAS survey prompted by Boigelot-Fontaine-
  Vergain's `Decidability of Difference Logic over the Reals with
  Uninterpreted Unary Predicates`. The first pass maps quantified difference
  logic, linear arithmetic, unary versus higher-arity predicates, real/integer/
  natural domains, and quantification into an SJAS suitability matrix. Current
  conclusion: no reviewed decidable first-order fragment is ready to host the
  existing Willard/Proflog `SelfCons_k(beta,d)` proof predicate; the best
  candidate for a new decidable SJAS variant is integer/natural difference
  logic with unary predicates and an automata/S1S-style proof apparatus whose
  certificates are regular/local. The note was extended with a parameter matrix
  distinguishing pure arithmetic from arithmetic plus arbitrary predicates, and
  satisfiability decidability from internal proof-predicate definability. It was
  then extended with a source-backed theorem inventory covering the prompt
  paper, adjacent BSR/difference-constraint linear-arithmetic fragments, and the
  current conclusion that no off-the-shelf decidable FOL fragment has yet been
  identified that both internalizes Willard-style `SelfCons_k(beta,d)` and
  preserves global decidability. A final survey pass added explicit parameter
  coverage, a deductive-apparatus verdict matrix, and a completion audit. See
  [SJAS Decidable Difference-Logic Fragment Survey](docs/log/2026-05-19-sjas-decidable-difference-logic-survey.md).
- Logged the SJAS `Pi*1` beta clarification: Willard's finite `IS#_D(beta)`
  condition should be read as requiring the installed Group-2 beta axioms to be
  checked `Pi*1` encodings, even if user-facing source uses a more convenient
  notation that is lowered into that fragment. The same note records that
  universal quantification over `Mult` relation arguments does not assert
  multiplication totality; the dangerous form is the existential totality
  principle `forall x y. exists z. Mult(x,y,z)`. It was extended with an
  explicit exchange summary covering the source location for `IS#_D(beta)`,
  the "has a `Pi*1` encoding" reading, `Pi1` versus `Pi*1`, and the conditional
  universal reading of relational multiplication. See
  [SJAS Pi-Star-1 Beta and Relational Multiplication Clarification](docs/log/2026-05-19-sjas-pi-star-beta-mult-clarification.md).

## 2026-05-18

- Prepared the LOPSTR+PPDP 2026 SJAS system-description draft in the sibling
  `lopstr-ppdp26` paper repository after archiving the earlier miniKanren draft
  artifacts. The paper is scoped as a system description of the finite
  ordinary-tableau `IS#_D(beta)` profile, with the public Proflog system link,
  U-Grounding syntax/proof-code support, and explicit limitations around
  Tab-1/proof-list reuse and open public-code synthesis. Fresh focused evidence
  for the paper-prep pass is recorded in
  [TEST_RUNTIME_BASELINE](docs/TEST_RUNTIME_BASELINE.md): `lein
  test-proflog-sjas-slow` passed in `real 746.91 s`, and `lein
  test-proflog-sjas` passed in `real 1687.83 s`. The standard gates also
  passed: `lein test-proflog-fast` in `real 83.45 s` and `lein
  test-proflog-extended` in `real 195.65 s`.
- Logged the research discussion that began with Turing-machine references in
  the SJAS literature and broadened into native self-justifying computational
  systems. The key correction is that alternative computational paradigms
  should not merely simulate first-order SJAS proof predicates; each needs its
  own native notions of assertion, evidence, checking, self-reference, failure,
  and restricted reflection. A later addendum marks the regular-invariant
  Turing-machine mechanism as suspect: "no halting oracle" is too coarse, since
  Willard's total-multiplication boundary appears to matter through its
  consequences for coding and diagonalization rather than by acting as a
  primitive halting oracle. The caution is explicitly provisional and should be
  revised if contrary evidence is found. A further addendum records evidence for
  the caution and frames the adjacent question of whether subrecursive or
  weaker-than-SJAS systems, such as primitive-recursive or automata-like
  systems, can support a native self-justifying property without merely
  reintroducing the expressive strength Willard suppresses. A later correction
  notes that primitive recursive arithmetic is quantifier-free in its usual
  native presentation: multiplication is a total term-former, not a quantified
  totality axiom. Additional provisional addenda record the free-variable PRA
  consistency-statement idea and the implementation-first route: search for
  sub-Turing substrates that can host syntax, proof checking, and a restricted
  self-consistency claim without reintroducing diagonalization. A speculative
  final addendum frames the general problem through Lawvere/Yanofsky
  diagonalization: useful systems may evade diagonal paradoxes by failing a
  specific fixed-point-theorem hypothesis while retaining restricted
  self-reference. See
  [Native Self-Justifying Computational Systems](docs/log/2026-05-18-native-self-justifying-computational-systems.md).

## 2026-05-17

- Completed [ADR-0071](docs/adr/ADR-0071-sjas-u-grounding-syntax-coding.md)
  on branch `adr-0071-sjas-u-grounding-syntax-coding`. SJAS systems now have an
  opt-in `:code-format :u-grounding` mode whose formula, system, and proof
  codes are ordinary binary U-Grounding numerals over `0`, `1`, `dbl`, and
  `add`, with no generated `code-N` constructors in that system language. The
  decoder preserves trailing zero byte strings with a sentinel and, for
  non-ground bound code entries, records the `byte + 64 * tail` byte-cons
  relation and fixed-radix multiplication proof evidence. The attempted
  `project` shortcut was removed; already-ground public codes use a
  deterministic constructor-pop entry shortcut before entering the structural
  formula/proof decoders, so open public code synthesis remains documented as
  unsupported. Final verification: `lein test-proflog-sjas-slow` passed with
  `5` tests, `22` assertions, `real 722.20 s`; `lein test-proflog-sjas` passed
  with `35` tests, `221` assertions, `real 1717.35 s`; `lein test-proflog-fast`
  passed with `145` tests, `548` assertions, `real 120.25 s`; and
  `lein test-proflog-extended` passed with `68` tests, `203` assertions,
  `real 254.93 s`. See
  [AAR-0071](docs/aar/AAR-0071-sjas-u-grounding-syntax-coding.md).

## 2026-05-15

- Started [ADR-0071](docs/adr/ADR-0071-sjas-u-grounding-syntax-coding.md) on
  branch `adr-0071-sjas-u-grounding-syntax-coding`. The new goal is to add an
  opt-in SJAS code format where formula, system, and proof codes are ordinary
  U-Grounding binary numerals rather than generated `code-N` constructors. This
  addresses the logged multiplication-tradeoff criterion by making the syntax
  decoder reconstruct byte-sequence cons cells through relation-backed
  multiplication in the SJAS proof profile.
- Started [ADR-0070](docs/adr/ADR-0070-sjas-byte-sequence-coding-audit.md) on
  branch `adr-0070-sjas-tableau-proof-coding-audit`. The Willard corpus was
  rechecked for the user's proof-coding question: Willard explicitly codes both
  formulae and semantic-tableaux proofs through `Prf`, `SemPrf`, `ExPrf`, and
  `SubstPrf`. The selected Proflog target remains finite ordinary-tableau
  `IS#_D(beta)`, with Tab-1/Tab-k deferred. The immediate implementation audit
  is to preserve public byte-string codes directly, rather than normalizing them
  through a natural-number conversion that can drop trailing zero bytes. See
  [SJAS Proof-Coding Citations](docs/log/2026-05-15-sjas-proof-coding-citations.md).
- Completed [ADR-0070](docs/adr/ADR-0070-sjas-byte-sequence-coding-audit.md)
  on branch `adr-0070-sjas-tableau-proof-coding-audit`. Canonical formula,
  system, and proof code terms now preserve exact byte strings directly, so
  embedded code payloads ending in byte `0` remain decodable by `wff/1`.
  Formula-entry deduplication now keys by byte vector rather than lossy natural
  value. Focused red-green evidence showed the trailing-zero formula-code
  regression failing before the change and passing after it. Verification:
  `lein test-proflog-sjas-slow` passed with `5` tests, `22` assertions,
  `real 16m40.470s`; `lein test-proflog-sjas` passed with `29` tests, `195`
  assertions, `real 32m32.713s`; `lein test-proflog-fast` passed with `145`
  tests, `548` assertions, `real 2m37.518s`; and
  `lein test-proflog-extended` passed with `68` tests, `203` assertions,
  `real 5m33.947s`. See
  [AAR-0070](docs/aar/AAR-0070-sjas-byte-sequence-coding-audit.md).
- Logged the SJAS multiplication-tradeoff relevance criterion. The
  programming-language demonstration must make the absence of a total
  multiplication function matter not only in user-visible arithmetic, but also
  in the arithmetized syntax/proof machinery; otherwise the implementation
  demonstrates an executable reflection profile rather than the distinctive
  Willard tradeoff. See
  [SJAS Multiplication Tradeoff Relevance](docs/log/2026-05-15-sjas-multiplication-tradeoff-relevance.md).
- Completed [ADR-0069](docs/adr/ADR-0069-sjas-general-subst-code.md) on branch
  `adr-0069-sjas-general-subst-code`. `subst-code/2` now decodes formula-code
  bytes and computes diagonal substitution structurally, including non-generated
  open formulas, quantifier shadowing, alpha-equivalent fixed-point targets,
  and the Level-1 `selfcons-skeleton-code -> group-three-code` path without
  generated substitution entries. `subst-prf/4` now uses source-code
  well-formedness when only substitution existence is needed and proves the
  supplied theorem code directly. Verification: `lein test-proflog-sjas-slow`
  passed with `5` tests, `22` assertions, `real 915.85 s`;
  `lein test-proflog-sjas` passed with `26` tests, `188` assertions,
  `real 2057.15 s`; `lein test-proflog-fast` passed with `145` tests,
  `548` assertions, `real 143.16 s`; and `lein test-proflog-extended` passed
  with `68` tests, `203` assertions, `real 349.26 s`. See
  [AAR-0069](docs/aar/AAR-0069-sjas-general-subst-code.md),
  [SJAS General Subst Code](docs/log/2026-05-15-sjas-general-subst-code.md),
  and [SJAS IS#_D(beta) Completion Audit](docs/log/2026-05-15-sjas-isdbeta-completion-audit.md).

## 2026-05-14

- Audited the SJAS implementation after ADR-0068. The finite ordinary-tableau
  `IS#_D(beta)` substrate now has arithmetized formula/system/proof codes,
  structural syntax predicates, Level-1 substitution-proof vocabulary,
  fixed-point substitution, structural theorem-code proof targets, and passing
  slow/fast/extended gates. Remaining documented non-goals are Tab-1/proof-list
  theorem reuse, general non-identity substitution beyond the generated
  fixed-point entry, and open proof-code synthesis. See
  [SJAS Completion Audit](docs/log/2026-05-14-sjas-completion-audit.md).
- Completed [ADR-0068](docs/adr/ADR-0068-sjas-structural-theorem-targets.md)
  on branch `adr-0068-sjas-theorem-code-targets`. `tableau-proof/3` and
  `subst-prf/4` can now check real certificates for non-generated theorem codes
  by structurally decoding the theorem formula, computing its NNF complement,
  translating that decoded complement into the kernel AST, and running the core
  proof checker. The promoted example is `lt(1,2)`, whose code is valid but not
  generated as a Group axiom; both proof predicates reject the same certificate
  for `lt(2,1)`. Verification: `lein test-proflog-sjas-slow` passed with `4`
  tests, `16` assertions, `real 452.96 s`; `lein test-proflog-sjas` passed
  with `25` tests, `182` assertions, `real 1947.15 s`;
  `lein test-proflog-fast` passed with `145` tests, `548` assertions,
  `real 129.32 s`; and `lein test-proflog-extended` passed with `68` tests,
  `203` assertions, `real 287.48 s`. See
  [AAR-0068](docs/aar/AAR-0068-sjas-structural-theorem-targets.md) and
  [SJAS Structural Theorem-Code Targets](docs/log/2026-05-14-sjas-structural-theorem-targets.md).
- Completed [ADR-0067](docs/adr/ADR-0067-sjas-structural-code-decoder.md) on
  branch `adr-0067-sjas-code-decoder`. The SJAS profile now structurally parses
  formula-code byte streams for `wff/1`, formula-class predicates,
  `neg-pair/2`, and identity `subst-code/2`, so a valid non-generated formula
  code such as `lt(1,2)` no longer has to appear in the finite generated axiom
  registry. The new semantic test is marked `^:slow`. Verification:
  `lein test-proflog-sjas-slow` passed with `2` tests, `8` assertions, `real
  170.85 s`; `lein test-proflog-sjas` passed with `23` tests, `174`
  assertions, `real 767.20 s`; `lein test-proflog-fast` passed with `145`
  tests, `548` assertions, `real 129.36 s`; and
  `lein test-proflog-extended` passed with `68` tests, `203` assertions,
  `real 299.24 s`. The remaining SJAS proof boundary is that
  `tableau-proof/3` still bridges theorem codes to kernel AST formulas for
  arbitrary proof targets. See
  [AAR-0067](docs/aar/AAR-0067-sjas-structural-code-decoder.md) and
  [SJAS Structural Code Decoder](docs/log/2026-05-14-sjas-structural-code-decoder.md).
- Completed [ADR-0066](docs/adr/ADR-0066-sjas-subst-relation.md) on branch
  `adr-0066-sjas-subst-relation`. The finite SJAS substitution boundary is now
  exposed as `subst-code/2`, with identity entries for generated closed
  formulas and the Level-1 `selfcons-skeleton-code -> group-three-code` entry.
  `subst-prf/4` now consults `subst-code/2` and proves the supplied theorem
  code independently of the substituted code. Verification:
  `lein test-proflog-sjas-slow` passed with `1` test, `3` assertions, `real
  91.34 s`; `lein test-proflog-sjas` passed with `22` tests, `169` assertions,
  `real 561.14 s`; `lein test-proflog-fast` passed with `145` tests, `548`
  assertions, `real 97.61 s`; and `lein test-proflog-extended` passed with
  `68` tests, `203` assertions, `real 225.08 s`. See
  [AAR-0066](docs/aar/AAR-0066-sjas-subst-relation.md).
- Started [ADR-0066](docs/adr/ADR-0066-sjas-subst-relation.md) on branch
  `adr-0066-sjas-subst-relation`. ADR-0065 still tied substitution facts to a
  theorem-code-specific `subst-prf/4` table, but Willard's Appendix A separates
  `Subst(g,h)` from `SubstPrf(g,t,p)`.
- Completed [ADR-0065](docs/adr/ADR-0065-sjas-selfcons-substitution-fixpoint.md)
  on branch `adr-0065-sjas-selfcons-subst-fixpoint`. Level-1 SJAS Group-3 now
  uses the generated `Gamma_1(g)` skeleton code as the `subst-prf/4`
  substitution argument, exposes `:selfcons-skeleton-code`, and rejects
  `system-code` in that position. The proof profile also accepts a formal
  `sjas-axiom` certificate by checking generated `axiom-member/2` facts, and
  proof-code decoding now supports a wide proof-symbol byte form. A generic
  Level-1 Group-3 theorem proof was stopped after about `7m44s` without a
  result; the promoted fixed-point certificate test instead uses the
  object-level axiom-citation proof path. Verification so far:
  `lein test-proflog-sjas-slow` passed with `1` test, `3` assertions, `real
  82.81 s`; `lein test-proflog-sjas` passed with `20` tests, `162` assertions,
  `real 406.83 s`; `lein test-proflog-fast` passed with `145` tests, `548`
  assertions, `real 93.95 s`; and `lein test-proflog-extended` passed with
  `68` tests, `203` assertions, `real 222.85 s`. See
  [AAR-0065](docs/aar/AAR-0065-sjas-selfcons-substitution-fixpoint.md).
- Started [ADR-0065](docs/adr/ADR-0065-sjas-selfcons-substitution-fixpoint.md)
  on branch `adr-0065-sjas-selfcons-subst-fixpoint`. The completion audit after
  ADR-0064 found that Level-1 Group-3 cited `subst-prf(system-code,
  system-code, ...)`, but Willard Appendix A requires a fixed-point skeleton
  `Gamma_1(g)` and final sentence `Gamma_1(n)` where `n` is the skeleton code.
- Logged the testing policy for the ongoing SJAS fidelity work: slow tests are
  acceptable when they capture substantive proof, correctness, or semantic
  properties. They should be marked as slow and timed, but completeness of the
  implementation takes priority over fast-running demonstrations.
- Started [ADR-0064](docs/adr/ADR-0064-sjas-substitution-proof-predicate.md)
  on branch `adr-0064-sjas-subst-proof`. The user correctly objected that even
  a code-term `tableau-proof/3` does not by itself justify the Level-1
  self-justification claim if Group-3 uses raw proof predicates rather than the
  substitution-aware vocabulary in Willard's `SelfCons_k(beta,d)`. The accepted
  correction is to add `subst-prf/4`, route it through decoded kernel proof
  checking, and make generated `SelfCons1` cite `subst-prf/4` instead of raw
  `tableau-proof/3`. See
  [SJAS Substitution-Proof Boundary](docs/log/2026-05-14-sjas-substitution-proof-boundary.md).
- Completed [ADR-0064](docs/adr/ADR-0064-sjas-substitution-proof-predicate.md).
  The SJAS language now declares `subst-prf/4`, Level-1 Group-3 cites
  `subst-prf/4` rather than raw `tableau-proof/3`, and the profile checks
  identity-substitution proof certificates by decoding proof-code terms and
  reusing the kernel proof route. Red evidence was the missing public
  `sjas/subst-prf` helper. Verification: `lein test-proflog-sjas` passed with
  `17` tests, `152` assertions, `0` failures, `0` errors, `real 299.59 s`;
  `lein test-proflog-fast` passed with `145` tests, `548` assertions, `0`
  failures, `0` errors, `real 96.78 s`; `lein test-proflog-extended` passed
  with `68` tests, `203` assertions, `0` failures, `0` errors, `real 219.78 s`.
  See [AAR-0064](docs/aar/AAR-0064-sjas-substitution-proof-predicate.md).
- Started [ADR-0063](docs/adr/ADR-0063-sjas-arithmetized-coding.md) on
  branch `adr-0063-sjas-arithmetized-coding`. The user correctly objected that
  ADR-0062 still cannot demonstrate full Willard self-justification if
  `tableau-proof/3` receives hash-derived formula labels and resolves theorem
  targets from `:sjas/proof-targets`. The accepted direction is to promote
	  formula, system, complement, and proof certificate codes to inspectable
	  base-64 SJAS Godel-code terms and make `wff`, formula-class predicates,
  `neg-pair`, and `tableau-proof/3` decode those codes at the proof-profile
  boundary. See also
  [SJAS Arithmetized Coding Research](docs/log/2026-05-14-sjas-arithmetized-coding-research.md).
- Completed [ADR-0063](docs/adr/ADR-0063-sjas-arithmetized-coding.md). The
  implementation replaces hash-derived formula labels and `:sjas/proof-targets`
  with compact base-64 Godel-code terms `(code-N b0 ... bN-1)`, generated
  formula/system decode relations, and proof-certificate byte decoding inside
  `tableau-proof/3`. During implementation, large binary `dbl/add` Godel
  numerals caused stack overflows, so codes now expose base-64 bytes directly
  while keeping each byte as a small SJAS binary numeral. A second mismatch came
  from Group-3 proof checking: theorem queries refute `(A -> T)` with the
  operational double negation of `A`, not merely `to-nnf(A)`, so the proof
  registry now stores the exact refutation-side axiom formula. Verification:
  `lein test-proflog-sjas` passed with `15` tests, `143` assertions, `0`
  failures, `0` errors, `elapsed 4:47.84`; `lein test-proflog-fast` passed with
  `145` tests, `548` assertions, `0` failures, `0` errors, `elapsed 2:07.41`;
  `lein test-proflog-extended` passed with `68` tests, `203` assertions, `0`
  failures, `0` errors, `elapsed 4:37.84`. See
  [AAR-0063](docs/aar/AAR-0063-sjas-arithmetized-coding.md) and
  [Willard SJAS Base-64 Coding Profile Example](worked-examples/willard-sjas.md).
- Logged the SJAS coding boundary exposed after ADR-0062. Hash-derived formula
  symbols are acceptable as finite Proflog codebook labels, but they are not
  Willard-style arithmetic Godel codes. Therefore the current SJAS profile is a
  finite reflected proof-substrate demonstrator, not a full arithmetized
  `IS#_D(beta)` implementation. See
  [SJAS Godel-Coding Boundary](docs/log/2026-05-14-sjas-godel-coding-boundary.md).
- Completed [ADR-0062](docs/adr/ADR-0062-sjas-self-justification-demonstration.md)
  for the Willard SJAS self-justification demonstration. The issue was that
  `SelfCons0` named `contradiction-code`, but the generated
  `:sjas/proof-targets` table did not map that code to a theorem target, so
  contradiction proof checks failed by lookup. The fix maps
  `contradiction-code` to the theorem target for `false`, maps `not-code(c)` to
  complement theorem targets, and extends proof-certificate encoding for nested
  generic kernel profile tags such as `first-order`. Red evidence:
  `lein test :only proflog.willard-sjas-test/sjas-system-builder-generates-groups-and-reflected-boundary`
  failed in `real 11.36 s`. Verification: `lein test-proflog-sjas` passed with
  `13` tests, `125` assertions, `0` failures, `0` errors, `real 33.95 s`;
  `lein test-proflog-fast` passed with `145` tests, `548` assertions,
  `0` failures, `0` errors, `real 100.47 s`; `lein test-proflog-extended`
  passed with `68` tests, `203` assertions, `0` failures, `0` errors,
  `real 227.65 s`. See
  [AAR-0062](docs/aar/AAR-0062-sjas-self-justification-demonstration.md) and
  [Willard SJAS Binary Profile Example](worked-examples/willard-sjas.md).
- Clarified the Willard SJAS worked example around query-triggered evaluation:
  constructing an SJAS system builds a reflected theory and executable program,
  while proof search starts only through `query/query-succeeds`,
  `sjas/query-succeeds`, `sjas/query-answers`, or related query entrypoints.
  Added side-by-side examples for reflected-only programs and programs with
  external clauses, plus the current Group-2b trust boundary. During validation,
  found that SJAS profile metadata attached to compiled programs prevented the
  ordinary Procedure Call Rule from seeing reflected/external user clauses.
  Fixed `proflog.program` lookup to accept SJAS-annotated compiled-program
  shapes while preserving the relational clause-list lookup contract. Red test:
  `lein test-proflog-sjas` failed with two new assertions showing `demo(1)` and
  `external-demo(0)` were not queryable. Verification after the fix:
  `lein test-proflog-sjas` passed with `11` tests, `112` assertions,
  `0` failures, `0` errors, `real 15.40 s`;
  `lein test-proflog-fast` passed with `145` tests, `548` assertions,
  `0` failures, `0` errors, `real 71.06 s`;
  `lein test-proflog-extended` passed with `68` tests, `203` assertions,
  `0` failures, `0` errors, `real 198.45 s`.
- Logged the SJAS authoring distinction between directly adding formulas and
  adding finite reflected clauses. A `beta` formula is a Group-2 proper axiom
  of the reflected SJAS and changes the system code and Group-3 claim, but it
  does not create an executable Proflog procedure clause. A `reflected`
  `(|- head body)` clause is both executable procedure text and a finite
  Group-2b axiom formula, universally closed as `body -> head`, so it can be
  used by ordinary procedure-call evaluation and cited by the internal
  `tableau-proof/3` axiom-membership path. An `external` clause is executable
  procedure text only: it can participate in ordinary query evaluation but is
  not an axiom of the reflected SJAS and does not change Group-3.
- Added worked and tested SJAS `composite/1` examples showing when a definition
  belongs in `beta` rather than `reflected`. The beta example uses
  `forall x. mult(2,2,x) -> composite(x)` as a Group-2 axiom: it proves
  `composite(4)` through `sjas/query-succeeds`, but the direct Procedure Call
  Rule query has no `composite/1` clause and returns no proof. The reflected
  version uses the same definition as a clause, yielding a Group-2b axiom,
  direct procedure-call success, theorem-level success, and answer synthesis
  for `x = 4`. An exploratory 120 s wrapper for the broader
  `exists y z. y != 1 and z != 1 and mult(y,z,x)` composite definition did not
  return a result, so the promoted example stays bounded. Verification:
  `lein test-proflog-sjas` passed with `12` tests, `119` assertions,
  `0` failures, `0` errors, `real 34.17 s`.
- Logged the reason `sjas/query-succeeds` exists while Robinson Q did not need
  an analogous public helper. The SJAS helper is not a separate prover; it wraps
  a user formula as `(:axiom-formula system) -> formula` and then calls ordinary
  `query/query-succeeds` on the generated program. Q arithmetic can use the
  ordinary query API because the `:robinson-q` language selects a fixed
  deduction-modulo proof profile over fixed Q rules/axioms. SJAS systems are
  generated per source declaration: `beta` and reflected clauses change the
  system code, formula codes, Group-3, and axiom basis. The helper exists so
  callers ask "prove from this generated SJAS basis" rather than accidentally
  asking only "run this formula against this compiled program."

## 2026-05-13

- Started [ADR-0061](docs/adr/ADR-0061-sjas-full-arithmetic-proof-checking.md)
  on branch `adr-0061-sjas-full-arithmetic-proof-checking` after the
  ADR-0060 MVP. The goal is to replace finite named SJAS numerals and
  arithmetic fact tables with binary object numerals over constants `0` and
  `1`, relation-backed U-grounding arithmetic, and a `tableau-proof/3`
  certificate checker that validates Proflog kernel proof terms instead of the
  `mini-closed` placeholder. The local Willard corpus supports this direction:
  the later Type-A presentations use constants `0` and `1`, addition and
  doubling for larger numerals, non-growth arithmetic including subtraction,
  division, maximum, logarithm, root, and bit-count, and semantic-tableau proof
  predicates.
- Completed ADR-0061 by replacing finite SJAS numerals and arithmetic fact
  tables with binary object numerals, relation-backed U-grounding arithmetic,
  answer-overlay theory hook support, and a `tableau-proof/3` checker that
  decodes structural proof certificates and checks them through the Proflog
  kernel relation. Updated the worked example, README pointer, user guide,
  ADR, and [AAR-0061](docs/aar/AAR-0061-sjas-full-arithmetic-proof-checking.md).
  Verification: `lein test-proflog-sjas` passed with `11` tests, `110`
  assertions, `0` failures, `0` errors, `real 51.22 s`;
  `lein test-proflog-fast` passed with `145` tests, `548` assertions, `0`
  failures, `0` errors, `real 236.58 s`; `lein test-proflog-extended` passed
  with `68` tests, `203` assertions, `0` failures, `0` errors,
  `real 572.25 s`.

## 2026-05-10

- Started [ADR-0060](docs/adr/ADR-0060-willard-sjas-mvp.md) on branch
  `adr-0060-willard-sjas-mvp` to implement the MVP Willard SJAS-lang from
  ADR-0058 and ADR-0059. The implementation target includes the frontend
  system builder, `:willard-sjas-tableau0` and `:willard-sjas-level1` proof
  profiles, generated Group-Zero through Group-3 axiom bases, relational
  arithmetic/classifier/certificate predicates, reflected/external clause
  boundary tests, route audits, worked examples, timing records, and a final
  completion audit. Initial documentation gate: `lein test-proflog-fast` passed
  with `143` tests, `537` assertions, `0` failures, `0` errors,
  `real 73.38 s`.
- Completed ADR-0060 by adding `proflog.willard-sjas`,
  `proflog.kernel.willard-sjas-profile`, `lein test-proflog-sjas`, focused
  tests, worked examples, user-guide/README links, runtime records, and
  [AAR-0060](docs/aar/AAR-0060-willard-sjas-mvp.md). The MVP generates finite
  Group-Zero through Group-3 axiom bases, stable object-language formula codes,
  `axiom-member` facts, reflected Group-2b user-clause entries, external
  application clauses, relation-backed finite `mult/3` examples, miniature
  certificate predicates, bounded-quantifier NNF lowering through `leq/2`
  guards, and proof terms tagged with `willard-sjas-tableau0` or
  `willard-sjas-level1`. Verification: `lein test-proflog-sjas` passed with
  `9` tests, `61` assertions, `0` failures, `0` errors, `real 30.95 s`;
  the focused bounded-quantifier NNF selector passed with `1` test, `8`
  assertions, `0` failures, `0` errors, `real 6.75 s`;
  the focused frontend clause-emission selector passed with `1` test, `3`
  assertions, `0` failures, `0` errors, `real 17.47 s`;
  `lein test-proflog-fast` passed with `145` tests, `548` assertions, `0`
  failures, `0` errors, `real 89.21 s`;
  `lein test-proflog-extended` passed with `68` tests, `203` assertions, `0`
  failures, `0` errors, `real 255.14 s`.
- On branch `review/sjas-lang-profile-design`, recorded an independent SJAS
  design review (nachlass posture, Willard commitments, Proflog proof-profile
  mapping, and implementation slices) without altering ADR-0058’s branch-owned
  text. Added [ADR-0059](docs/adr/ADR-0059-willard-sjas-profile-independent-review.md)
  and [Willard SJAS — Independent Agent Review Synthesis](docs/log/2026-05-10-willard-sjas-agent-review-synthesis.md).
  ADR-0058 remains the sibling canonical design ADR from the parallel effort.
- Reviewed the local Willard SJAS corpus in `sjas/nachlass/` and extracted the
  implementation-relevant requirements for a Proflog SJAS language profile. The
  first viable target is the Type-A, semantic-tableaux, Level-1 line rather than
  the later Hilbert/theta-function line, because it matches Proflog's
  Fitting-style kernel and existing proof-profile architecture. Opened
  [ADR-0058](docs/adr/ADR-0058-willard-sjas-language-profile.md) and the longer
  [Willard SJAS Profile Design Notes](docs/log/2026-05-10-willard-sjas-profile-design.md).
  Verification for the documentation branch: `lein test-proflog-fast` passed
  with `143` tests, `537` assertions, `0` failures, `0` errors, `real 99.84 s`.
- Refined ADR-0058 after follow-up design review to state the executable
  SJAS-lang motivation explicitly: mechanizing SJAS should expose the
  correspondence between logical restrictions and what programs can run. The
  design now stages a first ordinary-tableau `IS(A)`-style profile,
  `:willard-sjas-tableau0`, before the Level-1 `:willard-sjas-level1` profile,
  and records where Group-Zero, Group-1, Group-2, Group-3, and proof/syntax
  coding predicates live in Proflog. Verification: `lein test-proflog-fast`
  passed with `143` tests, `537` assertions, `0` failures, `0` errors,
  `real 70.33 s`.
- Logged the Q-versus-SJAS axiom-membership distinction and the intended
  programmer-facing SJAS authoring model. Existing Q/Pelletier examples use
  host-side axiom labels or conjoined antecedents, not reflected
  object-language axiom membership. SJAS needs a generated system wrapper or
  query wrapper that carries the axiom basis, reflected `axiom-member`
  relation, proof-coding relations, and generated Group-3 self-consistency
  axiom so users can write beta axioms and ordinary Proflog clauses without
  hand-constructing the fixed point. See
  [Willard SJAS Profile Design Notes](docs/log/2026-05-10-willard-sjas-profile-design.md).
  Verification: `lein test-proflog-fast` passed with `143` tests, `537`
  assertions, `0` failures, `0` errors, `real 86.43 s`.
- Clarified the ADR-0058 reflected-system boundary for user-supplied SJAS
  programs. The local 2001 Willard `IS(A)` witness defines Group-3 as a
  self-reference to proofs from `IS(A)`, and its Appendix B spells this as
  proofs from Group-Zero/Group-1/Group-2 plus the self sentence. The local 2013
  `ISD(A)` / `IS#_D(beta)` witness makes the dependency sharper: replacing the
  infinite Group-2 schema with finite beta changes the "I am" fragment of
  Group-3. Therefore a Proflog clause that is meant to be cited by the internal
  `tableau-proof` predicate is effectively a finite Group-2 or Group-2b
  reflected extension, while ordinary external Proflog code may reuse a fixed
  SJAS basis only if it is not included in `axiom-member`. See
  [Willard SJAS Profile Design Notes](docs/log/2026-05-10-willard-sjas-profile-design.md).
  Verification: `lein test-proflog-fast` passed with `143` tests, `537`
  assertions, `0` failures, `0` errors, `real 73.20 s`.

## 2026-05-09

- Started
  [ADR-0057](docs/adr/ADR-0057-relational-equality-fragment.md) on branch
  `adr-0057-relational-equality-fragment`. The ADR scopes an opt-in
  relational replacement experiment for the equality-fragment host engine,
  including a relational closed-term gamma enumerator, route guards against
  `prove-program-host` and `gamma/closed-terms-for-fuel`, representative GV and
  transition-system gates, timing probes, and an explicit promotion or
  rejection decision in the future AAR.
- Completed
  [ADR-0057](docs/adr/ADR-0057-relational-equality-fragment.md) by adding an
  opt-in relation-backed equality-fragment route with relational gamma
  generation and full ADR-0039 finite-verifier completion parity. The focused
  selector passed with `Ran 5 tests containing 32 assertions`, `0 failures`,
  `0 errors`, `real 82.97 s`; the comparison probe passed in `real 106.21 s`.
  The final concurrent gates passed: relational equality-fragment `real
  198.56 s`, kernel finite verifiers `real 221.58 s`, Fitting programs `real
  172.93 s`, fast `real 196.31 s`, and extended `real 319.86 s`.
  See [AAR-0057](docs/aar/AAR-0057-relational-equality-fragment.md).
- Started ADR-0056 to compose an authoritative greenfield user guide from the
  current implementation tutorial, source map, `src/proflog` code, and worked
  examples. See
  [ADR-0056](docs/adr/ADR-0056-greenfield-user-guide.md).
- Completed ADR-0056 by adding
  [Proflog Greenfield User Guide](docs/USER_GUIDE.md), linking it from the
  README, and recording the documentation-only AAR. The guide covers the public
  frontend, AST/language descent, query and answer APIs, kernel semantics,
  proof profiles, example families, test commands, all current `src/proflog`
  namespaces, and current operational boundaries. See
  [AAR-0056](docs/aar/AAR-0056-greenfield-user-guide.md).
- Completed
  [ADR-0055](docs/adr/ADR-0055-ski-relational-routing.md) on branch
  `adr-0055-ski-relational-routing`. The red route guard first failed because
  SKI tests entered `query/query-succeeds` and `answers/query-answers`.
  Closed proof rows now call `kernel/prove-programo` directly, and the answer
  row calls `answer-overlay/prove-program-query-entry-scheduledo`, which
  invokes `prove-program-query-entryo` and the relational residual scheduler.
  The route guard passed in `29.14 s`, the answer selector passed in
  `49.32 s`, and the full SKI selector passed with `Ran 8 tests containing 18
  assertions`, `0 failures, 0 errors`, `real 176.02 s`. The aggregate
  `lein test-proflog-turing-completeness` selector passed with `Ran 16 tests
  containing 35 assertions`, `0 failures, 0 errors`, `real 273.27 s`. See
  [AAR-0055](docs/aar/AAR-0055-ski-relational-routing.md) and
  [Combinatory Logic Example](worked-examples/combinatory-logic.md).
- Audited the current `:robinson-q` profile for remaining expressivity gaps.
  The theorem
  `forall x. x != zero -> exists y. s(add(y, s(zero))) = s(x)` is valid in Q
  and ordinary Q-as-antecedent proves it at fuel 16, but the profile returns no
  proof through fuel 384. This shows that ADR-0051's full-Q3 rule still closes
  only top-level predecessor disequalities; it does not expose `x = s(y)` for
  later congruence under successor contexts. See
  [Robinson Q Profile Expressivity Gap](docs/log/2026-05-09-robinson-q-profile-expressivity-gap.md).
- Completed
  [ADR-0052](docs/adr/ADR-0052-unified-robinson-q3-theory-rule.md) on branch
  `adr-0052-final-q3-deduction-modulo`. The old direct and add-one Q3 closers
  were replaced with one `q3-predecessor-equality-closeo` rule, and the profile
  now proves direct Q3, `q3-add-one-predecessor`, and the contextual theorem
  above with one `q3-predecessor-equality` proof marker. The red selector first
  failed with `Ran 12 tests containing 88 assertions`, `15 failures`,
  `real 36.46 s`; after implementation, `lein test-proflog-robinson-q` passed
  with `Ran 12 tests containing 88 assertions`, `real 22.24 s`, the comparison
  probe passed in `real 11.37 s`, and the concurrent standard gates passed:
  fast with `Ran 140 tests containing 502 assertions`, `real 100.89 s`;
  extended with `Ran 68 tests containing 203 assertions`, `real 241.21 s`. See
  [AAR-0052](docs/aar/AAR-0052-unified-robinson-q3-theory-rule.md) and
  [Robinson Q Proof Profile Example](worked-examples/robinson-q.md).
- Completed
  [ADR-0053](docs/adr/ADR-0053-robinson-q-theorem-examples.md) on branch
  `adr-0053-q-theorem-examples`. Added three non-trivial theorem examples:
  `forall x. add(x, s(s(zero))) = s(s(x))`,
  `forall x. mul(x, s(s(zero))) = add(add(zero, x), x)`, and
  `forall x. x != zero -> exists y. add(y, s(s(zero))) = s(x)`. The red
  selector first failed on the missing public theorem var in `real 10.83 s`;
  after implementation, `lein test-proflog-robinson-q` passed with `Ran 13
  tests containing 109 assertions`, `real 22.66 s`, and the comparison probe
  passed in `real 14.80 s`. The concurrent standard gates passed: fast with
  `Ran 141 tests containing 523 assertions`, `real 98.85 s`; extended with
  `Ran 68 tests containing 203 assertions`, `real 228.07 s`. See
  [AAR-0053](docs/aar/AAR-0053-robinson-q-theorem-examples.md).
- Completed
  [ADR-0054](docs/adr/ADR-0054-robinson-q-prime-evenness.md) on branch
  `adr-0054-robinson-q-prime-evenness`. Corrected the proposed Q primality
  helper by excluding both zero and one, corrected the "prime is not even"
  theorem by excluding two, and added two catalog formulas:
  `prime-other-than-two-has-no-two-factor` and
  `prime-other-than-two-is-not-left-even`. The red selector first failed with
  `No such var: rq/prime-other-than-two-has-no-two-factor`, `real 8.74 s`;
  after implementation, `lein test-proflog-robinson-q` passed with `Ran 15
  tests containing 123 assertions`, `real 20.69 s`, and the comparison probe
  passed in `real 12.27 s`. The concurrent standard gates passed: fast with
  `Ran 143 tests containing 537 assertions`, `real 84.57 s`; extended with
  `Ran 68 tests containing 203 assertions`, `real 200.09 s`. The theorem-only
  `:robinson-q` version of the factor theorem remains a documented search
  boundary: fuel 128 did not finish inside `timeout -k 5s 60s`,
  `real 60.07 s`. See
  [AAR-0054](docs/aar/AAR-0054-robinson-q-prime-evenness.md) and
  [Robinson Q Proof Profile Example](worked-examples/robinson-q.md).

## 2026-05-08

- Logged design notes on representing Robinson arithmetic Q in Proflog and on
  a possible deduction-modulo `:robinson-q` proof profile. The note records
  Q's function-symbol language, the distinction between axioms as assumptions
  versus theory conversion rules, Q3 as a controlled case split, and positive
  plus refutation proof-object sketches for Q7 when promoted to a rewrite
  rule. See
  [Robinson Q And Deduction Modulo Notes](docs/log/2026-05-08-robinson-q-deduction-modulo.md).
- Extended the Robinson Q note and opened
  [ADR-0048](docs/adr/ADR-0048-robinson-q-proof-profiles.md) on branch
  `adr-0048-robinson-q`. ADR-0048 requires both Q-as-antecedent and
  `:robinson-q` deduction-modulo proof-profile implementations, a generic
  language-level profile opt-in, and a shared correctness/performance
  comparison.
- Completed ADR-0048 with `proflog.robinson-q`, generic
  `proflog.proof-profile` dispatch, and the `:robinson-q` conversion profile.
  The focused selector passed with `Ran 5 tests containing 42 assertions`,
  `0 failures, 0 errors`, and `wall 8.94 s`; the comparison probe passed in
  `wall 7.82 s`. The standard concurrent gates passed:
  `lein test-proflog-fast` with `Ran 133 tests containing 456 assertions`,
  `0 failures, 0 errors`, `wall 75.27 s`; and
  `lein test-proflog-extended` with `Ran 68 tests containing 203 assertions`,
  `0 failures, 0 errors`, `wall 197.59 s`. See
  [AAR-0048](docs/aar/AAR-0048-robinson-q-proof-profiles.md) and
  [Robinson Q Proof Profile Example](worked-examples/robinson-q.md).
- Completed
  [ADR-0049](docs/adr/ADR-0049-robinson-q3-case-split-profile.md) so Q3 proves
  under both Robinson Q versions. Ordinary Q proves `(rq/q-implies rq/q3)` from
  the Q antecedent, while the `:robinson-q` profile closes the negated Q3 branch
  with `q3-case-split predecessor-or-zero` proof evidence. The red profile test
  first failed with `profile-proof` equal to `nil`; after implementation,
  `lein test-proflog-robinson-q` passed with `Ran 6 tests containing 48
  assertions`, `0 failures, 0 errors`, and `wall 8.89 s`. The comparison probe
  passed in `wall 7.83 s`. The concurrent commit gates passed:
  `lein test-proflog-fast` with `Ran 134 tests containing 462 assertions`,
  `0 failures, 0 errors`, `wall 73.22 s`; and
  `lein test-proflog-extended` with `Ran 68 tests containing 203 assertions`,
  `0 failures, 0 errors`, `wall 200.61 s`. See
  [AAR-0049](docs/aar/AAR-0049-robinson-q3-case-split-profile.md).
- Completed
  [ADR-0050](docs/adr/ADR-0050-kernel-interleaved-robinson-q-theory.md) to
  correct the `:robinson-q` profile architecture. The old host-side
  whole-formula normalizer and Q3 structural recognizer were replaced by a
  generic kernel theory hook plus miniKanren branch rules. The red checks first
  showed that Q proofs lacked ordinary `witness`, `once-univ`, and `neq-store`
  evidence and that the old host proof path was still present. After
  implementation, `lein test-proflog-robinson-q` passed with `Ran 9 tests
  containing 64 assertions`, `0 failures, 0 errors`, `real 13.06 s`; the
  comparison probe passed in `real 10.87 s`; and the concurrent gates passed:
  `lein test-proflog-fast` with `Ran 137 tests containing 478 assertions`,
  `0 failures, 0 errors`, `real 80.66 s`; and
  `lein test-proflog-extended` with `Ran 68 tests containing 203 assertions`,
  `0 failures, 0 errors`, `real 197.40 s`. See
  [AAR-0050](docs/aar/AAR-0050-kernel-interleaved-robinson-q-theory.md) and
  [Robinson Q Proof Profile Example](worked-examples/robinson-q.md).
- Logged the rationale for a future full Q3 theory rule. The note records why
  the current focused `q3-case-split` proves Q3 itself but is not enough for
  larger tableau refutations that need to introduce a predecessor and continue
  with `x = s(p)`. The example theorem is
  `forall x. x != zero -> exists y. add(y, s(zero)) = x`; its negated tableau
  branch requires general Q3 use, and a model of Q1, Q2, Q4, Q5, Q6, and Q7
  without Q3 shows that no proof can avoid Q3 or an equivalent lemma. See
  [Robinson Q3 Full Rule Rationale](docs/log/2026-05-08-robinson-q3-full-rule-rationale.md).
- Completed
  [ADR-0051](docs/adr/ADR-0051-full-robinson-q3-theory-rule.md) to add the
  full-Q3 predecessor rule required by that rationale. The profiled theorem
  `rq/q3-add-one-predecessor` now proves by storing `x != zero`, instantiating
  the single-use universal, reducing `add(y, s(zero))` by Q5/Q4, and closing
  with `q3-predecessor-intro`. The focused selector passed with `Ran 10 tests
  containing 73 assertions`, `0 failures, 0 errors`, `real 14.96 s`; the
  comparison probe passed in `real 12.01 s`; and the concurrent gates passed:
  `lein test-proflog-fast` with `Ran 138 tests containing 487 assertions`,
  `0 failures, 0 errors`, `real 89.92 s`; and
  `lein test-proflog-extended` with `Ran 68 tests containing 203 assertions`,
  `0 failures, 0 errors`, `real 226.94 s`. See
  [AAR-0051](docs/aar/AAR-0051-full-robinson-q3-theory-rule.md) and
  [Robinson Q Proof Profile Example](worked-examples/robinson-q.md).
- Completed
  [ADR-0047](docs/adr/ADR-0047-ski-quine-evaluation.md) on branch
  `adr-0047-ski-quine`. Direct `eval-for(3, omega, omega)` for the SKI
  self-reproducing term timed out inside a `240 s` wrapper, and adding
  argument-context reduction directly to `step/2` made the SKI selector time
  out inside a `900 s` wrapper. The accepted implementation isolates full
  contextual reduction in `full-step/2` and proves the guided omega trace in
  `95.44 s`; the full combinatory selector passed in `301.98 s`, and the
  aggregate TC selector passed in `438.34 s`. Standard gates passed too:
  `lein test-proflog-fast` in `96.41 s` and
  `lein test-proflog-extended` in `237.72 s`. See
  [AAR-0047](docs/aar/AAR-0047-ski-quine-evaluation.md) and
  [Combinatory Logic Example](worked-examples/combinatory-logic.md).
- Completed
  [ADR-0045](docs/adr/ADR-0045-minsky-trace-performance.md) and
  [ADR-0046](docs/adr/ADR-0046-combinatory-logic-turing-completeness.md) on
  branch `adr-0045-0046-tc-performance`. ADR-0045 adds a trace-shaped Minsky
  formula helper so the five-step transfer from `cfg(l0,2,0)` to
  `cfg(halt-label,0,2)` closes through compiled `step/2` calls; the focused
  namespace passed in `55.02 s`. ADR-0046 adds an independent SKI
  combinatory-logic TC demonstration with root reductions, bounded evaluation,
  answer export, and source audit; the focused namespace passed in `225.50 s`.
  The aggregate `lein test-proflog-turing-completeness` selector passed in
  `328.17 s`, and a post-merge completion-audit rerun on `main` passed in
  `308.91 s`. The standard gates passed as well: `lein test-proflog-fast` in
  `69.66 s` and `lein test-proflog-extended` in `195.92 s`.
  See [AAR-0045](docs/aar/AAR-0045-minsky-trace-performance.md),
  [AAR-0046](docs/aar/AAR-0046-combinatory-logic-turing-completeness.md),
  [Turing Completeness Example](worked-examples/turing-completeness.md), and
  [Combinatory Logic Example](worked-examples/combinatory-logic.md).

## 2026-05-07

- Completed
  [ADR-0044](docs/adr/ADR-0044-turing-completeness-demonstration.md) with a
  two-counter Minsky machine interpreter written through the ADR-0010 frontend.
  The opt-in `lein test-proflog-turing-completeness` suite passed in
  `68.64 s` after the long-probe smoke-test follow-up, while multi-step
  transfer recursion and open predecessor synthesis timeouts were recorded as
  runtime boundaries rather than promoted tests. See
  [AAR-0044](docs/aar/AAR-0044-turing-completeness-demonstration.md) and
  [Turing Completeness Example](worked-examples/turing-completeness.md).
- Revisited the ADR-0044 runtime boundaries with longer diagnostic probes.
  `halts-in-steps` for the three-step transfer eventually closed in `783.72 s`,
  and open predecessor synthesis returned answers in `645.66 s`. The direct
  ground three-step trace did not return before a controlled stop at about
  thirty minutes, and the five-step recursive transfer timed out after a
  `1800 s` wrapper. See
  [2026-05-07 ADR-0044 long Turing probes](docs/log/2026-05-07-adr44-long-turing-probes.md).
  The follow-up gates passed: `lein test-proflog-turing-completeness` in
  `68.64 s`, `lein test-proflog-fast` in `60.67 s`, and
  `lein test-proflog-extended` in `184.10 s`.
- Refreshed
  [Greenfield Implementation Tutorial](docs/GREENFIELD_IMPLEMENTATION_TUTORIAL.md)
  in light of ADR-0010 and the worked-example descent pass. The tutorial now
  presents the prefix frontend before raw backend constructors, documents
  `:=` helper inlining versus `|-` runtime relations, and distinguishes the
  ADR-0041 promoted constructor-recursive profile from the older diagnostic
  sidecar. Fresh fast and extended suite runtimes are recorded in
  [TEST_RUNTIME_BASELINE](docs/TEST_RUNTIME_BASELINE.md).
- Added the ADR-0010 frontend `answer-query` form so open answer examples can
  bind exported variables at the frontend layer and pass the resulting `:query`
  / `:answer-vars` pair to `proflog.answers` without manual backend nominal
  boilerplate.
- Streamlined that answer surface with `pf/run`, which binds answer variables in
  the evaluation form and delegates directly to `answers/query-answers`;
  `answer-query` remains the lower-level builder for diagnostics.
- Extended the `pf/run` presentation through the worked examples and tutorial,
  distinguishing ordinary answer evaluation from lower-level profile/diagnostic
  paths that still consume the translated `answer-query` pair. Added explanatory
  code comments to the frontend, AST, and language compiler layers for readers
  approaching the code from logic and theorem-proving rather than Clojure
  implementation details.

## 2026-05-06

- Accepted
  [ADR-0039](docs/adr/ADR-0039-kernel-level-group-verification.md) on branch
  `adr-0039-kernel-level-group-verification`. The branch will implement a
  generic proof-producing profiled finite equality-fragment kernel layer.
  Mandatory exit goals include `Z2` full group associativity success and
  non-group full associativity failure through the kernel path, plus a
  significant non-GV transition-system verification family. The spelling for
  this implementation track is `profiled`, matching the existing kernel
  layering terminology.
- Completed ADR-0039 with a generic `proflog.kernel.equality-fragment`
  component, proof-backed full GV associativity outcomes, and larger
  transition-system `delta` totality/determinism examples. The outcome is
  recorded in
  [AAR-0039](docs/aar/AAR-0039-kernel-level-group-verification.md).
- Completed ADR-0040 on branch `adr-0040-legacy-subsumption-parity` with a
  focused greenfield legacy-subsumption selector for the remaining GV,
  finite-domain, and Peano PA12-PA20 parity rows, each paired with an extended
  row. The closeout records passing runtimes, the corrected Peano recursion
  direction behind the `plus(3,4,7)` / `plus(4,3,7)` timing probe, and the
  remaining profile boundaries in
  [AAR-0040](docs/aar/AAR-0040-legacy-subsumption-parity.md) and
  [Legacy Subsumption Parity Examples](worked-examples/legacy-subsumption-parity.md).
- Updated
  [LEGACY_PROGRAM_PARITY_MATRIX](docs/LEGACY_PROGRAM_PARITY_MATRIX.md) after
  ADR-40 so the matrix no longer lists GV and FD as absent and distinguishes
  remaining operational/profile gaps from missing named-family coverage.
- Proposed
  [ADR-0041](docs/adr/ADR-0041-relational-constructor-recursive-profile.md) for
  promoting constructor-recursive descent into a relationally pure dispatched
  kernel profile, and
  [ADR-0042](docs/adr/ADR-0042-equality-fragment-status-consistency.md) for
  assessing and correcting the equality-fragment `:inconsistent` status
  behavior on universal finite-domain formulas.
- Completed ADR-0042 with a proof-variable requirement discipline in the
  equality-fragment profile. `warm-cool-disjoint` now reports `:succeeds` rather
  than `:inconsistent`; the promoted finite verifier suite still passes. See
  [AAR-0042](docs/aar/AAR-0042-equality-fragment-status-consistency.md).
- Completed ADR-0041 by adding the promoted
  `proflog.kernel.constructor-recursive-profile` answer profile over the ADR-35
  structural residual continuation engine. ADR-40 Peano answer rows now emit
  integrated `profiled constructor-recursive` records instead of calling the
  diagnostic sidecar directly. See
  [AAR-0041](docs/aar/AAR-0041-relational-constructor-recursive-profile.md).
- Added a query-status boundary characterization showing that `:inconsistent`
  is reachable when a valid compiled program is deliberately corrupted so a
  declared relation's body and negated body are the same closed constructor
  clash. The red pass failed against the ordinary compiled program with
  `actual: :succeeds`; the final focused test and `test-proflog-fast` timings
  are recorded in
  [TEST_RUNTIME_BASELINE](docs/TEST_RUNTIME_BASELINE.md).
- Completed ADR-0043 on branch `adr-0043-greenfield-doc-refresh` with a
  current greenfield source map, stale runtime/example cleanup, source-boundary
  docstring updates, and an AAR audit. Historical list and GV probe records now
  stay visible without being presented as current capability boundaries. See
  [AAR-0043](docs/aar/AAR-0043-greenfield-documentation-refresh.md).
- Enriched the worked-example corpus on branch
  `adr-0010-dsl-quickstart-docs` with a shared frontend-to-kernel descent
  reference and per-example source/backend/kernel notes. That pass initially
  recorded the open-answer query-binder gap; the 2026-05-07 ADR-0010 addendum
  closes it with `pf/answer-query` and the streamlined `pf/run` evaluator.
  Fresh `test-proflog-fast` and `test-proflog-extended` runtimes are recorded
  in [TEST_RUNTIME_BASELINE](docs/TEST_RUNTIME_BASELINE.md).

## 2026-05-05

- Closed ADR-0036 and ADR-0037 on branch
  `adr-0037-core-logic-minikanren-enhancements`. The closeout keeps production
  `kernel-support/step-fuelo` on finite-domain host integers, retains the
  relational arithmetic fuel adapter only as an opt-in/probe candidate, closes
  raw `core.logic/tabled` replacement as a non-replacement, and records the
  outcomes in
  [AAR-0036](docs/aar/AAR-0036-speculative-relational-arithmetic-and-tabling.md)
  and
  [AAR-0037](docs/aar/AAR-0037-core-logic-minikanren-enhancements.md).
- Accepted
  [ADR-0038](docs/adr/ADR-0038-fitting-program-kernel-evaluation.md) as the
  next development direction: evaluate deep Fitting Proflog programs in
  greenfield through the core proof kernel after source translation, without
  host-side semantic computation or named overlays in the promoted path.
- Completed ADR-0038 with `proflog.fitting-programs` and focused tests that
  disable the hard-family overlay and constructor-recursive sidecar while
  evaluating P1, P2, move-warning, finite-domain, list-family, and
  group-verifier-frontier examples. The outcome is recorded in
  [AAR-0038](docs/aar/AAR-0038-fitting-program-kernel-evaluation.md).

## 2026-05-03

- Continued ADR-0035 Track B on branch `adr-0035-track-b-guard-prefilter` with
  a relational guard-prefilter for raw live-state structural continuation.
  `proflog.answer-overlay/prefilter-structural-guardso` now gates guarded
  alternatives before recursive descent, saturating equality guards through
  `equality/unify-termo`, preserving proof-variable binding discipline and
  saved-disequality stability, and accepting only rigid constructor
  disequality guards. Focused tests use a generic sentinel recursive relation
  to show that an impossible guarded alternative is filtered before its calls
  are opened while a later viable alternative remains available. Longer note:
  [ADR-0035 Track B Guard Prefiltering](docs/log/2026-05-03-adr35-track-b-guard-prefilter.md).
- Continued ADR-0035 Track C on branch
  `adr-0035-track-c-structural-priority` with a generic structural residual
  priority selector. `proflog.answer-overlay/prioritize-structural-residual-frontiero`
  preserves an already demanded frontier head, otherwise promotes the first
  constructor-demanded negative residual ahead of less informative symbolic
  residuals without dispatching on predicate or constructor names. The
  scheduler uses a soft cut so raw export is only considered after prioritized
  continuation fails. Longer note:
  [ADR-0035 Track C Structural Priority](docs/log/2026-05-03-adr35-track-c-structural-priority.md).
- Integrated ADR-0035 Tracks A, B, and C on
  `adr-0035-relational-residual-continuation` in the required order. The merged
  scheduler now combines independent continuation fuel, guard prefiltering, and
  structural priority selection. Focused A/B/C checks, `proflog.answers-test`
  plus the guard-prefilter test namespace, `test-proflog-fast`,
  `test-proflog-constructor-recursive`, `proflog.synthesis-modes-test`,
  `proflog.list-kernel-matrix-test`, and the three carried reverse matrix
  probes all passed.
- Continued ADR-0035 Track D on branch
  `adr-0035-track-d-visited-continuation` with an active-call visited table in
  the fast residual continuation state. Recursive object-language ground calls
  are keyed by a canonical walked atom. Open symbolic calls are deliberately
  not tabled, because reverse/append continuation can revisit the same open
  shape while still making progress through surrounding substitutions. The
  continuation rejects active reentry for the same ground call key, but removes
  the key after success so later duplicate calls in the same sequence still
  close. Longer note:
  [ADR-0035 Track D Visited Continuation](docs/log/2026-05-03-adr35-track-d-visited-continuation.md).
- While running the full project suites for the Track D proof-search change,
  refreshed stale extended-suite fuel budgets for deeper recursive parity and
  Nim probes. The failing probes were semantic positives/negatives that still
  closed with slightly larger bounded fuel, not answer-overlay regressions.

## 2026-05-01

- Accepted [ADR-0032](docs/adr/ADR-0032-core-logic-performance.md) on branch
  `adr-0032-core-logic-performance`. ADR-0032 carries forward ADR-0031's still
  failing ordinary/raw reverse and synthesis rows, but moves the next experiment
  below Proflog into generic `core.logic` host performance and deployment work.
  The initial research and deployment design is recorded in
  [Core.logic Performance Research and Design](docs/log/2026-05-01-core-logic-performance-research-design.md).
- Added a runtime `core.logic` host probe and a published-upgrade Leiningen
  profile for `org.clojure/core.logic` 1.1.1. The upgrade profile is compatible
  with the focused suites and is modestly faster on the carried raw matrix rows,
  but it does not close any carried reverse or synthesis target. Longer note:
  [Core.logic 1.1.1 Upgrade Probe](docs/log/2026-05-01-core-logic-1-1-1-upgrade-probe.md).
- Added a verified source-overlay deployment lane for local `core.logic` host
  patches. The `+core-logic-source-overlay` profile resolves
  `clojure/core/logic.clj` to `vendor/core.logic-1.1.1/src`, reports an
  ADR-0032 marker var, and passes the host, constructor-recursive, and fast
  Proflog suites. Longer note:
  [Core.logic Source Overlay Deployment](docs/log/2026-05-01-core-logic-source-overlay.md).
- Tested and rejected a tiny generic `core.logic/unify` fast path that returned
  immediately when both walked terms were identical. The patch was compatible
  with focused suites, but timing was mixed and it did not close any carried
  matrix target, so it was reverted. Longer note:
  [Core.logic Unify Identical-After-Walk Probe](docs/log/2026-05-01-core-logic-unify-identical-probe.md).
- Tested and rejected a generic `ISeq` walk structural-sharing patch in the
  source overlay. It passed focused suites but slowed two of three carried rows
  and closed none, so it was reverted. Longer note:
  [Core.logic ISeq Walk Sharing Probe](docs/log/2026-05-01-core-logic-iseq-walk-probe.md).
- Compared the pinned 1.0.1 JVM source with the published 1.1.1 JVM source and
  found no implementation diff in the reviewed files beyond Proflog's overlay
  marker. The 1.1.1 artifact updates POM metadata, but Proflog still runs
  Clojure 1.11.1 in all ADR-0032 profiles. Longer note:
  [Core.logic 1.0.1 vs 1.1.1 Source Comparison](docs/log/2026-05-01-core-logic-1-0-1-1-1-source-comparison.md).
- Probed `core.logic` tabling/reification internals before patching them. The
  carried rows allocate the ordinary tabled-capable substitution, but they do
  not exercise `AnswerCache`, `reuse`, `subunify`, tabled reification, or
  suspended streams. No production host patch was retained. Longer note:
  [Core.logic Tabling/Reification Probe](docs/log/2026-05-01-core-logic-tabling-reification-probe.md).
- Tested and rejected batched `run-constraints*` dispatch across changed
  variables. It passed focused compatibility tests, but it did not close carried
  rows and materially slowed one of them. Longer note:
  [Core.logic Constraint Run Batch Probe](docs/log/2026-05-01-core-logic-constraint-run-batch-probe.md).
- Added a bounded Proflog-side `core.logic` count probe. The carried
  `reverse-input-flat` row shows counted calls dominated by `walk*` /
  reification and unification, with tabling unused. Longer note:
  [Core.logic Count Probe](docs/log/2026-05-01-core-logic-count-probe.md).
- Tested and rejected two additional small stream/walk allocation patches:
  `Choice.take*` lazy-tail simplification and `LCons` walk structural sharing.
  Both preserved answer shape and were slower on the carried rows. Longer note:
  [Core.logic Stream/Walk Negative Probe](docs/log/2026-05-01-core-logic-stream-walk-negative-probe.md).
- Ran a diagnostic no-occurs-check source-overlay experiment after the count
  probe showed high `occurs-check` volume. It was somewhat faster on carried
  rows but still closed none, so no unsound production path was retained.
  Longer note:
  [Core.logic No Occurs-Check Diagnostic](docs/log/2026-05-01-core-logic-no-occurs-check-diagnostic.md).
- Logged the remaining generic `core.logic` optimization frontiers after the
  first wave of rejected micro-patches. ADR-0032 is not treating the host as
  exhausted; it is splitting vector-specialized unification and bounded
  walk/reification memoization into independent worktree experiments. Longer
  note:
  [Core.logic Remaining Optimization Frontiers](docs/log/2026-05-01-core-logic-remaining-frontiers.md).
- Evaluated the concurrent ADR-0032 vector-unification and walk/reify-memo
  workers. Both were rejected as implementation merge candidates: the vector
  path was generic and exercised but did not improve carried rows, and the
  walk/reify memo variants regressed runtime without closing targets. The main
  ADR-0032 branch retest kept host, constructor-recursive, fast, and CI-safe
  matrix checks green, while the three carried raw reverse rows and two
  synthesis-mode failures remain. Longer notes:
  [Concurrent Probe Evaluation](docs/log/2026-05-01-adr32-concurrent-core-logic-probe-evaluation.md),
  [Vector Unification Probe](docs/log/2026-05-01-core-logic-vector-unification-probe.md),
  and
  [Walk/Reify Memo Probe](docs/log/2026-05-01-core-logic-walk-reify-memo-probe.md).
- Added worked legacy/greenfield traces for the exact current ADR-32 failures.
  Legacy closes the three carried reverse shapes by letting bare host logic
  variables flow through ordinary `proveo`; greenfield's ordinary raw answer
  path still exports residual frontiers, even though the constructor-recursive
  sidecar closes those rows. The two synthesis failures are narrower:
  `jump(x, 0)` has the right ground set with a non-disequality residual, and
  `down(2, y)` has the right set in legacy order reversed. Longer note:
  [Legacy / Greenfield Failure Traces](docs/log/2026-05-01-legacy-greenfield-failure-traces.md).
- Logged design lessons from the legacy/greenfield traces. The next promising
  direction is answer-frontier repair: complete procedural residuals before
  export, preserve base-before-recursive ordering where appropriate, integrate
  constructor-recursive descent into the ordinary raw path, and keep
  structurally safe answer variables live through recursion rather than turning
  them into residual frontiers. Longer note:
  [Greenfield Lessons From Legacy Traces](docs/log/2026-05-01-greenfield-lessons-from-legacy-traces.md).
- Started [ADR-0033](docs/adr/ADR-0033-structural-answer-variable-recursion.md)
  on branch `adr-0033-structural-answer-variable-recursion`. ADR-0033 keeps
  ADR-0031's list-family goal but moves the next implementation strategy to
  structural answer-variable recursion in the greenfield raw answer path:
  structurally safe answer variables should remain live across recursive
  descent instead of becoming premature residual frontiers. Longer note:
  [Structural Answer-Variable Recursion Architecture](docs/log/2026-05-01-structural-answer-variable-recursion-architecture.md).
- Continued ADR-0033 with a generic structural residual-completion hook at the
  ordinary program answer export boundary. The focused carried rows now close
  through the ordinary raw matrix path, `proflog.synthesis-modes-test` passes,
  `test-proflog-constructor-recursive` and `test-proflog-fast` pass, and answer
  diagnostics still opt out to expose raw unresolved frontiers. Longer note:
  [ADR-33 Structural Completion Progress](docs/log/2026-05-01-adr33-structural-completion-progress.md).
- Added [Language Namespace Spec](docs/LANGUAGE_NAMESPACE_SPEC.md), a
  pedagogical specification of declaration normalization, validation,
  alpha-renaming, NNF compilation, compiled program views, guarded alternatives,
  demand ordering, and the public language/proof-kernel boundary.
- Intensified the list-family matrix after the ADR-33 closure. The default
  matrix now includes a multi-answer inverse append row, longer reverse input
  synthesis, deeper nested reverse output synthesis, and a longer partial
  reverse output row. A heavier length-4 inverse append stress row passes at
  higher raw limit. Longer note:
  [Intensified List-Family Matrix](docs/log/2026-05-01-list-family-intensified-matrix.md).
- Traced `reverse(r, [c,b,a])` through greenfield's ordinary raw answer path
  and through the legacy `cljtap.alphaleantap-ep` prover. Greenfield now closes
  the row by structurally completing the raw residual frontier
  `append(a_3, [a_1], [c,b,a])` plus `reverse(a_2, a_3)`, while legacy closes
  the analogous query through a direct `neg-proc-call` proof. Longer note:
  [Three-Element Reverse Input-Synthesis Trace](docs/log/2026-05-01-three-element-reverse-trace.md).
- Started [ADR-0034](docs/adr/ADR-0034-greenfield-implementation-tutorial.md)
  on branch `adr-0034-greenfield-implementation-tutorial-docs` and added
  [Greenfield Implementation Tutorial and Reference](docs/GREENFIELD_IMPLEMENTATION_TUTORIAL.md).
  This documentation-only ADR provides a whole-stack tutorial for the current
  greenfield implementation: AST/language/normalize/substitution, compilation,
  program calls, kernel/equality/support/proof state, query and answer
  surfaces, constructor-recursive residual settlement, diagnostics, probes,
  tests, and end-to-end data/proof-state movement.
- Logged the current status of constructor-recursive proof terms. Heavier
  list-family answer probes are proof-bearing internally, but the CLI reports
  answer summaries; the appended constructor-recursive settlement proofs are
  still prototype sidecar certificates rather than ordinary kernel proof terms.
  The deeper integration path is to specify and check those proof terms, assess
  them as a possible derived tableau macro-rule, move residual completion
  earlier into raw answer-state context, and then re-express guarded constructor
  descent as an ordinary proof-producing answer-overlay rule. Longer note:
  [Constructor-Recursive Proof Terms and Integration Path](docs/log/2026-05-01-constructor-recursive-proof-terms.md).
- Proposed [ADR-0035](docs/adr/ADR-0035-relational-residual-continuation.md)
  for option (2): replace the ordinary answer-path role of the Clojure
  constructor-recursive sidecar with relational structural residual
  continuation inside `proflog.answer-overlay`. The ADR keeps the sidecar as a
  temporary diagnostic/oracle, but its exit criteria require the promoted
  ADR-0033 rows to pass with sidecar settlement disabled and with ordinary
  answer-overlay proof evidence.
- Accepted ADR-0035 and started implementation with sidecar-disabled matrix
  regressions plus raw proof-shape checks. These tests make the semantic
  requirement explicit: promoted rows must close through relational
  answer-overlay proof search, not by post-export constructor-recursive
  settlement.
- Continued ADR-0035 with a sidecar-independent structural continuation
  scheduler in `proflog.answer-overlay` for ordinary answer search. The
  promoted rows pass with `constructor-recursive/settle-record` redefined to
  throw, public proof records carry compact
  `structural-residual-scheduler-continue` /
  `structural-residual-continuation` evidence instead of
  `constructor-recursive-*` tags, and diagnostics can still opt out to expose
  unresolved raw frontiers. The scheduler lives next to the answer-mode agenda
  machinery, runs before answer export while `sigma`, `neqs`, and residuals are
  still live, and leaves `proflog.kernel` readable and unchanged. The broader
  raw live-state `core.logic` enumerator remains a follow-up because direct use
  still reopens too much search; the scheduled current path is first-success
  and is not yet complete for programs whose residual frontier should enumerate
  multiple distinct completions for one answer record.
  Longer note:
  [ADR-0035 Sidecar-Independent Structural Continuation](docs/log/2026-05-01-adr35-sidecar-independent-continuation.md).

## 2026-04-30

- Continued ADR-0031 by compiling and executing guarded clause alternatives in
  both the ordinary kernel and raw answer overlay. The promoted matrix now
  closes longer flat/nested ground append and reverse rows plus representative
  raw append output/suffix/prefix and reverse output rows. Reverse input and
  full inverse split enumeration remain bounded-search follow-up work.
- Reassessed the remaining ADR-0031 brainstormed enhancements after a narrow
  adaptive call-order experiment only improved `reverse(r, [b,a])`. The
  adaptive ordering was reverted; stricter residual deferral and residual
  frontier re-settlement were also rejected after slowing or regressing the
  matrix without closing length-three reverse rows. Longer note:
  [ADR-0031 Experiment Reassessment](docs/log/2026-04-30-adr31-experiment-reassessment.md).
- Registered generic `core.logic` host-language performance work as a possible
  ADR-0031 avenue. This includes tableau-prover specific handling only if it
  stays generic across Proflog programs, as well as fully general-purpose
  improvements that would benefit arbitrary `core.logic` programs.
- Tightened that avenue with prerequisites: upstream `core.logic` research,
  review of the exact patched implementation, a revised dependency/deployment
  sequence that selects the patched artifact or source path, and a runtime
  verification step proving Proflog is not still using the default dependency.
- Integrated ADR-0031 negative probe notes from parallel branches: the generic
  demand-selector idea regressed answer ordering, the answer-continuation
  prototype slowed passing rows without closing reverse blockers, and
  answer-path tabling diagnostics showed duplicate exported records rather than
  repeated raw proof families. Longer notes:
  [Demand IR Probe](docs/log/2026-04-30-adr31-demand-ir-worker2-probe.md),
  [Answer Continuation Probe](docs/log/2026-04-30-adr31-answer-continuation-probe.md),
  and [Answer-Path Tabling Probe](docs/probe/2026-04-30-adr31-answer-tabling.md).
- Collated all five ADR-0031 parallel sub-agent reports, including the parked
  structural-descent prototype and the promising constructor-recursive sidecar
  prototype. Longer note:
  [ADR-0031 Parallel Sub-Agent Reports](docs/log/2026-04-30-adr31-parallel-subagent-reports.md).
- Evaluated the parallel ADR-0031 experiments and merged the fruitful
  constructor-recursive sidecar prototype into the branch. It is generic over
  guarded constructor-recursive programs and closes representative blocked
  reverse/input, nested reverse/output, and append inverse-split matrix rows
  through an opt-in proof layer. Structural descent was parked as useful input
  but not merged because it did not improve reverse rows and left synthesis-mode
  failures.
- Closed ADR-0031 with AAR-0031. The branch is complete enough to merge back to
  `master`, but not because all list-family criteria are satisfied: ordinary
  raw reverse input, nested reverse output, reverse partial-output-tail, and two
  `proflog.synthesis-modes-test` failures are carried forward explicitly to
  ADR-0032.

## 2026-04-29

- Logged the ADR-0031 brainstorm for making list-family proof search genuinely
  family-parametric. The adopted order starts at the source-to-IR boundary:
  compile guarded alternatives, expose them relationally, then use them for
  guard-first recursive descent and answer residual handling before adding
  heavier tabling. Longer note:
  [List-Family Kernel Generalization Brainstorm](docs/log/2026-04-29-list-family-generalization-brainstorm.md).
- Completed [ADR-0030](docs/adr/ADR-0030-relational-constructor-search.md) on
  branch `adr-0030-relational-constructor-search`. The raw constructor-recursive
  list targets now close through the ordinary kernel using generic rigid
  constructor disequality discharge and call-local guarded alternatives; a
  non-list Peano recursive control is included in the new focused selector.
  See [AAR-0030](docs/aar/AAR-0030-relational-constructor-search.md).
- Added a raw-kernel append/reverse matrix to distinguish ADR-0030's ground
  closure improvement from remaining reverse and partial synthesis gaps. The
  matrix shows two-step flat and nested ground cases passing, longer outer-list
  ground cases timing out, and raw answer-mode synthesis rows failing to
  produce closed targets within the tested bounds. Longer note:
  [List Kernel Test Matrix](docs/log/2026-04-29-list-kernel-test-matrix.md).
- Reassessed ADR-0030 after the raw matrix. The branch is technically closed,
  but its result is too narrow to satisfy the family-level goal: arbitrary
  proper lists should be handled by a measurable recursive proof discipline,
  not only by selected two-step examples. Accepted
  [ADR-0031](docs/adr/ADR-0031-list-family-kernel-generalization.md) on branch
  `adr-0031-list-family-kernel-generalization` to revisit the work against
  deeper forward, reverse, partial, flat, and nested matrix rows.
- Accepted [ADR-0030](docs/adr/ADR-0030-relational-constructor-search.md) on
  branch `adr-0030-relational-constructor-search`. The plan treats the
  legacy-passing raw list proofs as constructor-recursive kernel search
  failures and proposes generic, pure relational improvements: rigid
  constructor disequality discharge, structural agenda focusing, guarded
  procedure-call descent, and optional call-stack descent preference. Longer
  note:
  [List-Family Kernel Search Plan](docs/log/2026-04-29-list-family-kernel-search-plan.md).
- Completed [ADR-0029](docs/adr/ADR-0029-relational-fuel-purity.md) for
  relational fuel stepping in `kernel_support.clj`. `step-fuelo` is now a
  structural finite-domain relation over unbounded `nil` and bounded fuel
  steps, with direct fuel synthesis, open-fuel `proveo`, and open-fuel
  procedure-call synthesis regressions. The discussion and examples are
  recorded in
  [Step Fuel Relational Purity Gap](docs/log/2026-04-29-step-fuelo-relational-purity-gap.md).
  Follow-up list-family probes showed this purity repair does not make the
  legacy-passing raw `append([a,b], [c], [a,b,c])` or
  `reverse([a,b], [b,a])` proofs close within a 45 second slice; the result is
  recorded in [AAR-0029](docs/aar/AAR-0029-relational-fuel-purity.md).
- Completed [ADR-0028](docs/adr/ADR-0028-kernel-support-disequality-purity.md)
  for saved disequality maintenance purity in `kernel_support.clj`.
  `prune-contradictory-neqso` and `stable-neqso` are now structural, with
  reverse/open branch-state regressions in `proflog.kernel-test`; the preceding
  analysis and examples are recorded verbatim in
  [Kernel Support Disequality Purity Gap](docs/log/2026-04-29-kernel-support-disequality-purity-gap.md).
- Completed [ADR-0027](docs/adr/ADR-0027-transitive-relational-purity.md) for
  transitive relational purity. `subst-formulao` is now structural rather than
  projected, reverse/partial substitution preimage regressions pass, and
  [AAR-0027](docs/aar/AAR-0027-transitive-relational-purity.md) records the
  remaining recursive synthesis boundaries.
- Logged a broader transitive purity risk: `proflog.subst/subst-formulao` uses
  `core.logic/project`, and that relation is called throughout the kernel and
  answer overlay. This is the basis for
  [ADR-0027](docs/adr/ADR-0027-transitive-relational-purity.md). Longer note:
  [`subst-formulao` Transitive Purity Risk](docs/log/2026-04-29-subst-formulao-transitive-purity-risk.md).
- Recovered the ADR-0026 branch profiler from a `core.logic/project`-based
  classifier to structural relational goals. This is now documented as a
  reusable example for preserving and recovering relational purity:
  [Structural Profiler Purity Recovery](docs/log/2026-04-29-structural-profiler-purity-recovery.md).
- Completed [ADR-0026](docs/adr/ADR-0026-kernel-layer-interoperation.md) for
  proof-producing kernel layer interoperation. The full program kernel can now
  close purified compound residual branches through propositional or
  equality-free first-order `proveo` relations, and
  [AAR-0026](docs/aar/AAR-0026-kernel-layer-interoperation.md) records the
  proof-boundary and partial-synthesis constraints.
- Logged the tableau foreground/background literature relevant to kernel layer
  interoperation. The key architectural note is that delegated branch closure
  must remain proof-producing; kernel purity rules out opaque background
  oracles. Longer note:
  [Tableau Foreground/Background Lessons](docs/log/2026-04-29-tableau-foreground-background-lessons.md).
- Accepted [ADR-0026](docs/adr/ADR-0026-kernel-layer-interoperation.md) to
  implement branch-level interoperation between the full program kernel and the
  propositional / equality-free first-order layers.
- Introduced this development log as a central timeline and documentation spine.
  The immediate prompt was a discussion about how to keep optimized Pelletier
  kernel layers useful inside general Proflog program execution, rather than
  only at the top-level theorem entry point. Longer note:
  [Kernel Layer Interoperation](docs/log/2026-04-29-kernel-layer-interoperation.md).
- Added a characterization test for the current layering gap on branch
  `pelletier-program-layering-gap-test`: two Pelletier subproblem relations
  close through theorem dispatch individually, but an aggregate Proflog program
  query remains on the full program kernel and does not reach the optimized
  first-order layer. Commit: `0522179`. Test:
  [pelletier_layering_test.clj](test/proflog/pelletier_layering_test.clj).
- Completed ADR-0025 and AAR-0025 for the lean Pelletier search policy. All
  Pelletier Problems 1-46 are now in the passing catalog without problem-id
  dispatch. See [ADR-0025](docs/adr/ADR-0025-pelletier-lean-search-policy.md),
  [AAR-0025](docs/aar/AAR-0025-pelletier-lean-search-policy.md), and
  [Pelletier Lean Search Policy Comparison](docs/PELLETIER_LEAN_SEARCH_POLICY_COMPARISON.md).

## 2026-04-28

- Completed the profiled-kernel sequence that made Pelletier progress possible.
  ADR-0023 introduced entry-only propositional dispatch, ADR-0024 introduced an
  equality-free first-order theorem layer and comparison report, and ADR-0025
  followed with alphaleanTAP-shaped search and narrow host-side Skolemization.
  See [ADR-0023](docs/adr/ADR-0023-profiled-kernel-layers.md),
  [ADR-0024](docs/adr/ADR-0024-pelletier-first-order-performance.md), and
  [Pelletier First-Order Comparison](docs/PELLETIER_FIRST_ORDER_COMPARISON.md).
- Memory was updated with detailed working-context records for ADR-0023 through
  ADR-0025. See [MEMORY.md](MEMORY.md).

## 2026-04-27

- Ported and classified the upstream Pelletier benchmark suite in greenfield.
  This created the baseline for later profiled-kernel work: passing problems,
  too-slow problems, and one propositional search problem that motivated the
  propositional layer. See
  [ADR-0022](docs/adr/ADR-0022-pelletier-problems.md),
  [AAR-0022](docs/aar/AAR-0022-pelletier-problems.md), and
  [worked-examples/pelletier-problems.md](worked-examples/pelletier-problems.md).
- Closed the gamma-candidate purity and search-repair sequence. ADR-0019 added
  bounded closed-term gamma candidates, ADR-0020 moved candidate choice outside
  the kernel path, and ADR-0021 repaired the regressions exposed by that
  boundary. See [ADR-0019](docs/adr/ADR-0019-closed-term-gamma-instantiation.md),
  [ADR-0020](docs/adr/ADR-0020-pure-gamma-candidate-boundary.md), and
  [ADR-0021](docs/adr/ADR-0021-gamma-search-regression-repair.md).
- Added and completed ADR-0018 around existential disequality witnesses,
  preserving the boundary between proof-time parameters and exportable
  object-language answers. See
  [ADR-0018](docs/adr/ADR-0018-existential-disequality-witnesses.md).

## 2026-04-26

- Reconstructed ADR scheduling, then completed the fair-agenda, micro-fuel, and
  relational tabling line of work. These records are the main source for the
  current proof-state scheduling and memoization story. See
  [ADR-0016](docs/adr/ADR-0016-fair-agenda-and-micro-fuel.md) and
  [ADR-0017](docs/adr/ADR-0017-relational-tabling-and-canonical-state.md).
- Recovered the hard-family overlay and legacy parity explorations that kept
  unresolved `GV` and related families visible while the greenfield kernel
  changed underneath. See [AAR-0014](docs/aar/AAR-0014-generic-legacy-evaluation.md).

## 2026-04-25

- Completed ADR-0015, extracting answer-oriented execution from the pure kernel
  into an overlay while leaving common proof mechanics in `kernel_support`.
  This became the project’s explicit pure-core / overlay boundary. See
  [ADR-0015](docs/adr/ADR-0015-answer-overlay-extraction.md),
  [AAR-0015](docs/aar/AAR-0015-answer-overlay-extraction.md), and
  [HANDOFF-2026-04-24-ADR-0015](docs/HANDOFF-2026-04-24-ADR-0015.md).

## 2026-04-24

- Concentrated on generic legacy evaluation, raw-kernel probes, group-verifier
  probes, and answer-performance boundaries. The project started treating
  "which layer first has the answer?" as a central diagnostic question. See
  [ADR-0014](docs/adr/ADR-0014-generic-legacy-evaluation.md),
  [AAR-0014](docs/aar/AAR-0014-generic-legacy-evaluation.md), and
  [LESSONS.md](LESSONS.md).
- Completed ADR-0013, improving relational answer performance while preserving
  the explicit closed-answer parity mode introduced by ADR-0012. See
  [ADR-0013](docs/adr/ADR-0013-relational-answer-performance.md).

## 2026-04-23

- Advanced open-answer relationality and closed-answer parity planning.
  ADR-0011 moved default open-answer search toward staged kernel descent, while
  ADR-0012 isolated long-running closed-answer parity work from the generic
  symbolic API. See [ADR-0011](docs/adr/ADR-0011-open-answer-relationality.md)
  and [ADR-0012](docs/adr/ADR-0012-closed-answer-parity-mode.md).

## 2026-04-22

- Deepened equality, procedure-call, list, quantified-program, synthesis, and
  documentation coverage. Several lessons from this date remain important:
  equality and disequality are operationally asymmetric; stale disequalities
  must be pruned after binding-producing steps; and first-order comparator
  relations should not be treated as higher-order predicate arguments. See
  [LESSONS.md](LESSONS.md).
- Added ADR-0010 for frontend inlining translation after sortedness and
  comparator examples made the language boundary explicit. See
  [ADR-0010](docs/adr/ADR-0010-frontend-inlining-translation.md).

## 2026-04-21

- Added ADR-0009 parity matrix work, worked examples, integration-family
  coverage, quantified clause-body executability, reverse program synthesis
  regressions, and baseline list program regressions. See
  [ADR-0009](docs/adr/ADR-0009-legacy-program-closure.md),
  [LEGACY_PROGRAM_PARITY_MATRIX](docs/LEGACY_PROGRAM_PARITY_MATRIX.md), and
  [worked-examples/README.md](worked-examples/README.md).

## 2026-04-20

- Recorded deeper Nim and `win(x)` probe results, then added symbolic answer
  export and synthesis coverage. These entries bridged ADR-0007 query
  remediation and the later answer-surface ADRs. See
  [ADR-0007](docs/adr/ADR-0007-nim-correctness-and-query-bounds.md) and
  [TEST_RUNTIME_BASELINE](docs/TEST_RUNTIME_BASELINE.md).

## 2026-04-18 to 2026-04-19

- Bootstrapped the greenfield process: execution plan, ADR/AAR stacks, branch
  policy, semantic boundary, pure relational kernel, equality kernel, procedure
  calls, query API, and query remediation baseline. See
  [EXECUTION_PLAN](docs/EXECUTION_PLAN.md),
  [ADR-0001](docs/adr/ADR-0001-greenfield-foundation.md),
  [ADR-0002](docs/adr/ADR-0002-language-and-semantic-boundary.md),
  [ADR-0003](docs/adr/ADR-0003-pure-relational-kernel.md),
  [ADR-0004](docs/adr/ADR-0004-equality-kernel.md),
  [ADR-0005](docs/adr/ADR-0005-procedure-calls-and-query-api.md), and
  [ADR-0007](docs/adr/ADR-0007-nim-correctness-and-query-bounds.md).
- Split slow recursive, reverse, and partial-synthesis regressions into the
  extended suite, a practice later codified in
  [development-practices.md](development-practices.md).

## 2026-04-03 to 2026-04-06

- Ran a performance-lab phase on the legacy/experimental implementation:
  forward execution, dual-engine dispatch, symbolic-to-fast cutover, lemma
  threading through cutover, equality closure optimization, neg-call caching,
  substitution caching, and non-default `Z4` group-verifier coverage. These
  were productive experiments but not the final greenfield architecture.

## 2026-03-13 to 2026-03-16

- Explored semantic variants and performance ideas in the experimental prover:
  closed-world / Clark-completion notes, L-ground guard justification,
  gamma-budgeted iterative deepening, lemma reuse between beta siblings, and
  group-verifier progress. See [SEMANTIC_VARIANTS](docs/SEMANTIC_VARIANTS.md).

## 2026-02-27 to 2026-03-08

- Initial experimental αleanTAP-E and αleanTAP-EP implementation work: equality,
  delta/existential handling, procedure-call rules, paramodulated closure,
  Nim/list/Peano program tests, groundness guards, equality-triggered procedure
  calls, and adversarial review cases. This phase remains reference material
  for greenfield comparisons rather than the authoritative design.
