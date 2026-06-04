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

## Reflected-Call Resolver Cleanup

The same proof-evidence reduction was extended into the reflected Procedure Call
Rule used by the SJAS structural checker.

Changes:

- `decode-app-arityo`, `decode-syntax-app-arityo`, and
  `skip-syntax-app-arityo` now use ordinary finite `conde` recursion over the
  encoded arity byte rather than `conda`.
- Application decoders expose `(app sym args)` before parsing the argument byte
  list, so ground formula-bearing tableau nodes constrain the arity relation
  early.
- `code-byte-termo` no longer builds an unused `bits->canonical-termo` proof
  branch after reading a compact code byte. The compact byte numeral relation is
  the single byte-term path.
- `skip-formula-byteso` advances over beta formulas with
  `skip-syntax-formula-byteso`, avoiding materialization of decoded syntax trees
  whose contents are discarded.
- `sjas-system-reflected-call-clauseo`,
  `sjas-system-reflected-call-alternativeso`, and
  `sjas-system-reflected-guarded-call-alternativeso` now read `system-code`
  through `sjas-public-code-bytes-coreo`, because the formula-bearing tableau
  proof does not include separate byte-read proof evidence for these scans.

Observed focused timings while the durable public structural proof probe was
also running:

```text
sjas-public-code-bytes-coreo on the 23-byte demo system: about 51s
sjas-system-reflected-call-clauseo for structural demo(1): about 53s
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code: passed
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry: passed
```

The direct reflected-clause probe returned:

```text
([([a_0 (app 1)]) (eq (var a_0) (app 1)) (neq (var a_0) (app 1))])
```

This confirms the resolver is reconstructing the reflected clause body and its
negation from encoded system data rather than from the compiled host registry.

## Committed-Choice Removal

The remaining proof-machinery `conda` sites were removed.

Changes:

- `skip-syntax-term-byteso` and `skip-syntax-formula-byteso` now use ordinary
  `conde` grammar dispatch.
- `internal-formula-conjunctso` no longer uses committed-choice fallback.
  It flattens top-level `and` formulas and otherwise succeeds only through an
  explicit `internal-non-and-formulao` structural relation.
- `internal-leading-exists-scopeo` no longer uses committed-choice fallback.
  It strips leading `exists` binders and otherwise succeeds only through an
  explicit `internal-non-exists-formulao` structural relation.
- `sjas-beta-member-in-formula-byteso` now scans beta formulas by first
  structurally skipping the current encoded formula and then branching on
  explicit byte-list match versus byte-list disequality.
- The `conda` import was removed from the SJAS profile namespace; the remaining
  source occurrence is a comment explaining the explicit match/nonmatch
  replacement relation.

Test adjustment:

- `sjas-tableau-proof-accepts-axiom-citation-certificates` now asserts semantic
  acceptance and absence of leaked internal trace steps in the answer proof. Its
  duplicate broad contradiction-code rejection branch was removed; bogus
  axiom-member rejection remains covered by the dedicated injected-fact tests.

Focused verification:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-from-system-code
lein test :only proflog.willard-sjas-test/sjas-proof-predicates-check-reflected-calls-without-symbol-registry
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-negative-reflected-bodies
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies
lein test :only proflog.willard-sjas-test/sjas-subst-prf-reconstructs-axiom-basis-without-system-registry
lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-axiom-citation-certificates
```

## Compact Code Reader Mode Split

The durable public formula-bearing theorem proof probe launched at
`test-runs/sjas-public-formula-bearing-true-theorem-core-20260604T041844Z.log`
failed after `elapsed 4:34:05 maxrss 4483872KB`. The failure occurred while
materializing compact byte terms through `compact-code-byte-bits-termo`; the
public proof-predicate path was generating finite byte candidates before using
the presented object-language numeral shape to constrain them.

The red source audit required two properties:

- Public compact-code byte decoding must parse the presented numeral with
  `compact-code-byte-bits-termo` before consulting the finite `byte-bitso`
  relation.
- Embedded decoded-code payload reconstruction must not reuse that public
  reader path. It must use a separate byte-first builder path, because in that
  mode the byte value is already known from object-level syntax decoding and
  the public compact code term is being reconstructed.

Implementation:

- `code-byte-termo` is now the public-reader relation. It reads numeral bits
  first and then relates those bits to a byte value.
- `code-byte-build-termo` is the decoded-byte builder. It uses the known finite
  byte value first and structurally builds the compact public numeral term.
- `code-args-buildo` applies the builder over compact-code argument lists.
- `sjas-internal-code-termo` now reconstructs embedded public code payloads
  with `code-args-buildo` instead of the public reader.

This split keeps both paths object-level: neither path projects a ground code
term through a host byte decoder or generated formula registry. It is only a
mode split for the same finite byte/numeral arithmetic relation.

Focused verification:

```text
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
lein test :only proflog.willard-sjas-test/sjas-formula-codes-preserve-trailing-zero-embedded-code-payloads
lein test :only proflog.willard-sjas-test/sjas-syntax-predicates-decode-application-codes-without-symbol-registry
```

The direct public system-code probe returned the expected demo system bytes:

```text
((31 32 2 5 25 1 0 1 25 1 0 1 2 34 24 2 5 21 1 25 1 0 1))
"Elapsed time: 27856.096106 msecs"
```

Regression verification:

```text
lein test-proflog-fast
Ran 165 tests containing 656 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
```

A new durable public formula-bearing theorem proof probe was launched under
`test-runs/sjas-public-formula-bearing-true-theorem-term-first-byte-reader-*`.
It was still running during this slice and is evidence for the term-first
public reader change, not for any further Track 2 correspondence claim.

## Ordinary Tableau Negation Rules

The formula-code grammar admits surface `not` formulas, and the Track 1
specification names ordinary semantic-tableau rules for double negation,
negated conjunction, and negated disjunction. The structural proof checker had
already covered positive conjunction/disjunction, literals, quantifiers,
equality/disequality, arithmetic/profile closures, and reflected calls, but it
did not accept formula-bearing proof trees whose local node formula was one of
these negated compound forms.

Red tests were added for formula-bearing structural proof trees with no
proof-rule trace tags:

- `not(not(false))`, whose child is `false`;
- `not(and(true,true))`, whose two child branches are `not(true)`;
- `not(or(false,true))`, whose same branch continues through `not(false)` and
  then closes at `not(true)`.

Implementation:

- `not(true)` is a closed false-truth leaf.
- `not(false)` behaves as a true formula over the current branch and continues
  with the next unexpanded formula.
- `not(not(phi))` continues with `phi`.
- `not(and(left,right))` branches into independent `not(left)` and
  `not(right)` child branches.
- `not(or(left,right))` adds `not(left)` and `not(right)` to the same branch.

These cases are local structural tableau rules over decoded formula-bearing
proof nodes. They do not introduce proof-rule tags, host normalization, or a
call back to the Proflog kernel proof checker.

Focused verification:

```text
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-double-negation-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-negated-conjunction-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-negated-disjunction-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-disjunction-tableaux
lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
```

Regression verification:

```text
lein test-proflog-fast
Ran 165 tests containing 656 assertions.
0 failures, 0 errors.

lein test-proflog-extended
Ran 68 tests containing 203 assertions.
0 failures, 0 errors.
```
