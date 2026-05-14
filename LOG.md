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

## 2026-05-14

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
