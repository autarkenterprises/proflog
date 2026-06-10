# AAR-0082: Kernel Callable Proof Hooks

- Date: 2026-06-09
- ADR: [ADR-0082](../adr/ADR-0082-kernel-callable-proof-hooks.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Replaced host optional proof-hook dispatch with callable default relations in
the generic kernel. The recursive proof hook now defaults to a relation that
delegates to `prove-stateo`; the theory-profile hook now defaults to a relation
that fails. `recursive-prove-stateo` and `theory-profile-closeo` simply apply
the current relation value, and `close-agendao` tries profile closure and
ordinary kernel closure as ordinary `conde` alternatives.

This keeps optional scheduling infrastructure outside the object proof rules
without encoding the choice as host nil selection.

## Evidence

The focused audit initially exposed a syntax error in the new test edit; after
fixing that test, it failed red on the intended implementation patterns:

```text
kernel-proof-hooks-avoid-host-optional-dispatch
Ran 1 tests containing 3 assertions.
3 failures, 0 errors.
elapsed 0:29.50 maxrss 276368KB
```

After the callable-default change, the hook audit passed:

```text
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.
elapsed 0:35.72 maxrss 305108KB
```

Related focused checks passed:

```text
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

The post-change broad gates passed:

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

## Follow-up

Continue the ADR-0073 Track 1 audit. The callable hook defaults remove a
generic kernel dispatch boundary; they do not by themselves prove Track 1
complete.
