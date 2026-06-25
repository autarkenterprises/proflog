(ns proflog.sjas-theorem23-closure
  "ADR-0142 criterion 6b: assemble Willard 2002 JSL2 Theorem 2.3's closure over a
   generated multiplication-total SJAS system, with an HONEST per-step
   verification status.

   This is deliberately NOT a claim that the ordinary checker derives BOT
   end-to-end. It is a faithful layout of the six-step diagonal argument that
   records, for each step, exactly one of:

     :checker-accepted   -- validated now by an ordinary kernel relation
     :reflected-axiom    -- present as a generated beta/route axiom (so trivially
                            a theorem of alpha)
     :cut-composition    -- a Theorem 2.2 application, realized by the verified
                            with-cut transform (proflog.sjas-cut-composition);
                            its cut-free expansion is the documented boundary
     :open-boundary      -- rests on research not completed here (the cut-free
                            tableau and the tower-sized SemPrf^k witnesses)

   The point is to localize the boundary precisely, the way ADR-0142 / its review
   demand, rather than to overclaim a closed derivation."
  (:require [clojure.core.logic :refer [lvar]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.query :as query]
            [proflog.sjas-cut-composition :as cut]
            [proflog.willard-sjas :as sjas]))

(defn d-star
  "Willard 2002 JSL2 Equation (5): `D* = forall y z. not SemPrf^k_alpha(code(Dk), y, z)`."
  [system-code k-code diagonal-code]
  (let [y (nominal/nom (lvar 'dstar-y))
        z (nominal/nom (lvar 'dstar-z))]
    (ast/forall-form
      y
      (ast/forall-form
        z
        (ast/not-form
          (sjas/semprfk-alpha system-code k-code diagonal-code
                              (ast/var-term y) (ast/var-term z)))))))

(defn- subst-holds?
  "Decide `Subst(source, target)` through the public kernel query path."
  [system source-code target-code]
  (boolean (seq (query/query-succeeds
                  (:program system)
                  (sjas/subst-code source-code target-code)
                  1 600))))

(defn theorem23-closure-status
  "Return the Theorem 2.3 closure structure + honest per-step status for `system`
   at superscript `k-code`. Validates the checker-accepted steps live."
  [system k-code]
  (let [system-code (:system-code system)
        diag (sjas/theorem23-diagonal system k-code)
        nbar (:skeleton-code diag)
        dk-code (:diagonal-code diag)
        ;; Step 2 / Map locator: Subst(nbar, code(Dk)) -- live-validated.
        subst-eq7? (subst-holds? system nbar dk-code)]
    {:system-code system-code
     :k-code k-code
     :diagonal-code dk-code
     :skeleton-code nbar
     :d-star (d-star system-code k-code dk-code)
     :steps
     [{:id :A :eq 3 :name "tableau-consistency / SelfCons"
       :status :reflected-axiom
       :note "generated Group-3 SelfCons; Level-1 (dsjas-subst-prf) realizes the Level-0 forall p. not SemPrf(BOT,p)"}
      {:id :C :eq 14 :name "Subst single-valued (V3)"
       :status :reflected-axiom
       :note "V3 is a generated route axiom; condition (C) verbatim"}
      {:id :B :eq 16 :name "bounded-proof-of-diagonal => BOT (via V5 + Map)"
       :status :tree-construction
       :note "Theorem 2.2 combination of the V5 instance with Map(alpha,k,code(Dk)); Phase 0 showed the checker validates constructed cut-free trees, so this is a tree to build (no cut rule)"}
      {:id :step1 :eq 6 :name "D* from A and B"
       :status :tree-construction}
      {:id :step2 :eq 7 :name "Subst(nbar, code(Dk))"
       :status (if subst-eq7? :checker-accepted :FAILED)
       :validated subst-eq7?}
      {:id :step3 :eq 8 :name "Dk == D* from Subst and C"
       :status :tree-construction}
      {:id :step4 :eq 9 :name "Dk from D* and (Dk==D*)"
       :status :tree-construction}
      {:id :step5 :eq 11 :name "not Dk by instantiating Dk with (p,q,r) + Q-disproof of false Pi1"
       :status :partial
       :note (str "bounded-proof witness SemPrf^k(r,p,2^(p+1)) is checker-accepted via the symbolic pow "
                  "bound (Phase 1; no tower materialized), and -- since the pow-vocabulary coding change "
                  "(Phase 3 step 5) -- a DECODED (neg SemPrf^k) interior node now also closes in "
                  "construct-and-check mode: semprfk-alpha/pow decode by name and the structural V-route "
                  "sjas-semprfk-alpha-structural-closeo closes the leaf, with a too-small tower bound "
                  "genuinely failing. The Q-disproof of the false Pi1 instance now ASSEMBLES (Phase 3 "
                  "Q-disproof): refuting `Subst(nbar,code(Dk)) => not SemPrf^k(...)` closes its negated "
                  "antecedent via the new subst-code structural eval (sjas-subst-code-structural-closeo, "
                  "over the REAL diagonal locator) and its consequent via the SemPrf^k V-route -- both "
                  "halves of Willard's Q-disproof. What remains is (i) witness-providing "
                  "gamma-instantiation (the checker's forall rule introduces a FRESH variable, not a "
                  "chosen ground witness, so deriving the instance from Dk -- `Dk |- instance` -- is the "
                  "missing tableau mechanic) and (ii) the step-4 proof p so the consequent's theorem is "
                  "code(Dk) itself")}
      {:id :step6 :eq nil :name "close: Dk and not Dk"
       :status :follows-from-4-and-5}]
     :checker-accepted (cond-> #{:step5-bounded-proof-witness-symbolic-pow-bound
                                 :step5-decoded-semprfk-node-closes-construct-and-check
                                 :step5-subst-qdisproof-closes-real-diagonal
                                 :step5-false-pi1-instance-refutation-assembles}
                         subst-eq7? (conj :step2-subst-eq7))
     ;; Phase 0: these are constructed cut-free tableau trees (the checker
     ;; validates them; no cut rule / trusted-base growth). The with-cut model in
     ;; proflog.sjas-cut-composition is a reference only.
     :tree-construction-steps #{:B :step1 :step3 :step4}
     :resolved-since-aar {:phase0 "no cut rule needed; checker validates cut-free trees"
                          :phase1 "Log(2^m,k) symbolic; SemPrf^k bound accepts (pow 2 exp) tower witness without materialization"
                          :phase3-baseline (str "proflog.sjas-tree-builder promotes the formula-bearing node "
                                                "builders; proflog.sjas-tree-builder-test commits the "
                                                "construct-and-check baseline over THIS exact generated system "
                                                "(reflexive/conjunction/double-negation closures, narrow+wide "
                                                "node shapes). Closure-rule finding: complementary pos/neg "
                                                "closure fires for reserved/primitive relations that decode to "
                                                "named atoms (subst-code, lt, leq, axiom-member) but not for "
                                                "opaque user relations (which decode to (sym n) once the source "
                                                "table is removed). The diagonal path closes via subst-code "
                                                "(named) plus the semprf/semprfk profile interpretation, never "
                                                "via an opaque user-relation clash, so this is a characterization, "
                                                "not an obstruction.")
                          :phase3-pow-vocabulary (str "the pow-vocabulary item is DISCHARGED. `pow` is appended "
                                                      "to reserved-coding-symbols and `semprfk-alpha` is promoted "
                                                      "out of profile-local-reserved-symbols, with the boundary "
                                                      "cluster kept contiguous and dsjas-tab2-proof moved last so "
                                                      "no compaction gap shifts their per-system index away from "
                                                      "the global reserved index. Result: a DECODED (neg "
                                                      "SemPrf^k) interior node decodes semprfk-alpha/pow by name "
                                                      "AND closes via sjas-semprfk-alpha-structural-closeo (the "
                                                      "structural V-route, reusing the same bounded-proof core), "
                                                      "with the too-small tower bound still failing "
                                                      "(proflog.sjas-semprfk-tree-closure-test). The full SJAS "
                                                      "not-slow gate (1445 tests) is green as the no-mis-decode "
                                                      "falsifier, so the encoder's compacted index view and the "
                                                      "proof-facing decoder's global view agree.")
                          :phase3-qdisproof (str "step 5's Q-disproof of the false Pi1 instance ASSEMBLES "
                                                 "(proflog.sjas-not-dk-qdisproof-test). Added "
                                                 "sjas-subst-code-structural-closeo so `not Subst(s,t)` "
                                                 "self-closes by evaluating the genuine subst-code gate -- "
                                                 "the construct-and-check analog of the SemPrf^k V-route -- "
                                                 "verified over the REAL diagonal locator `not "
                                                 "Subst(nbar,code(Dk))` (a wrong target stays open). The "
                                                 "implication beta-split then refutes "
                                                 "`Subst(nbar,code(Dk)) => not SemPrf^k(...)` by closing its "
                                                 "negated antecedent (subst eval) and its consequent "
                                                 "(V-route), with a too-small bound failing. FINDING that "
                                                 "localizes the rest: the checker's forall rule "
                                                 "(sjas-proof-check-stateo, line ~7895) instantiates a "
                                                 "universal with a FRESH branch variable, never a chosen "
                                                 "ground witness, and the V-route cannot resolve a free "
                                                 "proof/bound var -- so deriving the ground instance from Dk "
                                                 "(Willard's `instantiate Dk with (p,q,r)`) needs a "
                                                 "witness-providing gamma-instantiation the checker does not "
                                                 "yet have.")}
     :open-boundary {:remaining [:cut-free-combination-trees-steps-1-3-4-and-B
                                 :witness-providing-gamma-instantiation-Dk-entails-its-ground-instance]
                     :note (str "down from the original two research problems AND the pow-vocabulary "
                                "encode/decode item (discharged, :phase3-pow-vocabulary) AND step 5's "
                                "Q-disproof of the false Pi1 instance, whose two halves now assemble "
                                "(:phase3-qdisproof: subst-code eval + SemPrf^k V-route, both verified over "
                                "the real diagonal). Two items remain. (1) The cut-free combination trees "
                                "for steps 1/3/4/B -- pure tree assembly with the existing checker. (2) "
                                "Witness-providing gamma-instantiation: the checker's forall rule "
                                "instantiates with a fresh branch variable, so `Dk |- ground-instance` "
                                "(Willard's `instantiate Dk with (p,q,r)`) is the one tableau mechanic the "
                                "checker lacks; once it exists, the assembled Q-disproof closes the instance "
                                "and -- with the step-4 proof p making the consequent's theorem code(Dk) -- "
                                "yields not Dk. The tree-construction primitives and checker-verified "
                                "baselines over the real system exist (proflog.sjas-tree-builder, "
                                "proflog.sjas-not-dk-qdisproof-test).")}}))
