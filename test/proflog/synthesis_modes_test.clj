(ns proflog.synthesis-modes-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.language :as language]
            [proflog.pretty :as pretty]))

(def synthesis-language
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'step 2
                 'jump 2}}))

(def recursive-synthesis-language
  (language/language
    {:constants ['zero]
     :functions {'s 1}
     :relations {'down 2}}))

(defn numeral
  [n]
  (if (zero? n)
    (ast/app-term 'zero)
    (ast/app-term 's (numeral (dec n)))))

(defn synthesis-program
  []
  (ast/nom x y z
    (language/compile-program
      synthesis-language
      [(ast/clause 'step [x y]
                   (ast/or-form
                     (ast/eq-lit (ast/var-term x)
                                 (ast/app-term 's (ast/var-term y)))
                     (ast/eq-lit (ast/var-term x)
                                 (ast/app-term 's
                                               (ast/app-term 's
                                                             (ast/var-term y))))))
       (ast/clause 'jump [x y]
                   (ast/exists-form z
                                    (ast/and-form
                                      (ast/pos-lit (ast/app-term 'step
                                                                 (ast/var-term x)
                                                                 (ast/var-term z)))
                                      (ast/pos-lit (ast/app-term 'step
                                                                 (ast/var-term z)
                                                                 (ast/var-term y))))))])))

(defn down-program
  []
  (ast/nom x y z
    (language/compile-program
      recursive-synthesis-language
      [(ast/clause 'down [x y]
                   (ast/or-form
                     (ast/eq-lit (ast/var-term x) (ast/var-term y))
                     (ast/exists-form z
                                      (ast/and-form
                                        (ast/eq-lit (ast/var-term x)
                                                    (ast/app-term 's (ast/var-term z)))
                                        (ast/pos-lit (ast/app-term 'down
                                                                   (ast/var-term z)
                                                                   (ast/var-term y)))))))])))

(defn ground-decimals
  [records binding-nom]
  (->> records
       (keep #(pretty/peano->int (answers/binding-term % binding-nom)))
       vec))

(defn neq-residuals-only?
  [records]
  (every? (fn [record]
            (every? #(= 'neq (ast/tag-of %))
                    (:residuals record)))
          records))

(deftest partial-mode-step-query-produces-ground-predecessor-successors
  (testing "step(x, 1) synthesizes the two ground successors of 1"
    (ast/nom x
      (let [records (answers/query-answers
                      (synthesis-program)
                      (ast/pos-lit (ast/app-term 'step (ast/var-term x) (numeral 1)))
                      [x]
                      {:proof-limit 3
                       :call-depth 1})]
        (is (= [2 3]
               (ground-decimals records x)))
        (is (neq-residuals-only? records))))))

(deftest reverse-mode-step-query-honors-additional-constraints
  (testing "step(3, y) with y != 2 keeps only the remaining valid predecessor"
    (ast/nom y
      (let [records (answers/query-answers
                      (synthesis-program)
                      (ast/and-form
                        (ast/pos-lit (ast/app-term 'step (numeral 3) (ast/var-term y)))
                        (ast/neq-lit (ast/var-term y) (numeral 2)))
                      [y]
                      {:proof-limit 3
                       :call-depth 1})]
        (is (= [1]
               (ground-decimals records y)))))))

(deftest open-step-query-exports-symbolic-families
  (testing "step(x, y) exports the two symbolic clause families directly"
    (ast/nom x y
      (let [records (answers/query-answers
                      (synthesis-program)
                      (ast/pos-lit (ast/app-term 'step (ast/var-term x) (ast/var-term y)))
                      [x y]
                      {:proof-limit 2
                       :call-depth 1})
            bindings (set (map :bindings records))]
        (is (= #{[[x (ast/app-term 's (ast/var-term y))]
                  [y (ast/var-term y)]]
                 [[x (ast/app-term 's (ast/app-term 's (ast/var-term y)))]
                  [y (ast/var-term y)]]}
               bindings))
        (is (neq-residuals-only? records))))))

(deftest composed-partial-mode-query-traverses-multiple-calls
  (testing "jump(x, 0) synthesizes the reachable positions through the intermediate call chain"
    (ast/nom x
      (let [records (answers/query-answers
                      (synthesis-program)
                      (ast/pos-lit (ast/app-term 'jump (ast/var-term x) (numeral 0)))
                      [x]
                      {:proof-limit 6
                       :call-depth 2})
            decimals (set (ground-decimals records x))]
        (is (= #{2 3 4} decimals))
        (is (neq-residuals-only? records))))))

(deftest recursive-reverse-mode-query-synthesizes-descendants
  (testing "down(2, y) returns the reachable recursive descendants of 2"
    (ast/nom y
      (let [records (answers/query-answers
                      (down-program)
                      (ast/pos-lit (ast/app-term 'down (numeral 2) (ast/var-term y)))
                      [y]
                      {:proof-limit 2
                       :fuel 8
                       :call-depth 3})]
        (is (= [2 1]
               (ground-decimals records y)))
        (is (neq-residuals-only? records))))))

(deftest recursive-partial-mode-query-synthesizes-ancestors
  (testing "down(x, 1) returns the recursive ancestors of 1 within the unfolding bound"
    (ast/nom x
      (let [records (answers/query-answers
                      (down-program)
                      (ast/pos-lit (ast/app-term 'down (ast/var-term x) (numeral 1)))
                      [x]
                      {:proof-limit 2
                       :fuel 8
                       :call-depth 3})]
        (is (= [1 2]
               (ground-decimals records x)))
        (is (neq-residuals-only? records))))))
