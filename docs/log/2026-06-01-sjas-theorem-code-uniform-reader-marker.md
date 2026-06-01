# SJAS Theorem-Code Uniform Reader Marker

Date: 2026-06-01

## Context

The direct `tableau-proof/3` and `subst-prf/4` proof-predicate paths had a
size-dependent theorem-code decoder. Small compact theorem codes returned the
full recursive byte-reader evidence, while large compact theorem codes used a
separate detail decoder and a byte-count threshold before returning summarized
read evidence.

That shape was a Track 1 internalization problem. The semantic proof predicate
should not change its code-reading relation because a theorem code crosses a
host performance threshold. A large Group-3 theorem code and a small beta axiom
theorem code must be accepted by the same object-level code relation; otherwise
the proof predicate has a size-sensitive implementation boundary that is not a
feature of the arithmeticized tableau apparatus.

## Change

Removed the theorem-code large/small threshold and the separate
`sjas-decode-proof-formula-code-detail-proofo` path. The proof-predicate
theorem decoder now always consumes the public code through
`sjas-formal-code-byteso`, decodes the resulting byte stream through the
object-level proof formula decoder, and returns a uniform code-reader marker:

- compact code terms cite `sjas-code-bytes`;
- U-Grounding numeral code terms cite `sjas-ug-code-bytes`.

This marker is deliberately size-independent. It records which object-level
code-reader relation was checked without forcing proof reification to expose
the entire recursive byte-read tree for every large theorem code.

## Rejected Alternative

I first tried to remove the summary evidence entirely and reify the full
recursive byte-read evidence for large theorem codes. That aligned with the
maximal-evidence ideal but was not operationally useful: the large raw proof
evidence and self-consistency selectors actively consumed CPU for several
minutes without materializing evidence. The uniform marker keeps the semantic
code-reading relation fixed while avoiding the host byte-count threshold that
selected a different proof path.

## Verification

- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
  - 1 test / 41 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-kernel-certificates`
  - 1 test / 6 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-subst-prf-checks-identity-substitution-certificates`
  - 1 test / 6 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/large-tableau-proof-raw-direct-evidence-materializes`
  - 1 test / 5 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets`
  - 1 test / 6 assertions / 0 failures / 0 errors
- `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-checks-structural-non-generated-theorem-codes`
  - 1 test / 4 assertions / 0 failures / 0 errors
- `lein test-proflog-sjas-focused`
  - `:SUMMARY pass=465 fail=0 error=0`
- `lein test-proflog-fast`
  - 164 tests / 653 assertions / 0 failures / 0 errors
- `lein test-proflog-extended`
  - 68 tests / 203 assertions / 0 failures / 0 errors

## Remaining Track 1 Boundary

This slice removes size-dependent theorem-code decoding inside proof
predicates. It does not remove the large-code evidence summaries still used for
system-code and formula-code scans in axiom membership and system
reconstruction. Those summaries still need to be either internalized by the
same uniform-reader-marker discipline or justified as non-semantic proof
evidence that does not change the relation checked.
