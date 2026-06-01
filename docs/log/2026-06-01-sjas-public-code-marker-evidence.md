# SJAS Public Code Marker Evidence

Date: 2026-06-01

## Context

After theorem-code decoding was made size-independent, the remaining large
public-code evidence boundary was the relation named
`sjas-public-code-bytes-summaryo`. That relation did not project host byte
vectors: it still called `sjas-formal-code-byteso` and therefore checked the
object-level compact or U-Grounding code reader. The problem was that the
function name and comment preserved the wrong abstraction. They described large
system/formula proof paths as summaries, which invites treating those paths as
semantic shortcuts rather than as uniform proof-evidence markers.

## Change

Renamed the relation to `sjas-public-code-bytes-markero` and rewrote the
docstring to state the invariant directly:

- the checked relation is always `sjas-formal-code-byteso`;
- no host byte-vector projection or registry lookup is used;
- the public proof evidence records the checked code-reader kind with
  `sjas-code-bytes` or `sjas-ug-code-bytes`;
- this is a proof-evidence representation choice, not a separate semantic
  code-reading relation.

All system-code and large formula-code paths that previously used the summary
name now use the marker relation. The source audit rejects the old summary
relation name so future work cannot reintroduce the ambiguous boundary.

## Verification

Red check:

- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
  - failed with the expected `sjas-public-code-bytes-summaryo` source-audit
    assertion before the rename.

Focused green checks:

- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
  - 1 test / 42 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-cites-fixed-axiom-groups-from-system-code`
  - 1 test / 8 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-cites-tableau0-group-three-from-system-code`
  - 1 test / 3 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-cites-level1-group-three-from-system-code`
  - 1 test / 3 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry`
  - 1 test / 3 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code`
  - 1 test / 3 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry`
  - 1 test / 4 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-proof-predicates-ignore-external-runtime-clauses`
  - 1 test / 3 assertions / 0 failures / 0 errors

- `lein test-proflog-sjas-focused`
  - `:SUMMARY pass=466 fail=0 error=0`
- `lein test-proflog-fast`
  - 164 tests / 653 assertions / 0 failures / 0 errors
- `lein test-proflog-extended`
  - 68 tests / 203 assertions / 0 failures / 0 errors

## Remaining Track 1 Boundary

This slice makes public code-marker evidence explicit and source-audited. It
does not remove the direct host-ground top-level profile predicate entrypoints
or the generic sidecar-hiding scheduling bridge. Those remain to be audited as
either proof-search scheduling outside the SJAS predicate or direct
internalization gaps.
