(ns proflog.pelletier-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.normalize :as normalize]))

;; Upstream source of record:
;; https://github.com/namin/leanTAP/blob/master/cljtap/test/cljtap/test/alphaleantap.clj
;;
;; The formulas below are proved as ordinary closed-tableau obligations:
;; conjoin the upstream axioms, negate the theorem, normalize to NNF, and ask
;; the greenfield kernel to close that pure first-order branch. No Proflog
;; clauses or theorem-specific overlays are involved.

(def status-values
  #{:ported-passing
    :ported-too-slow
    :not-yet-ported
    :requires-kernel-work})

(defn term
  [sym]
  (ast/app-term sym))

(defn v
  [nom]
  (ast/var-term nom))

(defn pred
  [relation & args]
  (ast/pos-lit (apply ast/app-term relation args)))

(defn not*
  [formula]
  (ast/not-form formula))

(defn and2
  [left right]
  (ast/and-form left right))

(defn or2
  [left right]
  (ast/or-form left right))

(defn implies
  [antecedent consequent]
  (ast/implies-form antecedent consequent))

(defn iff
  [left right]
  (and2 (implies left right)
        (implies right left)))

(defn forall
  [nom body]
  (ast/forall-form nom body))

(defn exists
  [nom body]
  (ast/exists-form nom body))

(defn conjoin
  [formulas]
  (reduce and2 formulas))

(defn theorem-branch
  [{:keys [axioms theorem]}]
  (normalize/to-nnf
    (conjoin (concat axioms [(not* theorem)]))))

(defn proposition
  [sym]
  (pred sym))

(def p (proposition 'p))
(def q (proposition 'q))
(def r (proposition 'r))
(def s (proposition 's))

(defn empty-theorem
  [theorem]
  {:axioms []
   :theorem theorem})

(defn problem-1 []
  (empty-theorem
    (iff (implies p q)
         (implies (not* q) (not* p)))))

(defn problem-2 []
  (empty-theorem
    (implies (not* (not* p)) p)))

(defn problem-3 []
  (empty-theorem
    (implies (not* (implies p q))
             (implies q p))))

(defn problem-4 []
  (empty-theorem
    (iff (implies (not* p) q)
         (implies (not* q) p))))

(defn problem-5 []
  (empty-theorem
    (implies (implies (or2 p q)
                      (or2 p r))
             (or2 p (implies q r)))))

(defn problem-6 []
  (empty-theorem
    (or2 p (not* p))))

(defn problem-7 []
  (empty-theorem
    (or2 p (not* (not* (not* p))))))

(defn problem-8 []
  (empty-theorem
    (implies (implies (implies p q) p)
             p)))

(defn problem-9 []
  (empty-theorem
    (implies
      (and2 (or2 p q)
            (and2 (or2 (not* p) q)
                  (or2 p (not* q))))
      (not* (or2 (not* p) (not* q))))))

(defn problem-10 []
  {:axioms [(implies q r)
            (implies r (and2 p q))
            (implies p (or2 q r))]
   :theorem (iff p q)})

(defn problem-11 []
  (empty-theorem
    (iff p p)))

(defn problem-12 []
  (empty-theorem
    (iff (iff (iff p q) r)
         (iff p (iff q r)))))

(defn problem-13 []
  (empty-theorem
    (iff (or2 p (and2 q r))
         (and2 (or2 p q)
               (or2 p r)))))

(defn problem-14 []
  (empty-theorem
    (iff (iff p q)
         (and2 (or2 q (not* p))
               (or2 (not* q) p)))))

(defn problem-15 []
  (empty-theorem
    (iff (implies p q)
         (or2 (not* p) q))))

(defn problem-16 []
  (empty-theorem
    (or2 (implies p q)
         (implies q p))))

(defn problem-17 []
  (empty-theorem
    (iff (implies (and2 p (implies q r)) s)
         (and2 (or2 (not* p) (or2 q s))
               (or2 (not* p) (or2 (not* r) s))))))

(defn problem-18 []
  (ast/nom y x
    (empty-theorem
      (exists y
              (forall x
                      (implies (pred 'f (v y))
                               (pred 'f (v x))))))))

(defn problem-19 []
  (ast/nom x y z
    (empty-theorem
      (exists x
              (forall y
                      (forall z
                              (implies
                                (implies (pred 'p (v y))
                                         (pred 'q (v z)))
                                (implies (pred 'p (v x))
                                         (pred 'q (v x))))))))))

(defn problem-20 []
  (ast/nom x y z w x2 y2 z2
    (empty-theorem
      (forall x
              (forall y
                      (exists z
                              (forall w
                                      (implies
                                        (implies
                                          (and2 (pred 'p (v x))
                                                (pred 'q (v y)))
                                          (and2 (pred 'r (v z))
                                                (pred 's (v w))))
                                        (exists x2
                                                (exists y2
                                                        (implies
                                                          (and2 (pred 'p (v x2))
                                                                (pred 'q (v y2)))
                                                          (exists z2
                                                                  (pred 'r (v z2))))))))))))))

(def problem-catalog
  (concat
    [{:id 1 :status :ported-passing :builder problem-1}
     {:id 2 :status :ported-passing :builder problem-2}
     {:id 3 :status :ported-passing :builder problem-3}
     {:id 4 :status :ported-passing :builder problem-4}
     {:id 5 :status :ported-passing :builder problem-5}
     {:id 6 :status :ported-passing :builder problem-6}
     {:id 7 :status :ported-passing :builder problem-7}
     {:id 8 :status :ported-passing :builder problem-8}
     {:id 9 :status :ported-passing :builder problem-9}
     {:id 10 :status :ported-passing :builder problem-10}
     {:id 11 :status :ported-passing :builder problem-11}
     {:id 12 :status :ported-too-slow :builder problem-12}
     {:id 13 :status :ported-passing :builder problem-13}
     {:id 14 :status :ported-passing :builder problem-14}
     {:id 15 :status :ported-passing :builder problem-15}
     {:id 16 :status :ported-passing :builder problem-16}
     {:id 17 :status :ported-passing :builder problem-17}
     {:id 18 :status :ported-passing :builder problem-18}
     {:id 19 :status :ported-passing :builder problem-19}
     {:id 20 :status :ported-passing :builder problem-20}]
    (map (fn [id]
           {:id id
            :status :not-yet-ported})
         (range 21 47))))

(def problem-by-id
  (into {} (map (juxt :id identity) problem-catalog)))

(def prompt-passing-ids
  [1 2 3 4 5 6 7 8 9 11 13 14 15 16 18 19 20])

(def slow-passing-ids
  [10 17])

(def ported-too-slow-ids
  [12])

(def ported-passing-ids
  (into prompt-passing-ids slow-passing-ids))

(defn proof-for
  [id]
  (let [{:keys [builder]} (problem-by-id id)]
    (kernel/prove (theorem-branch (builder)) 1)))

(deftest ^:pelletier-catalog catalog-covers-upstream-pelletier-problems
  (testing "every upstream Pelletier problem 1 through 46 has an explicit local status"
    (is (= (set (range 1 47))
           (set (map :id problem-catalog))))
    (is (every? status-values
                (map :status problem-catalog)))))

(deftest ^{:pelletier-prompt true
           :pelletier-passing true}
  legacy-mirrored-pelletier-slice-closes
  (testing "the greenfield kernel proves the Pelletier slice already mirrored by the legacy tests"
    (doseq [id [1 2 18]]
      (is (seq (proof-for id))
          (str "Pelletier Problem " id " should close")))))

(deftest ^{:pelletier-prompt true
           :pelletier-passing true}
  prompt-passing-pelletier-tranche-closes
  (testing "the prompt subset of the initial ADR-0022 tranche closes in the pure kernel"
    (doseq [id prompt-passing-ids]
      (is (seq (proof-for id))
          (str "Pelletier Problem " id " should close")))))

(deftest ^:pelletier-passing slow-passing-pelletier-problems-close
  (testing "ported slow problems close, but remain outside the prompt selector"
    (doseq [id slow-passing-ids]
      (is (seq (proof-for id))
          (str "Pelletier Problem " id " should close")))))

(deftest ^:pelletier-exploratory ported-too-slow-problems-are-classified
  (testing "ported formulas that are not committed as passing regressions stay visible"
    (doseq [id ported-too-slow-ids
            :let [{:keys [status builder]} (problem-by-id id)]]
      (is (= :ported-too-slow status)
          (str "Pelletier Problem " id " should be classified as too slow"))
      (is builder
          (str "Pelletier Problem " id " should keep its ported builder")))))
