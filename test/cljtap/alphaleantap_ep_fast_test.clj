;; ============================================================================
;; αleanTAP-EP Fast Test Suite
;; ============================================================================

(ns cljtap.alphaleantap-ep-fast-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as logic]
            [clojure.core.logic.nominal :as nominal]
            [cljtap.alphaleantap-ep :as ref]
            [cljtap.alphaleantap-ep-fast :as fast]
            [cljtap.alphaleantap-ep-test :as fixtures]))

(defn- fresh-nom
  [label]
  (nominal/nom (logic/lvar label)))

(defn- provable-ref?
  ([formula]
   (provable-ref? '() formula 1 nil))
  ([program formula n gamma-budget]
   (boolean
     (seq
       (ref/prove program formula n gamma-budget)))))

(defn- provable-fast?
  ([formula]
   (provable-fast? '() formula 1 nil))
  ([program formula n gamma-budget]
   (boolean
     (seq
       (fast/prove-fast program formula n gamma-budget)))))

(deftest test-F01-base-contradiction
  (testing "Fast engine agrees with reference on direct complementary closure."
    (let [formula '(and (pos (app p)) (neg (app p)))]
      (is (= (provable-ref? formula)
             (provable-fast? formula))))))

(deftest test-F02-gamma-budget
  (testing "Fast engine agrees with reference on bounded gamma instantiation."
    (let [x       (fresh-nom 'x)
          formula ['forall (nominal/tie x ['neq ['var x] ['var x]])]]
      (is (= (provable-ref? '() formula 1 1)
             (provable-fast? '() formula 1 1))))))

(deftest test-F03-occurs-check-blocked
  (testing "Fast engine preserves the blocked existential occurs-check case."
    (let [x       (fresh-nom 'x)
          formula ['exists (nominal/tie x ['eq ['var x] ['app 'f ['var x]]])]]
      (is (= (provable-ref? '() formula 1 nil)
             (provable-fast? '() formula 1 nil))))))

(deftest test-F04-positive-procedure-call
  (testing "Fast engine agrees with the reference on a positive procedure call."
    (let [program [['r [] ['and ['pos ['app 'p]]
                               ['neg ['app 'p]]]]]
          formula ['pos ['app 'r]]]
      (is (= (provable-ref? program formula 1 nil)
             (provable-fast? program formula 1 nil))))))

(deftest test-F05-negative-procedure-call
  (testing "Fast engine agrees with the reference on a negative procedure call."
    (let [program [['r [] ['or ['pos ['app 'p]]
                              ['neg ['app 'p]]]]]
          formula ['neg ['app 'r]]]
      (is (= (provable-ref? program formula 1 nil)
             (provable-fast? program formula 1 nil))))))

(deftest test-F06-transitive-equality-closure
  (testing "Fast engine agrees with the reference on equality-driven disequality closure."
    (let [formula '(and (eq (app a) (app b))
                        (and (eq (app b) (app c))
                             (neq (app a) (app c))))]
      (is (= (provable-ref? formula)
             (provable-fast? formula))))))

(deftest test-F07-gv04-z2-assoc
  (testing "Fast engine proves the representative precomputed GV case."
    (let [x       (fresh-nom 'x)
          y       (fresh-nom 'y)
          z       (fresh-nom 'z)
          program (fixtures/gv-assoc-precomputed-program fixtures/gv-z2 x y z)
          formula ['neg ['app 'gv_assoc_pre]]]
      (is (provable-fast? program formula 1 nil)))))
