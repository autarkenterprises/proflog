# λᶜᵉʳᵗ tiling research (ADR-0143)

This directory carries out the four steps of
[ADR-0143](../../docs/adr/ADR-0143-lcert-tiling-four-steps.md): does an
arbitrary, lazily produced token budget resolve the tiling-agents / Löbian
obstacle problem for λᶜᵉʳᵗ agents?

## Provenance

`lcert/` is a copy of `code/lcert` from `github.com/jpt4/sjas`, branch
`sjas-codification`, commit `f5a8490f51807b59b14eca72d564faae17a7c5be`
("lcert: a tutorial, run by the fast suite"). The metatheory it implements is
`nachlass/refinement/R4-metatheory.md` in the same commit, cited below as R4.

The first commit that adds this directory holds that copy unchanged, so every
later change is a diff against the original. Changes made here are proposals
for `jpt4/sjas`; nothing is pushed there.

Proflog does not load this code: it is not on `project.clj`'s source or test
paths, and proflog's suites do not run it.

## Running

Only a JDK (11 or later), `git` and `curl` are needed; neither the Clojure CLI
nor Leiningen is used.

```sh
research/lcert-tiling/bin/deps           # once: jars from Maven Central, Ansatz 0.2.115 from GitHub
research/lcert-tiling/bin/test-fast      # the language, without Ansatz (seconds)
research/lcert-tiling/bin/test-extended  # plus the Ansatz kernel, mechanized lemmas and slow probes
research/lcert-tiling/bin/test-fast lcert.eval-test   # one namespace, for a red/green step
```

`bin/deps` puts everything in `research/lcert-tiling/.deps/`, which git
ignores. Ansatz is built from its source tag rather than its Clojars jar
because Clojars may be unreachable from a sandbox; `bin/deps` refuses a
checkout at any other revision. Ansatz runs offline on its bundled Lean Init
environment (`ANSATZ_OFFLINE=1`); no Mathlib store is ever fetched.

## Results

The verdict and its evidence are in
[AAR-0143](../../docs/aar/AAR-0143-lcert-tiling-four-steps.md); the proofs
in [METATHEORY.md](METATHEORY.md). In short: an arbitrary, lazily produced
token budget does resolve tiling for successors that keep their parent's
proof system, and does not help successors that strengthen it.

| Step | What was done | Where it is checked |
| --- | --- | --- |
| 1. Caller-charged model, reflect at the agent type | The charged cap `n − ‖v‖ + m`; the first-order class 𝓕, which holds every agent type; the Reflect case for 𝓕 | `lcert/ansatz/lcert/charged.clj` (Ansatz); `lcert.charged-reflect-test` |
| 2. Primitive destructor, lazy finite supplies | `caseR` in every layer; R4's metatheory re-checked; a lazy supply is a promise N denoting the spine of N; a minting lineage on 10^100 tokens | `lcert/ansatz/lcert/supply.clj`; `lcert.caser-test`, `lcert.lazy-supply-test`, `lcert.minting-agent-test` |
| 3. The higher-order case | R4's sets fail at shape (c); a world-indexed Kripke model validates reflect at every closed reflect-free type | `lcert/ansatz/lcert/kripke.clj`; `lcert.world-reflect-test` |
| 4. Changed proof systems | Seven child systems against a parent; trust stops exactly at derivability in the parent's own rules | `lcert.changed-system-test` |

[A later note](../../docs/log/2026-09-28-lcert-self-justification.md) asks
whether the calculus is self-justifying, and whether its resource discipline
is the reason. It also corrects Step 4: the soundness of `ax-con` is open,
not "true by T1".

The calculus these steps propose, λᶜᵉʳᵗ₁, is the vendored one run with
`lcert.syntax/*extensions*` `#{:caseR}` (the default),
`lcert.syntax/*reflect-class*` `:all` and `lcert.eval/*charged-cap*` `true`.
Its defaults otherwise stay those of the original, so the original tests
keep their meaning.

## Map of the changes to `lcert/`

- `src/lcert/syntax.clj`: the reflect classes and policy; the rule
  extensions (`caseR`, and Step 4's axioms `ax-con`, `ax-bot`); `case-r`.
- `src/lcert/typing.clj`, `check.clj`, `encode.clj`, `reduce.clj`: the
  `CaseR`, `AxCon` and `AxBot` rules, each gated by its extension.
- `src/lcert/eval.clj`: `caseR`; lazy supplies with the materialization
  guard; the charged cap (from the experiments patch).
- `src/lcert/examples.clj`: `out-prim-form`, `parse-prim-form`.
- `ansatz/lcert/{charged,supply,kripke}.clj`: the mechanized lemmas.
- `test/lcert/*`: one namespace per claim, listed above; `ho_forms.clj`
  holds the agent and shape forms.
