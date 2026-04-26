(ns proflog.tabling-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.tabling :as tabling]))

(deftest canonical-state-keys-ignore-order-and-alpha-equivalent-noms
  (testing "agenda, saved literals, disequalities, and substitutions are canonicalized"
    (let [left-key
          (ast/nom x y
            (tabling/state-key
              {:agenda [(ast/pos-lit (ast/app-term 'p (ast/var-term x)))
                        (ast/neg-lit (ast/app-term 'q (ast/var-term y)))]
               :lits [(ast/pos-lit (ast/app-term 'r (ast/var-term x)))
                      (ast/neg-lit (ast/app-term 's (ast/var-term y)))]
               :neqs [[(ast/var-term x) (ast/app-term 'a)]
                      [(ast/var-term y) (ast/app-term 'b)]]
               :sigma [[x (ast/app-term 'a)]
                       [y (ast/app-term 'b)]]
               :prog-key :same-program}))
          right-key
          (ast/nom a b
            (tabling/state-key
              {:agenda [(ast/neg-lit (ast/app-term 'q (ast/var-term b)))
                        (ast/pos-lit (ast/app-term 'p (ast/var-term a)))]
               :lits [(ast/neg-lit (ast/app-term 's (ast/var-term b)))
                      (ast/pos-lit (ast/app-term 'r (ast/var-term a)))]
               :neqs [[(ast/var-term b) (ast/app-term 'b)]
                      [(ast/var-term a) (ast/app-term 'a)]]
               :sigma [[b (ast/app-term 'b)]
                       [a (ast/app-term 'a)]]
               :prog-key :same-program}))]
      (is (= left-key right-key)))))

(deftest canonical-state-keys-distinguish-non-equivalent-states
  (testing "different predicates, bindings, or program keys do not collapse"
    (ast/nom x
      (let [base {:agenda [(ast/pos-lit (ast/app-term 'p (ast/var-term x)))]
                  :sigma [[x (ast/app-term 'a)]]
                  :prog-key :program-a}
            different-predicate {:agenda [(ast/pos-lit (ast/app-term 'q (ast/var-term x)))]
                                 :sigma [[x (ast/app-term 'a)]]
                                 :prog-key :program-a}
            different-binding {:agenda [(ast/pos-lit (ast/app-term 'p (ast/var-term x)))]
                               :sigma [[x (ast/app-term 'b)]]
                               :prog-key :program-a}
            different-program {:agenda [(ast/pos-lit (ast/app-term 'p (ast/var-term x)))]
                               :sigma [[x (ast/app-term 'a)]]
                               :prog-key :program-b}]
        (is (not= (tabling/state-key base)
                  (tabling/state-key different-predicate)))
        (is (not= (tabling/state-key base)
                  (tabling/state-key different-binding)))
        (is (not= (tabling/state-key base)
                  (tabling/state-key different-program)))))))
