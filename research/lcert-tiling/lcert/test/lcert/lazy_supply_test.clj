(ns lcert.lazy-supply-test
  "Step 2 of proflog ADR-0143: lazy finite supplies (METATHEORY.md §2.3).
  (Not part of jpt4/sjas.)

  A lazy finite supply promises N nodes and materializes a node only when a
  program takes it apart.  It denotes one finite tree: the right spine of N
  nodes with leaf left children, whose i-th token is the i-th of the
  supply's own.  These tests check that it reports its promised size without
  materializing, that forcing it yields exactly that tree, that programs
  cannot tell the two apart, that a program built on caseR materializes only
  what it destructs — even from a supply promised at 10^100 — and that an
  operation that would walk the whole supply stops at a guard instead of
  running forever."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.kernel :as k]
            [lcert.examples :as ex]))

(defn- closed-fn
  "Type check the closed surface program `form` and evaluate it (erasing)
  under cap n; returns the runtime value, usually a function."
  [n form]
  (#'ev/ev (ev/erase (t/check-top 0 (s/parse-term [] form))) [] n {:erase? true}))

(defmacro ^:private counting
  "Evaluate body with a fresh materialization counter; return [value count]."
  [& body]
  `(let [c# (atom 0)]
     (binding [ev/*materialized* c#]
       (let [v# (do ~@body)] [v# @c#]))))

(deftest a-lazy-supply-reports-its-promise
  (let [[v n] (counting (ev/nodes (ev/lazy-supply 1000)))]
    (is (= 1000 v))
    (is (zero? n) "no node was materialized"))
  (let [huge (.pow (biginteger 10) 100)
        [v n] (counting (ev/nodes (ev/lazy-supply huge)))]
    (is (= huge (biginteger v)))
    (is (zero? n))))

(deftest forcing-yields-the-spine
  (let [lz (ev/lazy-supply 40)
        [tree n] (counting (ev/materialize lz))]
    (is (= 40 n) "one materialization per node")
    (is (= 40 (ev/nodes tree)))
    (is (not (ev/lazy? tree)))
    (testing "it is the spine: every left child a leaf"
      (is (every? #(= [:rl :a] (nth % 3))
                  (take-while #(= :rn (first %)) (iterate #(nth % 4) tree)))))
    (testing "its tokens are distinct, and forcing again yields the same ones"
      (is (apply distinct? (ev/tokens tree)))
      (is (= (ev/tokens tree) (ev/tokens (ev/materialize lz)))))))

(def ^:private programs
  "Programs of type R ⊸ X, X data, to run on a supply."
  {:count-nodes '(fn [r 1 R] (itr Nat (fn [l w Lbl] zero)
                                  (fn [d 1 Dia] (fn [l w Lbl] (fn [m 1 Nat] (fn [n 1 Nat]
                                    (succ (rec-nat [z Nat] m [k y] (succ y) n))))))
                                  r))
   :print :print
   :top-two '(fn [r 1 R] (case-r Nat r [a] zero
                                 [d a2 r1 r2] (case-r Nat r2 [b] (succ zero) [d2 b2 u v] (succ (succ zero)))))})

(defn- run-on [prog supply n]
  (if (= prog :print)
    (ev/print-value supply)
    ((closed-fn n prog) supply)))

(deftest programs-cannot-tell-lazy-from-eager
  (doseq [size [0 1 2 17]
          [nm prog] programs]
    (let [lz (ev/lazy-supply size)
          eager (ev/materialize lz)]
      (is (= (run-on prog eager size) (run-on prog lz size)) (str nm " on " size " nodes"))))
  (testing "the parser, built on caseR, returns the same certificate and rest"
    (let [code [:sn :a [:sn :b [:sl :c] [:sl :b]] [:sl :a]]
          lz (ev/lazy-supply 9)
          parse (closed-fn 9 ex/parse-prim-form)
          [_ c1 r1] ((parse code) lz)
          [_ c2 r2] ((parse code) (ev/materialize lz))]
      (is (= (ev/print-value c1) (ev/print-value c2) code))
      (is (= (ev/tokens c1) (ev/tokens c2)))
      (is (= (ev/print-value r1) (ev/print-value r2))))))

(deftest destructing-materializes-only-what-it-takes
  (let [huge (.pow (biginteger 10) 100)
        code [:sn :a [:sn :b [:sl :c] [:sl :b]] [:sn :c [:sl :a] [:sn :b [:sl :a] [:sl :b]]]]
        parse (closed-fn huge ex/parse-prim-form)
        [[_ cert rest] n] (counting ((parse code) (ev/lazy-supply huge)))]
    (testing "parsing a code of 4 nodes from a supply promised at 10^100"
      (is (= code (ev/print-value cert)))
      (is (= 4 n) "exactly one supply node per code node")
      (is (ev/lazy? rest) "the rest stays unmaterialized")
      (is (= (- huge 4) (biginteger (ev/nodes rest)))))))

(deftest the-guard-stops-a-whole-supply-walk
  (let [huge (.pow (biginteger 10) 100)]
    (testing "print on a 10^100 supply stops at the materialization limit"
      (binding [ev/*materialize-limit* 1000]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"materializ"
                              (ev/print-value (ev/lazy-supply huge))))))
    (testing "so does the parser built on the definable out, which iterates over the whole supply"
      (binding [ev/*materialize-limit* 1000]
        (let [parse (closed-fn huge ex/parse-form)]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo #"materializ"
                                ((parse [:sn :a [:sl :b] [:sl :c]]) (ev/lazy-supply huge)))))))))
