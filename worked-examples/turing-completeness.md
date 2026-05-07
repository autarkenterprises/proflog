# Turing Completeness Example

ADR-0044 demonstrates that Proflog can encode a known minimal
Turing-complete model: two-counter Minsky machines. The implementation lives in
`src/proflog/turing_completeness.clj`, and its opt-in regression suite is
`test/proflog/turing_completeness_test.clj`.

This is an expressive-power demonstration. It does not claim that arbitrary
machine reachability is decidable or cheap. The tests prove small finite runs
through the compiled Proflog program and record the current runtime boundaries.

## Machine Model

A two-counter machine has labels, two natural-number counters, and instructions
of these forms:

```text
inc0(label, next)          ; counter0++, goto next
inc1(label, next)          ; counter1++, goto next
decjz0(label, dec, zero)   ; if counter0 = 0 goto zero, else counter0-- and goto dec
decjz1(label, dec, zero)   ; if counter1 = 0 goto zero, else counter1-- and goto dec
halt-state(label)
```

Machine configurations are first-order terms:

```text
cfg(label, counter0, counter1)
```

Counters are Peano terms:

```text
zero
s(zero)
s(s(zero))
```

Two-counter machines are Turing-complete. Proflog therefore has the same
expressive lower bound when it can represent arbitrary finite instruction
tables and a generic transition relation over those tables.

## Prolog-Style Pseudo-Code

The generic interpreter is written as ordinary Proflog clauses. A representative
increment case is:

```prolog
step(before, after) :-
  exists label next c0 c1.
    before = cfg(label, c0, c1)
    and inc0(label, next)
    and after = cfg(next, s(c0), c1).
```

The recursive reachability relation is the reflexive transitive closure of
`step/2`:

```prolog
run(start, final) :- start = final.

run(start, final) :-
  exists middle.
    step(start, middle)
    and run(middle, final).
```

The bounded variant used by tests takes a Peano step counter:

```prolog
run-for(zero, start, final) :-
  start = final.

run-for(s(rest), start, final) :-
  exists middle.
    step(start, middle)
    and run-for(rest, middle, final).
```

The transfer-machine instruction table is:

```text
l0: if counter0 = 0 then halt else counter0-- ; goto l1
l1: counter1++ ; goto l0
```

In Proflog pseudo-code:

```prolog
decjz0(label, dec, zero-next) :-
  label = l0 and dec = l1 and zero-next = halt-label.

inc1(label, next) :-
  label = l1 and next = l0.

halt-state(label) :-
  label = halt-label.
```

Starting from `cfg(l0, 1, 0)`, the intended trace is:

```text
cfg(l0, 1, 0)
=> cfg(l1, 0, 0)
=> cfg(l0, 0, 1)
=> cfg(halt-label, 0, 1)
```

## Frontend Definition

The executable frontend uses reusable language declarations and prefix clause
operators. The language is shared by the transfer machine and a second
incrementer machine:

```clojure
(def counter-machine-language
  (pf/language
    (constants zero
               l0 l1 halt-label
               i0 ihalt)
    (functions (s 1)
               (cfg 3))
    (relations (inc0 2)
               (inc1 2)
               (decjz0 3)
               (decjz1 3)
               (halt-state 1)
               (step 2)
               (run 2)
               (run-for 3)
               (halt-config 1)
               (halts-in 2)
               (halts-in-steps 3))))
```

The generic interpreter source is quoted once and appended to each concrete
instruction table by `machine-program`:

```clojure
(defmacro machine-program
  [& instruction-forms]
  `(pf/proflog counter-machine-language
     ~@interpreter-source
     ~@instruction-forms))
```

The transfer machine is then just instruction-table clauses:

```clojure
(def transfer-machine
  (machine-program
    (|- (decjz0 label dec zero-next)
      (and (= label l0)
           (= dec l1)
           (= zero-next halt-label)))

    (|- (inc1 label next)
      (and (= label l1)
           (= next l0)))

    (|- (halt-state label)
      (= label halt-label))))
```

This shape matters: the interpreter is generic, and the concrete machine is not
hard-coded into a host evaluator.

## Backend Descent

The increment case above descends to one compiled relation clause for `step/2`.
Schematically, the source clause:

```clojure
(|- (step before after)
  (exists [label next c0 c1]
    (and (= before (cfg label c0 c1))
         (inc0 label next)
         (= after (cfg next (s c0) c1)))))
```

becomes a backend formula body:

```clojure
(exists
  (tie label
    (exists
      (tie next
        (exists
          (tie c0
            (exists
              (tie c1
                (and
                  (eq (var before) (app cfg (var label) (var c0) (var c1)))
                  (and
                    (pos (app inc0 (var label) (var next)))
                    (eq (var after)
                        (app cfg
                             (var next)
                             (app s (var c0))
                             (var c1)))))))))))))
```

The compiler stores that body with its normalized negation in the compiled
program. When the kernel sees `step(before, after)`, the Procedure Call Rule
looks up the compiled `step/2` alternatives, binds the formal parameters to the
actual call arguments, and proves the selected body.

## Evaluation Process

The forward transition tests call the kernel-backed query surface directly:

```clojure
(query/query-succeeds
  (tc/transfer-program)
  (ast/pos-lit
    (ast/app-term 'step
                  (tc/config 'l0 2 1)
                  (tc/config 'l1 1 1)))
  1
  32)
```

This proves the decrement branch of `decjz0`: from `cfg(l0, 2, 1)` the machine
can step to `cfg(l1, 1, 1)`.

The second-machine test reuses the same interpreter with a different
instruction table:

```clojure
(query/query-succeeds
  (tc/incrementer-program)
  (ast/pos-lit
    (ast/app-term 'halts-in-steps
                  (tc/numeral 1)
                  (tc/config 'i0 1 2)
                  (tc/config 'ihalt 2 2)))
  1
  48)
```

This is a bounded recursive run through `run-for/3`, not a host loop.

The answer-mode transfer example uses the public frontend evaluator:

```clojure
(pf/run (tc/transfer-program) [final]
  (exists [middle0 middle1]
    (and (step (cfg l0 (s zero) zero) middle0)
         (step middle0 middle1)
         (step middle1 final)
         (halt-config final)))
  {:fuel 96
   :call-depth 5
   :proof-limit 8
   :max-raw-proof-limit 32})
```

It exports:

```clojure
{:bindings [[final (cfg halt-label zero (s zero))]]
 :residuals []}
```

The partial-synthesis check asks Proflog to fill one argument of an instruction
relation:

```clojure
(pf/run (tc/transfer-program) [label]
  (inc1 label l0)
  {:fuel 48
   :call-depth 1
   :proof-limit 4
   :max-raw-proof-limit 16})
```

It exports:

```clojure
{:bindings [[label l1]]
 :residuals []}
```

## Test Results

Run the focused suite explicitly:

```text
lein test-proflog-turing-completeness
```

Current promoted checks:

| Test | Mode | Outcome | Focused runtime |
|---|---|---|---:|
| `two-counter-machine-step-cases-close-through-the-kernel` | forward | three `step/2` branch proofs close with procedure-call evidence | `31.46 s` |
| `same-interpreter-runs-a-second-instruction-table` | bounded forward recursion | `halts-in-steps(1, cfg(i0,1,2), cfg(ihalt,2,2))` succeeds | `26.51 s` |
| `frontend-run-exports-the-transfer-machine-final-config` | answer | exports `cfg(halt-label, 0, 1)` with no residuals | `73.66 s` |
| `frontend-run-can-partially-synthesize-an-instruction` | partial synthesis | exports `label = l1` for `inc1(label, l0)` | `21.68 s` |
| `turing-completeness-namespace-does-not-contain-a-host-step-evaluator` | source audit | no host query/answer evaluator or host `step`/`run` functions in the namespace | `9.85 s` |

The full opt-in suite passed with:

```text
Ran 5 tests containing 12 assertions.
0 failures, 0 errors.
elapsed_seconds 94.45
```

## Correctness And Shortcomings

Correctness guardrails:

- transition, run, and halt semantics are compiled Proflog clauses;
- concrete machines provide instruction facts only;
- tests inspect proof terms for procedure-call evidence;
- source audit rejects a hidden host evaluator in the TC namespace;
- `pf/run` answer rows require empty residuals for the promoted answers.

Operational shortcomings:

- the direct three-step transfer answer is slow even when the trace shape is
  supplied explicitly;
- a direct open predecessor query over `step/2` timed out inside a 180s wrapper;
- recursive `halts-in-steps` transfer probes for sampled multi-step transfers
  also timed out inside 180s wrappers;
- unbounded `run/2` is present for the expressiveness argument, but it should
  not be treated as a practical default enumerator for arbitrary machine
  histories.

The current result is therefore a minimum viable but genuine demonstration:
Proflog represents a Turing-complete machine model at the kernel source level,
and finite computations are evaluated by the proof kernel after frontend
translation.
