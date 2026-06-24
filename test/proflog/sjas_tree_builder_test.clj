(ns proflog.sjas-tree-builder-test
  "ADR-0142 Phase 3: the ordinary SJAS checker validates constructed cut-free
   tableau trees over the EXACT generated multiplication-total system.

   Phase 0 characterized the checker as a validator of constructed cut-free trees
   over a demo system. These tests promote that result to the real generated
   `:willard-sjas-total-multiplication` system (the system the Theorem 2.3 closure
   must close over) and pin down which closing rules the diagonal path may rely
   on. They are the committed construct-and-check baseline every closure step is
   built on; they make no claim that BOT is derived."
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]))

(defn- mul-system
  "The exact generated multiplication-total system used by the Theorem 2.3
   closure assembler (matches proflog.sjas-theorem23-closure-test/mul-system)."
  []
  (sjas/system {:profile :willard-sjas-total-multiplication
                :functions sjas/total-multiplication-functions
                :relations sjas/total-multiplication-willard-relations
                :beta [(ast/eq-lit sjas/one sjas/one)]
                :code-format :u-grounding}))

(def ^:private refl
  "A reflexive disequality: closes by same-term recognition, relation-independent."
  (ast/neq-lit sjas/one sjas/one))

(deftest checker-validates-constructed-cut-free-trees-over-the-real-mul-system
  (testing "the ordinary checker accepts constructed cut-free tableau trees over the exact generated multiplication-total system"
    (let [system (mul-system)]
      ;; Reflexive disequality closure (relation-independent).
      (is (tb/valid-tree? system refl (tb/flex-tableau-node system refl))
          "(neq one one) closes by reflexive same-term recognition over the mul system")
      ;; Conjunction expansion: the left conjunct closes the branch.
      (let [tgt (ast/and-form refl refl)]
        (is (tb/valid-tree? system tgt
              (tb/flex-tableau-node system tgt
                (tb/flex-tableau-node system refl)))
            "a conjunction whose left conjunct closes is accepted"))
      ;; Nested conjunction: leftmost closes.
      (let [tgt (ast/and-form refl (ast/and-form refl refl))]
        (is (tb/valid-tree? system tgt
              (tb/flex-tableau-node system tgt
                (tb/flex-tableau-node system refl)))
            "nested conjunction with a closing leftmost conjunct is accepted"))
      ;; Double negation reduces to the closing formula.
      (let [tgt (ast/not-form (ast/not-form refl))]
        (is (tb/valid-tree? system tgt
              (tb/flex-tableau-node system tgt
                (tb/flex-tableau-node system refl)))
            "double negation of a closing formula is accepted")))))

(deftest flex-builder-auto-selects-narrow-and-wide-node-shapes
  (testing "flex-tableau-node picks the narrow (count-prefixed) shape under 64 bytes and the wide (byte-list) shape at or above it, and both validate"
    (let [system (mul-system)
          ;; A small closing formula -> narrow node.
          narrow-node (tb/flex-tableau-node system refl)
          ;; A wide closing formula: a reflexive disequality over a ~60-digit
          ;; numeral encodes to > 64 formula bytes.
          big (sjas/numeral (bigint 1e60))
          wide-target (ast/neq-lit big big)
          wide-node (tb/flex-tableau-node system wide-target)]
      (is (integer? (first narrow-node))
          "a narrow node is prefixed with an integer formula-byte count")
      (is (>= (count (tb/formula-code-bytes system wide-target)) 64)
          "the chosen wide target encodes to at least 64 formula bytes")
      (is (seq? (first wide-node))
          "a wide node carries its formula bytes as a leading byte list")
      (is (tb/valid-tree? system refl narrow-node)
          "the narrow node validates")
      (is (tb/valid-tree? system wide-target wide-node)
          "the wide node validates"))))

(deftest complementary-closure-uses-named-primitives-not-opaque-user-relations
  (testing "complementary pos/neg closure fires for reserved/primitive relations (named decode) but not for opaque user relations ((sym n) decode)"
    (let [system (mul-system)
          ;; A user relation added only for this characterization.
          system+opaque (sjas/system
                          {:profile :willard-sjas-total-multiplication
                           :functions sjas/total-multiplication-functions
                           :relations (assoc sjas/total-multiplication-willard-relations
                                             'opaque 1)
                           :beta [(ast/eq-lit sjas/one sjas/one)]
                           :code-format :u-grounding})
          clash (fn [sys atom]
                  (let [tgt (ast/and-form (ast/pos-lit atom) (ast/neg-lit atom))]
                    (tb/valid-tree? sys tgt
                      (tb/flex-tableau-node sys tgt
                        (tb/flex-tableau-node sys (ast/pos-lit atom)
                          (tb/flex-tableau-node sys (ast/neg-lit atom)))))))]
      ;; subst-code is a globally-reserved U-Grounding primitive: it decodes to a
      ;; named atom, so its complementary literals close. This is exactly the
      ;; closing rule the Theorem 2.3 diagonal path uses (the Subst conjunct).
      (is (clash system (ast/app-term 'subst-code sjas/one sjas/one sjas/one))
          "a subst-code pos/neg clash closes (reserved/named primitive)")
      ;; An opaque user relation decodes to (sym n) in U-Grounding (proof-facing)
      ;; mode -- the source symbol table is removed -- and does NOT close by raw
      ;; syntactic clash. The diagonal closure never relies on this: its non-Subst
      ;; closures go through the profile interpretation of semprf/semprfk, not a
      ;; user-relation literal clash.
      (is (not (clash system+opaque (ast/app-term 'opaque sjas/zero)))
          "an opaque user-relation pos/neg clash does not close syntactically")
      ;; The profile relations are likewise opaque to syntactic clash; they close
      ;; by interpretation, not by complementary literals.
      (is (not (clash system (ast/app-term 'semprfk-alpha
                                           sjas/zero sjas/zero sjas/zero
                                           sjas/zero sjas/zero)))
          "a profile-relation pos/neg clash does not close syntactically (it closes by interpretation)"))))
