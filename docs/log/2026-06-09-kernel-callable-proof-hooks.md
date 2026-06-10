# Kernel Callable Proof Hooks

## Context

After ADR-0081 removed committed-choice dispatch, the kernel still represented
two proof-facing hooks as optional host values:

- `(or *recursive-prove-stateo* prove-stateo)`;
- `(if-let [closeo *theory-profile-closeo*] ...)`;
- nil/non-nil tests for `*theory-profile-closeo*` inside `close-agendao`.

This was not a proof-search optimization issue. It was a Track 1 dispatch
cleanup: the proof-facing relation should not need host optional selection to
decide which relation is being invoked.

## Change

- Added ADR-0082.
- Added a red source audit rejecting host optional dispatch in the kernel proof
  hooks.
- Added callable default relations:
  - recursive dispatch delegates to `prove-stateo`;
  - theory-profile dispatch fails.
- Simplified `close-agendao` so theory-profile closure and generic kernel
  closure are ordinary `conde` alternatives.

## Evidence

The corrected audit failed red before implementation:

```text
kernel-proof-hooks-avoid-host-optional-dispatch
Ran 1 tests containing 3 assertions.
3 failures, 0 errors.
elapsed 0:29.50 maxrss 276368KB
```

After implementation:

```text
kernel-proof-hooks-avoid-host-optional-dispatch
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.
elapsed 0:35.72 maxrss 305108KB

sjas-proof-facing-dispatch-does-not-use-committed-choice
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
elapsed 0:37.60 maxrss 299012KB

lein test proflog.kernel-test proflog.tabling-test
Ran 26 tests containing 46 assertions.
0 failures, 0 errors.
elapsed 1:24.94 maxrss 336536KB

lein test proflog.robinson-q-test
Ran 15 tests containing 123 assertions.
0 failures, 0 errors.
elapsed 1:09.63 maxrss 321460KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 1:01.92 maxrss 382876KB
```

Broad gates:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 2:48.69 maxrss 470980KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 7:00.68 maxrss 679180KB
```

## Notes

ADR-0082 supersedes ADR-0081's intermediate nil/non-nil guard shape. The
current kernel hook state is callable-default dispatch, not optional nil
selection.
