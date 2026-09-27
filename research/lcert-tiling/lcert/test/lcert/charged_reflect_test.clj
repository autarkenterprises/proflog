(ns lcert.charged-reflect-test
  "Step 1 of proflog ADR-0143: reflect at the first-order resource class 𝓕,
  run under the caller-charged cap n − ‖v‖ + m.  (Not part of jpt4/sjas.)

  The class, from METATHEORY.md §1:
    O  ordinary types: R-free, ◇-free, reflect-free;
    D  cap-free types: O, R, ◇, and Σ-types of D-types;
    𝓕  D, Π-types with a D input (any input at usage 0) and an 𝓕 result,
       Σ(x :ω D). 𝓕, Σ(x :₀ ·). 𝓕, and Σ(x :₁ ·). · with one side in D and
       the other in 𝓕.
  The Reflect case of the fundamental lemma holds for every closed type in
  𝓕 (Ansatz: lcert.charged/reflect_case_charged).  These tests check that
  the calculus implements exactly that class, and that the agent types of
  the delegation demo are in it while the three higher-order shapes are not."
  (:require [clojure.test :refer [deftest is testing]]
            [lcert.syntax :as s]
            [lcert.typing :as t]
            [lcert.eval :as ev]
            [lcert.core :as lc]
            [lcert.ho-forms :as f]))

(defn- ty [form] (s/parse-type [] form))

(def minting-agent-type
  "An agent that is handed a code and a supply: Π(cs :ω Syn). Π(s :₁ R). σ."
  (list 'Pi '[cs w Syn] f/agent-type))

(def observing-agent-type
  "An agent that is handed a supply and an observation: Π(s :₁ R). Π(i :ω Nat). σ."
  (list 'Pi '[s 1 R] (list 'Pi '[i w Nat] f/sigma)))

(deftest the-classes
  (testing "ordinary types are cap-free and first-order"
    (doseq [A [f/sigma '(-o Nat Bool) '(Pi [x w Nat] (T (rec-nat [z Bool] tt [k y] ff x)))]]
      (is (s/ordinary-type? (ty A)) (pr-str A))
      (is (s/capfree-type? (ty A)) (pr-str A))
      (is (s/first-order-type? (ty A)) (pr-str A))))
  (testing "certificates, tokens and pairs of them are cap-free, not ordinary"
    (doseq [A ['R 'Dia '(tensor R (tensor Dia Nat)) '(Sigma [x w Nat] R)]]
      (is (not (s/ordinary-type? (ty A))) (pr-str A))
      (is (s/capfree-type? (ty A)) (pr-str A))
      (is (s/first-order-type? (ty A)) (pr-str A))))
  (testing "the agent types are first-order, and not cap-free"
    (doseq [A [f/agent-type minting-agent-type observing-agent-type f/H]]
      (is (s/first-order-type? (ty A)) (pr-str A))
      (is (not (s/capfree-type? (ty A))) (pr-str A))))
  (testing "a first-order type may carry one consumer beside cap-free data"
    (is (s/first-order-type? (ty (list 'tensor f/H 'R))))
    (is (s/first-order-type? (ty (list 'tensor 'Nat f/agent-type)))))
  (testing "the three higher-order shapes are outside the class"
    (is (not (s/first-order-type? (ty f/Fa-type))) "(a) a consumer inside an input")
    (is (not (s/first-order-type? (ty f/Fb-type))) "(b) two consumers in one tensor")
    (is (not (s/first-order-type? (ty f/Fc-type))) "(c) a reusable consumer")))

(deftest the-reflect-rule-follows-the-class
  (testing "under the first-order policy, reflect at the agent type type-checks"
    (binding [s/*reflect-class* :first-order]
      (is (= (ty f/agent-type) (:type (lc/certify 0 f/agent-form))))))
  (testing "under the first-order policy, reflect at shape (a) is a type error"
    (binding [s/*reflect-class* :first-order]
      (is (thrown? clojure.lang.ExceptionInfo (lc/certify 0 f/driver-a)))))
  (testing "under the default (ordinary) policy, reflect at the agent type is a type error"
    (is (thrown? clojure.lang.ExceptionInfo (lc/certify 0 f/agent-form))))
  (testing "reflect at a type mentioning reflect is refused by every policy"
    (doseq [p [:ordinary :first-order :all]]
      (binding [s/*reflect-class* p]
        (is (not (s/reflectable-type?
                  (ty '(T (reflect Bool (leaf :a) star))))))))))

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

(defn- chain-supply
  "A supply for the agent: a spine whose left children are the certificates
  of `depth` agents and then of the leaf agent.  Returns [value tokens]."
  [depth]
  (let [code-of #(binding [s/*reflect-class* :first-order] (:code (lc/certify 0 %)))
        spine (reduce (fn [rest c] [:sn :a c rest]) [:sl :a]
                      (reverse (concat (repeat depth (code-of f/agent-form)) [(code-of f/leaf-agent)])))
        [v next] (cert-value spine 1)]
    [v (dec next)]))

(defn- run-agent
  "Run the agent on its supply under cap n (the supply's size), with the
  charged cap, recording every reflect event."
  [supply n]
  (let [events (atom [])]
    (binding [s/*reflect-class* :first-order
              ev/*charged-cap* true
              ev/*trace* #(swap! events conj %)]
      (let [d (t/check-top 0 (s/parse-term [] f/agent-form))
            agent (#'ev/ev (ev/erase d) [] n {:erase? true})]
        [(agent supply) @events]))))

(deftest charged-delegation-under-the-first-order-rule
  (doseq [depth [0 1 2]]
    (let [[supply n] (chain-supply depth)
          [result events] (run-agent supply n)
          reflects (filter #(and (= :reflect (:event %)) (:runs %)) events)]
      (testing (str "delegation through " depth " agent levels ends in the leaf agent's action 2")
        (is (= [:pv 2 :star] result))
        (is (= (inc depth) (count reflects))))
      (testing "every reflection runs its program at the charged cap n − ‖v‖ + m, strictly
                below its caller's cap and at least the program's own budget"
        (doseq [{:keys [cap nodes m decoded-cap]} reflects]
          (is (= decoded-cap (+ (- cap nodes) m)))
          (is (< decoded-cap cap))
          (is (<= m decoded-cap))))
      (testing "each level's cap is the previous level's charged cap"
        (is (= (map :decoded-cap (butlast reflects)) (map :cap (rest reflects))))))))
