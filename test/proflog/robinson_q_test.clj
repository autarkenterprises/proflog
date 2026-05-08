(ns proflog.robinson-q-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.frontend :as pf]
            [proflog.language :as language]
            [proflog.proof :as proof]
            [proflog.query :as query]
            [proflog.robinson-q :as rq]))

(def frontend-profile-language
  (pf/language
    (constants zero)
    (functions (s 1)
               (add 2)
               (mul 2))
    (relations)
    (proof-profile :robinson-q)))

(defn succeeds?
  [program formula fuel]
  (seq (query/query-succeeds program formula 1 fuel)))

(defn first-success-proof
  [program formula fuel]
  (first (query/query-succeeds program formula 1 fuel)))

(deftest robinson-q-language-keeps-arithmetic-in-term-namespace
  (testing "Q uses constants and function symbols, not procedural relations"
    (is (= #{'zero} (:constants rq/language)))
    (is (= {'zero 0
            's 1
            'add 2
            'mul 2}
           (:functions rq/language)))
    (is (empty? (:relations rq/language)))
    (is (= :robinson-q (:proof-profile rq/profile-language)))
    (is (= :robinson-q (:proof-profile frontend-profile-language)))))

(deftest robinson-q-axioms-are-valid-first-order-formulas
  (testing "the seven Q axioms validate over the arithmetic function language"
    (is (= 7 (count rq/axioms)))
    (doseq [[label formula] rq/axioms]
      (is (= formula (language/validate-query rq/language formula))
          (str label " should validate over the Q language")))))

(deftest ordinary-q-as-antecedent-proves-shared-formulas
  (testing "ordinary Q assumptions can prove selected consequences through the existing kernel"
    (doseq [[label theorem fuel] [[:q7 rq/q7 32]
                                  [:add-one-zero (rq/eq (rq/add (rq/numeral 1)
                                                                 rq/zero)
                                                        (rq/numeral 1))
                                   48]
                                  [:mul-two-zero (rq/eq (rq/mul (rq/numeral 2)
                                                                 rq/zero)
                                                        rq/zero)
                                   48]
                                  [:add-one-two (rq/eq (rq/add (rq/numeral 1)
                                                                (rq/numeral 2))
                                                       (rq/numeral 3))
                                   64]
                                  [:mul-two-two (rq/eq (rq/mul (rq/numeral 2)
                                                                (rq/numeral 2))
                                                       (rq/numeral 4))
                                   96]]]
      (let [proof (first-success-proof
                    rq/ordinary-program
                    (rq/q-implies theorem)
                    fuel)]
        (is proof (str "ordinary Q antecedent should prove " label))
        (is (not (proof/contains-step? proof 'robinson-q))
            "ordinary Q should not silently use the deduction-modulo profile")))))

(deftest profiled-robinson-q-proves-shared-formulas-by-conversion
  (testing "the opt-in profile proves the same examples with explicit profile evidence"
    (doseq [[label theorem fuel] [[:q7 rq/q7 16]
                                  [:add-one-zero (rq/eq (rq/add (rq/numeral 1)
                                                                 rq/zero)
                                                        (rq/numeral 1))
                                   16]
                                  [:mul-two-zero (rq/eq (rq/mul (rq/numeral 2)
                                                                 rq/zero)
                                                        rq/zero)
                                   16]
                                  [:add-one-two (rq/eq (rq/add (rq/numeral 1)
                                                                (rq/numeral 2))
                                                       (rq/numeral 3))
                                   16]
                                  [:mul-two-two (rq/eq (rq/mul (rq/numeral 2)
                                                                (rq/numeral 2))
                                                       (rq/numeral 4))
                                   16]]]
      (let [proof (first-success-proof rq/profile-program theorem fuel)]
        (is proof (str "profiled Q should prove " label))
        (is (proof/contains-step? proof 'profiled))
        (is (proof/contains-step? proof 'robinson-q))))))

(deftest profiled-robinson-q-records-repeated-arithmetic-conversion
  (testing "profile conversion records repeated add/mul rewrites before kernel equality closure"
    (let [add-proof (first-success-proof
                      rq/profile-program
                      (rq/eq (rq/add (rq/numeral 1)
                                     (rq/numeral 2))
                             (rq/numeral 3))
                      16)
          mul-proof (first-success-proof
                      rq/profile-program
                      (rq/eq (rq/mul (rq/numeral 2)
                                     (rq/numeral 2))
                             (rq/numeral 4))
                      16)]
      (is add-proof)
      (is (proof/contains-step? add-proof 'q-rewrite))
      (is mul-proof)
      (is (proof/contains-step? mul-proof 'q-rewrite)))))

(deftest q3-is-proved-by-ordinary-assumptions-and-profile-case-split
  (testing "ordinary Q proves Q3 from assumptions, while the profile records the Q3 case split"
    (let [ordinary-proof (first-success-proof
                           rq/ordinary-program
                           (rq/q-implies rq/q3)
                           32)
          profile-proof (first-success-proof
                          rq/profile-program
                          rq/q3
                          32)]
      (is ordinary-proof)
      (is (not (proof/contains-step? ordinary-proof 'robinson-q)))
      (is profile-proof)
      (is (proof/contains-step? profile-proof 'profiled))
      (is (proof/contains-step? profile-proof 'robinson-q))
      (is (proof/contains-step? profile-proof 'q3-case-split)))))
