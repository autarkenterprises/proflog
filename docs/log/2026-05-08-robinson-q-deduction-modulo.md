# Robinson Q And Deduction Modulo Notes

Date: 2026-05-08

## Context

The discussion considered how Robinson arithmetic Q could be represented in
Proflog and how its axioms might become tableau-level theory rules.

Robinson Q is a first-order theory over equality with term-forming symbols:

```text
zero
s/1
add/2
mul/2
```

In Proflog that means the language declaration should put those symbols in the
function/constant namespace, not the relation namespace:

```clojure
(pf/language
  (constants zero)
  (functions (s 1)
             (add 2)
             (mul 2))
  (relations))
```

The formulas Q1-Q7 are then ordinary first-order formulas over equality. In a
plain tableau treatment, proving Q7 from Q is trivial if Q7 is among the
assumptions: the proof is a tableau closure for `Q7 and not Q7`, not a
computation of multiplication.

## Functions And Relations

The key distinction remains:

- function symbols build terms and have no proof procedure of their own;
- relation symbols create atoms that the Procedure Call Rule can expand through
  compiled Proflog clauses;
- quantified variables range over object-language terms, not over functions or
  relations.

Therefore `mul(x, s(y))` as a function term does not execute. It can only be
reasoned about through equality axioms or theory conversion. A relational
arithmetic predicate such as `times(x, y, z)` would be a different operational
encoding.

## Deduction Modulo Shape

A possible `:robinson-q` profile would promote some Q axioms from ordinary
formulas into theory machinery:

```text
add(x, zero) -> x
add(x, s(y)) -> s(add(x, y))
mul(x, zero) -> zero
mul(x, s(y)) -> add(mul(x, y), x)
```

Q1 and Q2 align with constructor equality behavior:

```text
s(x) != zero
s(x) = s(y) -> x = y
```

Q3 is different:

```text
forall y. y = zero or exists x. s(x) = y
```

It is not a terminating rewrite rule. It is a predecessor-or-zero case split:

```text
t = zero
or
t = s(p)    p fresh
```

That case split would need relevance and fuel controls because it can easily
create infinite or irrelevant predecessor search, especially in the presence of
nonstandard Q models.

## Proof Of An Axiom Formula Under The Profile

If Q7 is encoded as the conversion rule:

```text
mul(x, s(y)) -> add(mul(x, y), x)
```

and the user asks the prover to prove the formula:

```text
forall x y. mul(x, s(y)) = add(mul(x, y), x)
```

then the proof is no longer "Q7 is an axiom". It is a theory-conversion proof:

1. open the universal quantifiers with arbitrary parameters `a` and `b`;
2. reduce the left side by the `mul-succ` rewrite:

   ```text
   mul(a, s(b)) -> add(mul(a, b), a)
   ```

3. compare the normalized sides by reflexive equality;
4. record a proof step such as `(q-rewrite mul-succ)`, followed by equality
   reflexivity.

Equivalently, a refutation tableau for the negation:

```text
exists x y. mul(x, s(y)) != add(mul(x, y), x)
```

would instantiate witnesses, normalize the disequality modulo the Q rewrite,
and close because it becomes:

```text
add(mul(a, b), a) != add(mul(a, b), a)
```

The proof remains nontrivial in the proof object because it uses an explicit
theory conversion step. But it is not a derivation from Q7 as an assumption;
Q7 has been moved into definitional equality for the profile.

## Design Boundary

Promoting Q axioms to rules changes proof meaning. Ordinary formulas produce
assumption-driven tableau proofs. Deduction modulo produces proofs modulo a
trusted conversion relation. If Proflog implements this, proof objects must
record theory steps explicitly so users can audit which arithmetic conversions
were used.
