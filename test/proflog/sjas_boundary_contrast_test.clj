(ns proflog.sjas-boundary-contrast-test
  "ADR-0146: executable behavioral contrast across Willard's self-justification
   boundary (ADR-0142's target demonstration, per the falsifier guardrail).

   Two systems are built with the SAME profile, vocabulary (squaring-chain
   constants, `pow`, mul relations), coding format, and shared beta seed; they
   differ ONLY in whether the multiplication-totality content (the complete
   equational mul basis + the Willard V4/V5 route axioms + the squaring-chain
   witnesses) is in the reflected beta basis. Formula codes are asserted
   byte-identical across the sides, so every contrast below is purely about
   what the system RECOGNIZES (basis membership), never about coding drift.

   The demonstration routes through the INTERPRETED bounded-proof V-route
   (`sjas-semprfk-alpha-structural-closeo`) on a real constructed proof code —
   exactly the route the ADR-0142 falsifier guardrail mandates (a syntactic
   SemPrf clash would close over EVERY system and erase the boundary):

   - T1 (the boundary): the same constructed cut-free leaf
     `neg SemPrf^k(code(S), 1, code(mul-totality-axiom), sjas-axiom-cite,
     2^(p+1))` CLOSES over the mul-total side and FAILS over the addition-only
     side. The addition-side failure is this namespace's falsifier: any change
     that makes it close (e.g. promoting semprf to a named clash) breaks here.
   - T2 (control): the same leaf citing the SHARED beta axiom closes over BOTH
     sides — the addition side's V-route machinery (decode, membership, Log
     bound) is fully functional; T1's failure is exactly non-membership of the
     multiplication-totality content.
   - T3 (non-vacuity): a too-small tower bound fails ON THE MUL SIDE — the
     mul-side closure is genuinely Log-bound-checked, not profile-granted.
   - T4 (localization): the step-5 premise-clash witness-binding tree closes
     over BOTH sides — the alpha/gamma/beta/clash plumbing is boundary-blind;
     the boundary lives at the V-route membership step alone.

   System-level statement: `total-multiplication-hypothesis-report` returns
   :theorem-hypotheses-satisfied? true for the mul side and false for the
   addition side (a profile label alone cannot satisfy it)."
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.nominal :as nominal]
            [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.sjas-tree-builder :as tb]
            [proflog.willard-sjas :as sjas]
            [proflog.willard-sjas-code :as sjas-code]))

(def ^:private chain-depth 1)

(def ^:private shared-beta
  [(ast/eq-lit sjas/one sjas/one)])

(def ^:private mul-side
  "Multiplication-total side: the canonical complete Type-M system (complete
   equational mul basis + V4/V5 route axioms + squaring-chain witnesses)."
  (delay
    (sjas/total-multiplication-complete-system
      {:functions {'pow 2}
       :beta shared-beta
       :code-format :u-grounding
       :depth chain-depth})))

(def ^:private add-side
  "Addition-only side: IDENTICAL vocabulary, profile, and coding; the beta
   basis carries only the shared seed — multiplication is declared but never
   recognized as total."
  (delay
    (sjas/system {:profile :willard-sjas-total-multiplication
                  :constants (vec (sjas/total-multiplication-squaring-chain-constants
                                    chain-depth))
                  :functions (merge {'pow 2} sjas/total-multiplication-functions)
                  :relations sjas/total-multiplication-willard-relations
                  :beta shared-beta
                  :code-format :u-grounding})))

(def ^:private mul-totality-axiom
  (delay (first (sjas/total-multiplication-complete-axioms))))

(def ^:private axiom-cite
  (delay (sjas/proof-certificate 'sjas-axiom {:code-format :u-grounding})))

(def ^:private cite-value
  (delay (sjas-code/bytes->u-grounding-code-value
           (sjas-code/u-grounding-code-term-bytes @axiom-cite))))

(defn- pow-bound [exp]
  (ast/app-term 'pow (sjas/numeral 2) (sjas/numeral exp)))

(defn- semprfk-neg-leaf
  "The NNF bounded-proof literal `neg SemPrf^k(code(S), 1, thm, cite, bound)`."
  [system theorem-code bound]
  (ast/neg-lit
    (second (sjas/semprfk-alpha (:system-code system) sjas/one
                                theorem-code @axiom-cite bound))))

(defn- closes? [system leaf]
  (tb/valid-tree? system leaf (tb/flex-tableau-node system leaf) 80))

(deftest boundary-sides-share-codes-and-split-hypotheses
  (testing "identical vocabulary => identical formula codes; only the recognized basis differs"
    (is (= (sjas/formula-code @mul-side @mul-totality-axiom)
           (sjas/formula-code @add-side @mul-totality-axiom))
        "the mul-totality axiom encodes to identical bytes on both sides")
    (is (= (sjas/formula-code @mul-side (first shared-beta))
           (sjas/formula-code @add-side (first shared-beta)))
        "the shared beta axiom encodes to identical bytes on both sides"))
  (testing "the Type-M theorem hypotheses split the sides at system level"
    (is (true? (:theorem-hypotheses-satisfied?
                 (sjas/total-multiplication-hypothesis-report @mul-side)))
        "the mul-total side satisfies Willard's Type-M hypotheses")
    (is (false? (:theorem-hypotheses-satisfied?
                  (sjas/total-multiplication-hypothesis-report @add-side)))
        "the addition-only side does not (profile label alone is insufficient)")))

(deftest v-route-closes-mul-side-and-fails-addition-side
  (testing "T1: the interpreted bounded-proof V-route distinguishes the sides"
    (let [bound (pow-bound (inc @cite-value))
          mul-leaf (semprfk-neg-leaf @mul-side
                                     (sjas/formula-code @mul-side @mul-totality-axiom)
                                     bound)
          add-leaf (semprfk-neg-leaf @add-side
                                     (sjas/formula-code @add-side @mul-totality-axiom)
                                     bound)]
      (is (closes? @mul-side mul-leaf)
          "citing the mul-totality axiom closes over the side that recognizes it")
      (is (not (closes? @add-side add-leaf))
          (str "the SAME constructed leaf fails over the addition-only side -- "
               "this non-closure IS the falsifier; a syntactic-clash shortcut "
               "that made it close would erase Willard's boundary")))))

(deftest v-route-control-closes-on-both-sides
  (testing "T2: citing the SHARED axiom closes over both sides"
    (let [bound (pow-bound (inc @cite-value))]
      (is (closes? @mul-side
                   (semprfk-neg-leaf @mul-side
                                     (sjas/formula-code @mul-side (first shared-beta))
                                     bound))
          "control closes on the mul side")
      (is (closes? @add-side
                   (semprfk-neg-leaf @add-side
                                     (sjas/formula-code @add-side (first shared-beta))
                                     bound))
          (str "control closes on the addition side too: its V-route machinery "
               "(decode, membership, Log bound) is intact, so T1's failure is "
               "exactly non-membership of the multiplication-totality content")))))

(deftest v-route-bound-still-checked-on-mul-side
  (testing "T3: a too-small tower bound fails on the MUL side (closure is not profile-granted)"
    (is (not (closes? @mul-side
                      (semprfk-neg-leaf @mul-side
                                        (sjas/formula-code @mul-side @mul-totality-axiom)
                                        (pow-bound @cite-value))))
        "Log(2^p,1)=p rejects proof<p: the mul-side closure is genuinely bound-checked")))

(defn- subst-universal [arg]
  (let [h (nominal/nom (l/lvar 'wh))]
    (ast/forall-form h (ast/neg-lit (ast/app-term 'subst-code arg (ast/var-term h))))))

(def ^:private canonical-neg-subst-one-v0
  (list 'neg (list 'app 'subst-code (list 'app (symbol "1")) (list 'var 'v0))))

(defn- premise-clash-closes? [system]
  (let [univ (subst-universal sjas/one)
        premise (sjas/subst-code sjas/one (sjas/numeral 5))
        target (ast/and-form premise univ)
        proof (tb/flex-tableau-node system target
                (tb/flex-tableau-node system premise
                  (tb/flex-tableau-node system univ
                    (tb/canonical-flex-tableau-node system canonical-neg-subst-one-v0))))]
    (tb/valid-tree? system target proof 80)))

(deftest premise-clash-plumbing-is-boundary-blind
  (testing "T4: step 5's witness-binding tree closes over BOTH sides"
    (is (premise-clash-closes? @mul-side)
        "alpha/gamma/beta/clash close the premise-clash tree on the mul side")
    (is (premise-clash-closes? @add-side)
        (str "and identically on the addition side -- the tableau plumbing is "
             "boundary-blind; Willard's boundary is localized to the V-route "
             "membership step demonstrated in T1"))))
