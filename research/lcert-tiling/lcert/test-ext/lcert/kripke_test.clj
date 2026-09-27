(ns lcert.kripke-test
  "Tests of the Ansatz-checked lemmas of Step 3 (proflog ADR-0143,
  ansatz/lcert/kripke.clj): the world-indexed Kripke model that settles the
  higher-order case.

  Presence and kernel re-verification of every lemma, and the statements
  that carry the result pinned: the Reflect case holds for EVERY embedded
  type (no class hypothesis), and at shape (c) R4's fixed-cap sets reject
  what the world model accepts."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [lcert.kripke :as kr]))

(deftest the-lemmas-are-in-the-kernel
  (doseq [nm kr/theorem-names]
    (is (kr/declared? nm) nm)
    (is (kr/reverifies? nm) (str "the kernel re-checks " nm))))

(deftest the-key-statements
  (testing "monotonicity: down in worlds, up in footprint, for every type"
    (is (= "(∀ : STy, (WorldMono #0))" (kr/statement "vw_world_mono")))
    (is (= "(∀ : STy, (FootMono #0))" (kr/statement "vw_foot_mono"))))
  (testing "the Reflect case quantifies over every type, with no class hypothesis"
    (let [s (kr/statement "reflect_case_world")]
      (is (str/starts-with? s "(∀ : STy, (∀ : (∀ : Nat, (∀ : CT, (SCar #2)))"))
      (is (not (str/includes? s "IsF")))
      (is (not (str/includes? s "IsD")))
      (is (str/includes? s "VW"))
      (is (str/includes? s "(Nat.add (Nat.sub"))))
  (testing "the application and pair cases compose burns"
    (is (str/includes? (kr/statement "app_case_world") "VW"))
    (is (str/includes? (kr/statement "pair_case_world") "STy.tten")))
  (testing "shape (c): the fixed-cap sets hold the value at the charged cap, not at the caller's"
    (is (str/includes? (kr/statement "fixed_accepts_c") "VF"))
    (is (str/ends-with? (kr/statement "fixed_rejects_c") "False)"))
    (is (str/includes? (kr/statement "shape_c_world_ok") "VW"))))
