# SJAS Decidable Difference-Logic Fragment Survey

Date: 2026-05-19

This note records the first research pass on whether a decidable first-order
fragment, especially one based on quantified difference logic, can serve as the
language and axiom basis for a self-justifying axiom system.

Primary prompt source:

- Bernard Boigelot, Pascal Fontaine, and Baptiste Vergain, "Decidability of
  Difference Logic over the Reals with Uninterpreted Unary Predicates",
  arXiv:2305.15059, 2023, <https://arxiv.org/pdf/2305.15059>.

Related SJAS sources already used by the project:

- Dan E. Willard, "A Detailed Examination of Methods for Unifying,
  Simplifying and Extending Several Results About Self-Justifying Logics",
  arXiv:1108.6330, 2011.
- Dan E. Willard, "On the Significance of Self-Justifying Axiom Systems from
  the Perspective of Analytic Tableaux", arXiv:1307.0150, 2013/2014.

## Decidability Facts From Boigelot-Fontaine-Vergain

The paper studies first-order fragments that combine individual quantification,
weak arithmetic constraints, and uninterpreted unary predicates.

Important parameter values:

- uninterpreted predicate arity: unary only in the studied fragments;
- arithmetic domains: reals, integers, and mixed real/integer guards;
- arithmetic strength: pure order, integer difference constraints, or real
  difference constraints;
- quantification: first-order quantifiers over individual variables are allowed;
- interpreted integer predicate: `x in Z` is available in mixed fragments;
- higher-arity uninterpreted predicates: unrestricted first-order quantification
  with binary uninterpreted predicates is reported as immediately undecidable.

The core frontier:

| Fragment | Parameters | Decidability status | Relevance |
| --- | --- | --- | --- |
| `uf1.ro` | unary predicates plus order over reals | decidable, via known decidability of a universal monadic order fragment | Too weak/discrete-code-poor for direct SJAS coding, but relevant as a dense-order baseline. |
| `uf1.iro` | `uf1.ro` plus integer-membership atoms | included in decidable mixed fragment | Adds integer guards but no integer difference arithmetic by itself. |
| `uf1.idl.iro` | unary predicates, real/integer order, and difference constraints only between integer-guarded variables | decidable | Best candidate in this paper for a decidable first-order substrate with both order and discrete successor-like structure. |
| integer or natural difference logic with unary predicates | quantified, monadic predicates, discrete difference constraints | decidable; reduces to S1S/MSO over one successor | Best pure discrete candidate. Expressive power is regular/automata-like rather than arbitrary arithmetic. |
| `uf1.rdl` | unary predicates plus real difference constraints | undecidable, even with one unary predicate | Reject for decidable SJAS substrate. |
| `uf1.irdl` | integer and real difference constraints with unrestricted real difference part | not separately introduced because `uf1.rdl` is already undecidable | Reject if it contains unrestricted real difference constraints. |
| Presburger arithmetic plus one unary uninterpreted predicate | quantified linear integer arithmetic plus monadic predicate | undecidable, per cited prior results | Too strong when arbitrary unary predicates are mixed with full Presburger arithmetic. |
| real closed fields | quantified polynomial real arithmetic, no arbitrary predicates | decidable | Decidable as pure arithmetic, but wrong tradeoff for Willard-style SJAS and too strong if paired with arbitrary predicates. |
| unrestricted higher-arity uninterpreted predicates | binary or higher predicates plus unrestricted quantification | undecidable | Reject unless quantification or predicate use is sharply stratified. |

The paper also notes that adding constraints such as `x + y < 0` is outside the
decidability proof technique for the decidable real-order/mixed fragment. This
matters because full linear arithmetic is not a harmless extension of
difference/order fragments once unary predicates and quantifiers are present.

## Relation To Willard's Requirements

A candidate SJAS substrate is not merely a decidable satisfiability problem. It
must support:

1. a finite beta basis whose installed Group-2 axioms have checked `Pi*1`
   encodings;
2. a coding of formulas, finite systems, and proof objects;
3. object-language predicates corresponding to well-formedness, negation-pair,
   substitution, and proof checking;
4. a generated self-consistency sentence over those predicates;
5. a deduction apparatus `D` whose proof predicate can be internalized without
   reintroducing the strength that causes diagonalization.

This creates a tension. Decidable difference-logic fragments are attractive
because they are weak, but their weakness is also what makes Willard-style
arithmetized syntax and proof checking difficult.

In particular:

- quantifier-free fragments are decidable but cannot state the universal
  `SelfCons` sentence;
- dense real order with unary predicates lacks a natural discrete coding
  apparatus for finite syntax and proof objects;
- integer difference logic with unary predicates has a discrete order and a
  successor-like structure, but its definability is essentially automata/regular
  in character, not general primitive-recursive proof checking over binary
  Godel numbers;
- Presburger arithmetic has enough linear structure to be useful, but adding an
  arbitrary unary predicate already crosses into undecidability;
- unrestricted higher-arity uninterpreted predicates are unavailable if global
  decidability is required.

## Multiplication Parameter

The earlier `Pi*1` beta clarification remains important here. Universal
conditional use of a multiplication graph is not itself totality:

```text
forall x y z. Mult(x,y,z) -> Phi(x,y,z)
```

The dangerous form is existential totality:

```text
forall x y. exists z. Mult(x,y,z)
```

However, the decidable fragments surveyed by Boigelot-Fontaine-Vergain do not
include Willard's full U-Grounding multiplication graph as a native interpreted
relation. A decidable-difference-logic SJAS therefore cannot simply import the
current Proflog U-Grounding `mult/3` and proof-code decoder without rechecking
decidability. If `Mult` is left uninterpreted, the system loses the intended
arithmetized-syntax content; if it is fully axiomatized, it may leave the
decidable fragment.

## Candidate Deductive Apparatuses

### Semantic Tableaux Or Tab-1 Over Willard U-Grounding

This is the established Willard path. The 2013/2014 paper's finite
`IS#_D(beta)` result is stated for `D` as semantic tableaux or the `Tab-1`
construct. The same paper warns that stronger deduction methods can break the
consistency-preservation invariant, including Hilbert deduction and `Tab-2`.

This path is the best match to the current Proflog implementation, but it is
not a claim that the whole satisfiability problem for arbitrary beta/theorem
queries is decidable. It is a metamathematical consistency-preservation route,
not a decidable-SMT-fragment route.

### Automata/S1S Apparatus For Integer Difference Logic

Quantified integer/natural difference logic with unary predicates reduces to
S1S, the monadic second-order theory of one successor, and is decidable by
automata methods. A candidate SJAS here would have to make the proof predicate
native to this automata representation.

This suggests a possible new research direction:

- encode formulas/proofs as unary/word positions with monadic predicates for
  tags, delimiters, variable classes, proof-line classes, and local rule
  witnesses;
- restrict proof rules so each proof-checking condition is regular/MSO-definable
  over the word/successor structure;
- use the S1S/automata decision procedure, or a proof-carrying automata
  calculus, as `D`;
- state self-consistency against that automata-checkable proof predicate.

This would be a different SJAS, not a drop-in replacement for Willard's
U-Grounding tableau proof predicate. Its central open question is whether enough
logical syntax and substitution can be made regular/local without smuggling in
stronger arithmetic through auxiliary predicates.

### Automata On Linear Orderings For `uf1.ro` Or `uf1.idl.iro`

The paper reduces the decidable mixed fragment to order with unary predicates
and cites automata on linear orderings as a likely route toward practical
decision procedures. This could support a dense/discrete hybrid proof system:
real variables provide order regions, integer-guarded variables provide
difference steps, and unary predicates encode finite-state annotations.

This is less directly aligned with SJAS than the pure integer/S1S route because
syntax and proof codes are discrete. The real-order component may be useful for
model-theoretic experiments, but it does not obviously help with
self-reference.

### Decision-Procedure Consistency Rather Than Tableau Consistency

A more radical option is to make `D` be a complete decision procedure for the
chosen decidable fragment. The self-consistency statement would then be about no
accepted certificate pair for a formula and its negation.

This is attractive only if the decision procedure emits checkable certificates
whose correctness relation is definable inside the same weak object language.
If certificate checking is performed by the host or by an uninterpreted
predicate, the system no longer demonstrates internalized self-justification.

## Current Answer

No reviewed decidable first-order fragment is currently ready to serve as a
faithful Willard-style SJAS language and axiom basis with the existing Proflog
proof predicate.

The strongest negative reason is not simply undecidability. It is that the
decidable fragments appear too weak to internalize the formula/proof/substitution
machinery required by `SelfCons_k(beta,d)` unless the proof apparatus itself is
redesigned to have regular/automata-local certificates.

The most plausible research candidate is:

```text
quantified integer/natural difference logic
+ unary uninterpreted predicates
+ automata/S1S-style proof or decision certificates
```

or the paper's mixed decidable fragment:

```text
uf1.idl.iro
```

but only if the SJAS proof predicate is redesigned around automata-checkable
local certificates. The currently implemented Willard/Proflog route remains:

```text
Willard U-Grounding Pi*1 language
+ finite beta
+ semantic tableaux or Tab-1 as D
```

with decidability of the global satisfiability problem not claimed.

## Immediate Design Consequences For Proflog

1. Do not represent the current Proflog SJAS as a decidable difference-logic
   fragment. It is a Willard U-Grounding/tableau implementation.
2. If a decidable SJAS variant is pursued, give it a new ADR and a new proof
   profile name; do not overload `:willard-sjas-level1`.
3. The new profile's first red test should reject any attempt to cite an
   uninterpreted proof predicate as self-justification.
4. The first positive tests should demonstrate that formula syntax,
   complement/negation, and at least one proof-certificate rule are checked by
   the same decidable object-language apparatus.
5. Higher-arity uninterpreted predicates and unrestricted real difference
   constraints should be excluded from any decidable-profile language
   declaration.
