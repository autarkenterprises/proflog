;; Warm-JVM benchmark for one GV associativity case at a time.
;;
;; Usage:
;;   lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj <case>
;;
;; Suggested commands:
;;   lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj z1-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj pre-z2-neg
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj non-group-pos
;;   timeout 10s lein trampoline run -m clojure.main scripts/gv_assoc_bench.clj full-z2-neg

(require '[cljtap.alphaleantap-ep :refer :all])
(require '[clojure.core.logic :as l])

(load-file "test/cljtap/alphaleantap_ep_test.clj")

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
          (let [prog (cljtap.alphaleantap-ep-test/gv-assoc-program
                       cljtap.alphaleantap-ep-test/gv-z1
                       x y z w1 w2 w3 w4)]
            (proveo ['neg ['app 'gv_assoc]]
                    '() '() '() prog proof)))))))

(defn pre-z2-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z]
          (let [prog (cljtap.alphaleantap-ep-test/gv-assoc-precomputed-program
                       cljtap.alphaleantap-ep-test/gv-z2
                       x y z)]
            (proveo ['neg ['app 'gv_assoc_pre]]
                    '() '() '() prog proof)))))))

(defn non-group-pos []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z w1 w2 w3 w4]
          (let [prog (cljtap.alphaleantap-ep-test/gv-assoc-program
                       cljtap.alphaleantap-ep-test/gv-non-group
                       x y z w1 w2 w3 w4)]
            (proveo ['pos ['app 'gv_assoc]]
                    '() '() '() prog proof)))))))

(defn full-z2-neg []
  (boolean
    (seq
      (l/run 1 [proof]
        (clojure.core.logic.nominal/fresh [x y z w1 w2 w3 w4]
          (let [prog (cljtap.alphaleantap-ep-test/gv-assoc-program
                       cljtap.alphaleantap-ep-test/gv-z2
                       x y z w1 w2 w3 w4)]
            (proveo ['neg ['app 'gv_assoc]]
                    '() '() '() prog proof)))))))

(let [case-name (first *command-line-args*)]
  (case case-name
    "z1-neg"        (timed-ms "z1-neg" z1-neg)
    "pre-z2-neg"    (timed-ms "pre-z2-neg" pre-z2-neg)
    "non-group-pos" (timed-ms "non-group-pos" non-group-pos)
    "full-z2-neg"   (timed-ms "full-z2-neg" full-z2-neg)
    (do
      (println "Usage: ... scripts/gv_assoc_bench.clj <case>")
      (println "Cases: z1-neg pre-z2-neg non-group-pos full-z2-neg")
      (flush)
      (System/exit 1))))

(System/exit 0)
