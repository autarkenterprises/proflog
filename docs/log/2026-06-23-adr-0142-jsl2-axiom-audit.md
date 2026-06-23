# ADR-0142 Criterion 1: JSL2 Equation (12)-(16) Axiom Audit

Date: 2026-06-23
ADR: [ADR-0142](../adr/ADR-0142-sjas-boundary-genuine-derivation.md)

Formula-by-formula audit of `proflog.sjas-boundary-axioms` (and its
willard-sjas re-exports) against Willard 2002 JSL2
(`willard2002_semantic_tableaux_robinson_q_author_jsl2.pdf`) §3.2, Equations
(12)-(16), plus Definition 2.1, Lemma 3.3, and Theorem 2.3. This discharges
exit-criterion 1: *retain and audit the genuine apparatus; remove only invented
or incorrectly-specialized definitions.*

## Source forms (verbatim from JSL2)

```text
Def 2.1   SemPrfk_a(x,y,z)  iff  SemPrf_a(x,y) AND y < Log(z,K)
          Log(x,k) = k-fold iterated floor(log2(.)),  Log(0)=0,  Log(x,0)=x

Lem 3.3   Map(a,k,d) is a Delta0 formula true iff d = Goedel-number of D^k(a)

Eq (12)   Paradox(y,z,a,k) =df  EXISTS d<z { Map(a,k,d) AND SemPrfk_a(d,y,z) }
Eq (14)   V3 =df  forall g h h*. { Subst(g,h) AND Subst(g,h*) } => h=h*
Eq (15)   V4 =df  forall a k g h y z [ Y(a,k,g,h,y,z)
                       => EXISTS h*<=h y*<=y z*<=z  Y(a,k,g,h*,y*,z*) ]
              where  Y(a,k,g,h,y,z) =df { Subst(g,h) AND SemPrfk_a(h,y,z) }
Eq (16)   V5 =df  forall y z a k { [ FinAx4(a) AND k>=a AND Paradox(y,z,a,k) ]
                       => EXISTS x<z  SemPrf_a(BOT,x) }
```

## Audit table

| JSL2 object | Kernel builder | Verdict |
|---|---|---|
| `Subst(g,h)` (Eq 4) | `subst-code` atom + `sjas-subst-code-anyo` | **faithful** — Goedel substitution graph; checked relation |
| `Y` / Upsilon (Eq 15) | `total-multiplication-willard-upsilon` | **faithful** — `(and (subst-code g h) (semprfk-alpha a k h y z))` matches verbatim |
| `Map(a,k,d)` (Lem 3.3) | `willard-map` atom | atom present; **no kernel handler** → criterion 4 implements it |
| `Paradox` (Eq 12) | `total-multiplication-willard-paradox` | **was mis-specialized** (unbounded `exists d`); **repaired** to `exists d<z` this ADR |
| `V3` (Eq 14) | condition (C) builder / `subst-code` single-valuedness | **faithful** — literally Theorem 2.3 (C) |
| `V4` (Eq 15) | `total-multiplication-willard-v4-axiom` | **faithful** — bounded `<=` descent over `Y`, `h*<=h y*<=y z*<=z`; proof-compression axiom, not strict numeral descent |
| `V5` (Eq 16) | `total-multiplication-willard-v5-axiom` | **faithful** — `[FinAx4 AND a<=k AND Paradox] => exists x<z. SemPrf(BOT,x)`; `<z` encoded as `bounded-exists ... z` + explicit `lt` |
| `FinAx4(a)` | `finax4` atom + handler | **faithful** — recognizes the generated finite-system code (`FinAx4` test passes/rejects) |

## Findings

1. **One genuine defect, repaired.** JSL2 Eq (12) bounds the diagonal-locator
   witness `d` strictly below `z` (`EXISTS d<z`). The kernel encoded it with an
   *unbounded* `exists-form`, dropping the `d<z` bound. This is the only
   incorrectly-specialized definition the audit found. It is now encoded as
   `bounded-exists d z (and (lt d z) (and (Map ...) (SemPrfk ...)))`, mirroring
   the faithful `EXISTS x<z` encoding already used in V5's consequent (the
   grammar supplies only `<=`-bounded quantifiers, so a strict `<` bound is the
   `<=` bound conjoined with an explicit `lt` guard).
   - Red/green: `sjas-adr0142-paradox-bounds-witness-below-z`
     (`willard-sjas-test`) was red (tag `exists`, no `lt`, nil bound), now green
     (tag `bounded-exists`, bound `z`, `lt` present). The V-route installation
     and full-system bounded-axiom-query regressions stay green.

2. **No invented apparatus to remove.** `V3/V4/V5/Upsilon/Paradox/FinAx4`/`Subst`
   are all genuine JSL2 §3.2 objects. The revised ADR-0142 withdrew the first
   draft's blanket-removal claim; this audit confirms the withdrawal: each
   surviving builder corresponds to a specific JSL2 equation.

3. **`Map` is the single genuinely-missing executable piece.** The `willard-map`
   atom builds the formula but no checker handler decides it. Criterion 4
   implements `Map(a,k,d)` as a checked relation (`d` = code of the diagonal).

4. **De-duplication.** `willard-sjas/total-multiplication-willard-paradox` now
   delegates to the shared `sjas-boundary-axioms` source rather than
   re-implementing Eq (12), so the bounded encoding cannot drift between the two.

## Scope

This audit covers only the Type-M (multiplication) V-route apparatus that
Theorem 2.3 consumes. The Xtab/LEM and Tab-2 boundary fragments
(`boundary-arithmetic-basis-axioms`, `xtab-lem-witness-axioms`,
`tab2-rank2-witness-formula`) are out of ADR-0142's scope and unchanged.
