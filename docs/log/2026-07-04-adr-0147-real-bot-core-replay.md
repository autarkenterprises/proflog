# ADR-0147 real BOT-core replay: public checker closes D-star against concrete P2

Date: 2026-07-04. Branch `adr-0147-claude-step1-tree`.

## Result

The Claude step-4 BOT-core continuation is now incorporated into the ADR-0147
implementation line with a bounded, public-checker path:

- the real Theorem 2.3 diagonal `D* = forall y z. not SemPrf^k(code(Dk),y,z)`
  is generated through `theorem23-diagonal`;
- the proof tree is encoded as ordinary formula-bearing public proof nodes;
- the checker decodes the actual proof-node payloads and replays the branch
  state for conjunction, gamma, negated-atom, literal-save, and complementary
  literal closure;
- no source-side expected formula is rebuilt to justify the transition.

The key live probe now succeeds under a bounded heap:

```text
env JVM_OPTS='-Xmx2g' timeout 180 lein run -m proflog.sjas-step4-probe real
:REAL-CLOSURE-Dstar-and-concrete-P2 {:closes true, :elapsed-ms 789}
```

The focused regression is
`proflog.sjas-step4-bot-core-test/real-diagonal-dstar-closes-against-concrete-bounded-proof-premise`.

## Verification

- `env JVM_OPTS='-Xmx2g' timeout 300 lein test :only proflog.sjas-step4-bot-core-test/real-diagonal-dstar-closes-against-concrete-bounded-proof-premise`
  passed: 1 test, 1 assertion.
- `env JVM_OPTS='-Xmx2g' timeout 420 lein test proflog.sjas-theorem23-closure-test proflog.sjas-step4-bot-core-test proflog.sjas-tree-builder-test`
  passed: 7 tests, 36 assertions.
- Durable final fast gate `adr0147-real-bot-core-fast-final` passed under
  `-Xmx2g`: 275 tests, 2353 assertions, max RSS 934464 KB.
- Durable final extended gate `adr0147-real-bot-core-extended-final` passed
  under `-Xmx2g`: 92 tests, 971 assertions, max RSS 744060 KB.

## Why the previous run failed

The prior real-mass probe entered the relational checker and grew memory rather
than returning. A replay-only diagnostic reduced the failure to one concrete
transition:

```text
{:status :reject,
 :path [0 0 0],
 :reason :no-branch-formula-matches-proof-node}
```

The host replay had asked core.logic to return a decoded AST directly; core.logic
reified decoded nominal constants as public symbols such as `a_0`, while the
target branch still contained the shared `sjas-vN` nom objects. The fix decodes
only the internal indexed formula through the relation, then converts that
internal formula to AST in ordinary Clojure using `sjas-code/code-nom`. The replay
therefore compares the actual public proof-node formula against the same nom
objects used by the branch.

## Remaining obligation

This is BOT-core closure with a concrete bounded-proof premise. It does not by
itself construct the proof code for `Dk`, nor does it complete the full
Theorem 2.3 contradiction. The next mathematical obligation is to construct and
measure the bounded proof premise `P2 = SemPrf^k(code(Dk), p, 2^(p+1))` from the
step-4 proof of `Dk`, then use that measured object in the final closure target.
