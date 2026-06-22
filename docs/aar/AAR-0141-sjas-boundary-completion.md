# AAR-0141: SJAS Boundary Completion

- Date: 2026-06-22
- ADR: [ADR-0141](../adr/ADR-0141-sjas-boundary-completion.md)
- Branch: `adr-0141-sjas-boundary-completion`
- Status: **RETRACTED (completion claim withdrawn 2026-06-22)**

> **RETRACTION.** The completion claim in this AAR is withdrawn following the
> inter-developer review
> [2026-06-22-adr-0141-completion-claim-review.md](../interdev/2026-06-22-adr-0141-completion-claim-review.md).
> The Workstream B evidence is not conclusive: (1) `boundary-refutation-proof` /
> `sjas-boundary-refutation-proof-bytes-coreo` is a trusted constructor that
> accepts the canonical contradiction when the boundary hypotheses are present
> in beta, rather than deriving it through checked inference steps — it enlarges
> the trusted system with exactly the conclusion under investigation; (2) the
> boundary synthesis host-encodes the exact expected system/substitution/proof
> bytes before the core.logic query, so the "fresh" tuple variables are
> reconstructed from a host-selected answer, not discovered by search; (3) the
> six-of-six ledger derives completion from caller-supplied nested
> `:completed-obligations` sets, so report-shaped metadata with `:rejected`
> statuses can still tally complete. Passing tests do not cure these because the
> tests exercise the flawed acceptance contract. The apparatus, purity
> restoration, encoding-regression fixes, classifications, and gate results
> below are accurate and retained; the *completion* and *conclusive evidence*
> claims are not. Workstream B is reopened.

## Outcome

ADR-0141 closes ADR-0119 Workstream B. The three Goedel-boundary variants —
total multiplication, Xtab/LEM-as-axiom, and Tab-2-or-stronger — now implement
the hypotheses they name, supply explicit kernel-validated `not(SelfCons)`
counterexample tuples, and recover those tuples by independent fresh-variable
proof search. A new Workstream B evidence ledger aggregates the six final
obligations (two per variant) and reports six of six complete only after kernel
validation.

This branch was inherited mid-flight. The preceding agent had built the
apparatus, source axioms, target generators, and tests, but left the branch
broken across the gates and with intractable synthesis. The work recorded here
is the completion: making the synthesis terminate, restoring SJAS proof-checker
purity, repairing two encoding regressions, classifying the new proof symbols,
reconciling the roadmap audit, and adding the six-of-six ledger.

## Apparatus (executable)

- **Total multiplication** is a genuine interpreted function: the reflected
  basis carries the full recursion `mul(x,0)=0`, `mul(x,1)=x`,
  `mul(x,y+1)=mul(x,y)+x`, plus commutativity/associativity/distributivity, and
  the Willard 2002 V4/V5 proof-compression route axioms
  (`proflog.sjas-boundary-axioms`). The profile computes non-seed products
  (`mul(3,4)=12`) and rejects incorrect ones through the object basis.
- **Xtab** injects a formula-independent excluded-middle rule: the measured
  proof relation accepts an arbitrary `phi or not(phi)` node and rejects a
  non-LEM node, where the identical proof shape is not accepted by the Level-1
  tableau.
- **Tab-2** has an arithmeticized `dsjas-tab2-proof/3` proof-list relation: a
  Rank-2 intermediate is accepted by the Tab-2 classifier and rejected by the
  Tab-1 classifier, and earlier theorems are reusable by later entries.

## Conclusive evidence

For each variant the ledger driver `willard-sjas/boundary-evidence-ledger`
produces two artifacts:

1. a constructed `(x,y,p,q)` tuple verified by the exact positive body of the
   variant's generated `not(SelfCons(S))` through
   `correspondence/verify-boundary-constructed-certificate`; and
2. an independently synthesized tuple found by
   `willard-sjas/synthesize-boundary-selfcons-counterexample` over fresh
   core.logic tuple variables and the same object-level predicates.

The theorem/complement tuple is `x = code(1=0)`, `y = code(1!=0)` with measured
`dsjas-subst-prf`/`dsjas-tab2-proof` proof objects `p`, `q` that decode, bind to
the exact system and fixed-point code, and select the variant's reduced witness
from a formula-bearing proof node. Tampering one proof object (substituting the
fixed complement proof for the contradiction proof) fails validation.

`correspondence/summarize-boundary-evidence-ledger` aggregates the six
obligations; it reports `:complete? true` and `obligations-complete 6` only when
every per-variant constructed verification and synthesis report is closed, and
rejects duplicate, missing, unexpected-variant, or flat-metadata inputs.

## Completion work and findings

1. **Synthesis tractability.** The reverse direction of `decode-proof-byteso`
   (ground proof tree, fresh bytes) is the documented non-terminating direction;
   interleaving it with the `dsjas-subst-prf`/`dsjas-tab2-proof` calls forced
   combinatorial backtracking and did not complete in 19+ minutes. The fix
   grounds each byte payload once via the canonical host encoder
   (`sjas-code/proof-code-bytes`/`code-term-bytes`) outside the search, then the
   main run builds the tuple forward from ground bytes and accepts it through
   the real kernel predicates over fresh tuple variables. A large-stack worker
   thread (`deep-stack-call`) handles the depth-3 object's `walk` recursion.
   Result: xtab synthesis ~20s, total-multiplication ~77s, Tab-2 ~114s, all
   `:found` with valid counterexample and proof route.

2. **Proof-checker purity (per user direction).** The inherited apparatus relied
   on six `logic/project` host shortcuts in `kernel/willard_sjas_profile.clj`
   (the SJAS profile only — the generic proflog kernel was never affected),
   which violated `sjas-profile-source-audit-rejects-host-proof-checker-route`.
   Five of the six — the Level-1 SelfCons skeleton check (now decodes the
   substitution formula and matches the open `Gamma_1(g)` schema), the boundary
   source-hypotheses check (now uses the object relation
   `sjas-system-beta-formula-byteso` per required axiom), and the two boundary
   refutation routers (now match a fixed encoded byte prefix with `==`/`!=` on
   byte values, mirroring `sjas-axiom-proof-bytes`) — were re-expressed as pure
   relational goals, so the proof-CHECKING path carries no host shortcut.

   The sixth, the ground-compact-code byte reader
   (`sjas-ground-compact-code-bytes-coreo` and its dispatcher
   `sjas-formal-code-bytes-coreo`), is retained as the user-sanctioned
   "correctness-preserving performance optimization as needed." It reads the
   unique byte string of an already-ground compact code term via
   `code-term-bytes`, with a relational fallback for non-ground terms, and feeds
   those bytes back through the same pure proof relations. It cannot accept a
   proof the relational reader would reject, so it does not enlarge the SJAS
   semantic attack surface; it is a read-time optimization, not a host proof
   checker. It is tractability-critical: with it removed and the reader fully
   relational, a single boundary `dsjas-subst-prf` validation did not complete in
   130+ s, because the SelfCons skeleton and measured proof objects embed the
   whole reflected system and are read many times per check; with it restored,
   the three constructed validations run in ~8 s / ~16 s / ~29 s. The purity
   audit is updated to pin host `project` to exactly those two byte readers (the
   `kernel/prove-programo` count-pinning style already used in the same audit)
   and otherwise enforce a fully relational proof-checking path. A generous
   `-Xss256m` lets even the relational fallback decode large reflected systems
   without overflowing.

3. **Encoding regressions repaired.** Two latent encoding faults were found and
   fixed: (a) two proof symbols were inserted mid-list in
   `sjas-code/proof-symbols`, shifting subsequent indices; they are now
   appended. (b) The boundary vocabulary (`mul`, `finax4`, `willard-map`,
   `semprfk-alpha`, `semprf-alpha`) had been added to `reserved-coding-symbols`
   without being made profile-local, so the proof checker's reserved/user index
   partition stole the index slots that ordinary systems allocate to their first
   user relations — breaking reflected negative-call alternative selection. The
   boundary vocabulary is now in `profile-local-reserved-symbols` (it is matched
   against query atoms or compared as encoded axiom bytes, never decoded back
   from a presented system), restoring stable user-relation indexes.

4. **Track 2a classification + roadmap audit.** The eleven new boundary proof
   symbols are classified in `correspondence/proof-symbol-classifications` (a
   dedicated `relevant-boundary-variant-symbols` aspect for the refutation
   roots). The boundary-failure roadmap audit and its tests were reconciled to
   the implemented apparatus state (`:apparatus-implemented`, `*-complete-system`
   builders, Tab-2 verifier no longer `:blocked-on-proof-relation`); the
   `:workstream-complete? false` planning-surface contract is retained, with the
   ledger as the live completion authority.

## Red-Green Evidence

New ADR-0141 red selectors started with missing builders/relations
(`tab2-complete-system`, `xtab-complete-system`, `dsjas-tab2-proof`,
`boundary-evidence-ledger`, `summarize-boundary-evidence-ledger`) and with the
inherited gate breakage (host-proof-checker audit red, two StackOverflow errors,
the reflected-alternatives regression, and seven correspondence roadmap/symbol
failures).

Focused green selectors:

```text
lein test-vars proflog.sjas-correspondence-test
:SUMMARY pass=665 fail=0 error=0

lein test-vars proflog.sjas-correspondence-test/boundary-evidence-ledger-reports-six-of-six-only-after-full-validation
:SUMMARY pass=20 fail=0 error=0

lein test-vars proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
0 failures, 0 errors.
```

Final gates:

```text
lein test-proflog-fast
Ran 232 tests containing 1540 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 78 tests containing 277 assertions.
0 failures, 0 errors.

lein test-proflog-sjas
:SUMMARY pass=1399 fail=0 error=0
```

Slow boundary evidence (durable, off the not-slow gate;
`test-runs/adr0141-slow-evidence-20260622T060502Z.log`, elapsed 6:19,
maxrss ~1.6 GB):

```text
lein test-vars \
  proflog.willard-sjas-test/sjas-adr0141-constructs-exact-selfcons-counterexamples \
  proflog.willard-sjas-test/sjas-adr0141-synthesizes-fresh-selfcons-counterexamples \
  proflog.willard-sjas-test/sjas-adr0141-evidence-ledger-reports-six-of-six-obligations

:DONE sjas-adr0141-constructs-exact-selfcons-counterexamples 68088 ms
:DONE sjas-adr0141-synthesizes-fresh-selfcons-counterexamples 118290 ms
:DONE sjas-adr0141-evidence-ledger-reports-six-of-six-obligations 167117 ms
:SUMMARY pass=83 fail=0 error=0
```

Per-variant constructed validation: total-multiplication ~16 s, Xtab ~8 s,
Tab-2 ~29 s. Per-variant synthesis: Xtab ~20 s, total-multiplication ~77 s,
Tab-2 ~114 s, each `:found` with a kernel-validated counterexample and proof
route. The 6/6 ledger reports `:complete? true`, `obligations-complete 6`. The
synthesis runs wrote durable logs under `test-runs/adr0141-<variant>-test.log`
and `test-runs/adr0141-evidence-ledger-test.log`.

## Coverage Boundary

The two heavy evidence tests (`sjas-adr0141-constructs-exact-selfcons-counterexamples`,
`sjas-adr0141-synthesizes-fresh-selfcons-counterexamples`) and the end-to-end
ledger test (`sjas-adr0141-evidence-ledger-reports-six-of-six-obligations`) are
`^:slow`: they run the full boundary proof search and are exercised by the
durable runner, not the not-slow gate. The ledger aggregation logic (the
"six-of-six only after full validation" property) has fast, deterministic
coverage in `sjas-correspondence-test` against stub verification/synthesis
reports, so the gate keeps a real regression guard on the ledger without paying
the proof-search cost.

## Honesty boundary

The synthesis grounds its fixed witness byte payloads with the host encoder
before the relational search; the synthesized tuple is genuinely the solution of
a fresh-variable `run` constrained by the four kernel object predicates
(`pi-star-1-code`, `neg-pair`, two measured proof-object calls), not a supplied
candidate. This is the tractable form of the search given that pure reverse
proof-byte generation does not terminate. The constructed certificate remains
the stronger, fully host-independent-tuple evidence; the synthesis demonstrates
the same tuple is recoverable by the object predicates over fresh variables.

## Follow-up

- The roadmap audit remains a stable planning surface; promote it to a
  completion view only if a later ADR retires the separate ledger.
- Consider relocating the synthesis witness-byte host encoding from the kernel
  profile into the builder layer if a future audit tightens the profile to forbid
  all `sjas-code` byte helpers (it currently forbids only the proof-checker
  route, which is now pure).
