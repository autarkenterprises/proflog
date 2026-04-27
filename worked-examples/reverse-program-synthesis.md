# Reverse Program Synthesis

This file covers `test/proflog/reverse_program_synthesis_test.clj`.

## Fixed Clause-Shape Synthesis Works

The current relational kernel can synthesize a contradictory compiled clause
body when the compiled program shape is already fixed.

The test asks for a program whose only clause is structurally:

```clojure
{:relation 'p
 :params   [x]
 :body     (eq f g)
 :negated-body true}
```

with the side condition `f != g`.

Then it runs the positive call:

```clojure
p(one)
```

and obtains the proof:

```clojure
(pos-call (free-close))
```

So the kernel can relationally discover a body that fails by constructor clash.

## The Current Boundary Is Still Narrow

The second test supplies an inconsistent compiled program:

```clojure
{:relation 'p
 :params [x]
 :body (eq zero one)
 :negated-body (eq zero one)}
```

Both directions close:

```clojure
p(one)      => (pos-call (free-close))
not p(one)  => (neg-call (free-close))
```

That is the current limitation: the internal compiled representation does not
yet enforce coherence between `:body` and `:negated-body`. The fixed-shape
reverse-synthesis result is real, but it is not yet a sound surface-program
synthesis contract.

Operationally, that is why this namespace belongs in the extended semantic
surface rather than in any user-facing claim about relational source-program
synthesis. It is a worked example of what the kernel can currently do with a
directly supplied compiled shape, not evidence that arbitrary surface clauses
can already be synthesized soundly.
