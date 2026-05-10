(ns proflog.willard-sjas-test
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.proof :as proof]
            [proflog.query :as query]
            [proflog.willard-sjas :as sjas]))

(defn- successful?
  [proofs]
  (boolean (seq proofs)))

(defn- first-proof
  [proofs]
  (first proofs))

(deftest sjas-profile-languages-have-u-grounding-shape
  (testing "MVP languages expose Willard-style U-grounding symbols"
    (doseq [[profile lang] [[:willard-sjas-tableau0 sjas/tableau0-profile-language]
                            [:willard-sjas-level1 sjas/level1-profile-language]]]
      (is (= profile (:proof-profile lang)))
      (is (= 2 (get-in lang [:functions 'add])))
      (is (= 1 (get-in lang [:functions 'dbl])))
      (is (= 2 (get-in lang [:functions 'sub])))
      (is (= 2 (get-in lang [:functions 'div])))
      (is (= 2 (get-in lang [:functions 'root])))
      (is (= 2 (get-in lang [:functions 'count])))
      (is (nil? (get-in lang [:functions 'mul]))
          "multiplication must be a graph relation, not a function symbol")
      (is (= 3 (get-in lang [:relations 'mult]))))))

(deftest sjas-formula-classifiers-cover-bounded-and-unbounded-shapes
  (testing "bounded quantifiers stay visible to the SJAS classifier"
    (ast/nom x y
      (let [x-term (ast/var-term x)
            y-term (ast/var-term y)
            delta (sjas/bounded-forall x sjas/two
                    (sjas/lt x-term sjas/three))
            nested-delta (sjas/bounded-exists y sjas/three
                           (ast/and-form
                             (sjas/leq y-term sjas/three)
                             (sjas/mult y-term sjas/two sjas/four)))
            pi (ast/forall-form x delta)
            sigma (ast/exists-form x nested-delta)
            not-pi (ast/forall-form x
                     (ast/exists-form y
                       (sjas/lt y-term x-term)))]
        (is (sjas/delta-star-0? delta))
        (is (sjas/delta-star-0? nested-delta))
        (is (sjas/pi-star-1? pi))
        (is (sjas/sigma-star-1? sigma))
        (is (not (sjas/delta-star-0? (ast/exists-form y (sjas/lt y-term x-term)))))
        (is (not (sjas/pi-star-1? not-pi)))))))

(defn- demo-beta
  []
  (ast/eq-lit sjas/one sjas/one))

(defn- reflected-demo-clause
  []
  (ast/nom x
    (ast/clause 'demo [x]
                (ast/eq-lit (ast/var-term x) sjas/one))))

(defn- external-demo-clause
  []
  (ast/nom x
    (ast/clause 'external-demo [x]
                (ast/eq-lit (ast/var-term x) sjas/zero))))

(defn- demo-system
  [profile]
  (sjas/system
    {:profile profile
     :relations {'demo 1
                 'external-demo 1}
     :beta [(demo-beta)]
     :reflected-clauses [(reflected-demo-clause)]
     :external-clauses [(external-demo-clause)]}))

(deftest sjas-system-builder-generates-groups-and-reflected-boundary
  (testing "users supply beta/program clauses; the builder supplies codes and Group-3"
    (let [system (demo-system :willard-sjas-tableau0)]
      (is (= :willard-sjas-tableau0 (:profile system)))
      (is (= #{:group-zero :group-one :group-two :group-two-b :group-three}
             (set (map :group (:axioms system)))))
      (is (:system-code system))
      (is (= :group-three (-> system :group-three :group)))
      (is (some #(= (:code (:group-three system)) (:code %)) (:axioms system)))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/axiom-member (:system-code system)
                                  (:code (:group-three system)))
              1
              64)))))
  (testing "reflected changes alter Group-3; external-only changes do not"
    (let [base (demo-system :willard-sjas-tableau0)
          beta-changed (sjas/system
                         {:profile :willard-sjas-tableau0
                          :relations {'demo 1 'external-demo 1}
                          :beta [(ast/eq-lit sjas/zero sjas/zero)]
                          :reflected-clauses [(reflected-demo-clause)]
                          :external-clauses [(external-demo-clause)]})
          reflected-changed (sjas/system
                              {:profile :willard-sjas-tableau0
                               :relations {'demo 1 'external-demo 1}
                               :beta [(demo-beta)]
                               :reflected-clauses [(ast/nom x
                                                    (ast/clause 'demo [x]
                                                                (ast/eq-lit (ast/var-term x)
                                                                            sjas/two)))]
                               :external-clauses [(external-demo-clause)]})
          external-changed (sjas/system
                             {:profile :willard-sjas-tableau0
                              :relations {'demo 1 'external-demo 1}
                              :beta [(demo-beta)]
                              :reflected-clauses [(reflected-demo-clause)]
                              :external-clauses [(ast/nom x
                                                  (ast/clause 'external-demo [x]
                                                              (ast/eq-lit (ast/var-term x)
                                                                          sjas/one)))]})]
      (is (not= (:system-code base) (:system-code beta-changed)))
      (is (not= (-> base :group-three :code)
                (-> beta-changed :group-three :code)))
      (is (not= (:system-code base) (:system-code reflected-changed)))
      (is (= (:system-code base) (:system-code external-changed)))
      (is (= (-> base :group-three :code)
             (-> external-changed :group-three :code))))))

(deftest sjas-source-builder-accepts-prefix-program-sections
  (testing "source users do not need to hand-build backend AST clauses"
    (let [system (sjas/system-source
                   {:profile :willard-sjas-tableau0}
                   (language
                     (constants extra)
                     (functions (mark 1))
                     (relations (demo 1)
                                (external-demo 1)))
                   (beta
                     (= one one))
                   (reflected
                     (|- (demo x)
                         (= x one)))
                   (external
                     (|- (external-demo x)
                         (= x zero))))]
      (is (= :willard-sjas-tableau0 (:profile system)))
      (is (= 1 (get-in system [:language :functions 'mark])))
      (is (contains? (get-in system [:language :constants]) 'extra))
      (is (successful?
            (sjas/query-succeeds
              system
              (ast/eq-lit sjas/one sjas/one)
              {:proof-limit 1
               :fuel 64})))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/axiom-member (:system-code system)
                                  (:code (:group-three system)))
              1
              64))))))

(deftest sjas-arithmetic-and-mult-graph-run-through-the-compiled-program
  (let [system (demo-system :willard-sjas-tableau0)
        program (:program system)]
    (testing "a representative U-grounding function axiom proves from Group-1"
      (is (successful?
            (sjas/query-succeeds
              system
              (ast/eq-lit (sjas/add-term sjas/zero sjas/zero) sjas/zero)
              {:proof-limit 1
               :fuel 64}))))
    (testing "closed arithmetic facts prove through generated Proflog clauses"
      (is (successful?
            (query/query-succeeds program
                                  (sjas/mult sjas/two sjas/three sjas/six)
                                  1
                                  64))))
    (testing "answer mode synthesizes missing multiplicands"
      (ast/nom x y
         (let [left-records (answers/query-answers
                             program
                             (sjas/mult (ast/var-term x) sjas/two sjas/four)
                             [x]
                             {:proof-limit 1
                              :fuel 8})
              right-records (answers/query-answers
                              program
                              (sjas/mult sjas/two (ast/var-term y) sjas/four)
                              [y]
                              {:proof-limit 1
                               :fuel 8})]
          (is (some #(= sjas/two (-> % :bindings first second)) left-records))
          (is (some #(= sjas/two (-> % :bindings first second)) right-records)))))))

(deftest sjas-proof-certificates-are-relational-program-facts-not-host-checks
  (let [system (demo-system :willard-sjas-tableau0)
        code (:code (:group-three system))
        valid (sjas/mini-closed-certificate code)
        malformed (sjas/malformed-certificate code)]
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system) code valid)
            1
            64)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system) code malformed)
            1
            4)))))

(deftest sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile
  (doseq [profile [:willard-sjas-tableau0 :willard-sjas-level1]]
    (let [system (demo-system profile)
          beta-proof (first-proof
                       (sjas/query-succeeds system (demo-beta)
                                            {:proof-limit 1
                                             :fuel 64}))
          selfcons-proof (first-proof
                           (sjas/query-succeeds system
                                                (-> system :group-three :formula)
                                                {:proof-limit 1
                                                 :fuel 64}))]
      (is beta-proof)
      (is selfcons-proof)
      (is (proof/contains-step? beta-proof (symbol (name profile))))
      (is (proof/contains-step? selfcons-proof (symbol (name profile)))))))

(deftest sjas-level1-bounded-contradiction-probe-records-timing
  (let [system (demo-system :willard-sjas-level1)
        result (sjas/bounded-contradiction-probe system {:fuel 4
                                                         :proof-limit 1})]
    (is (= :not-found (:result result)))
    (is (= 4 (:fuel result)))
    (is (integer? (:duration-ms result)))
    (is (not (neg? (:duration-ms result))))))

(deftest sjas-profile-source-audit-rejects-host-proof-checker-route
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")]
    (is (not (re-find #"prove-program-host" source)))
    (is (not (re-find #"host-proof" source)))
    (is (not (re-find #"whole-formula" source)))))
