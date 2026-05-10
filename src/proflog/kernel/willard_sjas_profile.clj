(ns proflog.kernel.willard-sjas-profile
  "Proof-profile entrypoint for ADR-0060's MVP Willard SJAS language.

   The profile does not validate tableau certificates in Clojure. The generated
   SJAS program carries certificate predicates as ordinary Proflog relations.
   This namespace only selects the ordinary program-aware kernel and wraps proof
   evidence so callers can see which SJAS profile was selected."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== fail run]]
            [proflog.kernel :as kernel]))

(defn willard-sjas-theory-closeo
  "Reserved kernel-interleaved hook for later SJAS-specific theory steps.

   The MVP keeps arithmetic graph checking, formula-code classification, axiom
   membership, and miniature certificate validation in generated Proflog
   clauses. Leaving this hook present but failing makes that boundary explicit
   and keeps the profile shape aligned with the Robinson-Q profile architecture."
  [_fml _unexpanded _lits _env _proof-vars sigma sigma-out neqs neqs-out
   _prog _gamma-terms _fuel proof]
  (== sigma sigma-out)
  (== neqs neqs-out)
  (== proof '(willard-sjas-no-theory-step))
  fail)

(defn- profile-symbol
  "Convert a profile keyword into the symbol used in proof evidence."
  [profile]
  (symbol (name profile)))

(defn- wrap-proof
  "Attach an explicit SJAS profile marker to an ordinary kernel proof term."
  [profile proof]
  (list 'profiled (profile-symbol profile) proof))

(defn prove-program
  "Prove `formula` with the ordinary program kernel under the selected profile.

   Source translation, generated coding facts, and finite axiom assembly happen
   before this function is called. The proof search here stays on the
   program-aware relational kernel; generated predicates such as
   `axiom-member/2` and `tableau-proof/3` are opened by the Procedure Call Rule."
  [profile program formula proof-limit fuel]
  (let [proofs (binding [kernel/*theory-profile-closeo* willard-sjas-theory-closeo]
                 (doall
                   (if (nil? fuel)
                     (run proof-limit [proof]
                       (kernel/prove-programo formula '() '() '() program proof))
                     (run proof-limit [proof]
                       (kernel/prove-programo formula '() '() '() program fuel proof)))))]
    (map #(wrap-proof profile %) proofs)))
