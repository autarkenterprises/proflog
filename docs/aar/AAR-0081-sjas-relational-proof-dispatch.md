# AAR-0081: SJAS Relational Proof Dispatch

- Date: 2026-06-09
- ADR: [ADR-0081](../adr/ADR-0081-sjas-relational-proof-dispatch.md)
- Branch: `adr-0073-sjas-correspondence-program`

## Outcome

Removed proof-facing committed-choice dispatch from the generic kernel
theory-profile path and the SJAS proof profile. The affected paths moved to
ordinary `conde` alternatives guarded by explicit structure such as public code
shape, decoded proof bytes, or finite system-code membership relations. The
generic kernel initially used nil/non-nil dynamic-var guards; ADR-0082 later
replaced those with callable default hook relations.

This was a Track 1 internalization cleanup, not a performance optimization.
The proof predicate now exposes proof-code, code-format, axiom-member,
`tableau-proof/3`, and `subst-prf/4` classification as ordinary relations
rather than using miniKanren committed choice to select one operational branch.

## Evidence

The focused audit failed red before implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-facing-dispatch-does-not-use-committed-choice
Ran 1 tests containing 4 assertions.
4 failures, 0 errors.
elapsed 0:26.44 maxrss 295712KB
```

After replacing the committed-choice sites, the same audit passed:

```text
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
elapsed 0:45.03 maxrss 282296KB
```

Focused proof-facing selectors stayed green:

```text
sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 0:46.15 maxrss 259096KB

sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 0:56.53 maxrss 412852KB

sjas-subst-prf-substitution-axiom-branch-validates-system-code
Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
elapsed 0:44.54 maxrss 246152KB

sjas-tableau-proof-accepts-axiom-citation-certificates
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.
elapsed 0:47.97 maxrss 320412KB

sjas-u-grounding-tableau-proof-checks-numeral-system-theorem-and-proof-codes
Ran 1 tests containing 10 assertions.
0 failures, 0 errors.
elapsed 0:45.36 maxrss 265752KB
```

The post-change broad gates passed:

```text
lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 3:01.86 maxrss 443836KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 7:14.99 maxrss 675088KB
```

## Follow-up

Continue ADR-0073 Track 1 arithmeticization. The preserved long SelfCons probe
remains runtime evidence only; this ADR did not start another optimization
thread.

ADR-0082 supersedes this ADR's intermediate generic-kernel nil-guard shape. The
current implementation uses callable default proof hooks.
