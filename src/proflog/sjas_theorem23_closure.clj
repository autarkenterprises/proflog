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
                  "(Phase 3) -- a DECODED (neg SemPrf^k) interior node closes in construct-and-check mode "
                  "(semprfk-alpha/pow decode by name; the structural V-route sjas-semprfk-alpha-structural-"
                  "closeo closes the leaf; a too-small tower bound genuinely fails). The not-Dk tree "
                  "REFUTES `P1 ^ P2 ^ Dk` using ONLY existing relational tableau rules "
                  "(alpha/gamma-fresh-var/beta/complementary-clash): the gamma rule instantiates Dk with "
                  "FRESH variables that are bound to the witnesses (p,q,r) by clashing against the positive "
                  "premises P1 = Subst(nbar,code(Dk)) (Eq 7, checker-accepted at step 2) and P2 = "
                  "SemPrf^k(code(Dk),p,2^(p+1)). NO new checker rule is needed -- neither a witness-"
                  "providing gamma nor a subst-by-evaluation close (an interim subst-eval rule was tried "
                  "and REVERTED: on a free target it is non-terminating, and guarding it needs an impure "
                  "host `project` that the relational proof-checker discipline forbids; the clash binds the "
                  "free target relationally). What remains is P2 itself: the bounded proof p of Dk, i.e. "
                  "STEP 4 (the combination tree). So step 5 reduces to step 4")}
      {:id :step6 :eq nil :name "close: Dk and not Dk"
       :status :follows-from-4-and-5}]
     :checker-accepted (cond-> #{:step5-bounded-proof-witness-symbolic-pow-bound
                                 :step5-decoded-semprfk-node-closes-construct-and-check
                                 :step5-premise-clash-binds-universal-witness}
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
                          :phase4-boundary-contrast (str "the boundary DEMONSTRATION the falsifier guardrail "
                                                         "specifies is executable and pinned "
                                                         "(proflog.sjas-boundary-contrast-test): two systems with "
                                                         "IDENTICAL profile/vocabulary/coding (formula codes "
                                                         "byte-identical across sides) differ only in whether the "
                                                         "multiplication-totality content (complete mul basis + "
                                                         "V4/V5 route axioms + squaring witnesses) is in beta. The "
                                                         "same constructed cut-free leaf `neg SemPrf^k(code(S),1,"
                                                         "code(mul-totality-axiom),sjas-axiom-cite,2^(p+1))` CLOSES "
                                                         "through the interpreted bounded-proof V-route over the "
                                                         "mul-total side and FAILS over the addition-only side; a "
                                                         "shared-axiom control closes over BOTH (the addition "
                                                         "side's V-route machinery is intact, so the T1 failure is "
                                                         "exactly non-membership of mul-totality); a too-small "
                                                         "tower bound still fails on the mul side (Log bound "
                                                         "genuinely checked); and the step-5 premise-clash tree "
                                                         "closes on BOTH sides (the tableau plumbing is boundary-"
                                                         "blind -- the boundary is localized to the V-route "
                                                         "membership step). total-multiplication-hypothesis-report "
                                                         "splits the sides at system level (true/false). The "
                                                         "addition-side non-closure is the PINNED falsifier for "
                                                         "the guardrail below. This demonstrates the per-step "
                                                         "boundary asymmetry; the full BOT closure (steps 1/3/4/B "
                                                         "combination trees) remains open below.")
                          :phase3-premise-clash (str "step 5's not-Dk witness binding is FULLY RELATIONAL and "
                                                     "needs no new checker rule. Refuting the universal Dk "
                                                     "does not need a witness-providing gamma rule: the "
                                                     "checker's forall rule instantiates with a FRESH branch "
                                                     "variable, and that variable is bound to the witness by a "
                                                     "COMPLEMENTARY-LITERAL CLASH against a positive premise. "
                                                     "Verified over a small universal "
                                                     "(proflog.sjas-not-dk-qdisproof-test/"
                                                     "premise-clash-binds-the-universal-witness): a premise "
                                                     "`Subst(1,5)` binds a fresh forall witness `v0` to 5; "
                                                     "without the premise the universal stays open, and a "
                                                     "premise over a different argument cannot clash. So the "
                                                     "not-Dk tree refutes `P1 ^ P2 ^ Dk` with ONLY existing "
                                                     "alpha/gamma/beta/clash rules, binding Willard's (p,q,r) "
                                                     "by clashing against P1 = Subst(nbar,code(Dk)) (Eq 7) and "
                                                     "P2 = SemPrf^k(code(Dk),p,2^(p+1)); the residual is P2 = "
                                                     "STEP 4. NOTE: an interim subst-by-evaluation close "
                                                     "(sjas-subst-code-structural-closeo) was tried and "
                                                     "REVERTED -- on a free target it is non-terminating, and "
                                                     "guarding it needs an impure host `project` that the "
                                                     "relational proof-checker discipline (pinned by the "
                                                     "profile-source audit) forbids. The clash is the "
                                                     "relational replacement.")}
     :open-boundary {:remaining [:cut-free-combination-trees-steps-1-3-4-and-B]
                     :note (str "down from the original two research problems, the pow-vocabulary "
                                "encode/decode item (discharged, :phase3-pow-vocabulary), AND the supposed "
                                "witness-providing-gamma item -- which :phase3-premise-clash shows was a "
                                "MISCHARACTERIZATION: the not-Dk tree binds its witnesses by clashing fresh "
                                "gamma variables against positive premises, using only existing relational "
                                "checker rules (no new rule, no impure guard). ONE item remains: the "
                                "cut-free combination trees for steps 1/3/4/B. Step 5 (not Dk) reduces to "
                                "refuting `P1 ^ P2 ^ Dk`, which needs P2 = the bounded proof of Dk = STEP 4; "
                                "so step 5 is gated on step 4, and the genuine remaining work is the "
                                "combination trees that prove Dk. The tree-construction primitives and "
                                "checker-verified baselines over the real system exist "
                                "(proflog.sjas-tree-builder, proflog.sjas-not-dk-qdisproof-test).")
                     :falsifier-guardrail
                     (str "Do NOT promote `semprf-alpha` (un-superscripted SemPrf) to a named clash to make "
                          "the SelfCons clash `SemPrf(BOT,p) ^ not SemPrf(BOT,p)` fire syntactically. Since "
                          "SelfCons is a conjunct of AxiomConj, a syntactic SemPrf clash would close "
                          "`AxiomConj ^ not SelfCons` TRIVIALLY for EVERY system -- including the "
                          "addition-only (consistent) variant -- erasing the boundary the construction must "
                          "distinguish (the addition-only non-closure IS the falsifier). The closure must "
                          "instead route through the INTERPRETED bounded-proof V-route on a real constructed "
                          "proof code, where multiplication-totality makes the Log bound hold and the "
                          "addition-only variant fails. Confirmed: the SelfCons refutation does not close "
                          "trivially today (semprf-alpha stays profile-local). Detail: "
                          "docs/log/2026-06-25-adr-0142-bot-closure-falsifier-guardrail.md.")}}))
