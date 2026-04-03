;; ============================================================================
;; αleanTAP-EP Execution Layer
;; ============================================================================
;;
;; Unified execution interface for Proflog.
;;
;; Architecture:
;;   - `cljtap.alphaleantap-ep-fast` handles explicit forward proof search
;;     for fully specified inputs.
;;   - `cljtap.alphaleantap-ep` remains the symbolic/relational engine for
;;     synthesis, reverse use, and partially specified programs or formulas.
;;
;; This namespace chooses between them conservatively.
;;
;; ============================================================================

(ns cljtap.alphaleantap-ep-exec
  (:require [clojure.core.logic :as logic]
            [clojure.core.logic.nominal :as nominal]
            [cljtap.alphaleantap-ep :as ref]
            [cljtap.alphaleantap-ep-fast :as fast]))

(defn- contains-lvar?
  [x]
  (cond
    (logic/lvar? x)
    true

    (nominal/tie? x)
    (contains-lvar? (:body x))

    (map? x)
    (boolean
      (or (some contains-lvar? (keys x))
          (some contains-lvar? (vals x))))

    (vector? x)
    (boolean (some contains-lvar? x))

    (sequential? x)
    (boolean (some contains-lvar? x))

    :else
    false))

(defn fast-compatible?
  "True iff the input is fully specified enough to use the explicit fast engine.
   Any raw core.logic LVar anywhere in the program or formula/query forces the
   symbolic relational engine."
  ([formula]
   (not (contains-lvar? formula)))
  ([program formula]
   (and (fast-compatible? program)
        (fast-compatible? formula))))

(defn execution-mode
  "Select the execution mode for the given input."
  ([formula]
   (if (fast-compatible? formula) :fast :relational))
  ([program formula]
   (if (fast-compatible? program formula) :fast :relational)))

(defn reference-proveo
  "Alias for the relational prover relation.
   Use this directly inside `run` when you need synthesis or reverse inference."
  [& args]
  (apply ref/proveo args))

(defn prove
  "Unified proof interface.
   Dispatches to the fast engine for fully specified inputs and to the
   relational reference engine otherwise."
  ([formula]
   (prove formula 1))
  ([formula n]
   (prove '() formula n))
  ([program formula n]
   (prove program formula n nil))
  ([program formula n gamma-budget]
   (case (execution-mode program formula)
     :fast       (fast/prove-fast program formula n gamma-budget)
     :relational (ref/prove program formula n gamma-budget))))

(defn query-succeeds
  "Unified success query interface."
  ([program query]
   (query-succeeds program query 1))
  ([program query n]
   (query-succeeds program query n nil))
  ([program query n gamma-budget]
   (case (execution-mode program query)
     :fast       (fast/query-succeeds-fast program query n gamma-budget)
     :relational (ref/query-succeeds program query n gamma-budget))))

(defn query-fails
  "Unified failure query interface."
  ([program query]
   (query-fails program query 1))
  ([program query n]
   (query-fails program query n nil))
  ([program query n gamma-budget]
   (case (execution-mode program query)
     :fast       (fast/query-fails-fast program query n gamma-budget)
     :relational (ref/query-fails program query n gamma-budget))))

(defn query-succeeds-id
  "Unified iterative deepening success query interface."
  ([program query]
   (query-succeeds-id program query 64))
  ([program query max-budget]
   (case (execution-mode program query)
     :fast       (fast/query-succeeds-id-fast program query max-budget)
     :relational (ref/query-succeeds-id program query max-budget))))

(defn query-fails-id
  "Unified iterative deepening failure query interface."
  ([program query]
   (query-fails-id program query 64))
  ([program query max-budget]
   (case (execution-mode program query)
     :fast       (fast/query-fails-id-fast program query max-budget)
     :relational (ref/query-fails-id program query max-budget))))
