(ns proflog.sjas-runtime-probe
  "Bisect the whole-program SJAS query cost (ADR-0088).

   Each invocation mirrors one sub-query of the grinding regression
   `sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile`
   so the expensive combination can be attributed to a profile and query
   shape in isolation, one JVM per case. The system construction mirrors the
   test fixture exactly: demo beta `1 = 1`, one reflected demo clause, one
   external demo clause.

   Usage:

     lein run -m proflog.sjas-runtime-probe <profile> <case>

   where <profile> is `willard-sjas-tableau0` or `willard-sjas-level1` and
   <case> is `beta` (prove `1 = 1` through the compiled program, fuel 64) or
   `axiom-member` (cite the Group-3 code from the system code, fuel 64)."
  (:require [proflog.ast :as ast]
            [proflog.query :as query]
            [proflog.willard-sjas :as sjas]))

(defn- demo-system
  [profile]
  (sjas/system
    {:profile profile
     :relations {'demo 1
                 'external-demo 1}
     :beta [(ast/eq-lit sjas/one sjas/one)]
     :reflected-clauses [(ast/nom x
                           (ast/clause 'demo [x]
                                       (ast/eq-lit (ast/var-term x) sjas/one)))]
     :external-clauses [(ast/nom x
                          (ast/clause 'external-demo [x]
                                      (ast/eq-lit (ast/var-term x) sjas/zero)))]}))

(defn- run-case
  [profile query-kind]
  (let [build-start (System/nanoTime)
        system (demo-system profile)
        build-ms (/ (- (System/nanoTime) build-start) 1000000.0)
        goal (case query-kind
               :beta (ast/eq-lit sjas/one sjas/one)
               :axiom-member (sjas/axiom-member (:system-code system)
                                                (:code (:group-three system))))
        _ (println (format ":BUILD %s elapsed-ms=%.1f" (name profile) build-ms))
        _ (flush)
        query-start (System/nanoTime)
        result (query/query-succeeds (:program system) goal 1 64)
        query-ms (/ (- (System/nanoTime) query-start) 1000000.0)]
    (println (format ":CASE %s %s proofs=%d elapsed-ms=%.1f"
                     (name profile)
                     (name query-kind)
                     (count result)
                     query-ms))
    (flush)))

(defn -main
  [& [profile-arg kind-arg]]
  (run-case (keyword (or profile-arg "willard-sjas-tableau0"))
            (keyword (or kind-arg "beta")))
  (shutdown-agents)
  (System/exit 0))
