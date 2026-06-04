# SJAS Proof Predicate Core Evidence Removal

## Track 1 Slice

ADR-0073 Track 1, proof-code grammar, public code decoding, axiom membership,
substitution, and tableau proof checking.

## Problem

After the proof predicate was narrowed to formula-bearing tableau trees, the
public `tableau-proof/3` and `subst-prf/4` closures still constructed
auxiliary Proflog proof evidence for code readers, theorem-code decoding,
axiom-membership witnesses, and decoded proof trees. That evidence was not part
of the SJAS proof code supplied to the predicate. It was an implementation-side
trace adjoined to the Proflog query response.

Willard's ordinary-tableau `IS#_D(beta)` proof predicate requires a relation
over a system code, theorem code, and tableau proof code. The supplied proof
tree is sufficient evidence for the predicate. Reifying a second proof trace
for every byte-reader and helper relation is unnecessary proof machinery and
was also the source of public proof-predicate materialization blowups.

## Red Tests

The source audit was extended to reject returned proof evidence of the forms:

```text
(profiled willard-sjas-proof-check proof-read-proof theorem-read-proof decoded-proof)
(profiled willard-sjas-subst-proof-check proof-read-proof theorem-read-proof decoded-proof)
```

The U-Grounding `tableau-proof` selector was also changed to require that the
returned Proflog proof closes through `willard-sjas-proof-check` without
adjoining `sjas-ug-code-byte-cons`, `sjas-ug-code-canonical-byte`, or the
supplied `sjas-axiom` proof code into answer evidence.

A later red audit rejected `conda` in
`decode-reflected-proof-antecedent-formulaso`, because reflected axiom
antecedent reconstruction is part of `AxiomConj(s)` and must be an ordinary
relation over reflected-clause records rather than committed-choice fallback
decoding.

## Change

Added proof-free core companions for the proof-predicate path:

- compact and U-Grounding public code byte readers;
- syntax and proof formula-code decoders;
- `sjas-axiom` and structural non-axiom proof-code decoders;
- theorem-code negation;
- finite-system axiom membership;
- `AxiomConj` reconstruction;
- diagonal substitution and substituted-source antecedent reconstruction;
- finite byte-list disequality for beta scans.

`tableau-proof/3` and `subst-prf/4` now use these core relations. The returned
Proflog answer proof is a proof-predicate closure marker:

```text
(profiled willard-sjas-proof-check)
(profiled willard-sjas-subst-proof-check)
```

The object-level checks still execute: the code readers, axiom membership,
substitution relation, theorem negation, and structural tableau checker remain
in the relation. The change removes auxiliary proof-trace construction; it does
not replace the relation with a host-side acceptance summary.

`decode-reflected-proof-antecedent-formulaso` now decodes reflected Group-2b
antecedents through `decode-reflected-clause-formulao` directly.

The proof-facing object symbol decoder now separates fixed SJAS reserved
indexes from user symbol indexes by disjoint finite relations. A reserved
proof-predicate symbol such as `tableau-proof` can no longer also decode as a
generic structural `(sym n)` user relation.

## Verification

Focused selectors passing after the change:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.willard-sjas-test/sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates
lein test :only proflog.willard-sjas-test/sjas-proof-checker-rejects-legacy-proof-rule-tag-certificates
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-right-first-conjunction-tableaux
lein test :only proflog.willard-sjas-test/sjas-u-grounding-tableau-proof-checks-numeral-system-theorem-and-proof-codes
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
lein test :only proflog.willard-sjas-test/sjas-subst-prf-reconstructs-axiom-basis-without-system-registry
lein test :only proflog.willard-sjas-test/sjas-u-grounding-subst-code-computes-level1-fixed-point
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-generic-profiled-sidecar-certificates
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-rejects-answer-overlay-query-certificates
lein test :only proflog.willard-sjas-test/sjas-object-symbol-index-decoding-separates-reserved-and-user-symbols
git diff --check -- src/proflog/kernel/willard_sjas_profile.clj test/proflog/willard_sjas_test.clj
```

Durable slow probes:

```text
test-runs/sjas-subst-prf-core-20260604T043314Z.log
```

`sjas-subst-prf-uses-substitution-code-independently-of-theorem-code` passed:

```text
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.
elapsed 8:32.36 maxrss 367284KB
```

The public formula-bearing structural theorem proof probe was still running at
the time of this note:

```text
test-runs/sjas-public-formula-bearing-true-theorem-core-20260604T041844Z.log
```

It is active in the child JVM and should be monitored for completion rather
than treated as a pass.

## Remaining Work

The proof predicate is closer to the literature-level relation: it checks the
supplied tableau proof code without adjoining a second proof trace. Remaining
Track 1 work should continue auditing the actual proof-predicate path for
proof-producing helper relations, committed-choice search control, and
performance boundaries that prevent public structural theorem proofs from
finishing.
