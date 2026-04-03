;; ============================================================================
;; αleanTAP-EP Execution Layer Tests
;; ============================================================================

(ns cljtap.alphaleantap-ep-exec-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.logic :as logic]
            [clojure.core.logic.nominal :as nominal]
            [cljtap.alphaleantap-ep-exec :as exec]
            [cljtap.alphaleantap-ep-fast :as fast]
            [cljtap.alphaleantap-ep :as ref]))

(defn- fresh-nom
  [label]
  (nominal/nom (logic/lvar label)))

(deftest test-XE01-ground-input-uses-fast-mode
  (testing "Fully specified inputs select the fast execution path."
    (is (= :fast
           (exec/execution-mode '()
                                '(and (pos (app p))
                                      (neg (app p))))))))

(deftest test-XE02-lvar-in-formula-uses-relational-mode
  (testing "A raw logic variable in the formula forces relational mode."
    (let [x (logic/lvar 'x)]
      (is (= :relational
             (exec/execution-mode '()
                                  ['neq x x]))))))

(deftest test-XE03-lvar-in-program-uses-relational-mode
  (testing "A partially specified program forces relational mode."
    (let [rel (logic/lvar 'rel)
          x   (fresh-nom 'x)]
      (is (= :relational
             (exec/execution-mode [[rel [x] ['pos ['app 'p]]]]
                                  ['pos ['app 'q]]))))))

(deftest test-XE04-prove-dispatches-to-fast
  (testing "Ground proving dispatches to the fast engine."
    (let [called (atom [])]
      (with-redefs [fast/prove-fast (fn [& _] (swap! called conj :fast) [:fast])
                    ref/prove       (fn [& _] (swap! called conj :rel)  [:rel])]
        (is (= [:fast]
               (exec/prove '(and (pos (app p)) (neg (app p))) 1)))
        (is (= [:fast] @called))))))

(deftest test-XE05-prove-dispatches-to-relational
  (testing "Non-ground proving dispatches to the relational engine."
    (let [called (atom [])
          x      (logic/lvar 'x)]
      (with-redefs [fast/prove-fast (fn [& _] (swap! called conj :fast) [:fast])
                    ref/prove       (fn [& _] (swap! called conj :rel)  [:rel])]
        (is (= [:rel]
               (exec/prove '() ['neq x x] 1 nil)))
        (is (= [:rel] @called))))))

(deftest test-XE06-query-dispatch-agrees-on-ground-case
  (testing "Unified query API agrees with the fast engine on a ground case."
    (let [program [['r [] ['and ['pos ['app 'p]]
                               ['neg ['app 'p]]]]]
          query   ['pos ['app 'r]]]
      (is (= (fast/query-fails-fast program query 1 nil)
             (exec/query-fails program query 1 nil))))))

(deftest test-XE07-reference-proveo-exposed
  (testing "The execution layer explicitly exposes the relational prover."
    (let [called (atom false)]
      (with-redefs [ref/proveo (fn [& _] (reset! called true) :ok)]
        (is (= :ok (exec/reference-proveo :a :b :c)))
        (is @called)))))
