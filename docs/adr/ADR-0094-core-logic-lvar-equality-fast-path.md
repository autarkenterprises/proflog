# ADR-0094: Core.logic LVar Equality Fast Path

- Status: in progress
- Date: 2026-06-10
- Branch: `adr-0094-core-logic-lvar-equality`
- AAR: pending

## Context

JVM stack analysis of the running `subst-prf` negative-exhaustion durable
probe (three samples, all in the same loop) localized the dominant cost of
lvar-dense relational search to `LVar.equals`: the vendored implementation
reads the comparand's fields through keyword lookups (`(:id o)`), which
compile to a `KeywordLookupSite` dispatching through `LVar.valAt`'s
keyword-hashing `case` on every equality test. `LVar` equality is the
engine's innermost comparison — every substitution `find`, every
occurs-check node visit, `unify`'s var-var path. The full analysis,
original code, and proposal are recorded in
[LVar Equality Fast Path: Stack Analysis And Proposal](../log/2026-06-10-lvar-equality-fast-path-analysis.md).

ADR-0090's ground-term fast path removed walk/occurs work for ground
terms; this change cheapens the per-comparison cost for the variable-dense
terms that remain, the workloads 0090 could not reach.

Numbering: ADR-0093 is claimed by the parallel core.logic
canonical-regression-suite agent; this ADR takes 0094. Per the standing
rule, a core.logic patch carries its own ADR and AAR; per the 2026-06-10
doctrine, the change must preserve miniKanren's clean semantics.

## Decision

Add a type-hinted LVar-vs-LVar fast path to `LVar.equals` in both vendored
overlays (`core.logic-1.0.1`, mirrored to the opt-in `core.logic-1.1.1`),
using direct field access (`.-id` / `.-name`), and keep the existing
keyword-lookup `IVar` branch verbatim as the fallback for non-LVar `IVar`
implementors (the nominal subsystem), so cross-type comparisons are
untouched. `hashCode` already returns a construction-time cached field and
is unchanged.

Semantics are identical by construction: `LVar.valAt` is defined by the
case table `:id -> id`, `:name -> name`, so the fast path reads the same
objects the fallback returns and `identical?` yields the same booleans for
every input. Because the change is observationally equivalent, red/green
takes the performance-evidence form: a baseline/patched measurement pair
under identical machine conditions (the reniced durable probe running
throughout), plus semantic regression pinning.

## Test Obligations

- New focused regression `proflog.core-logic-lvar-equality-test` pinning
  the equality contract the patch must preserve: unique-var equality by
  identical id (fresh vars unequal, a var equal to itself and to its
  `with-meta` copy), named-var equality by identical name object,
  LVar-vs-non-IVar falsity, hash stability, and substitution-map key
  behavior (walk through a map keyed by the variable). Green before and
  after by design; it exists so any future equals change that breaks the
  contract goes red.
- Existing `core-logic-ground-walk-test` and
  `core-logic-occurs-check-test` regressions stay green.
- Performance evidence under identical load: the bisect probe
  `axiom-member` cases and three deterministic heavy-lane vars, measured
  immediately before and after the patch; both broad gates.
- The reniced durable probe continues untouched for the pre-change
  negative-exhaustion envelope; a post-change probe is launched after it
  completes (or after a recorded decision to supersede it).

## Exit Criteria

- Both overlays carry the same patch; the contract regression and both
  gates are green; the measurement pair is recorded in AAR-0094 with the
  delta stated plainly, including a null or negative result if that is
  what measurement shows.
