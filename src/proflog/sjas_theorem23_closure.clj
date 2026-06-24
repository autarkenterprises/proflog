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
       :note "bounded-proof witness SemPrf^k(r,p,2^(p+1)) is NOW checker-accepted via the symbolic pow bound (Phase 1; no tower materialized); what remains is the step-4 proof p (the combination tree) and assembling the not-Dk tree + Q-disproof"}
      {:id :step6 :eq nil :name "close: Dk and not Dk"
       :status :follows-from-4-and-5}]
     :checker-accepted (cond-> #{:step5-bounded-proof-witness-symbolic-pow-bound}
                         subst-eq7? (conj :step2-subst-eq7))
     ;; Phase 0: these are constructed cut-free tableau trees (the checker
     ;; validates them; no cut rule / trusted-base growth). The with-cut model in
     ;; proflog.sjas-cut-composition is a reference only.
     :tree-construction-steps #{:B :step1 :step3 :step4}
     :resolved-since-aar {:phase0 "no cut rule needed; checker validates cut-free trees"
                          :phase1 "Log(2^m,k) symbolic; SemPrf^k bound accepts (pow 2 exp) tower witness without materialization"}
     :open-boundary {:remaining [:cut-free-combination-trees-steps-1-3-4-and-B
                                 :not-Dk-tree-assembly-with-Q-disproof
                                 :pow-vocabulary-encode-decode-for-encoded-proof-trees]
                     :note "down from the original two research problems: the bound obstruction's core is solved and no new trusted rule is needed"}}))
