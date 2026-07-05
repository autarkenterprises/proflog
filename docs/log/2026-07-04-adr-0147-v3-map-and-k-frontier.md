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
