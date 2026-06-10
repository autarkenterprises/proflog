# SJAS Proof-Machinery Internalization Slices

Date: 2026-05-28
Branch: `adr-0073-sjas-correspondence-program`

## Context

The current "full proof machinery internalization" goal is not merely to
construct Godel codes for each dependency of the proof predicate. Codes are
necessary, but inert. Full internalization requires each dependency of
`tableau-proof(system-code, theorem-code, proof-code)` and related predicates
to be available as an SJAS object-language relation over those codes.

The Proflog kernel will still evaluate those relations. The critical boundary
is that the SJAS proof predicate must not escape into a meta-level proof
validator that decides whether a decoded host proof term is valid. Instead, the
kernel should evaluate ordinary object-language relations for syntax,
arithmetic, axiom membership, substitution, proof-code decoding, and tableau
step checking.

## Logical Slices

The work is deliberately split into logical slices so that each host-side
assumption can be isolated, tested red/green, and either internalized or
assigned to the Track 2 correspondence proof. The slices are:

1. **Code format slice.**
   Replace opaque labels or host hashes with inspectable Godel-code terms,
   first compact byte constructors and then U-Grounding numerals. This makes
   formulas, systems, substitutions, and proof certificates nameable inside the
   object language.

2. **Syntax slice.**
   Provide object-level predicates that recognize and classify encoded
   formulas, including `wff`, formula-class predicates, and `neg-pair`.
   Without this, a theorem code is just data, not something the SJAS can reason
   about as formula syntax.

3. **System-code slice.**
   Reconstruct the finite axiom basis from `system-code`, including Group-0,
   Group-1, beta, reflected Group-2b clauses, and Group-3. This closes the
   nominal registry gap: `axiom-member(S,F)` must mean that `F` occurs
   structurally in encoded system `S`, not that a host map says so.

4. **Proof-code slice.**
   Give proof certificates a grammar, encoder, and decoder. Proof objects must
   be inspectable trees whose constructors and payload evidence can be checked,
   not host Clojure values smuggled through the predicate boundary.

5. **Arithmetic slice.**
   Implement the U-Grounding arithmetic needed by code decoding, byte/numeral
   interpretation, relation-backed fixed-radix shifts, and proof-predicate
   helper relations. Arithmetic cannot be replaced by host-side computation
   where it affects the internal proof predicate.

6. **Substitution slice.**
   Internalize diagonal and fixed-point substitution over formula codes,
   including `subst-code/2` and `subst-prf/4`. This is essential for Level-1
   self-reference, where the system reasons about substituting a formula code
   into its own skeleton.

7. **Tableau-checker slice.**
   Check proof-code trees against formula-code trees: branch expansion,
   conjunction/disjunction handling, quantifier instantiation, equality and
   disequality progress, saved literals, branch closure, and proof-constructor
   size/shape evidence. This is the core replacement for a host proof-kernel
   validation shortcut.

8. **Reflected-procedure slice.**
   Recover procedure-call proof evidence from encoded reflected clauses in the
   system code, not from external runtime clause tables. This preserves the
   distinction between SJAS-internal axioms and ordinary Proflog context.

## Why Not One Fellswoop

The slices are ordered because each creates the semantic substrate needed by
later slices. If a monolithic proof predicate fails, the failure may be in code
injectivity, symbol-table reconstruction, syntax decoding, U-Grounding
arithmetic, substitution, axiom membership, equality closure, procedure-call
recovery, or the tableau rule itself. Collapsing all dependencies into one
relation would make red/green testing and correspondence analysis opaque.

Incremental internalization also gives Track 2 a precise boundary. Each removed
host shortcut reduces the burden of a future correspondence theorem. Each
remaining bridge must be named as a dependency and either internalized in Track
1 or justified by a proof-and-test correspondence in Track 2b.

## Current Process

The current process is therefore:

1. Identify one proof-predicate dependency that is still host-side, nominal, or
   only partially relation-backed.
2. Write a focused failing regression that disables the old shortcut or audits
   the proof evidence expected from the object-level path.
3. Implement the smallest object-language relation or proof-code constructor
   extension that makes that dependency explicit.
4. Verify with targeted SJAS tests first, then broader fast/extended gates when
   the semantic surface warrants it.
5. Record whether the slice is fully internalized, still a runtime
   tractability boundary, or deferred to the ADR-0073 correspondence proof.

This keeps the goal concrete: full internalization means the proof predicate is
an object-level relation over encoded systems, formulas, substitutions, proof
trees, and branch states. It is not satisfied by merely assigning codes to those
objects unless the relations that consume the codes are also internalized.
