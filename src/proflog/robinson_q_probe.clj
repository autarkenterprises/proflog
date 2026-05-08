(ns proflog.robinson-q-probe
  "Timing probe for ADR-0048 Robinson Q proof paths.

   This is a reproducible documentation aid rather than a regression gate. The
   committed tests assert correctness; this namespace records wall-clock timing
   for the shared ordinary-vs-profiled examples."
  (:require [proflog.query :as query]
            [proflog.robinson-q :as rq]))

(def common-theorems
  "The formulas used to compare Q-as-antecedent and profiled conversion."
  [[:q7 rq/q7 32 16]
   [:add-one-zero (rq/eq (rq/add (rq/numeral 1) rq/zero)
                         (rq/numeral 1))
    48 16]
   [:mul-two-zero (rq/eq (rq/mul (rq/numeral 2) rq/zero)
                         rq/zero)
    48 16]
   [:add-one-two (rq/eq (rq/add (rq/numeral 1) (rq/numeral 2))
                        (rq/numeral 3))
    64 16]
   [:mul-two-two (rq/eq (rq/mul (rq/numeral 2) (rq/numeral 2))
                        (rq/numeral 4))
    96 16]])

(defn- elapsed-ms
  "Return `[value elapsed-ms]` for one thunk."
  [f]
  (let [start (System/nanoTime)
        value (f)]
    [value (/ (double (- (System/nanoTime) start)) 1000000.0)]))

(defn- ordinary-row
  [[label theorem ordinary-fuel _profile-fuel]]
  (let [[proofs ms] (elapsed-ms
                      #(query/query-succeeds
                         rq/ordinary-program
                         (rq/q-implies theorem)
                         1
                         ordinary-fuel))]
    {:path :ordinary-q-antecedent
     :label label
     :fuel ordinary-fuel
     :proofs (count proofs)
     :elapsed-ms ms}))

(defn- profile-row
  [[label theorem _ordinary-fuel profile-fuel]]
  (let [[proofs ms] (elapsed-ms
                      #(query/query-succeeds
                         rq/profile-program
                         theorem
                         1
                         profile-fuel))]
    {:path :robinson-q-profile
     :label label
     :fuel profile-fuel
     :proofs (count proofs)
     :elapsed-ms ms}))

(defn run-probe
  "Return timing rows for the ADR-0048 comparison."
  []
  (concat
    (map ordinary-row common-theorems)
    (map profile-row common-theorems)))

(defn -main
  [& _args]
  (doseq [row (run-probe)]
    (prn row)))
