(ns cljtap.run-section
  "Section-based test runner with timing.

   REPL usage:
     (require '[cljtap.run-section :as rs])
     (rs/run-section \"PA\")        ; run one section with timing
     (rs/run-all-sections)          ; run all, print timing table
     (rs/save-times!)               ; run all, save timing to EDN

   Command line:
     lein test-section PA           ; run one section
     lein test-all-timed            ; run all, save timing"
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

;; Load the test namespace so its vars are available.
(require 'cljtap.alphaleantap-ep-test)

(def section-codes
  "All section codes in presentation order."
  ["A" "B" "Bp" "Bpp" "Bc" "Bd" "C" "D" "Dp"
   "E" "F" "G" "H" "I" "J" "K" "L" "M" "N" "O" "P"
   "Q" "R" "S" "T" "U" "V" "W" "X" "Y" "Z"
   "DI" "ADV" "SUB" "RV" "MV" "OC" "TC" "PA" "SO" "SS"
   "GP" "GV" "FD"])

(defn section-vars
  "Return test vars for a given section code, sorted by name."
  [section]
  (let [pat (re-pattern (str "^test-" section "\\d"))]
    (->> (ns-publics 'cljtap.alphaleantap-ep-test)
         vals
         (filter (fn [v] (and (:test (meta v))
                              (re-find pat (name (:name (meta v)))))))
         (sort-by (comp name :name meta)))))

(defn run-section
  "Run all tests in a section. Returns map with :pass :fail :error :time-ms :count."
  [section]
  (let [vars (section-vars section)]
    (if (empty? vars)
      (do (println (str "  [" section "] no tests found"))
          {:pass 0 :fail 0 :error 0 :time-ms 0 :count 0})
      (let [counter (atom {:pass 0 :fail 0 :error 0})
            start   (System/nanoTime)]
        ;; Bind report to capture pass/fail/error counts
        (binding [t/report (fn [m]
                             (case (:type m)
                               :pass  (swap! counter update :pass inc)
                               :fail  (do (swap! counter update :fail inc)
                                          (t/with-test-out
                                            (println (str "  FAIL: " (last t/*testing-vars*)))
                                            (when (:message m) (println (str "    " (:message m))))
                                            (println (str "    expected: " (pr-str (:expected m))))
                                            (println (str "    actual:   " (pr-str (:actual m))))))
                               :error (do (swap! counter update :error inc)
                                          (t/with-test-out
                                            (println (str "  ERROR: " (last t/*testing-vars*)))
                                            (when (:message m) (println (str "    " (:message m))))
                                            (println (str "    " (:actual m)))))
                               ;; :begin-test-var, :end-test-var, etc — ignore
                               nil))]
          (t/test-vars vars))
        (let [elapsed (/ (- (System/nanoTime) start) 1e6)
              result  (assoc @counter :time-ms elapsed :count (count vars))
              status  (if (pos? (+ (:fail result) (:error result))) "FAIL" "OK")]
          (printf "  [%-4s] %3d tests  %8.0f ms  %s\n" section (:count result) elapsed status)
          (flush)
          result)))))

(defn run-all-sections
  "Run all sections in order. Returns ordered map of section -> result."
  []
  (println "Running all sections...\n")
  (let [results (into (array-map)
                      (for [sec section-codes]
                        [sec (run-section sec)]))]
    (println "\n========================================")
    (println " Section Timing Summary")
    (println "========================================")
    (doseq [[sec r] results
            :when (pos? (:count r 0))]
      (printf "  %-6s  %3d tests  %8.0f ms  %s\n"
              sec (:count r 0) (:time-ms r 0)
              (if (pos? (+ (:fail r 0) (:error r 0))) "FAIL" "OK")))
    (let [total-tests (reduce + (map #(:count % 0) (vals results)))
          total-ms    (reduce + (map #(:time-ms % 0) (vals results)))
          total-fail  (reduce + (map #(+ (:fail % 0) (:error % 0)) (vals results)))]
      (println "----------------------------------------")
      (printf "  TOTAL   %3d tests  %8.0f ms  %s\n"
              total-tests total-ms
              (if (pos? total-fail) "FAIL" "OK")))
    (println "========================================")
    results))

(def times-file "test/section-times.edn")

(defn save-times!
  "Run all sections and save timing data to EDN file."
  []
  (let [results (run-all-sections)
        data    (into (array-map)
                      (for [[sec r] results
                            :when (pos? (:count r 0))]
                        [sec {:count   (:count r 0)
                              :time-ms (Math/round ^double (:time-ms r 0))
                              :status  (if (pos? (+ (:fail r 0) (:error r 0)))
                                         :fail :pass)}]))]
    (spit times-file (with-out-str (pp/pprint data)))
    (println (str "\nTimes saved to " times-file))))

(defn load-times
  "Load previously saved section times from EDN file."
  []
  (when (.exists (io/file times-file))
    (read-string (slurp times-file))))

(defn -main
  "Entry point for lein run -m cljtap.run-section <SECTION|--all>."
  [& args]
  (cond
    (empty? args)
    (do (println "Usage: lein test-section <SECTION|--all>")
        (println)
        (println "Sections:" (str/join " " section-codes))
        (when-let [times (load-times)]
          (println)
          (println "Last recorded times:")
          (doseq [[sec t] times]
            (printf "  %-6s  %3d tests  %6d ms  %s\n"
                    sec (:count t) (:time-ms t) (name (:status t)))))
        (System/exit 0))

    (= (first args) "--all")
    (do (save-times!)
        (System/exit 0))

    :else
    (let [sec (first args)]
      (if (some #{sec} section-codes)
        (do (run-section sec)
            (System/exit 0))
        (do (println (str "Unknown section: " sec))
            (println "Available:" (str/join " " section-codes))
            (System/exit 1))))))
