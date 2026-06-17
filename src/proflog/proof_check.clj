(ns proflog.proof-check
  "Independent, non-relational structural checker for greenfield proof terms.

   This is the third oracle in the proof-checking *quorum* (ADR-0117):

   1. the kernel as prover (`proflog.kernel/prove*`) generates a proof,
   2. the kernel as checker (the same relation, run with the `proof` argument
      *bound*) re-accepts it, and
   3. THIS namespace re-validates it independently.

   It shares no code with the kernel and uses no core.logic. It re-checks that a
   proof term is a well-formed Proflog tableau-derivation tree against the
   proof-tag grammar verified directly from `proflog.kernel` and
   `proflog.equality`.

   It is deliberately a STRUCTURAL checker, not a semantic re-checker. Greenfield
   proof terms are *pure tag trees*: every node is `(tag subproof*)` (or the empty
   terminal `()`), and a tag wraps only subproofs — never a formula, term,
   witness, or unifier. So an independent oracle cannot re-derive WHICH formula
   each node proves without re-running the search (this is the documented
   proof-term-adequacy limitation, ADR-0117). What it CAN do — and what the quorum
   needs — is reject any tree that is not a well-formed derivation: an unknown
   rule tag, a wrong subproof arity, or a non-proof node. A silent kernel bug that
   emitted a malformed certificate would have to be replicated identically here to
   escape the quorum."
  (:require [clojure.set :as set]))

(def rule-arities
  "Each greenfield proof tag mapped to the set of subproof counts it may wrap.
   Verified against src/proflog/kernel.clj and src/proflog/equality.clj.
   `neq-close` is overloaded: arity 0 when a saved disequality is violated by a
   new equality (equality/neq-violatedo), arity 1 when the disequality is closed
   by forcing equality (kernel negative-equality rule)."
  {;; --- structural tableau rules ---
   'conj                       #{1}   ; alpha / conjunction
   'split                      #{2}   ; beta / disjunction
   'univ                       #{1}   ; gamma / universal
   'once-univ                  #{1}   ; single-use universal (negated existential body)
   'witness                    #{1}   ; delta / existential
   'eq-step                    #{2}   ; positive equality that made progress
   'neq-close                  #{0 1} ; disequality closed (violated=0 / forced=1)
   'neq-rigid                  #{1}   ; disequality discharged by rigid clash
   'neq-store                  #{1}   ; disequality stored, search continues
   'savefml                    #{1}   ; atom saved, search continues
   'pos-call                   #{1}   ; positive procedure call
   'neg-call                   #{1}   ; negative procedure call (single clause)
   'neg-call-alt               #{1}   ; negative call via alternatives
   'neg-call-guarded-alt       #{1}   ; negative call via guarded alternatives
   ;; --- closure leaves ---
   'close                      #{0}   ; complementary-literal closure
   'refl-close                 #{0}   ; disequality reflexive closure
   'occurs-close               #{0}   ; occurs-check contradiction (proof variable)
   'free-close                 #{0}   ; free-constructor clash
   ;; --- equality-engine internal tags ---
   'eq-refl                    #{0}
   'eq-bind                    #{0}
   'par-bind                   #{0}
   'decompose                  #{1}
   'args                       #{2}
   'atom-close                 #{1}
   ;; --- equality-triggered saved calls ---
   'eq-triggered-call          #{1}
   'eq-triggered-neg-call      #{1}
   ;; --- alternative descent ---
   'alt                        #{1}
   ;; --- guarded-alternative machinery ---
   'guarded-alt                #{1}
   'guarded-scope-done         #{0}
   'guarded-scope-exists       #{1}
   'guarded-seq-done           #{0}
   'guarded-seq-last           #{1}
   'guarded-seq-step           #{2}
   'guard-saturation-done      #{0}
   'guard-eq                   #{2}
   'guarded-neg-alt            #{2}
   'guarded-neg-alt-saturated  #{4}
   'guarded-call-seq-done      #{0}
   'guarded-call-seq-step      #{2}})

(def known-tags (set (keys rule-arities)))

(defn check
  "Return true iff `proof` is a well-formed greenfield tableau-derivation tree.

   A node is valid when it is the empty terminal `()`, or a list `(tag subproof*)`
   whose `tag` is a known rule tag and whose number of subproofs is allowed for
   that tag, recursively. Strict: an unknown tag, a wrong arity, or a non-list
   node fails."
  [proof]
  (cond
    ;; The empty terminal () (nullary argument-list unification).
    (and (sequential? proof) (empty? proof)) true
    (sequential? proof)
    (let [tag (first proof)
          subs (rest proof)
          allowed (get rule-arities tag)]
      (boolean
        (and (symbol? tag)
             allowed
             (contains? allowed (count subs))
             (every? check subs))))
    :else false))

(defn all-tags
  "The set of every tag symbol occurring anywhere in `proof`."
  [proof]
  (if (and (sequential? proof) (seq proof))
    (into #{(first proof)} (mapcat all-tags (rest proof)))
    #{}))

(defn unrecognized-tags
  "The set of tags in `proof` that are not part of the known grammar.
   Empty for any genuine greenfield proof; non-empty flags either a malformed
   certificate or a grammar that has fallen behind the kernel."
  [proof]
  (set/difference (all-tags proof) known-tags))

(defn explain
  "Diagnostic map for `proof`: validity, and any unrecognized tags."
  [proof]
  {:ok? (check proof)
   :unrecognized-tags (unrecognized-tags proof)})
