# SJAS Relational Proof Dispatch

## Context

The ADR-0073 Track 1 audit found remaining proof-facing committed-choice
dispatch in the generic kernel and SJAS profile. These sites were not host
proof checkers, but they still selected proof-predicate branches by
implementation scheduling rather than by ordinary structural alternatives.

## Change

- Added ADR-0081.
- Added a red source audit requiring the generic kernel profile-dispatch path
  and the SJAS proof profile to avoid `conda`.
- Replaced the generic kernel theory-profile committed-choice dispatch with
  `conde` alternatives. ADR-0082 subsequently removed the intermediate
  nil/non-nil dynamic-var guard shape and made proof hooks callable default
  relations.
- Replaced SJAS public-code, proof-free code, axiom-membership, `tableau-proof`,
  and `subst-prf` committed-choice branch classification with ordinary `conde`.
- Removed `conda` imports from both implementation namespaces.

## Evidence

Red:

```text
sjas-proof-facing-dispatch-does-not-use-committed-choice
Ran 1 tests containing 4 assertions.
4 failures, 0 errors.
elapsed 0:26.44 maxrss 295712KB
```

Green focused selectors:

```text
sjas-proof-facing-dispatch-does-not-use-committed-choice
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
elapsed 0:45.03 maxrss 282296KB

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

Broad gates:

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

The preserved SelfCons durable probe was still alive during this work and was
not sampled or optimized further. This slice is Track 1 relational
internalization evidence, not a SelfCons runtime pass.

ADR-0082 supersedes the intermediate nil/non-nil guard shape used immediately
after ADR-0081. The current proof-hook shape is callable-default dispatch.
