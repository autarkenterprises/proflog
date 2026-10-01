# ADR-0144: A Resource-Conserving Machine For λᶜᵉʳᵗ

- Status: proposed
- Date: 2026-09-29; revised 2026-10-01
  - Clojure and core.logic, at the user's direction.
  - A machine definition, which the first draft lacked.
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
[2026-09-28 note](../log/2026-09-28-lcert-self-justification.md), §C4):
- **Conservation holds on a real run.** On the minting lineage promised
  10^100 tokens, 37,919 tokens were created, all by materializing the lazy
  supply. That is exactly the certificate nodes minted. `reflect` created
  none. Each reflection burned its whole certificate: 18,831, 18,831 and 257
  nodes, since `m = 0` for these closed agents.
- **The handoff is not checked, but assumed.**
  - `reflect` hands the decoded program `(take m (tokens v))`: the first `m`
    of the certificate's own tokens, in preorder, root first. It never checks
    `m < ‖v‖`; it relies on `Check`, that is on E3.
  - `eval-deriv` does not check that it received exactly `m` tokens. Handed
    too few, a program either binds a token variable to the wrong token
    silently (de Bruijn shift), or fails with an incidental
    `IndexOutOfBoundsException`.
  - On unreachable paths, `default-value` at `◇` creates a phantom token, and
    a `reflect` whose certificate fails `Check` returns a default value
    instead of failing.

So today strict overhead is enforced by a checker rule upstream, not by the
runtime. The user's suspicion is correct about the implementation.

**What a machine can and cannot secure** (the note, §C4):
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
- The machine makes one discipline mechanical: a node is made only by
  consuming a free cell, and there is no allocation instruction. That is
  Hofmann's non-size-increasing discipline (LFPL, LICS 1999).
- λᶜᵉʳᵗ's `◇` descends from it (2026-09-26 assessment).
- LFPL's non-size-increasing theorem is the model for the conservation
  theorem below.

**Scope.** This ADR is a machine model *of λᶜᵉʳᵗ*.
- The checker stays an oracle, so that the machine's theorems hold for
  every checker.
- A general cons-free or LFPL-style language, of the one-sorted kind the
  note's F3–F4 discuss, is out of scope. It would need its own ADR.

**Language** (the user, 2026-10-01). Clojure with core.logic, Clojure's
miniKanren, rather than Scheme with miniKanren. This keeps one language
environment with the λᶜᵉʳᵗ implementation and the Ansatz proofs.
- `org.clojure/core.logic` 1.1.1 is on Maven Central, checked reachable
  2026-10-01. For JVM use it needs only Clojure.

## The machine

**What it is.** A *resource kernel*: a heap of cells, and six instructions
over linear cell handles.
- λᶜᵉʳᵗ's evaluator becomes a *client* of the kernel. It holds no cells of
  its own, and every operation on tokens or certificates is a kernel
  instruction.
- The theorems below quantify over every instruction sequence, every
  initial heap and every oracle answer.
  - The client only chooses which instructions to issue, and the oracle
    only chooses answers.
  - The kernel checks what it relies on against its own state: that the
    handle is held, that the cell is of the right kind, and `m` against
    the tree's actual node count.
  - So the *resource* theorems hold whatever the evaluator asks and
    whatever the checker answers. They are invariants of the transitions,
    not facts trusted from callers.
  - This is the LCF principle. Theorems are an abstract type whose only
    constructors are the inference rules, so untrusted tactic code cannot
    forge one. Here cells are such a type, and the six instructions are
    its only operations.

  *(Qualified 2026-10-01; the first version said flatly "they do not depend
  on the evaluator, the encoding or the checker being correct".)* What the
  theorems still depend on is under "What the guarantee rests on" below.

**What the guarantee rests on.**
- **The kernel's own correctness.** It is small; it is searched
  exhaustively in core.logic; and its definition's theorems are checked in
  Ansatz. The Ansatz proofs concern a model, so the agreement between the
  Clojure kernel and that model is tested, not proved.
- **Complete mediation.** The client must reach cells *only* through the
  kernel: it must be unable to create a token, forge a handle, or touch
  the heap.
  - Today this fails. The evaluator makes tokens itself (`token`, and
    `default-value` at `◇`), and the theorems say nothing about tokens
    made outside the kernel.
  - Part 1 therefore makes the kernel the only creator of tokens and
    handles. Handles are opaque objects that the kernel checks by identity
    against `K`, so a forged handle traps.
  - On the JVM, this isolation is a discipline enforced by code inspection
    and tests, not by hardware. Reflection can reach private state. So the
    guarantee covers clients that use only the kernel's interface, which a
    static test checks.
- **Resources only.** The theorems are about cells. Whether an accepted
  certificate is a genuine derivation, and so the logic's consistency (T1),
  still depends on the checker and λᶜᵉʳᵗ's typing rules. Whether runs
  *trap* also depends on the checker: theorem 6 is exactly that
  dependence. A checker that breaks E3 cannot create cells, but it can
  make runs trap.

**Values.**
- **Data**: labels, codes, numbers, Booleans, closures and decoded
  programs. It may be copied. The kernel never inspects it except to return
  it.
- **Handles**: names of cells. They are linear. An instruction that takes a
  handle consumes it, so a second use of the same handle traps, even if the
  client has duplicated it.
- **R-values**, λᶜᵉʳᵗ's certificates: either `(leaf ℓ)`, which is data and
  owns no cell, or a handle to a node cell.

**State.** `Σ = ⟨H, K, b, σ⟩`, where:
- `H` is the heap: a finite map from cell ids to contents, each either
  `free` (a token, λᶜᵉʳᵗ's `◇`) or `(node ℓ r₁ r₂)`, with `r₁` and `r₂`
  R-values;
- `K` is the set of handles the client holds;
- `b` is the number of cells burned so far;
- `σ` is the status: `run`, or `(trap reason)`.

**Ownership (Own).** Every cell id in `dom H` is in `K` or is a child of
exactly one node in `H`, never both. Trees are finite and acyclic.

**Initial states.** Any `⟨H₀, K₀, 0, run⟩` satisfying Own. Two are used:
- **budget `n`**: `n` free cells, all in `K₀`. These are λᶜᵉʳᵗ's tokens
  `$1 … $n`.
- **supply `N`**: one spine of `N` node cells, with its root in `K₀`. This
  is λᶜᵉʳᵗ's supply certificate. A lazy supply implements the same initial
  state, laying out each cell when it is first touched. Its equivalence to
  the eager one is a lemma, as in ADR-0143 Step 2.

`supplied = |dom H₀|`.

**Size and order.** `‖c‖` is the number of node cells in the tree at `c`.
Preorder lists them root first, then left before right.

**Instruction set.** Every instruction names handles that must be in `K`.
Using a handle not in `K`, or a cell of the wrong kind, traps. A trap stops
the machine; no instruction fails in any other way.

| Instruction | Precondition | Effect | free | node | burned |
| --- | --- | --- | --- | --- | --- |
| `NODE c ℓ r₁ r₂ → c` | `H(c) = free`; each `rᵢ` a leaf or a node handle in `K`; the two distinct | `H(c) := (node ℓ r₁ r₂)`; `r₁`, `r₂` leave `K` | −1 | +1 | 0 |
| `SPLIT c → (c, ℓ, r₁, r₂)` | `H(c) = (node ℓ r₁ r₂)` | `H(c) := free`; the children's handles enter `K` | +1 | −1 | 0 |
| `READ c → code` | `H(c)` a node | none; returns the tree's code, as data | 0 | 0 | 0 |
| `INSPECT c D → bool` | `H(c)` a node | none; returns whether the oracle accepts `(READ c, D)` | 0 | 0 | 0 |
| `DROP c` | — | removes `c`'s tree from `H` and `c` from `K` | −1 if free | −‖c‖ if node | +1, or +‖c‖ |
| `REFLECT c D → (p, d₁ … d_m)` | `H(c)` a node; the oracle returns `accept(m, p)` on `(READ c, D)`; `m < ‖c‖` | of the tree's `k = ‖c‖` cells in preorder, the first `m` become free and enter `K` as `d₁ … d_m`; the other `k − m` are removed | +m | −k | +(k − m) |

There is no allocation instruction and no copy instruction. A `REFLECT` the
oracle rejects traps.

**The oracle.** `O : (code, type code) → reject | accept(m, p)`. It is a
parameter of the machine.
- **For λᶜᵉʳᵗ**: accept iff `Check(code, ⌜D⌝)`. Then `p` is the decoded
  derivation, and `m` is the length of its context.
- **To the kernel**, `p` is opaque data.

**How λᶜᵉʳᵗ runs on it.** This is the client contract, which Part 1's
hardened evaluator follows.

| λᶜᵉʳᵗ | Kernel |
| --- | --- |
| tokens `$1 … $n` | the budget-`n` initial handles |
| `node d ℓ r₁ r₂` | `NODE` |
| `caseR` on a node; `itR` | `SPLIT`; `itR` recursively |
| `caseR` on a leaf; `leaf ℓ` | nothing (data) |
| `inspect` | `INSPECT` |
| `print r` | `READ`, then `DROP` |
| `reflect D r e` | `REFLECT`, then run `p` on `d₁ … d_m` |
| an R-value or `◇` left unused | stays held, or `DROP` |
| everything else | nothing |

The handoff order, the first `m` cells in preorder, is the evaluator's own
`(take m (tokens v))`. So the client and legacy modes agree node for node.

**Theorems.** For every initial state, every oracle and every finite
instruction sequence:
1. **Own is preserved.**
2. **Conservation.** free + node + `b` = `supplied`, at every state.
3. **Burn.** A `REFLECT` that does not trap increases `b` by
   `k − m ≥ 1`.
4. **Bound.** At most `supplied` `REFLECT`s do not trap.
5. **Physical cap.** No certificate given to `REFLECT` has more than
   `supplied − b` nodes. The evaluator's charged cap `n − ‖v‖ + m` is this
   live-cell count: a consequence, not a parameter.
6. **Progress iff E3.** Under an oracle `O`, no `REFLECT` of an accepted
   certificate traps iff `O` accepts only certificates whose `m` is less
   than their node count. λᶜᵉʳᵗ's `Check` has that property by E3 (R4 Lemma
   2.7). A compact-budget oracle does not.

At the client level, for λᶜᵉʳᵗ running on the kernel:
7. **Termination.** Every run ends or traps. This follows from 4, and from
   the termination of λᶜᵉʳᵗ without `reflect`, which has only structural
   recursion.
8. **No fabricated evidence.** No run that ends yields a value of the empty
   type.
   - λᶜᵉʳᵗ has no constructor of `0`.
   - A rejected `REFLECT` traps.
   - 4 bounds the depth of reflection.

Theorems 1–6 are proved by cases on the instruction table, 7–8 by induction
on `b`.

## Decision

Build it in three parts, test-first, in Clojure with core.logic.

**Part 1 — harden the evaluator, and make it a client of the kernel.**
- `reflect` checks `m < ‖v‖` and hands on exactly `m` tokens, trapping
  otherwise. A certificate that fails `Check` traps.
- `eval-deriv` checks that it received exactly as many tokens as the context
  declares.
- `default-value` at `◇` traps instead of creating a phantom token.
- A switch runs the evaluator as a kernel client, following the contract
  above. Without it, the evaluator keeps its current behaviour on every path
  that a well-typed program reaches.
- **Complete mediation.** In client mode the kernel is the only creator of
  tokens and handles.
  - Token construction moves into `lcert.machine`.
  - Handles are opaque, and are checked by identity against the held set.
  - No other namespace may construct a token or handle, or read kernel
    state.

**Part 2 — the kernel, its logic model, and its proofs.**
- `lcert.machine`: the kernel as a pure step function on states (Clojure
  maps), with the oracle as an argument.
- A core.logic model of the same step relation, with counts in
  `core.logic.fd`.
  - At each `REFLECT` the oracle's answer is a fresh logic variable, so every
    accept, reject and `m` is explored.
  - The search covers all instruction sequences up to length `L`, from all
    initial heaps of at most `N` cells. The test fixes `L` and `N`, tuned for
    runtime.
  - It must find no violation of theorems 1–4.
- **Mutation tests.** The search must find violations in two altered
  kernels:
  - one with an `ALLOC` instruction, which makes a free cell;
  - one whose `REFLECT` lacks the `m < ‖c‖` check, and makes up any cells
    it lacks.
- **Ansatz.** Theorems 2 and 3 are stated over a list-of-cells model and
  checked by the Ansatz kernel, as ADR-0143 checked its key lemmas.
- `bin/deps` fetches `org.clojure/core.logic` 1.1.1 from Maven Central.

**Part 3 — cross-validation.** The evaluator runs ADR-0143's delegation and
minting lineages twice: in legacy mode, and as a kernel client. Results and
token accounts must agree node for node. Both runs use λᶜᵉʳᵗ's real `Check`
as the oracle, in one JVM.

## Consequences

- The research directory gains `lcert.machine`, its tests, and an Ansatz
  namespace. No Scheme component is added. Proflog's suites do not run it,
  as with the rest of `research/lcert-tiling`.
- The conservation argument no longer depends on trusting the encoding,
  the evaluator or the checker. It depends on six instructions, which are
  small enough to audit, and on the evaluator reaching cells only through
  them (complete mediation, tested). It says nothing about logical
  consistency, which stays with T1.
- The evaluator's behaviour changes only on paths that well-typed runs do
  not reach. Mismatched token handoffs, phantom defaults and rejected
  reflections now trap.

## Test Obligations

Red first, then green.
- **Part 1.**
  - A test that `eval-deriv` with too few tokens traps; red now (it
    misbinds).
  - A test that a `reflect` whose certificate declares `m ≥ ‖v‖` traps. The
    certificate is forged below `Check`, at the evaluator level.
  - Tests that default `◇`, and a reflection the checker rejects, trap.
  - **Mediation tests.**
    - A forged handle, one never issued by the kernel, traps at every
      instruction.
    - A static scan finds no construction of tokens or handles, and no read
      of kernel state, outside `lcert.machine`. It is red now, because
      `lcert.eval` constructs tokens.
- **Part 2.**
  - Unit tests per instruction, each covering its precondition, effect and
    counts.
  - Invariant tests under three oracles:
    - λᶜᵉʳᵗ's `Check`;
    - a compact-budget oracle, which must trap, as theorem 6 predicts;
    - the unconstrained oracle of the core.logic search.
  - The search finds no counterexample up to the stated bounds, and finds
    one in each mutant.
  - The Ansatz statements of theorems 2 and 3 check.
- **Part 3.** The two modes agree, in results and in token accounts, on
  both lineages.

## Exit Criteria

| Part | Succeeds if | Fails (and is then recorded) if |
| --- | --- | --- |
| 1 | All handoff mismatches and rejected reflections trap; in client mode the kernel is the only creator of cells (mediation tests green); the fast and extended suites stay green | A well-typed run trips the new checks. Then E3 or T3 has a gap, which would be a finding against the calculus |
| 2 | Theorems 1–4 hold in the search under every oracle; both mutants are caught; theorem 6's traps appear exactly for oracles that break `m < ‖c‖`; Ansatz checks theorems 2 and 3 | A kernel run creates a cell, or a `REFLECT` burns none |
| 3 | Results and accounts agree node for node | They disagree. Then one of the two is wrong, and the discrepancy is the finding |
