# Syntax And Normalization

This file covers:

- `test/proflog/ast_test.clj`
- `test/proflog/language_test.clj`
- `test/proflog/subst_test.clj`
- `test/proflog/normalize_test.clj`
- `test/proflog/pretty_test.clj`

## AST Constructors Stay Uniform

The greenfield AST uses one tagged shape for application:

```clojure
(ast/app-term 'succ (ast/var-term a))
=> (app succ (var a))
```

The same constructor style extends to literals and quantifiers:

```clojure
(ast/forall-form a (ast/pos-lit (ast/app-term 'even (ast/var-term a))))
(ast/once-forall-form a (ast/pos-lit (ast/app-term 'even (ast/var-term a))))
```

So the raw tree stays regular even when the operational meaning differs.

## Language Validation Rejects Bad Surface Terms Early

The language layer blocks malformed user programs and queries before the kernel
sees them. Representative failures are:

- undeclared relation symbol: `mystery`
- undeclared function symbol: `weird`
- wrong relation arity: `even(x, zero)`
- internal parameter terms `par(...)` inside surface programs

The same layer also compiles multiple surface clauses for one relation into one
core clause with an `or` body. For example:

```clojure
value(x) :- x = zero.
value(x) :- x = one.
```

becomes one compiled body equivalent to:

```clojure
x = zero or x = one
```

## Substitution Respects Quantifier Shadowing

Representative substitution:

```clojure
forall x. (value(x) and value(y))
```

under the environment:

```clojure
x -> zero
y -> one
```

becomes:

```clojure
forall x. (value(x) and value(one))
```

The inner binder for `x` is preserved. Only the free occurrence of `y` is
rewritten.

## NNF Translation Preserves The Operational Quantifier Distinction

One representative normalization is:

```clojure
not exists x. (p(x) and not q(x))
```

which becomes:

```clojure
once-forall x. (not p(x) or q(x))
```

This is the important greenfield operational point: negated existential clause
bodies normalize to `once-forall`, not to an ordinary re-enqueued universal.

## Pretty Printing Collapses Ground Peano Structure

The pretty layer turns ground Peano numerals into ordinary integers while
leaving symbolic structure intact. For example:

```clojure
(app move (app s (app s (app zero))) (var x))
```

pretty-prints as:

```clojure
(move 2 x)
```

The same happens inside answer records, so bindings and residuals can show
`2`, `1`, and similar numerals directly while unresolved variables remain in
AST form.
