# Equality Bound Parameter Walk

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

## Performance/Correctness Slice

While the long SJAS self-consistency selector was running, stack samples showed
the active proof search spending time in `proflog.equality/lookupo` through
`walko`. The first safe optimization target was a semantic issue in bound
parameter walking.

`walko` already treated bound proof variables functionally: if `(var x)` is in
the equality substitution, walking follows the binding; only unbound variables
remain at the root. Bound rigid parameters, however, had an extra alternative:
the relation could follow the binding or leave `(par p)` rigid at the root.
That made existing equality state optional for parameters and opened extra
lookup/search branches during proof checking.

The fix is relational, not a host-side shortcut. The rigid-parameter branch now
uses the same `unboundo` guard used by variables, so a parameter is left rigid
only when it is not bound in the current equality substitution.

## Red Evidence

The focused regression failed before the implementation:

```text
lein test :only proflog.equality-test/bound-parameter-walk-is-deterministic
  expected: [(app zero)]
  actual: ((par a_0) (app zero))
```

## Verification

Focused green checks:

```text
lein test :only proflog.equality-test/bound-parameter-walk-is-deterministic
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.equality-test/lookupo-guards-skipped-nominal-key
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-proof-check-keeps-split-branch-state-independent
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

lein test proflog.equality-test
  Ran 15 tests containing 29 assertions.
  0 failures, 0 errors.
  elapsed 0:24.45 maxrss 277644KB

lein test-proflog-fast
  Ran 165 tests containing 654 assertions.
  0 failures, 0 errors.
  elapsed 2:20.11 maxrss 465488KB

lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
  elapsed 5:53.76 maxrss 658960KB
```

The still-running self-consistency selector was started before this change, so
its result is useful as pre-optimization long-run evidence but cannot measure
this fix.
