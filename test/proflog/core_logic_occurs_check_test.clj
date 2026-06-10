(ns proflog.core-logic-occurs-check-test
  (:require [clojure.core.logic :as logic]
            [clojure.test :refer [deftest is testing]]))

(def deep-acyclic-depth
  "A depth high enough to expose host-stack recursion in the upstream
  first-child occurs-check path while still keeping the post-fix test cheap."
  20000)

(defn- deep-acyclic-ground-term
  "Build a unary nested vector without recursion so the test measures
  core.logic's occurs-check traversal rather than this helper."
  [depth]
  (loop [remaining depth
         term :leaf]
    (if (zero? remaining)
      term
      (recur (dec remaining) [term]))))

(deftest occurs-check-is-stack-safe-for-deep-acyclic-ground-terms
  (testing "large acyclic ground terms can be bound through the normal occurs check"
    (let [term (deep-acyclic-ground-term deep-acyclic-depth)]
      (is (= '(:ok)
             (logic/run 1 [q]
               (logic/fresh [x]
                 (logic/== x term)
                 (logic/== q :ok)))))))
  (testing "the occurs check still rejects direct self-reference"
    (is (= '()
           (logic/run 1 [q]
             (logic/fresh [x]
               (logic/== x [x])
               (logic/== q :cycle)))))))
