# ADR-0073 Track 1 Completion Audit

## Scope

This audit covers only ADR-0073 Track 1: direct arithmeticization of the SJAS
proof predicate. It does not start Track 2 relevance or correspondence work.

The standard for this audit is the current Track 1 specification:
[SJAS Tableau Proof Arithmeticization Specification](../SJAS_TABLEAU_ARITHMETIZATION_SPEC.md).
The concrete MVP endpoint remains public `tableau-proof/3` acceptance for the
ordinary-tableau Group-3 SelfCons certificate `(s,t,p)`.

## Requirement Status

| Requirement | Current evidence | Status |
| --- | --- | --- |
| Public code terms are inspectable compact byte terms or U-Grounding numerals, not opaque labels. | Compact and U-Grounding code-reader selectors cover trailing zero preservation, noncanonical byte numerals, bound U-Grounding byte-cons decoding, and public compact reader source shape. ADR-0083 requires `code-argso` and `code-args-coreo` to parse presented byte numerals through `code-byte-termo`. | Implemented and tested. |
| Formula syntax predicates decode formula codes through object relations. | `wff`, formula-class, and `neg-pair` tests run with compact and U-Grounding codes, including no source-symbol-registry variants. | Implemented and tested. |
| `axiom-member/2` and `AxiomConj(s)` are reconstructed from encoded `system-code`. | Fixed Group-0/1, beta, reflected Group-2b, Tableau-0 Group-3, and Level-1 Group-3 relations decode system bytes. Source audits reject generated fact registries and proof antecedent registries. ADR-0084 additionally verifies that structural proof predicates read `system-code` through branch equality state before reconstructing `AxiomConj`. | Implemented and tested. |
| Proof-code grammar is inspectable and excludes legacy proof-rule traces for Track 1 structural certificates. | Formula-bearing proof nodes, byte payloads, wide proof nodes, U-Grounding canonical-byte evidence, sidecar rejection, and proof-rule tag exclusion tests cover the current proof-code grammar. | Implemented and tested. |
| U-Grounding arithmetic is evaluated by relations inside the SJAS profile. | Byte/numeral readers, canonical byte-cons proof evidence, arithmetic closure, false positive arithmetic closure, substitution, and U-Grounding proof certificate selectors exercise relation-backed arithmetic. | Implemented and tested. |
| `subst-code/2`, `subst-prf/4`, diagonal substitution, and fixed-point comparison are internalized over formula codes. | General substitution-code tests, U-Grounding fixed-point tests, `subst-prf` system-code validation, substituted-source antecedent tests, and Level-1 SelfCons fixed-point selectors cover this path. | Implemented and tested. |
| Formula-bearing tableau proof checking validates proof trees through local branch expansion and closure relations. | Focused selectors cover conjunction, disjunction, implication, negated connectives, quantifiers, bounded quantifiers, equality, disequality, arithmetic closure, complementary literals, recursive proof-predicate leaves, and no runtime-fuel consumption. | Implemented and tested. |
| Reflected procedure calls recover proof evidence from encoded `system-code`, not host runtime clause tables. | Positive, negative, multi-alternative, guarded-body, guarded-scope, equality-triggered, no-registry, and external-runtime-clause rejection tests cover reflected calls. The `l-ground` guard in this path is `support/l-ground-term*o`, a structural relation over decoded object terms, not a host predicate. | Implemented and tested. |
| Proof-facing shortcuts are absent from the public proof predicate. | `sjas-profile-source-audit-rejects-host-proof-checker-route` rejects host proof-kernel validation, host byte projectors, generated registries, marker summaries, direct ground profile entrypoints, committed choice, optional nil proof hooks, and separate proof-code re-read branches. Last focused rerun: 128 assertions, 0 failures, elapsed 0:41.23. | Implemented and tested. |
| Occurs-check stack safety is in the default test/runtime path. | Project source paths load the vendored core.logic 1.0.1 overlay first; the overlay marker reports `vendor/core.logic-1.0.1/src stack-safe-occurs-check`; focused occurs-check test passed earlier under ADR-0075. | Implemented and tested. |
| Public SelfCons MVP selector accepts concrete current-source `s`, `t`, and `p`. | Current-source focused selector `proflog.willard-sjas-test/sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate` passed: 8 assertions, 0 failures, elapsed 2:17.71, maxrss 992768KB. The older durable pre-repair probe PID `34144` was left running as instructed and is no longer the completion evidence. | Implemented and tested. |

## Current Classification

The current source state satisfies ADR-0073 Track 1 by the specified public
SelfCons MVP endpoint. The proof predicate validates the concrete
`tableau-proof/3(s,t,p)` Group-3 SelfCons certificate through the
formula-bearing structural checker and object code-reader relations. No known
proof-facing host shortcut remains in the audited path.

Track 1 can be marked complete for the executable ordinary-tableau predicate
only to the extent that the implementation accurately forms the literature
`IS#_D(beta)` axiom basis and SelfCons fixed point. Later-discovered defects in
that formation are Track 1 defects, not Track 2 correspondence work. Track 2
work concerns explicitly modified deductive apparatuses or variants built on
top of, or in comparison with, this predicate shape.

## Commands And Evidence

Focused and broad evidence after ADR-0085:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
Ran 1 tests containing 128 assertions.
0 failures, 0 errors.
elapsed 0:17.19 maxrss 239172KB

lein test-proflog-fast
Ran 167 tests containing 659 assertions.
0 failures, 0 errors.
elapsed 1:56.85 maxrss 407724KB

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
elapsed 4:34.15 maxrss 606528KB

git diff --check
clean
```

Track 1 SelfCons evidence:

```text
sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
elapsed 1:08.67 maxrss 667564KB

sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:23.34 maxrss 989076KB

sjas-proof-check-accepts-formula-bearing-selfcons-tableau
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:20.39 maxrss 1036916KB

sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
elapsed 2:17.71 maxrss 992768KB
```

Live public SelfCons probe:

```text
PID: 34144
Selector: proflog.willard-sjas-test/sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
Log: test-runs/selfcons-public-track1-current-20260609T014154Z.log
Exit: test-runs/selfcons-public-track1-current-20260609T014154Z.exit
Last observed: live at 2026-06-09T02:09:05Z, elapsed 27:10, namespace header only
```

Post-ADR-0084 update:

```text
Observed: 2026-06-09T02:30:41Z
Elapsed: 48:46
Status: live, namespace header only, no exit file
Additional evidence: ADR-0084 focused selectors passed; fast gate passed in 3:38.29; extended gate passed in 8:46.17
```

Long-run monitoring update:

```text
Observed: 2026-06-09T06:28:49Z
Elapsed: 4:45:03
Status: live, namespace header only, no exit file
Related core probe: test-runs/selfcons-core-post-adr79-20260609T002713Z.log exited 1 after 2:08:37 with Java heap space OutOfMemoryError, maxrss 4447844KB
Audit result: no further proof-code reader correctness gap was found; the remaining `sjas-public-code-bytes-coreo` call sites are top-level public entries or are fed by walked-code wrappers.
```

Historical JVM diagnostic for the old durable probe:

```text
Observed: 2026-06-09T06:30:47Z
Diagnostic: test-runs/selfcons-public-track1-current-20260609T063029Z-diagnostic.log
Test JVM: PID 34205, elapsed 4:48:47, CPU 94.6%, RSS 351216KB
Classpath evidence: vendor/core.logic-1.0.1/src appears before the core.logic jar
Heap: G1 total 243712K, used 182163K
Top-stack shape: main thread is runnable in repeated core.logic walk* traversal
Interpretation at the time: the durable public MVP probe was using the revised core.logic path and was CPU-bound in relational traversal, not blocked on JVM heap exhaustion.
```

## Next Step

Return to ADR-0073 Track 2 work. Further optimization should remain subordinate
to preserving the internalized/arithmeticized proof predicate established here.
