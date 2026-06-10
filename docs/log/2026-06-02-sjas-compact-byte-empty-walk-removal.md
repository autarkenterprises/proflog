# SJAS Compact Byte Empty Walk Removal

Date: 2026-06-02
Branch: `adr-0073-sjas-correspondence-program`

## Performance Slice

While the long SJAS self-consistency proof-predicate validation was running,
stack samples alternated between equality walking and compact public-code byte
decoding. The compact byte reader was paying for
`(equality/walko term '() walked)` before matching the byte numeral shape.

That walk has no semantic work to do in this relation. Compact code byte
arguments are parsed without a substitution state, and the only accepted terms
are public U-Grounding numerals built from `0`, `1`, `dbl`, and `add`. With an
empty substitution, an accepted constructor term walks to itself; unbound
`var`/`par` roots cannot satisfy the following numeral-shape clauses anyway.

The change removes that redundant empty-substitution walk and matches the same
numeral clauses directly against `term`. This is not a host-side projector or a
mode-specific shortcut: the relation still interprets bytes through object
U-Grounding numeral structure and the finite byte relation.

The same performance pass also moved the public root-shape checks in
`bits->canonical-termo` before recursive canonical numeral construction. This
lets mismatched ground terms fail at the current root before building the tail
numeral proof. Generation remains relational: once the root shape is supplied
or synthesized, the tail numeral is still produced by the same recursive
object-level relation.

## Red Evidence

The focused source audit failed before the implementation:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route

FAIL in (sjas-profile-source-audit-rejects-host-proof-checker-route)
compact code byte decoding has no substitution state and must parse numeral structure directly
expected: (not (re-find #"equality/walko term '\(\) walked" profile-source))
  actual: (not (not "equality/walko term '() walked"))

FAIL in (sjas-profile-source-audit-rejects-host-proof-checker-route)
canonical byte generation must reject mismatched public roots before recursive numeral construction
expected: (not (re-find #"bits->canonical-termo tail tail-term tail-proof\)\n\s+\(== \(list 'app 'dbl tail-term\) term\)" profile-source))
  actual: (not (not "bits->canonical-termo tail tail-term tail-proof)\n       (== (list 'app 'dbl tail-term) term)"))
```

## Verification

Focused green checks:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 67 assertions.
  0 failures, 0 errors.

lein test :only proflog.willard-sjas-test/sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.
```

Local direct relation timing for the noncanonical byte-one term changed from
approximately `191ms` before the empty-walk removal to `154ms` after that patch,
then to `149ms` after the canonical root-shape reordering. Each probe returned
the same single byte answer `(1)`. This is only hotspot evidence; the full large
certificate validation still requires focused end-to-end confirmation.

## Rejected Follow-Up

Stack sampling of a focused Group-3 axiom-citation selector showed additional
time in `bits->canonical-termo`. A tempting follow-up was to parse compact
ground byte terms before enumerating the finite byte relation. A separate
candidate relation improved the direct noncanonical decode probe to about
`63ms`, but it did not return promptly in byte-term generation mode and timed
out under a 180 second cap. An interleaved variant also timed out in generation
mode and gave no decode improvement in the probe.

Those candidates were not implemented. The current finite-byte-first structure
therefore remains intentional: it bounds public byte generation, even though it
can pay extra canonical-fallback cost while rejecting wrong byte candidates in
ground decoding mode.

The current long self-consistency diagnostic was started before this patch, so
its eventual result remains useful as pre-patch long-run evidence but cannot
measure this optimization.

## Long-Run Evidence Caveat

The earlier focused
`sjas-selfcons-demonstration-uses-substantive-proof-targets` run exited
nonzero after `7:10:21` with `4480452KB` max RSS. The captured terminal tail
reported only `Tests failed`; it did not include the failing assertion or
exception, and `.lein-failures` was empty. That result therefore means only
that the pre-optimization implementation failed to carry the selector to a
green result after a very expensive `tableau-proof/3` validation/materializing
run. It is not evidence of a semantic counterexample, SJAS unsoundness, or
current-source failure.

Two later long probes, a current-source self-consistency diagnostic started
before the byte-reader changes and a focused Group-3 axiom-citation selector
started before the canonical root-shape reordering, remained CPU-active for
hours. After the session handle transition their PTY handles were unavailable
and the worker PIDs were gone; no terminal tail, `.lein-failures` entry, or
target artifact was recoverable. Their final pass/fail state is therefore
unknown and must not be cited as verification.

Future SJAS probes expected to run for hours should be detached or run in
`tmux`, and must write stdout/stderr plus `/usr/bin/time` output to
`test-runs/` with a saved PID. This preserves pass/fail evidence and lets later
sessions resume monitoring with `ps` and `tail` instead of depending on
tool-owned PTY handles.
