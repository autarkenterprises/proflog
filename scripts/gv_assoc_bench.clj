;; Warm-JVM benchmark for one GV associativity case at a time.
;;
;; Usage:
;;   lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj <case>
;;
;; Suggested commands:
;;   lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj z1-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj pre-z2-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj full-non-group-pos
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj full-z2-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj chained-z2-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj chained-non-group-pos
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj pre-z4-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj pre-non-group-4-pos
;;
;; These larger cases correspond to the manual GV associativity stress checks
;; that are too expensive to keep in the default `lein test` path on
;; perf-lab-review, but are still useful to rerun against a warm JVM.

(require '[cljtap.alphaleantap-ep :refer :all])
(require '[clojure.core.logic :as l])
(require '[cljtap.gv-assoc :as gv])

(defn timed-ms [label thunk]
  (let [start  (System/nanoTime)
        result (thunk)
        ms     (/ (- (System/nanoTime) start) 1e6)]
    (printf "%-18s %10.3f ms  %s\n" label ms result)
    (flush)
    result))

(defn z1-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z w1 w2 w3 w4]
          (let [prog (gv/gv-assoc-program
                       gv/gv-z1
                       x y z w1 w2 w3 w4)]
            (proveo ['neg ['app 'gv_assoc]]
                    '() '() '() prog proof)))))))

(defn pre-z2-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z]
          (let [prog (gv/gv-assoc-precomputed-program
                       gv/gv-z2
                       x y z)]
            (proveo ['neg ['app 'gv_assoc_pre]]
                    '() '() '() prog proof)))))))

(defn full-non-group-pos []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z w1 w2 w3 w4]
          (let [prog (gv/gv-assoc-program
                       gv/gv-non-group
                       x y z w1 w2 w3 w4)]
            (proveo ['pos ['app 'gv_assoc]]
                    '() '() '() prog proof)))))))

(defn full-z2-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z w1 w2 w3 w4]
          (let [prog (gv/gv-assoc-program
                       gv/gv-z2
                       x y z w1 w2 w3 w4)]
            (proveo ['neg ['app 'gv_assoc]]
                    '() '() '() prog proof)))))))

(defn chained-z2-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z]
          (let [prog (gv/gv-assoc-chained-program
                       gv/gv-z2
                       x y z)]
            (proveo ['neg ['app 'gv_assoc_ch]]
                    '() '() '() prog proof)))))))

(defn chained-non-group-pos []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z]
          (let [prog (gv/gv-assoc-chained-program
                       gv/gv-non-group
                       x y z)]
            (proveo ['pos ['app 'gv_assoc_ch]]
                    '() '() '() prog proof 8)))))))

(defn pre-z4-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z]
          (let [prog (gv/gv-assoc-precomputed-program
                       gv/gv-z4
                       x y z)]
            (proveo ['neg ['app 'gv_assoc_pre]]
                    '() '() '() prog proof)))))))

(defn pre-non-group-4-pos []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z]
          (let [prog (gv/gv-assoc-precomputed-program
                       gv/gv-non-group-4
                       x y z)]
            (proveo ['pos ['app 'gv_assoc_pre]]
                    '() '() '() prog proof)))))))

(let [case-name (first *command-line-args*)]
  (case case-name
    "z1-neg"             (timed-ms "z1-neg" z1-neg)
    "pre-z2-neg"         (timed-ms "pre-z2-neg" pre-z2-neg)
    "full-non-group-pos" (timed-ms "full-non-group-pos" full-non-group-pos)
    "non-group-pos"      (timed-ms "non-group-pos" full-non-group-pos)
    "full-z2-neg"        (timed-ms "full-z2-neg" full-z2-neg)
    "chained-z2-neg"     (timed-ms "chained-z2-neg" chained-z2-neg)
    "chained-non-group-pos"
    (timed-ms "chained-non-group-pos" chained-non-group-pos)
    "pre-z4-neg"         (timed-ms "pre-z4-neg" pre-z4-neg)
    "pre-non-group-4-pos"
    (timed-ms "pre-non-group-4-pos" pre-non-group-4-pos)
    (do
      (println "Usage: ... scripts/gv_assoc_bench.clj <case>")
      (println "Cases: z1-neg pre-z2-neg full-non-group-pos full-z2-neg")
      (println "       chained-z2-neg chained-non-group-pos")
      (println "       pre-z4-neg pre-non-group-4-pos")
      (flush)
      (System/exit 1))))

(System/exit 0)
