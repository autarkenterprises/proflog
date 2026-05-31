# SJAS Proof Materialization Current Assessment

Date: 2026-05-31
Branch: `adr-0073-sjas-correspondence-program`

## Question

The earlier failure line

```text
Timed out while materializing full proof evidence.
```

needed reassessment after the guarded-call internalization work. The practical
choice was whether to let the large proof-evidence path run to completion or to
improve performance without changing the semantics of proof-predicate
acceptance.

## Finding

The timeout is not reproduced on the current public `tableau-proof/3` path. The
large self-consistency selector completed:

```text
timeout -k 10s 1200s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets

Ran 1 tests containing 6 assertions.
0 failures, 0 errors.
elapsed 1:10.65 maxrss 596252KB
```

The adjacent smaller structural theorem-code selector also completed with
ordinary proof evidence:

```text
timeout -k 5s 360s /usr/bin/time -f 'elapsed %E maxrss %MKB' lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes

Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
elapsed 0:41.11 maxrss 343344KB
```

## Cause

The original timeout was caused by asking `core.logic` to reify the full raw
internal proof-search state for a large Group-3 `tableau-proof/3` query. The
SJAS proof predicate could accept the certificate in truth mode, but forcing the
complete miniKanren proof term required materializing a very large internal
state, including code-reading and branch-checking structure that is operationally
much larger than the checked certificate tree.

That is a reporting/runtime materialization boundary, not evidence that the
arithmeticized proof predicate rejected or failed to evaluate the supplied
certificate.

## Correctness Boundary

The current public large-proof path keeps semantic acceptance relational. It
first requires `direct-negated-profile-closeo` to accept the ground
`tableau-proof/3` query through the SJAS checker. Only after acceptance does the
public API build a compact report from the already-supplied proof code:

```clojure
(profiled willard-sjas-proof-check
          proof-code-read-marker
          theorem-code-read-marker
          decoded-certificate-proof)
```

The report avoids reifying the raw miniKanren search state, but it does not
replace the proof predicate's semantic check. The decoded certificate proof tree
is still visible in public evidence, and the source audit continues to reject the
old host-kernel proof-checking route.

## Status

No implementation change was required for the current public timeout: the
current branch already runs the relevant selector to completion under the focused
testing envelope. Further optimization would need to target the private/raw
miniKanren reification artifact itself, not the public acceptance path, and
should be treated as a separate performance project because it is not needed for
correct SJAS proof-predicate acceptance.
