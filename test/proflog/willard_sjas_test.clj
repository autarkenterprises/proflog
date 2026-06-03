(ns proflog.willard-sjas-test
  (:require [clojure.core.logic :as l]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.gamma :as gamma]
            [proflog.kernel :as kernel]
            [proflog.kernel.willard-sjas-profile :as sjas-profile]
            [proflog.language :as language]
            [proflog.normalize :as normalize]
            [proflog.proof :as proof]
            [proflog.query :as query]
            [proflog.sjas-correspondence :as correspondence]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(defn- successful?
  [proofs]
  (boolean (seq proofs)))

(defn- first-proof
  [proofs]
  (first proofs))

(defn- proof-symbol-audit
  [proof]
  (correspondence/audit-proof-term proof))

(defn- equality-triggered-atom-closure-proof
  "Certificate shape for saving complementary atoms, then closing them after an
   equality step makes their arguments identical."
  []
  (let [argument-proof (list 'args '(eq-refl) '())
        atom-proof (list 'atom-close argument-proof)
        equality-proof (list 'eq-step '(eq-bind) atom-proof)
        save-negative (list 'savefml equality-proof)
        negative-then-equality (list 'conj save-negative)
        save-positive (list 'savefml negative-then-equality)
        positive-then-rest (list 'conj save-positive)
        universal-proof (list 'once-univ positive-then-rest)]
    (list 'conj universal-proof)))

(defn- equality-triggered-positive-call-proof
  "Certificate shape for saving a positive atom, then calling it after an
   equality step makes its arguments object-language ground."
  []
  (list 'conj
        (list 'witness
              (list 'conj
                    (list 'savefml
                          (list 'eq-step
                                '(par-bind)
                                (list 'eq-triggered-call
                                      '(free-close))))))))

(defn- equality-triggered-negative-call-proof
  "Certificate shape for saving a negative atom, then calling its negated body
   after equality makes its arguments object-language ground."
  []
  (list 'conj
        (list 'witness
              (list 'conj
                    (list 'savefml
                          (list 'eq-step
                                '(par-bind)
                                (list 'eq-triggered-neg-call
                                      '(refl-close))))))))

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

(defn- code-constructor-symbol?
  [sym]
  (boolean (sjas-code/code-symbol-byte-count sym)))

(defn- binding-for
  [records nom]
  (some (fn [record]
          (some (fn [[record-nom value]]
                  (when (= nom record-nom)
                    value))
                (:bindings record)))
        records))

(defn- subst-code-relation-succeeds?
  [system source-code target-code]
  (successful?
    (l/run 1 [q]
      (l/fresh [sigma-out]
        ((var-get #'sjas-profile/sjas-subst-code-anyo)
         (:program system)
         source-code
         target-code
         '()
         sigma-out)
        (l/== true q)))))

(defn- formula-relation-symbols
  "Collect relation symbols appearing in atomic literals of an SJAS formula."
  [formula]
  (case (ast/tag-of formula)
    pos [(second (second formula))]
    neg [(second (second formula))]
    eq []
    neq []
    true []
    false []
    and (concat (formula-relation-symbols (second formula))
                (formula-relation-symbols (nth formula 2)))
    or (concat (formula-relation-symbols (second formula))
               (formula-relation-symbols (nth formula 2)))
    not (formula-relation-symbols (second formula))
    implies (concat (formula-relation-symbols (second formula))
                    (formula-relation-symbols (nth formula 2)))
    forall (formula-relation-symbols (:body (second formula)))
    once-forall (formula-relation-symbols (:body (second formula)))
    exists (formula-relation-symbols (:body (second formula)))
    bounded-forall (formula-relation-symbols (get-in (second formula) [:body :body]))
    bounded-exists (formula-relation-symbols (get-in (second formula) [:body :body]))
    []))

(defn- formula-atoms
  "Collect atomic application terms appearing in an SJAS formula."
  [formula]
  (case (ast/tag-of formula)
    pos [(second formula)]
    neg [(second formula)]
    eq []
    neq []
    true []
    false []
    and (concat (formula-atoms (second formula))
                (formula-atoms (nth formula 2)))
    or (concat (formula-atoms (second formula))
               (formula-atoms (nth formula 2)))
    not (formula-atoms (second formula))
    implies (concat (formula-atoms (second formula))
                    (formula-atoms (nth formula 2)))
    forall (formula-atoms (:body (second formula)))
    once-forall (formula-atoms (:body (second formula)))
    exists (formula-atoms (:body (second formula)))
    bounded-forall (formula-atoms (get-in (second formula) [:body :body]))
    bounded-exists (formula-atoms (get-in (second formula) [:body :body]))
    []))

(defn- encoded-relation-index
  "Return the formula-code relation index for `relation` in `system`.

   Private proof-checker tests intentionally feed decoded proof targets rather
   than public theorem codes. User relation symbols in those decoded targets
   should therefore use the same structural `(sym n)` identity that the
   arithmeticized theorem-code decoder produces, not the host source symbol."
  [system relation arity]
  (let [args (repeat arity sjas/zero)
        formula (ast/pos-lit (apply ast/app-term relation args))
        bytes (sjas-code/code-term-bytes (sjas/formula-code system formula))]
    (nth bytes 2)))

(defn- structural-app-term
  [system relation & args]
  (list* 'app
         (list 'sym (encoded-relation-index system relation (count args)))
         args))

(defn- structural-pos-lit
  [system relation & args]
  (ast/pos-lit (apply structural-app-term system relation args)))

(defn- structural-neg-lit
  [system relation & args]
  (ast/neg-lit (apply structural-app-term system relation args)))

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
      (is (= 3 (get-in lang [:relations 'mult])))
      (is (= 2 (get-in lang [:relations 'subst-code])))
      (is (= 4 (get-in lang [:relations 'subst-prf]))))))

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
  ([]
   (reflected-demo-clause 'demo))
  ([relation]
   (ast/nom x
     (ast/clause relation [x]
                 (ast/eq-lit (ast/var-term x) sjas/one)))))

(defn- external-demo-clause
  ([]
   (external-demo-clause 'external-demo))
  ([relation]
   (ast/nom x
     (ast/clause relation [x]
                 (ast/eq-lit (ast/var-term x) sjas/zero)))))

(defn- demo-system
  ([profile]
   (demo-system profile {}))
  ([profile opts]
   (sjas/system
     (merge
       {:profile profile
        :relations {'demo 1
                    'external-demo 1}
        :beta [(demo-beta)]
        :reflected-clauses [(reflected-demo-clause)]
        :external-clauses [(external-demo-clause)]}
       opts))))

(deftest reflected-call-header-match-and-nonmatch-are-explicit-relations
  (let [system (demo-system :willard-sjas-tableau0
                            {:relations {'multi-demo 1}})
        relation-index (encoded-relation-index system 'multi-demo 1)
        other-index (if (= 1 relation-index) 2 1)
        atom (structural-app-term system 'multi-demo sjas/one)
        matcho (var-get #'sjas-profile/reflected-call-header-matcho)
        nonmatcho (var-get #'sjas-profile/reflected-call-header-nonmatcho)]
    (is (successful?
          (l/run 1 [q]
            (matcho atom relation-index 2)
            (l/== true q)))
        "matching reflected clauses are selected by encoded relation index and arity byte")
    (is (empty?
          (l/run 1 [q]
            (matcho atom relation-index 1)
            (l/== true q)))
        "the match relation rejects the same relation with the wrong encoded arity")
    (is (successful?
          (l/run 1 [q]
            (nonmatcho atom relation-index 1)
            (l/== true q)))
        "wrong arity is an explicit reflected-clause nonmatch")
    (is (successful?
          (l/run 1 [q]
            (nonmatcho atom other-index 2)
            (l/== true q)))
        "wrong relation index is an explicit reflected-clause nonmatch")
    (is (empty?
          (l/run 1 [q]
            (nonmatcho atom relation-index 2)
            (l/== true q)))
        "a matching reflected clause cannot also be skipped by the nonmatch relation")))

(defn- renamed-demo-system
  [profile reflected-relation external-relation]
  (sjas/system
    {:profile profile
     :relations {reflected-relation 1
                 external-relation 1}
     :beta [(demo-beta)]
     :reflected-clauses [(reflected-demo-clause reflected-relation)]
     :external-clauses [(external-demo-clause external-relation)]}))

(defn- target-for-theorem
  [system formula]
  (normalize/negate-formula (sjas/theorem-query system formula)))

(defn- canonical-formula-code
  [system canonical-formula]
  (sjas-code/canonical-formula-code-term (:coding-context system)
                                         canonical-formula))

(defn- wff-var0-substitution-codes
  [system]
  (let [source-formula '(pos (app wff (var v0)))
        source-code (canonical-formula-code system source-formula)
        target-formula (list 'pos (list 'app 'wff source-code))
        target-code (canonical-formula-code system target-formula)]
    {:source-code source-code
     :target-code target-code}))

(defn- demo-var0-substitution-codes
  [system]
  (let [source-formula '(pos (app demo (var v0)))
        source-code (canonical-formula-code system source-formula)
        target-formula (list 'pos (list 'app 'demo source-code))
        target-code (canonical-formula-code system target-formula)]
    {:source-code source-code
     :target-code target-code}))

(defn- shadowed-var0-substitution-code
  [system]
  (canonical-formula-code
    system
    '(forall v0 (pos (app wff (var v0))))))

(defn- proof-symbol-count
  "Count proof-symbol leaves in an encoded kernel proof payload."
  [proof]
  (cond
    (symbol? proof) 1
    (sequential? proof) (reduce + 0 (map proof-symbol-count proof))
    :else 0))

(defn- formula-code-bytes
  [system formula]
  (or (sjas-code/code-term-bytes (sjas/formula-code system formula))
      (sjas-code/u-grounding-code-term-bytes (sjas/formula-code system formula))))

(defn- structural-tableau-node
  "Encode one formula-bearing tableau node as proof data without a rule tag.

   Shape: `(byte-count byte... child...)`. The local checker must infer the
   applicable rule from the decoded node formula and its children."
  [system formula & children]
  (let [bytes (formula-code-bytes system formula)]
    (is (pos? (count bytes)))
    (is (< (count bytes) 64)
        "the current structural-node slice uses one proof byte for formula length")
    (apply list (concat [(count bytes)] bytes children))))

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
      (is (not-any? #(contains? (:program system) %)
                    [:sjas/system-code :sjas/fact-atoms :sjas/proof-targets])
          "compiled SJAS programs must not carry stale host-side proof or fact tables")
      (is (not (contains? @(get-in system [:program :sjas/registry])
                          :sjas/reflected-program))
          "proof-predicate reflected calls must not depend on a reflected compiled-program side table")
      (is (not (contains? @(get-in system [:program :sjas/registry])
                          :sjas/symbol-index-entries))
          "proof predicates must not depend on a generated source symbol table"))))

(deftest sjas-symbol-table-is-irrelevant-up-to-signature-isomorphism
  (let [base (demo-system :willard-sjas-tableau0)
        renamed (renamed-demo-system :willard-sjas-tableau0
                                     'zz-demo
                                     'aa-external-demo)
        base-index (get-in base [:coding-context :symbol->index 'demo])
        renamed-index (get-in renamed [:coding-context :symbol->index 'zz-demo])
        base-reflected-record (first (filter #(= :group-two-b (:group %))
                                             (:axioms base)))
        renamed-reflected-record (first (filter #(= :group-two-b (:group %))
                                                (:axioms renamed)))
        base-theorem (ast/pos-lit (ast/app-term 'demo sjas/one))
        renamed-theorem (ast/pos-lit (ast/app-term 'zz-demo sjas/one))
        base-code (sjas/formula-code base base-theorem)
        renamed-code (sjas/formula-code renamed renamed-theorem)
        base-proof (first-proof
                     (sjas/query-succeeds base base-theorem
                                          {:proof-limit 1
                                           :fuel 160}))
        renamed-proof (first-proof
                        (sjas/query-succeeds renamed renamed-theorem
                                             {:proof-limit 1
                                              :fuel 160}))
        base-certificate (when base-proof
                           (sjas/proof-certificate base-proof))
        renamed-certificate (when renamed-proof
                              (sjas/proof-certificate renamed-proof))]
    (is base-proof)
    (is renamed-proof)
    (is (not= base-index renamed-index)
        "the regression must exercise an actual finite codebook renaming")
    (is (not= (:system-code base) (:system-code renamed))
        "renamed signatures are recoded rather than nominally identical")
    (is (not= base-code renamed-code))
    (is (= base-index (nth (sjas-code/code-term-bytes base-code) 2)))
    (is (= renamed-index (nth (sjas-code/code-term-bytes renamed-code) 2)))
    (is (= base-certificate renamed-certificate)
        "proof constructors and proof size are preserved by signature renaming")
    (is (successful?
          (query/query-succeeds
            (:program base)
            (sjas/axiom-member (:system-code base)
                               (:code base-reflected-record))
            1
            160)))
    (is (successful?
          (query/query-succeeds
            (:program renamed)
            (sjas/axiom-member (:system-code renamed)
                               (:code renamed-reflected-record))
            1
            160)))))

(deftest sjas-byte-codes-preserve-sequence-length-and-trailing-zeroes
  (testing "public code terms are byte strings, not lossy natural labels"
    (let [bytes [1 0]
          code (sjas-code/bytes->code-term bytes)
          normalized-through-natural (sjas-code/code-term
                                       (sjas-code/bytes->natural bytes))]
      (is (= bytes (sjas-code/code-term-bytes code)))
      (is (not= bytes (sjas-code/code-term-bytes normalized-through-natural))
          "natural-number views are diagnostic; byte-sequence encoders must not use them when trailing zeroes matter"))))

(deftest sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically
  (let [system (demo-system :willard-sjas-level1)
        noncanonical-one (sjas/add-term (sjas/dbl-term sjas/zero) sjas/one)
        true-code (ast/app-term (sjas-code/code-symbol 1) noncanonical-one)
        proofs (query/query-succeeds
                 (:program system)
                 (sjas/wff true-code)
                 1
                 96)]
    (is (successful? proofs)
        "compact code bytes should be read as U-Grounding numerals, not matched against generated canonical byte terms")
    (is (proof/contains-step? (first-proof proofs) 'sjas-code-arg))))

(deftest sjas-formula-codes-preserve-trailing-zero-embedded-code-payloads
  (testing "an embedded code term at formula end remains structurally decodable"
    (let [system (demo-system :willard-sjas-level1)
          embedded (sjas-code/bytes->code-term [1 0])
          formula (sjas/wff embedded)
          formula-code (sjas/formula-code system formula)
          formula-bytes (sjas-code/code-term-bytes formula-code)]
      (is (= 0 (last formula-bytes))
          "this regression must exercise a formula code whose final byte is zero")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/wff formula-code)
              1
              96))))))

(deftest sjas-u-grounding-code-format-emits-numeral-codes-without-code-constructors
  (testing "the stronger SJAS code format uses only the U-Grounding signature"
    (let [system (demo-system :willard-sjas-level1
                              {:code-format :u-grounding})
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))]
      (is (= :u-grounding (:code-format system)))
      (is (sjas-numeral-term? (:system-code system)))
      (is (not (sjas-code/code-term? (:system-code system))))
      (is (sjas-numeral-term? (:code beta-record)))
      (is (not (sjas-code/code-term? (:code beta-record))))
      (is (not-any? code-constructor-symbol?
                    (keys (get-in system [:language :functions])))
          "U-Grounding coded systems must not expose generated code-N constructors"))))

(deftest sjas-code-format-dispatch-does-not-read-source-registry
  (is (not (str/includes?
             (slurp "src/proflog/kernel/willard_sjas_profile.clj")
             "sjas-code-format"))
      "proof-profile code readers must infer compact vs U-Grounding from the code term, not source registry metadata"))

(deftest sjas-syntax-predicates-decode-application-codes-without-symbol-registry
  (testing "syntax checks need only application structure, numeric symbol ids, and arities"
    (let [system (demo-system :willard-sjas-level1)
          formula (sjas/lt sjas/one sjas/two)
          code (sjas/formula-code system formula)
          no-registry-program (dissoc (:program system) :sjas/registry)
          wff-proofs (query/query-succeeds
                       no-registry-program
                       (sjas/wff code)
                       1
                       48)]
      (is (sjas-code/code-term? code))
      (is (not (contains? no-registry-program :sjas/registry)))
      (is (successful? wff-proofs)
          "wff should decode app-bearing formula codes without the source symbol table")
      (is (proof/contains-step? (first-proof wff-proofs) 'sjas-code-arg)
          "the syntax proof should still read the code bytes through the object relation"))))

(deftest sjas-u-grounding-codes-preserve-trailing-zero-byte-sequences
  (testing "sentinel natural codes remain injective for byte strings ending in zero"
    (let [bytes [1 0]
          code (sjas-code/bytes->u-grounding-code-term bytes)]
      (is (sjas-numeral-term? code))
      (is (not (sjas-code/code-term? code)))
      (is (= bytes (sjas-code/u-grounding-code-term-bytes code))))))

(deftest sjas-u-grounding-syntax-predicates-decode-numeral-codes
  (testing "wff, class predicates, and neg-pair accept pure U-Grounding numeral codes"
    (let [system (demo-system :willard-sjas-level1
                              {:code-format :u-grounding})
          formula (sjas/lt sjas/one sjas/two)
          code (sjas/formula-code system formula)
          complement-code (sjas/formula-code
                            system
                            (normalize/negate-formula formula))
          wff-proofs (query/query-succeeds
                       (:program system)
                       (sjas/wff code)
                       1
                       160)]
      (is (sjas-numeral-term? code))
      (is (sjas-numeral-term? complement-code))
      (is (successful? wff-proofs))
      (is (proof/contains-step? (first-proof wff-proofs) 'sjas-ug-code-bytes)
          "the proof should route through the relation-backed U-Grounding code decoder")
      (is (proof/contains-step? (first-proof wff-proofs) 'sjas-ug-code-byte-cons)
          "ground U-Grounding code decoding must prove byte-cons equations inside the object relation")
      (is (proof/contains-step? (first-proof wff-proofs) 'sjas-ug-code-mul64-shift)
          "ground U-Grounding code decoding must cite the fixed-radix multiplication relation")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/delta-star-0-code code)
              1
              160)))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/neg-pair code complement-code)
              1
              200))))))

(deftest sjas-u-grounding-bound-code-decoding-uses-byte-cons-relation
  (testing "non-ground entry decodes U-Grounding codes through the radix-64 relation"
    (ast/nom code-var
      (let [system (demo-system :willard-sjas-level1
                                {:code-format :u-grounding})
            formula (ast/eq-lit sjas/one sjas/one)
            code (sjas/formula-code system formula)
            code-term (ast/var-term code-var)
            proofs (query/query-succeeds
                     (:program system)
                     (ast/exists-form
                       code-var
                       (ast/and-form
                         (ast/eq-lit code-term code)
                         (sjas/wff code-term)))
                     1
                     220)]
        (is (successful? proofs))
        (is (proof/contains-step? (first-proof proofs) 'sjas-ug-code-byte-cons)
            "the fallback decoder should prove the byte = low-6-bits relation")
        (is (proof/contains-step? (first-proof proofs) 'sjas-ug-code-mul64-shift)
            "the fallback decoder should cite the fixed-radix multiplication rule")))))

(deftest sjas-u-grounding-tableau-proof-checks-numeral-system-theorem-and-proof-codes
  (testing "tableau-proof can consume U-Grounding system, theorem, and proof numerals"
    (let [system (demo-system :willard-sjas-tableau0
                              {:code-format :u-grounding})
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          axiom-certificate (sjas/proof-certificate 'sjas-axiom
                                                    {:code-format :u-grounding})]
      (is (sjas-numeral-term? (:system-code system)))
      (is (sjas-numeral-term? (:code beta-record)))
      (is (sjas-numeral-term? axiom-certificate))
      (let [proofs (query/query-succeeds
                     (:program system)
                     (sjas/tableau-proof (:system-code system)
                                         (:code beta-record)
                                         axiom-certificate)
                     1
                     200)]
        (is (successful? proofs))
        (let [proof (first-proof proofs)
              audit (correspondence/audit-proof-term proof)]
          (is (proof/contains-step? proof 'sjas-ug-code-byte-cons)
              "U-Grounding proof predicates must not decode ground system or theorem codes with a host shortcut")
          (is (proof/contains-step? proof 'sjas-ug-code-canonical-byte))
          (is (= #{}
                 (:unencodable-symbols audit)))
          (is (= #{}
                 (:unclassified-symbols audit))))))))

(deftest sjas-u-grounding-subst-code-computes-level1-fixed-point
  (testing "Level-1 Subst uses the U-Grounding source code numeral as the diagonal term"
    (let [system (sjas/system {:profile :willard-sjas-level1
                               :code-format :u-grounding})
          group3-record (:group-three system)]
      (is (sjas-numeral-term? (:selfcons-skeleton-code system)))
      (is (sjas-numeral-term? (:code group3-record)))
      (is (subst-code-relation-succeeds?
            system
            (:selfcons-skeleton-code system)
            (:code group3-record))
          "the arithmeticized relation should verify the Level-1 fixed point without a host byte projector")
      (is (not (subst-code-relation-succeeds?
                 system
                 (:system-code system)
                 (:code group3-record)))
          "a system code is not a formula code and must not pass as a substitution source"))))

(deftest sjas-proof-codes-are-byte-strings-with-symbol-bit-lower-bound
  (testing "proof certificates encode proof syntax rather than hashing it"
    (let [proof '(conj (profiled willard-sjas-proof-check (sjas-code-bytes) sjas-axiom))
          bytes (sjas-code/proof-code-bytes proof)
          certificate (sjas-code/proof-code-term proof)
          symbol-count (proof-symbol-count proof)]
      (is (sjas-code/code-term? certificate))
      (is (= bytes (sjas-code/code-term-bytes certificate)))
      (is (<= (* 5 symbol-count) (* 6 (count bytes)))
          "Willard's ordinary-tableau coding requirement needs at least five bits per encoded proof symbol"))))

(deftest sjas-proof-codes-encode-minimal-arithmetic-close-certificates
  (testing "arithmetic branch closure can be encoded as a tableau leaf, not a trace of numeral reads"
    (let [proof '(conj (skip-true (arith-close)))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-formula-bearing-tableau-nodes-without-rule-tags
  (testing "semantic-tableau proof objects can carry formula nodes rather than Proflog proof-rule tags"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form (ast/true-form) (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    (ast/true-form)
                    (structural-tableau-node system (ast/false-form))))
          certificate (sjas/proof-certificate proof)]
      (is (zero? (proof-symbol-count proof))
          "the structural node proof should not use rule names such as conj, skip-true, or false-close")
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-byte-payload-evidence
  (testing "code-reader proof evidence carries inspectable byte payloads rather than escaping the certificate grammar"
    (let [proof '(conj
                   (sjas-code-arg 1 sjas-code-args-end)
                   (free-close))
          certificate (sjas/proof-certificate proof)
          bytes (sjas-code/code-term-bytes certificate)]
      (is (sjas-code/code-term? certificate))
      (is (= bytes (sjas-code/proof-code-bytes proof)))
      (is (some #{sjas-code/proof-byte-tag} bytes)
          "numeric byte payloads in proof evidence must be encoded explicitly"))))

(deftest sjas-proof-codes-encode-nested-equality-closure-evidence
  (testing "nested free-constructor equality closure evidence stays inside the proof-code grammar"
    (let [proof '(conj
                   (decompose
                     (args
                       (decompose ())
                       (free-close))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-positive-equality-step-evidence
  (testing "proof-local equality binding evidence stays inside the proof-code grammar"
    (let [proof '(conj
                   (witness
                     (conj
                       (eq-step
                         (par-bind)
                         (free-close)))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-proof-variable-disequality-closure-evidence
  (testing "proof-variable equality binding evidence stays inside the proof-code grammar"
    (let [proof (list 'conj
                      (list 'once-univ
                            (list 'neq-close
                                  (list 'decompose
                                        (list 'args '(eq-bind) '())))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-equality-triggered-atom-closure-evidence
  (testing "saved atom closure evidence stays inside the proof-code grammar"
    (let [proof (equality-triggered-atom-closure-proof)
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-equality-triggered-positive-call-evidence
  (testing "equality-triggered reflected call evidence stays inside the proof-code grammar"
    (let [proof (equality-triggered-positive-call-proof)
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-equality-triggered-negative-call-evidence
  (testing "equality-triggered reflected negative-call evidence stays inside the proof-code grammar"
    (let [proof (equality-triggered-negative-call-proof)
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-negative-call-alternative-evidence
  (testing "multi-clause reflected negative-call evidence stays inside the proof-code grammar"
    (let [proof '(neg-call-alt (alt (refl-close)))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-guarded-negative-call-evidence
  (testing "guarded reflected negative-call evidence stays inside the proof-code grammar"
    (let [proof '(neg-call-guarded-alt
                   (guarded-alt
                     (guarded-neg-alt
                       (guarded-scope-done)
                       (guarded-seq-last (false-close)))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-saturated-guarded-negative-call-evidence
  (testing "saturated guarded negative-call evidence stays inside the proof-code grammar"
    (let [proof '(neg-call-guarded-alt
                   (guarded-alt
                     (guarded-neg-alt-saturated
                       (guarded-scope-done)
                       (guard-saturation-done)
                       (guarded-call-seq-done)
                       (guarded-residual-seq-last (false-close)))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-guard-equality-saturation-evidence
  (testing "non-empty guarded equality saturation evidence stays inside the proof-code grammar"
    (let [proof '(neg-call-guarded-alt
                   (guarded-alt
                     (guarded-neg-alt-saturated
                       (guarded-scope-done)
                       (guard-eq (eq-refl) (guard-saturation-done))
                       (guarded-call-seq-done)
                       (guarded-residual-seq-last (false-close)))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-existential-guarded-scope-evidence
  (testing "existential guarded scope evidence stays inside the proof-code grammar"
    (let [proof '(neg-call-guarded-alt
                   (guarded-alt
                     (guarded-neg-alt
                       (guarded-scope-exists (guarded-scope-done))
                       (guarded-seq-last (false-close)))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-certificates-preserve-generic-profiled-sidecar-evidence
  (testing "only outer SJAS profile annotations are erased before proof-code encoding"
    (let [sidecar-proof '(profiled propositional (conj (false-close)))
          certificate (sjas/proof-certificate sidecar-proof)]
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes sidecar-proof)))
      (is (= sidecar-proof
             (sjas-code/proof-formal-code-term->proof certificate))))))

(deftest sjas-proof-codes-encode-answer-overlay-evidence
  (testing "answer-overlay proof evidence remains inspectable but outside the SJAS proof predicate"
    (let [proof '(query-pos-call (conj (false-close)))
          certificate (sjas/proof-certificate proof)]
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof)))
      (is (= proof
             (sjas-code/proof-formal-code-term->proof certificate))))))

(deftest sjas-proof-codes-encode-recursive-guarded-call-sequence-evidence
  (testing "recursive guarded-call sequence evidence stays inside the proof-code grammar"
    (let [proof '(neg-call-guarded-alt
                   (guarded-alt
                     (guarded-neg-alt-saturated
                       (guarded-scope-done)
                       (guard-saturation-done)
                       (guarded-call-seq-step
                         (neg-call-guarded-alt
                           (guarded-alt
                             (guarded-neg-alt-saturated
                               (guarded-scope-done)
                               (guard-saturation-done)
                               (guarded-call-seq-done)
                               (guarded-residual-seq-last (false-close)))))
                         (guarded-call-seq-done))
                       (guarded-residual-seq-last (false-close)))))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-codes-encode-occurs-check-closure-evidence
  (testing "occurs-check equality contradiction evidence stays inside the proof-code grammar"
    (let [proof '(conj (once-univ (occurs-close)))
          certificate (sjas/proof-certificate proof)]
      (is (sjas-code/code-term? certificate))
      (is (= (sjas-code/code-term-bytes certificate)
             (sjas-code/proof-code-bytes proof))))))

(deftest sjas-proof-code-decoder-round-trips-byte-payload-evidence
  (testing "the object-level proof-code decoder consumes explicit byte payloads"
    (let [proof '(conj
                   (sjas-code-arg 1 sjas-code-args-end)
                   (free-close))
          certificate (sjas/proof-certificate proof)
          decode-proof-codeo (var-get #'sjas-profile/decode-proof-codeo)
          decoded (l/run* [q]
                    (l/fresh [bytes decoded-proof read-proof]
                      (decode-proof-codeo certificate
                                          '()
                                          '()
                                          bytes
                                          decoded-proof
                                          read-proof)
                      (l/== [bytes decoded-proof read-proof] q)))]
      (is (= 1 (count decoded)))
      (is (= proof (sjas-code/proof-formal-code-term->proof certificate)))
      (is (= proof (second (first decoded)))))))

(deftest sjas-proof-code-decoder-round-trips-equality-triggered-atom-closure-evidence
  (testing "the object-level proof-code decoder consumes saved atom closure evidence"
    (let [proof (equality-triggered-atom-closure-proof)
          certificate (sjas/proof-certificate proof)
          decode-proof-codeo (var-get #'sjas-profile/decode-proof-codeo)
          decoded (l/run 1 [q]
                    (l/fresh [bytes decoded-proof read-proof]
                      (decode-proof-codeo certificate
                                          '()
                                          '()
                                          bytes
                                          decoded-proof
                                          read-proof)
                      (l/== decoded-proof q)))]
      (is (= 1 (count decoded)))
      (is (= proof (sjas-code/proof-formal-code-term->proof certificate)))
      (is (= proof (first decoded))))))

(deftest sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates
  (testing "proof predicates classify sjas-axiom from proof bytes without committed choice"
    (let [decode-axiom (var-get #'sjas-profile/decode-sjas-axiom-proof-codeo)
          decode-non-axiom (var-get #'sjas-profile/decode-non-sjas-axiom-proof-codeo)
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          ug-axiom-certificate (sjas/proof-certificate 'sjas-axiom
                                                       {:code-format :u-grounding})
          substantive-proof '(false-close)
          substantive-certificate (sjas/proof-certificate substantive-proof)
          axiom-bytes (apply list (sjas-code/proof-code-bytes 'sjas-axiom))
          axiom-decodes (l/run 1 [q]
                          (l/fresh [sigma-out bytes read-proof]
                            (decode-axiom axiom-certificate
                                          '()
                                          sigma-out
                                          bytes
                                          read-proof)
                            (l/== [sigma-out bytes] q)))
          ug-axiom-decodes (l/run 1 [q]
                             (l/fresh [sigma-out bytes read-proof]
                               (decode-axiom ug-axiom-certificate
                                             '()
                                             sigma-out
                                             bytes
                                             read-proof)
                               (l/== [sigma-out bytes] q)))
          axiom-as-non-axiom (l/run 1 [q]
                               (l/fresh [sigma-out bytes decoded read-proof]
                                 (decode-non-axiom axiom-certificate
                                                   '()
                                                   sigma-out
                                                   bytes
                                                   decoded
                                                   read-proof)
                                 (l/== decoded q)))
          substantive-decodes (l/run 1 [q]
                                (l/fresh [sigma-out bytes decoded read-proof]
                                  (decode-non-axiom substantive-certificate
                                                    '()
                                                    sigma-out
                                                    bytes
                                                    decoded
                                                    read-proof)
                                  (l/== [sigma-out decoded] q)))]
      (is (= [['() axiom-bytes]] axiom-decodes))
      (is (= [['() axiom-bytes]] ug-axiom-decodes))
      (is (empty? axiom-as-non-axiom))
      (is (= [['() substantive-proof]] substantive-decodes)))))

(deftest sjas-proof-codes-encode-u-grounding-canonical-byte-evidence
  (testing "U-Grounding byte-reader evidence carries an explicit byte payload in the proof-code grammar"
    (let [proof '(sjas-ug-code-canonical-byte
                   7
                     (sjas-ug-code-byte-cons
                       (sjas-ug-code-mul64-shift)
                       (sjas-ug-code-canonical-byte)))
          certificate (sjas/proof-certificate proof)
          ug-certificate (sjas/proof-certificate proof {:code-format :u-grounding})
          bytes (sjas-code/code-term-bytes certificate)]
      (is (sjas-code/code-term? certificate))
      (is (= bytes (sjas-code/proof-code-bytes proof)))
      (is (= proof (sjas-code/proof-formal-code-term->proof ug-certificate)))
      (is (some #{sjas-code/proof-byte-tag} bytes)
          "the canonical-byte evidence payload must be represented as a proof byte"))))

(deftest sjas-syntax-predicates-decode-formula-godel-codes
  (testing "wff, class predicates, and neg-pair are derived from formula Godel codes"
    (let [system (demo-system :willard-sjas-level1)
          formula (ast/true-form)
          code (sjas/formula-code system formula)
          complement-code (sjas/formula-code system
                                             (normalize/negate-formula formula))
          registry @(get-in system [:program :sjas/registry])
          fact-atoms (get-in system [:program :sjas/fact-atoms])
          wff-proofs (query/query-succeeds
                       (:program system)
                       (sjas/wff code)
                       1
                       32)
          delta-proofs (query/query-succeeds
                         (:program system)
                         (sjas/delta-star-0-code code)
                         1
                         32)
          neg-pair-proofs (query/query-succeeds
                            (:program system)
                            (sjas/neg-pair code complement-code)
                            1
                            48)]
      (is (sjas-code/code-term? complement-code))
      (is (not= (sjas/not-code code) complement-code)
          "complements must be formula Godel-code terms, not not-code wrappers")
      (is (not-any? #(contains? registry %)
                    [:sjas/formula-entries
                     :sjas/formula-negation-entries
                     :sjas/formula-class-entries
                     :sjas/neg-pair-entries])
          "syntax predicates must not depend on generated formula lookup registries")
      (is (not-any? (fn [atom]
                      (contains? '#{wff delta-star-0-code
                                    pi-star-1-code sigma-star-1-code}
                                 (second atom)))
                    fact-atoms)
          "syntax predicates must not be generated whole-formula facts")
      (is (successful? wff-proofs))
      (is (proof/contains-step? (first-proof wff-proofs) 'sjas-code-arg)
          "compact formula-code predicates must read code constructor bytes through the object relation")
      (is (= #{}
             (:unencodable-symbols (proof-symbol-audit (first-proof wff-proofs)))))
      (is (= #{}
             (:unclassified-symbols (proof-symbol-audit (first-proof wff-proofs)))))
      (is (sjas-code/code-term?
            (sjas/proof-certificate (first-proof wff-proofs)))
          "syntax predicate proof evidence must be representable in the SJAS proof-code grammar")
      (is (successful? delta-proofs))
      (is (= #{}
             (:unencodable-symbols (proof-symbol-audit (first-proof delta-proofs)))))
      (is (= #{}
             (:unclassified-symbols (proof-symbol-audit (first-proof delta-proofs)))))
      (is (successful? neg-pair-proofs))
      (is (= #{}
             (:unencodable-symbols (proof-symbol-audit (first-proof neg-pair-proofs)))))
      (is (= #{}
             (:unclassified-symbols (proof-symbol-audit (first-proof neg-pair-proofs))))))))

(deftest sjas-system-does-not-generate-axiom-member-fact-registry
  (let [system (demo-system :willard-sjas-tableau0)
        registry @(get-in system [:program :sjas/registry])]
    (is (not (contains? registry :sjas/fact-atoms))
        "axiom-member/2 predicate evaluation must not depend on generated host fact metadata")
    (is (not (contains? (:clauses (:program system)) 'axiom-member))
        "the generated SJAS basis must not add axiom-member/2 facts as ordinary clauses")))

(deftest sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry
  (testing "beta membership compares formula bytes without source symbol lookup"
    (let [beta-formula (sjas/lt sjas/one sjas/two)
          system (demo-system :willard-sjas-tableau0
                              {:beta [beta-formula]})
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          no-registry-program (dissoc (:program system) :sjas/registry)
          proofs (query/query-succeeds
                   no-registry-program
                   (sjas/axiom-member (:system-code system)
                                      (:code beta-record))
                   1
                   96)]
      (is (not (contains? no-registry-program :sjas/registry)))
      (is (sjas-code/code-term? (:code beta-record)))
      (is (successful? proofs)
          "Group-2 beta membership should not need the finite source symbol table"))))

(deftest sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry
  (testing "reflected axiom membership reconstructs clause formulas structurally"
    (let [system (demo-system :willard-sjas-tableau0)
          reflected-record (first (filter #(= :group-two-b (:group %)) (:axioms system)))
          no-registry-program (dissoc (:program system) :sjas/registry)
          proofs (query/query-succeeds
                   no-registry-program
                   (sjas/axiom-member (:system-code system)
                                      (:code reflected-record))
                   1
                   128)]
      (is (not (contains? no-registry-program :sjas/registry)))
      (is (sjas-code/code-term? (:code reflected-record)))
      (is (successful? proofs)
          "Group-2b reflected axiom membership should not need the finite source symbol table"))))

(deftest sjas-system-does-not-generate-proof-antecedent-registry
  (let [system (demo-system :willard-sjas-tableau0)
        registry @(get-in system [:program :sjas/registry])]
    (is (not (contains? registry :sjas/system-entries))
        "proof predicates must reconstruct the finite axiom basis from system-code, not generated host antecedents")))

(deftest sjas-proof-predicates-do-not-require-source-preprocessing-registry
  (let [system (demo-system :willard-sjas-tableau0)
        fixed-record (first (filter #(= :group-zero (:group %)) (:axioms system)))
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)
        no-registry-program (dissoc (:program system) :sjas/registry)]
    (is (not (contains? no-registry-program :sjas/registry))
        "the regression must remove the source-preprocessing registry")
    (is (not (str/includes?
               (slurp "src/proflog/kernel/willard_sjas_profile.clj")
               "sjas-active-systemo"))
        "proof predicates must not retain an active-system registry guard")
    (is (successful?
          (query/query-succeeds
            no-registry-program
            (sjas/tableau-proof (:system-code system)
                                (:code fixed-record)
                                axiom-certificate)
            1
            96))
        "tableau-proof must validate fixed-axiom certificates from system-code, not an active source registry")
    (is (successful?
          (query/query-succeeds
            no-registry-program
            (sjas/subst-prf (:system-code system)
                            (:code fixed-record)
                            (:code fixed-record)
                            axiom-certificate)
            1
            96))
        "subst-prf must validate fixed-axiom certificates from system-code, not an active source registry")))

(deftest ^:slow sjas-structural-code-predicates-accept-non-generated-formula-codes
  (testing "formula-code predicates parse codes beyond the generated axiom registry"
    (let [system (demo-system :willard-sjas-level1)
          formula (sjas/lt sjas/one sjas/two)
          code (sjas/formula-code system formula)
          complement-code (sjas/formula-code system
                                             (normalize/negate-formula formula))
          generated-codes (set (map :code (:axioms system)))
          wff-proofs (query/query-succeeds
                       (:program system)
                       (sjas/wff code)
                       1
                       32)]
      (is (not (contains? generated-codes code))
          "the test formula must not be one of the generated axiom codes")
      (is (successful? wff-proofs))
      (is (proof/contains-step? (first-proof wff-proofs) 'sjas-code-arg)
          "compact formula-code predicates must read code constructor bytes through the object relation")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/delta-star-0-code code)
              1
              32)))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/neg-pair code complement-code)
              1
              48)))
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/subst-code code code)
              1
              48))))))

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
          composite-four (ast/pos-lit (ast/app-term 'composite (n 4)))
          group2b-record (first (filter #(= :group-two-b (:group %))
                                        (:axioms system)))
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          reflected-citation-proofs (query/query-succeeds
                                      (:program system)
                                      (sjas/tableau-proof (:system-code system)
                                                          (:code group2b-record)
                                                          axiom-certificate)
                                      1
                                      120)]
      (is (= {:group-zero 2
              :group-one 3
              :group-two-b 1
              :group-three 1}
             (frequencies (map :group (:axioms system)))))
      (is (successful? reflected-citation-proofs))
      (is (proof/contains-step? (first-proof reflected-citation-proofs)
                                'sjas-system-reflected-axiom)
          "reflected clause axiom citations must be recovered from encoded system-code clauses")
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
        valid-proofs (query/query-succeeds
                       (:program system)
                       (sjas/tableau-proof (:system-code system)
                                           (:code beta-record)
                                           valid)
                       1
                       160)]
    (is beta-proof)
    (is (sjas-code/code-term? valid)
        "proof certificates must be base-64 Godel-code terms")
    (is (successful? valid-proofs))
    (is (proof/contains-step? (first-proof valid-proofs) 'willard-sjas-theorem-code)
        "tableau-proof must decode generated theorem codes structurally during predicate application")
    (is (proof/contains-step? (first-proof valid-proofs) 'sjas-code-bytes)
        "theorem-code decoding inside tableau-proof must cite the uniform object code-reader relation")
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                (:system-code system)
                                valid)
            1
            80)))))

(deftest sjas-tableau-proof-reconstructs-axiom-basis-without-system-registry
  (let [system (demo-system :willard-sjas-tableau0)
        registry (get-in system [:program :sjas/registry])
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        beta-proof (first-proof
                     (sjas/query-succeeds system (:formula beta-record)
                                          {:proof-limit 1
                                           :fuel 96}))
        certificate (sjas/proof-certificate beta-proof)]
    (is beta-proof)
    (swap! registry dissoc :sjas/system-entries)
    (is (not (contains? @registry :sjas/system-entries))
        "the regression must remove the generated host-side proof antecedent")
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                (:code beta-record)
                                certificate)
            1
            200))
        "tableau-proof must reconstruct the axiom basis from system-code during predicate application")))

(deftest sjas-proof-predicates-ignore-external-runtime-clauses
  (let [system (demo-system :willard-sjas-tableau0)
        formula (ast/pos-lit (ast/app-term 'external-demo sjas/zero))
        theorem-code (sjas/formula-code system formula)
        certificate (sjas/proof-certificate
                      '(conj
                         (neg-call
                           (profiled willard-sjas-arithmetic
                             (sjas-equal
                               (sjas-read-zero)
                               (sjas-read-zero)
                               (sjas-bind-done))))))]
    (is (successful?
          (query/query-succeeds
            (:program system)
            formula
            1
            96))
        "external clauses remain executable for ordinary host-side Proflog queries")
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                theorem-code
                                certificate)
            1
            200))
        "tableau-proof must validate certificates against the reflected SJAS system, not external runtime clauses")
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            theorem-code
                            theorem-code
                            certificate)
            1
            200))
        "subst-prf must validate certificates against the reflected SJAS system, not external runtime clauses")))

(deftest ^:slow sjas-tableau-proof-checks-structural-non-generated-theorem-codes
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (sjas/lt sjas/one sjas/two)
        theorem-code (sjas/formula-code system theorem)
        wrong-theorem-code (sjas/formula-code system
                                              (sjas/lt sjas/two sjas/one))
        theorem-proof (first-proof
                        (sjas/query-succeeds system theorem
                                             {:proof-limit 1
                                              :fuel 96}))
        certificate (when theorem-proof
                      (sjas/proof-certificate theorem-proof))
        generated-codes (set (map :code (:axioms system)))]
    (is theorem-proof)
    (is (not (contains? generated-codes theorem-code))
        "the theorem code must not be one of the generated axiom codes")
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                theorem-code
                                certificate)
            1
            180)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                wrong-theorem-code
                                certificate)
            1
            120)))))

(deftest ^:slow sjas-proof-predicates-decode-built-in-relations-without-symbol-registry
  (let [system (demo-system :willard-sjas-tableau0)
        no-registry-program (dissoc (:program system) :sjas/registry)
        theorem (sjas/lt sjas/one sjas/two)
        theorem-code (sjas/formula-code system theorem)
        theorem-proof (first-proof
                        (sjas/query-succeeds system theorem
                                             {:proof-limit 1
                                              :fuel 96}))
        certificate (when theorem-proof
                      (sjas/proof-certificate theorem-proof))]
    (is theorem-proof)
    (is (not (contains? no-registry-program :sjas/registry)))
    (is (successful?
          (query/query-succeeds
            no-registry-program
            (sjas/tableau-proof (:system-code system)
                                theorem-code
                                certificate)
            1
            180))
        "tableau-proof must recover fixed arithmetic relation semantics from formula-code structure, not the source symbol registry")
    (is (successful?
          (query/query-succeeds
            no-registry-program
            (sjas/subst-prf (:system-code system)
                            theorem-code
                            theorem-code
                            certificate)
            1
            220))
        "subst-prf must recover fixed arithmetic relation semantics from formula-code structure, not the source symbol registry")))

(deftest ^:slow sjas-subst-prf-checks-structural-non-generated-theorem-codes
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (sjas/lt sjas/one sjas/two)
        theorem-code (sjas/formula-code system theorem)
        wrong-theorem-code (sjas/formula-code system
                                              (sjas/lt sjas/two sjas/one))
        theorem-proof (first-proof
                        (sjas/query-succeeds system theorem
                                             {:proof-limit 1
                                              :fuel 96}))
        certificate (when theorem-proof
                      (sjas/proof-certificate theorem-proof))
        generated-codes (set (map :code (:axioms system)))]
    (is theorem-proof)
    (is (not (contains? generated-codes theorem-code))
        "the theorem code must not be one of the generated axiom codes")
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            theorem-code
                            theorem-code
                            certificate)
            1
            220)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            theorem-code
                            wrong-theorem-code
                            certificate)
            1
            160)))))

(deftest sjas-subst-prf-checks-identity-substitution-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        beta-proof (first-proof
                     (sjas/query-succeeds system (:formula beta-record)
                                          {:proof-limit 1
                                           :fuel 96}))
        valid (sjas/proof-certificate beta-proof)
        valid-proofs (query/query-succeeds
                       (:program system)
                       (sjas/subst-prf (:system-code system)
                                       (:code beta-record)
                                       (:code beta-record)
                                       valid)
                       1
                       160)]
    (is beta-proof)
    (is (successful? valid-proofs))
    (is (proof/contains-step? (first-proof valid-proofs) 'willard-sjas-theorem-code)
        "subst-prf must decode generated theorem codes structurally during predicate application")
    (is (proof/contains-step? (first-proof valid-proofs) 'sjas-code-bytes)
        "theorem-code decoding inside subst-prf must cite the uniform object code-reader relation")
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            (:code beta-record)
                            (:system-code system)
                            valid)
            1
            80)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            (:system-code system)
                            (:code beta-record)
                            valid)
            1
            80)))))

(deftest sjas-proof-predicates-check-simple-arithmetic-certificates-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        beta-proof (first-proof
                     (sjas/query-succeeds system (:formula beta-record)
                                          {:proof-limit 1
                                           :fuel 96}))
        certificate (when beta-proof
                      (sjas/proof-certificate beta-proof))]
    (is beta-proof)
    (do
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  (:code beta-record)
                                  certificate)
              1
              200))
          "tableau-proof must validate simple arithmetic certificates without delegating to the host kernel")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/subst-prf (:system-code system)
                              (:code beta-record)
                              (:code beta-record)
                              certificate)
              1
              240))
          "subst-prf must validate identity-substitution arithmetic certificates without delegating to the host kernel"))))

(deftest sjas-proof-check-accepts-free-equality-closures-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        target (ast/and-form
                 (ast/true-form)
                 (ast/eq-lit sjas/zero sjas/one))
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (do
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           40
                           '(conj (free-close)))
              (l/== true q)))
          "decoded tableau proof checking must consume free equality closure evidence object-level"))))

(deftest sjas-proof-check-accepts-truth-and-falsehood-constructors-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        target (ast/and-form
                 (ast/true-form)
                 (ast/false-form))
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (do
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           40
                           '(conj (skip-true (false-close))))
              (l/== true q)))
          "decoded tableau proof checking must consume skip-true and false-close evidence object-level"))))

(deftest sjas-proof-check-keeps-split-branch-state-independent
  (let [system (demo-system :willard-sjas-tableau0)
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/exists-form
                       x
                       (ast/or-form
                         (ast/and-form
                           (ast/eq-lit x-term sjas/zero)
                           (ast/false-form))
                         (ast/neq-lit x-term sjas/zero))))
            invalid-proof '(conj
                             (witness
                               (split
                                 (conj
                                   (eq-step
                                     (par-bind)
                                     (false-close)))
                                 (refl-close))))]
        (is (empty?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             100
                             invalid-proof)
                (l/== true q)))
            "split branches must not share equality substitutions from sibling branch closures")))))

(deftest sjas-tableau-proof-accepts-false-close-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/true-form)
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate '(conj (false-close)))]
    (do
      (let [proofs (query/query-succeeds
                     (:program system)
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     160)]
        (is (successful? proofs)
            "tableau-proof must validate encoded false-close certificates object-level")
        (is (proof/contains-step? (first-proof proofs) 'false-close))))))

(deftest sjas-tableau-proof-rejects-generic-profiled-sidecar-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/true-form)
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(profiled propositional (conj (false-close))))]
    (do
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              160))
          "SJAS proof predicates must reject generic optimized sidecar certificates rather than erasing the wrapper"))))

(deftest sjas-tableau-proof-rejects-answer-overlay-query-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/true-form)
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(query-pos-call (conj (false-close))))]
    (do
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              160))
          "SJAS proof predicates must reject answer-overlay query-entry certificates"))))

(deftest sjas-tableau-proof-accepts-free-equality-closure-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/neq-lit sjas/zero sjas/one)
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate '(conj (free-close)))]
    (do
      (let [proofs (query/query-succeeds
                     (:program system)
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     160)]
        (is (successful? proofs)
            "tableau-proof must validate encoded free equality closure certificates object-level")
        (is (proof/contains-step? (first-proof proofs) 'free-close))))))

(deftest sjas-tableau-proof-accepts-decomposed-free-equality-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/neq-lit
                  (ast/app-term 'code-2 sjas/zero sjas/zero)
                  (ast/app-term 'code-2 sjas/zero sjas/one))
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(conj
                         (decompose
                           (args
                             (decompose ())
                             (free-close)))))]
    (do
      (let [proofs (query/query-succeeds
                     (:program system)
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     180)]
        (is (successful? proofs)
            "tableau-proof must validate nested free equality closure certificates object-level")
        (is (proof/contains-step? (first-proof proofs) 'decompose))
        (is (proof/contains-step? (first-proof proofs) 'args))))))

(deftest sjas-proof-check-accepts-positive-equality-steps-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/exists-form
                       x
                       (ast/and-form
                         (ast/eq-lit x-term sjas/zero)
                         (ast/eq-lit x-term sjas/one))))]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               80
                               '(conj
                                  (witness
                                    (conj
                                      (eq-step
                                        (par-bind)
                                        (free-close))))))
                  (l/== true q)))
              "decoded tableau proof checking must consume positive equality-step evidence object-level"))))))

(deftest sjas-tableau-proof-accepts-positive-equality-step-certificates
  (let [system (demo-system :willard-sjas-tableau0)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            theorem (ast/forall-form
                      x
                      (ast/or-form
                        (ast/neq-lit x-term sjas/zero)
                        (ast/neq-lit x-term sjas/one)))
            theorem-code (sjas/formula-code system theorem)
            certificate (sjas/proof-certificate
                          '(conj
                             (witness
                               (conj
                                 (eq-step
                                   (par-bind)
                                   (free-close))))))]
        (do
      (let [proofs (query/query-succeeds
                         (:program system)
                         (sjas/tableau-proof (:system-code system)
                                             theorem-code
                                             certificate)
                         1
                         220)]
            (is (successful? proofs)
                "tableau-proof must validate encoded equality-step certificates object-level")
            (is (proof/contains-step? (first-proof proofs) 'eq-step))
            (is (proof/contains-step? (first-proof proofs) 'par-bind))))))))

(deftest sjas-proof-check-accepts-reflexive-disequality-closures-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        term (ast/app-term 'code-1 sjas/zero)
        target (ast/and-form
                 (ast/true-form)
                 (ast/neq-lit term term))
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (do
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           40
                           '(conj (refl-close)))
              (l/== true q)))
          "decoded tableau proof checking must consume reflexive disequality closure evidence object-level"))))

(deftest sjas-tableau-proof-accepts-reflexive-disequality-closure-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        term (ast/app-term 'code-1 sjas/zero)
        theorem (ast/eq-lit term term)
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate '(conj (refl-close)))]
    (do
      (let [proofs (query/query-succeeds
                     (:program system)
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     160)]
        (is (successful? proofs)
            "tableau-proof must validate encoded reflexive disequality closure certificates object-level")
        (is (proof/contains-step? (first-proof proofs) 'refl-close))))))

(deftest sjas-proof-check-accepts-rigid-disequality-progress-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        left (ast/app-term 'code-1 sjas/zero)
        right (ast/app-term 'code-1 sjas/one)
        target (ast/and-form
                 (ast/true-form)
                 (ast/and-form
                   (ast/neq-lit left right)
                   (ast/eq-lit sjas/zero sjas/one)))
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (do
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           60
                           '(conj
                              (conj
                                (neq-rigid
                                  (free-close)))))
              (l/== true q)))
          "decoded tableau proof checking must consume rigid disequality progress evidence object-level"))))

(deftest sjas-tableau-proof-accepts-rigid-disequality-progress-certificates
  (let [system (demo-system :willard-sjas-tableau0)
        left (ast/app-term 'code-1 sjas/zero)
        right (ast/app-term 'code-1 sjas/one)
        theorem (ast/or-form
                  (ast/eq-lit left right)
                  (ast/neq-lit sjas/zero sjas/one))
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(conj
                         (conj
                           (neq-rigid
                             (free-close)))))]
    (do
      (let [proofs (query/query-succeeds
                     (:program system)
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     180)]
        (is (successful? proofs)
            "tableau-proof must validate encoded rigid disequality progress certificates object-level")
        (is (proof/contains-step? (first-proof proofs) 'neq-rigid))))))

(deftest sjas-proof-check-accepts-stored-disequality-closures-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/exists-form
                       x
                       (ast/and-form
                         (ast/neq-lit x-term sjas/zero)
                         (ast/eq-lit x-term sjas/zero))))]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               80
                               '(conj
                                  (witness
                                    (conj
                                      (neq-store
                                        (eq-step
                                          (par-bind)
                                          (neq-close)))))))
                  (l/== true q)))
              "decoded tableau proof checking must store disequality evidence and close it after equality progress"))))))

(deftest sjas-tableau-proof-accepts-stored-disequality-closure-certificates
  (let [system (demo-system :willard-sjas-tableau0)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            theorem (ast/forall-form
                      x
                      (ast/or-form
                        (ast/eq-lit x-term sjas/zero)
                        (ast/neq-lit x-term sjas/zero)))
            theorem-code (sjas/formula-code system theorem)
            certificate (sjas/proof-certificate
                          '(conj
                             (witness
                               (conj
                                 (neq-store
                                   (eq-step
                                     (par-bind)
                                     (neq-close)))))))]
        (do
      (let [proofs (query/query-succeeds
                         (:program system)
                         (sjas/tableau-proof (:system-code system)
                                             theorem-code
                                             certificate)
                         1
                         220)]
            (is (successful? proofs)
                "tableau-proof must validate encoded stored-disequality closure certificates object-level")
            (is (proof/contains-step? (first-proof proofs) 'neq-store))
            (is (proof/contains-step? (first-proof proofs) 'neq-close))))))))

(deftest sjas-proof-check-accepts-proof-variable-disequality-closures-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0 {:functions {'f 1}})
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/once-forall-form
                       x
                       (ast/neq-lit (ast/app-term 'f x-term)
                                    (ast/app-term 'f sjas/zero))))]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               80
                               (list 'conj
                                     (list 'once-univ
                                           (list 'neq-close
                                                 (list 'decompose
                                                       (list 'args '(eq-bind) '()))))))
                  (l/== true q)))
              "decoded tableau proof checking must consume proof-variable disequality closure evidence object-level"))))))

(deftest sjas-tableau-proof-accepts-proof-variable-disequality-closure-certificates
  (let [system (demo-system :willard-sjas-tableau0 {:functions {'f 1}})]
    (ast/nom x
      (let [x-term (ast/var-term x)
            theorem (ast/exists-form
                      x
                      (ast/eq-lit (ast/app-term 'f x-term)
                                  (ast/app-term 'f sjas/zero)))
            theorem-code (sjas/formula-code system theorem)
            certificate (sjas/proof-certificate
                          (list 'conj
                                (list 'once-univ
                                      (list 'neq-close
                                            (list 'decompose
                                                  (list 'args '(eq-bind) '()))))))]
        (do
      (let [proofs (query/query-succeeds
                         (:program system)
                         (sjas/tableau-proof (:system-code system)
                                             theorem-code
                                             certificate)
                         1
                         220)]
            (is (successful? proofs)
                "tableau-proof must validate encoded proof-variable disequality closure certificates object-level")
            (is (proof/contains-step? (first-proof proofs) 'neq-close))
            (is (proof/contains-step? (first-proof proofs) 'eq-bind))))))))

(deftest sjas-proof-check-accepts-equality-triggered-atom-closures-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0 {:relations {'color 1}})
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            color-x (ast/app-term 'color x-term)
            color-zero (ast/app-term 'color sjas/zero)
            target (ast/and-form
                     (ast/true-form)
                     (ast/once-forall-form
                       x
                       (ast/and-form
                         (ast/pos-lit color-x)
                         (ast/and-form
                           (ast/neg-lit color-zero)
                           (ast/eq-lit x-term sjas/zero)))))
            proof (equality-triggered-atom-closure-proof)]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               100
                               proof)
                  (l/== true q)))
              "decoded tableau proof checking must consume equality-triggered atom closure evidence object-level"))))))

(deftest sjas-proof-check-accepts-equality-triggered-positive-calls-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/exists-form
                       x
                       (ast/and-form
                         (structural-pos-lit system 'demo x-term)
                         (ast/eq-lit x-term sjas/zero))))
            proof (equality-triggered-positive-call-proof)]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               120
                               proof)
                  (l/== true q)))
              "decoded tableau proof checking must recover equality-triggered reflected calls from system-code"))))))

(deftest sjas-proof-check-accepts-equality-triggered-negative-calls-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/exists-form
                       x
                       (ast/and-form
                         (structural-neg-lit system 'demo x-term)
                         (ast/eq-lit x-term sjas/one))))
            proof (equality-triggered-negative-call-proof)]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               120
                               proof)
                  (l/== true q)))
              "decoded tableau proof checking must recover equality-triggered reflected negative calls from system-code"))))))

(deftest sjas-proof-check-accepts-occurs-check-closures-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0 {:functions {'f 1}})
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (ast/nom x
      (let [x-term (ast/var-term x)
            target (ast/and-form
                     (ast/true-form)
                     (ast/once-forall-form
                       x
                       (ast/eq-lit x-term (ast/app-term 'f x-term))))]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               80
                               '(conj (once-univ (occurs-close))))
                  (l/== true q)))
              "decoded tableau proof checking must consume occurs-check closure evidence object-level"))))))

(deftest sjas-tableau-proof-accepts-occurs-check-closure-certificates
  (let [system (demo-system :willard-sjas-tableau0 {:functions {'f 1}})]
    (ast/nom x
      (let [x-term (ast/var-term x)
            theorem (ast/exists-form
                      x
                      (ast/neq-lit x-term (ast/app-term 'f x-term)))
            theorem-code (sjas/formula-code system theorem)
            certificate (sjas/proof-certificate
                          '(conj (once-univ (occurs-close))))]
        (do
      (let [proofs (query/query-succeeds
                         (:program system)
                         (sjas/tableau-proof (:system-code system)
                                             theorem-code
                                             certificate)
                         1
                         220)]
            (is (successful? proofs)
                "tableau-proof must validate encoded occurs-check closure certificates object-level")
            (is (proof/contains-step? (first-proof proofs) 'occurs-close))))))))

(deftest sjas-proof-predicates-check-reflected-clause-certificates-without-kernel-validator
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/pos-lit (ast/app-term 'demo sjas/one))
        theorem-code (sjas/formula-code system theorem)
        theorem-proof (first-proof
                        (sjas/query-succeeds system theorem
                                             {:proof-limit 1
                                              :fuel 160}))
        certificate (when theorem-proof
                      (sjas/proof-certificate theorem-proof))]
    (is theorem-proof)
    (is (proof/contains-step? theorem-proof 'neg-call))
    (do
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              240))
          "tableau-proof must validate reflected-clause certificates without delegating to the host kernel"))))

(deftest sjas-proof-check-accepts-reflected-negative-call-alternatives-from-system-code
  (ast/nom x
    (let [system (demo-system
                   :willard-sjas-tableau0
                   {:relations {'multi-demo 1}
                    :reflected-clauses [(ast/clause 'multi-demo
                                                    [x]
                                                    (ast/eq-lit (ast/var-term x) sjas/one))
                                        (ast/clause 'multi-demo
                                                    [x]
                                                    (ast/eq-lit (ast/var-term x) sjas/zero))]})
          target (ast/and-form
                   (ast/true-form)
                   (structural-neg-lit system 'multi-demo sjas/one))
          proof '(conj (skip-true (neg-call-alt (alt (refl-close)))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (do
      (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             120
                             proof)
                (l/== true q)))
            "decoded tableau proof checking must recover multi-clause reflected negative calls from system-code")))))

(deftest sjas-proof-check-accepts-minimal-arithmetic-close-certificates
  (testing "the proof tree need only mark arithmetic branch closure; arithmetic evidence is computed by the predicate"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form
                   (ast/true-form)
                   (ast/neg-lit (ast/app-term 'leq sjas/one sjas/one)))
          proof '(conj (skip-true (arith-close)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           80
                           proof)
              (l/== true q)))
          "minimal arithmetic leaves must close by evaluating the SJAS arithmetic relation internally"))))

(deftest sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux
  (testing "the checker can infer local tableau rules from formula-bearing proof nodes"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form (ast/true-form) (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    (ast/true-form)
                    (structural-tableau-node system (ast/false-form))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing nodes should validate by structural Deduction/Closure checks, not rule tags"))))

(deftest sjas-proof-check-accepts-formula-bearing-disjunction-tableaux
  (testing "structural disjunction uses two formula-bearing child branches"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/or-form (ast/false-form) (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form))
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural disjunction proof should not use the split proof-rule tag")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing disjunction nodes should validate by child branch structure"))))

(deftest sjas-proof-check-accepts-formula-bearing-complementary-literal-closures
  (testing "structural literal nodes save branch context and close at a complementary leaf"
    (let [system (demo-system :willard-sjas-tableau0)
          atom (ast/app-term 'wff sjas/zero)
          positive (ast/pos-lit atom)
          negative (ast/neg-lit atom)
          target (ast/and-form positive negative)
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    positive
                    (structural-tableau-node system negative)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural literal proof should not use savefml or close proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing literal leaves should close against saved branch literals"))))

(deftest sjas-proof-check-accepts-guarded-reflected-negative-call-from-system-code
  (testing "guarded negative call evidence is validated from encoded reflected clauses"
    (let [system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'guarded-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'guarded-demo
                                                    []
                                                    (ast/true-form))
                                        (ast/clause 'guarded-demo
                                                    []
                                                    (ast/false-form))]})
          target (ast/and-form
                   (ast/true-form)
                   (structural-neg-lit system 'guarded-demo))
          proof '(conj
                   (skip-true
                     (neg-call-guarded-alt
                       (guarded-alt
                         (guarded-neg-alt
                           (guarded-scope-done)
                           (guarded-seq-last (false-close)))))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (do
      (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             120
                             proof)
                (l/== true q)))
            "decoded tableau proof checking must recover guarded reflected negative calls from system-code")))))

(deftest sjas-tableau-proof-accepts-guarded-reflected-negative-call-certificates
  (let [system (sjas/system
                 {:profile :willard-sjas-tableau0
                  :relations {'guarded-demo 0}
                  :beta []
                  :reflected-clauses [(ast/clause 'guarded-demo
                                                  []
                                                  (ast/true-form))
                                      (ast/clause 'guarded-demo
                                                  []
                                                  (ast/false-form))]})
        stripped-program (assoc (:program system)
                                :clauses nil
                                :clause-list '()
                                :alternative-clause-list '()
                                :guarded-clause-list '())
        theorem (ast/pos-lit (ast/app-term 'guarded-demo))
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(conj
                         (neg-call-guarded-alt
                           (guarded-alt
                             (guarded-neg-alt
                               (guarded-scope-done)
                               (guarded-seq-last (false-close)))))))]
    (do
      (let [proofs (query/query-succeeds
                     stripped-program
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     220)]
        (is (successful? proofs)
            "tableau-proof must validate encoded guarded negative-call certificates from system-code, not compiled clause tables")
        (is (proof/contains-step? (first-proof proofs) 'neg-call-guarded-alt))
        (is (proof/contains-step? (first-proof proofs) 'guarded-alt))))))

(deftest sjas-proof-check-accepts-saturated-guarded-reflected-negative-call-from-system-code
  (testing "saturated guarded evidence is validated from encoded reflected clauses"
    (let [system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'guarded-saturated-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'guarded-saturated-demo
                                                    []
                                                    (ast/true-form))
                                        (ast/clause 'guarded-saturated-demo
                                                    []
                                                    (ast/false-form))]})
          target (ast/and-form
                   (ast/true-form)
                   (structural-neg-lit system 'guarded-saturated-demo))
          proof '(conj
                   (skip-true
                     (neg-call-guarded-alt
                       (guarded-alt
                         (guarded-neg-alt-saturated
                           (guarded-scope-done)
                           (guard-saturation-done)
                           (guarded-call-seq-done)
                           (guarded-residual-seq-last (false-close)))))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (do
      (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             120
                             proof)
                (l/== true q)))
            "decoded tableau proof checking must recover saturated guarded reflected negative calls from system-code")))))

(deftest sjas-tableau-proof-accepts-saturated-guarded-reflected-negative-call-certificates
  (let [system (sjas/system
                 {:profile :willard-sjas-tableau0
                  :relations {'guarded-saturated-demo 0}
                  :beta []
                  :reflected-clauses [(ast/clause 'guarded-saturated-demo
                                                  []
                                                  (ast/true-form))
                                      (ast/clause 'guarded-saturated-demo
                                                  []
                                                  (ast/false-form))]})
        stripped-program (assoc (:program system)
                                :clauses nil
                                :clause-list '()
                                :alternative-clause-list '()
                                :guarded-clause-list '())
        theorem (ast/pos-lit (ast/app-term 'guarded-saturated-demo))
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(conj
                         (neg-call-guarded-alt
                           (guarded-alt
                             (guarded-neg-alt-saturated
                               (guarded-scope-done)
                               (guard-saturation-done)
                               (guarded-call-seq-done)
                               (guarded-residual-seq-last (false-close)))))))]
    (do
      (let [proofs (query/query-succeeds
                     stripped-program
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     220)]
        (is (successful? proofs)
            "tableau-proof must validate encoded saturated guarded negative-call certificates from system-code, not compiled clause tables")
        (is (proof/contains-step? (first-proof proofs) 'guarded-neg-alt-saturated))
        (is (proof/contains-step? (first-proof proofs) 'guarded-residual-seq-last))))))

(deftest sjas-proof-check-accepts-guard-equality-saturation-from-system-code
  (testing "non-empty guard saturation is validated from encoded reflected clauses"
    (let [guarded-body (ast/and-form
                         (ast/eq-lit sjas/one sjas/one)
                         (ast/true-form))
          system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'guard-eq-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'guard-eq-demo
                                                    []
                                                    guarded-body)
                                        (ast/clause 'guard-eq-demo
                                                    []
                                                    (ast/false-form))]})
          target (ast/and-form
                   (ast/true-form)
                   (structural-neg-lit system 'guard-eq-demo))
          proof '(conj
                   (skip-true
                     (neg-call-guarded-alt
                       (guarded-alt
                         (guarded-neg-alt-saturated
                           (guarded-scope-done)
                           (guard-eq (eq-refl) (guard-saturation-done))
                           (guarded-call-seq-done)
                           (guarded-residual-seq-last (false-close)))))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (do
      (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             120
                             proof)
                (l/== true q)))
            "decoded tableau proof checking must recover non-empty reflected equality guards from system-code")))))

(deftest sjas-tableau-proof-accepts-guard-equality-saturation-certificates
  (let [guarded-body (ast/and-form
                       (ast/eq-lit sjas/one sjas/one)
                       (ast/true-form))
        system (sjas/system
                 {:profile :willard-sjas-tableau0
                  :relations {'guard-eq-demo 0}
                  :beta []
                  :reflected-clauses [(ast/clause 'guard-eq-demo
                                                  []
                                                  guarded-body)
                                      (ast/clause 'guard-eq-demo
                                                  []
                                                  (ast/false-form))]})
        stripped-program (assoc (:program system)
                                :clauses nil
                                :clause-list '()
                                :alternative-clause-list '()
                                :guarded-clause-list '())
        theorem (ast/pos-lit (ast/app-term 'guard-eq-demo))
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      '(conj
                         (neg-call-guarded-alt
                           (guarded-alt
                             (guarded-neg-alt-saturated
                               (guarded-scope-done)
                               (guard-eq (eq-refl) (guard-saturation-done))
                               (guarded-call-seq-done)
                               (guarded-residual-seq-last (false-close)))))))]
    (do
      (let [proofs (query/query-succeeds
                     stripped-program
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     220)]
        (is (successful? proofs)
            "tableau-proof must validate encoded guard saturation certificates from system-code, not compiled clause tables")
        (is (proof/contains-step? (first-proof proofs) 'guard-eq))
        (is (proof/contains-step? (first-proof proofs) 'guarded-residual-seq-last))))))

(deftest sjas-proof-check-accepts-existential-guarded-scope-from-system-code
  (testing "existential guarded scope is validated from encoded reflected clauses"
    (ast/nom x
      (let [scoped-body (ast/exists-form x (ast/true-form))
            system (sjas/system
                     {:profile :willard-sjas-tableau0
                      :relations {'guarded-scope-demo 0}
                      :beta []
                      :reflected-clauses [(ast/clause 'guarded-scope-demo
                                                      []
                                                      scoped-body)
                                          (ast/clause 'guarded-scope-demo
                                                      []
                                                      (ast/false-form))]})
            target (ast/and-form
                     (ast/true-form)
                     (structural-neg-lit system 'guarded-scope-demo))
            proof '(conj
                     (skip-true
                       (neg-call-guarded-alt
                         (guarded-alt
                           (guarded-neg-alt
                             (guarded-scope-exists (guarded-scope-done))
                             (guarded-seq-last (false-close)))))))
            check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
        (do
      (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               120
                               proof)
                  (l/== true q)))
              "decoded tableau proof checking must recover existential guarded scope from system-code"))))))

(deftest sjas-tableau-proof-accepts-existential-guarded-scope-certificates
  (ast/nom x
    (let [scoped-body (ast/exists-form x (ast/true-form))
          system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'guarded-scope-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'guarded-scope-demo
                                                    []
                                                    scoped-body)
                                        (ast/clause 'guarded-scope-demo
                                                    []
                                                    (ast/false-form))]})
          stripped-program (assoc (:program system)
                                  :clauses nil
                                  :clause-list '()
                                  :alternative-clause-list '()
                                  :guarded-clause-list '())
          theorem (ast/pos-lit (ast/app-term 'guarded-scope-demo))
          theorem-code (sjas/formula-code system theorem)
          certificate (sjas/proof-certificate
                        '(conj
                           (neg-call-guarded-alt
                             (guarded-alt
                               (guarded-neg-alt
                                 (guarded-scope-exists (guarded-scope-done))
                                 (guarded-seq-last (false-close)))))))]
      (do
      (let [proofs (query/query-succeeds
                       stripped-program
                       (sjas/tableau-proof (:system-code system)
                                           theorem-code
                                           certificate)
                       1
                       220)]
          (is (successful? proofs)
              "tableau-proof must validate encoded guarded-scope certificates from system-code, not compiled clause tables")
          (is (proof/contains-step? (first-proof proofs) 'guarded-scope-exists))
          (is (proof/contains-step? (first-proof proofs) 'guarded-seq-last)))))))

(defn- nested-guarded-call-system
  "Build a reflected-only two-relation system for recursive guarded-call tests."
  []
  (let [outer-body (ast/and-form
                     (ast/pos-lit (ast/app-term 'inner-guarded-demo))
                     (ast/true-form))]
    (sjas/system
      {:profile :willard-sjas-tableau0
       :relations {'outer-guarded-demo 0
                   'inner-guarded-demo 0}
       :beta []
       :reflected-clauses [(ast/clause 'outer-guarded-demo [] outer-body)
                           (ast/clause 'outer-guarded-demo [] (ast/false-form))
                           (ast/clause 'inner-guarded-demo [] (ast/true-form))
                           (ast/clause 'inner-guarded-demo [] (ast/false-form))]})))

(def nested-guarded-call-branch-proof
  '(neg-call-guarded-alt
     (guarded-alt
       (guarded-neg-alt-saturated
         (guarded-scope-done)
         (guard-saturation-done)
         (guarded-call-seq-step
           (neg-call-guarded-alt
             (guarded-alt
               (guarded-neg-alt-saturated
                 (guarded-scope-done)
                 (guard-saturation-done)
                 (guarded-call-seq-done)
                 (guarded-residual-seq-last (false-close)))))
           (guarded-call-seq-done))
         (guarded-residual-seq-last (false-close))))))

(deftest sjas-proof-check-accepts-recursive-guarded-call-sequence-from-system-code
  (testing "recursive guarded call sequences are validated from encoded reflected clauses"
    (let [system (nested-guarded-call-system)
          target (ast/and-form
                   (ast/true-form)
                   (structural-neg-lit system 'outer-guarded-demo))
          proof (list 'conj
                      (list 'skip-true nested-guarded-call-branch-proof))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (do
      (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             160
                             proof)
                (l/== true q)))
            "decoded tableau proof checking must recover recursive guarded call sequences from system-code")))))

(deftest sjas-tableau-proof-accepts-recursive-guarded-call-sequence-certificates
  (let [system (nested-guarded-call-system)
        stripped-program (assoc (:program system)
                                :clauses nil
                                :clause-list '()
                                :alternative-clause-list '()
                                :guarded-clause-list '())
        theorem (ast/pos-lit (ast/app-term 'outer-guarded-demo))
        theorem-code (sjas/formula-code system theorem)
        certificate (sjas/proof-certificate
                      (list 'conj nested-guarded-call-branch-proof))]
    (do
      (let [proofs (query/query-succeeds
                     stripped-program
                     (sjas/tableau-proof (:system-code system)
                                         theorem-code
                                         certificate)
                     1
                     240)]
        (is (successful? proofs)
            "tableau-proof must validate encoded guarded-call sequence certificates from system-code, not compiled clause tables")
        (is (proof/contains-step? (first-proof proofs) 'guarded-call-seq-step))
        (is (proof/contains-step? (first-proof proofs) 'guarded-residual-seq-last))))))

(deftest sjas-tableau-proof-accepts-reflected-negative-call-alternative-certificates
  (ast/nom x
    (let [system (demo-system
                   :willard-sjas-tableau0
                   {:relations {'multi-demo 1}
                    :reflected-clauses [(ast/clause 'multi-demo
                                                    [x]
                                                    (ast/eq-lit (ast/var-term x) sjas/one))
                                        (ast/clause 'multi-demo
                                                    [x]
                                                    (ast/eq-lit (ast/var-term x) sjas/zero))]})
          stripped-program (assoc (:program system)
                                  :clauses nil
                                  :clause-list '()
                                  :alternative-clause-list '()
                                  :guarded-clause-list '())
          theorem (ast/pos-lit (ast/app-term 'multi-demo sjas/one))
          theorem-code (sjas/formula-code system theorem)
          certificate (sjas/proof-certificate
                        '(conj (neg-call-alt (alt (refl-close)))))]
      (do
      (let [proofs (query/query-succeeds
                       stripped-program
                       (sjas/tableau-proof (:system-code system)
                                           theorem-code
                                           certificate)
                       1
                       220)]
          (is (successful? proofs)
              "tableau-proof must validate encoded neg-call-alt certificates from system-code, not compiled clause tables")
          (is (proof/contains-step? (first-proof proofs) 'neg-call-alt))
          (is (proof/contains-step? (first-proof proofs) 'alt)))))))

(deftest sjas-proof-predicates-check-reflected-calls-from-system-code
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/pos-lit (ast/app-term 'demo sjas/one))
        theorem-code (sjas/formula-code system theorem)
        theorem-proof (first-proof
                        (sjas/query-succeeds system theorem
                                             {:proof-limit 1
                                              :fuel 160}))
        certificate (when theorem-proof
                      (sjas/proof-certificate theorem-proof))
        registry (atom (dissoc @(get-in system [:program :sjas/registry])
                               :sjas/reflected-program))
        stripped-program (assoc (:program system)
                                :clauses nil
                                :clause-list '()
                                :alternative-clause-list '()
                                :guarded-clause-list '()
                                :sjas/registry registry)]
    (is theorem-proof)
    (is (proof/contains-step? theorem-proof 'neg-call))
    (do
      (is (successful?
            (query/query-succeeds
              stripped-program
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              240))
          "reflected procedure calls inside proof certificates must be recovered from encoded system-code clauses"))))

(deftest sjas-proof-predicates-check-reflected-calls-without-symbol-registry
  (let [system (demo-system :willard-sjas-tableau0)
        theorem (ast/pos-lit (ast/app-term 'demo sjas/one))
        theorem-code (sjas/formula-code system theorem)
        theorem-proof (first-proof
                        (sjas/query-succeeds system theorem
                                             {:proof-limit 1
                                              :fuel 160}))
        certificate (when theorem-proof
                      (sjas/proof-certificate theorem-proof))
        stripped-program (-> (:program system)
                             (assoc :clauses nil
                                    :clause-list '()
                                    :alternative-clause-list '()
                                    :guarded-clause-list '())
                             (dissoc :sjas/registry))]
    (is theorem-proof)
    (is (proof/contains-step? theorem-proof 'neg-call))
    (is (not (contains? stripped-program :sjas/registry))
        "the regression must remove the finite source symbol table")
    (do
      (is (successful?
            (query/query-succeeds
              stripped-program
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              240))
          "reflected procedure calls must compare encoded symbol ids from system-code, not host symbol names"))))

(deftest sjas-subst-prf-reconstructs-axiom-basis-without-system-registry
  (let [system (demo-system :willard-sjas-tableau0)
        registry (get-in system [:program :sjas/registry])
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        beta-proof (first-proof
                     (sjas/query-succeeds system (:formula beta-record)
                                          {:proof-limit 1
                                           :fuel 96}))
        certificate (sjas/proof-certificate beta-proof)]
    (is beta-proof)
    (swap! registry dissoc :sjas/system-entries)
    (is (not (contains? @registry :sjas/system-entries))
        "the regression must remove the generated host-side proof antecedent")
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            (:code beta-record)
                            (:code beta-record)
                            certificate)
            1
            220))
        "subst-prf must reconstruct the axiom basis from system-code during predicate application")))

(deftest sjas-tableau-proof-accepts-axiom-citation-certificates
  (let [system (demo-system :willard-sjas-level1)
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)
        beta-citation-proofs (query/query-succeeds
                               (:program system)
                               (sjas/tableau-proof (:system-code system)
                                                   (:code beta-record)
                                                   axiom-certificate)
                               1
                               96)]
    (is (successful? beta-citation-proofs))
    (is (proof/contains-step? (first-proof beta-citation-proofs)
                              'sjas-system-beta-axiom)
        "beta axiom citations must be recovered from encoded system-code beta formulas")
    (is (proof/contains-step? (first-proof beta-citation-proofs)
                              'sjas-code-arg)
        "compact axiom citations must expose code constructor byte reads")
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                (:contradiction-code system)
                                axiom-certificate)
            1
            96)))))

(deftest sjas-tableau-proof-cites-fixed-axiom-groups-from-system-code
  (let [system (demo-system :willard-sjas-tableau0)
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)]
    (doseq [[group expected-step] [[:group-zero 'sjas-system-group-zero-axiom]
                                  [:group-one 'sjas-system-group-one-axiom]]]
      (let [record (first (filter #(= group (:group %)) (:axioms system)))
            citation-proofs (query/query-succeeds
                              (:program system)
                              (sjas/tableau-proof (:system-code system)
                                                  (:code record)
                                                  axiom-certificate)
                              1
                              96)
            proof (first-proof citation-proofs)]
        (is (successful? citation-proofs))
        (is (proof/contains-step? proof expected-step)
            (str group " citations must be recovered from the fixed SJAS axiom profile"))
        (is (proof/contains-step? proof 'sjas-code-arg)
            (str group " citations must expose formula-code constructor byte reads"))
        (is (not (proof/contains-step? proof 'sjas-generated-axiom-member))
            (str group " citations should not fall back to generated axiom-member facts"))))))

(deftest sjas-tableau-proof-cites-tableau0-group-three-from-system-code
  (let [system (demo-system :willard-sjas-tableau0)
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)
        citation-proofs (query/query-succeeds
                          (:program system)
                          (sjas/tableau-proof (:system-code system)
                                              (:code (:group-three system))
                                              axiom-certificate)
                          1
                          160)
        proof (first-proof citation-proofs)]
    (is (successful? citation-proofs))
    (is (proof/contains-step? proof 'sjas-system-group-three-axiom)
        "Tableau-0 Group-3 citations must be reconstructed from system-code")
    (is (not (proof/contains-step? proof 'sjas-generated-axiom-member))
        "Tableau-0 Group-3 citations should not fall back to generated axiom-member facts")))

(deftest sjas-tableau-proof-cites-level1-group-three-from-system-code
  (let [system (demo-system :willard-sjas-level1)
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)
        citation-proofs (query/query-succeeds
                          (:program system)
                          (sjas/tableau-proof (:system-code system)
                                              (:code (:group-three system))
                                              axiom-certificate)
                          1
                          200)
        proof (first-proof citation-proofs)]
    (is (successful? citation-proofs))
    (is (proof/contains-step? proof 'sjas-system-level1-group-three-axiom)
        "Level-1 Group-3 citations must validate the fixed-point skeleton from system-code")
    (is (not (proof/contains-step? proof 'sjas-generated-axiom-member))
        "Level-1 Group-3 citations should not fall back to generated axiom-member facts")))

(deftest sjas-tableau-proof-ignores-injected-generated-axiom-member-facts
  (let [system (demo-system :willard-sjas-tableau0)
        registry (get-in system [:program :sjas/registry])
        bogus-code (:contradiction-code system)
        bogus-fact (ast/app-term 'axiom-member (:system-code system) bogus-code)
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)]
    (swap! registry update :sjas/fact-atoms conj bogus-fact)
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                bogus-code
                                axiom-certificate)
            1
            160))
        "tableau-proof must not trust generated axiom-member facts during sjas-axiom citation")))

(deftest sjas-axiom-member-query-ignores-injected-generated-facts
  (let [system (demo-system :willard-sjas-tableau0)
        registry (get-in system [:program :sjas/registry])
        bogus-code (:contradiction-code system)
        bogus-fact (ast/app-term 'axiom-member (:system-code system) bogus-code)]
    (swap! registry update :sjas/fact-atoms conj bogus-fact)
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/axiom-member (:system-code system) bogus-code)
            1
            160))
        "axiom-member/2 queries must be checked from decoded system code, not generated facts")))

(deftest sjas-subst-code-relates-structural-substitution-codes
  (let [system (demo-system :willard-sjas-level1)
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        group3-record (:group-three system)]
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-code (:selfcons-skeleton-code system)
                             (:code group3-record))
            1
            96)))
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-code (:code beta-record)
                             (:code beta-record))
            1
            96)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-code (:system-code system)
                             (:code group3-record))
            1
            96)))))

(deftest ^:slow sjas-subst-code-computes-general-formula-code-substitution
  (let [system (demo-system :willard-sjas-level1)
        {:keys [source-code target-code]} (wff-var0-substitution-codes system)
        shadowed-code (shadowed-var0-substitution-code system)
        registry @(get-in system [:program :sjas/registry])]
    (is (nil? (:sjas/subst-code-entries registry))
        "general Subst must not be implemented by generated substitution entries")
    (is (sjas-code/code-term? source-code))
    (is (sjas-code/code-term? target-code))
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-code source-code target-code)
            1
            240))
        "Subst should replace free v0 with the source formula's own code term")
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-code source-code source-code)
            1
            160))
        "open formulas containing free v0 must not pass through identity")
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-code shadowed-code shadowed-code)
            1
            160))
        "a quantifier binding v0 shadows the substitution variable")))

(deftest ^:slow sjas-subst-code-decodes-user-symbols-without-symbol-registry
  (let [system (demo-system :willard-sjas-level1)
        no-registry-program (dissoc (:program system) :sjas/registry)
        {:keys [source-code target-code]} (demo-var0-substitution-codes system)]
    (is (not (contains? no-registry-program :sjas/registry)))
    (is (successful?
          (query/query-succeeds
            no-registry-program
            (sjas/subst-code source-code target-code)
            1
            240))
        "Subst is structural and must not need the source symbol registry for user application heads")
    (is (empty?
          (query/query-succeeds
            no-registry-program
            (sjas/subst-code source-code source-code)
            1
            160))
        "the no-registry structural path must still reject the unsubstituted open formula")))

(deftest sjas-subst-source-result-computes-explicit-proof-antecedent
  (let [system (demo-system :willard-sjas-tableau0)
        fixed-record (first (filter #(= :group-zero (:group %)) (:axioms system)))
        results (l/run 1 [q]
                  (l/fresh [antecedent proof sigma-out]
                    ((var-get #'sjas-profile/sjas-subst-source-result-antecedento)
                     (:program system)
                     (:code fixed-record)
                     '()
                     sigma-out
                     antecedent
                     proof)
                    (l/== [antecedent proof sigma-out] q)))
        [antecedent proof sigma-out] (first results)]
    (is (= 1 (count results)))
    (is (= '() sigma-out))
    (is (= 'neq (first antecedent)))
    (is (proof/contains-step? proof 'willard-sjas-subst-source-result)
        "subst-prf must have a relation-backed witness for the substituted source sentence")
    (is (proof/contains-step? proof 'sjas-code-arg)
        "the substituted source witness must be decoded from public code bytes")))

(deftest ^:slow sjas-subst-prf-uses-substitution-code-independently-of-theorem-code
  (let [system (demo-system :willard-sjas-tableau0)
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        fixed-record (first (filter #(= :group-zero (:group %)) (:axioms system)))
        certificate (sjas/proof-certificate 'sjas-axiom)
        proofs (query/query-succeeds
                 (:program system)
                 (sjas/subst-prf (:system-code system)
                                 (:code fixed-record)
                                 (:code beta-record)
                                 certificate)
                 1
                 220)
        proof (first-proof proofs)]
    (is (successful? proofs))
    (is (proof/contains-step? proof 'willard-sjas-subst-source-result)
        "subst-prf must explicitly compute the substituted source witness, not merely check that the source code is well formed")))

(deftest sjas-level1-group-three-uses-substitution-proof-vocabulary
  (let [system (demo-system :willard-sjas-level1)
        relations (set (formula-relation-symbols (:formula (:group-three system))))]
    (is (contains? relations 'neg-pair))
    (is (contains? relations 'subst-prf))
    (is (not (contains? relations 'tableau-proof)))))

(deftest sjas-level1-group-three-uses-selfcons-skeleton-code
  (let [system (demo-system :willard-sjas-level1)
        skeleton-code (:selfcons-skeleton-code system)
        subst-atoms (filter #(= 'subst-prf (second %))
                            (formula-atoms (:formula (:group-three system))))]
    (is (sjas-code/code-term? skeleton-code))
    (is (= 2 (count subst-atoms)))
    (is (every? #(= (:system-code system) (nth % 2)) subst-atoms))
    (is (every? #(= skeleton-code (nth % 3)) subst-atoms))
    (is (not-any? #(= (:system-code system) (nth % 3)) subst-atoms))))

(deftest ^:slow sjas-subst-prf-checks-selfcons-fixed-point-certificate
  (let [system (demo-system :willard-sjas-level1)
        group3-record (:group-three system)
        valid (sjas/proof-certificate 'sjas-axiom)]
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/tableau-proof (:system-code system)
                                (:code group3-record)
                                valid)
            1
            160)))
    (is (successful?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            (:selfcons-skeleton-code system)
                            (:code group3-record)
                            valid)
            1
            220)))
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            (:system-code system)
                            (:code group3-record)
                            valid)
            1
            120)))))

(deftest ^:slow sjas-subst-prf-rejects-selfcons-complement-axiom-certificate
  (let [system (sjas/system {:profile :willard-sjas-level1})
        group3 (:formula (:group-three system))
        complement-code (sjas/formula-code system
                                           (normalize/negate-formula group3))
        axiom-certificate (sjas/proof-certificate 'sjas-axiom)]
    (is (empty?
          (query/query-succeeds
            (:program system)
            (sjas/subst-prf (:system-code system)
                            (:selfcons-skeleton-code system)
                            complement-code
                            axiom-certificate)
            1
            220))
        "the Level-1 proof predicate must not treat the complement of its fixed-point SelfCons axiom as an axiom instance")))

(deftest sjas-selfcons-demonstration-uses-substantive-proof-targets
  (testing "the generated self-consistency axiom is a theorem with a checked certificate"
    (let [system (demo-system :willard-sjas-tableau0)
          group3-proof (first-proof
                         (sjas/query-succeeds system
                                              (:formula (:group-three system))
                                              {:proof-limit 1
                                               :fuel 96}))
          group3-certificate (when group3-proof
                               (sjas/proof-certificate group3-proof))
          tableau-proofs (when group3-certificate
                           (query/query-succeeds
                             (:program system)
                             (sjas/tableau-proof (:system-code system)
                                                 (:code (:group-three system))
                                                 group3-certificate)
                             1
                             160))
          tableau-proof (first-proof tableau-proofs)]
      (is group3-proof)
      (is (successful? tableau-proofs))
      (is (proof/contains-step? tableau-proof 'witness))
      (is (proof/contains-step? tableau-proof 'once-univ))))
  (testing "an explicitly inconsistent reflected basis can cite the real contradiction target"
    (let [system (sjas/system {:profile :willard-sjas-tableau0
                               :beta [(ast/false-form)]})
          contradiction-certificate (sjas/proof-certificate 'sjas-axiom)]
      (is (some #(and (= :group-two (:group %))
                      (= (:contradiction-code system) (:code %)))
                (:axioms system))
          "the inconsistent beta basis must encode false as a reflected axiom")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  (:contradiction-code system)
                                  contradiction-certificate)
              1
              160))))))

(deftest large-tableau-proof-zero-limit-does-not-materialize-report
  (testing "the reporting-side proof decoder is not run when no proof is requested"
    (let [system (demo-system :willard-sjas-tableau0)
          large-theorem-code (sjas-code/bytes->code-term
                               (repeat 33 1))
          proof-code (sjas/proof-certificate 'sjas-axiom)]
      (with-redefs [sjas-code/proof-formal-code-term->proof
                    (fn [_]
                      (throw (ex-info "report decoder should not run"
                                      {})))]
        (is (empty?
              (query/query-succeeds
                (:program system)
                (sjas/tableau-proof (:system-code system)
                                    large-theorem-code
                                    proof-code)
                0
                1)))))))

(deftest large-tableau-proof-full-evidence-materializes
  (testing "the public proof predicate can reify a large checked certificate"
    (let [system (demo-system :willard-sjas-tableau0)
          group3-proof (first-proof
                         (sjas/query-succeeds system
                                              (:formula (:group-three system))
                                              {:proof-limit 1
                                               :fuel 96}))
          certificate (sjas/proof-certificate group3-proof)
          proofs (query/query-succeeds
                   (:program system)
                   (sjas/tableau-proof (:system-code system)
                                       (:code (:group-three system))
                                       certificate)
                   1
                   160)
          direct-proof (first-proof proofs)]
      (is group3-proof)
      (is (successful? proofs))
      (is (proof/contains-step? direct-proof 'witness))
      (is (proof/contains-step? direct-proof 'once-univ))
      (is (proof/contains-step? direct-proof 'sjas-system-code-bytes)
          "large proof-predicate evidence must expose the checked public code-reader relation")
      (is (proof/contains-step? direct-proof 'sjas-code-arg)
          "large proof-predicate evidence must reify recursive byte-reader evidence rather than marker summaries"))))

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

(deftest ^:slow sjas-tableau0-selfcons-negating-witness-separates-external-proflog
  (let [system (sjas/system {:profile :willard-sjas-tableau0})
        witness (sjas/tableau-proof (:system-code system)
                                    (:contradiction-code system)
                                    (sjas/proof-certificate 'sjas-axiom))
        external-program (ast/nom s f p
                           (language/compile-program
                             (dissoc (:language system) :proof-profile)
                             [(ast/clause 'tableau-proof [s f p]
                                          (ast/true-form))]))
        external-proofs (binding [gamma/*closed-term-depth-cap* 0
                                  gamma/*closed-term-count-cap* 0]
                          (query/query-succeeds external-program witness 1 80))]
    (is (empty?
          (query/query-succeeds
            (:program system)
            witness
            1
            160))
        "the generated SJAS system must reject the witness negating its Tableau-0 self-consistency axiom")
    (is (successful? external-proofs)
        "ordinary Proflog accepts the same witness when tableau-proof/3 is supplied as an external runtime procedure")))

(deftest sjas-profile-source-audit-rejects-host-proof-checker-route
  (let [profile-source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        builder-source (slurp "src/proflog/willard_sjas.clj")]
    (is (not (re-find #"prove-program-host" profile-source)))
    (is (not (re-find #"host-proof" profile-source)))
    (is (not (re-find #"whole-formula" profile-source)))
    (is (not (re-find #"mini-closed" profile-source)))
    (is (not (re-find #"kernel/prove-programo target" profile-source))
        "SJAS proof predicates must not short-circuit non-axiom certificates through the host proof kernel")
    (is (not (re-find #"sjas-reflected-proof-program" profile-source))
        "SJAS proof predicates must not recover proof-time clauses from a reflected compiled-program registry")
    (is (not (re-find #"program/call-clauseo" profile-source))
        "SJAS proof-predicate procedure calls must decode reflected system-code clauses")
    (is (not (re-find #"sjas-decode-compact-formula-code-staged-proofo" profile-source))
        "SJAS proof predicates must not use staged compact theorem-code decoding")
    (is (not (re-find #"sjas-decode-substitution-target-codeo" profile-source))
        "SJAS substitution predicates must not use staged target-code decoding")
    (is (not (re-find #"ground-formal-code-term source-code" profile-source))
        "SJAS substitution predicates must not use the old broad staged source-code branch")
    (is (not (re-find #"defn- ground-formal-code-term" profile-source))
        "SJAS proof predicates must not project public code bytes through a deterministic host decoder")
    (is (not (re-find #":sjas/symbol-index-entries" profile-source))
        "SJAS proof predicates must not recover formula-code symbols from a generated source table")
    (is (not (re-find #":sjas/symbol-index-entries" builder-source))
        "compiled SJAS programs must not carry a generated source symbol table")
    (is (not (re-find #"program-coding-context" profile-source))
        "proof-facing formula decoding must not reconstruct a host source-signature codebook")
    (is (not (re-find #"sjas-host-symbol-indexo" profile-source))
        "reflected proof checking must match user symbols structurally by encoded index")
    (is (not (re-find #"sjas-reflected-relation-indexo" profile-source))
        "reflected call matching must not fall back to host relation names")
    (is (not (re-find #"code-byte-term->byte" profile-source))
        "compact code byte terms must not be decoded through a host lookup table")
    (is (not (re-find #"code-byte-term-entries" profile-source))
        "compact code byte terms must be interpreted arithmetically rather than by generated byte-term facts")
    (is (not (re-find #"ground-compact-code-args" profile-source))
        "compact public code terms must not use a host-ground argument fast path")
    (is (not (re-find #"ground-code-argso" profile-source))
        "compact public code byte evidence must use the ordinary object code-argument relation")
    (is (not (re-find #"compact-system-has-false-beta" profile-source))
        "false axiom-citation rejection must not use a host system-code scanner")
    (is (not (re-find #"false-axiom-tableau-query-status" profile-source))
        "tableau-proof must reject absent contradiction axioms through SJAS relations")
    (is (not (re-find #"skip-host-formula-bytes" profile-source))
        "large semantic system-code scans must use object-level structural relations")
    (is (not (re-find #"sjas-code/code-term-bytes system-code" profile-source))
        "proof-profile system codes must not be decoded by host byte extraction")
    (is (not (re-find #"sjas-decode-proof-formula-code-detail-proofo" profile-source))
        "theorem-code decoding inside proof predicates must not use a separate large-code decoder")
    (is (not (re-find #"detailed-theorem-code-byte-limit" profile-source))
        "theorem-code evidence must not switch behavior at a performance byte limit")
    (is (not (re-find #"large-compact-code-term\\?" profile-source))
        "large theorem codes must be decoded by the same object relation as small theorem codes")
    (is (not (re-find #"sjas-public-code-bytes-summaryo" profile-source))
        "large system/formula code paths must use full object code-reader evidence, not a semantic summary relation")
    (is (not (re-find #"sjas-public-code-bytes-markero" profile-source))
        "large system/formula code paths must not collapse recursive byte-reader evidence into a marker")
    (is (not (re-find #"sjas-decode-proof-formula-code-markero" profile-source))
        "theorem-code decoding inside proof predicates must return full byte-reader evidence")
    (is (not (re-find #"decode-proof-code-kindo" profile-source))
        "proof-code decoding inside proof predicates must return full byte-reader evidence")
    (is (not (re-find #"code-read-marker-o" profile-source))
        "proof predicates must not replace code-reader derivations with compact marker evidence")
    (is (not (re-find #"defn- ground-negated-subst-code-args" profile-source))
        "substitution proof predicates must not destructure top-level atoms through host-ground helpers")
    (is (not (re-find #"defn- ground-negated-app-args" profile-source))
        "proof-profile predicates must not destructure top-level atoms through host-ground helpers")
    (is (not (re-find #"defn- ground-negated-relation" profile-source))
        "proof-profile predicate dispatch must not inspect host-ground relation symbols")
    (is (not (re-find #"direct-negated-profile" profile-source))
        "SJAS proof predicates must be reached through the ordinary relational theory rule, not a direct host-ground entrypoint")
    (is (not (re-find #"hide-sjas-clauses-from-generic-sidecars" profile-source))
        "SJAS proof search must not hide host runtime clause metadata to steer generic sidecar scheduling")
    (is (not (re-find #"defn- ground-code-byte-termo" profile-source))
        "compact code byte decoding must not use a host projector for ground byte terms")
    (is (not (re-find #"equality/walko term '\(\) walked" profile-source))
        "compact code byte decoding has no substitution state and must parse numeral structure directly")
    (is (not (re-find #"bits->canonical-termo tail tail-term tail-proof\)\n\s+\(== \(list 'app 'dbl tail-term\) term\)" profile-source))
        "canonical byte generation must reject mismatched public roots before recursive numeral construction")
    (is (not (re-find #"defn- ground-code-constructoro" profile-source))
        "compact code constructor decoding must not use a host projector for ground constructors")
    (is (not (re-find #"project \[" profile-source))
        "SJAS proof-profile code readers must not leave host-side project guards in the internalized proof path")
    (is (not (re-find #"compact-code-bytes-no-walko" profile-source))
        "compact public code reading must not bypass equality walking through a host-mode helper")
    (is (not (re-find #"compact-code-term-byte-count" profile-source))
        "public code-format dispatch must not inspect host-ground compact constructors")
    (is (not (re-find #"\\(seq\\? term\\)" profile-source))
        "compact byte decoding must not branch on host-ground term shape")
    (is (not (re-find #"symbol\\? constructor" profile-source))
        "compact code constructor decoding must use the finite object relation, not host symbol inspection")
    (is (not (re-find #"integer\\? byte-count" profile-source))
        "compact code constructor decoding must use the finite object relation, not host integer inspection")
    (is (not (re-find #"sjas-ground-structural-negated-theorem-proofo" profile-source))
        "proof predicates must not keep a separate ground-direct theorem decoder")
    (is (not (re-find #"sjas-arithmetic-branch-closeo" profile-source))
        "proof predicates must not bypass tableau state validation with arithmetic branch shortcuts")
    (is (not (re-find #"sjas-top-conj-" profile-source))
        "proof predicates must not bypass tableau state validation with top-conjunction shortcuts")
    (is (not (re-find #"sjas-negated-theorem-branch-proof-checko" profile-source))
        "proof predicates must not bypass tableau state validation by focusing the negated theorem directly")
    (is (not (re-find #"(?s)defn- sjas-tableau-proof-closeo.*?\(conda" profile-source))
        "tableau-proof proof-code classification must be a relation, not a committed-choice scheduler")
    (is (not (re-find #"(?s)defn- sjas-axiom-membero.*?\(conda" profile-source))
        "axiom-member group selection must be an ordinary finite relation, not committed-choice search control")
    (is (not (re-find #"(?s)defn- reflected-call-alternatives-in-clauseso(?:(?!\n\(defn-).)*\(conda" profile-source))
        "reflected negative-call alternative collection must use explicit encoded-clause match/nonmatch relations")
    (is (not (re-find #"(?s)defn- reflected-call-guarded-alternatives-in-clauseso(?:(?!\n\(defn-).)*\(conda" profile-source))
        "guarded reflected alternative collection must use explicit encoded-clause match/nonmatch relations")
    (is (= 2 (count (re-seq #"\(kernel/prove-programo" profile-source)))
        "the ordinary kernel may remain only as the public proof-search engine, not as an internal proof-predicate validator")
    (is (not (re-find #"compact-false-formula-code" profile-source))
        "axiom membership must not special-case a precomputed host formula code")
    (is (not (re-find #"large-non-axiom-tableau-proof-query" profile-source))
        "large tableau-proof queries must not replace decoded proof evidence with an acceptance summary")
    (is (not (re-find #"large-tableau-proof-summary" profile-source))
        "large tableau-proof queries must return the object-level proof evidence they checked")
    (is (not (re-find #"large-tableau-proof-report" profile-source))
        "large tableau-proof queries must not synthesize proof reports after a truth-mode acceptance check")
    (is (not (re-find #"direct-profile-accepted\\?" profile-source))
        "direct profile predicates must reify the object-level proof evidence they accept")
    (is (not (re-find #"reported-decoded-proof-o" profile-source))
        "proof reporting must not hide decoded non-axiom proof trees behind a summary marker")
    (is (not (re-find #"proof-formal-code-term->proof" profile-source))
        "large proof reporting must decode proof-code trees through the SJAS proof-code relation")
    (is (not (re-find #"defn- ground-u-grounding-substitution-bytes" profile-source))
        "SJAS substitution predicates must not recover U-Grounding formula bytes through a host projector")
    (is (not (re-find #"sjas-code/code-term-bytes term" profile-source))
        "compact public code terms must be read through the object code-byte relation")
    (is (not (re-find #"ground-u-grounding-code-term-bytes" profile-source))
        "U-Grounding public code terms must be read through the object numeral relation")
    (is (not (re-find #"sjas-subst-source-codeo" profile-source))
        "subst-prf must compute the substituted source witness instead of using a source-only well-formedness shortcut")
    (is (not (re-find #"mini-closed" builder-source)))
    (is (not (re-find #"malformed" profile-source)))
    (is (not (re-find #"malformed" builder-source)))
    (is (not (re-find #"defn- mult-facts" builder-source)))
    (is (not (re-find #"defn- order-facts" builder-source)))))
