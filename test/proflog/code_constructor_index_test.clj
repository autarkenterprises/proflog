(ns proflog.code-constructor-index-test
  "ADR-0107 integration agreement. `code-constructor-buildo`, re-expressed over
   the pure fd-trie indexed lookup (`clojure.core.logic.index/int-indexo`), must
   have *exactly* the same answer set as the original linear `or*` over the
   constructor table, in every mode -- by structure, with no `project`/`conda`/
   `condu`/host cut. The isolated primitive contract lives in
   `proflog.core-logic-indexed-lookup-test`; this pins the production re-expression."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l :refer [== fresh or* run*]]
            [clojure.test :refer [deftest is testing]]
            [proflog.willard-sjas-code :as sjas-code]
            proflog.kernel.willard-sjas-profile))

(def ^:private code-constructor-buildo
  "The integrated relation under test (private in the profile namespace)."
  @#'proflog.kernel.willard-sjas-profile/code-constructor-buildo)

(defn- linear-buildo
  "Faithful reconstruction of the *original* linear constructor relation: a flat
   `or*` over the same source table, byte-count-keyed. This is the pre-ADR-0107
   behaviour the re-expression must preserve."
  [constructor byte-count]
  (or*
    (map (fn [[entry-constructor entry-byte-count]]
           (fresh []
             (== entry-byte-count byte-count)
             (== entry-constructor constructor)))
         sjas-code/code-functions)))

(deftest code-constructor-buildo-agrees-with-linear-baseline
  (testing "forward (ground byte-count): same single constructor, no double-count"
    (doseq [bc [0 1 42 1590 4095]]
      (is (= (run* [c] (linear-buildo c bc))
             (run* [c] (code-constructor-buildo c bc)))
          (str "forward byte-count " bc))
      (is (= 1 (count (run* [c] (code-constructor-buildo c bc))))
          (str "exactly one answer for byte-count " bc))))
  (testing "forward on an out-of-table byte-count fails in both"
    (doseq [bc [4096 9999]]
      (is (= (run* [c] (linear-buildo c bc))
             (run* [c] (code-constructor-buildo c bc)))
          (str "absent byte-count " bc))
      (is (= '() (run* [c] (code-constructor-buildo c bc)))
          (str "no answer for absent byte-count " bc))))
  (testing "backward (ground constructor): same byte-count"
    (doseq [c '[code-0 code-42 code-4095]]
      (is (= (run* [bc] (linear-buildo c bc))
             (run* [bc] (code-constructor-buildo c bc)))
          (str "backward constructor " c))))
  (testing "free enumeration: identical answer set (sound + complete)"
    (is (= (set (run* [q] (fresh [c bc] (linear-buildo c bc) (== [c bc] q))))
           (set (run* [q] (fresh [c bc] (code-constructor-buildo c bc) (== [c bc] q)))))
        "indexed enumeration equals linear enumeration as a set")
    (is (= (count sjas-code/code-functions)
           (count (run* [q] (fresh [c bc] (code-constructor-buildo c bc) (== [c bc] q)))))
        "every table entry enumerated exactly once")))
