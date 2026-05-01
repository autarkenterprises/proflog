(ns proflog.core-logic-count-probe
  "Bounded core.logic call-count probe for ADR-0032 carried rows.

   The probe uses `with-redefs-fn` around selected core.logic Vars while running
   one Proflog list-kernel matrix case. It is intentionally process-local
   instrumentation: no production relation or vendored core.logic source is
   changed."
  (:gen-class)
  (:require [clojure.pprint :as pp]
            [proflog.core-logic-host :as host]
            [proflog.list-kernel-matrix-probe :as matrix])
  (:import [java.util.concurrent.atomic LongAdder]))

(def ^:private metric-specs
  [{:id 'clojure.core.logic/unify
    :category :unification}
   {:id 'clojure.core.logic/unify-with-sequential*
    :category :unification}
   {:id 'clojure.core.logic/unify-with-map*
    :category :unification}
   {:id 'clojure.core.logic/ext
    :category :unification}
   {:id 'clojure.core.logic/occurs-check
    :category :unification}

   {:id 'clojure.core.logic/walk*
    :category :walk-reification}
   {:id 'clojure.core.logic/-reify*
    :category :walk-reification}
   {:id 'clojure.core.logic/-reify
    :category :walk-reification}
   {:id 'clojure.core.logic/reifyg
    :category :walk-reification}
   {:id 'clojure.core.logic/force-ans
    :category :walk-reification}

   {:id 'clojure.core.logic/choice
    :category :streams}
   {:id 'clojure.core.logic/to-stream
    :category :streams}

   {:id 'clojure.core.logic/cgoal
    :category :constraints}
   {:id 'clojure.core.logic/run-constraint
    :category :constraints}
   {:id 'clojure.core.logic/run-constraints
    :category :constraints}
   {:id 'clojure.core.logic/run-constraints*
    :category :constraints}
   {:id 'clojure.core.logic/disunify
    :category :constraints}
   {:id 'clojure.core.logic/!=
    :category :constraints}

   {:id 'clojure.core.logic.protocols/reuse
    :category :tabling}
   {:id 'clojure.core.logic.protocols/subunify
    :category :tabling}
   {:id 'clojure.core.logic/master
    :category :tabling}

   {:id 'clojure.core.logic.nominal/hash
    :category :nominal}
   {:id 'clojure.core.logic.nominal/swap-noms
    :category :nominal}
   {:id 'clojure.core.logic.nominal/suspc
    :category :nominal}
   {:id 'clojure.core.logic.nominal/tie
    :category :nominal}])

(def ^:private vector-unify-id
  'clojure.core.logic/unify-with-vector*)

(def ^:private vector-unify-counter-id
  'clojure.core.logic/*proflog-adr32-vector-unify-counter*)

(defn- require-namespace
  [sym]
  (require (symbol (namespace sym))))

(defn- resolve-var
  [sym]
  (require-namespace sym)
  (ns-resolve (symbol (namespace sym)) (symbol (name sym))))

(defn- nanos->millis
  [nanos]
  (/ (double nanos) 1000000.0))

(defn- round3
  [x]
  (/ (Math/round (* 1000.0 (double x))) 1000.0))

(defn- update-metric
  [metric category]
  (or metric {:category category
              :calls (LongAdder.)}))

(defn- metered-fn
  [metrics {:keys [id category]} f]
  (let [counter (get-in (swap! metrics update id update-metric category)
                        [id :calls])]
    (fn [& args]
      (.increment ^LongAdder counter)
      (apply f args))))

(defn- metric-redefs
  [metrics specs]
  (reduce (fn [redefs spec]
            (if-let [v (resolve-var (:id spec))]
              (assoc redefs v (metered-fn metrics spec @v))
              redefs))
          {}
          specs))

(defn- counter-binding-var
  []
  (ns-resolve 'clojure.core.logic '*proflog-adr32-vector-unify-counter*))

(defn- calls-adder
  [calls]
  (doto (LongAdder.)
    (.add calls)))

(defn- unresolved-vars
  [specs]
  (->> specs
       (keep (fn [{:keys [id]}]
               (when-not (resolve-var id)
                 id)))
       vec))

(defn- summarize-metric
  [id {:keys [category calls]}]
  {:id id
   :category category
   :calls (.sum ^LongAdder calls)})

(defn- summarize-category
  [[category metrics]]
  (let [calls (reduce + (map #(.sum ^LongAdder (:calls %)) metrics))]
    {:category category
     :calls calls}))

(defn- add-call-share
  [total-calls category]
  (assoc category
         :call-share
         (if (pos? total-calls)
           (round3 (/ (:calls category) total-calls))
           0.0)))

(defn- category-totals
  [raw-metrics]
  (let [totals (->> raw-metrics
                    vals
                    (group-by :category)
                    (map summarize-category)
                    (sort-by (juxt (comp - :calls) :category))
                    vec)
        total-calls (reduce + (map :calls totals))]
    (mapv (partial add-call-share total-calls) totals)))

(defn- top-metrics
  [raw-metrics metric-limit]
  (->> raw-metrics
       (map (fn [[id metric]] (summarize-metric id metric)))
       (sort-by (juxt (comp - :calls) :id))
       (take metric-limit)
       vec))

(defn- counted-case-result
  [case-id metric-limit]
  (let [metrics (atom {})
        specs metric-specs
        redefs (metric-redefs metrics specs)
        vector-counter-var (counter-binding-var)
        vector-calls (atom 0)
        started (System/nanoTime)
        case-result (with-redefs-fn redefs
                      #(if vector-counter-var
                         (with-bindings {vector-counter-var vector-calls}
                           (matrix/run-case (keyword case-id)))
                         (matrix/run-case (keyword case-id))))
        elapsed-ns (- (System/nanoTime) started)
        raw-metrics (cond-> @metrics
                      vector-counter-var
                      (assoc vector-unify-id
                             {:category :unification
                              :calls (calls-adder @vector-calls)}))
        categories (category-totals raw-metrics)]
    {:probe :core-logic-count
     :case-id (keyword case-id)
     :host (select-keys (host/host-info)
                        [:source :source-kind :group-id :artifact-id :version :marker])
     :instrumented-var-count (count redefs)
     :unresolved-vars (cond-> (unresolved-vars specs)
                        (nil? vector-counter-var)
                        (conj vector-unify-counter-id))
     :elapsed-ms (round3 (nanos->millis elapsed-ns))
     :dominant-category-by-calls (some-> categories first :category)
     :category-totals categories
     :top-metrics-by-calls (top-metrics raw-metrics metric-limit)
     :case-result case-result}))

(defn run-counted-case
  "Run one list-kernel matrix case with process-local core.logic counters."
  ([case-id]
   (run-counted-case case-id {}))
  ([case-id {:keys [metric-limit]
             :or {metric-limit 14}}]
   (counted-case-result case-id metric-limit)))

(defn- parse-positive-int
  [text fallback]
  (try
    (let [value (Long/parseLong text)]
      (if (pos? value) value fallback))
    (catch RuntimeException _
      fallback)))

(defn -main
  [& [case-id metric-limit-text]]
  (let [case-id (or case-id "reverse-input-flat")
        metric-limit (parse-positive-int metric-limit-text 14)]
    (pp/pprint (run-counted-case case-id {:metric-limit metric-limit})))
  (shutdown-agents))
