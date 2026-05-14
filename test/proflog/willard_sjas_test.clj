(ns proflog.willard-sjas-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.normalize :as normalize]
            [proflog.proof :as proof]
            [proflog.query :as query]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- successful?
  [proofs]
  (boolean (seq proofs)))

(defn- first-proof
  [proofs]
  (first proofs))

(defn- n
  [value]
  (sjas/numeral value))

(defn- sjas-numeral-term?
  "True when `term` is written in the public binary SJAS numeral vocabulary."
  [term]
  (and (= 'app (ast/tag-of term))
       (let [head (second term)
             args (nnext term)]
         (cond
           (= (symbol "0") head) (empty? args)
           (= (symbol "1") head) (empty? args)
           (= 'dbl head) (and (= 1 (count args))
                              (sjas-numeral-term? (first args)))
           (= 'add head) (and (= 2 (count args))
                              (sjas-numeral-term? (first args))
                              (sjas-numeral-term? (second args)))
           :else false))))

(defn- generated-code-symbol?
  [sym]
  (and (symbol? sym)
       (or (str/starts-with? (name sym) "sjas_formula_")
           (str/starts-with? (name sym) "sjas_system_"))))

(defn- binding-for
  [records nom]
  (some (fn [record]
          (some (fn [[record-nom value]]
                  (when (= nom record-nom)
                    value))
                (:bindings record)))
        records))

(deftest sjas-profile-languages-have-binary-u-grounding-shape
  (testing "SJAS languages expose Willard-style binary U-grounding symbols"
    (doseq [[profile lang] [[:willard-sjas-tableau0 sjas/tableau0-profile-language]
                            [:willard-sjas-level1 sjas/level1-profile-language]]]
      (is (= profile (:proof-profile lang)))
      (is (contains? (:constants lang) (symbol "0")))
      (is (contains? (:constants lang) (symbol "1")))
      (is (not (contains? (:constants lang) 'zero)))
      (is (not (contains? (:constants lang) 'one)))
      (is (not (contains? (:constants lang) 'two)))
      (is (= 2 (get-in lang [:functions 'add])))
      (is (= 1 (get-in lang [:functions 'dbl])))
      (is (= 1 (get-in lang [:functions 'pred])))
      (is (= 2 (get-in lang [:functions 'sub])))
      (is (= 2 (get-in lang [:functions 'div])))
      (is (= 2 (get-in lang [:functions 'max])))
      (is (= 1 (get-in lang [:functions 'log])))
      (is (= 2 (get-in lang [:functions 'root])))
      (is (= 2 (get-in lang [:functions 'count])))
      (is (nil? (get-in lang [:functions 'mul]))
          "multiplication must be a graph relation, not a function symbol")
      (is (= 3 (get-in lang [:relations 'mult]))))))

(deftest sjas-numerals-are-binary-composed-terms
  (testing "only 0 and 1 are object-language numeral constants"
    (is (= (ast/app-term (symbol "0")) sjas/zero))
    (is (= (ast/app-term (symbol "1")) sjas/one))
    (is (= (sjas/dbl-term sjas/one) sjas/two))
    (is (= (sjas/add-term (sjas/dbl-term sjas/one) sjas/one) sjas/three))
    (is (= (sjas/dbl-term sjas/two) sjas/four))
    (is (= (sjas/add-term (sjas/dbl-term sjas/two) sjas/one) (n 5)))
    (is (= (sjas/dbl-term sjas/three) sjas/six))))

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

(defn- target-for-theorem
  [system formula]
  (normalize/negate-formula (sjas/theorem-query system formula)))

(deftest sjas-system-builder-generates-groups-and-reflected-boundary
  (testing "users supply beta/program clauses; the builder supplies codes and Group-3"
    (let [system (demo-system :willard-sjas-tableau0)]
      (is (= :willard-sjas-tableau0 (:profile system)))
      (is (= #{:group-zero :group-one :group-two :group-two-b :group-three}
             (set (map :group (:axioms system)))))
      (is (:system-code system))
      (is (= :group-three (-> system :group-three :group)))
      (is (some #(= (:code (:group-three system)) (:code %)) (:axioms system)))
      (let [beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))]
        (is (successful?
              (query/query-succeeds
                (:program system)
                (sjas/wff (:code beta-record))
                1
                96)))
        (is (successful?
              (query/query-succeeds
                (:program system)
                (sjas/neg-pair
                  (:code beta-record)
                  (sjas/formula-code system
                                     (normalize/negate-formula (:formula beta-record))))
                1
                128))
            "Level-1 complement relations must decode Godel-code terms"))
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

(deftest sjas-formal-codes-are-godel-byte-terms
  (testing "formal SJAS codes are inspectable base-64 Godel terms, not hash labels"
    (let [system (demo-system :willard-sjas-tableau0)]
      (is (sjas-code/code-term? (:system-code system)))
      (doseq [{:keys [code]} (:axioms system)]
        (is (sjas-code/code-term? code)
            (str "axiom code is not an SJAS Godel-code term: " (pr-str code))))
      (is (empty? (filter generated-code-symbol?
                          (get-in system [:language :constants])))
          "hash-derived code labels must not be formal language constants")
      (is (nil? (get-in system [:program :sjas/proof-targets]))
          "tableau-proof must decode theorem/system code terms, not use host target labels"))))

(deftest sjas-syntax-predicates-decode-formula-godel-codes
  (testing "wff, class predicates, and neg-pair are derived from formula Godel codes"
    (let [system (demo-system :willard-sjas-level1)
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          complement-code (sjas/formula-code
                            system
                            (normalize/negate-formula (:formula beta-record)))
          fact-atoms (get-in system [:program :sjas/fact-atoms])]
      (is (sjas-code/code-term? complement-code))
      (is (not= (sjas/not-code (:code beta-record)) complement-code)
          "complements must be formula Godel-code terms, not not-code wrappers")
      (is (not-any? (fn [atom]
                      (contains? '#{wff delta-star-0-code
                                    pi-star-1-code sigma-star-1-code}
                                 (second atom)))
                    fact-atoms)
          "syntax predicates must not be generated whole-formula facts")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/wff (:code beta-record))
              1
              96)))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/delta-star-0-code (:code beta-record))
              1
              96)))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/neg-pair (:code beta-record) complement-code)
              1
              128))))))

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
                    (= 1 1))
                 (reflected
                   (|- (demo x)
                       (= x 1)))
                 (external
                   (|- (external-demo x)
                       (= x 0))))]
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
              (ast/pos-lit (ast/app-term 'demo sjas/one))
              1
              96))
          "reflected user clauses should remain executable procedure clauses")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (ast/pos-lit (ast/app-term 'external-demo sjas/zero))
              1
              96))
          "external user clauses should be queryable outside the reflected basis")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/axiom-member (:system-code system)
                                  (:code (:group-three system)))
              1
              64))))))

(deftest sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures
  (testing "a beta-only composite axiom can prove a theorem without defining an executable relation"
    (let [system (sjas/system-source
                   {:profile :willard-sjas-tableau0}
                   (language
                     (relations (composite 1)))
                   (beta
                     (forall [x]
                       (implies
                         (mult (dbl 1) (dbl 1) x)
                         (composite x)))))
          composite-four (ast/pos-lit (ast/app-term 'composite (n 4)))]
      (is (= {:group-zero 2
              :group-one 3
              :group-two 1
              :group-three 1}
             (frequencies (map :group (:axioms system)))))
      (is (successful?
            (sjas/query-succeeds
              system
              composite-four
              {:proof-limit 1
               :fuel 64})))
      (is (empty?
            (query/query-succeeds
              (:program system)
              composite-four
              1
              64))
          "Group-2 formulas are axiom text, not Procedure Call Rule clauses")))
  (testing "a reflected composite clause is executable and also becomes Group-2b"
    (let [system (sjas/system-source
                   {:profile :willard-sjas-tableau0}
                   (language
                     (relations (composite 1)))
                   (reflected
                     (|- (composite x)
                         (mult (dbl 1) (dbl 1) x))))
          composite-four (ast/pos-lit (ast/app-term 'composite (n 4)))]
      (is (= {:group-zero 2
              :group-one 3
              :group-two-b 1
              :group-three 1}
             (frequencies (map :group (:axioms system)))))
      (is (successful?
            (query/query-succeeds
              (:program system)
              composite-four
              1
              64)))
      (is (successful?
            (sjas/query-succeeds
              system
              composite-four
              {:proof-limit 1
               :fuel 64})))
      (ast/nom x
        (let [records (sjas/query-answers
                        system
                        (ast/pos-lit (ast/app-term 'composite (ast/var-term x)))
                        [x]
                        {:proof-limit 1
                         :fuel 64})]
          (is (= (n 4) (binding-for records x))))))))

(deftest sjas-arithmetic-runs-through-binary-relations
  (let [system (demo-system :willard-sjas-tableau0)
        program (:program system)]
    (testing "closed U-grounding function equations are proved by the SJAS profile"
      (doseq [formula [(ast/eq-lit (sjas/add-term (n 2) (n 3)) (n 5))
                       (ast/eq-lit (sjas/dbl-term (n 6)) (n 12))
                       (ast/eq-lit (sjas/pred-term (n 0)) (n 0))
                       (ast/eq-lit (sjas/pred-term (n 5)) (n 4))
                       (ast/eq-lit (sjas/sub-term (n 2) (n 5)) (n 0))
                       (ast/eq-lit (sjas/sub-term (n 7) (n 3)) (n 4))
                       (ast/eq-lit (sjas/div-term (n 7) (n 0)) (n 7))
                       (ast/eq-lit (sjas/div-term (n 7) (n 3)) (n 2))
                       (ast/eq-lit (sjas/max-term (n 4) (n 9)) (n 9))
                       (ast/eq-lit (sjas/log-term (n 1)) (n 0))
                       (ast/eq-lit (sjas/log-term (n 8)) (n 3))
                       (ast/eq-lit (sjas/root-term (n 10) (n 2)) (n 4))
                       (ast/eq-lit (sjas/root-term (n 8) (n 3)) (n 2))
                       (ast/eq-lit (sjas/count-term (n 13) (n 4)) (n 3))]]
        (is (successful?
              (query/query-succeeds program formula 1 160))
            (pr-str formula))))
    (testing "closed arithmetic relation facts are profile relations, not finite facts"
      (is (successful?
            (query/query-succeeds program
                                  (sjas/mult (n 4) (n 3) (n 12))
                                  1
                                  160)))
      (is (successful?
            (query/query-succeeds program
                                  (sjas/leq (n 13) (n 13))
                                  1
                                  80)))
      (is (successful?
            (query/query-succeeds program
                                  (sjas/lt (n 13) (n 14))
                                  1
                                  80)))
      (is (empty?
            (query/query-succeeds program
                                  (sjas/mult (n 4) (n 3) (n 11))
                                  1
                                  80)))
      (is (empty?
            (query/query-succeeds program
                                  (ast/eq-lit (sjas/add-term (n 2) (n 3)) (n 6))
                                  1
                                  80))))))

(deftest sjas-arithmetic-supports-answer-and-partial-synthesis-modes
  (let [system (demo-system :willard-sjas-tableau0)]
    (ast/nom x y z
      (testing "answer mode synthesizes missing multiplicands"
        (let [left-records (sjas/query-answers
                             system
                             (sjas/mult (ast/var-term x) (n 3) (n 12))
                             [x]
                             {:proof-limit 1
                              :fuel 160})
              right-records (sjas/query-answers
                              system
                              (sjas/mult (n 4) (ast/var-term y) (n 12))
                              [y]
                              {:proof-limit 1
                               :fuel 160})]
          (is (= (n 4) (binding-for left-records x)))
          (is (= (n 3) (binding-for right-records y)))))
      (testing "partial synthesis solves an arithmetic function equation"
        (let [records (sjas/query-answers
                        system
                        (ast/eq-lit (sjas/add-term (ast/var-term z) (n 3)) (n 7))
                        [z]
                        {:proof-limit 1
                         :fuel 160})]
          (is (= (n 4) (binding-for records z))))))))

(deftest sjas-tableau-proof-checks-kernel-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        beta-proof (first-proof
                     (sjas/query-succeeds system (:formula beta-record)
                                          {:proof-limit 1
                                           :fuel 96}))
        valid (sjas/proof-certificate beta-proof)
        malformed (sjas/proof-certificate '(refl-close))]
    (is beta-proof)
    (is (sjas-code/code-term? valid)
        "proof certificates must be base-64 Godel-code terms")
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system) (:code beta-record) valid)
            1
            160)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                (:code (:group-three system))
                                valid)
            1
            80)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system) (:code beta-record) malformed)
            1
            80)))))

(deftest sjas-selfcons-demonstration-uses-substantive-proof-targets
  (testing "the generated self-consistency axiom is a theorem with a checked certificate"
    (let [system (demo-system :willard-sjas-tableau0)
          group3-proof (first-proof
                         (sjas/query-succeeds system
                                              (:formula (:group-three system))
                                              {:proof-limit 1
                                               :fuel 96}))
          group3-certificate (when group3-proof
                               (sjas/proof-certificate group3-proof))]
      (is group3-proof)
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  (:code (:group-three system))
                                  group3-certificate)
              1
              160)))))
  (testing "an explicitly inconsistent reflected basis can prove the real contradiction target"
    (let [system (sjas/system {:profile :willard-sjas-tableau0
                               :beta [(ast/false-form)]})
          contradiction-proof (first-proof
                                (sjas/query-succeeds system
                                                     (ast/false-form)
                                                     {:proof-limit 1
                                                      :fuel 96}))
          contradiction-certificate (when contradiction-proof
                                      (sjas/proof-certificate contradiction-proof))]
      (is contradiction-proof)
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  sjas/contradiction-code
                                  contradiction-certificate)
              1
              160))))))

(deftest sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile
  (doseq [profile [:willard-sjas-tableau0 :willard-sjas-level1]]
    (let [system (demo-system profile)
          beta-proof (first-proof
                       (query/query-succeeds (:program system)
                                             (demo-beta)
                                             1
                                             64))
          selfcons-proof (first-proof
                           (query/query-succeeds
                             (:program system)
                             (sjas/axiom-member (:system-code system)
                                                 (:code (:group-three system)))
                             1
                             64))]
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
  (let [profile-source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        builder-source (slurp "src/proflog/willard_sjas.clj")]
    (is (not (re-find #"prove-program-host" profile-source)))
    (is (not (re-find #"host-proof" profile-source)))
    (is (not (re-find #"whole-formula" profile-source)))
    (is (not (re-find #"mini-closed" profile-source)))
    (is (not (re-find #"mini-closed" builder-source)))
    (is (not (re-find #"malformed" profile-source)))
    (is (not (re-find #"malformed" builder-source)))
    (is (not (re-find #"defn- mult-facts" builder-source)))
    (is (not (re-find #"defn- order-facts" builder-source)))))
