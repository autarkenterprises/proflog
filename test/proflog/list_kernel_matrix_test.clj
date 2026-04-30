(ns proflog.list-kernel-matrix-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.list-kernel-matrix-probe :as matrix]))

(deftest list-kernel-matrix-covers-forward-reverse-and-partial-modes
  (testing "the raw-kernel matrix spans append/reverse, flat/nested, and synthesis modes"
    (let [catalog (matrix/case-catalog)
          by-op (group-by :operation catalog)
          modes (set (map :mode catalog))
          shapes (set (map :shape catalog))]
      (is (seq (:append by-op)))
      (is (seq (:reverse by-op)))
      (is (contains? modes :forward))
      (is (contains? modes :output-synthesis))
      (is (contains? modes :input-synthesis))
      (is (contains? modes :partial-suffix))
      (is (contains? modes :partial-prefix))
      (is (contains? modes :inverse-splits))
      (is (contains? modes :partial-output))
      (is (contains? shapes :flat))
      (is (contains? shapes :nested))
      (is (some #(= :longer (:size %)) catalog)))))
