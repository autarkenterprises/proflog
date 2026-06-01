# SJAS Tableau Proof Arithmeticization Specification

Date: 2026-06-01

## Scope

This document specifies the Track 1 target for full proof-machinery
internalization: an in-principle arithmeticized semantic-tableau proof predicate
for ordinary-tableau `IS#_D(beta)`.

The specification is intentionally independent of runtime tractability. An
implementation may be too slow to synthesize or materialize large proof objects,
but the semantic target is a relation over object-language numbers/codes, not a
host proof oracle. It is also not the Track 2 correspondence theorem for the
Proflog kernel. Track 2 may later prove that the kernel is an adequate shortcut,
but this document states what the shortcut must correspond to.

## Mathematical Inputs

Fix an ordinary semantic-tableau deductive apparatus `D` and a finite axiom set
`beta` whose members satisfy the `Pi*1` truth conditions required by Willard's
`IS#_D(beta)` construction.

The arithmeticized proof predicate receives:

- `s`: a system code.
- `t`: a theorem formula code.
- `p`: a proof certificate code.

The intended relation is:

```text
TabPrf_beta(s, t, p)
```

`TabPrf_beta(s,t,p)` holds exactly when `s` decodes to the finite
`IS#_D(beta)` system under evaluation, `t` decodes to a well-formed theorem
formula, and `p` decodes to a finite closed semantic tableau for the finite
formula set consisting of the decoded system axioms plus the negation of the
decoded theorem.

Equivalently, if `AxiomConj(s)` is the decoded finite conjunction of the
system's axiom basis and `Formula(t)` is the decoded theorem, then `p` must
encode a closed tableau for:

```text
AxiomConj(s) /\ not(Formula(t)).
```

For a consistency theorem, `t` is the code of the selected contradiction
formula. Thus `SelfCons_D(beta)` is represented by the assertion that no `p`
satisfies `TabPrf_beta(s, contradiction-code, p)` for the system's own `s`.

## Encodings

The specification assumes a natural base-64 byte-string Godel coding of terms,
formulas, finite systems, substitutions, and proof trees. The implementation may
present those byte strings as compact `code-N` terms or as U-grounding numerals
over `0`, `1`, `dbl`, and `add(_,1)`, provided the representation is injective
and preserves trailing zero bytes.

The proof encoding must satisfy Willard's conventional tableaux encoding
discipline: a proof tree with `J` function-symbol occurrences is assigned a
Godel number whose bit length is at least linear in `J` and, in Willard's stated
form, at least `5J` bits. This lower bound is the relevant intensional size
condition. The exact byte layout need not be Willard's curly-brace layout, but
the proof object must remain inspectable as a tree with labeled nodes and local
rule evidence.

Formula and proof codes may use fixed numeric symbol indexes. This is sound only
under one of two conventions:

- The finite signature is itself decoded from `s`, including symbol kind and
  arity.
- The symbol table is treated as a fixed Godel codebook, justified up to
  kind-preserving and arity-preserving signature isomorphism.

Under neither convention may a proof close because the host spelling of a
symbol has a special meaning.

Profile tags and other nominal markers are permitted only as finite constructor
tags selecting a fixed rule profile or fixed decoder branch. A tag may dispatch
to an arithmeticized relation, but it may not stand in for an extrasystemic
lookup of theoremhood, axiom membership, or proof validity.

## Object Relations

The arithmeticization is a family of bounded relations over codes. The names
below are schematic; the implementation may use different predicate names as
long as the relations are represented in the object language.

### Code And Syntax Relations

`ByteSeq(c, bs)` relates a public code term or numeral `c` to its finite byte
sequence `bs`.

`ReadTerm(bs, i, j, tau)` states that bytes `bs[i,j)` decode to term `tau`.

`ReadFormula(bs, i, j, phi)` states that bytes `bs[i,j)` decode to formula
`phi`.

`WffCode(c, phi)` abbreviates byte reading plus formula well-formedness.

These relations check tags, arities, binders, variable indexes, payload lengths,
and end positions. They are syntactic relations, not host parsers.

### System Relations

`SystemCode(s, profile, sig, beta, refl, fixed)` decodes the finite system
record:

- the selected SJAS profile;
- the finite signature or fixed codebook context;
- the finite `beta` sequence;
- reflected Group-2b clauses or their first-order expansions;
- fixed Group-0, Group-1, Group-2, and Group-3 axiom schemata instantiated for
  the selected profile.

`AxiomMember(s, f)` holds when `f` is one decoded member of the finite axiom
basis named by `s`. This is a structural membership relation over decoded
system bytes. It is not a generated host fact and not a registry lookup.

`AxiomConj(s, gamma)` states that `gamma` is the finite conjunction of all
decoded axiom members of `s`, in the fixed system-code order.

### Proof Tree Relations

`ReadProof(pc, pi)` decodes proof code `pc` into a finite proof tree `pi`.

Each proof-tree node contains:

- a formula code or decoded formula;
- a rule tag;
- zero, one, or more child nodes;
- bounded rule evidence, such as a selected ancestor index, selected literal
  index, witness term, substitution code, equality environment, or reflected
  clause index.

Tree navigation relations such as `Root`, `Child`, `Ancestor`, `Sibling`,
`Leaf`, and `Depth` are bounded by the proof-code length. They correspond to
Willard's Appendix-C style proof-recognition predicates such as
`FormulaTree`, `SentenceTree`, `IndexedSentence`, `Ancestor`, `Leaf`,
`Closure`, `Deduction`, and `SemPrf`.

## Tableau State

A proof checker can be specified either as a direct tree validator or as a
state-transition relation. The state-transition presentation is convenient for
matching the implementation.

```text
State(node, agenda, literals, env, sigma, disequalities)
```

The components mean:

- `node`: the current formula being expanded or closed.
- `agenda`: unexpanded formulas on the current branch.
- `literals`: saved branch literals available for complementary closure.
- `env`: scoped variables introduced by quantifiers.
- `sigma`: equality/unification substitutions accumulated on the branch.
- `disequalities`: stored branch disequality constraints.

The initial state for `TabPrf_beta(s,t,p)` is the root formula:

```text
AxiomConj(s) /\ not(Formula(t))
```

with empty agenda, literals, environment, substitution, and disequality store.

## Local Rule Relation

`Step(s, state, proof-node, child-states)` is the local validator for one proof
node. It is syntax-directed and bounded by the current decoded formula and
finite proof-node evidence.

The rule set includes the ordinary semantic-tableau rules needed by
`IS#_D(beta)`:

- Conjunction adds both conjuncts to the same branch.
- Disjunction branches into one child for each disjunct.
- Negated conjunction branches into the negated left and negated right cases.
- Negated disjunction adds both negated disjuncts to the same branch.
- Double negation removes two negations.
- Universal formulas instantiate a branch term according to the selected
  tableau profile's bounded instantiation discipline.
- Existential formulas introduce a witness term or parameter.
- Bounded quantifier variants expand according to their bounded guard formulas.
- Equality updates the branch substitution or closes when a reflexive
  disequality is reached.
- Disequality is stored, progressed structurally, or closes when equality of
  the same term pair is forced.
- Literal save records atoms and negated atoms for later closure.
- Complementary closure closes a branch containing `A` and `not(A)` after the
  branch equality environment is applied.
- Arithmetic/profile atoms close only through the arithmeticized relations of
  the selected SJAS profile, such as byte reading, numeral comparison,
  addition/doubling, code canonicalization, and fixed finite profile facts.
- `AxiomMember(s,f)` atoms close only through the decoded `SystemCode` and
  `AxiomMember` relations.
- `tableau-proof(s,t,p)` and `subst-prf(s,g,t,p)` atoms close only by invoking
  this same arithmeticized proof-checking relation and the arithmeticized
  substitution relation on the supplied codes.

Reflected procedure calls are admissible only if they are decoded from `s` and
are justified as finite macros for first-order clauses in the axiom basis. A
reflected call rule must therefore be equivalent to expanding the corresponding
decoded clause formula. It is not sound for the checker to consult the host
runtime's current clause table.

## Closure Relation

`ClosedBranch(s, branch, proof-leaf)` holds exactly when the leaf evidence
establishes one of the admissible closure conditions:

- a complementary pair of branch literals;
- decoded falsehood;
- reflexive disequality or equality/disequality contradiction after applying
  the branch substitution;
- an arithmetic/profile atom whose arithmeticized relation proves the closed
  branch condition;
- an `AxiomMember`, `tableau-proof`, or `subst-prf` atom closed by the
  corresponding arithmeticized object relation.

All closure evidence is part of the proof code or is reconstructible from the
decoded system code and bounded branch state.

## Definition Of `TabPrf_beta`

`TabPrf_beta(s,t,p)` is the bounded relation asserting:

1. `SystemCode(s, profile, sig, beta, refl, fixed)`.
2. `WffCode(t, T)`.
3. `AxiomConj(s, Gamma)`.
4. `ReadProof(p, Pi)`.
5. The root of `Pi` is the formula `Gamma /\ not(T)`, or an equivalent root
   representation fixed by the proof-code grammar.
6. Every internal node of `Pi` satisfies the local `Step` relation for its
   children.
7. Every leaf of `Pi` satisfies `ClosedBranch`.
8. Every byte interval, tree pointer, symbol index, formula reference, branch
   reference, witness reference, and substitution reference used by the proof
   node is bounded by the length of `s`, `t`, or `p` as appropriate.

This is the arithmeticized semantic-tableau proof predicate. It is a relation
over codes. It does not ask whether the host Proflog kernel can prove `T`, and
it does not ask whether any separately generated theorem registry contains
`(s,t,p)`.

## Soundness Theorem

If the standard model satisfies `TabPrf_beta(s,t,p)`, then the decoded proof
tree `p` is a closed ordinary semantic tableau for the decoded formula set
`AxiomConj(s) /\ not(Formula(t))`.

Proof sketch. By definition, `ReadProof` gives a finite tree whose root is the
target formula. Each internal node is accepted only by `Step`, and each case of
`Step` is one of the ordinary semantic-tableau decomposition rules or a
macro-rule proven equivalent to expansion of decoded finite first-order clauses
from `s`. Each leaf is accepted only by `ClosedBranch`, whose cases are standard
branch contradictions or arithmeticized decidable relations of the profile.
Induction over the finite proof tree gives that every open model of a parent
branch would induce an open model of at least one child branch. Since all leaves
are closed, no model satisfies `AxiomConj(s) /\ not(Formula(t))`. Therefore the
decoded system entails the decoded theorem.

## Encoded-Tableau Completeness

Conversely, every finite closed semantic tableau using the admitted rules for
the decoded system has some proof code `p` such that `TabPrf_beta(s,t,p)` holds.

This is not a claim that the logic is complete in the unbounded proof-search
sense. It is the representability claim needed for arithmeticization: once a
finite closed tableau exists and its local rule choices are fixed, the tree,
node labels, branch references, witnesses, substitutions, and closure evidence
can be serialized into the selected proof-code grammar, and each local checker
will accept the corresponding node.

## Representability In The SJAS Object Language

All relations above are primitive-recursive or bounded over the lengths of the
input codes and proof tree:

- byte extraction and fixed-radix shifting are bounded by code length;
- syntax decoding is bounded by byte intervals;
- tree navigation is bounded by proof-code length;
- axiom membership is bounded by the decoded finite system sequence;
- local tableau rule checking is bounded by formula and proof-node size;
- closure checks are bounded by branch length and finite equality/disequality
  state;
- reflected procedure-call checking is bounded by the decoded finite reflected
  clause list in `s`;
- substitution and fixed-point checking are bounded by source and target formula
  code lengths.

These bounds make the relations expressible in the U-grounding style used by
the SJAS profile. Multiplication need not be asserted as a total function; where
products or fixed-radix shifts are needed, they are represented by graph
relations on concrete numerals.

## Current Implementation Mapping

The current Proflog implementation is an executable approximation of this
specification and has been moving toward it slice by slice:

| Specification relation | Current implementation area |
| --- | --- |
| `ByteSeq`, public code reading | `sjas-formal-code-byteso`, compact and U-grounding code readers |
| formula-code reading | structural theorem-code decoders in `willard_sjas_profile.clj` |
| `SystemCode` and `AxiomMember` | `sjas-system-axiom-formulao`, `sjas-axiom-membero` |
| proof-code reading | `decode-proof-code-kindo` and proof-code byte tags |
| local tableau state | `sjas-proof-check-stateo` |
| local proof-program wrapper | `sjas-proof-check-programo` |
| top-level `tableau-proof` closure | `sjas-tableau-proof-closeo` |
| substitution and fixed points | `sjas-subst-code-anyo`, `sjas-subst-prf-closeo` |
| reflected calls | system-code-driven reflected clause relations |
| correspondence audit | `proflog.sjas-correspondence` |

The implementation may still contain performance shortcuts, focused fast paths,
or profile-specific proof-directed entry points. Such paths are acceptable only
when they are extensionally the same as the bounded relation above over the
accepted proof-code fragment, and when they do not consult host theorem,
axiom, symbol, or proof registries as semantic authorities.

## Non-Claims

This specification does not prove that the current Proflog kernel is equivalent
to the SJAS semantic-tableau apparatus. That is Track 2.

This specification does not make theorem-level equivalence sufficient. A proof
kernel that proves the same theorems using shorter, flatter, or otherwise
different proof objects may fail to preserve the self-reference-relevant
intensional structure.

This specification does not require byte-for-byte identity with one historical
Willard proof-code layout. It requires a natural, injective, inspectable
encoding that preserves the tableau tree and the proof-size lower bound needed
by the Type-A semantic-tableau arguments.
