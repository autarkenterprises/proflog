# ADR-0100 Explicit Counterexample: Fixed `sjas-axiom` Citation

Date: 2026-06-13

ADR: [ADR-0102](../adr/ADR-0102-sjas-counterexample-proof-targets.md)

Refuted claim: [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md)

## Statement Refuted

ADR-0100 quantified over formula-bearing structural certificates plus the bare
`sjas-axiom` citation and claimed:

```text
For every covered system code S, theorem code F, and first-fragment proof
certificate P:

  ProflogAccepts(P,S,F) iff SemPrf_D(decode(P),S,F)

and the encoding of P satisfies >= 5J bits.
```

This is false over the stated covered domain.

## Concrete Witness

Let:

```text
P = compact public proof-code term for the symbol sjas-axiom
S = Tableau-0 SJAS system with unary function symbol f and beta axiom F

t0      = (app 1)
t(n+1)  = (app f t(n))
F       = (eq t8 t8)
```

The accepted public proof code is:

```text
(app code-3
     (app dbl (app dbl (app add (app dbl (app add (app dbl (app dbl (app 1))) (app 1))) (app 1))))
     (app 1)
     (app dbl (app add (app dbl (app add (app dbl (app add (app dbl (app 1)) (app 1))) (app 1))) (app 1))))
```

It has:

```text
proof-byte-count(P) = 3
proof-bit-count(P)  = 18
J(F)                = 18
5J(F)               = 90
```

The accepted citation therefore satisfies:

```text
ProflogAccepts(P,S,F)
proof-bit-count(P) = 18 < 90 = 5J(F)
```

That contradicts the ADR-0100 proof-size conclusion.

## Executable Evidence

The counterexample is encoded as:

```text
test/proflog/willard_sjas_test.clj
sjas-axiom-citation-counterexamples-adr-0100-size-claim
```

The focused selector passes:

```text
lein test :only proflog.willard-sjas-test/sjas-axiom-citation-counterexamples-adr-0100-size-claim
Ran 1 tests containing 3 assertions.
0 failures, 0 errors.
```

The test constructs the system, validates that `tableau-proof/3` accepts the
fixed `sjas-axiom` certificate for the large beta axiom, computes `J`, and
checks that the fixed proof-code bit count is strictly less than `5J`.

## Why This Refutes ADR-0100 As Stated

The failure is not a runtime bug. It is a theorem-target mismatch.

The bare `sjas-axiom` certificate is a citation: it says the theorem code is a
member of the encoded axiom basis. The formula content is carried by `S` and
`F`, not by `P`. If the theorem states the lower bound over `P` alone, a fixed
citation can always cite larger beta formulas and eventually violate `>= 5J`.

The proof can be repaired only by changing the theorem:

- exclude bare `sjas-axiom` from the proof-size theorem;
- replace it with a formula-bearing axiom leaf carrying formula bytes;
- or define the SJAS proof object as a combined object including the relevant
  theorem/system payload, then prove that combined encoding is
  literature-compliant.
