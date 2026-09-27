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
