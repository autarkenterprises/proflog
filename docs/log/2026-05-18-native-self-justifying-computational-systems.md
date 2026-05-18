# Native Self-Justifying Computational Systems

Date: 2026-05-18

## Context

This note records a discussion that began with the role of Turing machines in
the SJAS literature and ended with a broader research question: how to define
self-justifying systems natively for computational paradigms other than
first-order logic with equality and arithmetic.

The discussion should not be read as an implementation decision for Proflog.
It is a conceptual research direction adjacent to the Willard SJAS work.

## Turing Machines in the SJAS Literature

The local SJAS corpus was searched for references to Turing machines and Turing
functions. The substantive material is concentrated in Willard 2001:

- `willard2001_self_verifying_axiom_systems_author_jsl1.txt` Appendix B,
  "The Turing-Function Encoding of Group-3 Axioms", introduces Turing versions
  of `IS(A)`, `ISREF(A)`, and `ISlambda(A)`.
- The appendix defines a universal four-tape Turing machine whose tapes encode
  the instruction table and inputs. The associated `Tape_i`, `Head_i`, and
  `State_i` functions are arithmetized over numerals representing machine
  configurations.
- Willard states that finitely many Pi-1 axioms suffice to define these Turing
  functions and that they satisfy the required non-growth condition, so they can
  be added to the Group-1 schema.
- Appendix C then matters: it shows that the Turing-function route is
  sufficient but not necessary. The same Group-3 machinery can be encoded using
  the weaker Grounding vocabulary.

The 2002 semantic-tableaux/RQ paper briefly repeats the complexity-theoretic
setup around nondeterministic oracle Turing machines and the linear hierarchy.
Later Willard papers mostly cite Turing historically, as part of the discussion
of Godel's changing view of the Second Incompleteness Theorem.

## SJAS Proof Predicate and Turing Encodings

It is not necessary to encode a Turing machine in the language of an SJAS in
order to evaluate the system's proof predicate. What is necessary, for an SJAS
claim rather than a host-language checker, is an internal representation of the
proof predicate over codes of formulae, proof objects, and the relevant axiom
system.

The Turing-function route is one way to do this. In that route the Turing
simulation functions are themselves arithmetized: tapes, heads, states, times,
and instruction tables are represented by numerals, and the proof predicate
ultimately reasons about numerically encoded machine configurations. It is not a
host-side call to an external Turing machine.

The Grounding route is another way to do it. It avoids taking Turing simulation
functions as object-language primitives, while still expressing proof checking
through arithmetized syntax and proof codes.

## Reversing the Encoding Direction

The reverse thought experiment was then considered: instead of encoding Turing
machines as Group-1 functions, encode the Group-1 functions as Turing machines.

Externally, the equivalent of proof checking would be a proof-verifier machine:

```text
Verifier accepts (system-code, proof-code, theorem-code)
```

For a self-justifying claim, however, this does not remove the internalization
problem. The accepting computation must itself be represented as native
evidence. A Turing-machine presentation would typically use an explicit
configuration trace and a checker for local transition correctness, rather than
an opaque `accepts` oracle.

Thus the machine-native analogue of proof checking is not a first-order
predicate by default. It is a finite certificate that a proof-checker machine
has a valid accepting run on a particular proof object.

## Initial Alternative Mechanisms Considered

Several internalization mechanisms were identified:

- word or concatenation theories, where formulae, proofs, and traces are finite
  words and checking is expressed through decomposition and concatenation;
- Post or semi-Thue rewrite systems, where proof checking is derivability by
  explicit rewrite histories;
- computation-history tableaux, where a machine run is a finite grid whose local
  neighborhoods obey transition constraints;
- term rewriting, SKI, lambda calculus, or graph-reduction systems, where the
  native evidence is a reduction trace.

This first pass was useful but still too close to first-order logic: it spoke of
proof predicates for these models as if the models natively used FOL
quantifiers and predicates.

## Correction: Native Self-Justification Is More General

The corrected research question is broader than simulating an SJAS in another
paradigm.

SJAS are defined in a fragment of first-order logic with equality plus a custom
arithmetic theory. Other computational calculi do not necessarily have FOL
formulae, quantifiers, or proof predicates as primitive notions. For example,
lambda calculus has no native quantifier syntax, though dependent type theory
can put dependent products and sums into correspondence with universal and
existential quantification.

The right abstraction is therefore:

```text
What are the native notions of assertion, evidence, checking,
self-reference, and failure in the computational model?
```

Only after those are identified can one ask whether a native self-justifying
system exists.

## Candidate Native Definitions

### Turing Machines

A Turing-machine-native self-justifying system is closer to safety
certification than theorem proving.

The machine has:

- a finite transition-table description;
- a distinguished bad configuration class;
- a finite certificate, such as an invariant or ranking/safety argument;
- a native checker that validates the certificate against the transition table.

The self-justifying claim is operational:

```text
This machine cannot reach a bad configuration.
```

The evidence is not a first-order proof unless one deliberately adds a logic on
top. It may be an invariant certificate checked by local transition inspection.

The analogue of the U-Grounding restriction is that the system must not have a
primitive halting oracle, accepting-run oracle, or full evaluator. It may inspect
its own finite description and validate explicit evidence, but not collapse
safety into an unrestricted semantic truth predicate.

### Dependent Type Theory and Typed Lambda Calculi

In a dependent type theory, the native judgment is not "formula has a proof" in
the FOL sense, but:

```text
term : type
```

A type-theoretic self-justifying system would contain a checked term inhabiting
a type expressing the system's restricted consistency or safety property:

```text
selfCons : Consistency(CodeOfThisSystem)
```

The checker is the type checker. The self-reference mechanism is quotation or a
code of the signature/rules. The restrictions corresponding to U-Grounding are
universe stratification, controlled reflection, no unrestricted internal
evaluator, and no `Type : Type` style collapse.

### Rewrite Systems

For Post systems, semi-Thue systems, and term-rewriting systems, native
evidence is a derivation or a certificate about derivations.

A native self-justifying rewrite system might certify that its own rewrite
relation cannot derive a forbidden term:

```text
start_S ->* bad
```

is impossible.

The certificate could be a termination ordering, confluence argument,
critical-pair completion certificate, regular-language invariant, or bounded
derivation certificate. The checker validates this evidence using rewrite-local
rules, not an FOL proof predicate unless one is added as an overlay.

## Research Direction

The more general object is not "SJAS implemented in other computational
models." It is a family of self-justifying operational systems.

For each computational paradigm, the task is to define:

1. its native assertion or judgment form;
2. its native evidence objects;
3. its native checking relation or algorithm;
4. its native self-reference/quotation mechanism;
5. its analogue of contradiction, inconsistency, or bad behavior;
6. the expressivity restriction that prevents full truth, halting, or
   unrestricted evaluation reflection.

The central analogue of Willard's tradeoff is not necessarily "multiplication is
not a total function." It is the broader pattern:

```text
The system has enough internal access to certify a restricted self-safety claim,
but not enough reflective power to express or decide its full semantic truth.
```

This reframing should guide any future ADR in this area. Such an ADR should not
begin by translating SJAS formulae into another substrate. It should begin by
choosing a computational model and defining what self-justification means in
that model's own native terms.

## Addendum: Suspect TM-Regular-Invariant Mechanism

Later in the discussion, a concrete candidate mechanism was proposed for a
Turing-machine-native analogue: represent configurations as words, transitions
as finite transducers, and safety certificates as regular invariants checked by
automata operations.

In outline, such a checker would validate:

```text
Init subset I
Post_delta(I) subset I
I intersect Bad = empty
```

This would give a machine-native safety-certificate discipline: a system carries
its own transition description plus an explicit invariant certificate, and a
native checker validates the certificate by local automata/transducer
operations.

This mechanism is **suspect and insufficiently justified** as an analogue of
Willard-style self-justification.

The working objection is that the problem is not solved by saying that the
machine lacks a halting oracle. A halting oracle may not be necessary to
recreate the diagonalization pressure behind reachability or self-reference
antinomies. The analogy with Willard's SJAS is also too coarse: giving an SJAS
total multiplication is not the same as giving it a halting oracle as a
primitive. The issue appears to be that the consequences of total
multiplication, when combined with coding and proof machinery, supply enough
expressive strength for the relevant diagonalization/incompleteness obstacles.

This is a caution to test, not a settled theorem. If the literature or a direct
construction shows that the regular-invariant mechanism avoids the relevant
diagonalization for principled reasons, or that "no halting oracle" is in fact a
valid native restriction in the relevant formal setting, that evidence should
override this objection and be logged explicitly.

Therefore, any credible Turing-machine-native mechanism must identify the exact
restricted expressive resource that corresponds to U-Grounding's missing total
multiplication. It is not enough to say "no halting oracle" or "no full
evaluator." The research problem is to characterize which native closure,
reachability, trace, invariant, or self-inspection principles are strong enough
to certify nontrivial self-safety while still weak enough to avoid the analogous
diagonal construction.

Until that boundary is made precise, the regular-invariant proposal should be
treated only as a candidate safety-certification substrate, not as an
established self-justifying computational model.

## Addendum: Evidence For The Caution

The caution above is supported by several independent considerations.

First, ordinary Turing-machine reachability already captures the halting
problem: asking whether a halting state is reachable from an initial
configuration is just the halting question in reachability form. This supports
the objection that "no halting oracle" is not the right explanatory boundary.
Diagonalization pressure arises from the system's ability to encode and reason
about its own computational behavior, not only from the presence of a primitive
oracle.

Second, the general incompleteness background points to the same issue. The
Stanford Encyclopedia of Philosophy's Godel article summarizes the threshold as
"a certain amount of elementary arithmetic"; more precisely, Q suffices for the
first theorem and something like PRA is used in the standard second-theorem
proofs. The same source also notes that the details of the provability predicate
and the presentation of axioms matter: Rosser-style predicates can alter what a
formal "consistency" sentence does. This reinforces that the boundary is not
captured by a single slogan such as "no halting oracle."

Third, Willard's own discussion treats total multiplication as a trigger through
its consequences, not as a disguised halting oracle. The 2011 self-justifying
logics paper says the relevant invariant would collapse if a multiplication
function symbol were added to U-Grounding, and says total multiplication is the
trigger point causing the semantic-tableaux second-incompleteness effect to
become active. The 2004 paper likewise frames the positive region as the narrow
gap where multiplication is treated as a three-place relation rather than a
total function.

Together these observations support the user's caution: a Turing-machine-native
analogue must identify the specific expressive closure principle that plays the
role total multiplication plays for Willard. It is not enough to deny a primitive
halting oracle.

## Adjacent Question: Subrecursive Or Weaker-Than-SJAS Systems

A follow-on research question is whether axiom systems even weaker than
Willard-style SJAS can attain a self-justifying property. The motivating
observation is that Willard's Turing versions can encode Turing-machine
computation, while one might ask for a system whose implementation/evaluation
does not require Turing completeness, for example one based on primitive
recursive computation.

This question needs a sharp distinction between implementation strength and
object-theory strength.

At the implementation level, a bounded proof checker or certificate checker can
often be primitive recursive: given an explicit proof object and finite bounds,
checking local syntactic/proof-step correctness is a total computation. This
does not imply that complete proof search, theorem enumeration, or unbounded
machine simulation is primitive recursive.

At the object-theory level, however, "primitive recursive" is not automatically
weaker in the relevant Willard sense. Primitive recursive arithmetic contains
many total functions, including addition, multiplication, and exponentiation.
That places PRA near the standard second-incompleteness threshold, not below
Willard's Type-A boundary. In other words, a theory whose official function
vocabulary includes all primitive recursive functions would likely reintroduce
exactly the expressive strength Willard suppresses by refusing total
multiplication.

The plausible research target is therefore not "PRA as an SJAS." A more
promising target would be a subrecursive or decidable native system with:

1. explicit finite proof/certificate objects;
2. a total checker for those objects;
3. enough self-reference to name the system and its checker;
4. a restricted self-safety or consistency statement;
5. a carefully identified missing closure principle analogous to missing total
   multiplication;
6. an external soundness theorem showing the self-justifying assertion is not
   vacuous.

Candidate directions include bounded-consistency families, regular/automata
certificate systems, Presburger-like additive systems with restricted coding,
and typed or stratified calculi whose native quotation/evaluation principles
are weaker than universal computation. The central open question is whether any
such system can state a nontrivial global self-justifying claim, rather than
only a family of bounded or certificate-relative claims.
