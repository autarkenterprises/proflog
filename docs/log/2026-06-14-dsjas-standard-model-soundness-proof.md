# Standard-Model Soundness of the D_SJAS U-Grounding Arithmetic Primitives

Date: 2026-06-14

This note discharges ADR-0108's standing assumption
`:standard-model-soundness-of-ugrounding-primitives` — the foundation under the
family-5 (arithmetic closure) and family-4 (equality) rule-soundness clauses
proved in `docs/interdev/2026-06-14-dsjas-rule-family-soundness-proof.md`, and
under Willard's Theorem 6.3 consistency-preservation (which requires the relevant
sentences to be "true in the Standard-Model"). It is a direct-examination proof
grounded in the actual relations, corroborated by a property test
(`proflog.dsjas-arithmetic-soundness-test`); it is not machine-checked, and it
cites the published Kiselyov–Byrd–Friedman–Shan correctness result for the
bit-level core.

## Theorem (informal)

Let `val(t)` be the intended standard value of a closed SJAS numeral term `t`
under Willard's totalized arithmetic, and let `⟦β⟧` be the standard value of a
canonical little-endian binary numeral `β`. Then:

1. **(reader)** if `sjas-num-inputo(t, β, σ, σ, [], [], _)` succeeds for a closed
   `t` (empty pending-binds in and out), then `⟦β⟧ = val(t)`; and conversely the
   reader produces such a `β` for every closed `t`;
2. **(relations)** for closed numeral terms with values `a, b, c`,
   `sjas-relation-holdso(mult,[a,b,c])` ⟺ `a·b = c`,
   `…(leq,[a,b])` ⟺ `a ≤ b`, `…(lt,[a,b])` ⟺ `a < b`; and
   `sjas-relation-failso` is their exact complement on ground args;
3. **(equality)** `sjas-normal-equalo(l, r, …)` ⟺ `val(l) = val(r)`.

Consequently the arithmetic closures fire exactly on standard-false literals, and
the equality closures exactly on standard equalities/disequalities — the
satisfiability-preservation that family-5/family-4 require.

## 1. Encoding, denotation, canonicity

Numerals are little-endian binary bit-lists (`relational_arithmetic.clj` header):
`()` = 0, `(1)` = 1, `(0 1)` = 2, `(1 1)` = 3, …; in general
`⟦(b₀ b₁ … bₖ)⟧ = Σ bᵢ·2ⁱ`. The representation is **canonical**: the high bit is
`1` (no trailing zero), enforced throughout by `poso`/`>1o` guards. Canonicity
gives the key fact used for equality:

> **(Uniqueness).** Distinct canonical bit-lists denote distinct naturals; so
> `β₁ = β₂` (syntactic) ⟺ `⟦β₁⟧ = ⟦β₂⟧`.

## 2. Bit-level core — Kiselyov–Byrd–Friedman–Shan (cited + translation verified)

`proflog.relational-arithmetic` is, by its own header and by inspection, a
**faithful Clojure translation of faster-minikanren's `numbers.scm`** — the
relational arithmetic of Kiselyov, Byrd, Friedman & Shan, *"Pure, Declarative,
and Constructive Arithmetic Relations"* (FLOPS 2008). That paper proves these
relations **sound and complete** w.r.t. standard arithmetic on the canonical
encoding, and **decidable / terminating on ground numerals**. I verified the
translation matches the canonical definitions clause-for-clause:

| relation | contract (on canonical numerals) | status |
|---|---|---|
| `pluso n m k` | `⟦n⟧+⟦m⟧ = ⟦k⟧` | KBFS, matches `numbers.scm` |
| `minuso n m k` | `⟦n⟧−⟦m⟧ = ⟦k⟧` (defined iff `n≥m`) — `(pluso m k n)` | matches |
| `*o n m p` | `⟦n⟧·⟦m⟧ = ⟦p⟧` | matches (incl. `odd-*o`/`bound-*o`) |
| `=lo / <lo` | equal / shorter bit-length | matches |
| `<o n m` / `<=o n m` | `⟦n⟧<⟦m⟧` / `≤` | matches |
| `divo n m q r` | `⟦n⟧ = ⟦m⟧·⟦q⟧+⟦r⟧ ∧ 0≤⟦r⟧<⟦m⟧` | matches (Euclidean) |
| `logo n b q r` | `b^q ≤ n < b^{q+1}` (with remainder) | matches |

So **part (2)'s bit-level operations are standard-sound by the published KBFS
theorem**, with the implementation a verified faithful port (provenance +
clause-level check). `sjas-relation-holdso` evaluates `mult/leq/lt` directly via
`*o/<=o/<o`, so it computes their standard truth; `sjas-relation-failso` computes
the complement (`mult`: `expected = a·b` via `*o`, then `distinct-num-bitso` =
`<o ∨ >o`), guarded to **ground args** (empty pending-binds), so it succeeds iff
the relation is standard-false. Part (2) holds.

## 3. SJAS totalization wrappers (the SJAS-specific content)

Willard's arithmetic uses **total** functions (subtraction, division, …
totalized at their classical boundary). Each SJAS wrapper is an **exhaustive and
mutually-exclusive** case split over the KBFS core, so it denotes a total
function; each computes the standard totalized value:

- `sjas-monuso x y` = `monus`: `x≤y ⟹ 0`; `y<x ⟹ minuso x y = x−y`. Cases
  partition (`≤` vs `<` reversed); equals `max(x−y, 0)`. ✓
- `sjas-divo x y`: `y=0 ⟹ x` (Willard's div-by-zero = numerator); `y>0 ⟹
  divo x y out _ = ⌊x/y⌋`. ✓
- `sjas-maxo x y` = `max`: `x≤y ⟹ y`; `y<x ⟹ x`. ✓
- `sjas-logo x` = `⌊log₂x⌋` totalized: `x∈{0,1} ⟹ 0`; `x>1 ⟹ logo x 2 = ⌊log₂x⌋`. ✓
- `sjas-powo b e` = `b^e` by induction on `e` (`b^0=1`; `b^{p+1}=b^p·b` via `*o`). ✓
- `sjas-rooto x y` = `⌈x^{1/y}⌉` totalized: `y=0 ⟹ x`; `y>0,x=0 ⟹ 0`; else the
  unique `out` with `(out−1)^y < x ≤ out^y` (via `sjas-powo`,`<=o`,`<o`). ✓
- `sjas-counto bits width` = number of `1`s among the low `width` bits, by
  structural recursion decrementing `width`. ✓

Each split is decidable on ground inputs (the `zeroo/poso`, `≤o/<o` guards
partition the ground domain), so the wrappers are **total decidable functions**
computing the stated standard values. The totalizing conventions (monus→0,
div0→numerator, log0→0, root0→numerator) are **definitional choices of the SJAS
language**: the standard model M interprets these function symbols by exactly
these conventions, so "standard value" *means* the totalized value, and the
wrappers compute it. This is not an open assumption but a specification of which
function the symbol denotes.

## 4. The numeral-term bridge (`sjas-num-appo` / `sjas-num-inputo`)

`sjas-num-inputo` walks `t` under σ; an unbound `(var nom)` is **deferred** as a
pending-bind (never denoted until ground); otherwise `sjas-num-appo` interprets
the head constructor, recursing on subterms and combining with the matching
wrapper:

```
0 → ()    1 → (1)    (dbl a) → pluso â â    (add a b) → pluso â b̂
(pred a) → monus â 1    (sub a b) → monus â b̂    (div a b) → div â b̂
(max a b) → max â b̂    (log a) → log â    (root a b) → root â b̂    (count a b) → count â b̂
```

**Lemma (reader soundness).** For every closed numeral term `t`, if
`sjas-num-inputo(t, β, σ, σ, [], [], _)` succeeds then `⟦β⟧ = val(t)`.
*Proof.* Induction on the structure of `t`. Base: `0`,`1` denote `0`,`1`. Step:
each constructor's subterms are closed, so by IH their bit-lists denote their
values; the wrapper (§3) then computes the standard value of the constructor
applied to those values, and the result is canonical (KBFS/wrappers return
canonical numerals). ∎ Completeness (a `β` exists for every closed `t`) is the
totality of the wrappers on ground inputs.

Because `sjas-relation-failso` (and the ground `sjas-relation-holds-coreo` paths)
demand **empty pending-binds**, arithmetic closure fires only when all argument
terms are closed — exactly the regime where this lemma applies. So family-5
closure decides the *standard* truth of the atom over closed terms.

## 5. Equality (`sjas-normal-equalo`) and the bridge to M

`sjas-normal-equalo l r` reads both sides to canonical `β_l, β_r` (§4) and asserts
`β_l = β_r`. By Uniqueness (§1), this holds iff `⟦β_l⟧ = ⟦β_r⟧`, i.e. iff
`val(l) = val(r)`. Part (3) holds; family-4 closures (`sjas-neq-close-coreo`,
`sjas-eq-progresso`) therefore fire exactly on standard (dis)equalities.

**Bridge to the Normed model M.** KBFS soundness is w.r.t. the full standard
model ℕ. Fact D.3's model M agrees with ℕ on Δ₀ facts for arguments ≤ the
U-height bound. Closure fires on atoms whose argument terms are on the branch,
hence U-grounded ≤ bound; `mult/leq/lt/=` are Δ₀ graph facts about those
arguments, so `M ⊨ atom ⟺ ℕ ⊨ atom ⟺` the KBFS evaluation. (The checker's
*internal* product `a·b` may exceed the bound, but the *model fact decided* —
whether `(a,b,c)` is in the graph for branch-present `a,b,c ≤ bound` — is the Δ₀
fact M shares.) Thus standard soundness transfers to soundness w.r.t. M, which is
what Fact D.3 needs.

## 6. What is discharged, and the residual

`:standard-model-soundness-of-ugrounding-primitives` is reduced to:
1. the **published KBFS** soundness/completeness theorem for the bit-level core;
2. a **verified faithful translation** of `numbers.scm` (provenance + clause check);
3. **verified totalization wrappers** (§3 — the genuinely SJAS-specific part);
4. **structural induction** for the term reader (§4);
5. **canonical-form uniqueness** for equality (§1, §5).

No open soundness gap remains at the primitive level. The only non-derived inputs
are *definitional*: M interprets the SJAS function symbols with Willard's
totalizing conventions (which is what specifies the language's standard model),
and the encoding is the canonical one (enforced by the `poso`/`>1o` guards). The
one ancillary code dependency, `sjas-powo`, is verified here (§3).

## 7. Caveat

This is rigorous direct examination plus property-based corroboration, not a
mechanized proof; the bit-level correctness is cited from KBFS rather than
re-proved. That matches the epistemic standing of the rest of the `D_SJAS` line
(ADR-0104/0108). A mechanized port of the KBFS proof, or a generative
`test.check` property suite over a wider operator/range envelope, would raise the
assurance further and is the natural follow-up.
