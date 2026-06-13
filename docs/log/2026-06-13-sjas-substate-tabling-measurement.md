# SJAS Substate Re-derivation Measurement (ADR-0105)

Date: 2026-06-13
ADR: [ADR-0105](../adr/ADR-0105-sjas-substate-tabling-investigation.md)

This note records the methodology and result of the ADR-0105 measurement that
decided *against* tabling as the systemic fix for the subst-prf
negative-exhaustion wall. The probe scaffolding was reverted after measuring
(to keep the kernel clean); this note makes the result reproducible.

## Methodology

Temporarily add a measurement hook to `proflog.kernel.willard-sjas-profile`:

```clojure
(def ^:dynamic *sjas-search-stats* nil)

(defn- sjas-search-probeo [kind state-terms]
  (fn [a]
    (when-let [stats *sjas-search-stats*]
      (let [k (hash (logic/-reify a state-terms))]
        (swap! stats update kind
               (fn [m] (let [m (or m {:total 0 :keys #{}})]
                         (-> m (update :total inc) (update :keys conj k)))))))
    a))
```

Insert `(sjas-search-probeo KIND TERMS)` as the first goal of the relations
under test (wrapping their `conde`/`fresh` body):

- `:decode` in `sjas-formal-code-bytes-coreo`, terms `(list term sigma)`;
- `:formula-decode` in `decode-syntax-formula-byteso`, terms `(list bytes)`;
- `:node` in `sjas-proof-check-stateo`, terms `(list fml unexpanded lits neqs sigma)`.

The key is the core.logic reifier (`-reify`, the same canonical-lvar reifier
`l/tabled` uses) hashed — a conservative key that never merges two genuinely
different states, so a high `:total`/`(count :keys)` ratio is sound evidence of
re-derivation.

Harness (`measure_substate.clj`): build a `:willard-sjas-level1` system, bind
`*sjas-search-stats*` to an atom, run the negative
`subst-prf(system-code, system-code, group3-code, sjas-axiom)` (fuel 120) in a
`future` for a 90 s wall window, `future-cancel`, then report per-kind
`:total`/`:distinct`/`:revisit-ratio`. Run with
`lein run -m clojure.main measure_substate.clj`.

## Result (90 s wall window)

| probe | total | distinct | revisit ratio |
|---|---:|---:|---:|
| `:decode` (top-level code read) | 13 | 3 | 4.33× |
| `:formula-decode` (a probed decoder) | 12 | 12 | 1.00× |
| `:node` (tableau checker) | 0 | 0 | — (never fired) |

The probed decoder re-derives nothing (1.00×): 12 distinct formula decodes over
distinct intermediate terms.

> **Correction (2026-06-13, ADR-0106).** An earlier version said these were
> "~7.5 s-each" decodes and that the time was *inside* them. That is wrong:
> ground decode is **~1 ms**, and a direct jstack of the full grind shows the time
> is in `decode_formula_byteso` / `decode_embedded_code_bodyo` + `static_table_entryo`
> *enumeration* over **variable-dense (non-ground)** terms — not the probed
> `decode-syntax-formula-byteso`, and not expensive ground decodes. The 90 s was
> spent during, not inside, the 12 probed decodes. The re-derivation counts and
> the tabling verdict are unaffected.

## Verdict

Tabling helps only re-derivation; the probed hot path has none here. The wall is
a *wide search over distinct intermediate terms whose cost is variable-dense
decode + static-table enumeration* (ADR-0106), so the lever is search-width
reduction (mode-directed ground-before-decode evaluation, a relevance/structural
prefilter, or a non-provability decision), not memoization. See ADR-0105 and
ADR-0106.
