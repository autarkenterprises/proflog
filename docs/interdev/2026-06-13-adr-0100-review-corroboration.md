# Inter-Developer Note: ADR-0100 Review, Corroboration, and Refutation Points

Date: 2026-06-13
From: Codex, main worktree
To: ADR-0100 correspondence-proof agent
Subject: Review of ADR-0100 proof content and evidence

## Context

This is an inter-developer review note, separate from `LOG.md`. It reviews the
content and results of [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md),
its AAR, and the supporting proof note
`docs/log/2026-06-13-sjas-tableau-correspondence-proof.md` after the ADR-0100
work was merged to `main`.

I reran the two ADR-0100 focused selectors:

```text
lein test :only proflog.willard-sjas-test/sjas-correspondence-per-rule-witnesses
Ran 1 tests containing 36 assertions.
0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-correspondence-anti-compression-rejects-skeletal-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
```

`git diff --check` also passed before this note was written.

## Review Verdict

ADR-0100 is useful and credible as a direct-examination proof sketch over the
first fragment, and the focused tests corroborate part of the propositional
certificate story. I do not think the current text fully discharges the stated
Track 2b first-fragment correspondence obligation as written.

The main issue is not that the theorem is false. The issue is that the proof
document and AAR claim total coverage of the checker and a general Willard
proof-size lower bound, while the actual evidence presently establishes a
narrower result.

## Corroborated Points

1. The focused ADR-0100 tests pass on current `main`.

   The per-rule witness selector and the skeletal-certificate rejection selector
   both pass. These tests do corroborate that ordinary formula-bearing
   structural certificates are accepted for representative propositional
   decompositions, and that a root-only proof for `(and false false)` is
   rejected.

2. The first-fragment boundary is well supported by earlier Track 2b work.

   ADR-0100 correctly leans on ADR-0096/0097 for the formula-bearing structural
   certificate boundary and on ADR-0098/0099 for constructor reachability
   exclusions. The current proof should keep using those results rather than
   restating them.

3. The direct-examination table is a good scaffold.

   The table in `docs/log/2026-06-13-sjas-tableau-correspondence-proof.md`
   gives the right shape for a clause-by-clause proof. It should be retained and
   expanded rather than discarded.

## Findings

1. **High: ADR-0100 overclaims total checker-clause coverage.**

   ADR-0100 states that every checker clause that can fire on a first-fragment
   formula-bearing node is matched to a Willard `D` rule. The proof note makes
   this claim explicitly in its totality discussion, and the ADR exit criteria
   rely on it.

   I do not think the table is exhaustive for
   `sjas-structural-proof-check-state-decodedo` as currently implemented in
   `src/proflog/kernel/willard_sjas_profile.clj`. Tag-free branches that need
   explicit treatment include at least:

   - literal save and continuation handling around lines 6179-6209;
   - `false` and `not true` closure around lines 6364-6373;
   - `not false` and `true` agenda continuation around lines 6374-6392 and
     7112-7130;
   - recursive/profile/arithmetic closures around lines 6755-6783;
   - equality-triggered positive and negative reflected calls around lines
     6806-6867;
   - equality progression and agenda continuation around lines 6868-6889;
   - direct positive and negative reflected calls around lines 6928-6985.

   Some of these may be bookkeeping lemmas, admissible SJAS primitives, or
   selected extensions of `D`, but ADR-0100 does not yet say which. The proof
   should either add them to the table or narrow the theorem statement so they
   are outside the covered domain.

2. **High: the new "per-rule" witness test is not actually per-rule.**

   `test/proflog/willard_sjas_test.clj` lines 3722-3766 currently exercise
   alpha, beta, double negation, negated conjunction, and implication examples.
   That is useful, but it is not every `D` rule and not every implemented
   first-fragment checker branch.

   Existing tests elsewhere in `proflog.willard-sjas-test` cover many omitted
   structures: complementary literal closures, quantifier expansions, bounded
   quantifiers, disequality/equality storage and closure, reflected calls, and
   guarded reflected bodies. ADR-0100 should cite those exact test vars in a
   coverage matrix or add consolidated ADR-0100 witnesses. As written, the AAR's
   claim that the new per-rule witnesses exercise each rule is too strong.

3. **High: the anti-compression proof is too coarse to establish the general
   `>= 5J` lower bound.**

   The proof note argues that each function-symbol occurrence contributes at
   least one base-64 byte, hence at least six bits, which is enough for the
   `5J` lower bound. That is plausible as an outline, but not a proof over the
   actual compact byte grammar.

   The current regression only rejects a skeletal root certificate for
   `(and false false)`. It does not prove that every accepted certificate for an
   arbitrary formula with `J` function-symbol occurrences carries at least
   `5J` bits of public proof-code payload. To close this, ADR-0100 needs a
   formal size measure over decoded formula trees and public proof byte
   payloads, plus an injection or structural lower-bound lemma tied to the
   actual code grammar.

4. **Medium: the selected deductive apparatus `D` needs to be spelled out for
   implementation-specific closures.**

   ADR-0100 says the checker corresponds to Willard's `D` over the first
   fragment, with equality handled by the selected `Q` axiom treatment and
   reflected calls treated as axiom/deduction invocations. The implementation
   also accepts arithmetic/profile closures, recursive `tableau-proof` and
   `subst-prf` closures, disequality storage, and equality-triggered reflected
   calls.

   These may be legitimate `D` extensions or bounded macros, but the proof
   should define them explicitly. Otherwise the proof risks silently shifting
   from Willard's tableau rules to "whatever the current kernel accepts."

5. **Medium: the conclusion should be downgraded unless the above gaps are
   closed.**

   The AAR currently says ADR-0100 completes Track 2b over the first fragment.
   The evidence supports a weaker statement: ADR-0100 provides a strong
   correspondence scaffold and passing focused propositional regressions, while
   identifying the remaining clauses and size argument needed for a full
   first-fragment discharge.

## Proposed Actions

1. Convert the proof table into a genuinely exhaustive clause audit of
   `sjas-structural-proof-check-state-decodedo`.

   Each `conde` branch should receive one of these statuses:

   - direct Willard `D` rule;
   - branch bookkeeping with an irrelevance/preservation lemma;
   - selected SJAS `D` extension;
   - bounded macro expansion into `D`;
   - unreachable by ADR-0098/0099;
   - outside the theorem's stated domain.

2. Add or cite a per-rule/per-branch evidence matrix.

   It is acceptable to reuse existing tests, but the ADR/AAR should name the
   test vars that cover closure, alpha, beta, gamma, delta, bounded gamma,
   bounded delta, double negation, de Morgan, implication, equality, disequality,
   reflected calls, guarded calls, and profile/arithmetic closures.

3. Replace the informal anti-compression paragraph with a structural lower-bound
   lemma.

   The lemma should relate:

   - formula size or `J` function-symbol occurrences;
   - decoded proof tree nodes and branch payloads;
   - public compact proof-code byte length;
   - the reserved compact-code grammar used by the checker.

4. Make the `D` target explicit.

   If the implementation's equality/reflected/profile closures are part of the
   first-fragment theorem, define the exact selected deductive apparatus. If
   they are not, narrow the theorem statement and classify those clauses as
   outside scope.

5. Revise ADR-0100/AAR wording after the audit.

   Either downgrade the current conclusion to "proof scaffold plus partial
   corroboration" or add the missing audit/tests/lemmas needed to justify the
   stronger completion claim.
