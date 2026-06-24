# ADR-0142 Phase 3: construct-and-check baseline over the real mul system

Date: 2026-06-23
Plan: `~/.claude/plans/velvet-conjuring-frost.md`
Builds on: [Phase 0](2026-06-23-adr-0142-phase0-checker-characterization.md),
[Phase 3 tree-construction spec](2026-06-23-adr-0142-phase3-tree-construction-spec.md)

This session promoted the Phase 3 tree-construction infrastructure and committed
a checker-verified construct-and-check baseline over the **exact generated
multiplication-total system** (not just the demo system Phase 0 used). It also
pinned down precisely which tableau closing rules the Theorem 2.3 diagonal path
may rely on, and the precise risk in the remaining `pow`-vocabulary step. No
closure step was promoted to `:checker-accepted`; nothing is overclaimed.

## Delivered (checker-verified, committed)

- **`proflog.sjas-tree-builder`** (new src ns). Promotes the formula-bearing
  tableau node builders that were private helpers in `proflog.willard-sjas-test`:
  `formula-code-bytes`, `canonical-formula-code-bytes`, `flex-tableau-node`
  (auto-selects narrow `(count bytes... child...)` vs wide `((bytes...) child...)`),
  `canonical-flex-tableau-node`, and `valid-tree?` (wrapper over the public
  `structural-proof-valid?`). These are the construction primitives every closure
  step is built from.
- **`proflog.sjas-tree-builder-test`** (new, 3 tests / 12 assertions, green).
  Constructs and validates cut-free tableau trees over the real `mul-system`
  (`:willard-sjas-total-multiplication`, profile relations, `:u-grounding`),
  matching the closure assembler's system:
  - reflexive disequality closure, conjunction expansion (left conjunct closes),
    nested conjunction, double negation — all accepted;
  - the flex builder's narrow/wide shape auto-selection, with a wide (>= 64-byte,
    ~60-digit-numeral) closing node validating;
  - the closure-rule characterization below.

## Finding: which complementary closures fire (precise)

Over the real U-Grounding mul system, a constructed `(and (pos A) (neg A))` clash
closes **iff** the relation head of `A` decodes to a *named* symbol:

| relation | decode (proof-facing) | pos/neg clash closes? |
|---|---|---|
| `subst-code`, `lt`, `leq`, `axiom-member` | named (`subst-code`, ...) | **yes** |
| `finax4`, `semprf-alpha`, `semprfk-alpha`, `willard-map`, `mul` | `(sym n)` | no |
| any *user* relation (e.g. an added `opaque`) | `(sym n)` | no |

Reason: in U-Grounding / proof-facing mode the generated source symbol table is
removed (`willard_sjas_profile.clj` `sjas-object-symbol-indexo`). Only symbols in
`reserved-symbol-index-entries` (= `reserved-coding-symbols` minus
`profile-local-reserved-symbols`) recover a semantic name; everything else decodes
to a structural `(sym n)` id. The complementary-literal closure compares atoms,
and `(sym n)`-headed atoms are deliberately not closed by raw syntactic clash —
the boundary profile relations are matched by **interpretation** (the `semprf` /
`semprfk` profile rules, the `subst-code` relation) rather than by literal clash.

**This is a characterization, not an obstruction for the closure.** The Theorem
2.3 diagonal closing path (`Dk = forall h y z. Subst(nbar,h) => not
SemPrf^k(h,y,z)`, instantiated at the diagonal witnesses) closes its `Subst`
conjunct via `subst-code` (reserved, named) and its `SemPrf^k` conjunct via the
profile interpretation of `semprfk` — never via an opaque user-relation clash. So
the closing rules the construction needs are exactly the ones that fire.

## Finding: the precise `pow`-vocabulary risk (open-boundary item 3)

Putting the step-5 witness `SemPrf^k(code(Dk), p, (pow 2 (p+1)))` inside an
*encoded* proof tree needs `pow` to decode to its name (the bound check at
`sjas-semprfk-bound-holdso` matches the literal `(list 'app 'pow base exp)`). That
requires appending `pow` to `sjas-code/reserved-coding-symbols` and **keeping it
out** of `profile-local-reserved-symbols` (unlike `finax4` et al.), so the decoder
gives it a semantic name.

The real risk, now characterized: the **encoder** (`willard-sjas-code/context`)
assigns each system a *compacted* index over `[declared-reserved ++ sorted-user]`,
while the **proof-facing decoder** resolves *global* reserved indexes from
`reserved-symbol-index-entries`. For a system that declares `pow`, appending `pow`
to reserved shifts the encoder's user-symbol indexes by +1 for user symbols
sorting before `pow`. Appending `pow` must therefore be verified to keep the
encoder's compacted view and the decoder's global view in agreement, with the full
SJAS gate (1429 not-slow tests) as the no-mis-decode falsifier. Deferred to the
step-5 tree work that actually exercises it, rather than changing global coding
speculatively.

## Status (unchanged; honest)

`theorem23-closure-status` still reports steps 1/3/4/B as `:tree-construction`,
step 5 as `:partial`, and the open boundary still enumerates the three remaining
items. The new `:resolved-since-aar :phase3-baseline` records the infrastructure +
baseline + closure-rule finding; the `:open-boundary :note` records the precise
`pow` index-consistency risk. No BOT derivation is claimed.

## Next

Build the step trees test-first with `proflog.sjas-tree-builder` against the real
system, in dependency order (steps 1 -> 3 -> 4 and B; then step 5 once `pow` is in
the coding vocabulary; then step 6). The step-5 `not Dk` tree is the natural first
target whose hard sub-part (the `SemPrf^k` bound witness) is already
checker-accepted via the query path; it gates on the `pow`-vocabulary work above.
