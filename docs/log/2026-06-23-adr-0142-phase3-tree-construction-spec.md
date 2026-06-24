# ADR-0142 Phase 3: cut-free tableau tree construction spec

Date: 2026-06-23
Plan: `~/.claude/plans/velvet-conjuring-frost.md`
Builds on: [Phase 0](2026-06-23-adr-0142-phase0-checker-characterization.md)

Phase 3 constructs the cut-free tableau trees that the ordinary SJAS checker
validates, completing the Theorem 2.3 closure. This note specifies the
construction precisely, from the checker contract established this session.

## Checker contract (verified)

- Entry: `proflog.kernel.willard-sjas-profile/structural-proof-valid?
  [prog system-code target proof fuel]` (or the raw
  `sjas-proof-check-programo [prog system-code target fuel proof]`).
- `target` is an AST formula; for `alpha |- Xi` it is the proof-predicate branch
  `(and AxiomConj(alpha) (not Xi))` (the relation docstring), though a bare
  logically-closing `target` is also accepted (e.g. a complementary-literal
  conjunction or `(exists b false)`).
- `proof` is a formula-bearing tree: each node is `(byte-count formula-bytes...
  child...)` (narrow, <64 bytes) or `((formula-bytes...) child...)` (wide). The
  checker INFERS the tableau rule from the node formula + branch state; there is
  no rule tag and no cut tag.
- Builders (test helpers, to be promoted into a construction module):
  `structural-flex-tableau-node` / `canonical-structural-flex-tableau-node`
  (auto-select narrow vs wide). Encode formulas with
  `formula-code-bytes` / `canonical-formula-code-bytes`.

**Verified this session (REPL):** a constructed complementary-literal clash
`node[(and (wff 0) (not (wff 0)))] -> node[(wff 0)] -> node[(not (wff 0))]`
validates over a demo system; the existing
`sjas-proof-check-accepts-formula-bearing-quantifier-expansions` (18 assertions)
validates delta/gamma/once-univ nodes. So all rules the diagonal needs are
construct-and-checkable.

**Lesson (construction discipline):** hand-built trees are fiddly — a small
difference in a binder's canonical index or in the generating system's coding
context silently breaks acceptance (observed while replicating a passing test in
the REPL). Construction must be test-driven against the checker, node by node,
not ad-hoc; child nodes that mention a parent binder's variable MUST use the
`canonical-*` node builders (runtime noms are not stable proof-code bytes).

## Per-step tree shapes

`alpha` = the multiplication-total system; `Dk = Gamma(nbar)`;
`D* = forall y z. not SemPrf^k(code(Dk), y, z)`. Conditions A (SelfCons) and C
(V3) are reflected beta; step 2 `Subst(nbar, code(Dk))` is already checker-
accepted via `subst-code`.

1. **Step 1 (Eq 6): `alpha |- D*`.** target `(and AxiomConj (not D*))`. Tree:
   `node[target] -> node[AxiomConj] (unexpanded) -> node[(not D*)] ...`. `(not
   D*) = exists y z. SemPrf^k(code(Dk),y,z)`; delta gives params `y0,z0`; the
   leaf `SemPrf^k(code(Dk),y0,z0)` plus B's instance yields `SemPrf(BOT,x0)`,
   which clashes with A's `forall p. not SemPrf(BOT,p)` instantiated at `x0`.
   (Simple modus-tollens shape; Willard calls it "immediate".)
2. **Step 3 (Eq 8): `alpha |- (Dk == D*)`.** From `Subst(nbar,code(Dk))` (step 2)
   and V3 (C): V3 forces the only `h` with `Subst(nbar,h)` to be `code(Dk)`, so
   `Dk`'s matrix collapses to `D*`'s. A finite propositional+gamma tree.
3. **Step 4 (Eq 9): `alpha |- Dk`.** From `D*` (step 1) and `Dk == D*` (step 3),
   a trivial equivalence-substitution tree.
4. **Condition B (Eq 16) instance:** `alpha |- (exists y z SemPrf^k(code(Dk),
   y,z)) => exists x SemPrf(BOT,x)`, built from the V5 axiom instance +
   `Map(alpha,k,code(Dk))` (the locator, = `Subst(nbar,code(Dk))`). Feeds step 1.
5. **Step 5 (Eq 11): `alpha |- not Dk`.** `not Dk = exists h y z. Subst(nbar,h)
   /\\ SemPrf^k(h,y,z)`. Witnesses: `h = code(Dk)`, `y = p` (= the step-4 proof
   of `Dk`), `z = (pow 2 (p+1))`. The `Subst` conjunct closes via `subst-code`;
   the `SemPrf^k(code(Dk), p, (pow 2 (p+1)))` conjunct is **already checker-
   accepted** (Phase 1 symbolic bound: `p < Log(2^(p+1),1) = p+1`, tower never
   built; verified by
   `sjas-adr0142-semprfk-tower-witness-validates-through-the-checker`).
6. **Step 6: close.** `Dk` (step 4) and `not Dk` (step 5) clash = `BOT`. The
   whole assembles into a closing tree for `(and AxiomConj (not SelfCons))`.

## Ordering and remaining gaps

- Hard dependency: step 5 needs `p` = the step-4 proof of `Dk`, so steps
  1->3->4 (and B) come first; then 5; then 6.
- **`p` magnitude:** `p` is the code of the step-4 `Dk` proof. It must stay
  closed-form representable; the bound only needs `(pow 2 (p+1))` (compact) and
  `Log` of it (Phase 1). Measure `p` once step 4 exists.
- **`pow` vocabulary encode/decode:** the step-5 witness `(pow 2 (p+1))` is fine
  through the formula-level checker with `pow` declared in `:functions`
  (verified). Putting it inside an *encoded* proof tree additionally needs `pow`
  in the coding vocabulary — `pow` is decoded in the witness so it cannot be
  profile-local; this is the careful reserved-symbol work to do uncontended
  (append to `reserved-coding-symbols`, regenerate, re-run the full SJAS gate to
  confirm no user-index mis-decode).

## Deliverable plan

Promote the test-helper node builders into a `proflog.sjas-tree-builder`
namespace, then build each step's tree test-first against `structural-proof-valid?`
over the exact generated system, promoting the corresponding
`theorem23-closure-status` tag to `:checker-accepted` as each lands. The
addition-only contrast (Phase 4) is the falsifier: the same construction must
fail at the `SemPrf^k`/`Log` step there.
