# Inter-Developer Note: ADR-0141 Genuine-Derivation Feasibility

Date: 2026-06-22

Reply to [2026-06-22-adr-0141-completion-claim-review.md](2026-06-22-adr-0141-completion-claim-review.md).
Direction taken: attempt the genuine checked derivation (review criterion 1).

## Headline finding

The review says the boundary contradiction is accepted by a trusted constructor
instead of being derived. Investigation shows a stronger fact:

**The unsafe systems, as constructed, do not entail their contradiction at all.**
The ordinary proof search rejects `1 = 0` from the total-multiplication system's
program in ~18 ms at fuel 40/120/300 (it fails fast, not on a budget). So the
`willard-sjas-boundary-refutation` constructor was not merely a shortcut around
a real derivation — it was the *entire* demonstration, and it makes the
`dsjas-subst-prf` proof checker **unsound** (it accepts a proof of `1 = 0` that
no ordinary derivation can produce).

Removing the constructor (required) therefore does not leave a slower-but-valid
derivation behind. It leaves the boundary failure unproven, because the system
is not exhibited as inconsistent.

## Why the system does not derive its contradiction

The relevant axioms *are* present (verified on `total-multiplication-complete-system`,
depth 1, 16 axioms: 2 Group-0, 3 Group-1, 10 Group-2, 1 Group-3):

- **Group-3 SelfCons** `Gamma_1` is present: `forall x y p q.
  not pi-star-1-code(x) or not neg-pair(x,y) or not dsjas-subst-prf(S,g,x,p) or
  not dsjas-subst-prf(S,g,y,q)`. It asserts no complementary pair has measured
  proofs — i.e. `not exists p. Prf(bottom)`, the system's own consistency.
- **V5** (Willard 2002, specialized to the contradiction code) is present in
  Group 2: under `FinAx4(alpha) and alpha <= k and Paradox(y,z,alpha,k)` it
  yields `exists p < z. SemPrf_alpha(alpha, contradiction, p)` — i.e. under the
  paradox conditions, `bottom` *is* provable.

The intended contradiction is direct: V5 (under its antecedent) gives
`exists p. Prf(bottom)`; SelfCons gives `not exists p. Prf(bottom)`. They close.

The search cannot produce it because **V5's antecedent is not establishable by
ordinary derivation**. `Paradox(y,z,alpha,k)` unfolds to
`exists d. WillardMap(alpha,k,d) and SemPrfk_alpha(alpha,k,d,y,z)`, and the
witness `d` is Willard's self-referential proof code (a `d` that codes a bounded
proof about `d`). There is no rule by which the tableau constructs that `d`;
`FinAx4(alpha)` is establishable (the v-route test confirms `finax4(system-code)`
succeeds), but `WillardMap`/`SemPrfk_alpha` for a self-referential `d` are not.
So V5 never fires and `bottom` is underivable.

## What a genuine derivation requires

1. **Remove** `willard-sjas-boundary-refutation` from the proof grammar and the
   Level-1/Tab-2 routing, and `boundary-refutation-proof` from the builder.
2. **Construct a concrete diagonal witness `d`** (and `y,z,alpha,k`) that
   *genuinely* satisfies the kernel-checked `WillardMap` and `SemPrfk_alpha`
   predicates for the exact generated system. This is the mechanization of
   Willard's fixed point (a Goedel/Loeb-style self-referential proof code). It
   is the crux and the hard part.
   - Open sub-question to settle first: are `sjas-semprf-alpha` /
     `sjas-semprfk-alpha` (kernel handlers exist, e.g.
     `sjas-semprf-alpha-destructureo`) *genuinely checking* a decoded bounded
     proof, or do they destructure/trust? If they trust, that trust must be
     removed too, or the witness is again circular.
3. **Supply that witness in an ordinary measured `D_SJAS` proof object** whose
   steps the checker validates: instantiate V5, discharge its antecedent with
   the witness, derive `exists p. Prf(bottom)`, instantiate SelfCons, close.
4. The complementary member (`1 != 0`) is a genuine Group-0 axiom; that leaf is
   legitimate. Only the contradictory member needs the derivation.
5. Then redo synthesis as genuine search (review section 2) and harden the
   ledger (review section 3).

## Assessment

This is the mechanization of a Goedel-second-incompleteness / Loeb inconsistency
inside the SJAS measured-proof apparatus. It is the central hard problem of the
SJAS program, not an implementation cleanup. It is unlikely to be completed in a
single session, and may require redesigning the unsafe systems so the diagonal
witness is concretely constructible and every paradox predicate is genuinely
checked.

Workstreams B and C remain reopened. The honest near-term step that improves
soundness regardless of the mechanization is to remove the trusted constructor
(item 1) and add a test asserting the checker rejects it; that will turn the
boundary evidence tests red, which correctly reflects the open state.

## Mechanization analysis (2026-06-22, continued)

Direction: "start the mechanization." Verified the supporting predicate status
and the V4/V5 structure (Willard 2002, Eq 12-15; paper present at
`sjas/nachlass/papers/willard2002_new_exceptions_tableaux_author_tab2.pdf`).

### Predicate implementation status (in `willard_sjas_profile.clj`)

- **Genuinely checked** (decode a proof / reconstruct the system, no trust):
  `FinAx4` (`sjas-finax4-coreo`: `alpha` must reconstruct as a complete,
  source-valid generated system), `SemPrf_alpha` and `SemPrfk_alpha`
  (`sjas-semprf*-coreo`: proof must be an axiom citation of an axiom member, or a
  decoded structural tableau proof of the theorem; the bounded form additionally
  checks `proof < bound` via `lt`), `subst-code` (`sjas-subst-code-anyo` /
  `sjas-subst-code-closeo`), `lt`.
- **Missing / uninterpreted:** `willard-map` has **no** kernel handler (only a
  `profile-local-reserved-symbols` entry) — so V5's antecedent `Paradox` (Eq 12,
  `exists d. willard-map(alpha,k,d) and SemPrfk(alpha,k,d,y,z)`) cannot be
  established at all, which is exactly why the search derives nothing.
- **No least-number principle / induction** anywhere in the profile (SJAS is
  deliberately weak). This matters for how the descent terminates.

### V4 is *finite* descent

V4 (Eq 15 `Upsilon`, descent): `forall ... Upsilon(alpha,k,g,h,y,z) ->
bounded-exists h*<h, y*<y, z*<z. Upsilon(alpha,k,g,h*,y*,z*)`, where
`Upsilon(alpha,k,g,h,y,z) = subst-code(g,h) and SemPrfk_alpha(alpha,k,h,y,z)`.
For a **concrete** witness with a small `z0`, V4 unfolds `z0` times to a
contradiction (each step strictly decreases the natural-number bound `z`); no
induction is required. This avoids the missing-LNP gap.

### Two implementation paths

- **Path A (preferred) — V4 / descent via `subst-code`.** Needs *no new trusted
  kernel relation*: `subst-code`, `SemPrfk_alpha`, `lt` are all already genuine,
  and V4 is an axiom. The whole task is to construct one genuine witness
  `Upsilon(alpha,k,g,h0,y0,z0)` — a formula `g`, its diagonal `h0 = subst(g)`
  with `subst-code(g,h0)` checking, a real bounded tableau proof `y0` of `h0`,
  and a small `z0` — then build the `z0`-step finite-descent proof object whose
  every step is an ordinary V4 instantiation + bound check. The closure comes
  from the bound reaching a value no proof code can satisfy.
- **Path B — V5 / `willard-map`.** Requires implementing `willard-map` as a
  genuine checked relation (the self-referential diagonal map of the paper) plus
  the witness. More machinery; defer unless Path A's witness proves impossible.

### The remaining hard step

The crux is the **provable diagonal `h0`** and its bounded proof `y0`. In
Willard's construction `h0` is the self-referential sentence the compression
operates on; `subst-code(g,h0)` pins it as `g`'s diagonal. It must be genuinely
provable with a small `z0` for `SemPrfk_alpha(alpha,k,h0,y0,z0)` to hold without
trust. Determining `g`/`h0`/`y0` precisely requires reading Willard 2002's
multiplication section (Eq 12-16 and the V4/V5 derivation). This must not be
guessed: a wrong `h0` that is *asserted* provable would re-introduce the
circularity the review removed. Next concrete steps, in order:

1. Read Willard 2002 Eq 12-16 + surrounding text; extract the exact `g`/`h0`
   diagonal and why descent + boundedness contradict.
2. Build `g`, check `subst-code(g,h0)` decodes, and construct a real tableau
   proof `y0` of `h0` that `SemPrfk_alpha` accepts at a small `z0`.
3. Encode the `z0`-step descent as one structural proof object; verify each step
   is an ordinary V4 instantiation (no boundary constructor).
4. Only then remove `willard-sjas-boundary-refutation` and repoint the
   constructed evidence at the genuine descent proof; redo synthesis as real
   search; harden the ledger; unify surfaces.

Status: mechanization *started* (architecture + path A identified, predicate
trust audited). The witness/descent implementation (steps 1-3) is the open deep
work and is a focused multi-session effort, not yet done.

## Soundness fix landed (2026-06-22)

Direction: "restore soundness first." The two trusted-constructor branches were
removed from `sjas-subst-prf-bytes-coreo` and `sjas-tab1-tableau-proof-bytes-coreo`;
the proof checker now rejects a `willard-sjas-boundary-refutation` proof of
`1 = 0` (verified: `counterexample-valid? false`, `status :rejected`,
`theorem-dsjas-subst-prf false`). The not-slow SJAS gate (1399) and fast gate
(232/1540) stay green. The three slow ADR-0141 evidence tests were converted to
assert the open/undemonstrated state.

**Follow-up bug:** with the boundary branch gone, validating the (still
host-built) constructor proof object can *throw* a core.logic
`ISubstitutions/walk of nil` inside `ast-alpha-bound-nomo` rather than rejecting
cleanly — i.e. the structural path does not gracefully reject a symbol-headed
boundary certificate. This is a robustness bug, not a soundness one (the
constructor is still not accepted); the converted tests tolerate it via
`try/catch`. The dead boundary-check helpers
(`sjas-boundary-refutation-proof-bytes-coreo`, `sjas-boundary-profile-hypotheses-coreo`,
the byte-prefix routers, the `boundary-refutation-proof` emitter) remain as
unused code to be removed when the genuine derivation replaces them.
