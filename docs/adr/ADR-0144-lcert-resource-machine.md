# ADR-0144: A Resource-Conserving Machine For λᶜᵉʳᵗ

- Status: proposed
- Date: 2026-09-29
- Branch: `claude/sjas-lobian-obstacle-r3csa9`, the branch of this research
  line (see ADR-0143)
- AAR: none yet

## Context

λᶜᵉʳᵗ's consistency proof (T1) descends on the token budget. It needs strict
overhead: a certificate declaring `m` tokens has more than `m` nodes (R4
Lemma 2.7). That property comes from a rule of the encoding, E3, which writes
out every token of a derivation's context. The user's objection (2026-09-29):

> I understand in an academic sense that rules on the primitive components of
> a formal system constrain that formal system, but there remains the
> unsatisfied sense that this encoding, or any other such rule, could "just"
> be changed, and the system would lose its desirable properties. A machine
> model would tie the enforcement of the encoding to a mechanical operation,
> and increase my confidence in its inviolability.

**What an audit of the existing evaluator shows** (2026-09-29; the addendum
of the
[2026-09-28 note](../log/2026-09-28-lcert-self-justification.md), §C3):
- **Conservation holds on a real run.** On the minting lineage promised
  10^100 tokens, 37,919 tokens were created, all by materializing the lazy
  supply. That is exactly the certificate nodes minted. `reflect` created
  none. Each reflection burned its whole certificate: 18,831, 18,831 and 257
  nodes, since `m = 0` for these closed agents.
- **The handoff is not checked, but assumed.**
  - `reflect` hands the decoded program `(take m (tokens v))`, the
    certificate's own tokens. It never checks `m < ‖v‖`; it relies on
    `Check`, that is on E3.
  - `eval-deriv` does not check that it received exactly `m` tokens. Handed
    too few, a program either binds a token variable to the wrong token
    silently (de Bruijn shift), or fails with an incidental
    `IndexOutOfBoundsException`.
  - On unreachable paths, `default-value` at `◇` creates a phantom token.

So today strict overhead is enforced by a checker rule upstream, not by the
runtime. The user's suspicion is correct about the implementation.

**What a machine can and cannot secure** (the note, §C3):
- **It can** make three properties mechanical, whatever the encoding:
  - no instruction creates a token;
  - a certificate *carries* the tokens it grants, rather than *describing*
    them;
  - `reflect` hands on at most `‖v‖ − 1` of the certificate's own cells and
    destroys the rest.

  Then strict overhead is the arithmetic of the `reflect` instruction.
  Every run performs at most as many reflections as it holds cells, so every
  run terminates or traps. A terminating run produces no value of an empty
  type, so no fabricated evidence.
- **It cannot** make the *logic* consistent by itself. T1 is a theorem about
  the typing rules. The machine's role is to show what E3 is: exactly the
  typing rule under which a well-typed `reflect` never traps (progress).
  Changing E3 would not strengthen the system. It would make typing promise
  tokens the machine cannot hand over, and that shows up as traps, never as
  created tokens.

**Prior art** (added 2026-10-01; the
[2026-09-28 note](../log/2026-09-28-lcert-self-justification.md), §F4).
- Part 2 makes one discipline mechanical: a node is made only by consuming
  a free cell, and there is no allocation instruction. That is Hofmann's
  non-size-increasing discipline (LFPL, LICS 1999).
- λᶜᵉʳᵗ's `◇` descends from it (2026-09-26 assessment).
- LFPL's non-size-increasing theorem is the model for Part 2's
  conservation theorem.

**Scope.** This ADR is a machine model *of λᶜᵉʳᵗ*.
- The checker stays an oracle, so that the machine's theorems hold for
  every checker.
- A general cons-free or LFPL-style language, of the one-sorted kind the
  note's F3–F4 discuss, is out of scope. It would need its own ADR.

## Decision

Build the machine in three parts, test-first.

**Part 1 — harden the Clojure evaluator.**
- `reflect` checks `m < ‖v‖` and hands on exactly `m` tokens, trapping
  otherwise.
- `eval-deriv` checks that it received exactly as many tokens as the context
  declares.
- `default-value` at `◇` traps instead of creating a phantom token.
- A conservation monitor (a dynamic var) records tokens created, handed on
  and burned. Tests assert, on the ADR-0143 runs, that:
  - `created = supplied`;
  - every reflect burns at least one token;
  - the tokens live at the end plus those burned equal those created.

**Part 2 — an independent abstract machine, in Scheme with miniKanren.**
This follows the user's standing preference for Scheme and miniKanren. Chez
Scheme 9.5 and Guile 3.0 are installable here via apt; the choice between
them is made at implementation.
- A heap of cells, with linear pointers. `node` turns one free cell into a
  certificate node. There is no allocation instruction.
- `reflect` takes a certificate pointer:
  - reads the code;
  - calls a checker, which is a *parameter* (an oracle);
  - destroys the root cell;
  - hands `m` of the remaining cells to the decoded program;
  - destroys the rest.

  If `m ≥ ‖v‖`, it traps.
- Machine theorems, for every checker oracle, proved on paper and tested:
  - conservation;
  - at least one cell burned per `reflect`;
  - at most `N` reflects on `N` cells;
  - no value of an empty type in a terminating run.
- A miniKanren relational model of the step relation searches exhaustively
  for small programs, up to a fixed size, that violate the invariants. None
  is expected.
- The theorem "E3 ⟺ progress for reflect" is proved on paper. It is shown
  by running the machine with λᶜᵉʳᵗ's checker (no traps) and with a
  compact-budget checker (traps).

**Part 3 — cross-validation.** The Scheme machine and the hardened evaluator
run the ADR-0143 delegation and minting lineages. Their token accounts must
agree node for node. Certificates go from Clojure to Scheme as S-expression
codes. The Scheme side uses a recorded oracle (the accept/reject decisions
Clojure's `Check` made) rather than reimplementing `Check`.

## Consequences

- The research directory gains a Scheme component with its own run script.
  Proflog's suites do not run it, as with the rest of
  `research/lcert-tiling`.
- The conservation argument no longer depends on trusting the encoding. It
  depends on the machine's instruction set, which is small enough to audit.
- The evaluator's behaviour changes only on paths that are unreachable in
  well-typed runs: mismatched token handoffs and phantom defaults now trap.

## Test Obligations

Red first, then green.
- **Part 1.**
  - A test that `eval-deriv` with too few tokens traps; red now (it
    misbinds).
  - A test that a `reflect` whose certificate declares `m ≥ ‖v‖` traps. The
    certificate is forged below `Check`, at the evaluator level.
  - A test that default `◇` traps.
  - Conservation-monitor tests on the minting and delegation lineages.
- **Part 2.**
  - Unit tests per instruction.
  - Invariant tests under two oracles (λᶜᵉʳᵗ-style, and compact-budget).
  - The miniKanren search returns no counterexample up to the stated bound,
    and does return one when the machine is deliberately given a
    cell-creating instruction (a mutation test).
- **Part 3.** Token accounts agree on both lineages.

## Exit Criteria

| Part | Succeeds if | Fails (and is then recorded) if |
| --- | --- | --- |
| 1 | All handoff mismatches trap; the monitor shows conservation and burn ≥ 1 on every ADR-0143 run; the fast and extended suites stay green | A well-typed run trips the new checks (then E3 or T3 has a gap, which would be a finding against the calculus) |
| 2 | The invariants hold under every oracle tested; the mutation test is caught; progress holds exactly for oracles that respect `m < ‖v‖` | A conserving-machine run creates a cell, or burns none at some reflect |
| 3 | Accounts agree node for node | They disagree (then one of the two is wrong, and the discrepancy is the finding) |
