# Worked Example: Legacy GV Probes In Greenfield

Date: 2026-04-24
Branch: `adr-0014-generic-legacy-evaluation`

This note records the first greenfield probes against the legacy
group-verifier (`GV`) family.

## Purpose

The point of these probes is not just "does greenfield have a group-theory
story?" The narrower question is:

```text
Can greenfield resolve the exact legacy-style GV formulas, and if so, through
which semidecision surface?
```

That matters because the legacy prover's documented `GV` boundary was:

- some simple group axioms close,
- precomputed associativity on `Z₂` closes,
- full 7-universal associativity on `Z₂` does not,
- and the truth-side associativity probe on the non-group magma is also
  intractable.

## Probe Surface

The exploratory runner is:

```bash
lein probe-proflog-gv
```

Source: [src/proflog/gv_probe.clj](/home/jpt4/code/proflog/src/proflog/gv_probe.clj:1)

It rebuilds the legacy `GV` formulas in the greenfield AST and reports one of
three semidecision surfaces at a time:

- `status` -> `query-status`
- `succeeds` -> `query-succeeds-within`
- `fails` -> `query-fails-within`

Example commands:

```bash
timeout -k 10s 60s lein probe-proflog-gv z2-identity status 5000
timeout -k 10s 60s lein probe-proflog-gv z2-full-assoc-truth status 15000
timeout -k 10s 60s lein probe-proflog-gv z2-full-assoc-truth succeeds 15000
timeout -k 10s 60s lein probe-proflog-gv non-group-full-assoc status 15000
```

The shell `timeout` is the outer guard. The probe timeout is the inner
greenfield query budget.

## Results

Measured on `2026-04-24`:

- `z2-identity`, `status`, `5000 ms`
  - result: `:succeeds`
  - elapsed: about `6.1 s`

- `z2-closure`, `status`, `5000 ms`
  - result: `:unresolved`
  - elapsed: about `5.1 s`

- `z2-inverses`, `status`, `5000 ms`
  - result: `:unresolved`
  - elapsed: about `6.7 s`

- `z1-full-assoc-truth`, `status`, `15000 ms`
  - result: `:unresolved`
  - elapsed: about `23.0 s`

- `z2-precomputed-assoc-truth`, `status`, `5000 ms`
  - no result before the outer `60 s` timeout

- `z2-full-assoc-truth`, `status`, `15000 ms`
  - no result before the outer `60 s` timeout

- `z2-full-assoc-truth`, `succeeds`, `15000 ms`
  - no result before the outer `60 s` timeout

- `non-group-full-assoc`, `status`, `15000 ms`
  - no result before the outer `60 s` timeout

- `non-group-full-assoc`, `fails`, `15000 ms`
  - no result before the outer `60 s` timeout

## Current Interpretation

This first `GV` slice does **not** show greenfield and legacy having different
overlapping strengths.

Instead, it shows:

- greenfield can resolve the simplest `GV` identity case,
- but greenfield is currently weaker than legacy on the broader `GV` family,
- because even legacy-solvable cases such as `Z₂` precomputed associativity and
  `Z₁` full associativity do not currently resolve in the measured greenfield
  windows.

So the current architectural takeaway is:

- greenfield does not yet have a distinct `GV` capability win that would argue
  for preserving the present structure unchanged,
- and the repo is therefore justified in considering larger architectural
  revision if later ADR-14 probes keep the same shape.
