# Greenfield Implementation Tutorial and Reference

Date: 2026-05-03
Related ADRs:
[ADR-0034](adr/ADR-0034-greenfield-implementation-tutorial.md),
[ADR-0035](adr/ADR-0035-relational-residual-continuation.md),
[ADR-0036](adr/ADR-0036-speculative-relational-arithmetic-and-tabling.md),
[ADR-0037](adr/ADR-0037-core-logic-minikanren-enhancements.md),
[ADR-0038](adr/ADR-0038-fitting-program-kernel-evaluation.md)

This chapter explains the current greenfield Proflog implementation as a
whole system. It is written for a reader who needs to understand the design,
the code, and the proof-state mechanics well enough to extend the
implementation without treating any layer as magic.

The scope is the greenfield stack under `src/proflog` and `test/proflog`. The
legacy `cljtap` code remains reference material and experimental prior art, not
the authority for this implementation.

Current checkpoint:

- ADR-0035 moved the promoted structural residual-continuation path into the
  answer overlay, so public list-family answer closure no longer depends on
  the constructor-recursive diagnostic sidecar.
- ADR-0036 added speculative faster-minikanren relational arithmetic and proved
  that direct raw `core.logic/tabled` is not a drop-in replacement for
  Proflog's canonical proof-state tabling. Production fuel remains the
  finite-domain host-integer relation.
- ADR-0037 completed a project-local miniKanren constraint overlay plus
  relational map/fuel/tree/performance probes. Those probes are research
  surfaces, not production proof-search semantics.
- ADR-0038 adds a kernel-backed fitting-program evaluation catalog. Promoted
  true/false outcomes carry proof evidence; GV associativity remains an
  explicit bounded proof-search frontier rather than a host-overlay result.

## 1. Orientation

The greenfield implementation is a tableau-based logic programming system in
Clojure/core.logic. Its core idea is simple:

1. User-facing declarations, clauses, and queries are represented as a small
   tagged object-language AST.
2. The language layer validates and compiles source clauses into a synchronized
   compiled program.
3. The proof kernel closes tableaux over formulas relative to explicit branch
   state.
4. The query layer interprets success and failure through proof of a query or
   proof of its negation.
5. The answer layer runs the same proof machinery in a mode that exports
   selected user variables, symbolic residuals, and proof evidence.

The implementation is not a monolith. It is a stack of deliberately narrow
components:

```text
public source declarations, clauses, queries
  |
  v
AST constructors and validators
  |
  v
language compiler: NNF, alpha-renaming, compiled program views
  |
  v
program lookup and argument binding for procedure calls
  |
  v
ordinary proof kernel, equality theory, support relations, gamma candidates
  |
  +--> profiled propositional / first-order proof layers
  +--> optional tabling wrapper
  |
  v
query status surface
  |
  v
answer overlay, answer export, residual completion, constructor recursion
  |
  v
diagnostics, probes, public answer records, tests
```

The most important engineering principle is that proof state is explicit. The
kernel does not mutate a hidden branch. It threads:

- pending formulas;
- saved literals;
- lexical substitution for bound variables;
- gamma-introduced proof variables;
- the equality substitution;
- delayed disequalities;
- the compiled program;
- generated gamma candidates;
- fuel; and
- proof evidence.

The answer overlay adds:

- the selected user answer variables;
- deferred procedure-call residuals;
- recursive call-depth budget; and
- answer-export behavior for existential witnesses.

## 2. Source Map

The main implementation namespaces are:

| Layer | Source | Responsibility |
|---|---|---|
| AST | [`src/proflog/ast.clj`](../src/proflog/ast.clj) | Tagged constructors and recognizers for object-language terms, literals, formulas, clauses, and nominal variables. |
| Language | [`src/proflog/language.clj`](../src/proflog/language.clj) | Declaration normalization, validation, alpha-renaming, source-clause compilation, alternatives, guarded alternatives. |
| NNF | [`src/proflog/normalize.clj`](../src/proflog/normalize.clj) | Convert surface formulas to negation normal form and compute formula negation. |
| Substitution | [`src/proflog/subst.clj`](../src/proflog/subst.clj) | Pure and relational substitution over terms and formulas with binder shadowing. |
| Program calls | [`src/proflog/program.clj`](../src/proflog/program.clj) | Relational lookup of compiled clauses and binding of formal parameters to actual call arguments. |
| Kernel | [`src/proflog/kernel.clj`](../src/proflog/kernel.clj) | Full Fitting-style tableau relation with equality, disequality, procedure calls, fuel, and proof terms. |
| Equality | [`src/proflog/equality.clj`](../src/proflog/equality.clj) | Free-constructor equality, walking, occurs checks, unification, atom closure, disequality violations. |
| Kernel support | [`src/proflog/kernel_support.clj`](../src/proflog/kernel_support.clj) | Shared closure, L-groundness, fuel, disequality maintenance, and proof-variable discipline. |
| Gamma candidates | [`src/proflog/gamma.clj`](../src/proflog/gamma.clj) | Bounded closed-term generation for gamma instantiation in call-free fragments. |
| Proof helpers | [`src/proflog/proof.clj`](../src/proflog/proof.clj) | Small proof-term inspection helpers. |
| Profile dispatch | [`src/proflog/formula_profile.clj`](../src/proflog/formula_profile.clj) | Classify formulas for specialized theorem proving layers. |
| Propositional layer | [`src/proflog/kernel/propositional.clj`](../src/proflog/kernel/propositional.clj) | Small proof-producing propositional tableau component. |
| First-order layer | [`src/proflog/kernel/first_order.clj`](../src/proflog/kernel/first_order.clj) | Equality-free first-order proof component, including a lean alphaleanTAP-shaped path. |
| Tabling | [`src/proflog/tabling.clj`](../src/proflog/tabling.clj) | Optional canonical proof-state tabling wrapper around the ordinary kernel. |
| Query | [`src/proflog/query.clj`](../src/proflog/query.clj) | Top-level success, failure, bounded status, and iterative fuel probing. |
| Answer overlay | [`src/proflog/answer_overlay.clj`](../src/proflog/answer_overlay.clj) | Answer-mode tableau overlay: answer variables, residuals, call-depth, existential-as-variable execution, and ADR-0035 structural residual continuation. |
| Answers | [`src/proflog/answers.clj`](../src/proflog/answers.clj) | Public answer records, export, canonicalization, ranking, diagnostics, parity and ground materialization. |
| Constructor recursion | [`src/proflog/kernel/constructor_recursive.clj`](../src/proflog/kernel/constructor_recursive.clj) | Guarded-IR constructor-recursive proof and residual-settlement diagnostic layer. |
| Hard-family overlay | [`src/proflog/hard_family_overlay.clj`](../src/proflog/hard_family_overlay.clj) | Named non-default status accelerator for restricted hard-family probes. |
| Relational arithmetic | [`src/proflog/relational_arithmetic.clj`](../src/proflog/relational_arithmetic.clj) | ADR-0036 Clojure translation of faster-minikanren bit-list arithmetic for speculative fuel and arithmetic probes. |
| MiniKanren constraints | [`src/proflog/minikanren_constraints.clj`](../src/proflog/minikanren_constraints.clj) | ADR-0037 project-local compatibility overlay for `symbolo`, `numbero`, and general-purpose `absento`; type checks still use `predc`, while `absento` is a project-owned deep absence constraint. |
| Fitting program catalog | [`src/proflog/fitting_programs.clj`](../src/proflog/fitting_programs.clj) | ADR-0038 kernel-backed catalog for P1, P2, move-warning, finite-domain, list-family, and GV-frontier examples. |
| ADR probes | [`src/proflog/relational_fuel_adapter_probe.clj`](../src/proflog/relational_fuel_adapter_probe.clj), [`src/proflog/relational_maps_probe.clj`](../src/proflog/relational_maps_probe.clj), and related probe namespaces | Speculative or measurement-only namespaces. They document evidence and should not be mistaken for default production behavior. |
| Host probes | [`src/proflog/core_logic_host.clj`](../src/proflog/core_logic_host.clj) and related probe namespaces | Report and instrument the loaded core.logic host implementation. |

The root documentation spine is:

- [Execution Plan](EXECUTION_PLAN.md);
- [Development Log](../LOG.md);
- [Semantic Variants](SEMANTIC_VARIANTS.md);
- [Test Matrix](TEST_MATRIX.md);
- [ADR index](adr/README.md);
- [AAR index](aar/README.md);
- [Language Namespace Spec](LANGUAGE_NAMESPACE_SPEC.md).

## 3. The Object Language

The AST layer uses tagged lists for terms and formulas. That choice is not only
cosmetic. Tagged lists let core.logic decompose formulas relationally without
turning every operation into host-side map inspection.

Terms:

```clojure
(var x)              ; object-language variable
(par a)              ; internal rigid parameter
(app zero)           ; nullary constructor / constant
(app s (app zero))   ; positive-arity constructor application
```

Atoms are also `app` nodes, but the language validator decides whether the head
symbol is a relation or a function:

```clojure
(app reverse xs ys)
```

Formula tags:

```clojure
(pos atom)
(neg atom)
(eq left right)
(neq left right)
(true)
(false)
(and left right)
(or left right)
(not body)          ; surface only
(implies a b)       ; surface only
(forall tie)
(once-forall tie)
(exists tie)
```

The implementation uses nominal ties for binders. In ordinary Clojure tests and
compile-time helpers, `ast/nom` allocates fresh noms:

```clojure
(ast/nom x
  (ast/pos-lit (ast/app-term 'p (ast/var-term x))))
```

### Public And Internal Terms

`(var nom)` is an object-language variable. It is legal in source queries and
can appear in exported symbolic answers.

`(par nom)` is a proof-time rigid parameter introduced by the delta rule for
existentials in ordinary proof search. Public source programs and queries may
not mention `par`, and exported answer records reject it through the language
validator.

That split is one of the central semantic safeguards. The prover may create
rigid parameters internally, but the public object language remains the
declared language `L`.

## 4. Language Declarations And Compilation

The language layer is a compiler. It does not prove anything.

A declaration is a map:

```clojure
{:constants ['zero 'null]
 :functions {'s 1
             'cons 2}
 :relations {'append 3
             'reverse 2}}
```

`language/language` normalizes this map:

- constants become zero-arity functions;
- constants, functions, and relations are checked for namespace conflicts;
- arities must be non-negative integers;
- function and relation symbols stay separate.

`language/validate-term`, `validate-atom`, `validate-formula`,
`validate-clause`, and `validate-query` enforce that source terms and formulas
fit the declaration. Validation is structural and declaration-sensitive. It
rejects undeclared symbols, arity mismatches, malformed tagged forms, non-nom
clause parameters, and all public `par` terms.

### Source Clauses

A source clause is a map:

```clojure
{:relation 'append
 :params [xs ys zs]
 :body formula}
```

Use `ast/clause` to construct this shape. A surface program may contain
multiple clauses for the same relation. The compiler groups them by relation
and turns each relation into one Fitting-style compiled clause whose body is
the disjunction of the source bodies.

### Alpha-Renaming

When several clauses for the same relation are merged, their parameters must
not accidentally share noms. `language/clause-group->core-clause` creates one
fresh parameter vector for the compiled relation and substitutes each source
clause body from its original parameters to those fresh parameters.

This means every compiled alternative for one relation talks about the same
compiled formal parameters, while binders inside each body retain normal
lexical scope.

### Normalization To NNF

`normalize/to-nnf` removes surface `not` and `implies`. After compilation:

- negation appears only as literal polarity or dualized equality, disequality,
  and quantifiers;
- `forall` and `exists` use nominal ties;
- negating an existential produces `once-forall`, the single-use universal used
  by negative procedure-call execution.

Each compiled clause stores both:

```clojure
:body
:negated-body
```

The invariant is:

```text
:negated-body = normalize/negate-formula(:body)
```

The kernel trusts this invariant. It does not revalidate compiled program maps.
The public boundary is therefore:

```text
source declaration -> language/language
source clauses     -> language/compile-program
source query       -> language/validate-query
```

Constructing compiled maps manually is a low-level experiment, not a normal
public use of Proflog.

### Compiled Program Views

`language/compile-program` returns:

```clojure
{:language lang
 :clauses {...}
 :clause-list (...)
 :alternative-clause-list (...)
 :guarded-clause-list (...)}
```

The same logical clauses are stored in multiple executable views:

- `:clauses` is a map keyed by relation symbol for host-side lookup.
- `:clause-list` is the compact relational view used by ordinary procedure
  calls.
- `:alternative-clause-list` keeps top-level disjuncts separate for answer
  search and diagnostics.
- `:guarded-clause-list` partitions alternatives into guards, calls, and
  residuals for constructor-recursive execution.

The compiler flattens top-level disjunctions into alternatives. A source
program with two `p` clauses and a source body containing `or` both become a
compiled relation with several alternatives.

### Guarded Alternatives

A guarded alternative records executable metadata:

```clojure
{:formula ...
 :negated-formula ...
 :scope ...
 :core ...
 :conjuncts ...
 :guards ...
 :calls ...
 :residuals ...
 :negated-guards ...
 :negated-calls ...
 :negated-residuals ...
 :negated-ordered-conjuncts ...}
```

The partition is generic:

- equality and disequality formulas become guards;
- positive or negative calls to relations defined by the same compiled program
  become calls;
- everything else remains residual.

No production logic dispatches on `append`, `reverse`, `cons`, or `null` to
create this IR. The constructor-recursive layer later consumes only
constructors, equality/disequality guards, and calls to defined relations.

## 5. Substitution

`proflog.subst` has two roles.

Pure substitution is used at compile time:

- `subst-term`;
- `subst-formula`;
- `lookup-binding`;
- `remove-binding`.

Relational substitution is used in proof search:

- `subst-termo`;
- `subst-term*o`;
- `subst-formulao`;
- `lookupo`;
- `unboundo`;
- `remove-bindo`.

Both versions preserve binder scope. When substitution enters a quantified
formula, the binding nom is removed from the environment so an outer binding
does not capture a locally bound variable.

The kernel uses substitution for two different environments:

- the source/compiler environment that maps old clause parameters to compiled
  parameters; and
- the proof-time lexical environment that maps nominal binders to the terms
  introduced by quantifier and procedure-call rules.

Do not confuse that lexical environment with the equality substitution
`sigma`. `env` says "what does this binder denote in the current formula?"
`sigma` says "what equality bindings has this proof branch learned?"

## 6. Program Calls

`proflog.program` is the relational interface between compiled programs and
the proof kernel.

The central operation is `call-clauseo`:

```clojure
(program/call-clauseo prog atom env body negated-body)
```

It:

1. decomposes the atom into a relation and arguments;
2. finds the compiled clause for that relation in `:clause-list`;
3. binds compiled formal parameters to actual atom arguments; and
4. returns an environment plus the compiled positive and negative bodies.

The richer lookup variants expose alternatives and guarded alternatives:

- `call-clause-with-alternativeso`;
- `call-clause-with-guarded-alternativeso`.

The program namespace deliberately keeps a list view of compiled clauses so
lookup can stay relational. The full `:clauses` map is useful at host-side
layers, but the kernel's Procedure Call Rule should not depend on ordinary map
lookup.

## 7. The Proof Kernel

`proflog.kernel` is the ordinary proof-search core. It asks:

```text
Can this branch be closed?
```

Its main relation is `prove-stateo`:

```clojure
(prove-stateo fml
              unexpanded
              lits
              env
              proof-vars
              sigma
              sigma-out
              neqs
              neqs-out
              prog
              gamma-terms
              fuel
              proof)
```

Read the state fields as:

| Field | Meaning |
|---|---|
| `fml` | Current formula for backward-compatible callers. Internally it becomes part of an agenda. |
| `unexpanded` | Remaining pending formulas on the branch. |
| `lits` | Saved positive or negative atoms on the branch. |
| `env` | Lexical binder environment for quantified formulas and procedure-call parameters. |
| `proof-vars` | Noms introduced by gamma instantiation and allowed in proof-local disequality closure. |
| `sigma` | Explicit equality substitution at branch entry. |
| `sigma-out` | Equality substitution at branch closure. |
| `neqs` | Delayed disequality pairs at branch entry. |
| `neqs-out` | Delayed disequality pairs after closure and pruning. |
| `prog` | Compiled program or `nil` for theorem-only proof. |
| `gamma-terms` | Explicit finite collection of closed terms available to gamma fallback. |
| `fuel` | Optional bounded micro-step budget; `nil` means unbounded. |
| `proof` | Tagged proof term explaining how the branch closed. |

`close-agendao` turns `fml` plus `unexpanded` into an agenda and uses
`kernel-support/selecto` to choose the next formula. That is the ADR-0016 fair
agenda hook: proof search is not forced to expand exactly the leftmost pending
formula.

### Rule Families

The kernel is organized around Fitting-style tableau rules plus operational
bookkeeping.

Alpha rule, conjunction:

```text
A and B
```

keeps one branch. The kernel proves `A` now and pushes `B` onto pending work.

Beta rule, disjunction:

```text
A or B
```

splits the branch. Both sibling branches must close. Since branch state is
explicit, the first sibling's output `sigma` and `neqs` thread into the second
sibling.

Gamma rule, universal:

```text
forall x. A
```

usually introduces a fresh proof variable `(var nom)`. When the formula is
call-free and the fresh-variable path is not enough, a bounded closed-term
candidate from `proflog.gamma` may be used.

`once-forall` is the single-use universal created when negating existential
clause bodies. It instantiates once and does not re-enqueue itself.

Delta rule, existential:

```text
exists x. A
```

introduces a rigid proof parameter `(par nom)` in ordinary proof mode.

Positive equality:

```text
t1 = t2
```

is handled by the equality engine. It may:

- close immediately if equality is impossible;
- extend `sigma`;
- close because a saved disequality has become false;
- close because saved complementary atoms now unify; or
- reopen a saved procedure call whose arguments became L-ground after walking.

Negative equality:

```text
t1 != t2
```

closes immediately if the terms are already the same under `sigma`. It may
also close by unifying through proof-local gamma variables. If it is not yet
settled, it is stored in `neqs` and rechecked after later equalities.

Positive and negative atoms:

- first try complementary closure against saved literals;
- then try the Procedure Call Rule if the atom is L-ground;
- otherwise save the literal in `lits`.

Positive procedure calls prove the compiled clause body. Negative procedure
calls prove the compiled negated body or, when guarded alternatives are
available, close the negation of every guarded alternative.

### L-Groundness

The Procedure Call Rule applies only to atoms in the object language `L`.
`kernel-support/l-ground-termo` defines this structurally:

- `(var ...)` is allowed;
- constructor applications recurse through their arguments;
- `(par ...)` is rejected.

This prevents rigid delta witnesses from leaking into ordinary object-language
procedure calls.

### Saved Calls And Equality

Atoms may be saved because they cannot close or call yet. Later equality can
make a saved atom callable. `kernel/saved-call-closeso` walks saved atoms
through the refined `sigma`, checks L-groundness, and opens the corresponding
subsidiary tableau.

This is an important completeness mechanism. Procedure-call behavior depends
on branch state, not on whether enabling equality happened before or after the
atom was first seen.

## 8. Equality And Disequality

`proflog.equality` is a free-constructor equality theory over the tagged term
language.

The main concepts are:

- `walko`: normalize the root of a term through `sigma`;
- `walk*o`: deeply normalize a term through `sigma`;
- `same-termo`: read-only structural equality after walking;
- `eq-contradictiono`: recognize impossible equalities;
- `unify-termo`: extend `sigma` so two terms become equal;
- `atom-unifyo`: unify two atom argument lists under the same relation;
- `neq-violatedo`: detect a saved disequality that became false;
- `contradictory-atomso`: detect saved positive/negative atoms that now unify.

The equality substitution is an association list. It can bind both `(var ...)`
and `(par ...)` roots, but with separate soundness checks:

- proof variables use an occurs check;
- rigid parameters use an absent-parameter check.

Constructor heads are free constructors. Distinct heads cannot unify. Same-head
constructors decompose argumentwise.

`proflog.kernel-support` adds the branch-state operations around equality:

- direct complementary literal closure;
- stable and pruned disequality stores;
- rigid constructor disequality;
- L-groundness;
- fuel stepping; and
- proof-variable-only binding discipline.

The disequality store is symbolic. A stored pair means "not currently false,
but recheck when equality changes." The kernel does not eagerly enumerate all
disunifiers.

## 9. Fuel And Gamma Candidates

Fuel is an operational guard, not a semantic truth value.

`kernel-support/step-fuelo` consumes one micro-step for non-closing branch
progress. A `nil` fuel value means unbounded search. Production finite fuel is
currently represented as host integers constrained by core.logic finite-domain
relations: the current fuel must be positive, the next fuel must be
non-negative, and `fuel = next-fuel + 1`.

ADR-0036 and ADR-0037 add an opt-in relational arithmetic path based on
faster-minikanren bit-list numerals. That path is useful for testing whether
finite-domain fuel blocks reverse or partial synthesis, but it is not the
production API. Public callers still pass `nil` or host integers unless a later
ADR explicitly changes the boundary.

`proflog.gamma` owns bounded closed-term enumeration for the gamma rule. The
kernel receives a finite `gamma-terms` collection and asks
`closed-term-candidateo` for one candidate. Generation is intentionally outside
the kernel relation so the kernel remains a readable proof rule over explicit
state.

Closed-term candidates are only supplied for call-free compiled programs. That
prevents recursive program search from being multiplied by Herbrand
enumeration unless a later ADR explicitly changes that policy.

## 10. Proof Terms

Proofs are tagged lists. Examples of step tags include:

- `conj`;
- `split`;
- `univ`;
- `once-univ`;
- `witness`;
- `eq-step`;
- `neq-close`;
- `pos-call`;
- `neg-call`;
- `neg-call-guarded-alt`;
- `profiled`;
- `constructor-recursive-*`.

`proflog.proof/contains-step?` and `collect-steps` support tests and
diagnostics. The implementation intentionally keeps proof terms inspectable
instead of hiding closure evidence behind opaque objects.

Proof terms are not only presentation. They are used to verify that the right
layer closed a branch, that optimized or constructor-recursive layers are
actually exercised, and that public answer records retain evidence.

## 11. Profiled Proof Layers

The full kernel remains authoritative for equality, disequality, program
calls, and answer-oriented execution. Some formulas do not need the full
machine.

`proflog.formula-profile` classifies formulas as:

- pure propositional;
- equality-bearing;
- equality-free first-order; or
- unsupported by a named narrow profile.

`kernel/prove` dispatches theorem-only formulas to the weakest sufficient
layer:

- pure propositional formulas use `proflog.kernel.propositional`;
- equality-free first-order formulas use `proflog.kernel.first-order`;
- broader formulas use the full kernel.

Inside `kernel/close-agendao`, `profiled-closeo` can close a residual branch
through the optimized layer when the branch is isolated from equality state and
active program calls. The proof term records the handoff with a `profiled` tag.

The design rule is separation. Optimized layers can close simple branches, but
they do not replace the full kernel's semantics for Proflog programs.

## 12. Tabling

`proflog.tabling` is an optional wrapper around the ordinary kernel. It exists
so the kernel can remain a direct readable relation while tabling and canonical
state reuse live elsewhere.

The tabling layer builds canonical keys from:

- agenda formulas;
- saved literals;
- residuals;
- disequalities;
- equality substitution;
- proof vars;
- program identity;
- gamma terms;
- fuel; and
- call depth where applicable.

It normalizes alpha-equivalent noms and walks formulas through `sigma` before
forming keys. The wrapper uses core.logic tabled relations and binds the
kernel's dynamic recursive dispatcher so recursive calls route through the
tabled relation for the duration of a run.

This is an operational layer. It should not be read as a different semantic
kernel.

ADR-0036 checked whether Proflog was trivially duplicating raw core.logic
tabling. The answer was no: a direct raw `core.logic/tabled` replacement did
not reproduce the ADR-0035 list-family answer-cache behavior. Proflog's tabling
layer remains an extension around canonical proof-state keys. Future tabling
work should be about concrete integration points with that canonical state, not
about replacing `proflog.tabling` wholesale.

## 13. Query Status

`proflog.query` exposes top-level truth-status helpers.

For a query `Q` relative to a program:

- `query-succeeds` tries to prove `not Q`;
- `query-fails` tries to prove `Q`;
- `query-status` probes both sides under bounded fuel and timeout.

This follows the tableau reading: a query succeeds when the tableau for its
negation closes, and fails when the tableau for the query itself closes.

`query-status` returns:

- `:succeeds`;
- `:fails`;
- `:unresolved`; or
- `:inconsistent` if both sides close within the probe budget.

Search divergence is not reported as falsity. Bounded timeout produces
`:unresolved` unless a proof is found.

## 14. Answer Overlay

The answer overlay asks a richer question than the ordinary kernel:

```text
How far can this branch be closed, which selected variables were refined,
and what symbolic obligations remain if recursive descent stops here?
```

`proflog.answer-overlay` mirrors the kernel structure but adds:

- answer variables;
- residual procedure-call obligations;
- recursive `call-depth`;
- `existentials-as-vars?`;
- answer-visible existential witnesses; and
- deferral branches for calls that are not unfolded further.

The main answer relation is `prove-stateo` with the ordinary kernel fields plus
`residuals`, `residuals-out`, `call-depth`, and `existentials-as-vars?`.

### Existentials As Variables

In ordinary proof search, the delta rule introduces rigid `(par ...)`
parameters.

In answer mode, `existentials-as-vars?` makes existential witnesses fresh
object-language variables instead. That is why open and reverse-mode queries
can export symbolic structure rather than hiding it behind internal
parameters.

This is also why the answer overlay is a real layer and not just "call the
kernel backwards." Open-query answering needs symbolic witnesses that remain
visible to equality and residual export.

### Call Depth And Residuals

When a procedure call is encountered in answer mode:

- if `call-depth` still permits descent, the overlay opens the subsidiary
  tableau and decrements the recursive budget;
- if descent is no longer permitted, the overlay can keep the call as a
  residual formula;
- saved calls can also become residuals when equality later makes them
  callable but the answer budget is exhausted.

This produces bounded symbolic answers. A record may contain both a binding
and residual obligations such as a deferred `neg` call or a disequality.

Diagnostics can opt out of residual completion so raw unresolved frontiers
remain visible.

ADR-0035 adds structural residual continuation to this layer. When an answer
frontier consists of constructor-demanded negative calls to relations defined by
the same compiled program, the overlay can continue that frontier through the
guarded IR before public export. This is the promoted list-family path. It is
separate from the older constructor-recursive sidecar so ordinary public answer
closure does not depend on a host-side post-processing layer.

### Query Entry

`answer-overlay/prove-program-query-entryo` is the bridge from a top-level
literal query into answer search. The root query call itself does not consume
recursive `call-depth`; that budget is reserved for descendants below the
surface query boundary.

For an original positive query, answer search proves the negated query and
therefore enters the negative procedure-call side. For an original negative
query, the polarity is reversed by NNF negation before entry.

## 15. Answer Records

`proflog.answers` is the public answer surface.

An answer record has this shape:

```clojure
{:bindings [[answer-nom term] ...]
 :residuals [formula ...]
 :proofs [proof ...]}
```

Some parity and ground materialization helpers also include `:query`, the
instantiated ground query.

### Export

`export-answer-record` converts one raw answer-overlay state into a public
record:

1. Walk every requested answer variable through `sigma`.
2. Rename reified internal variables back to the requested answer noms.
3. Convert delayed disequalities into residual `neq` formulas.
4. Walk and rename residual procedure-call formulas.
5. Drop tautological residuals such as constructor-clash disequalities.
6. Reject contradictory residuals.
7. Validate every binding and residual against the declared object language.
8. Attach proof evidence.

The public answer layer therefore enforces the same boundary as the language
layer: exported terms and residuals must live inside the declared language and
must not contain internal `par` terms.

### Canonicalization And Ranking

Raw proof search can find many proof paths for the same exported answer.
`merge-answer-records` canonicalizes records by bindings and residuals and
collects proof evidence.

Canonicalization:

- preserves requested answer noms;
- renames other internal variables to stable `_0`, `_1`, ... names;
- sorts residuals by alpha-insensitive shape; and
- removes duplicate residuals.

Ranking prefers:

1. closed answers over answers with non-disequality residuals;
2. fewer open residuals;
3. fewer free variables;
4. fewer residuals;
5. simpler answer/residual shapes; and
6. first-seen order as a tie-break.

For deeper recursive answer requests, ADR-0033 adds a derivation-depth ordering
pass so base derivations can precede recursive descendants when records are
otherwise comparable.

### Search Deepening

`collect-answer-records` grows the raw proof limit until it has enough unique
answers, exhausts raw search, or reaches `max-raw-proof-limit`. If selected
answers still have non-disequality residuals, it keeps deepening so later
closed answers can displace shallower symbolic frontiers.

For structurally demanded recursive frontiers, the raw answer stream can also
schedule ADR-0035 residual continuation before export. The exported-record
completion hook is now implemented through `proflog.answer-overlay`, not
through the constructor-recursive diagnostic namespace.

### Public Answer APIs

The main answer functions are:

- `formula-answers`: symbolic answers for one formula and selected free noms;
- `query-answers`: symbolic answers for a query relative to a compiled program;
- `query-answer-diagnostics`: raw proof-stream snapshots at one call-depth;
- `query-stage-diagnostics`: diagnostics across staged call-depth values;
- `query-parity-answers`: closed ground parity answers through a named
  materialization mode;
- `query-ground-answers`: bounded Herbrand enumeration above the semantic
  kernel.

The default symbolic API is `query-answers`. The parity and ground helpers are
explicitly separate because they enumerate or materialize candidates above the
kernel instead of being the generic symbolic proof path.

Current `query-answers` also includes a narrow extensional fast path for known
closed `append` and `reverse` answer shapes. That path is part of the public
answer surface, not the ordinary kernel proof relation, and the raw matrix
probes intentionally bypass it when they need to measure the central proof
path.

## 16. Constructor-Recursive Layer

`proflog.kernel.constructor-recursive` is a generic proof layer over guarded
IR. It was introduced as a sidecar layer for constructor-recursive proof and
residual-settlement experiments. After ADR-0035, it is best read as a
diagnostic and comparison layer: the promoted public answer path uses
`proflog.answer-overlay` structural residual continuation instead.

It consumes `:guarded-clause-list`, not relation-specific code. Its state is a
plain host map:

```clojure
{:subst {...}
 :fuel n}
```

The layer:

1. freshens guarded alternatives;
2. binds relation parameters to actual arguments;
3. saturates equality guards through free-constructor unification;
4. discharges rigid constructor disequality guards;
5. recurses through positive defined calls;
6. solves residual formulas it understands; and
7. returns proof terms tagged with `constructor-recursive-*`.

It is conservative:

- it does not inspect list-family names;
- it only solves defined positive calls from guarded alternatives;
- disequality guards must be rigidly constructor-different;
- negative defined-call residual settlement succeeds by constructively proving
  the underlying atom through the same guarded layer.

### Residual Settlement

`constructor-recursive/settle-record` takes a public answer record and tries to
discharge negative defined-call residuals. If successful, it:

- refines `:bindings` through the constructor-recursive substitution;
- clears `:residuals`; and
- appends a `constructor-recursive-residual-settlement` proof.

ADR-0033 originally wired this into `answers/complete-structural-residuals`
behind a conservative classifier:

- every residual must be a negative call to a relation defined by the compiled
  program;
- the frontier must expose constructor demand somewhere; and
- wholly symbolic recursive families remain residuals.

This was how ordinary raw answer records could close list-family residual
frontiers without adding list-specific production dispatch.

That description is historically important but no longer the current default.
ADR-0035 replaced ordinary public residual settlement with answer-overlay
continuation before export, plus an answer-overlay exported-record continuation
backstop. The constructor-recursive namespace remains useful for focused tests,
proof-shape comparison, and guarded-IR experiments.

## 17. End-To-End Data Flow

The following walk-through describes the normal path for a public answer query.

### Step 1: Source Declaration

The caller defines a language:

```clojure
(language/language
  {:constants ['zero 'null]
   :functions {'s 1
               'cons 2}
   :relations {'append 3}})
```

The language layer normalizes constants, validates namespaces, and records
function and relation arities.

### Step 2: Source Clauses

The caller constructs clauses with AST constructors. For append, the source
shape is:

```text
append(xs, ys, zs) :-
  xs = null and zs = ys
  or exists head tail rest.
       xs = cons(head, tail)
       and zs = cons(head, rest)
       and append(tail, ys, rest)
```

The actual source is a tagged formula built with `ast/and-form`,
`ast/or-form`, `ast/exists-form`, `ast/eq-lit`, and `ast/pos-lit`.

### Step 3: Compilation

`language/compile-program` validates every clause, alpha-renames relation
parameters, converts bodies to NNF, computes `:negated-body`, flattens
alternatives, and builds guarded alternatives.

The result is a compiled program with synchronized ordinary, alternative, and
guarded views.

### Step 4: Query Validation

`answers/query-answers` validates the query against `(:language program)` and
checks that requested answer variables are distinct free noms in the query.

For a positive query such as:

```text
append(x, y, [a, b])
```

the answer layer searches the tableau for the negated query because success is
closure of the query's negation.

### Step 5: Query Entry

If the searched formula is a top-level program literal, the answer layer uses
`answer-overlay/prove-program-query-entryo`. That opens the matching compiled
clause at the query boundary and starts branch search with answer export
enabled.

The root query call does not consume recursive `call-depth`.

### Step 6: Branch Search

Inside the overlay, the branch evolves through explicit state:

- `env` maps compiled clause parameters and quantified binders to terms;
- `sigma` accumulates equality bindings;
- `neqs` stores delayed disequalities;
- `lits` stores atoms that may close later;
- `residuals` stores deferred calls when answer descent stops;
- `proof-vars` includes variables that can be used for symbolic closure;
- `fuel` limits micro-steps;
- `call-depth` limits recursive procedure-call descent;
- `proof` records how the branch closed or where it stopped.

Equality guards refine `sigma`. Procedure calls either descend or become
residuals. Saved atoms can be reopened after equality walking makes them
callable.

### Step 7: Raw State Reification

`answers/program-raw-answer-states` runs core.logic and receives tuples:

```clojure
[answer-vars-out sigma-out neqs-out residuals-out proof]
```

This is still internal state. It may contain reified noms, delayed
disequalities, residual calls, and proof structures.

### Step 8: Public Export

`export-program-answer-record` calls `export-answer-record` to:

- walk selected answer vars through `sigma-out`;
- walk residuals through `sigma-out`;
- convert `neqs-out` to residual `neq` formulas;
- rename internal variables consistently;
- validate all terms and formulas against the language; and
- attach proof evidence.

Historically, ADR-0033 structural residual completion could call the
constructor-recursive layer here. The current ADR-0035 path uses answer-overlay
structural residual continuation for promoted public answers. That continuation
may already have run before raw state export; an exported-record continuation
backstop also lives in `proflog.answer-overlay`.

### Step 9: Merge, Rank, Return

The answers layer merges duplicate records, sorts records by completion and
shape, optionally applies derivation-depth ordering, takes `proof-limit`
records, and returns public answer maps.

At this point the caller sees only declared object-language terms, residual
formulas, and proof evidence.

## 18. Diagnostics And Probes

The implementation has several diagnostic surfaces because many Proflog
questions are operationally difficult even when their semantics are small.

### Answer Diagnostics

`answers/query-answer-diagnostics` reports, for selected raw limits:

- raw proof count;
- search exhaustion;
- inadmissible export count;
- exported count;
- duplicate export count;
- unique answer count;
- sample records;
- proof-root counts;
- common proof signatures.

`answers/query-stage-diagnostics` repeats that across call-depth stages to
show whether deeper recursive answer stages are productive.

### List Kernel Matrix

`proflog.list-kernel-matrix-probe` builds a canonical append/reverse program
and runs parameterized ground and answer rows. It intentionally bypasses public
list materialization to expose what the raw kernel and answer path can do.

The corresponding tests are in:

- [`test/proflog/list_kernel_matrix_test.clj`](../test/proflog/list_kernel_matrix_test.clj);
- [`test/proflog/kernel/constructor_recursive_test.clj`](../test/proflog/kernel/constructor_recursive_test.clj);
- [`test/proflog/synthesis_modes_test.clj`](../test/proflog/synthesis_modes_test.clj).

### Host And Performance Probes

ADR-0032 added host-level probes for core.logic:

- `proflog.core-logic-host`;
- `proflog.core-logic-host-probe`;
- `proflog.core-logic-count-probe`;
- `proflog.core-logic-tabling-probe`.

These report the loaded core.logic implementation and instrument selected host
entry points. They are diagnostics and deployment checks, not semantic layers.

ADR-0036 and ADR-0037 add additional speculative probes:

- `proflog.relational-arithmetic` and its upstream-style tests translate
  faster-minikanren bit-list arithmetic;
- `proflog.relational-fuel-probe` and
  `proflog.relational-fuel-adapter-probe` test fuel replacement boundaries;
- `proflog.fd-fuel-synthesis-probe` records current finite-domain fuel
  synthesis behavior;
- `proflog.minikanren-constraints` is a temporary symbolic constraint overlay;
- `proflog.relational-maps-probe`, `proflog.l-ground-constraint-probe`, and
  `proflog.core-logic-disequality-probe` capture ADR-0037 evidence.

Those namespaces are intentionally explicit probes or overlays. Promote their
ideas only through a later ADR decision with production tests.

### Hard Family Overlay

`proflog.hard-family-overlay` is a named non-default overlay for restricted
hard-family evaluation. It can answer some statuses through a host-level
equality fast path before falling back to the ordinary query surface. It exists
beside the pure kernel and must remain visibly separate from default semantics.

## 19. Test Architecture

The fast greenfield suite is defined in `project.clj` as:

```text
lein test-proflog-fast
```

It covers the core stack:

- AST;
- language;
- normalization;
- substitution;
- tabling;
- existential disequality;
- gamma;
- formula profiling;
- kernel dispatch;
- propositional and first-order layers;
- Pelletier layering;
- kernel;
- proof;
- equality;
- Herbrand oracle;
- program calls; and
- query status.

The extended suite is:

```text
lein test-proflog-extended
```

It covers public answer behavior and program families:

- answers;
- integration families;
- list programs;
- quantified programs;
- extended query behavior;
- recursive synthesis;
- reverse program synthesis;
- synthesis modes;
- Nim synthesis.

Other important aliases:

```text
lein test-proflog-constructor-recursive
lein test-proflog-pelletier
lein test-proflog-parity
lein test-proflog-hard-families
lein probe-proflog-list-kernel-matrix <case-id>
lein probe-core-logic-host
lein probe-core-logic-count
lein probe-core-logic-tabling
```

Speculative ADR-0036/0037 checks are not all part of default suites. Run
focused namespaces directly, for example:

```text
lein test proflog.relational-arithmetic-test proflog.relational-arithmetic-upstream-test
lein test proflog.minikanren-constraints-test
lein test proflog.relational-fuel-adapter-probe-test
lein test proflog.relational-fuel-replacement-test
lein test proflog.relational-maps-probe-test
lein test proflog.l-ground-constraint-probe-test
lein test proflog.core-logic-disequality-probe-test
lein probe-relational-fuel-performance
lein test-proflog-fitting-programs
```

The [Test Matrix](TEST_MATRIX.md) defines the project-level coverage policy.
Every implementation ADR should name which test surfaces matter for its risk
area.

## 20. Semantic Boundaries

The default greenfield baseline is recorded in [Semantic Variants](SEMANTIC_VARIANTS.md).
The most important boundaries are:

- public answers must stay inside the declared object language;
- internal `par` terms must not escape as final answers;
- search divergence is not semantic falsity;
- the default kernel keeps L-groundness structural;
- symbolic disequality is the default, not eager disunifier enumeration;
- host projection must not silently enter the default semantic kernel;
- proofless fast paths must remain named overlays or preserve a proof mode.
- raw core.logic tabling is not a replacement for Proflog's canonical-state
  tabling layer;
- ADR-0036 bit-list fuel remains opt-in and production fuel remains the
  finite-domain host-integer relation;
- ADR-0037 probe namespaces are not production proof-search semantics;
- ADR-0038 promoted Fitting-program outcomes must be proved or classified by
  the core proof kernel after source translation, not by host-side semantic
  computation.

These boundaries explain many implementation choices that may otherwise look
indirect. For example, the language compiler builds several synchronized views
so the kernel can remain relational. The answer layer exports residuals instead
of projecting host values. The constructor-recursive layer consumes guarded IR
instead of matching list-family names.

## 21. Working Example: Reading A Query Answer

Suppose an answer record is:

```clojure
{:bindings [[r (app cons (app b)
                    (app cons (app a) (app null)))]]
 :residuals []
 :proofs [proof]}
```

Read it as:

- the requested answer variable `r` is bound to `[b, a]` in the declared list
  language;
- there are no remaining symbolic obligations;
- the attached proof vector contains at least one closure witness;
- if a `structural-residual-continuation` proof appears, then ADR-0035
  continued a constructor-demanded residual frontier through the answer
  overlay before the final public record was ranked;
- if a historical `constructor-recursive-residual-settlement` proof appears,
  then the older guarded-IR sidecar discharged an exported residual frontier.

If the record instead contains:

```clojure
:residuals [(neg (app plus _0 (app zero) (app zero)))]
```

then the record is an open symbolic family. It is not a closed ground answer.
It says the current bindings are valid subject to the remaining procedure-call
obligation. Public ranking will prefer a closed answer for the same bindings if
one is found later.

## 22. How To Extend The Stack Safely

When changing the implementation, first identify the layer being changed.

Language/compiler changes should preserve:

- declaration validation;
- no public `par`;
- alpha-renaming;
- `:negated-body = negate(:body)`;
- synchronized ordinary, alternative, and guarded views.

Kernel changes should preserve:

- explicit branch state;
- structural L-groundness;
- proof-variable discipline;
- no silent host projection in the default path;
- proof evidence;
- query success/failure semantics.

Answer changes should preserve:

- public language validation;
- answer-var distinctness and freeness checks;
- residual visibility;
- proof attachment;
- separation between symbolic answers and parity/ground materialization.

Constructor-recursive changes should preserve:

- generic use of guarded IR;
- no dispatch on list-family names;
- explicit fuel;
- proof tags identifying the layer.

Diagnostics and probes should preserve:

- the distinction between semantic behavior and operational measurement;
- opt-out paths for residual completion when raw frontier shape is the thing
  being measured;
- branch-local logs or ADR/AAR notes for rejected experiments.

## 23. Minimal Mental Model

The implementation can be remembered as five invariants:

1. The language layer owns source validity and compiled-program invariants.
2. The kernel owns branch closure through explicit tableau state.
3. Equality is explicit `sigma` plus symbolic `neqs`, not hidden rewriting.
4. The answer overlay exports controlled state: bindings, residuals, proofs.
5. Specialized layers may help, but they must be named, proof-producing or
   explicitly diagnostic, and separated from the default semantics.

Those invariants are what let Proflog remain both a research implementation and
a codebase that can be audited one layer at a time.
