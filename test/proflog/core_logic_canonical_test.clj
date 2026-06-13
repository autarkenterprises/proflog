(ns proflog.core-logic-canonical-test
  "ADR-0093 canonical regression coverage for the vendored core.logic engine.

   The namespace is intentionally below Proflog's proof kernel. It checks the
   public miniKanren/core.logic surface that ADR-0090's ground-term fast path
   must preserve: core unification and search, list relations, cKanren-style
   constraints, alphaKanren nominal terms, tabling, finite-domain constraints,
   and a small performance canary for repeated ground walks."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as l
             :refer [!= == conde fresh lcons run run*]]
            [clojure.core.logic.fd :as fd]
            [clojure.core.logic.nominal :as nominal]
            [clojure.core.logic.protocols :as lp]
            [clojure.test :refer [deftest is testing]]
            [proflog.minikanren-constraints :as mkc]))

(def ^:private ground-key
  "ADR-0090's metadata marker. Tests only use it to confirm the optimization is
   actually exercised; all semantic assertions use public relational behavior."
  :clojure.core.logic/ground)

(defn- tagged-ground?
  [v]
  (and (instance? clojure.lang.IMeta v)
       (true? (ground-key (meta v)))))

(defn- residual-answer?
  [answer]
  (and (seq? answer)
       (some #{':-} answer)))

(defn- elapsed-ms
  "Measure a small deterministic canary without printing timing noise into the
   test log. The threshold in the test is deliberately broad enough for ordinary
   laptop variance but narrow enough to catch losing the tagged fixed point."
  [f]
  (let [start (System/nanoTime)
        result (f)
        stop (System/nanoTime)]
    {:result result
     :elapsed-ms (/ (double (- stop start)) 1000000.0)}))

(declare nato)

(defn nato
  "Canonical miniKanren natural numbers: z, (s z), (s (s z)), ..."
  [n]
  (conde
    [(== 'z n)]
    [(fresh [p]
       (nato p)
       (== (list 's p) n))]))

(defn alwayso
  "A productive infinite goal used to check that conde interleaves branches."
  []
  (conde
    [(== true true)]
    [(alwayso)]))

(defn parento
  [x y]
  (conde
    [(== :alice x) (== :bob y)]
    [(== :alice x) (== :drew y)]
    [(== :bob x) (== :cora y)]))

(declare ancestoro)

(def ancestoro
  (l/tabled [x y]
    (conde
      [(parento x y)]
      [(fresh [z]
         (parento x z)
         (ancestoro z y))])))

(def shapeo
  "A tabled lookup whose keys are ground Clojure trees. Tagged metadata must not
   change equality, hashing, table lookup, or answer reification."
  (l/tabled [x out]
    (conde
      [(== (list :shape [1 2 3]) x) (== :first out)]
      [(== (list :shape [1 2 4]) x) (== :second out)])))

(declare tiny-evalo)

(defn not-in-envo
  "Association-list absence relation used by the tiny literature-derived
   interpreter. It keeps special forms from being treated as variables when
   they are not shadowed by the interpreted environment."
  [x env]
  (conde
    [(== '() env)]
    [(fresh [y v rest]
       (== (lcons (lcons y v) rest) env)
       (!= y x)
       (not-in-envo x rest))]))

(defn lookupo
  [x env value]
  (fresh [rest y v]
    (== (lcons (lcons y v) rest) env)
    (conde
      [(== y x) (== v value)]
      [(!= y x) (lookupo x rest value)])))

(defn tiny-evalo
  "A deliberately small relational interpreter adapted from the quine
   interpreter pattern in Byrd/Holk/Friedman, \"miniKanren, Live and
   Untagged\". The language has variables, quote, two-argument list, unary
   lambda, and application, which is enough for the canonical quine example."
  [expr env value]
  (conde
    [(fresh [datum]
       (== (list 'quote datum) expr)
       (not-in-envo 'quote env)
       (== datum value))]
    [(fresh [left-expr right-expr left-val right-val]
       (== (list 'list left-expr right-expr) expr)
       (not-in-envo 'list env)
       (tiny-evalo left-expr env left-val)
       (tiny-evalo right-expr env right-val)
       (== (list left-val right-val) value))]
    [(fresh [x body]
       (== (list 'lambda (list x) body) expr)
       (mkc/symbolo x)
       (not-in-envo 'lambda env)
       (== (list 'closure x body env) value))]
    [(fresh [rator rand x body saved-env arg]
       (== (list rator rand) expr)
       (tiny-evalo rator env (list 'closure x body saved-env))
       (tiny-evalo rand env arg)
       (tiny-evalo body (lcons (lcons x arg) saved-env) value))]
    [(mkc/symbolo expr)
     (lookupo expr env value)]))

(def quine-program
  "The standard self-quoting Scheme quine from the miniKanren quine literature,
   represented as Clojure data for the tiny interpreter:
   ((lambda (x) (list x (list 'quote x)))
    '(lambda (x) (list x (list 'quote x))))"
  (let [body (list 'list 'x (list 'list (list 'quote 'quote) 'x))
        fn-expr (list 'lambda '(x) body)]
    (list fn-expr (list 'quote fn-expr))))

(defn quine-skeletono
  "A relational version of the same quine shape. Leaving the binder symbolic
   mirrors the literature's quine query output, where the variable is reified
   with residual symbol/disequality constraints."
  [x program]
  (fresh [body fn-expr]
    (mkc/symbolo x)
    (!= x 'closure)
    (!= x 'lambda)
    (!= x 'list)
    (!= x 'quote)
    (== (list 'list x (list 'list (list 'quote 'quote) x)) body)
    (== (list 'lambda (list x) body) fn-expr)
    (== (list fn-expr (list 'quote fn-expr)) program)))

(deftest core-unification-reification-and-occurs-check
  (testing "unification orientation and repeated ground bindings are unchanged"
    (is (= '((1 2 3))
           (run* [q]
             (fresh [x y]
               (== x (list 1 2 3))
               (== y x)
               (== q y))))))
  (testing "the sound occurs check still rejects direct and indirect cycles"
    (is (= '()
           (run* [q]
             (== q (list :self q)))))
    (is (= '()
           (run* [q]
             (fresh [x y]
               (== x (lcons :head y))
               (== y x)
               (== q :cycle)))))))
  (testing "reification preserves distinct fresh variables in stable order"
    (let [[answer] (run 1 [q]
                     (fresh [x y]
                       (== q [x y])))]
      (is (vector? answer))
      (is (= 2 (count answer)))
      (is (every? symbol? answer))
      (is (not= (first answer) (second answer)))))
  (testing "project observes the walked value, including tagged ground vectors"
    (is (= '([1 2 3 4])
           (run* [q]
             (fresh [x]
               (== x [1 2 3])
               (l/project [x]
                 (== q (conj x 4))))))))
  (testing "onceo commits to the first result of its goal"
    (is (= '(:a)
           (run* [q]
             (l/onceo
               (conde
                 [(== q :a)]
                 [(== q :b)]))))))

(deftest fair-search-and-list-relations
  (testing "recursive natural generation follows the expected prefix"
    (is (= '(z (s z) (s (s z)) (s (s (s z))))
           (run 4 [q]
             (nato q)))))
  (testing "conde interleaves a productive infinite branch with later branches"
    (let [answers (vec
                    (run 4 [q]
                      (conde
                        [(alwayso) (== q :left)]
                        [(== q :right)])))]
      (is (= 4 (count answers)))
      (is (some #{:right} answers)
          "the finite later branch must not be starved by the infinite branch")
      (is (every? #{:left :right} answers))))
  (testing "appendo works forward and backward over proper lists"
    (is (= '((:a :b :c))
           (run* [q]
             (l/appendo '(:a :b) '(:c) q))))
    (is (= [['() '(:a :b)]
            ['(:a) '(:b)]
            ['(:a :b) '()]]
           (vec
             (run* [q]
               (fresh [x y]
                 (l/appendo x y '(:a :b))
                 (== [x y] q)))))))
  (testing "membero keeps duplicate answers while member1o prunes repeats"
    (is (= '(:a :b :a)
           (run* [q]
             (l/membero q '(:a :b :a)))))
    (is (= '(:a :b)
           (run* [q]
             (l/member1o q '(:a :b :a))))))
  (testing "improper lcons tails remain inspectable after walking"
    (let [[answer] (run 1 [q]
                     (fresh [tail]
                       (== tail :tail)
                       (== (lcons :head tail) q)))]
      (is (l/lcons? answer))
      (is (= :head (lp/lfirst answer)))
      (is (= :tail (lp/lnext answer))))))

(deftest literature-derived-classic-programs
  (testing "core.logic rembero matches the classic remove-one relation"
    (is (= '((:b :a :c))
           (run* [q]
             (l/rembero :a '(:a :b :a :c) q))))
    (is (= '((:b :c))
           (run* [q]
             (l/rembero :a '(:b :a :c) q)))))
  (testing "the tiny relational interpreter evaluates the standard quine"
    (is (= (list quine-program)
           (run 1 [q]
             (tiny-evalo quine-program '() q))))
    (is (= (list quine-program)
           (run 1 [q]
             (== q quine-program)
             (tiny-evalo q '() q)))))
  (testing "the quine shape also works with a synthesized binder"
    (let [[answer] (run 1 [q]
                     (fresh [x]
                       (quine-skeletono x q)
                       (tiny-evalo q '() q)))]
      (is (residual-answer? answer))
      (is (some #{'symbolo} (flatten answer)))
      (is (some #{'!=} (flatten answer))))))

(deftest constraints-remain-live-through-ground-walks
  (testing "disequality still rejects, accepts, and reifies residuals"
    (is (= '()
           (run* [q]
             (!= q [:a :b])
             (== q [:a :b]))))
    (is (= '([:a :c])
           (run* [q]
             (!= q [:a :b])
             (== q [:a :c]))))
    (let [[answer] (run 1 [q]
                     (!= q [:a :b]))]
      (is (residual-answer? answer))
      (is (some #{'!=} (flatten answer)))))
  (testing "symbolo and numbero delay and later refine correctly"
    (is (= '(token)
           (run* [q]
             (mkc/symbolo q)
             (== q 'token))))
    (is (= '()
           (run* [q]
             (mkc/symbolo q)
             (== q 7))))
    (is (= '(7)
           (run* [q]
             (mkc/numbero q)
             (== q 7))))
    (is (= '()
           (run* [q]
             (mkc/numbero q)
             (== q 'token)))))
  (testing "absento traverses tagged-safe structures and remains delayed on open tails"
    (is (= '(:ok)
           (run* [q]
             (mkc/absento :forbidden [:safe (list :also-safe [1 2 3])])
             (== q :ok))))
    (is (= '()
           (run* [q]
             (mkc/absento :forbidden [:safe {:nested :forbidden}])
             (== q :bad))))
    (let [[answer] (run 1 [q]
                     (fresh [tail]
                       (mkc/absento :forbidden q)
                       (== (lcons :safe tail) q)))]
      (is (residual-answer? answer))
      (is (some #{'absento} (flatten answer)))))
    (is (= '()
           (run* [q]
             (fresh [tail]
               (mkc/absento :forbidden q)
               (== (lcons :safe tail) q)
               (== (lcons :forbidden '()) tail))))))

(deftest nominal-freshness-and-binding-terms
  (testing "hash rejects a nom that appears free but allows the same nom bound by tie"
    (is (= '()
           (run* [q]
             (nominal/fresh [a]
               (nominal/hash a [a])
               (== q :bad)))))
    (is (= '(:ok)
           (run* [q]
             (nominal/fresh [a]
               (nominal/hash a (nominal/tie a [:body [1 2 3]]))
               (== q :ok))))))
  (testing "nominal equality and delayed hash constraints survive later aliasing"
    (is (= '()
           (run* [q]
             (nominal/fresh [wanted]
               (fresh [key skipped]
                 (nominal/hash key skipped)
                 (== key skipped)
                 (== skipped wanted)
                 (== q true)))))))
  (testing "nominal tie records are not treated as ADR-0090 ground trees"
    (let [tie (nominal/tie :binder [:body [1 2 3]])
          walked (l/walk* l/empty-s tie)]
      (is (= tie walked))
      (is (nominal/tie? walked))
      (is (not (tagged-ground? walked))))))

(deftest tabling-preserves-answers-and-ground-keys
  (testing "tabled recursive answers are reused without changing the answer set"
    (is (= #{:bob :drew :cora}
           (set
             (run* [q]
               (ancestoro :alice q)))))
    (is (= '(:ok)
           (run 1 [q]
             (ancestoro :alice :cora)
             (ancestoro :alice :cora)
             (== q :ok)))))
  (testing "ground metadata does not perturb tabled key equality"
    (let [tagged (l/walk* l/empty-s (list :shape [1 2 3]))]
      (is (tagged-ground? tagged))
      (is (= '(:first)
             (run* [q]
               (shapeo tagged q)))))))

(deftest finite-domain-constraints-and-equation-sugar
  (testing "interval arithmetic enumerates the expected ordered pairs"
    (is (= #{[1 4] [2 3]}
           (set
             (run* [q]
               (fresh [x y]
                 (fd/in x y (fd/interval 1 4))
                 (fd/< x y)
                 (fd/+ x y 5)
                 (== [x y] q)))))))
  (testing "domain membership, disequality, and distinctness compose"
    (is (= '(2 6)
           (run* [q]
             (fd/in q (fd/domain 2 4 6))
             (fd/!= q 4))))
    (let [answers (run* [q]
                    (fresh [x y z]
                      (fd/in x y z (fd/interval 1 3))
                      (fd/distinct [x y z])
                      (== [x y z] q)))]
      (is (= 6 (count answers)))
      (is (= #{[1 2 3] [1 3 2] [2 1 3] [2 3 1] [3 1 2] [3 2 1]}
             (set answers)))))
  (testing "fd/eq expands arithmetic syntax to the same small solution set"
    (is (= #{[1 3] [3 2] [5 1]}
           (set
             (run* [q]
               (fresh [x y]
                 (fd/in x y (fd/interval 0 5))
                 (fd/eq (= (+ x (* 2 y)) 7))
                 (== [x y] q))))))))

(deftest ^{:performance true
           :expected-duration-ms 1500}
  tagged-ground-walk-performance-canary
  (testing "a tagged finite ground sequence is a repeated-walk fixed point"
    (let [term (doall (range 4000))
          walked (l/walk* l/empty-s term)
          {:keys [elapsed-ms result]}
          (elapsed-ms
            (fn []
              (dotimes [_ 20000]
                (when-not (identical? walked (l/walk* l/empty-s walked))
                  (throw (ex-info "Tagged ground walk lost identity"
                                  {:walked walked}))))
              :ok))]
      (is (= term walked))
      (is (tagged-ground? walked))
      (is (= :ok result))
      (is (< elapsed-ms 1500)
          (str "Expected repeated tagged walks under 1500ms, got "
               elapsed-ms "ms")))))
