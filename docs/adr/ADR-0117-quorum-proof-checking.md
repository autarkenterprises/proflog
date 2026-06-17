# ADR-0117: Quorum Proof-Checking and the Proof-Term Adequacy Finding

- Status: accepted
- Date: 2026-06-17
- Branch: `fitting-fidelity-audit`
- AAR: [AAR-0117](../aar/AAR-0117-quorum-proof-checking.md)
- Audit: [docs/FITTING_FIDELITY_AUDIT.md](../FITTING_FIDELITY_AUDIT.md) (Quorum section)

## Context

The Fitting-fidelity audit's strongest *correctness* lever (as opposed to
*fidelity*) is that a closed tableau is a **certificate**: if the kernel is
correct, an emitted proof must be **re-checkable**. The guiding insight (from the
plan review): *the kernel is a miniKanren relation, so binding its `proof`
argument turns the prover into a checker with no new logic* — "if the kernel is
correct it already is a checker." A second, independently-authored checker then
provides cross-validation, and the decisive signal is **agreement across a
quorum**: a silent kernel bug would have to be replicated identically in an
independent checker to escape.

## Decision

Build a quorum of three verdict oracles over each `(formula, program, proof)`
triple and require them to agree:

1. **kernel-as-prover** — `kernel/prove` / `prove-program` generates the proof
   (the reference for which proofs exist).
2. **kernel-as-checker** — the *same* `proveo` / `prove-programo` relation run
   with the `proof` argument **bound** (`proof-quorum-test/kernel-accepts?`,
   `kernel-program-accepts?`). It succeeds iff the candidate is consistent with a
   closing search. Zero new logic.
3. **independent checker** — `proflog.proof-check/check`
   (`src/proflog/proof_check.clj`): a non-relational, plain-Clojure structural
   validator that shares no code with the kernel and uses no core.logic. It
   re-validates the proof term against the proof-tag grammar
   (`proof-check/rule-arities`) verified directly from `kernel.clj` and
   `equality.clj`.

The harness `test/proflog/proof_quorum_test.clj` requires oracles (2) and (3) to
**accept** every genuine certificate and **reject** mutated ones (garbage tag,
dropped subproof).

## The proof-term adequacy finding (the substantive result)

Every greenfield proof node is `(tag subproof*)` (or the empty terminal `()`),
and a tag wraps **only subproofs** — never a formula, term, δ-witness,
γ-instantiation, or unifier. (Confirmed exhaustively: the full grammar — α/β/γ/δ,
equality-internal `decompose`/`args`/`eq-bind`/`par-bind`/`atom-close`,
saved-call `eq-triggered-*`, and the guarded-alternative family — carries no
object-language payload; see `rule-arities` and
`proof-quorum-test/proof-terms-are-pure-tag-trees-no-embedded-context`.)

Two consequences for the quorum:

- **Adequacy.** An independent oracle **cannot** re-derive *which* formula each
  node proves from the certificate alone — it would have to re-run the search.
  So oracle (3) is necessarily a **structural** (well-formedness) checker, not a
  full semantic re-checker. The semantic re-validation is supplied by oracle (2),
  which re-runs the relation. The certificate is adequate to *guide and constrain*
  re-checking, not to *replace* it.
- **Check determinism.** Binding `proof` constrains the **rule applied at each
  node** (each rule unifies `proof` with a distinct tag shape) but leaves the
  witnesses (γ proof-variable noms, δ parameters, γ-term candidates) to be
  re-searched. Kernel-as-checker is therefore *rule-structure-guided re-search*,
  not a deterministic replay.

This is an honest, useful boundary: the quorum still decisively cross-checks the
kernel's **closure verdicts** (a malformed or mis-tagged certificate is rejected
by both independent checkers), and it precisely characterizes what a Proflog
certificate does and does not record.

## Results

`lein test proflog.proof-quorum-test` → **5 tests / 58 assertions, 0 failures**:

- 6 raw-kernel certificates (free-close, occurs-close, decompose/args, conj +
  eq-step + neq-close, split, witness + savefml + close) — accepted by both
  oracles; garbage and arity-truncated mutants rejected.
- 2 program certificates (P1 `odd(1)`, P2 `win(4)` — the latter exercising the
  guarded-alternative tags) — accepted by both oracles; garbage rejected. No
  `profiled`/`equality-fragment` tags leaked (these closures are pure kernel).
- The independent grammar accepts every genuine proof with **zero unrecognized
  tags**, confirming completeness for the corpus.

## Honest scope

- Oracle (3) is structural by necessity (adequacy, above); it does not re-derive
  tableau semantics. The quorum's guarantee is: *a well-formed, kernel-consistent
  certificate that both independent oracles accept, and that both reject under
  mutation.* It is N-version validation of the certificate shape and the kernel's
  rule selection, not an independent soundness proof of the rules.
- Mutation coverage is structural (tag/arity/garbage). Semantic-witness mutation
  is out of reach precisely because witnesses are not recorded (adequacy).

## Reconciliation note (parallel work)

`origin/main` ADR-0113 (proof object diagnostic renderer) is adjacent
proof-term tooling; reconcile `proof_check.clj` with it at audit close (numbering
already clears 0111–0115).

## Exit criteria

- `proof_check.clj` + `proof_quorum_test.clj` committed; quorum green.
- Kernel-as-checker demonstrated (proof bound ⇒ accept genuine / reject garbage).
- Proof-term adequacy + check-determinism findings recorded here and in the audit
  matrix's Quorum section.
