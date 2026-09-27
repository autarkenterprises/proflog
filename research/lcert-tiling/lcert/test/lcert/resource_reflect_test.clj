(ns lcert.resource-reflect-test
  "Assessor's demo (not part of jpt4/sjas): what goes wrong when reflect
  targets a type that consumes certificates, Π(s :₁ R) …, under the fixed
  budget cap, and what a cap charged to the caller's resources changes."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.check :as c]
            [lcert.eval :as ev]
            [lcert.typing :as t]
            [lcert.core :as lc]))

(def sigma-form '(Sigma [x w Nat] (T (rec-nat [z Bool] ff [k y] tt x))))

(def f-form
  "The child's certified program: given a certificate of Σ(x :ω Nat). T(x ≠ 0)
  and its evidence, return the witness it certifies."
  (list 'fn '[s 1 R]
        (list 'fn ['e 1 (list 'T (list 'chk '(print s) (list 'code sigma-form)))]
              (list 'reflect sigma-form 's 'e))))

(defn demo-program
  "reflect the certificate of f-form, apply the result to a certificate of
  the pair (1, ⋆), and return the witness's first component.  Returns
  [program budget]."
  []
  (binding [s/*allow-resource-reflect* true]
    (let [cf (lc/certify 0 f-form)
          cw (lc/certify 0 (list 'pair sigma-form 1 'star))
          nf (:nodes cf) nw (:nodes cw)
          f-type (:type cf)
          lit-f (s/shift (c/certificate-literal (:code cf)) nw)
          lit-w (c/certificate-literal (:code cw))
          sigma (s/parse-type [] sigma-form)
          prog [:let [:Nat]
                [:app [:app [:reflect f-type lit-f [:star]] lit-w] [:star]]
                [:var 1]]]
      [prog (+ nf nw)])))

(deftest reflecting-a-certificate-consuming-function
  (binding [s/*allow-resource-reflect* true]
    (let [[prog n] (demo-program)
          d (t/check-top n prog)]
      (testing "under the fixed cap, the reflected function runs with its own budget 0, so the
                certificate it is handed is over its cap and it returns the default witness 0,
                which does not satisfy T(x ≠ 0)"
        (is (= 0 (ev/eval-deriv d n {:erase? true}))))
      (testing "charging the call to the caller's resources, it returns the certified witness 1"
        (binding [ev/*dynamic-cap* true]
          (is (= 1 (ev/eval-deriv d n {:erase? true}))))))))
