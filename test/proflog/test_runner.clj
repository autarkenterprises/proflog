(ns proflog.test-runner
  "In-process runner for repeated greenfield test probes.

   Leiningen startup dominates short test runs on this repository. This helper
   is intended to be called from a persistent REPL or nREPL session so a single
   JVM can reload and run selected Proflog namespaces while reporting the actual
   per-namespace test time."
  (:require [clojure.string :as str]
            [clojure.test :as test]))

(defn- namespace-symbol
  [value]
  (cond
    (symbol? value) value
    (string? value) (symbol value)
    :else (throw (ex-info "Test runner expects namespace symbols or strings"
                          {:value value}))))

(defn- require-reload!
  [ns-sym]
  (require ns-sym :reload))

(defn- elapsed-ms
  [start-ns]
  (/ (- (System/nanoTime) start-ns) 1000000.0))

(defn run-selected!
  "Reload and run the given test namespaces sequentially in the current JVM.

   Returns a vector of per-namespace result maps:
   {:ns 'proflog.some-test
    :summary {... clojure.test summary ...}
    :elapsed-ms 12.3}"
  [namespaces]
  (mapv (fn [ns-sym]
          (let [start-ns (System/nanoTime)]
            (require-reload! ns-sym)
            {:ns ns-sym
             :summary (test/run-tests ns-sym)
             :elapsed-ms (elapsed-ms start-ns)}))
        (map namespace-symbol namespaces)))

(defn summarize-results
  [results]
  {:test (reduce + (map #(get-in % [:summary :test] 0) results))
   :pass (reduce + (map #(get-in % [:summary :pass] 0) results))
   :fail (reduce + (map #(get-in % [:summary :fail] 0) results))
   :error (reduce + (map #(get-in % [:summary :error] 0) results))
   :elapsed-ms (reduce + (map :elapsed-ms results))})

(defn format-results
  [results]
  (let [format-row (fn [{:keys [ns summary elapsed-ms]}]
                     (format "%-40s %6.1f ms  %2d tests  %2d failures  %2d errors"
                             ns
                             elapsed-ms
                             (get summary :test 0)
                             (get summary :fail 0)
                             (get summary :error 0)))
        total (summarize-results results)]
    (str
      (str/join "\n" (map format-row results))
      "\n"
      (format "TOTAL%35s %6.1f ms  %2d tests  %2d failures  %2d errors"
              ""
              (:elapsed-ms total)
              (:test total)
              (:fail total)
              (:error total)))))

(defn -main
  [& namespaces]
  (let [results (run-selected! namespaces)
        total (summarize-results results)]
    (println (format-results results))
    (flush)
    (shutdown-agents)
    (System/exit (if (zero? (+ (:fail total) (:error total)))
                   0
                   1))))
