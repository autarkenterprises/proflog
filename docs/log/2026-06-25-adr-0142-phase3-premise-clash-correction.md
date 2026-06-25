# ADR-0142 Phase 3 (premise-clash): refuting Dk is fully relational, no new rule

Date: 2026-06-25
Builds on / CORRECTS: [Phase 3 step-5 Q-disproof](2026-06-25-adr-0142-phase3-step5-qdisproof-assembly.md)

The previous step named **witness-providing gamma-instantiation** as the remaining
not-Dk obstruction and added a **subst-by-evaluation** close rule. This step shows
both were wrong turns: the checker already binds a universal's instantiation
witness by a **complementary-literal clash against a positive premise**, fully
relationally, so refuting `Dk` needs no new rule -- and the interim subst-eval rule
is reverted because it cannot be made terminating without an impure host guard the
relational-checker discipline forbids.

## The correction

Refuting `Dk = forall h y z. Subst(nbar,h) => not SemPrf^k(sys,k,h,y,z)` does not
require a *chosen ground witness*. The standard free-variable tableau suffices:

1. the `forall` rule instantiates with a FRESH branch variable `v`;
2. that `v` is **bound by unification** when a leaf mentioning it closes by a
   complementary-literal clash against a positive premise on the branch.

So the not-Dk tree refutes `P1 ^ P2 ^ Dk`, the premises supplying the witnesses:

- `P1 = Subst(nbar, code(Dk))` (Eq 7, checker-accepted at step 2) -- clashes the
  gamma branch `not Subst(nbar, v_h)`, binding `v_h := code(Dk)`;
- `P2 = SemPrf^k(code(Dk), p, 2^(p+1))` -- clashes `not SemPrf^k(code(Dk), v_y,
  v_z)`, binding `v_y := p`, `v_z := 2^(p+1)`.

These are exactly Willard's "instantiate Dk with (p,q,r)": (p,q,r) come from the
premises, not from a new gamma rule.

**Verified** (`proflog.sjas-not-dk-qdisproof-test`):
- `premise-clash-binds-the-universal-witness` (fast): over `forall h. not
  Subst(1,h)`, the premise `Subst(1,5)` binds the fresh witness `v0` to `5` and the
  refutation closes; WITHOUT the premise the universal stays open.
- `wrong-premise-leaves-the-universal-open` (`^:slow`): `Subst(9,5)` cannot clash
  `not Subst(1,v0)` -- the clash respects the universal's fixed argument.

## Why the interim subst-eval rule was reverted

The previous step's `sjas-subst-code-structural-closeo` closed a `(neg Subst(s,t))`
leaf by *evaluating* the subst-code gate. On a **free** target `t` -- precisely the
universal-instantiation case, where `t = v_h` before the clash binds it -- the
decoder enumerates code bytes for `t` and does not terminate. The only guard that
stops it is a host `(project [t] (if (lvar? t) ...))`, but the SJAS profile-source
audit (`willard-sjas-test`) pins host `project` to exactly the two ground-compact
byte readers and requires *"the proof-checking path itself [to] carry no host
project guard"*. So subst-eval on the proof-checking path is incompatible with both
termination on the not-Dk tree and the relational-checker discipline. It is
reverted; the complementary clash is the relational replacement, and it binds the
free target without any host guard.

## Honest status

Step 5 stays `:partial`. `theorem23-closure-status` now records
`:phase3-premise-clash` under `:resolved-since-aar`, keeps
`:step5-premise-clash-binds-universal-witness` in `:checker-accepted` (dropping the
reverted subst-eval keys), and the open boundary is a SINGLE item: the cut-free
combination trees for steps 1/3/4/B. Step 5 (not Dk) reduces to refuting
`P1 ^ P2 ^ Dk`, whose only missing input is `P2` = the bounded proof of `Dk` =
STEP 4. The genuine remaining work is the combination trees that prove `Dk`.

## Gates

No-regression check (the kernel is back to fully relational; the subst-eval rule
and its guard are removed):

- SJAS not-slow: **pass=1447 fail=0 error=0** -- including the profile-source audit,
  whose host-`project` count is back to the pinned 2.
- fast: **273 tests / 2350 assertions, 0 failures**.
- the `^:slow` `wrong-premise-leaves-the-universal-open` returns false in ~66s.
