(ns proflog.willard-sjas-test
  (:require [clojure.core.logic :as l]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [proflog.answers :as answers]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
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

(defn- substring-count
  "Count non-overlapping appearances of `needle` in `haystack`.

   Source-audit tests use this to guard proof-search scheduling structure that
   is otherwise hard to observe from a single success/failure query."
  [haystack needle]
  (loop [offset 0
         hits 0]
    (if-let [index (str/index-of haystack needle offset)]
      (recur (+ index (count needle)) (inc hits))
      hits)))

(defn- private-defn-contains?
  "True when private function `defn-name` contains literal `needle`.

   This intentionally scans only until the next top-level form. It keeps source
   audits focused on one helper instead of rejecting dynamic uses elsewhere in
   the SJAS profile."
  [source defn-name needle]
  (let [quoted-start (java.util.regex.Pattern/quote (str "(defn- " defn-name))
        quoted-needle (java.util.regex.Pattern/quote needle)
        pattern (re-pattern (str "(?s)"
                                 quoted-start
                                 "(?:(?!\\n\\().)*"
                                 quoted-needle))]
    (boolean (re-find pattern source))))

(defn- private-defn-source
  "Return the source slice for private function `defn-name`."
  [source defn-name]
  (let [start-token (str "(defn- " defn-name)
        start (str/index-of source start-token)
        end (str/index-of source "\n(defn" (inc start))]
    (subs source start end)))

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

(def ^:private zero-numeral-symbol (symbol "0"))
(def ^:private one-numeral-symbol (symbol "1"))

(defn- sjas-numeral-term?
  "True when `term` is written in the public binary SJAS numeral vocabulary."
  [term]
  (loop [work (list term)]
    (if (seq work)
      (let [current-term (first work)
            remaining-work (next work)]
        (if (= 'app (ast/tag-of current-term))
          (let [head (second current-term)
                args (nnext current-term)]
            (cond
              (= zero-numeral-symbol head) (and (empty? args)
                                                (recur remaining-work))
              (= one-numeral-symbol head) (and (empty? args)
                                               (recur remaining-work))
              (= 'dbl head) (and (= 1 (count args))
                                 (recur (conj remaining-work (first args))))
              (= 'add head) (and (= 2 (count args))
                                 (recur (conj (conj remaining-work (second args))
                                              (first args))))
              :else false))
          false))
      true)))

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

(defn- conjunction-leaves
  [formula]
  (if (= 'and (ast/tag-of formula))
    (concat (conjunction-leaves (second formula))
            (conjunction-leaves (nth formula 2)))
    [formula]))

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
      (is (= 4 (get-in lang [:relations 'subst-prf])))
      (is (= 3 (get-in lang [:relations 'dsjas-tableau-proof])))
      (is (= 4 (get-in lang [:relations 'dsjas-subst-prf]))))))

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

(deftest sjas-formula-classifiers-close-delta-star-0-under-connectives
  (testing "Delta-star-0 is closed under not and implies like the code grammar"
    (ast/nom x y
      (let [x-term (ast/var-term x)
            y-term (ast/var-term y)
            implies-matrix (ast/implies-form
                             (ast/eq-lit x-term sjas/one)
                             (sjas/leq x-term sjas/two))
            not-matrix (ast/not-form (sjas/lt x-term sjas/three))]
        (is (sjas/delta-star-0? implies-matrix))
        (is (sjas/delta-star-0? not-matrix))
        (is (sjas/pi-star-1? (ast/forall-form x implies-matrix)))
        (is (not (sjas/delta-star-0?
                   (ast/implies-form
                     (ast/exists-form y (sjas/lt y-term x-term))
                     (ast/eq-lit x-term sjas/one)))))))))

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

(defn- app-occurrence-count
  "Count explicit function/relation application nodes in a decoded formula.

   ADR-0102 uses this as a concrete lower-bound witness for Willard's `J`
   measure. It deliberately counts syntax-tree occurrences, not distinct
   function symbols, because the Conventional Tableaux Encoding Requirement is
   occurrence-sensitive."
  [formula]
  (count
    (filter #(and (sequential? %) (= 'app (first %)))
            (tree-seq sequential? seq formula))))

(defn- repeated-unary-app-term
  "Build `depth` nested applications of `function-symbol` around `base-term`."
  [function-symbol depth base-term]
  (nth (iterate #(ast/app-term function-symbol %) base-term) depth))

(defn- formula-code-bytes
  [system formula]
  (or (sjas-code/code-term-bytes (sjas/formula-code system formula))
      (sjas-code/u-grounding-code-term-bytes (sjas/formula-code system formula))))

(defn- formal-code-term-bytes
  [term]
  (or (sjas-code/code-term-bytes term)
      (sjas-code/u-grounding-code-term-bytes term)))

(defn- compact-code-term-for
  "Return the compact public code term with the same byte payload as `term`."
  [term]
  (sjas-code/bytes->code-term (formal-code-term-bytes term)))

(defn- canonical-formula-code-bytes
  [system canonical-formula]
  (sjas-code/code-term-bytes
    (sjas-code/canonical-formula-code-term (:coding-context system)
                                           canonical-formula)))

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

(defn- canonical-structural-tableau-node
  "Encode a formula-bearing node from canonical formula-code syntax.

   This is needed for child nodes that mention variables or parameters
   introduced by a parent quantifier. Runtime AST noms are not stable proof-code
   bytes; canonical `v0`, `v1`, ... payloads are."
  [system canonical-formula & children]
  (let [bytes (canonical-formula-code-bytes system canonical-formula)]
    (is (pos? (count bytes)))
    (is (< (count bytes) 64)
        "the current structural-node slice uses one proof byte for formula length")
    (apply list (concat [(count bytes)] bytes children))))

(defn- structural-byte-list-tableau-node
  "Encode one formula-bearing node with its formula bytes as a proof byte list.

   This shape is needed once formula-code payloads exceed the one-byte flat
   prefix supported by the first structural-node fragment."
  [system formula & children]
  (let [bytes (formula-code-bytes system formula)]
    (is (pos? (count bytes)))
    (apply list (cons (apply list bytes) children))))

(defn- structural-flex-tableau-node
  "Encode a formula-bearing node, using a proof byte list for wide formulas."
  [system formula & children]
  (let [bytes (formula-code-bytes system formula)]
    (is (pos? (count bytes)))
    (if (< (count bytes) 64)
      (apply list (concat [(count bytes)] bytes children))
      (apply list (cons (apply list bytes) children)))))

(defn- canonical-structural-flex-tableau-node
  "Encode a canonical formula-bearing node, using a proof byte list if needed."
  [system canonical-formula & children]
  (let [bytes (canonical-formula-code-bytes system canonical-formula)]
    (is (pos? (count bytes)))
    (if (< (count bytes) 64)
      (apply list (concat [(count bytes)] bytes children))
      (apply list (cons (apply list bytes) children)))))

(defn- replace-var-term
  [formula binding-nom replacement]
  (walk/postwalk
    (fn [node]
      (if (= (ast/var-term binding-nom) node)
        replacement
        node))
    formula))

(defn- replace-canonical-var-term
  [formula var-symbol replacement]
  (walk/postwalk
    (fn [node]
      (if (= (list 'var var-symbol) node)
        replacement
        node))
    formula))

(declare decoded-formula->canonical)

(defn- decoded-term->canonical
  [term env]
  (case (first term)
    var (list 'var (get env (second term) (second term)))
    par (list 'par (get env (second term) (second term)))
    app (apply list
               'app
               (second term)
               (map #(decoded-term->canonical % env) (nnext term)))))

(defn- decoded-formula->canonical
  [formula env]
  (case (first formula)
    true formula
    false formula
    pos (list 'pos (decoded-term->canonical (second formula) env))
    neg (list 'neg (decoded-term->canonical (second formula) env))
    eq (list 'eq
             (decoded-term->canonical (second formula) env)
             (decoded-term->canonical (nth formula 2) env))
    neq (list 'neq
              (decoded-term->canonical (second formula) env)
              (decoded-term->canonical (nth formula 2) env))
    and (list 'and
              (decoded-formula->canonical (second formula) env)
              (decoded-formula->canonical (nth formula 2) env))
    or (list 'or
             (decoded-formula->canonical (second formula) env)
             (decoded-formula->canonical (nth formula 2) env))
    not (list 'not (decoded-formula->canonical (second formula) env))
    implies (list 'implies
                  (decoded-formula->canonical (second formula) env)
                  (decoded-formula->canonical (nth formula 2) env))
    forall (let [tie (second formula)
                 binder (:binding-nom tie)
                 canonical-binder (get env binder 'v0)]
             (list 'forall
                   canonical-binder
                   (decoded-formula->canonical
                     (:body tie)
                     (assoc env binder canonical-binder))))
    once-forall (let [tie (second formula)
                      binder (:binding-nom tie)
                      canonical-binder (get env binder 'v0)]
                  (list 'once-forall
                        canonical-binder
                        (decoded-formula->canonical
                          (:body tie)
                          (assoc env binder canonical-binder))))
    exists (let [tie (second formula)
                 binder (:binding-nom tie)
                 canonical-binder (get env binder 'v1)]
             (list 'exists
                   canonical-binder
                   (decoded-formula->canonical
                     (:body tie)
                     (assoc env binder canonical-binder))))))

(defn- conjunction-path-tableau-proof
  "Wrap `leaf-proof` in formula-bearing nodes for the conjunction path to leaf.

   The structural checker can select either immediate conjunct after one
   conjunction step, but it cannot jump over nested conjunction nodes. The
   SelfCons certificate therefore has to represent the spine from `AxiomConj`
   to the Group-3 antecedent explicitly."
  [system formula target-leaf leaf-proof]
  (cond
    (= formula target-leaf)
    leaf-proof

    (= 'and (ast/tag-of formula))
    (let [left (second formula)
          right (nth formula 2)
          next-formula (if (some #(= target-leaf %) (conjunction-leaves left))
                         left
                         right)]
      (structural-flex-tableau-node
        system
        formula
        (conjunction-path-tableau-proof system
                                        next-formula
                                        target-leaf
                                        leaf-proof)))

    :else
    (throw (ex-info "target leaf is not reachable through conjunctions"
                    {:formula formula
                     :target-leaf target-leaf}))))

(defn- proof-side-antecedent-formula
  "Build the host-side formula shape reconstructed by `tableau-proof/3`.

   The public proof predicate does not check the raw surface axiom conjunction:
   it decodes each finite-system formula through the kernel's proof-antecedent
   relation, which eliminates implication into NNF and treats antecedent
   universals as single-use branch formulas. The SelfCons fixture must encode
   formula-bearing proof nodes in that same shape so the structural checker can
   compare child nodes directly against the object target it reconstructs."
  [formula]
  (letfn [(once-forall-antecedents [formula]
            (case (ast/tag-of formula)
              and (ast/and-form
                    (once-forall-antecedents (second formula))
                    (once-forall-antecedents (nth formula 2)))
              or (ast/or-form
                   (once-forall-antecedents (second formula))
                   (once-forall-antecedents (nth formula 2)))
              forall (ast/once-forall-form
                       (:binding-nom (second formula))
                       (once-forall-antecedents (:body (second formula))))
              once-forall (ast/once-forall-form
                             (:binding-nom (second formula))
                             (once-forall-antecedents (:body (second formula))))
              exists (ast/exists-form
                       (:binding-nom (second formula))
                       (once-forall-antecedents (:body (second formula))))
              formula))]
    (once-forall-antecedents (normalize/to-nnf formula))))

(declare selfcons-object-targeto)

(defn- selfcons-formula-bearing-proof
  "Build a formula-bearing tableau proof of the system's Group-3 SelfCons axiom.

   The proof is a structural tableau tree, not the compact `sjas-axiom`
   citation. It decomposes `AxiomConj(s) /\\ not(SelfCons)` far enough to
   select the Group-3 antecedent, saves its negated `tableau-proof` atom, then
   expands the negated theorem's existential and closes against the saved
   complement."
  [system]
  (let [axiom-formula (:axiom-formula system)
        selfcons (:formula (:group-three system))
        neg-selfcons (normalize/negate-formula selfcons)
        object-target (first (l/run 1 [target]
                               (selfcons-object-targeto system target)))
        object-axiom-formula (second object-target)
        object-neg-selfcons (nth object-target 2)
        group-three-antecedent (last (conjunction-leaves axiom-formula))
        proof-axiom-formula (proof-side-antecedent-formula axiom-formula)
        proof-group-three-antecedent (last (conjunction-leaves proof-axiom-formula))
        proof-target (ast/and-form proof-axiom-formula neg-selfcons)
        object-group-three-antecedent (nth object-axiom-formula 2)
        canonical-group-three (decoded-formula->canonical
                                object-group-three-antecedent
                                {})
        canonical-neg-selfcons (decoded-formula->canonical object-neg-selfcons {})
	        saved-neg-atom (nth canonical-group-three 2)
	        closing-pos-atom (replace-canonical-var-term
	                           (nth canonical-neg-selfcons 2)
	                           'v1
	                           (list 'par 'v0))
        group-three-proof (canonical-structural-flex-tableau-node
                            system
                            canonical-group-three
                            (canonical-structural-flex-tableau-node
                              system
                              saved-neg-atom
                              (canonical-structural-flex-tableau-node
                                system
                                canonical-neg-selfcons
                                (canonical-structural-flex-tableau-node
                                  system
                                  closing-pos-atom))))
        axiom-proof (conjunction-path-tableau-proof
                      system
                      proof-axiom-formula
                      proof-group-three-antecedent
                      group-three-proof)
        proof (structural-flex-tableau-node system proof-target axiom-proof)]
    {:target proof-target
     :proof proof}))

(defn- selfcons-core-formula-bearing-proof
  "Build the minimal Group-3/negated-SelfCons tableau core.

   This strips away `AxiomConj(s)` so focused tests can distinguish quantifier
   and literal-closure failures from agenda-selection failures over the full
   system antecedent."
  [system]
  (let [axiom-formula (:axiom-formula system)
        selfcons (:formula (:group-three system))
        neg-selfcons (normalize/negate-formula selfcons)
        group-three-antecedent (last (conjunction-leaves axiom-formula))
        group-three-binding (:binding-nom (second group-three-antecedent))
        neg-selfcons-binding (:binding-nom (second neg-selfcons))
        saved-neg-atom (replace-var-term
                         (:body (second group-three-antecedent))
                         group-three-binding
                         (list 'var 'v0))
	        closing-pos-atom (replace-var-term
	                           (:body (second neg-selfcons))
	                           neg-selfcons-binding
	                           (list 'par 'v0))
        target (ast/and-form group-three-antecedent neg-selfcons)]
    {:target target
     :proof (structural-flex-tableau-node
              system
              target
              (structural-flex-tableau-node
                system
                group-three-antecedent
                (canonical-structural-flex-tableau-node
                  system
                  saved-neg-atom
                  (structural-flex-tableau-node
                    system
                    neg-selfcons
                    (canonical-structural-flex-tableau-node
                      system
                      closing-pos-atom)))))}))

(defn- selfcons-object-targeto
  "Reconstruct the object-level SelfCons tableau target inside the logic query."
  [system target]
  (let [axiom-formula-coreo (var-get #'sjas-profile/sjas-system-axiom-formula-coreo)
        negated-theorem-coreo (var-get #'sjas-profile/sjas-structural-negated-theorem-coreo)]
    (l/fresh [axiom-formula neg-theorem sigma-out]
      (axiom-formula-coreo (:program system)
                           (:system-code system)
                           axiom-formula)
      (negated-theorem-coreo (:program system)
                             (:code (:group-three system))
                             '()
                             sigma-out
                             neg-theorem)
      (l/== (list 'and axiom-formula neg-theorem) target))))

(defn- level1-selfcons-formula
  "Test-local copy of the Level-1 Group-3 schema used for malformed-code probes."
  [system-code substitution-code]
  (ast/nom x y p q
    (ast/forall-form
      x
      (ast/forall-form
        y
        (ast/forall-form
          p
          (ast/forall-form
            q
            (ast/or-form
              (ast/neg-lit
                (ast/app-term 'pi-star-1-code
                              (ast/var-term x)))
              (ast/or-form
                (ast/neg-lit
                  (ast/app-term 'neg-pair
                                (ast/var-term x)
                                (ast/var-term y)))
                (ast/or-form
                  (ast/neg-lit
                    (ast/app-term 'dsjas-subst-prf
                                  system-code
                                  substitution-code
                                  (ast/var-term x)
                                  (ast/var-term p)))
                  (ast/neg-lit
                    (ast/app-term 'dsjas-subst-prf
                                  system-code
                                  substitution-code
                                  (ast/var-term y)
                                  (ast/var-term q))))))))))))

(defn- right-nested-true-chain
  [depth]
  (if (zero? depth)
    (ast/false-form)
    (ast/and-form (ast/true-form)
                  (right-nested-true-chain (dec depth)))))

(deftest sjas-tableau0-selfcons-targets-zero-equals-one
  (testing "ordinary Tableau-0 SelfCons uses Willard's minimal 0=1 target"
    (let [system (demo-system :willard-sjas-tableau0)
          zero-one (ast/eq-lit sjas/zero sjas/one)
          zero-one-code (sjas/formula-code system zero-one)
          false-code (sjas/formula-code system (ast/false-form))
          zero-one-bytes [5 25 0 0 25 1 0 1]]
      (is (= zero-one-code (:contradiction-code system))
          "Tableau-0 contradiction-code must denote the formula 0=1")
      (is (not= false-code (:contradiction-code system))
          "primitive false is a tableau closure formula, not Willard's minimal SelfCons target")
      (is (= zero-one-bytes (formula-code-bytes system zero-one)))
      (is (= zero-one-bytes (formal-code-term-bytes (:contradiction-code system)))))))

(deftest sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target
  (testing "kernel-side AxiomConj(s) reconstruction uses the same 0=1 target"
    (let [system (demo-system :willard-sjas-tableau0)
          zero-one-code (sjas/formula-code system
                                           (ast/eq-lit sjas/zero sjas/one))
          target (first (l/run 1 [target]
                          (selfcons-object-targeto system target)))
          axiom-formula (second target)
          atoms (formula-atoms axiom-formula)
          measured-proof-atoms (filter #(= 'dsjas-tableau-proof (second %)) atoms)
          bare-proof-atoms (filter #(= 'tableau-proof (second %)) atoms)]
      (is target
          "the test must reconstruct the object target through the proof predicate relation")
      (is (seq measured-proof-atoms)
          "AxiomConj(s) must include a measured reconstructed Tableau-0 Group-3 atom")
      (is (empty? bare-proof-atoms)
          "SelfCons must not quantify bare tableau-proof/3 proof codes")
      (is (some #(= zero-one-code (nth % 3)) measured-proof-atoms)
          "reconstructed Tableau-0 Group-3 must assert no measured proof of 0=1"))))

(deftest sjas-tableau0-group-three-rejects-wrong-public-code-representation
  (testing "Group-3 cites the presented public s term, not just equivalent bytes"
    (ast/nom p
      (let [system (demo-system :willard-sjas-tableau0
                                {:code-format :u-grounding})
            compact-system-code (compact-code-term-for (:system-code system))
            compact-contradiction-code (compact-code-term-for
                                         (:contradiction-code system))
            wrong-representation-formula
            (ast/forall-form
              p
              (ast/neg-lit
                (ast/app-term 'dsjas-tableau-proof
                              compact-system-code
                              compact-contradiction-code
                              (ast/var-term p))))
            wrong-representation-code (sjas/formula-code
                                        system
                                        wrong-representation-formula)
            axiom-member-coreo (var-get #'sjas-profile/sjas-axiom-member-coreo)]
        (is (sjas-numeral-term? (:system-code system))
            "the regression must exercise the U-Grounding public code format")
        (is (not= wrong-representation-code
                  (:code (:group-three system)))
            "the theorem code must differ by the embedded public code representation")
        (is (empty?
              (l/run 1 [q]
                (axiom-member-coreo (:program system)
                                    (:system-code system)
                                    wrong-representation-code)
                (l/== true q)))
            "axiom-member must reject a Group-3 formula that embeds compact codes for a U-Grounding system")))))

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

(deftest sjas-system-builder-axiom-formula-includes-fixed-group-one
  (testing "the public theorem antecedent agrees with the fixed axiom basis"
    (let [system (demo-system :willard-sjas-tableau0)
          code-canonical-formula (var-get #'sjas/code-canonical-formula)
          axiom-leaves (conjunction-leaves (:axiom-formula system))
          group-one-formulas (map (comp code-canonical-formula :formula)
                                  (filter #(= :group-one (:group %))
                                          (:axioms system)))]
      (is (= 3 (count group-one-formulas))
          "the regression must exercise the fixed Group-1 axiom records")
      (doseq [formula group-one-formulas]
        (is (some #(= formula %) axiom-leaves)
            "the theorem antecedent must include every fixed Group-1 formula accepted by axiom-member/2")))))

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

(deftest sjas-tableau0-selfcons-godel-code-is-publicly-printable
  (testing "the concrete ordinary-tableau Group-3 formula code is exposed as a numerical Godel code"
    (let [system (demo-system :willard-sjas-tableau0)
          report (sjas/selfcons-godel-code-report system)
          group3-code (:code (:group-three system))
          group3-bytes (sjas-code/code-term-bytes group3-code)
          printed (with-out-str
                    (sjas/print-selfcons-godel-code system))]
      (is (= :willard-sjas-tableau0 (:profile report)))
      (is (= :compact (:code-format report)))
      (is (= :group-three (:group report)))
      (is (= group3-code (:code-term report)))
      (is (= group3-bytes (:bytes report))
          "the report must decode the generated formula code term rather than use a proof-predicate shortcut")
      (is (= (sjas-code/bytes->natural group3-bytes)
             (:godel-number report)))
      (is (pos? (:godel-number report)))
      (is (= (str (:godel-number report) "\n") printed))))
  (testing "the printed code follows the encoded self-reference, not unrelated runtime clauses"
    (let [base (demo-system :willard-sjas-tableau0)
          beta-changed (demo-system
                         :willard-sjas-tableau0
                         {:beta [(ast/neq-lit sjas/one sjas/zero)]})
          external-changed (demo-system
                             :willard-sjas-tableau0
                             {:external-clauses [(external-demo-clause 'renamed-external)]})
          code-of #(-> % sjas/selfcons-godel-code-report :godel-number)]
      (is (not= (code-of base) (code-of beta-changed))
          "changing beta must change the self-consistency sentence code")
      (is (= (code-of base) (code-of external-changed))
          "external runtime-only clauses must not change the encoded IS#_D(beta) SelfCons sentence"))))

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

(deftest sjas-object-symbol-index-decoding-separates-reserved-and-user-symbols
  (let [decode-symbol (var-get #'sjas-profile/sjas-object-symbol-indexo)
        tableau-proof-index (sjas-code/reserved-symbol->index 'tableau-proof)]
    (is (= '(tableau-proof)
           (l/run 1 [q]
             (decode-symbol tableau-proof-index q)))
        "reserved proof-predicate symbols decode to their semantic relation name")
    (is (empty?
          (l/run 1 [q]
            (decode-symbol tableau-proof-index
                           (list 'sym tableau-proof-index))
            (l/== true q)))
        "reserved proof-predicate symbols must not also decode as generic user symbols")))

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
          (is (proof/contains-step? proof 'willard-sjas-proof-check)
              "tableau-proof should close through the SJAS proof predicate")
          (is (proof/contains-step? proof 'sjas-ug-code-byte-cons)
              "U-Grounding citation answers carry the byte-reader evidence inside the membership proof (ADR-0091)")
          (is (proof/contains-step? proof 'sjas-ug-code-canonical-byte)
              "U-Grounding citation answers carry canonical-byte evidence inside the membership proof (ADR-0091)")
          (is (not (proof/contains-step? proof 'sjas-axiom))
              "the supplied proof-code is checked, not copied into answer evidence")
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

(deftest sjas-proof-code-decoder-checks-formula-bearing-tableau-nodes
  (testing "encoded structural proof certificates are consumed by the SJAS proof checker"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form (ast/true-form) (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    (ast/true-form)
                    (structural-tableau-node system (ast/false-form))))
          certificate (sjas/proof-certificate proof)
          decode-proof (var-get #'sjas-profile/decode-non-sjas-axiom-proof-codeo)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the encoded structural proof should not rely on symbolic proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (l/fresh [decoded-proof sigma-out proof-bytes proof-read-proof]
                (decode-proof certificate
                              '()
                              sigma-out
                              proof-bytes
                              decoded-proof
                              proof-read-proof)
                (check-proof (:program system)
                             (:system-code system)
                             target
                             30
                             decoded-proof)
                (l/== true q))))
          "public proof-code decoding should preserve formula-bearing proof nodes for object-level checking"))))

(deftest sjas-proof-check-accepts-byte-list-formula-bearing-false-nodes
  (testing "formula-bearing nodes may carry formula bytes as a proof byte list"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/false-form)
          proof (structural-byte-list-tableau-node system target)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof)))
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "the structural checker should accept a byte-list encoded formula node"))))

(deftest sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes
  (testing "encoded structural proof certificates support formula byte payloads beyond the flat one-byte node fragment"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form
                   (ast/false-form)
                   (right-nested-true-chain 32))
          target-bytes (formula-code-bytes system target)
          proof (structural-byte-list-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form)))
          certificate (sjas/proof-certificate proof)
          decode-proof (var-get #'sjas-profile/decode-non-sjas-axiom-proof-codeo)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (> (count target-bytes) 63)
          "the root formula should be too large for the old flat formula-length byte")
      (is (zero? (proof-symbol-count proof))
          "the wide structural proof should not rely on symbolic proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           120
                           proof)
              (l/== true q)))
          "the structural checker should accept the in-memory wide formula-bearing node")
      (is (successful?
            (l/run 1 [q]
              (l/fresh [decoded-proof sigma-out proof-bytes proof-read-proof]
                (decode-proof certificate
                              '()
                              sigma-out
                              proof-bytes
                              decoded-proof
                              proof-read-proof)
                (check-proof (:program system)
                             (:system-code system)
                             target
                             120
                             decoded-proof)
                (l/== true q))))
          "public proof-code decoding should preserve wide formula-bearing proof nodes for object-level checking"))))

(deftest ^:slow sjas-tableau-proof-accepts-formula-bearing-true-theorem-certificates
  (testing "public tableau-proof accepts a formula-bearing structural certificate"
    (let [system (demo-system :willard-sjas-tableau0)
          theorem (ast/true-form)
          theorem-code (sjas/formula-code system theorem)
          target (ast/and-form
                   (proof-side-antecedent-formula (:axiom-formula system))
                   (ast/false-form))
          proof (structural-byte-list-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form)))
          certificate (sjas/proof-certificate proof)]
      (is (> (count (formula-code-bytes system target)) 63)
          "the public target should require the wide formula-bearing node shape")
      (is (zero? (proof-symbol-count proof))
          "the public structural theorem proof should not use symbolic proof-rule tags")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              260))
          "tableau-proof should validate encoded formula-bearing structural certificates end to end"))))

(deftest ^{:slow true
           :expected-duration "about 2-3 minutes on 2026-06-08; latest measured 2:18.62"
           :correctness "public tableau-proof/3 validates a substantive formula-bearing proof certificate supplied as a U-Grounding numeral"}
  sjas-tableau-proof-accepts-u-grounding-formula-bearing-proof-certificate
  (testing "public tableau-proof accepts a substantive formula-bearing proof path as a U-Grounding numeral"
    (let [system (demo-system :willard-sjas-tableau0)
          theorem (ast/true-form)
          theorem-code (sjas/formula-code system theorem)
          target (ast/and-form
                   (proof-side-antecedent-formula (:axiom-formula system))
                   (ast/false-form))
          proof (structural-byte-list-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form)))
          certificate (sjas/proof-certificate proof
                                              {:code-format :u-grounding})]
      (is (sjas-numeral-term? certificate)
          "the proof certificate itself should be an arithmetic U-Grounding numeral")
      (is (zero? (proof-symbol-count proof))
          "the arithmeticized proof path should still be formula-bearing, not a symbolic proof trace")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  theorem-code
                                  certificate)
              1
              260))
          "tableau-proof should validate the U-Grounding proof-code path end to end"))))

(deftest ^{:slow true
           :expected-duration "about 2m20s on 2026-06-09 current source under load; use focused selector before public gate"
           :correctness "decoded compact proof-code tree should validate the full Group-3 SelfCons formula-bearing tableau target"}
  sjas-proof-check-accepts-formula-bearing-selfcons-tableau
  (testing "decoded proof-code trees can prove the Group-3 SelfCons statement structurally"
    (let [system (demo-system :willard-sjas-tableau0)
          {:keys [proof]} (selfcons-formula-bearing-proof system)
          certificate (sjas/proof-certificate proof)
          decode-proof (var-get #'sjas-profile/decode-non-sjas-axiom-proof-code-coreo)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the SelfCons certificate should be a formula-bearing tableau tree, not a proof-rule trace")
      (is (successful?
            (l/run 1 [q]
              (l/fresh [decoded-proof sigma-out proof-bytes target]
                (decode-proof certificate
                              '()
                              sigma-out
                              proof-bytes
                              decoded-proof)
                (selfcons-object-targeto system target)
                (check-proof (:program system)
                             (:system-code system)
                             target
                             260
                             decoded-proof)
                (l/== true q))))
          "the decoded SelfCons proof code should close by structural tableau checking"))))

(deftest ^{:slow true
           :expected-duration "about 1m09s on 2026-06-09 current source under load"
           :correctness "lower-level checker validates the five-node Group-3/negated-SelfCons tableau core without proof-rule tags"}
  sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau
  (testing "the Group-3 and negated-SelfCons tableau core closes structurally"
    (let [system (demo-system :willard-sjas-tableau0)
          {:keys [target proof]} (selfcons-core-formula-bearing-proof system)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the core SelfCons certificate should be a formula-bearing tableau tree")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           160
                           proof)
              (l/== true q)))
          "the core contradiction should close by quantifier expansion plus literal closure"))))

(deftest ^{:slow true
           :expected-duration "about 2m23s on 2026-06-09 current source under load"
           :correctness "in-memory formula-bearing SelfCons tableau should close after object target reconstruction, before proof-code decoding"}
  sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau
  (testing "the Group-3 SelfCons tableau tree itself closes structurally"
    (let [system (demo-system :willard-sjas-tableau0)
          {:keys [proof]} (selfcons-formula-bearing-proof system)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the in-memory SelfCons certificate should be a formula-bearing tableau tree")
      (is (successful?
            (l/run 1 [q]
              (l/fresh [target]
                (selfcons-object-targeto system target)
                (check-proof (:program system)
                             (:system-code system)
                             target
                             260
                             proof)
                (l/== true q))))
          "the SelfCons formula-bearing tableau should close before proof-code decoding is considered"))))

(deftest ^{:slow true
           :expected-duration "about 8m30s on 2026-06-09 after ADR-0086 changed Tableau-0 SelfCons from false to 0=1"
           :correctness "public tableau-proof/3 decodes s, t, and compact p for the Group-3 SelfCons statement and validates the formula-bearing tableau tree"}
  sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate
  (testing "public tableau-proof accepts the system consistency statement and structural proof code"
    (let [system (demo-system :willard-sjas-tableau0)
          {:keys [proof]} (selfcons-formula-bearing-proof system)
          certificate (sjas/proof-certificate proof)]
      (is (zero? (proof-symbol-count proof))
          "the public SelfCons certificate must not be the `sjas-axiom` citation")
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/tableau-proof (:system-code system)
                                  (:code (:group-three system))
                                  certificate)
              1
              320))
          "tableau-proof should decode s, t, and p and validate the SelfCons tableau structurally"))))

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
    (let [system (demo-system :willard-sjas-tableau0)
          decode-axiom (var-get #'sjas-profile/decode-sjas-axiom-proof-codeo)
          decode-non-axiom (var-get #'sjas-profile/decode-non-sjas-axiom-proof-codeo)
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          ug-axiom-certificate (sjas/proof-certificate 'sjas-axiom
                                                       {:code-format :u-grounding})
          legacy-proof '(false-close)
          legacy-certificate (sjas/proof-certificate legacy-proof)
          structural-proof (structural-tableau-node system (ast/false-form))
          structural-certificate (sjas/proof-certificate structural-proof)
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
          legacy-as-non-axiom (l/run 1 [q]
                                 (l/fresh [sigma-out bytes decoded read-proof]
                                   (decode-non-axiom legacy-certificate
                                                     '()
                                                     sigma-out
                                                     bytes
                                                     decoded
                                                     read-proof)
                                   (l/== decoded q)))
          structural-decodes (l/run 1 [q]
                               (l/fresh [sigma-out bytes decoded read-proof]
                                 (decode-non-axiom structural-certificate
                                                   '()
                                                   sigma-out
                                                   bytes
                                                   decoded
                                                   read-proof)
                                 (l/== [sigma-out decoded] q)))]
      (is (= [['() axiom-bytes]] axiom-decodes))
      (is (= [['() axiom-bytes]] ug-axiom-decodes))
      (is (empty? axiom-as-non-axiom))
      (is (empty? legacy-as-non-axiom)
          "non-axiom proof-code decoding should reject legacy proof-rule traces")
      (is (= [['() structural-proof]] structural-decodes)
          "non-axiom proof-code decoding should accept formula-bearing structural tableau trees"))))

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
      (is (successful?
            (query/query-succeeds program
                                  (ast/neg-lit (ast/app-term 'leq (n 1) (n 0)))
                                  1
                                  80))
          "negated false arithmetic atoms must close by the interpreted positive-false branch rule")
      (is (successful?
            (query/query-succeeds program
                                  (ast/neg-lit (ast/app-term 'lt (n 1) (n 1)))
                                  1
                                  80)))
      (is (successful?
            (query/query-succeeds program
                                  (ast/neg-lit (ast/app-term 'mult (n 2) (n 2) (n 3)))
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



(deftest sjas-proof-predicates-ignore-external-runtime-clauses
  (let [system (demo-system :willard-sjas-tableau0)
        formula (ast/pos-lit (ast/app-term 'external-demo sjas/zero))
        neg-theorem (structural-neg-lit system 'external-demo sjas/zero)
        canonical-zero (list 'app (symbol "0"))
        canonical-neg-theorem (list 'neg (list 'app 'external-demo canonical-zero))
        canonical-external-negated-body (list 'neq canonical-zero canonical-zero)
        proof (canonical-structural-tableau-node
                system
                canonical-neg-theorem
                (canonical-structural-tableau-node
                  system
                  canonical-external-negated-body))
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (is (zero? (proof-symbol-count proof))
        "the external-clause rejection certificate should be a tableau tree, not a kernel trace")
    (is (successful?
          (query/query-succeeds
            (:program system)
            formula
            1
            96))
        "external clauses remain executable for ordinary host-side Proflog queries")
    (is (empty?
          (l/run 1 [q]
            (check-proof (:program system)
                         (:system-code system)
                         neg-theorem
                         80
                         proof)
            (l/== true q)))
        "SJAS proof checking must recover reflected clauses from system-code, not external runtime clauses")))










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
          "formula-bearing nodes should validate by structural Deduction/Closure checks, not rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           0
                           proof)
              (l/== true q)))
          "fixed formula-bearing proof validation must not depend on external runtime fuel"))))

(deftest sjas-structural-proof-checker-does-not-consume-runtime-fuel
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        start-token "(defn- sjas-structural-proof-check-state-decodedo"
        end-token "(defn- sjas-proof-check-stateo"
        start (str/index-of source start-token)
        end (str/index-of source end-token start)
        structural-source (subs source start end)]
    (is (not (str/includes? structural-source "support/step-fuelo"))
        "fixed SJAS tableau proof validation must be a relation over the proof tree, not over external evaluator fuel")))

(deftest sjas-structural-proof-checker-does-not-duplicate-guided-branches
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        start-token "(defn- sjas-structural-proof-check-state-decodedo"
        end-token "(defn- sjas-proof-check-stateo"
        start (str/index-of source start-token)
        end (str/index-of source end-token start)
        structural-source (subs source start end)]
	    (is (= 1
	           (substring-count
	             structural-source
	             "(sjas-proof-guided-selecto child-formula"))
	        "guided literal continuation should be scheduled once in the structural checker")
    (is (= 1
           (substring-count
             structural-source
             "sjas-complementary-lit-close-coreo lit lits sigma sigma-out"))
        "complementary literal closure should be scheduled once in the structural checker")
    (is (= 1
           (substring-count structural-source "(== (list 'and left right) fml)"))
        "conjunction continuation should be scheduled once in the structural checker")
	    (is (= 1
	           (substring-count structural-source "(lcons lit lits)"))
	        "literal continuation should add to the branch literal list only through the guided child-formula path")))

(deftest sjas-structural-proof-checker-preserves-delayed-sibling-environments
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        structural-start (str/index-of source
                                       "(defn- sjas-structural-proof-check-state-decodedo")
        structural-end (str/index-of source
                                     "(defn- sjas-proof-check-stateo"
                                     structural-start)
        structural-source (subs source structural-start structural-end)
        guided-start (str/index-of source "(defn- sjas-proof-guided-selecto")
        guided-end (str/index-of source
                                 "(defn- sjas-proof-check-stateo"
                                 guided-start)
        guided-source (subs source guided-start guided-end)]
    (is (str/includes? source "(== [formula entry-env] entry)")
        "delayed structural agenda entries must store formula and branch environment as relation data")
    (is (str/includes? structural-source
                       "(sjas-agenda-cons-coreo right env unexpanded next-unexpanded)")
        "conjunction siblings must be enqueued with their current branch environment")
    (is (str/includes? structural-source
                       "(sjas-agenda-heado env unexpanded next next-env rest)")
        "agenda continuations must recover the saved branch environment")
    (is (not (str/includes? guided-source "subst/subst-formulao"))
        "agenda selection must not duplicate large-formula substitution before decoded rule validation")
    (is (not (str/includes? guided-source "sjas-proof-node-formula-matcho"))
        "agenda selection must leave proof-node formula validation to the decoded structural checker")
    (is (not (str/includes? guided-source "sjas-formula-env-irrelevanto"))
        "agenda selection must not depend on an ambient-env shortcut")
	    (is (not (re-find #"\(project \[" guided-source))
	        "agenda selection must remain a relation, not a host projection")))

(deftest sjas-static-code-table-lookups-avoid-membero-scheduling
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        static-helpers ["proof-byte-decremento"
                        "byte-bitso"
                        "byte-six-bitso"
                        "code-constructoro"
                        "positive-byteo"
                        "positive-byte-except-oneo"
                        "positive-byte-neqo"
                        "byte-neqo"
                        "sjas-reserved-symbol-indexo"
                        "sjas-user-symbol-indexo"
                        "proof-symbol-indexo"
                        "proof-symbol-wide-indexo"
                        "positive-wide-proof-counto"
                        "decrement-wide-proof-counto"]]
    (doseq [helper static-helpers]
      (is (not (private-defn-contains? source helper "membero"))
          (str helper
               " should expose its fixed table as explicit finite alternatives,"
               " not recursive membero scheduling")))))

(deftest sjas-embedded-payload-decoders-check-header-before-payload-fresh
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        header-first-pattern #"(?s)\(== expected-low low\).*?\(== expected-high high\).*?\(fresh \[payload\]"]
    (doseq [helper ["decode-embedded-code-bodyo" "decode-natural-bodyo"]]
      (is (re-find header-first-pattern (private-defn-source source helper))
          (str helper
               " should reject wrong low/high length headers before allocating payload state")))))

(deftest sjas-app-arity-decoders-destructure-arity-byte-once
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")]
    (doseq [helper ["decode-app-arityo"
                    "decode-syntax-app-arityo"
                    "skip-syntax-app-arityo"]]
      (let [helper-source (private-defn-source source helper)]
        (is (not (str/includes? helper-source (str "(" helper " ")))
            (str helper " should not recursively retry arity candidates"))
        (is (str/includes? helper-source "(sjas-acyclic-unifyo (lcons arity-byte arg-bytes) after-symbol)")
            (str helper " should destructure the encoded arity byte once"))))))

(deftest sjas-proof-facing-dispatch-does-not-use-committed-choice
  (let [profile-source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        kernel-source (slurp "src/proflog/kernel.clj")
        close-agenda-start (str/index-of kernel-source "(defn close-agendao")
        close-agenda-end (str/index-of kernel-source
                                       "\n(defn prove-stateo"
                                       close-agenda-start)
        close-agenda-source (subs kernel-source close-agenda-start close-agenda-end)]
    (is (not (re-find #":refer \[[^\]]*\bconda\b" profile-source))
        "SJAS proof-facing profile code must not import committed-choice dispatch")
    (is (not (re-find #"\(conda\b" profile-source))
        "SJAS proof-facing profile code must use structural relations rather than committed choice")
    (is (not (re-find #":refer \[[^\]]*\bconda\b" kernel-source))
        "the generic kernel proof-dispatch path must not import committed-choice dispatch")
    (is (not (re-find #"\(conda\b" close-agenda-source))
        "kernel theory-profile dispatch must use explicit structural guards and conde")))

(deftest kernel-proof-hooks-avoid-host-optional-dispatch
  (let [kernel-source (slurp "src/proflog/kernel.clj")
        recursive-source (private-defn-source kernel-source "recursive-prove-stateo")
        theory-source (private-defn-source kernel-source "theory-profile-closeo")
        close-agenda-source (subs kernel-source
                                  (str/index-of kernel-source "(defn close-agendao")
                                  (str/index-of kernel-source
                                                "\n(defn prove-stateo"))]
    (is (not (str/includes? recursive-source
                            "(or *recursive-prove-stateo* prove-stateo)"))
        "recursive proof dispatch must use a callable default relation, not host optional selection")
    (is (not (str/includes? theory-source
                            "(if-let [closeo *theory-profile-closeo*]"))
        "theory profile dispatch must use a callable default failing relation, not host optional selection")
    (is (not (str/includes? close-agenda-source
                            "*theory-profile-closeo* nil"))
        "close-agendao must not branch on host nilness of the profile hook")))

(deftest sjas-public-compact-code-readers-parse-presented-byte-numerals
  (let [profile-source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        proof-reader-source (private-defn-source profile-source "code-argso")
        core-reader-source (private-defn-source profile-source "code-args-coreo")
        builder-source (private-defn-source profile-source "code-args-buildo")]
    (doseq [[label source] [["proof-producing reader" proof-reader-source]
                            ["proof-free reader" core-reader-source]]]
      (is (str/includes? source "code-byte-termo")
          (str label " must parse presented compact byte numerals"))
      (is (not (str/includes? source "code-byte-build-termo"))
          (str label " must not use byte-first reconstruction for public input")))
    (is (str/includes? builder-source "code-byte-build-termo")
        "byte-first embedded payload reconstruction should keep the builder relation")))

(deftest sjas-proof-check-accepts-formula-bearing-right-first-conjunction-tableaux
  (testing "structural conjunction may close the right conjunct before expanding the left"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form (ast/true-form) (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "right-first structural conjunction should not use conj or false-close tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing conjunction should permit either conjunct to be selected next"))))

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

(deftest sjas-equality-closure-is-formula-bearing-and-tag-free
  (testing "ADR-0098: reflexive-disequality closure goes through a formula-bearing, equality-tag-free first-fragment certificate"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/neq-lit sjas/one sjas/one)
          proof (structural-tableau-node system target)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "the structural checker closes (neq one one) by reflexive same-term recognition")
      (is (zero? (proof-symbol-count proof))
          "the closing certificate is formula-bearing: it carries no proof-symbol tags")
      (is (= #{}
             (:equality-symbols-present
               (correspondence/audit-equality-reachability proof)))
          "no equality/disequality constructor tag is reachable in the accepted certificate")
      (is (false?
            (:equality-reachable?
              (correspondence/audit-equality-reachability proof))))
      (is (= :formula-bearing-tableau
             (:fragment-status
               (correspondence/audit-first-correspondence-fragment proof)))
          "the equality-closing certificate is inside the first correspondence fragment"))))

(deftest sjas-proof-check-accepts-formula-bearing-double-negation-tableaux
  (testing "structural double negation removes both negations without proof-rule tags"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/not-form (ast/not-form (ast/false-form)))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural double-negation proof should not use proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing double negation should validate by local tableau structure"))))

(deftest sjas-proof-check-accepts-formula-bearing-negated-conjunction-tableaux
  (testing "structural negated conjunction branches into negated conjuncts"
    (let [system (demo-system :willard-sjas-tableau0)
          not-true (ast/not-form (ast/true-form))
          target (ast/not-form (ast/and-form (ast/true-form) (ast/true-form)))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node system not-true)
                  (structural-tableau-node system not-true))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural negated-conjunction proof should not use split or false-close proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing negated conjunction should validate by branch structure"))))

(deftest sjas-proof-check-accepts-formula-bearing-negated-disjunction-tableaux
  (testing "structural negated disjunction adds both negated disjuncts to one branch"
    (let [system (demo-system :willard-sjas-tableau0)
          not-false (ast/not-form (ast/false-form))
          not-true (ast/not-form (ast/true-form))
          target (ast/not-form (ast/or-form (ast/false-form) (ast/true-form)))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    not-false
                    (structural-tableau-node system not-true)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural negated-disjunction proof should not use proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           30
                           proof)
              (l/== true q)))
          "formula-bearing negated disjunction should validate by same-branch structure"))))

(deftest sjas-proof-check-accepts-formula-bearing-implication-tableaux
  (testing "structural implication branches into a negated antecedent and consequent"
    (let [system (demo-system :willard-sjas-tableau0)
          not-true (ast/not-form (ast/true-form))
          target (ast/implies-form (ast/true-form) (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node system not-true)
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural implication proof should not use proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing implication should validate by branch structure"))))

(deftest sjas-proof-check-accepts-formula-bearing-negated-implication-tableaux
  (testing "structural negated implication adds antecedent and negated consequent to one branch"
    (let [system (demo-system :willard-sjas-tableau0)
          not-true (ast/not-form (ast/true-form))
          target (ast/not-form (ast/implies-form (ast/true-form) (ast/true-form)))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    (ast/true-form)
                    (structural-tableau-node system not-true)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural negated-implication proof should not use proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           30
                           proof)
              (l/== true q)))
          "formula-bearing negated implication should validate by same-branch structure"))))

(deftest sjas-proof-check-accepts-formula-bearing-negated-atomic-duals
  (testing "structural surface negation over literals and equality dualizes locally"
    (let [system (demo-system :willard-sjas-tableau0)
          leq-zero-zero (ast/app-term 'leq sjas/zero sjas/zero)
          not-pos (ast/not-form (ast/pos-lit leq-zero-zero))
          not-neg (ast/not-form (ast/neg-lit leq-zero-zero))
          not-eq (ast/not-form (ast/eq-lit sjas/zero sjas/zero))
          not-neq (ast/not-form (ast/neq-lit sjas/zero sjas/zero))
          cases [[not-pos (structural-tableau-node
                            system
                            not-pos
                            (structural-tableau-node system (ast/neg-lit leq-zero-zero)))]
                 [(ast/and-form not-neg (ast/false-form))
                  (structural-tableau-node
                    system
                    (ast/and-form not-neg (ast/false-form))
                    (structural-tableau-node
                      system
                      not-neg
                      (structural-tableau-node
                        system
                        (ast/pos-lit leq-zero-zero)
                        (structural-tableau-node system (ast/false-form)))))]
                 [not-eq (structural-tableau-node
                           system
                           not-eq
                           (structural-tableau-node
                             system
                             (ast/neq-lit sjas/zero sjas/zero)))]
                 [(ast/and-form not-neq (ast/false-form))
                  (structural-tableau-node
                    system
                    (ast/and-form not-neq (ast/false-form))
                    (structural-tableau-node
                      system
                      not-neq
                      (structural-tableau-node
                        system
                        (ast/eq-lit sjas/zero sjas/zero)
                        (structural-tableau-node system (ast/false-form)))))]]
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (doseq [[target proof] cases]
        (is (zero? (proof-symbol-count proof))
            "the structural negated atomic proof should not use proof-rule tags")
        (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             50
                             proof)
                (l/== true q)))
            (str "formula-bearing surface negation should dualize structurally: "
                 (pr-str target)))))))

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

(deftest sjas-complementary-literal-closure-uses-proof-free-atom-unifier
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")]
    (is (str/includes? source "sjas-atom-unify-coreo atom opposite sigma sigma-out")
        "structural complementary literal closure should compute atom unification without kernel proof-trace output")
    (is (not (str/includes? source "equality/atom-unifyo atom opposite sigma sigma-out atom-proof"))
        "formula-bearing complementary literal closure must not call the proof-producing kernel atom unifier")))

(deftest sjas-proof-check-accepts-formula-bearing-quantifier-expansions
  (testing "structural quantifier nodes infer expansion without witness or universal proof tags"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          cases [(ast/exists-form binding (ast/false-form))
                 (ast/forall-form binding (ast/false-form))
                 (ast/once-forall-form binding (ast/false-form))]
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (doseq [target cases]
        (let [proof (structural-tableau-node
                      system
                      target
                      (structural-tableau-node system (ast/false-form)))]
          (is (zero? (proof-symbol-count proof))
              "the structural quantifier proof should not use witness, univ, or once-univ proof-rule tags")
          (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               20
                               proof)
                  (l/== true q)))
              (str "formula-bearing quantifier node should validate structurally: "
                   (pr-str (ast/tag-of target)))))))))

(deftest sjas-proof-check-accepts-formula-bearing-bounded-quantifier-expansions
  (testing "structural bounded quantifiers expand through their arithmetic guard formulas"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          canonical-zero (list 'app (symbol "0"))
          bounded-exists (sjas/bounded-exists binding sjas/zero (ast/false-form))
          exists-guard (list 'pos (list 'app 'leq (list 'par 'v0) canonical-zero))
          exists-child (list 'and exists-guard (list 'false))
          exists-proof (structural-tableau-node
                         system
                         bounded-exists
                         (canonical-structural-tableau-node
                           system
                           exists-child
                           (structural-tableau-node system (ast/false-form))))
          bounded-forall (sjas/bounded-forall binding sjas/zero (ast/false-form))
          forall-guard (list 'neg (list 'app 'leq (list 'var 'v0) canonical-zero))
          forall-child (list 'or forall-guard (list 'false))
          forall-proof (structural-tableau-node
                         system
                         bounded-forall
                         (canonical-structural-tableau-node
                           system
                           forall-child
                           (canonical-structural-tableau-node system forall-guard)
                           (structural-tableau-node system (ast/false-form))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (doseq [[target proof] [[bounded-exists exists-proof]
                              [bounded-forall forall-proof]]]
        (is (zero? (proof-symbol-count proof))
            "the structural bounded-quantifier proof should not use witness, univ, or arithmetic proof tags")
        (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             50
                             proof)
                (l/== true q)))
            (str "formula-bearing bounded quantifier should validate structurally: "
                 (pr-str (ast/tag-of target))))))))

(deftest sjas-proof-check-accepts-formula-bearing-negated-quantifier-expansions
  (testing "structural negated quantifiers expand through their dual quantifier rule"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          not-true (ast/not-form (ast/true-form))
          cases [(ast/not-form (ast/forall-form binding (ast/true-form)))
                 (ast/not-form (ast/exists-form binding (ast/true-form)))
                 (ast/not-form (ast/once-forall-form binding (ast/true-form)))]
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (doseq [target cases]
        (let [proof (structural-tableau-node
                      system
                      target
                      (structural-tableau-node system not-true))]
          (is (zero? (proof-symbol-count proof))
              "the structural negated-quantifier proof should not use witness, univ, or proof-rule tags")
          (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               30
                               proof)
                  (l/== true q)))
              (str "formula-bearing negated quantifier should validate structurally: "
                   (pr-str (ast/tag-of (second target))))))))))

(deftest sjas-proof-check-accepts-formula-bearing-negated-bounded-quantifier-expansions
  (testing "structural negated bounded quantifiers expand through dual guard formulas"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          canonical-zero (list 'app (symbol "0"))
          not-true (list 'not (list 'true))
          negated-bounded-forall (ast/not-form
                                   (sjas/bounded-forall binding sjas/zero (ast/true-form)))
          forall-guard (list 'pos (list 'app 'leq (list 'par 'v0) canonical-zero))
          forall-child (list 'and forall-guard not-true)
          forall-proof (structural-tableau-node
                         system
                         negated-bounded-forall
                         (canonical-structural-tableau-node
                           system
                           forall-child
                           (canonical-structural-tableau-node system not-true)))
          negated-bounded-exists (ast/not-form
                                   (sjas/bounded-exists binding sjas/zero (ast/true-form)))
          exists-guard (list 'neg (list 'app 'leq (list 'var 'v0) canonical-zero))
          exists-child (list 'or exists-guard not-true)
          exists-proof (structural-tableau-node
                         system
                         negated-bounded-exists
                         (canonical-structural-tableau-node
                           system
                           exists-child
                           (canonical-structural-tableau-node system exists-guard)
                           (canonical-structural-tableau-node system not-true)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (doseq [[target proof] [[negated-bounded-forall forall-proof]
                              [negated-bounded-exists exists-proof]]]
        (is (zero? (proof-symbol-count proof))
            "the structural negated bounded-quantifier proof should not use witness, univ, or proof-rule tags")
        (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             60
                             proof)
                (l/== true q)))
            (str "formula-bearing negated bounded quantifier should validate structurally: "
                 (pr-str (ast/tag-of (second target)))))))))

(deftest sjas-proof-check-accepts-formula-bearing-quantifier-variable-children
  (testing "structural quantifier children may use canonical variable payloads"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          body (ast/neq-lit (ast/var-term binding) sjas/zero)
          target (ast/forall-form binding body)
          canonical-child (list 'neq
                                (list 'var 'v0)
                                (list 'app (symbol "0")))
          proof (structural-tableau-node
                  system
                  target
                  (canonical-structural-tableau-node system canonical-child))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural quantified proof should not use witness, univ, neq-close, or arithmetic proof tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           30
                           proof)
	              (l/== true q)))
	          "formula-bearing quantifier children should decode canonical v0 payloads into branch variables"))))

(deftest sjas-proof-check-preserves-delayed-sibling-scope-after-quantifiers
  (testing "delayed conjunction siblings keep the environment active when they were enqueued"
    (let [binding (sjas-code/code-nom 1)
          delayed (ast/neq-lit (ast/var-term binding) (ast/var-term binding))
          ambient-env (list [binding (ast/var-term 'v0)])
          saved-env '()
          agenda (list [delayed saved-env])
          guided-select (var-get #'sjas-profile/sjas-proof-guided-selecto)]
      (is (successful?
            (l/run 1 [q]
              (l/fresh [selected selected-env remaining]
                (guided-select delayed
                               ambient-env
                               agenda
                               selected
                               selected-env
                               remaining)
                (l/== delayed selected)
                (l/== saved-env selected-env)
                (l/== '() remaining))
              (l/== true q)))
          "a delayed sibling must be matched under its saved environment, not the later ambient quantifier environment"))))

(deftest sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures
  (testing "structural disequality leaves can close reflexive contradictions without refl-close tags"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/neq-lit sjas/zero sjas/zero)
          proof (structural-tableau-node system target)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural disequality proof should not use the refl-close proof-rule tag")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing disequality leaves should close when both sides are equal in branch state"))))

(deftest sjas-proof-check-accepts-formula-bearing-arithmetic-closures
  (testing "structural arithmetic leaves close through profile arithmetic without arith-close tags"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/neg-lit (ast/app-term 'leq sjas/one sjas/one))
          proof (structural-tableau-node system target)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural arithmetic proof should not use arith-close or profiled arithmetic proof tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           80
                           proof)
              (l/== true q)))
          "formula-bearing arithmetic leaves should close by evaluating the SJAS arithmetic relation internally"))))

(deftest sjas-proof-check-accepts-formula-bearing-positive-false-arithmetic-closures
  (testing "structural arithmetic leaves also close positive interpreted atoms when the relation is false"
    (let [system (demo-system :willard-sjas-tableau0)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (doseq [target [(ast/pos-lit (ast/app-term 'leq sjas/one sjas/zero))
                      (ast/pos-lit (ast/app-term 'lt sjas/one sjas/one))
                      (ast/pos-lit (ast/app-term 'mult (n 2) (n 2) (n 3)))]]
        (let [proof (structural-tableau-node system target)]
          (is (zero? (proof-symbol-count proof))
              "the structural arithmetic proof should not add a proof-rule tag for false interpreted atoms")
          (is (successful?
                (l/run 1 [q]
                  (check-proof (:program system)
                               (:system-code system)
                               target
                               80
                               proof)
                  (l/== true q)))
              "positive arithmetic atoms should close when the interpreted relation is false"))))))

(deftest sjas-structural-arithmetic-closure-uses-proof-free-arithmetic
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        start-token "(defn- sjas-structural-proof-check-state-decodedo"
        end-token "(defn- sjas-proof-check-stateo"
        start (str/index-of source start-token)
        end (str/index-of source end-token start)
        structural-source (subs source start end)]
    (is (str/includes? structural-source "sjas-neq-close-structural-coreo")
        "structural arithmetic disequality closure should not require arithmetic proof payloads")
    (is (str/includes? structural-source "sjas-neg-relation-close-structural-coreo")
        "structural arithmetic relation closure should not require arithmetic proof payloads")
    (is (str/includes? structural-source "sjas-pos-relation-close-structural-coreo")
        "structural arithmetic false-relation closure should not require arithmetic proof payloads")
    (is (not (str/includes? structural-source "sjas-neq-close-coreo fml env sigma sigma-out neqs neqs-out arithmetic-proof"))
        "formula-bearing structural arithmetic closure must not call the proof-producing disequality core")
    (is (not (str/includes? structural-source "sjas-neg-relation-close-coreo fml env sigma sigma-out neqs neqs-out arithmetic-proof"))
        "formula-bearing structural arithmetic closure must not call the proof-producing relation core")
    (is (not (str/includes? structural-source "sjas-pos-relation-close-coreo fml env sigma sigma-out neqs neqs-out arithmetic-proof"))
        "formula-bearing structural false-relation closure must not call the proof-producing relation core")))

(deftest sjas-structural-recursive-proof-predicate-closures-use-object-relations
  (testing "structural tableau leaves close recursive proof predicates through arithmeticized predicate relations"
    (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
          start-token "(defn- sjas-structural-proof-check-state-decodedo"
          end-token "(defn- sjas-proof-check-stateo"
          start (str/index-of source start-token)
          end (str/index-of source end-token start)
          structural-source (subs source start end)
          axiom-structural-start (str/index-of
                                   source
                                   "(defn- sjas-axiom-member-structural-closeo")
          axiom-structural-end (str/index-of source
                                             "(defn- sjas-eq-progresso"
                                             axiom-structural-start)
          axiom-structural-source (subs source
                                        axiom-structural-start
                                        axiom-structural-end)
          axiom-member-close-var (ns-resolve 'proflog.kernel.willard-sjas-profile
                                             'sjas-axiom-member-structural-closeo)
          tableau-close-var (ns-resolve 'proflog.kernel.willard-sjas-profile
                                        'sjas-tableau-proof-structural-closeo)
          subst-close-var (ns-resolve 'proflog.kernel.willard-sjas-profile
                                      'sjas-subst-prf-structural-closeo)]
      (is (str/includes? structural-source "sjas-axiom-member-structural-closeo")
          "structural proof checking must close axiom-member atoms through decoded system-code membership")
      (is (str/includes? structural-source "sjas-tableau-proof-structural-closeo")
          "structural proof checking must close tableau-proof atoms through the arithmeticized predicate relation")
      (is (str/includes? structural-source "sjas-subst-prf-structural-closeo")
          "structural proof checking must close subst-prf atoms through the arithmeticized predicate relation")
      (is (fn? (when axiom-member-close-var (var-get axiom-member-close-var)))
          "axiom-member structural closure relation should exist")
      (is (fn? (when tableau-close-var (var-get tableau-close-var)))
          "tableau-proof structural closure relation should exist")
      (is (fn? (when subst-close-var (var-get subst-close-var)))
          "subst-prf structural closure relation should exist")
      (is (str/includes? axiom-structural-source "sjas-walked-axiom-member-coreo")
          "structural axiom-member closure must use proof-free decoded membership")
      (is (not (str/includes? axiom-structural-source "sjas-axiom-member-closeo"))
          "structural axiom-member closure must not materialize ordinary axiom-member proof evidence"))))

(deftest sjas-structural-recursive-proof-predicate-closures-avoid-answer-proof-wrappers
  (testing "formula-bearing recursive proof-predicate leaves do not build ordinary answer-proof evidence"
    (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
          tableau-structural-start (str/index-of
                                     source
                                     "(defn- sjas-tableau-proof-structural-closeo")
          tableau-structural-end (str/index-of
                                   source
                                   "(defn- sjas-subst-prf-structural-closeo"
                                   tableau-structural-start)
          tableau-structural-source (subs source
                                          tableau-structural-start
                                          tableau-structural-end)
          subst-structural-start (str/index-of
                                   source
                                   "(defn- sjas-subst-prf-structural-closeo")
          subst-structural-end (str/index-of
                                 source
                                 "(defn willard-sjas-theory-closeo"
                                 subst-structural-start)
          subst-structural-source (subs source
                                        subst-structural-start
                                        subst-structural-end)]
      (is (str/includes? tableau-structural-source "sjas-tableau-proof-coreo")
          "structural tableau-proof closure should call the proof-free object relation")
      (is (not (str/includes? tableau-structural-source "sjas-tableau-proof-closeo"))
          "structural tableau-proof closure must not materialize the ordinary answer-proof wrapper")
      (is (str/includes? subst-structural-source "sjas-subst-prf-coreo")
          "structural subst-prf closure should call the proof-free object relation")
      (is (not (str/includes? subst-structural-source "sjas-subst-prf-closeo"))
          "structural subst-prf closure must not materialize the ordinary answer-proof wrapper"))))

(deftest sjas-proof-check-accepts-formula-bearing-equality-continuations
  (testing "structural equality nodes advance branch state without eq-step tags"
    (let [system (demo-system :willard-sjas-tableau0)
          equality (ast/eq-lit sjas/one sjas/one)
          target (ast/and-form equality (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    equality
                    (structural-tableau-node system (ast/false-form))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural equality proof should not use the eq-step proof-rule tag")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           30
                           proof)
              (l/== true q)))
          "formula-bearing equality nodes should update branch state and continue structurally"))))

(deftest sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures
  (testing "structural equality leaves close free-constructor contradictions without kernel proof tags"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/eq-lit sjas/zero sjas/one)
          proof (structural-tableau-node system target)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural equality contradiction proof should not use free-close or decompose tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "formula-bearing equality leaves should close when the equation is impossible in the term algebra"))))

(deftest sjas-structural-proof-checker-uses-proof-free-equality-progression
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        start-token "(defn- sjas-structural-proof-check-state-decodedo"
        end-token "(defn- sjas-proof-check-stateo"
        start (str/index-of source start-token)
        end (str/index-of source end-token start)
        structural-source (subs source start end)]
    (is (str/includes? structural-source "sjas-unify-termo-coreo")
        "structural equality progression should compute branch substitutions without kernel proof-trace output")
    (is (not (str/includes? structural-source "equality/unify-termo"))
        "formula-bearing structural equality progression must not call the proof-producing kernel unifier")))

(deftest sjas-structural-proof-checker-has-no-proof-rule-tag-shortcuts
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        start-token "(defn- sjas-structural-proof-check-state-decodedo"
        end-token "(defn- sjas-proof-check-stateo"
        start (str/index-of source start-token)
        end (str/index-of source end-token start)
        structural-source (subs source start end)
        legacy-tags ['conj
                     'split
                     'univ
                     'once-univ
                     'witness
                     'eq-step
                     'eq-triggered-call
                     'eq-triggered-neg-call
                     'neq-close
                     'neq-rigid
                     'neq-store
                     'refl-close
                     'savefml
                     'false-close
                     'arith-close
                     'close
                     'pos-call
                     'neg-call
                     'neg-call-alt
                     'neg-call-guarded-alt
                     'skip-true
                     'alt
                     'guarded-alt
                     'guarded-neg-alt
                     'guarded-neg-alt-saturated
                     'guarded-seq-step
                     'guarded-seq-last
                     'guarded-call-seq-step
                     'guarded-residual-seq-step
                     'guarded-residual-seq-last
                     'guarded-scope-exists
                     'guarded-scope-done
                     'guarded-seq-done
                     'guarded-call-seq-done
                     'guarded-residual-seq-done
                     'guard-saturation-done
                     'guard-eq]]
    (doseq [tag legacy-tags]
      (is (not (str/includes? structural-source (str "'" tag)))
          (str "formula-bearing structural proof checking must infer rules instead of matching proof tag "
               tag)))))

(deftest sjas-proof-checker-rejects-legacy-proof-rule-tag-certificates
  (testing "the SJAS proof predicate consumes tableau trees, not kernel proof traces"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form (ast/true-form) (ast/false-form))
          structural-proof (structural-tableau-node
                             system
                             target
                             (structural-tableau-node
                               system
                               (ast/true-form)
                               (structural-tableau-node system (ast/false-form))))
          legacy-proof '(conj (skip-true (false-close)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           structural-proof)
              (l/== true q)))
          "formula-bearing tableau trees remain the accepted proof predicate input")
      (is (empty?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           legacy-proof)
              (l/== true q)))
          "legacy kernel proof-rule traces must not be accepted as SJAS tableau certificates"))))

(deftest sjas-proof-check-accepts-formula-bearing-rigid-disequality-continuations
  (testing "structural disequality nodes continue when terms are rigidly different"
    (let [system (demo-system :willard-sjas-tableau0)
          disequality (ast/neq-lit sjas/zero sjas/one)
          target (ast/and-form disequality (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node
                    system
                    disequality
                    (structural-tableau-node system (ast/false-form))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural rigid disequality proof should not use the neq-rigid proof-rule tag")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           30
                           proof)
              (l/== true q)))
          "formula-bearing rigid disequality nodes should continue structurally"))))

(deftest sjas-proof-check-accepts-formula-bearing-disequality-storage
  (testing "structural disequality nodes store unresolved parameter constraints without neq-store tags"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          body (ast/and-form
                 (ast/neq-lit (ast/var-term binding) sjas/zero)
                 (ast/false-form))
          target (ast/exists-form binding body)
          canonical-body (list 'and
                               (list 'neq
                                     (list 'par 'v0)
                                     (list 'app (symbol "0")))
                               (list 'false))
          canonical-disequality (list 'neq
                                      (list 'par 'v0)
                                      (list 'app (symbol "0")))
          proof (structural-tableau-node
                  system
                  target
                  (canonical-structural-tableau-node
                    system
                    canonical-body
                    (canonical-structural-tableau-node
                      system
                      canonical-disequality
                      (structural-tableau-node system (ast/false-form)))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural disequality proof should not use witness or neq-store proof-rule tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           40
                           proof)
              (l/== true q)))
          "formula-bearing unresolved disequalities should be stored structurally and the branch should continue"))))

(deftest sjas-proof-check-accepts-formula-bearing-distinct-nested-existential-parameters
  (testing "nested structural existential parameters use distinct canonical noms"
    (let [system (demo-system :willard-sjas-tableau0)
          outer-binding (sjas-code/code-nom 1)
          inner-binding (sjas-code/code-nom 2)
          outer-var (ast/var-term outer-binding)
          inner-var (ast/var-term inner-binding)
          inner-body (ast/and-form
                       (ast/neq-lit inner-var sjas/one)
                       (ast/false-form))
          outer-body (ast/and-form
                       (ast/neq-lit outer-var sjas/zero)
                       (ast/exists-form inner-binding inner-body))
          target (ast/exists-form outer-binding outer-body)
          canonical-zero (list 'app (symbol "0"))
          canonical-one (list 'app (symbol "1"))
          canonical-par0 (list 'par 'v0)
          canonical-par1 (list 'par 'v1)
          canonical-var1 (list 'var 'v1)
          canonical-outer-disequality (list 'neq canonical-par0 canonical-zero)
          canonical-inner-disequality (list 'neq canonical-par1 canonical-one)
          canonical-inner-body (list 'and
                                     canonical-inner-disequality
                                     (list 'false))
          canonical-inner-exists (list 'exists
                                       'v1
                                       (list 'and
                                             (list 'neq canonical-var1 canonical-one)
                                             (list 'false)))
          canonical-outer-body (list 'and
                                     canonical-outer-disequality
                                     canonical-inner-exists)
          proof (structural-tableau-node
                  system
                  target
                  (canonical-structural-tableau-node
                    system
                    canonical-outer-body
                    (canonical-structural-tableau-node
                      system
                      canonical-outer-disequality
                      (canonical-structural-tableau-node
                        system
                        canonical-inner-exists
                        (canonical-structural-tableau-node
                          system
                          canonical-inner-body
                          (canonical-structural-tableau-node
                            system
                            canonical-inner-disequality
                            (structural-tableau-node system (ast/false-form))))))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural nested-existential proof should not use witness or neq-store tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           80
                           proof)
              (l/== true q)))
          "formula-bearing nested existentials should allocate distinct canonical parameters"))))

(deftest sjas-proof-check-accepts-formula-bearing-stored-disequality-closures
  (testing "structural equality leaves close when stored disequalities become false"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          body (ast/and-form
                 (ast/neq-lit (ast/var-term binding) sjas/zero)
                 (ast/eq-lit (ast/var-term binding) sjas/zero))
          target (ast/exists-form binding body)
          canonical-body (list 'and
                               (list 'neq
                                     (list 'par 'v0)
                                     (list 'app (symbol "0")))
                               (list 'eq
                                     (list 'par 'v0)
                                     (list 'app (symbol "0"))))
          canonical-disequality (list 'neq
                                      (list 'par 'v0)
                                      (list 'app (symbol "0")))
          canonical-equality (list 'eq
                                   (list 'par 'v0)
                                   (list 'app (symbol "0")))
          proof (structural-tableau-node
                  system
                  target
                  (canonical-structural-tableau-node
                    system
                    canonical-body
                    (canonical-structural-tableau-node
                      system
                      canonical-disequality
                      (canonical-structural-tableau-node
                        system
                        canonical-equality))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural stored-disequality proof should not use witness, neq-store, eq-step, or neq-close tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           50
                           proof)
              (l/== true q)))
          "formula-bearing equality leaves should close when equality violates stored disequalities"))))

(deftest sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures
  (testing "structural equality leaves close saved complementary literals after unification"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          positive (ast/pos-lit (ast/app-term 'wff (ast/var-term binding)))
          negative (ast/neg-lit (ast/app-term 'wff sjas/zero))
          equality (ast/eq-lit (ast/var-term binding) sjas/zero)
          body (ast/and-form positive (ast/and-form negative equality))
          target (ast/forall-form binding body)
          canonical-positive (list 'pos
                                   (list 'app 'wff (list 'var 'v0)))
          canonical-negative (list 'neg
                                   (list 'app 'wff (list 'app (symbol "0"))))
          canonical-equality (list 'eq
                                   (list 'var 'v0)
                                   (list 'app (symbol "0")))
          canonical-tail (list 'and canonical-negative canonical-equality)
          canonical-body (list 'and canonical-positive canonical-tail)
          proof (structural-tableau-node
                  system
                  target
                  (canonical-structural-tableau-node
                    system
                    canonical-body
                    (canonical-structural-tableau-node
                      system
                      canonical-positive
                      (canonical-structural-tableau-node
                        system
                        canonical-tail
                        (canonical-structural-tableau-node
                          system
                          canonical-negative
                          (canonical-structural-tableau-node
                            system
                            canonical-equality))))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural equality-triggered literal proof should not use savefml, eq-step, or close tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           60
                           proof)
              (l/== true q)))
          "formula-bearing equality leaves should close saved complementary literals after unification"))))

(deftest sjas-proof-check-accepts-formula-bearing-equality-triggered-positive-calls
  (testing "structural equality nodes open saved reflected positive calls after unification"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          positive (structural-pos-lit system 'demo (ast/var-term binding))
          equality (ast/eq-lit (ast/var-term binding) sjas/zero)
          body (ast/and-form positive equality)
          target (ast/exists-form binding body)
          canonical-zero (list 'app (symbol "0"))
          canonical-one (list 'app (symbol "1"))
          canonical-par (list 'par 'v0)
          canonical-bound (list 'var 'v0)
          canonical-positive (list 'pos
                                   (list 'app 'demo canonical-par))
          canonical-equality (list 'eq canonical-par canonical-zero)
          canonical-body (list 'and canonical-positive canonical-equality)
          canonical-target (list 'exists
                                 'v0
                                 (list 'and
                                       (list 'pos
                                             (list 'app 'demo canonical-bound))
                                       (list 'eq canonical-bound canonical-zero)))
          canonical-call-body (list 'eq canonical-zero canonical-one)
          proof (canonical-structural-tableau-node
                  system
                  canonical-target
                  (canonical-structural-tableau-node
                    system
                    canonical-body
                    (canonical-structural-tableau-node
                      system
                      canonical-positive
                      (canonical-structural-tableau-node
                        system
                        canonical-equality
                        (canonical-structural-tableau-node
                          system
                          canonical-call-body)))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural equality-triggered positive call proof should not use savefml, eq-step, or eq-triggered-call tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           80
                           proof)
              (l/== true q)))
          "formula-bearing equality nodes should recover saved positive reflected calls from encoded system-code"))))

(deftest sjas-proof-check-accepts-formula-bearing-equality-triggered-negative-calls
  (testing "structural equality nodes open saved reflected negative calls after unification"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          negative (structural-neg-lit system 'demo (ast/var-term binding))
          equality (ast/eq-lit (ast/var-term binding) sjas/one)
          body (ast/and-form negative equality)
          target (ast/exists-form binding body)
          canonical-one (list 'app (symbol "1"))
          canonical-par (list 'par 'v0)
          canonical-bound (list 'var 'v0)
          canonical-negative (list 'neg
                                   (list 'app 'demo canonical-par))
          canonical-equality (list 'eq canonical-par canonical-one)
          canonical-body (list 'and canonical-negative canonical-equality)
          canonical-target (list 'exists
                                 'v0
                                 (list 'and
                                       (list 'neg
                                             (list 'app 'demo canonical-bound))
                                       (list 'eq canonical-bound canonical-one)))
          canonical-call-body (list 'neq canonical-one canonical-one)
          proof (canonical-structural-tableau-node
                  system
                  canonical-target
                  (canonical-structural-tableau-node
                    system
                    canonical-body
                    (canonical-structural-tableau-node
                      system
                      canonical-negative
                      (canonical-structural-tableau-node
                        system
                        canonical-equality
                        (canonical-structural-tableau-node
                          system
                          canonical-call-body)))))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural equality-triggered negative call proof should not use savefml, eq-step, or eq-triggered-neg-call tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           80
                           proof)
              (l/== true q)))
          "formula-bearing equality nodes should recover saved negative reflected calls from encoded system-code"))))

(deftest sjas-proof-check-accepts-formula-bearing-positive-reflected-calls
  (testing "structural positive calls expand reflected system-code clauses without pos-call tags"
    (let [system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'positive-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'positive-demo
                                                    []
                                                    (ast/false-form))]})
          target (structural-pos-lit system 'positive-demo)
          canonical-target (list 'pos (list 'app 'positive-demo))
          proof (canonical-structural-tableau-node
                  system
                  canonical-target
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural reflected-call proof should not use the pos-call proof-rule tag")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           60
                           proof)
              (l/== true q)))
          "formula-bearing positive calls should recover reflected bodies from encoded system-code"))))

(deftest sjas-proof-check-accepts-formula-bearing-negative-reflected-calls
  (testing "structural negative calls expand negated reflected system-code clauses without neg-call tags"
    (let [system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'negative-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'negative-demo
                                                    []
                                                    (ast/true-form))]})
          target (structural-neg-lit system 'negative-demo)
          canonical-target (list 'neg (list 'app 'negative-demo))
          proof (canonical-structural-tableau-node
                  system
                  canonical-target
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural reflected negative-call proof should not use the neg-call proof-rule tag")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           60
                           proof)
              (l/== true q)))
          "formula-bearing negative calls should recover negated reflected bodies from encoded system-code"))))

(deftest sjas-procedure-call-expansion-is-formula-bearing-and-tag-free
  (testing "ADR-0099: reflected procedure-call closure goes through a formula-bearing, call-tag-free first-fragment certificate"
    (let [system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'positive-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'positive-demo
                                                    []
                                                    (ast/false-form))]})
          target (structural-pos-lit system 'positive-demo)
          canonical-target (list 'pos (list 'app 'positive-demo))
          proof (canonical-structural-tableau-node
                  system
                  canonical-target
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           60
                           proof)
              (l/== true q)))
          "the structural checker recovers the reflected body without a call tag")
      (is (= #{}
             (:procedure-call-expansion
               (:reachable-by-aspect
                 (correspondence/audit-fragment-reachability proof))))
          "no procedure-call constructor is reachable in the accepted certificate")
      (is (false? (:reachable? (correspondence/audit-fragment-reachability proof))))
      (is (= :formula-bearing-tableau
             (:fragment-status
               (correspondence/audit-first-correspondence-fragment proof)))
          "the reflected-call certificate is inside the first correspondence fragment"))))

(deftest sjas-quantifier-instantiation-is-formula-bearing-and-tag-free
  (testing "ADR-0099: quantifier instantiation goes through formula-bearing children with no univ/once-univ/witness tags"
    (let [system (demo-system :willard-sjas-tableau0)
          binding (sjas-code/code-nom 1)
          target (ast/exists-form binding (ast/false-form))
          proof (structural-tableau-node
                  system
                  target
                  (structural-tableau-node system (ast/false-form)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           20
                           proof)
              (l/== true q)))
          "the structural checker expands the quantifier into a formula-bearing child")
      (is (= #{}
             (:quantifier-instantiation
               (:reachable-by-aspect
                 (correspondence/audit-fragment-reachability proof))))
          "no quantifier-instantiation constructor is reachable in the accepted certificate")
      (is (false? (:reachable? (correspondence/audit-fragment-reachability proof))))
      (is (= :formula-bearing-tableau
             (:fragment-status
               (correspondence/audit-first-correspondence-fragment proof)))
          "the quantifier certificate is inside the first correspondence fragment"))))

(deftest sjas-correspondence-per-rule-witnesses
  (testing "ADR-0100: each Willard D connective/branching rule is witnessed by an accepted formula-bearing, tag-free, in-fragment certificate"
    (let [system (demo-system :willard-sjas-tableau0)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)
          node (fn [& args] (apply structural-tableau-node system args))
          accepts? (fn [target proof]
                     (successful?
                       (l/run 1 [q]
                         (check-proof (:program system)
                                      (:system-code system)
                                      target
                                      20
                                      proof)
                         (l/== true q))))
          in-fragment? (fn [proof]
                         (and (zero? (proof-symbol-count proof))
                              (false? (:reachable?
                                        (correspondence/audit-fragment-reachability proof)))
                              (= :formula-bearing-tableau
                                 (:fragment-status
                                   (correspondence/audit-first-correspondence-fragment proof)))))
          and-target (ast/and-form (ast/true-form) (ast/false-form))
          or-target (ast/or-form (ast/false-form) (ast/false-form))
          notnot-target (ast/not-form (ast/not-form (ast/false-form)))
          notand-target (ast/not-form (ast/and-form (ast/true-form) (ast/true-form)))
          implies-target (ast/implies-form (ast/true-form) (ast/false-form))
          cases [["rule 1 (alpha)" and-target
                  (node and-target (node (ast/false-form)))]
                 ["rule 3 (beta)" or-target
                  (node or-target (node (ast/false-form)) (node (ast/false-form)))]
                 ["rule 2 (double negation)" notnot-target
                  (node notnot-target (node (ast/false-form)))]
                 ["rule 2 (de Morgan + beta)" notand-target
                  (node notand-target
                        (node (ast/not-form (ast/true-form)))
                        (node (ast/not-form (ast/true-form))))]
                 ["rule 4 (implication)" implies-target
                  (node implies-target
                        (node (ast/not-form (ast/true-form)))
                        (node (ast/false-form)))]]]
      (doseq [[rule target proof] cases]
        (is (accepts? target proof)
            (str rule " witness must be accepted by the structural checker"))
        (is (in-fragment? proof)
            (str rule " witness must be a tag-free, in-fragment formula-bearing certificate"))))))

(deftest sjas-correspondence-anti-compression-rejects-skeletal-certificate
  (testing "ADR-0100: a skeletal certificate cannot validate a formula-bearing tree that requires expansion (5J lower bound)"
    (let [system (demo-system :willard-sjas-tableau0)
          target (ast/and-form (ast/false-form) (ast/false-form))
          full (structural-tableau-node system target
                                        (structural-tableau-node system (ast/false-form)))
          skeletal (structural-tableau-node system target)
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)
          accepts? (fn [proof]
                     (successful?
                       (l/run 1 [q]
                         (check-proof (:program system)
                                      (:system-code system)
                                      target
                                      20
                                      proof)
                         (l/== true q))))]
      (is (accepts? full)
          "the full formula-bearing certificate, carrying the expansion subtree, validates")
      (is (not (accepts? skeletal))
          "a skeletal root-only certificate cannot validate an expansion-requiring target: the formula-bearing subtree cannot be compressed away"))))

(deftest sjas-proof-check-accepts-formula-bearing-negative-reflected-alternatives
  (testing "structural negative calls select encoded reflected alternatives without neg-call-alt tags"
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
            target (structural-neg-lit system 'multi-demo sjas/one)
            canonical-one (list 'app (symbol "1"))
            canonical-target (list 'neg (list 'app 'multi-demo canonical-one))
            canonical-child (list 'neq canonical-one canonical-one)
            proof (canonical-structural-tableau-node
                    system
                    canonical-target
                    (canonical-structural-tableau-node system canonical-child))
            check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
        (is (zero? (proof-symbol-count proof))
            "the structural reflected negative alternative proof should not use neg-call-alt or alt tags")
        (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             80
                             proof)
                (l/== true q)))
            "formula-bearing negative calls should select reflected alternatives from encoded system-code")))))

(deftest sjas-proof-check-accepts-formula-bearing-guarded-negative-reflected-bodies
  (testing "structural negative calls close guarded-shaped reflected bodies without guarded proof tags"
    (let [guarded-body (ast/and-form
                         (ast/eq-lit sjas/one sjas/one)
                         (ast/true-form))
          system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :relations {'guarded-structural-demo 0}
                    :beta []
                    :reflected-clauses [(ast/clause 'guarded-structural-demo
                                                    []
                                                    guarded-body)
                                        (ast/clause 'guarded-structural-demo
                                                    []
                                                    (ast/false-form))]})
          target (structural-neg-lit system 'guarded-structural-demo)
          canonical-one (list 'app (symbol "1"))
          canonical-target (list 'neg (list 'app 'guarded-structural-demo))
          canonical-left (list 'neq canonical-one canonical-one)
          canonical-right (list 'false)
          canonical-child (list 'or canonical-left canonical-right)
          proof (canonical-structural-tableau-node
                  system
                  canonical-target
                  (canonical-structural-tableau-node
                    system
                    canonical-child
                    (canonical-structural-tableau-node system canonical-left)
                    (canonical-structural-tableau-node system canonical-right)))
          check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
      (is (zero? (proof-symbol-count proof))
          "the structural guarded negative-call proof should not use neg-call-guarded-alt, guarded-alt, guard-eq, or guarded sequence tags")
      (is (successful?
            (l/run 1 [q]
              (check-proof (:program system)
                           (:system-code system)
                           target
                           100
                           proof)
              (l/== true q)))
          "formula-bearing negative calls should close the decoded negated reflected body structurally"))))

(deftest sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies
  (testing "structural negative calls use ordinary quantifier expansion for guarded existential scope"
    (ast/nom x
      (let [scoped-body (ast/exists-form x (ast/true-form))
            system (sjas/system
                     {:profile :willard-sjas-tableau0
                      :relations {'guarded-scope-structural-demo 0}
                      :beta []
                      :reflected-clauses [(ast/clause 'guarded-scope-structural-demo
                                                      []
                                                      scoped-body)
                                          (ast/clause 'guarded-scope-structural-demo
                                                      []
                                                      (ast/false-form))]})
            target (structural-neg-lit system 'guarded-scope-structural-demo)
            canonical-target (list 'neg (list 'app 'guarded-scope-structural-demo))
            canonical-child (list 'once-forall 'v0 (list 'false))
            proof (canonical-structural-tableau-node
                    system
                    canonical-target
                    (canonical-structural-tableau-node
                      system
                      canonical-child
                      (canonical-structural-tableau-node system (list 'false))))
            check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
        (is (zero? (proof-symbol-count proof))
            "the structural guarded-scope proof should not use neg-call-guarded-alt or guarded-scope-exists tags")
        (is (successful?
              (l/run 1 [q]
                (check-proof (:program system)
                             (:system-code system)
                             target
                             100
                             proof)
                (l/== true q)))
            "formula-bearing negative calls should close negated existential reflected bodies by ordinary structural quantifier rules")))))

(deftest sjas-proof-predicates-check-reflected-calls-from-system-code
  (let [system (demo-system :willard-sjas-tableau0)
        neg-theorem (structural-neg-lit system 'demo sjas/one)
        canonical-one (list 'app (symbol "1"))
        canonical-neg-theorem (list 'neg (list 'app 'demo canonical-one))
        canonical-negated-reflected-body (list 'neq canonical-one canonical-one)
        proof (canonical-structural-tableau-node
                system
                canonical-neg-theorem
                (canonical-structural-tableau-node
                  system
                  canonical-negated-reflected-body))
        registry (atom (dissoc @(get-in system [:program :sjas/registry])
                               :sjas/reflected-program))
        stripped-program (assoc (:program system)
                                :clauses nil
                                :clause-list '()
                                :alternative-clause-list '()
                                :guarded-clause-list '()
                                :sjas/registry registry)
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (is (zero? (proof-symbol-count proof))
        "the public reflected-call certificate should be a formula-bearing tableau tree")
    (is (successful?
          (l/run 1 [q]
            (check-proof stripped-program
                         (:system-code system)
                         neg-theorem
                         80
                         proof)
            (l/== true q)))
        "reflected procedure calls inside structural proof certificates must be recovered from encoded system-code clauses")))

(deftest sjas-proof-predicates-check-reflected-calls-without-symbol-registry
  (let [system (demo-system :willard-sjas-tableau0)
        neg-theorem (structural-neg-lit system 'demo sjas/one)
        canonical-one (list 'app (symbol "1"))
        canonical-neg-theorem (list 'neg (list 'app 'demo canonical-one))
        canonical-negated-reflected-body (list 'neq canonical-one canonical-one)
        proof (canonical-structural-tableau-node
                system
                canonical-neg-theorem
                (canonical-structural-tableau-node
                  system
                  canonical-negated-reflected-body))
        stripped-program (-> (:program system)
                             (assoc :clauses nil
                                    :clause-list '()
                                    :alternative-clause-list '()
                                    :guarded-clause-list '())
                             (dissoc :sjas/registry))
        check-proof (var-get #'sjas-profile/sjas-proof-check-programo)]
    (is (zero? (proof-symbol-count proof))
        "the no-registry reflected-call certificate should be a formula-bearing tableau tree")
    (is (not (contains? stripped-program :sjas/registry))
        "the regression must remove the finite source symbol table")
    (is (successful?
          (l/run 1 [q]
            (check-proof stripped-program
                         (:system-code system)
                         neg-theorem
                         80
                         proof)
            (l/== true q)))
        "reflected procedure calls must compare encoded symbol ids from system-code, not host symbol names")))

(deftest sjas-subst-prf-reconstructs-axiom-basis-without-system-registry
  (let [system (demo-system :willard-sjas-tableau0)
        registry (get-in system [:program :sjas/registry])
        beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
        certificate (sjas/proof-certificate 'sjas-axiom)]
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

(deftest sjas-subst-prf-substitution-axiom-branch-validates-system-code
  (let [source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        subst-core-start (str/index-of source "(defn- sjas-subst-prf-coreo")
        subst-core-end (str/index-of source
                                     "(defn- sjas-subst-prf-closeo"
                                     subst-core-start)
        subst-core-source (subs source subst-core-start subst-core-end)]
    (is (str/includes? subst-core-source "sjas-system-code-valid-walked-coreo")
        "subst-prf must validate the supplied finite system code before accepting the substitution-result axiom")
    (is (re-find #"(?s)sjas-system-code-valid-walked-coreo prog\s+system-code.*sjas-subst-code-any-coreo"
                 subst-core-source)
        "system-code validation through equality state must occur in the same substitution-axiom branch as the subst-code check")))

(deftest sjas-proof-predicate-system-code-reconstruction-walks-equality-state
  (let [system (demo-system :willard-sjas-tableau0)
        source (slurp "src/proflog/kernel/willard_sjas_profile.clj")
        tableau-core-source (subs source
                                  (str/index-of source "(defn- sjas-tableau-proof-callo")
                                  (str/index-of source
                                                "(defn- sjas-tableau-proof-closeo"))
        subst-core-source (subs source
                                (str/index-of source "(defn- sjas-subst-prf-coreo")
                                (str/index-of source
                                              "(defn- sjas-subst-prf-closeo"))
        walked-axiom-var (ns-resolve 'proflog.kernel.willard-sjas-profile
                                     'sjas-system-axiom-formula-walked-coreo)
        walked-valid-var (ns-resolve 'proflog.kernel.willard-sjas-profile
                                     'sjas-system-code-valid-walked-coreo)]
    (is (fn? (when walked-axiom-var (var-get walked-axiom-var)))
        "proof predicates need a proof-free AxiomConj relation that reads system-code through sigma")
    (is (fn? (when walked-valid-var (var-get walked-valid-var)))
        "subst-prf needs a proof-free system-code validator that reads system-code through sigma")
    (is (str/includes? tableau-core-source "sjas-system-axiom-formula-walked-coreo")
        "tableau-proof structural certificates must reconstruct AxiomConj after walking system-code")
    (is (str/includes? subst-core-source "sjas-system-axiom-formula-walked-coreo")
        "subst-prf structural certificates must reconstruct AxiomConj after walking system-code")
    (is (str/includes? subst-core-source "sjas-system-code-valid-walked-coreo")
        "subst-prf substitution-result axiom branch must validate walked system-code")
    (when (and walked-axiom-var walked-valid-var)
      (let [walked-axiom-coreo (var-get walked-axiom-var)
            walked-valid-coreo (var-get walked-valid-var)]
        (is (successful?
              (l/run 1 [q]
                (l/fresh [system-code sigma sigma-out walked-system-code axiom-formula
                          eq-proof]
                  (equality/unify-termo system-code (:system-code system) '() sigma eq-proof)
                  (walked-axiom-coreo (:program system)
                                      system-code
                                      sigma
                                      sigma-out
                                      walked-system-code
                                      axiom-formula)
                  (l/== (:system-code system) walked-system-code)
                  (l/== true q))))
            "AxiomConj reconstruction must read a system-code variable already bound in sigma")
        (is (successful?
              (l/run 1 [q]
                (l/fresh [system-code sigma sigma-out walked-system-code eq-proof]
                  (equality/unify-termo system-code (:system-code system) '() sigma eq-proof)
                  (walked-valid-coreo (:program system)
                                      system-code
                                      sigma
                                      sigma-out
                                      walked-system-code)
                  (l/== (:system-code system) walked-system-code)
                  (l/== true q))))
            "system-code validation must read a system-code variable already bound in sigma")))))

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
                               96)
        beta-citation-proof (first-proof beta-citation-proofs)]
    (is (successful? beta-citation-proofs)
        "beta axiom citations must be accepted from encoded system-code beta formulas")
    (is (proof/contains-step? beta-citation-proof
                              'sjas-system-beta-axiom)
        "beta axiom citations must carry the membership evidence (ADR-0091 restored the e248c8b summary)")
    (is (proof/contains-step? beta-citation-proof
                              'sjas-code-arg)
        "compact axiom citations must carry the code-reader evidence inside the membership proof (ADR-0091)")))

(deftest sjas-axiom-citation-counterexamples-adr-0100-size-claim
  (testing "ADR-0102: fixed-size sjas-axiom certificates can cite beta formulas whose J measure exceeds the certificate-size bound"
    (let [depth 8
          large-term (repeated-unary-app-term 'f depth sjas/one)
          large-beta (ast/eq-lit large-term large-term)
          system (sjas/system
                   {:profile :willard-sjas-tableau0
                    :functions {'f 1}
                    :beta [large-beta]})
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          proof-byte-count (count (formal-code-term-bytes axiom-certificate))
          proof-bit-count (* 6 proof-byte-count)
          j (app-occurrence-count large-beta)
          required-bit-count (* 5 j)
          citation-proofs (query/query-succeeds
                            (:program system)
                            (sjas/tableau-proof (:system-code system)
                                                (:code beta-record)
                                                axiom-certificate)
                            1
                            160)]
      (is (successful? citation-proofs)
          "the fixed sjas-axiom certificate is accepted for the large beta axiom")
      (is (= (inc depth) (/ j 2))
          "the witness should have the expected copied unary-app occurrences on both sides of equality, including the base numeral application")
      (is (< proof-bit-count required-bit-count)
          (str "ADR-0100's size(P) >= 5J claim fails for the accepted citation: "
               {:proof-byte-count proof-byte-count
                :proof-bit-count proof-bit-count
                :J j
                :required-bit-count required-bit-count})))))

(deftest sjas-internal-code-builder-yields-canonical-axiom-certificate
  (testing "reverse construction from the fixed axiom proof bytes is the canonical compact certificate (ADR-0095)"
    (let [builder (var-get #'sjas-profile/sjas-internal-code-termo)
          bytes (var-get #'sjas-profile/sjas-axiom-proof-bytes)
          built (l/run 2 [term] (builder bytes term))]
      (is (= 1 (count built))
          "the canonical builder must produce exactly one compact certificate term")
      (is (= (sjas/proof-certificate 'sjas-axiom) (first built))
          "the synthesized certificate must equal the host-encoded sjas-axiom certificate"))))

(deftest sjas-compact-code-reader-rejects-arity-mismatched-terms
  (testing "a code-N constructor whose declared arity differs from its argument count is not a readable public code (ADR-0095 reader hardening)"
    (let [proof-reader (var-get #'sjas-profile/sjas-public-code-byteso)
          core-reader (var-get #'sjas-profile/sjas-public-code-bytes-coreo)
          canonical (sjas/proof-certificate 'sjas-axiom)
          args (drop 2 canonical)
          malformed (apply ast/app-term (sjas-code/code-symbol 2) args)]
      (is (= 3 (count args))
          "the canonical axiom certificate must carry three byte arguments for this regression")
      (is (empty? (l/run 1 [bytes] (l/fresh [proof] (proof-reader malformed bytes proof))))
          "the proof-producing reader must reject a code-2 term carrying three byte arguments")
      (is (empty? (l/run 1 [bytes] (core-reader malformed bytes)))
          "the proof-free reader must reject a code-2 term carrying three byte arguments"))))

(deftest ^:slow sjas-tableau-proof-synthesizes-beta-axiom-citation
  (testing "a fresh proof variable is bound to the citation certificate (ADR-0095)"
    (ast/nom p
      (let [system (demo-system :willard-sjas-tableau0)
            beta-record (first (filter #(= :group-two (:group %))
                                       (:axioms system)))
            records (sjas/query-answers
                      system
                      (sjas/tableau-proof (:system-code system)
                                          (:code beta-record)
                                          (ast/var-term p))
                      [p]
                      {:proof-limit 1
                       :fuel 96
                       :defer-calls? false})]
        (is (seq records) "synthesis must produce an answer")
        (is (= (sjas/proof-certificate 'sjas-axiom)
               (binding-for records p))
            "the synthesized proof code must be the axiom citation certificate")))))

(deftest ^:slow sjas-tableau-proof-synthesizes-selfcons-citation
  (testing "the runtime generates the Henkin proof of its own consistency (ADR-0095)"
    (ast/nom p
      (let [system (demo-system :willard-sjas-tableau0)
            records (sjas/query-answers
                      system
                      (sjas/tableau-proof (:system-code system)
                                          (:code (:group-three system))
                                          (ast/var-term p))
                      [p]
                      {:proof-limit 1
                       :fuel 96
                       :defer-calls? false})]
        (is (seq records) "synthesis must produce an answer")
        (is (= (sjas/proof-certificate 'sjas-axiom)
               (binding-for records p))
            "the synthesized proof of SelfCons must be the Group-3 citation certificate")))))

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

(deftest sjas-axiom-conj-reconstructs-fixed-group-one-axioms
  (testing "AxiomConj(s) includes the same fixed axioms that axiom-member accepts"
    (let [fixed-antecedents (var-get #'sjas-profile/sjas-fixed-proof-antecedent-formulaso)
          antecedent-ast (var-get #'sjas-profile/sjas-proof-antecedent-formula-asto)
          group-one-formula (first (var-get #'sjas-profile/group-one-internal-formulas))
          expected (first
                     (l/run 1 [q]
                       (antecedent-ast group-one-formula q)))
          fixed-formulas (first
                           (l/run 1 [q]
                             (fixed-antecedents q)))]
      (is expected
          "the test must construct the Group-1 antecedent formula through the same relation as AxiomConj")
      (is fixed-formulas
          "the test must reconstruct fixed antecedent formulas through the proof predicate relation")
      (is (some #(= expected %) fixed-formulas)
          "AxiomConj(s) must include fixed Group-1 axioms, not only make them citable by axiom-member/2"))))

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
    (is (successful? citation-proofs)
        "Tableau-0 Group-3 citations must be reconstructed from system-code")
    (is proof)
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
    (is (successful? citation-proofs)
        "Level-1 Group-3 citations must validate the fixed-point skeleton from system-code")
    (is proof)
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
    (is (proof/contains-step? proof 'willard-sjas-subst-proof-check)
        "subst-prf should close through the SJAS substitution proof predicate")
    (is (not (proof/contains-step? proof 'willard-sjas-subst-source-result))
        "subst-prf answer proofs should not adjoin the substitution-code trace to the supplied tableau certificate")))

(deftest sjas-level1-group-three-uses-substitution-proof-vocabulary
  (let [system (demo-system :willard-sjas-level1)
        relations (set (formula-relation-symbols (:formula (:group-three system))))]
    (is (contains? relations 'neg-pair))
    (is (contains? relations 'dsjas-subst-prf))
    (is (not (contains? relations 'subst-prf))
        "Level-1 SelfCons must quantify measured substitution-proof objects")
    (is (not (contains? relations 'tableau-proof)))))

(deftest sjas-level1-group-three-uses-selfcons-skeleton-code
  (let [system (demo-system :willard-sjas-level1)
        skeleton-code (:selfcons-skeleton-code system)
        atoms (formula-atoms (:formula (:group-three system)))
        subst-atoms (filter #(= 'dsjas-subst-prf (second %)) atoms)
        bare-subst-atoms (filter #(= 'subst-prf (second %)) atoms)]
    (is (sjas-code/code-term? skeleton-code))
    (is (= 2 (count subst-atoms)))
    (is (empty? bare-subst-atoms)
        "Level-1 SelfCons must quantify composite proof-object codes, not bare subst-prf proof codes")
    (is (every? #(= (:system-code system) (nth % 2)) subst-atoms))
    (is (every? #(= skeleton-code (nth % 3)) subst-atoms))
    (is (not-any? #(= (:system-code system) (nth % 3)) subst-atoms))))

(deftest dsjas-composite-tableau-proof-object-carries-measured-components
  (testing "composite tableau proof objects encode S, F, and P bytes under one measured proof variable"
    (let [system (demo-system :willard-sjas-tableau0)
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          composite (sjas/dsjas-tableau-proof-object (:system-code system)
                                                     (:code beta-record)
                                                     axiom-certificate)
          decoded (sjas-code/proof-formal-code-term->proof composite)]
      (is (= ['dsjas-tableau-proof-object
              (formal-code-term-bytes (:system-code system))
              (formal-code-term-bytes (:code beta-record))
              (formal-code-term-bytes axiom-certificate)]
             (vec decoded)))
      (is (> (count (formal-code-term-bytes composite))
             (count (formal-code-term-bytes axiom-certificate)))
          "the measured object must include more than the fixed citation marker"))))

(deftest dsjas-tableau-proof-accepts-and-checks-composite-axiom-citations
  (testing "measured tableau proof checks the embedded S/F/P bytes before delegating to tableau-proof"
    (let [system (demo-system :willard-sjas-tableau0)
          wrong-system (demo-system :willard-sjas-level1)
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          composite (sjas/dsjas-tableau-proof-object (:system-code system)
                                                     (:code beta-record)
                                                     axiom-certificate)
          wrong-system-composite (sjas/dsjas-tableau-proof-object
                                   (:system-code wrong-system)
                                   (:code beta-record)
                                   axiom-certificate)
          wrong-theorem-composite (sjas/dsjas-tableau-proof-object
                                    (:system-code system)
                                    (:contradiction-code system)
                                    axiom-certificate)]
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-tableau-proof (:system-code system)
                                        (:code beta-record)
                                        composite)
              1
              160))
          "matching composite citation object must be accepted")
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-tableau-proof (:system-code system)
                                        (:code beta-record)
                                        wrong-system-composite)
              1
              160))
          "mismatched embedded system bytes must be rejected")
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-tableau-proof (:system-code system)
                                        (:code beta-record)
                                        wrong-theorem-composite)
              1
              160))
          "mismatched embedded theorem bytes must be rejected"))))

(deftest dsjas-composite-subst-prf-object-carries-measured-components
  (testing "composite substitution proof objects encode S, G, F, and P bytes under one measured proof variable"
    (let [system (demo-system :willard-sjas-level1)
          group3-record (:group-three system)
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          composite (sjas/dsjas-subst-prf-object (:system-code system)
                                                 (:selfcons-skeleton-code system)
                                                 (:code group3-record)
                                                 axiom-certificate)
          decoded (sjas-code/proof-formal-code-term->proof composite)]
      (is (= ['dsjas-subst-prf-object
              (formal-code-term-bytes (:system-code system))
              (formal-code-term-bytes (:selfcons-skeleton-code system))
              (formal-code-term-bytes (:code group3-record))
              (formal-code-term-bytes axiom-certificate)]
             (vec decoded)))
      (is (> (count (formal-code-term-bytes composite))
             (count (formal-code-term-bytes axiom-certificate)))
          "the measured substitution object must include more than the fixed citation marker"))))

(deftest dsjas-subst-prf-accepts-and-checks-composite-axiom-citations
  (testing "measured subst-prf checks embedded S/G/F/P bytes before applying substitution proof"
    (let [system (demo-system :willard-sjas-tableau0)
          wrong-system (demo-system :willard-sjas-level1)
          beta-record (first (filter #(= :group-two (:group %)) (:axioms system)))
          fixed-record (first (filter #(= :group-zero (:group %)) (:axioms system)))
          axiom-certificate (sjas/proof-certificate 'sjas-axiom)
          composite (sjas/dsjas-subst-prf-object (:system-code system)
                                                 (:code beta-record)
                                                 (:code beta-record)
                                                 axiom-certificate)
          wrong-system-composite
          (sjas/dsjas-subst-prf-object (:system-code wrong-system)
                                       (:code beta-record)
                                       (:code beta-record)
                                       axiom-certificate)
          wrong-substitution-composite
          (sjas/dsjas-subst-prf-object (:system-code system)
                                       (:code fixed-record)
                                       (:code beta-record)
                                       axiom-certificate)
          wrong-theorem-composite
          (sjas/dsjas-subst-prf-object (:system-code system)
                                       (:code beta-record)
                                       (:code fixed-record)
                                       axiom-certificate)]
      (is (successful?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-subst-prf (:system-code system)
                                    (:code beta-record)
                                    (:code beta-record)
                                    composite)
              1
              220))
          "matching composite substitution citation object must be accepted")
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-subst-prf (:system-code system)
                                    (:code beta-record)
                                    (:code beta-record)
                                    wrong-system-composite)
              1
              220))
          "mismatched embedded system bytes must be rejected")
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-subst-prf (:system-code system)
                                    (:code beta-record)
                                    (:code beta-record)
                                    wrong-substitution-composite)
              1
              220))
          "mismatched embedded substitution bytes must be rejected")
      (is (empty?
            (query/query-succeeds
              (:program system)
              (sjas/dsjas-subst-prf (:system-code system)
                                    (:code beta-record)
                                    (:code beta-record)
                                    wrong-theorem-composite)
              1
              220))
          "mismatched embedded theorem bytes must be rejected"))))

(deftest sjas-level1-group-three-rejects-wrong-public-code-representation
  (testing "Level-1 Group-3 is fixed to the presented public s term"
    (ast/nom g
      (let [system (demo-system :willard-sjas-level1
                                {:code-format :u-grounding})
            formula-code-term (var-get #'sjas/formula-code-term)
            compact-system-code (compact-code-term-for (:system-code system))
            compact-skeleton (level1-selfcons-formula compact-system-code
                                                       (ast/var-term g))
            compact-skeleton-code (formula-code-term (:coding-context system)
                                                     compact-skeleton
                                                     {g 'v0}
                                                     (:code-format system))
            wrong-representation-formula
            (level1-selfcons-formula compact-system-code
                                     compact-skeleton-code)
            wrong-representation-code (sjas/formula-code
                                        system
                                        wrong-representation-formula)
            axiom-member-coreo (var-get #'sjas-profile/sjas-axiom-member-coreo)]
        (is (sjas-numeral-term? (:system-code system))
            "the regression must exercise the U-Grounding public system code")
        (is (not= wrong-representation-code
                  (:code (:group-three system)))
            "the theorem code must differ by the embedded public code representation")
        (is (empty?
              (l/run 1 [q]
                (axiom-member-coreo (:program system)
                                    (:system-code system)
                                    wrong-representation-code)
                (l/== true q)))
            "Level-1 axiom-member must reject a compact-code fixed point for a U-Grounding system")))))

(deftest sjas-level1-group-three-restricts-pair-to-pi-star-1
  (let [system (demo-system :willard-sjas-level1)
        group3-relations (set (formula-relation-symbols
                                (:formula (:group-three system))))
        skeleton-relations (set (formula-relation-symbols
                                  (:selfcons-skeleton-formula system)))]
    (is (contains? group3-relations 'pi-star-1-code)
        "Willard 2013 sentence (7): Pair(x,y) requires x to code a Pi-star-1 sentence")
    (is (contains? group3-relations 'neg-pair))
    (is (contains? skeleton-relations 'pi-star-1-code)
        "the fixed-point skeleton must carry the same Pi-star-1 restriction")))

(deftest sjas-system-rejects-non-pi-star-1-reflected-basis
  (testing "Definition 5.1 requires the reflected basis to have Pi*1 encodings"
    (ast/nom x y
      (let [unbounded (ast/exists-form x
                        (sjas/lt (ast/var-term x) sjas/two))]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Pi\*1"
              (sjas/system {:profile :willard-sjas-level1
                            :beta [unbounded]})))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Pi\*1"
              (sjas/system {:profile :willard-sjas-tableau0
                            :relations {'bad 1}
                            :reflected-clauses
                            [(ast/clause 'bad [y]
                               (ast/forall-form x
                                 (sjas/lt (ast/var-term x)
                                          (ast/var-term y))))]}))
            "a universal clause body negates to a positive existential and has no Pi*1 encoding")
        (is (map? (sjas/system {:profile :willard-sjas-tableau0
                                :relations {'ok 1}
                                :reflected-clauses
                                [(ast/clause 'ok [y]
                                   (ast/exists-form x
                                     (sjas/lt (ast/var-term x)
                                              (ast/var-term y))))]}))
            "an antecedent existential prenexes universally and stays admissible (ADR-0092)")
        (is (map? (sjas/system {:profile :willard-sjas-tableau0
                                :relations {'demo 1}
                                :beta [(ast/eq-lit sjas/one sjas/one)]
                                :reflected-clauses
                                [(ast/clause 'demo [y]
                                   (ast/eq-lit (ast/var-term y) sjas/one))]}))
            "worked-example shapes must remain buildable")))))

(deftest sjas-syntax-class-predicates-accept-implies-codes
  (testing "object-language Delta-star-0 classification covers implies codes"
    (ast/nom x
      (let [system (demo-system :willard-sjas-tableau0)
            formula (ast/forall-form x
                      (ast/implies-form
                        (ast/eq-lit (ast/var-term x) sjas/one)
                        (sjas/leq (ast/var-term x) sjas/two)))
            code (sjas/formula-code system formula)]
        (is (successful?
              (query/query-succeeds (:program system)
                                    (sjas/pi-star-1-code code)
                                    1
                                    160)))))))

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
  (testing "an explicitly inconsistent reflected basis can cite the real contradiction target"
    (let [system (sjas/system {:profile :willard-sjas-tableau0
                               :beta [(ast/eq-lit sjas/zero sjas/one)]})
          contradiction-certificate (sjas/proof-certificate 'sjas-axiom)]
      (is (some #(and (= :group-two (:group %))
                      (= (:contradiction-code system) (:code %)))
                (:axioms system))
          "the inconsistent beta basis must encode 0=1 as a reflected axiom")
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
        builder-source (slurp "src/proflog/willard_sjas.clj")
        non-axiom-start (str/index-of profile-source
                                      "(defn- decode-non-sjas-axiom-proof-codeo")
        non-axiom-end (str/index-of profile-source
                                    "(defn- sjas-system-profile-tago"
                                    non-axiom-start)
        non-axiom-source (subs profile-source non-axiom-start non-axiom-end)
        tableau-core-start (str/index-of profile-source
                                         "(defn- sjas-tableau-proof-callo")
        tableau-core-end (str/index-of profile-source
                                       "(defn- sjas-tableau-proof-closeo"
                                       tableau-core-start)
        tableau-core-source (subs profile-source
                                  tableau-core-start
                                  tableau-core-end)
        tableau-close-start (str/index-of profile-source
                                          "(defn- sjas-tableau-proof-closeo")
        tableau-close-end (str/index-of profile-source
                                        "(defn- sjas-subst-prf-coreo"
                                        tableau-close-start)
        tableau-close-source (subs profile-source
                                   tableau-close-start
                                   tableau-close-end)
        subst-core-start (str/index-of profile-source
                                       "(defn- sjas-subst-prf-coreo")
        subst-core-end (str/index-of profile-source
                                     "(defn- sjas-subst-prf-closeo"
                                     subst-core-start)
        subst-core-source (subs profile-source
                                subst-core-start
                                subst-core-end)
        subst-close-start (str/index-of profile-source
                                        "(defn- sjas-subst-prf-closeo")
        subst-close-end (str/index-of profile-source
                                      "(defn- sjas-tableau-proof-structural-closeo"
                                      subst-close-start)
        subst-close-source (subs profile-source
                                 subst-close-start
                                 subst-close-end)
        reflected-antecedent-start (str/index-of
                                     profile-source
                                     "(defn- decode-reflected-proof-antecedent-formulaso")
        reflected-antecedent-end (str/index-of
                                   profile-source
                                   "(defn- sjas-fixed-proof-antecedent-formulaso"
                                   reflected-antecedent-start)
        reflected-antecedent-source (subs profile-source
                                          reflected-antecedent-start
                                          reflected-antecedent-end)
        fixed-antecedent-start (str/index-of
                                 profile-source
                                 "(defn- sjas-fixed-proof-antecedent-formulaso")
        fixed-antecedent-end (str/index-of
                               profile-source
                               "(defn- sjas-system-group-three-proof-antecedento"
                               fixed-antecedent-start)
        fixed-antecedent-source (subs profile-source
                                      fixed-antecedent-start
                                      fixed-antecedent-end)
        beta-member-start (str/index-of
                            profile-source
                            "(defn- sjas-beta-member-in-formula-byteso")
        beta-member-end (str/index-of
                          profile-source
                          "(defn- sjas-system-beta-formula-byteso"
                          beta-member-start)
        beta-member-source (subs profile-source beta-member-start beta-member-end)
        object-symbol-start (str/index-of profile-source
                                          "(defn- sjas-object-symbol-indexo")
        object-symbol-end (str/index-of profile-source
                                        "(declare decode-formula-byteso"
                                        object-symbol-start)
        object-symbol-source (subs profile-source
                                   object-symbol-start
                                   object-symbol-end)
        code-byte-start (str/index-of profile-source
                                      "(defn- code-byte-termo")
        code-byte-end (str/index-of profile-source
                                    "(defn- code-constructoro"
                                    code-byte-start)
        code-byte-source (subs profile-source code-byte-start code-byte-end)
        internal-code-start (str/index-of profile-source
                                          "(defn- sjas-internal-code-termo")
        internal-code-end (str/index-of profile-source
                                        "(defn- sjas-internal-term-list-asto"
                                        internal-code-start)
        internal-code-source (subs profile-source
                                   internal-code-start
                                   internal-code-end)
        app-arity-start (str/index-of profile-source
                                      "(defn- decode-app-arityo")
        app-arity-end (str/index-of profile-source
                                    "(defn- decode-syntax-app-termo"
                                    app-arity-start)
        app-arity-source (subs profile-source app-arity-start app-arity-end)
        skip-app-arity-start (str/index-of profile-source
                                           "(defn- skip-syntax-app-arityo")
        skip-app-arity-end (str/index-of profile-source
                                         "(defn- skip-syntax-app-termo"
                                         skip-app-arity-start)
        skip-app-arity-source (subs profile-source
                                    skip-app-arity-start
                                    skip-app-arity-end)
        skip-formula-start (str/index-of profile-source
                                         "(defn- skip-formula-byteso")
        skip-formula-end (str/index-of profile-source
                                       "(defn- reflected-head-argso"
                                       skip-formula-start)
        skip-formula-source (subs profile-source
                                  skip-formula-start
                                  skip-formula-end)
        skip-syntax-term-start (str/index-of profile-source
                                             "(defn- skip-syntax-term-byteso")
        skip-syntax-term-end (str/index-of profile-source
                                           "(defn- skip-syntax-formula-byteso"
                                           skip-syntax-term-start)
        skip-syntax-term-source (subs profile-source
                                      skip-syntax-term-start
                                      skip-syntax-term-end)
        skip-syntax-formula-start (str/index-of
                                    profile-source
                                    "(defn- skip-syntax-formula-byteso")
        skip-syntax-formula-end (str/index-of
                                  profile-source
                                  "(defn- sjas-decode-syntax-formula-code-proofo"
                                  skip-syntax-formula-start)
        skip-syntax-formula-source (subs profile-source
                                         skip-syntax-formula-start
                                         skip-syntax-formula-end)
        reflected-call-start (str/index-of
                               profile-source
                               "(defn- sjas-system-reflected-call-clauseo")
        reflected-call-end (str/index-of
                             profile-source
                             "(defn- sjas-system-reflected-call-alternativeso"
                             reflected-call-start)
        reflected-call-source (subs profile-source
                                    reflected-call-start
                                    reflected-call-end)
        reflected-alternatives-start (str/index-of
                                       profile-source
                                       "(defn- sjas-system-reflected-call-alternativeso")
        reflected-alternatives-end (str/index-of
                                     profile-source
                                     "(defn- sjas-system-reflected-guarded-call-alternativeso"
                                     reflected-alternatives-start)
        reflected-alternatives-source (subs profile-source
                                           reflected-alternatives-start
                                           reflected-alternatives-end)
        reflected-guarded-start (str/index-of
                                  profile-source
                                  "(defn- sjas-system-reflected-guarded-call-alternativeso")
        reflected-guarded-end (str/index-of
                                profile-source
                                "(defn- reflected-member-in-clauseso"
                                reflected-guarded-start)
        reflected-guarded-source (subs profile-source
                                       reflected-guarded-start
                                       reflected-guarded-end)
        conjuncts-start (str/index-of profile-source
                                      "(defn- internal-formula-conjunctso")
        conjuncts-end (str/index-of profile-source
                                    "(defn- internal-formula-list-negated-asto"
                                    conjuncts-start)
        conjuncts-source (subs profile-source conjuncts-start conjuncts-end)
        leading-exists-start (str/index-of
                               profile-source
                               "(defn- internal-leading-exists-scopeo")
        leading-exists-end (str/index-of
                             profile-source
                             "(defn- internal-guarded-alternative-asto"
                             leading-exists-start)
        leading-exists-source (subs profile-source
                                    leading-exists-start
                                    leading-exists-end)]
    (is (not (re-find #"prove-program-host" profile-source)))
    (is (not (re-find #"host-proof" profile-source)))
    (is (not (re-find #"whole-formula" profile-source)))
    (is (not (re-find #"mini-closed" profile-source)))
    (is (not (re-find #"kernel/prove-programo target" profile-source))
        "SJAS proof predicates must not short-circuit non-axiom certificates through the host proof kernel")
    (is (str/includes? non-axiom-source "decode-structural-proof-byteso")
        "non-axiom proof predicates must decode formula-bearing tableau trees structurally")
    (is (not (str/includes? non-axiom-source "decode-proof-byteso"))
        "non-axiom proof predicates must not decode legacy symbolic proof traces")
    (is (not (re-find #"sjas-proof-check-close-agendao" profile-source))
        "SJAS proof predicates must not retain the legacy proof-trace agenda checker")
    (is (not (re-find #"sjas-saved-positive-call-closeso" profile-source))
        "SJAS proof predicates must not retain proof-trace saved positive-call helpers")
    (is (not (re-find #"sjas-close-guarded-negated-alternativeo" profile-source))
        "SJAS proof predicates must not retain guarded proof-trace sequence helpers")
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
    (is (not (re-find #"(?s)defn- sjas-tableau-proof-closeo(?:(?!\n\(defn-).)*\(conda" profile-source))
        "tableau-proof proof-code classification must be a relation, not a committed-choice scheduler")
    (is (not (re-find #"(?s)defn- sjas-axiom-membero(?:(?!\n\(defn-).)*\(conda" profile-source))
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
    (is (not (re-find #"(?s)\(list 'profiled\s+'willard-sjas-proof-check\s+proof-read-proof\s+theorem-read-proof\s+decoded-proof\)"
                      profile-source))
        "tableau-proof answer evidence must not adjoin code-reader and decoded-proof traces to the supplied tableau proof code")
    (is (not (re-find #"(?s)\(list 'profiled 'willard-sjas-subst-proof-check\s+proof-read-proof\s+theorem-read-proof\s+decoded-proof\)"
                      profile-source))
        "subst-prf answer evidence must not adjoin code-reader and decoded-proof traces to the supplied tableau proof code")
    (is (str/includes? tableau-close-source "sjas-tableau-proof-callo")
        "tableau-proof wrapper must destructure the call through the shared object preamble")
    (is (str/includes? tableau-close-source "sjas-walked-axiom-membero")
        "tableau-proof citation answers must validate membership through the proof-bearing object relation (ADR-0091)")
    (is (str/includes? tableau-close-source "sjas-tableau-proof-structural-coreo")
        "tableau-proof structural answers must delegate to the proof-free object relation")
    (is (str/includes? tableau-close-source "willard-sjas-proof-check member-proof")
        "tableau-proof citation answers must nest the membership evidence inside the profile wrapper (ADR-0091)")
    (is (str/includes? tableau-core-source "sjas-formal-code-bytes-coreo proof-code")
        "tableau-proof must read proof-code bytes once before classifying certificate shape")
    (is (str/includes? tableau-core-source "sjas-axiom-proof-bytes")
        "tableau-proof must recognize axiom certificates from decoded proof bytes")
    (is (str/includes? tableau-core-source "decode-structural-proof-bytes-coreo")
        "tableau-proof must decode structural certificates from the already-read proof bytes")
    (is (str/includes? tableau-core-source "sjas-structural-negated-theorem-coreo")
        "tableau-proof must decode theorem codes without auxiliary theorem-code traces")
    (is (str/includes? tableau-core-source "sjas-system-axiom-formula-walked-coreo")
        "tableau-proof must reconstruct AxiomConj through proof-free object relations after walking system-code")
    (is (str/includes? tableau-core-source "sjas-walked-axiom-member-coreo")
        "tableau-proof axiom citation must check finite-system membership without auxiliary proof traces")
    (is (not (str/includes? tableau-core-source "decode-sjas-axiom-proof-codeo"))
        "tableau-proof must not call the proof-producing axiom certificate decoder")
    (is (not (str/includes? tableau-core-source "decode-non-sjas-axiom-proof-codeo"))
        "tableau-proof must not call the proof-producing structural certificate decoder")
    (is (not (str/includes? tableau-core-source "decode-sjas-axiom-proof-code-coreo"))
        "tableau-proof must not re-read proof-code bytes separately for the axiom branch")
    (is (not (str/includes? tableau-core-source "decode-non-sjas-axiom-proof-code-coreo"))
        "tableau-proof must not re-read proof-code bytes separately for the structural branch")
    (is (str/includes? subst-close-source "sjas-subst-prf-coreo")
        "subst-prf wrapper must delegate to the proof-free object relation")
    (is (str/includes? subst-core-source "sjas-formal-code-bytes-coreo proof-code")
        "subst-prf must read proof-code bytes once before classifying certificate shape")
    (is (str/includes? subst-core-source "sjas-axiom-proof-bytes")
        "subst-prf must recognize axiom certificates from decoded proof bytes")
    (is (str/includes? subst-core-source "decode-structural-proof-bytes-coreo")
        "subst-prf must decode structural certificates from the already-read proof bytes")
    (is (str/includes? subst-core-source "sjas-structural-negated-theorem-coreo")
        "subst-prf must decode theorem codes without auxiliary theorem-code traces")
    (is (str/includes? subst-core-source "sjas-system-axiom-formula-walked-coreo")
        "subst-prf must reconstruct AxiomConj through proof-free object relations after walking system-code")
    (is (str/includes? subst-core-source "sjas-walked-axiom-member-coreo")
        "subst-prf axiom citation must check finite-system membership without auxiliary proof traces")
    (is (not (str/includes? subst-core-source "decode-sjas-axiom-proof-codeo"))
        "subst-prf must not call the proof-producing axiom certificate decoder")
    (is (not (str/includes? subst-core-source "decode-non-sjas-axiom-proof-codeo"))
        "subst-prf must not call the proof-producing structural certificate decoder")
    (is (not (str/includes? subst-core-source "decode-sjas-axiom-proof-code-coreo"))
        "subst-prf must not re-read proof-code bytes separately for the axiom branch")
    (is (not (str/includes? subst-core-source "decode-non-sjas-axiom-proof-code-coreo"))
        "subst-prf must not re-read proof-code bytes separately for the structural branch")
    (is (not (str/includes? reflected-antecedent-source "conda"))
        "reflected axiom antecedent reconstruction must be a relation over reflected records, not committed-choice fallback decoding")
    (is (str/includes? fixed-antecedent-source "group-zero-internal-formulas")
        "AxiomConj fixed antecedents must include Group-0 axioms")
    (is (str/includes? fixed-antecedent-source "group-one-internal-formulas")
        "AxiomConj fixed antecedents must include Group-1 axioms")
    (is (not (str/includes? fixed-antecedent-source "(first group-zero-internal-formulas)"))
        "AxiomConj fixed antecedents must not hard-code only the first Group-0 formulas")
    (is (not (str/includes? fixed-antecedent-source "(second group-zero-internal-formulas)"))
        "AxiomConj fixed antecedents must not hard-code only the second Group-0 formula")
    (is (not (str/includes? beta-member-source "conda"))
        "beta axiom membership scans must use explicit encoded-formula match/nonmatch relations, not committed-choice fallback")
    (is (not (str/includes? object-symbol-source "conda"))
        "proof-facing object symbol decoding must use disjoint reserved/user symbol relations, not committed-choice fallback")
    (is (not (str/includes? code-byte-source "conda"))
        "compact byte decoding must relate presented and generated byte numerals without committed-choice search control")
    (is (not (str/includes? code-byte-source "bits->canonical-termo bits term"))
        "compact byte decoding must not build unused canonical numeral proof evidence")
    (is (re-find #"compact-code-byte-bits-termo term bits\)\s+\(byte-bitso bits byte\)"
                 code-byte-source)
        "compact byte decoding must parse the presented numeral before consulting the finite byte relation")
    (is (str/includes? code-byte-source "code-byte-build-termo")
        "compact byte decoding must expose a separate byte-first builder mode for embedded code payload reconstruction")
    (is (str/includes? internal-code-source "code-args-build-counto")
        "embedded code payload reconstruction must use the counted byte-first builder mode")
    (is (not (str/includes? internal-code-source "code-argso"))
        "embedded code payload reconstruction must not use the public code reader mode")
    (is (not (str/includes? app-arity-source "conda"))
        "application arity decoding must be a finite relation over the encoded arity byte, not committed-choice recursion")
    (is (not (str/includes? skip-app-arity-source "conda"))
        "syntax-skip application arity decoding must be a finite relation over the encoded arity byte, not committed-choice recursion")
    (is (str/includes? skip-formula-source "skip-syntax-formula-byteso")
        "system-code beta-block scans must advance structurally without decoding discarded formula trees")
    (is (not (str/includes? skip-formula-source "decode-syntax-formula-byteso"))
        "system-code beta-block scans must not materialize discarded beta formula syntax")
    (is (not (str/includes? skip-syntax-term-source "conda"))
        "syntax term skipping must be an ordinary structural relation, not committed-choice grammar dispatch")
    (is (not (str/includes? skip-syntax-formula-source "conda"))
        "syntax formula skipping must be an ordinary structural relation, not committed-choice grammar dispatch")
    (is (str/includes? reflected-call-source "sjas-public-code-bytes-coreo")
        "reflected procedure-call resolution must read system-code bytes without auxiliary byte-read proof traces")
    (is (not (re-find #"\(sjas-public-code-byteso\b" reflected-call-source))
        "reflected procedure-call resolution must not materialize unused system-code read proof evidence")
    (is (str/includes? reflected-alternatives-source "sjas-public-code-bytes-coreo")
        "reflected alternative collection must read system-code bytes without auxiliary byte-read proof traces")
    (is (not (re-find #"\(sjas-public-code-byteso\b" reflected-alternatives-source))
        "reflected alternative collection must not materialize unused system-code read proof evidence")
    (is (str/includes? reflected-guarded-source "sjas-public-code-bytes-coreo")
        "guarded reflected alternative collection must read system-code bytes without auxiliary byte-read proof traces")
    (is (not (re-find #"\(sjas-public-code-byteso\b" reflected-guarded-source))
        "guarded reflected alternative collection must not materialize unused system-code read proof evidence")
    (is (not (str/includes? conjuncts-source "conda"))
        "guarded reflected conjunction flattening must use explicit non-and structure, not committed-choice fallback")
    (is (not (str/includes? leading-exists-source "conda"))
        "guarded reflected existential scope stripping must use explicit non-exists structure, not committed-choice fallback")
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
