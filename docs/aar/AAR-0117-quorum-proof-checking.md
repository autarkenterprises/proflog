# AAR-0117: Quorum Proof-Checking and the Proof-Term Adequacy Finding

- Status: accepted
- Date: 2026-06-17
- ADR: [ADR-0117](../adr/ADR-0117-quorum-proof-checking.md)

## Outcome

A three-oracle quorum (kernel-as-prover → kernel-as-checker with the proof bound
→ independent non-relational `proof_check.clj`) cross-validates the kernel's
closure verdicts. `proof_quorum_test.clj` is green at 5 tests / 58 assertions:
every genuine certificate (incl. P2 `win(4)` with guarded alternatives) is
accepted by both checkers; every mutant (garbage tag, dropped subproof) is
rejected. The independent grammar covers the entire proof-tag vocabulary with
zero unrecognized tags on the corpus.

## What worked

- The user's framing was exactly right: binding `proof` turns `proveo` into a
  verifier with no new code. Confirmed empirically (accept genuine / reject
  garbage), so kernel-as-checker is a real, free oracle.
- Reading the *complete* tag grammar from `kernel.clj` + `equality.clj` first
  (incl. the guarded-alternative family) meant the independent checker validated
  every genuine proof on the first green run — no grammar gaps.

## What surprised / was learned

- **Proof-term adequacy.** Greenfield certificates are *pure tag trees*: a tag
  wraps only subproofs, never a formula/witness/unifier. The independent oracle
  therefore can only validate *structure*, and the kernel-as-checker is what
  supplies *semantic* re-validation. The certificate guides re-checking; it does
  not replace it.
- **Check determinism.** Binding the proof fixes the rule at each node but not
  the witnesses (re-searched). Kernel-as-checker is guided re-search, not replay.
- These two are genuine, honest limitations — and the quorum still decisively
  catches malformed/mis-tagged certificates, which is the soundness signal wanted.

## Follow-ups

- A *semantic* certificate (one recording δ-witnesses / γ-instantiations / the
  clause used at each call) would let an independent oracle re-check meaning
  without re-search — a possible enhancement, and the point of contact with
  `origin/main` ADR-0113 (proof object diagnostic renderer) at reconciliation.
- Extend mutation coverage if/when certificates carry witnesses (semantic
  mutation is currently out of reach because witnesses are not recorded).
