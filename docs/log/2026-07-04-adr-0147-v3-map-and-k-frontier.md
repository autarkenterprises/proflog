# ADR-0147: V3/Map correction and the real `K` frontier

Date: 2026-07-04. Branch `adr-0147-claude-step1-tree`.

## Why this continuation was necessary

The Claude continuation established an executable, public-checker BOT core:
the real `D*` closes against a concrete positive `SemPrf^k(code(Dk),p,z)`
premise. That result remains the base of this branch. It is conditional,
however: the premise must still be produced by a measured proof of the actual
diagonal. Auditing that construction exposed two source-level overclaims and a
third bound-selection error.

## Corrected prerequisites

1. **V3 was documented but absent.** ADR-0142, its equation audit, and the
   closure ledger all said the generated multiplication system contained V3
   (condition (C)). The source installed only V4 and V5. The reflected route now
   contains the exact JSL2 Equation (14) formula followed by V4 and V5. The
   regression inspects V3's three universal binders, implication, two
   `subst-code` antecedents, and equality conclusion, and confirms all three
   route axioms are reflected Group-2 records.

2. **`Map` had no executable relation.** The old “Map locator” test checked
   only `Subst(nbar,code(Dk))`; no test queried `willard-map`, and the profile
   had no handler for it. `Map(alpha,k,d)` now decodes the presented public
   formula code `d`, verifies the closed `D^k(alpha)` schema, follows its
   embedded `nbar` to the open `Gamma(g)` schema, compares the encoded `alpha`
   and `k`, and validates the real `Subst(nbar,d)` relation. The implementation
   is pure core.logic over encoded terms and is shared by public queries and
   structural leaves. A genuine tuple succeeds publicly; wrong `d`, `k`, and
   `alpha` tuples fail in the proof-free core relation.

The initial Map implementation redundantly revalidated the whole finite system
inside `Map`. JSL2 keeps that obligation in V5's separate `FinAx4(alpha)`
conjunct, and the duplicate decode was both semantically misplaced and costly.
It was removed. Goal ordering now compares the small `k` first and compares
`alpha` by exact formal-code bytes before decoding the embedded skeleton.

## Red/green evidence

- V3 red: missing public builder; green: 1 test, 12 assertions.
- Map red: genuine public locator returned false; green: 1 test, 8 assertions.
- Fast gate after both corrections: 275 tests, 2,353 assertions, status 0,
  max RSS 524,604 KB under `-Xmx1g`.
- Extended gate: 92 tests, 971 assertions, status 0, max RSS 612,084 KB under
  a separate `-Xmx1g` cap.

All runs used the detachable Proflog runner (`nice -n 10`, idle-class I/O,
hard timeout, durable log). No test JVM was left behind after a timeout.

## The newly exposed correctness frontier

The existing ADR-0142 implementation fixes the diagonal superscript at `k=1`
and uses `z = 2^(p+1)`, because `Log(z,1)=p+1`. That is not the JSL2 Theorem 3.5
instantiation:

- Definition 2.1 fixes a constant `K > 1`.
- V5 requires `k >= alpha`.
- Theorem 3.5 explicitly chooses `kbar = alpha + 1`.
- For proof code `p`, Equation (11) therefore needs a `q` satisfying
  `Log(q,kbar) > p`, i.e. a `kbar`-fold power-tower witness, not one power of
  two.

For the executable generated system, `alpha` is its public system code, so
`k=1` cannot close V5's `not(leq(alpha,k))` branch. The next correct slice is
therefore not merely “assemble the old step-4 tree.” It must:

1. set the diagonal's superscript to the public term `alpha + 1`;
2. encode and validate a symbolic `K`-fold tower bound without materializing
   the astronomical numeral, while preserving the same proof predicate and
   public tuple;
3. make `FinAx4` check the required multiplication/Q/V1-V4 basis rather than
   accept an arbitrary generated SJAS source; and
4. only then construct and measure the public proof of `Dk` that feeds the
   already-working Claude BOT-core continuation.

Until these hold, the concrete-P2 BOT core is valid conditional evidence, not a
full Theorem 2.3 closure.

## 2026-07-05 continuation: real `kbar`, symbolic tower, and source-aware FinAx4

The first two frontier items now have executable coverage. The generated
Theorem 3.5 instance uses `kbar = alpha + 1`; the structural arithmetic relation
proves `alpha <= alpha+1` over the public system code. A new symbolic
`tower-bound(k,top)` representation implements the identity
`Log(E_k(top),k)=top` inside the existing `SemPrf^k` bound checker, so the real
public `kbar` term is checked without iterating it or materializing the tower.
The real axiom-certificate boundary test passes with `top=p+1` and rejects
`top=p`.

`FinAx4` no longer accepts every well-formed generated system. It now requires
the total-multiplication profile tag and exact encoded beta membership of the
complete reflected multiplication equations, V3, and V4. Public red evidence
showed the old relation incorrectly accepted a same-profile shell and variants
missing V3 or V4; all three now fail while the generated complete system passes.
This is a necessary source-integrity correction, not yet the full literal
`Q+V1+V2+V3+V4` discharge: the explicit Q/V1/V2 deduction-modulo bridge remains
the next arithmetic-basis obligation.

The audit also found that the proof-producing arithmetic reader understood
`mul(left,right)` while the proof-free structural reader did not. A focused
red/green regression now proves that the structural checker accepts `3*4=12`
and rejects `3*4=11`. This branch is the one the eventual V1/V2 equations and
formula-bearing `Dk` tableau use.

Focused evidence (detachable runner, `nice -n 10`, idle-class I/O, `-Xmx1g`):

- corrected `kbar` and symbolic tower: 10 assertions across three selectors;
- source-aware `FinAx4` plus existing V-route: 7 assertions across two selectors;
- combined focused sequence: status 0, elapsed 4:47.80, max RSS 877,044 KB;
- proof-free multiplication equality: red on the true product, then green with
  2 assertions, elapsed 0:32.14, max RSS 315,160 KB.

The first fast gate caught a reserved-code ordering regression: placing
`tower-bound` before the established `pow` slot made a pow-only legacy system
compact `pow` into the new global index, which decoded as `tower-bound`.
Preserving `pow`'s slot and appending `tower-bound` fixed both affected
proof-facing regressions; the two old pow selectors and the real tower selector
then passed together (5 assertions, status 0).

Final checkpoint gates were green under separate `-Xmx1g` caps: fast 275
tests/2,353 assertions in 7:59.46 (max RSS 716,432 KB), and extended 92
tests/971 assertions in 3:52.69 (max RSS 566,204 KB). The long fast-gate slice
was sampled in `fitting-fidelity-test` and completed normally; it was not an
SJAS proof-search stall.
