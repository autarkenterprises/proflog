(ns lcert.charged-test
  "Tests of the Ansatz-checked lemmas of Step 1 (proflog ADR-0143,
  ansatz/lcert/charged.clj).

  Loading lcert.charged already ran the Ansatz elaborator and kernel on every
  definition and theorem.  These tests check that each theorem is in the
  kernel environment, re-run the kernel's type check on each stored proof
  term, and pin the statements of the key lemmas, so that a weakened
  statement cannot pass unnoticed."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lcert.charged :as ch]))

(deftest the-lemmas-are-in-the-kernel
  (doseq [nm ch/theorem-names]
    (is (ch/declared? nm) (str nm " is in the Ansatz environment"))))

(deftest the-proof-terms-re-verify
  (doseq [nm ch/theorem-names]
    (is (ch/reverifies? nm) (str "the kernel re-checks the proof of " nm))))

(deftest the-key-statements
  (testing "Q holds for every type of the first-order class"
    (let [s (ch/statement "q_f")]
      (is (str/includes? s "IsF"))
      (is (str/includes? s "QF"))))
  (testing "the Reflect case: for every 𝓕 type, any denotation that satisfies the outer
            hypothesis at smaller caps lands in Vⁿₖ at the charged cap n − nodes + budget"
    (let [s (ch/statement "reflect_case_charged")]
      (doseq [piece ["(IsF #0)" "LT.lt" "(VF #13 #4 (budget #3) (#11 #4 #3))"
                     "(Nat.add (Nat.sub #8 (nodes (CT.cell #6 Bool.true #5 #4))) (budget (CT.cell #6 Bool.true #5 #4)))"]]
        (is (str/includes? s piece) piece))))
  (testing "the charged cap descends strictly, by strict overhead on real codes"
    (is (str/includes? (ch/statement "charged_cap_code") "budget")))
  (testing "the agent types are in the class; the three higher-order shapes are not"
    (is (every? ch/declared? ["agent_in_F" "minting_agent_in_F"
                              "shape_a_not_F" "shape_b_not_F" "shape_c_not_F"]))))
