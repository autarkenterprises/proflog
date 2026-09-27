(ns lcert.world-reflect-test
  "Step 3 of proflog ADR-0143: reflect at every closed reflect-free type,
  under the caller-charged cap (METATHEORY.md §3).  (Not part of jpt4/sjas.)

  The world model (Ansatz: lcert.kripke/reflect_case_world) justifies the
  :all reflect policy together with lcert.eval/*charged-cap*.  It relies on
  one property of the evaluator: a closure runs at the cap it was created
  under, as the model's ⟦λx. t⟧ⁿ does, not at the cap of whoever calls it.
  These tests pin that property and run the three higher-order shapes that
  R4's model cannot validate."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.core :as lc]
            [lcert.ho-forms :as f]))

(defn- code-of [form]
  (binding [s/*reflect-class* :all] (:code (lc/certify 0 form))))

(defn- cert-value
  "A runtime certificate with the shape of `code`, spending fresh tokens
  numbered from `start` in preorder.  Returns [value next]."
  [code start]
  (case (first code)
    :sl [[:rl (second code)] start]
    :sn (let [[_ l a b] code
              tok (ev/token start)
              [va n1] (cert-value a (inc start))
              [vb n2] (cert-value b n1)]
          [[:rn tok l va vb] n2])))

(defn- certs [codes]
  (reduce (fn [[vs next] c] (let [[v n] (cert-value c next)] [(conj vs v) n]))
          [[] 1] codes))

(defn- run-traced
  "Type check `form` closed under the :all policy, evaluate it erased under
  cap n with the charged cap, apply it to `args`; return [result events]."
  [n form & args]
  (let [events (atom [])]
    (binding [s/*reflect-class* :all
              ev/*charged-cap* true
              ev/*trace* #(swap! events conj %)]
      (let [d (t/check-top 0 (s/parse-term [] form))
            v (#'ev/ev (ev/erase d) [] n {:erase? true})]
        [(reduce (fn [acc a] (acc a)) v args) @events]))))

(defn- sigma-code [x] (code-of (list 'pair f/sigma x 'star)))

(deftest the-all-policy-admits-the-higher-order-shapes
  (doseq [[nm ty] [[:a f/Fa-type] [:b f/Fb-type] [:c f/Fc-type]]]
    (testing (str "shape " nm)
      (binding [s/*reflect-class* :all]
        (is (s/reflectable-type? (s/parse-type [] ty))))
      (binding [s/*reflect-class* :first-order]
        (is (not (s/reflectable-type? (s/parse-type [] ty))))))))

(deftest the-shapes-return-certified-results
  (testing "(a) a third-order argument: the certified pair (1, ⋆)"
    (let [[[cf w] total] (certs [(code-of f/Fa) (sigma-code 1)])]
      (is (= [:pv 1 :star] (first (run-traced (dec total) f/driver-a cf w))))))
  (testing "(b) two consumers in a tensor: 1 + 2 = 3"
    (let [[[cf w1 w2] total] (certs [(code-of f/Fb) (sigma-code 1) (sigma-code 2)])]
      (is (= 3 (first (run-traced (dec total) f/driver-b cf w1 w2))))))
  (testing "(c) a reusable consumer, used twice: 1 + 2 = 3"
    (let [[[cf w1 w2] total] (certs [(code-of f/Fc) (sigma-code 1) (sigma-code 2)])]
      (is (= 3 (first (run-traced (dec total) f/driver-c cf w1 w2)))))))

(deftest closures-keep-their-creation-cap
  (testing "shape (c): the consumer is built inside the reflected program, at the charged
            cap n′, and called later by the driver, which runs at n.  Its own reflections
            run at n′ — the cap it was created under — not at n."
    (let [[[cf w1 w2] total] (certs [(code-of f/Fc) (sigma-code 1) (sigma-code 2)])
          n (dec total)
          [result events] (run-traced n f/driver-c cf w1 w2)
          reflects (filterv #(and (= :reflect (:event %)) (:runs %)) events)
          [outer & inner] reflects
          n′ (:decoded-cap outer)]
      (is (= 3 result))
      (is (= n (:cap outer)) "Fc is reflected by the driver, at the driver's cap")
      (is (< n′ n))
      (is (= 2 (count inner)) "the consumer reflects each of the two certificates it is handed")
      (doseq [e inner]
        (is (= n′ (:cap e)) "at the cap the consumer closed over")
        (is (= 1 (:depth e)) "one reflection deep: inside Fc's program")))))
