(ns proflog.sjas-synthesis-guard-test
  "ADR-0142 criterion 8: the dataflow-independence guard distinguishes a
   genuinely fresh synthesis tuple from an ADR-0141-style host-seeded one."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as l]
            [proflog.sjas-synthesis-guard :as guard]))

(deftest dataflow-guard-distinguishes-fresh-from-host-seeded
  (testing "independence is a dataflow property: fresh lvars pass, host-ground data fails"
    (let [fresh-tuple [(l/lvar 'x) (l/lvar 'y) (l/lvar 'p) (l/lvar 'q)]
          ;; the ADR-0141 failure mode: p and q are host-encoded bytes/values,
          ;; only x and y are fresh -- fresh NAMES hiding a host-selected answer.
          seeded-tuple [(l/lvar 'x) (l/lvar 'y) [0 1 1 0 1] 42]]
      (is (guard/dataflow-independent? fresh-tuple)
          "an all-fresh tuple is dataflow-independent")
      (is (not (guard/dataflow-independent? seeded-tuple))
          "a tuple with host-ground components is NOT independent")
      (is (= fresh-tuple (guard/assert-dataflow-independent! fresh-tuple))
          "asserting independence returns a fresh tuple unchanged")
      (is (thrown? clojure.lang.ExceptionInfo
                   (guard/assert-dataflow-independent! seeded-tuple))
          "asserting independence throws on a host-seeded tuple"))))

(deftest dataflow-guard-rejects-empty-tuple
  (testing "an empty tuple is not a meaningful independent synthesis target"
    (is (not (guard/dataflow-independent? [])))))
