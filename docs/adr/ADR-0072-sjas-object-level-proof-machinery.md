# ADR-0072: SJAS Object-Level Proof Machinery

- Status: in progress
- Date: 2026-05-21
- Branch: `adr-0072-sjas-object-proof-machinery`

## Context

ADR-0063 through ADR-0071 moved SJAS formula, system, and proof codes away from
opaque host labels and toward inspectable byte/base-64 and U-Grounding numeral
representations. The current implementation is non-vacuous: `tableau-proof/3`
and `subst-prf/4` consume code terms, decode proof certificates, and check the
decoded certificates through the Proflog kernel.

The remaining objective is stricter. Host-side computation is acceptable at the
source-compilation boundary, just as Proflog source syntax is compiled into
kernel-recognizable formulae. But arithmetic coding or decoding needed while
applying an object-language predicate must be performed by kernel-level
relations. In particular, a program written in SJAS must be composable with the
proof predicate; that is not true if the proof predicate depends on host-only
lookups or host-only theorem-target reconstruction during its own evaluation.

The current implementation still has several object-level proof machinery
boundaries:

- the active system's axiom formula is selected by a generated
  `:sjas/system-entries` registry entry rather than decoded from `system-code`;
- formal axiom citation for standard SJAS axiom groups no longer needs generated
  `axiom-member/2` facts: Group-0, Group-1, Tableau-0 Group-3, and Level-1
  Group-3 citations validate the encoded system header and decode the theorem
  formula; Group-2 beta citations and reflected Group-2b citations read the
  corresponding encoded sections from `system-code`. All of those paths still
  enter through the staged byte extractor, but generated `axiom-member/2` facts
  are no longer trusted by `sjas-axiom` proof-certificate checking;
- already-ground U-Grounding code terms use a deterministic Clojure entry
  shortcut before structural relation checking, which is acceptable only as an
  operational staging boundary if it does not become the semantic proof rule;
- validation that user-supplied beta axioms meet Willard's truth and formula
  class constraints remains delegated to the user.

## Decision

Implement object-level proof machinery in stages, keeping each stage covered by
tests that fail against the previous host boundary.

The first stage removes theorem-target registry shortcuts from proof-predicate
application. `tableau-proof/3` and `subst-prf/4` must always derive the negated
theorem target from the theorem-code bytes through the structural formula-code
decoder, including for generated axiom theorem codes. The proof evidence must
name this theorem-code decoding step so future regressions cannot silently
reintroduce generated theorem-target lookup.

The first stage also fixes the nominal boundary between source theorem queries
and structurally decoded theorem codes. Formula codes store binder indexes, not
arbitrary host nom identities. Source-side theorem queries therefore use the
same canonical code-nom table as the structural decoder, and the decoder builds
AST variable and quantifier nodes by enumerating that finite table with concrete
nom constants. Calling `nominal/tie` on a logic variable standing for a nom is
not sufficient: the resulting formula may print correctly after reification but
will not behave as the ground formula that proof certificates were generated
against.

The second stage removes the finite formula syntax registries. `wff/1`,
`delta-star-0-code/1`, `pi-star-1-code/1`, `sigma-star-1-code/1`, and
`neg-pair/2` no longer consult generated `:sjas/formula-*` or
`:sjas/neg-pair-entries` tables. They decode the supplied formula code into the
kernel's internal formula syntax, then apply the structural recognizer or
formula-complement relation. This is a narrower improvement than complete
object-level proof machinery: already-ground compact and U-Grounding code terms
can still enter through `ground-formal-code-term`, which extracts bytes in
Clojure before the relational formula grammar consumes those bytes. That
shortcut is host-side computation and remains a temporary operational boundary,
not a semantic account of reflection inside the SJAS object language.

The third stage makes beta axiom citation consume the beta section of
`system-code`. A proof certificate whose decoded proof is `sjas-axiom` now first
checks whether the theorem code's byte string is one of the encoded Group-2 beta
formulas. Only non-beta axiom citations fall back to generated axiom-member
metadata. This is deliberately partial: it internalizes the user-supplied beta
membership boundary enough for beta citation composition, but it does not yet
derive the full Group-0/1/2b/3 axiom basis from system-code and it still relies
on `ground-formal-code-term` to expose already-ground byte strings.

The fourth stage makes reflected Group-2b axiom citation consume the reflected
clause section of `system-code`. Reflected clause records encode relation index,
arity, and body formula bytes. The profile reconstructs the axiom formula
`forall x1 ... forall xn. body -> R(x1, ..., xn)` and compares it against the
theorem code modulo alpha-equivalence. This removes the generated
`axiom-member/2` dependency for user-supplied reflected clauses. At that stage
it still kept the same ground-byte extraction boundary as the beta path and
still left Group-0, Group-1, and Group-3 on generated fallback metadata.

The fifth stage makes fixed Group-0 and Group-1 axiom citation consume the
system-code header and decoded theorem formula instead of generated membership
facts. These axiom groups are fixed by the SJAS profile, so the system side only
needs to prove that the supplied code is an encoded SJAS system with a supported
profile tag. The theorem side decodes the formula code and compares it with the
fixed decoded axiom shapes. Compact formula codes canonicalize object numerals
as `num` payloads, so the first two Group-1 equations are recognized as their
canonical numeric code forms rather than as literal `add` and `dbl` syntax. This
removes generated fallback for Group-0 and Group-1, but still leaves Group-3,
ground-byte extraction, and code-level proof-tree checking open.

The sixth stage makes Tableau-0 Group-3 citation reconstruct the
self-consistency axiom from `system-code`. For compact systems the embedded
system and contradiction codes decode as `(code bytes)` terms; for U-Grounding
systems they decode as canonical `num` payloads with the code sentinel appended.
The profile accepts either representation and matches the decoded theorem
formula against `forall p. not tableau-proof(system-code, false-code, p)`. This
removes generated fallback for Tableau-0 self-consistency citations, but Level-1
Group-3 still requires an object-level reconstruction of the fixed-point
substitution skeleton before its generated fallback can be removed.

The seventh stage makes Level-1 Group-3 citation validate its fixed-point
skeleton from encoded code terms. The decoded final Group-3 formula must have the
Level-1 self-consistency shape. The embedded substitution-code argument is then
read as compact `(code bytes)` or U-Grounding sentinel `num` bytes, decoded as a
formula, and checked against the expected skeleton formula over the same
system-code term and a free `v0` placeholder. This removes generated fallback
for the remaining standard axiom group, while still leaving the ground byte
extractor, the generated fallback branch itself, and proof-tree checking for
later stages.

The eighth stage removes the generated `axiom-member/2` fallback from
`sjas-axiom` proof-certificate checking. A regression test injects a bogus
generated membership fact for the contradiction code and verifies that
`tableau-proof(system-code, false-code, sjas-axiom-code)` still fails. Removing
the fallback exposed a legitimate relational `subst-prf` path where code terms
arrive bound by core.logic rather than available to host ground extraction. The
staged byte reader therefore now falls back to the structural code relation when
host ground extraction fails, and relational axiom-citation paths walk code terms
through equality sigma before decoding. This preserves legitimate
proof-certificate composition without trusting generated axiom facts. It is
still not the final object-level reader: the host ground shortcut remains, and
ordinary `axiom-member/2` queries still close from generated facts.

Later stages must internalize, in order:

1. object-level `axiom-member/2` query evaluation over decoded system code with
   no generated host fallback;
2. removal or strict isolation of the host ground-code shortcut from
   proof-predicate semantics;
3. code-level checking of tableau proof trees against decoded formulas and
   axiom membership, instead of validating a decoded Proflog kernel proof term
   by calling `kernel/prove-programo`;
4. optional future validation that beta axioms are true and in the required
   Willard formula classes.

## Consequences

- This ADR deliberately does not treat the existing finite registry substrate as
  sufficient. It is a staging aid, not the end state.
- The proof predicates will become slower as registry shortcuts are removed.
  Correctness and composability with SJAS programs are the priority.
- The current branch may ship multiple commits before the full objective is
  complete. The active goal remains open until all proof machinery needed by
  object-language predicates is internalized or explicitly marked as a future
  enhancement approved by the user.

## Test Obligations

Tests must be red before implementation and then pass:

- `tableau-proof/3` over a generated theorem code returns proof evidence that
  includes structural theorem-code decoding, not only proof-code decoding;
- `subst-prf/4` over a generated theorem code returns proof evidence that
  includes structural theorem-code decoding;
- existing structural non-generated theorem-code tests continue to pass;
- formula syntax predicates succeed without generated formula/class/neg-pair
  registries;
- beta axiom citation proof evidence includes a system-code beta membership
  step, not only generated axiom-member metadata;
- reflected Group-2b axiom citation proof evidence includes a system-code
  reflected-clause membership step, not only generated axiom-member metadata;
- fixed Group-0 and Group-1 axiom citation proof evidence includes a decoded
  system/profile membership step, not generated axiom-member metadata;
- Tableau-0 Group-3 axiom citation proof evidence includes a decoded
  system/profile membership step, not generated axiom-member metadata;
- Level-1 Group-3 axiom citation proof evidence includes a decoded skeleton
  membership step, not generated axiom-member metadata;
- injected generated `axiom-member/2` facts are ignored by `sjas-axiom`
  proof-certificate checking;
- malformed certificates and wrong theorem codes remain rejected;
- focused tests record whether the remaining proof-predicate path still uses
  generated system/axiom registries, so later work can remove them.

## Exit Criteria

- `lein test :only proflog.willard-sjas-test/<new-focused-tests>` passes for
  each completed slice.
- Before merging this ADR, `lein test-proflog-sjas` and
  `lein test-proflog-sjas-slow` must pass and record runtimes.
- Before declaring the full active goal complete, a completion audit must show
  that each proof-predicate coding/decoding operation needed during predicate
  application is kernel-level and object-code driven, with only source
  preprocessing left on the host side.
