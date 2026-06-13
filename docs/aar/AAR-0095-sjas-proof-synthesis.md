# AAR-0095: SJAS Proof Synthesis

- Date: 2026-06-13
- ADR: [ADR-0095](../adr/ADR-0095-sjas-proof-synthesis.md)
- Branch: `adr-0095-sjas-proof-synthesis`

## Outcome

Citation synthesis works: running the arithmeticized `tableau-proof/3`
relation with the proof-code argument a fresh object variable, through
`sjas/query-answers` with residual deferral disabled, binds that variable
to the canonical `sjas-axiom` citation certificate — for the Tableau-0
Group-3 code, the runtime *generates* (rather than checks) the Henkin
proof of its own consistency.

Getting there required two relational repairs and one refactor, localized
by the [interdev review](../interdev/2026-06-13-adr-0095-proof-synthesis-review.md)
(Codex, main worktree) and the smaller red tests it recommended. This AAR
concurs with that review; the [reply note](../interdev/2026-06-13-adr-0095-proof-synthesis-reply.md)
records the one point of refinement.

1. **Construct, do not read backward.** The first implementation built the
   certificate by running the presented-code reader `sjas-public-code-byteso`
   backward over the fixed axiom bytes. That reader is deliberately
   many-to-one — `code-byte-termo` reads byte numerals *arithmetically*, so
   non-canonical numerals decode to the same byte — and is therefore not a
   bijection. Run backward it produced a non-canonical, non-admissible
   `(app code-1590 b0 b1 b2)` (constructor chosen by hash order from the
   `code-functions` map, arity unrelated to the three bytes), which answer
   export silently dropped, yielding empty records. The synthesis branch now
   builds through the canonical compact builder `sjas-internal-code-termo`
   (`code-byte-build-termo`'s 64-entry table + `code-constructor-buildo`),
   verified to reproduce `(sjas/proof-certificate 'sjas-axiom)` exactly and
   as the single solution.

2. **Harden the reader's forward direction.** `code-argso` /
   `code-args-coreo` decoded a `code-N` term without relating the declared
   constructor `byte-count` to the actual argument count, so the malformed
   `(app code-2 b0 b1 b2)` read as a valid two-byte code. The running count
   is now threaded through the single decoding walk — perf-neutral on the
   forward (presented-code) path, no second traversal of large code payloads
   — so arity-mismatched terms are rejected.

3. **Share the destructure preamble.** `sjas-tableau-proof-destructureo`
   (the negated-atom walk to the three code arguments) was extracted from
   `sjas-tableau-proof-callo` and reused in the synthesis branch, so the
   single proof-code position does not drift between the checking and
   synthesizing branches (and the source audit still sees `callo` in the
   close wrapper).

The dedicated synthesis branch is kept distinct from the checking branch
(rather than merged) on purpose: its `sjas-synthesized-citation` evidence
marks an answer the runtime *generated* versus one it *validated* — the
distinction the SelfCons execution question turns on.

## Evidence

Red/green TDD. The reader-hardening unit test was red before the fix
(both readers accepted `(app code-2 b0 b1 b2)`, returning bytes
`(44 1 30)`); the canonical-builder contract test was green from the start
(documenting the relation synthesis relies on). After the repairs, the two
unit tests and the two end-to-end synthesis selectors are green:

```text
lein test :only \
  proflog.willard-sjas-test/sjas-internal-code-builder-yields-canonical-axiom-certificate \
  proflog.willard-sjas-test/sjas-compact-code-reader-rejects-arity-mismatched-terms \
  proflog.willard-sjas-test/sjas-tableau-proof-synthesizes-beta-axiom-citation \
  proflog.willard-sjas-test/sjas-tableau-proof-synthesizes-selfcons-citation
Ran 4 tests containing 9 assertions.
0 failures, 0 errors.
```

Broad gates on the worktree (pre-merge):

- `lein test-proflog-fast` — Ran 175 tests containing 691 assertions. 0
  failures, 0 errors (includes the ADR-0090/0094 core.logic regression
  contracts; my reader change leaves them unaffected).
- `lein test-proflog-sjas` (not-slow partition) — `:SELECTION
  proflog.willard-sjas-test 140 tests (not-slow)` / `:SUMMARY pass=993 fail=0
  error=0`. This carries every source-audit selector, so the `callo` refactor
  and the reader arity tie are validated against the structure audits.
- Slow lane, one JVM per var with a timeout (the established heavy-lane
  pattern; the two intractable `subst-prf` negatives excluded — see below).
  All tractable slow vars and both synthesis selectors pass, test-body times
  (focused-runner `:DONE`, excluding JVM startup):

  | Var | Result | Test body |
  |---|---|---:|
  | `sjas-tableau-proof-accepts-formula-bearing-true-theorem-certificates` | pass=6 | 7.97 s |
  | `sjas-structural-code-predicates-accept-non-generated-formula-codes` | pass=6 | 3.71 s |
  | `sjas-subst-code-computes-general-formula-code-substitution` | pass=6 | 3.05 s |
  | `sjas-subst-code-decodes-user-symbols-without-symbol-registry` | pass=3 | 2.60 s |
  | `sjas-subst-prf-uses-substitution-code-independently-of-theorem-code` | pass=3 | 7.62 s |
  | `sjas-tableau0-selfcons-negating-witness-separates-external-proflog` | pass=2 | 3.21 s |
  | `sjas-tableau-proof-synthesizes-beta-axiom-citation` | pass=2 | 5.20 s |
  | `sjas-tableau-proof-synthesizes-selfcons-citation` | pass=2 | 6.90 s |

Probe synthesis case (`lein run -m proflog.sjas-runtime-probe <profile>
synthesis`), one recorded run per profile, both binding the fresh proof
variable to the `sjas-axiom` certificate (`certificate-match=true`):

| Profile | records | match | query ms |
|---|---:|---|---:|
| `willard-sjas-tableau0` | 1 | true | 4908.3 |
| `willard-sjas-level1` | 1 | true | 11878.8 |

After merging current `main` (ADR-0093/0096/0097), re-validated on the merged
tree: `lein test-proflog-fast` (Ran 191 tests containing 1009 assertions, 0
failures — the +16 over the pre-merge count are ADR-0093's canonical
core.logic regressions, which run against this branch's ADR-0090/0094 vendored
overlay and `proflog.sjas-correspondence-test`, exercising the SJAS profile the
reader change touches), `lein test-proflog-extended` (Ran 73 tests containing
219 assertions, 0 failures), and both synthesis selectors (4/4 assertions).

### Pre-existing intractable negatives (unchanged by ADR-0095)

The two `subst-prf` negative selectors remain envelope-exceeders, as recorded
before this ADR (`TEST_RUNTIME_BASELINE.md`,
`SJAS_RUNTIME_BASELINE_2026-06-10.md`):

- `sjas-subst-prf-checks-selfcons-fixed-point-certificate` — third assertion
  is a negative `subst-prf` query (`subst-prf(s, s, group3, sjas-axiom)` must
  be `empty?`). Confirmed grinding ~85 min CPU at 101% on this run before it
  was stopped; baselines record 25:00+ and 137:53 CPU without completing.
- `sjas-subst-prf-rejects-selfcons-complement-axiom-certificate` — same
  negative-exhaustion shape.

These are `subst-code` negatives embedded in `subst-prf` negatives:
confirming non-provability forces exhaustion of the substitution search at
fuel 120, which is structural search width, not a constant factor. ADR-0095
touches neither `subst-prf` nor these tests; their two positive companions in
the fixed-point selector pass. Tractability is a successor-ADR concern
(subgoal tabling / Track 2a relevance prefilter), consistent with the
exhausted-constant-factor finding from the ADR-0090/0094 hotspot study.

Durable logs in this worktree's `test-runs/`:
`adr0095-greenrun-*.log`, `adr0095-gate-*-*.log`,
`adr0095-slowvar-*-*.log`, `adr0095-probe-synthesis-*.log`.

## Follow-up

- **Structural synthesis** (a fresh proof variable against the structural
  branch, making the node-by-node validator enumerate proof trees) is
  unaddressed here; its tractability and its products are successor-ADR
  material (direct Track 2a/2b comparison artifacts).
- **U-Grounding citation synthesis.** The builder emits canonical *compact*
  certificates; synthesizing into the U-Grounding representation for
  `:code-format :u-grounding` systems is not yet supported (such a system
  rejects `code-N` constructors), and is deferred.
- **`subst-prf/4` synthesis mirror** — the same fresh-proof-variable mode
  for the substitution-proof predicate.
- Upstream-ward, the compact-code arity tie is a local SJAS concern, not a
  core.logic change; nothing to propose upstream from this ADR.
