(ns proflog.answer-overlay
  "Extracted answer-oriented overlay above the greenfield proof kernel.

   This namespace preserves the answer-mode execution path that used to live in
   `proflog.kernel`: exported answer vars, existential-as-variable behavior,
   residual deferred calls, and recursive answer-call budgeting. ADR-0015 moves
   that flow out of the ordinary proof kernel so the pure proof surface remains
   directly accessible.

   For a reader of Fitting's Proflog paper, this file should be read as:

   - the same tableau engine as `proflog.kernel`,
   - but reparameterized for open-query execution,
   - with explicit output of learned bindings for selected answer variables,
   - and with a relational notion of \"stop descending here and leave the rest
     as a symbolic obligation\".

   The key idea is that answer search is not a different logic. It is the same
   branch-closing machinery, plus extra exported state describing what the
   branch learned before it chose to stop unfolding recursive calls."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== appendo conde fail fresh lcons membero run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.kernel-support :as support]
            [proflog.program :as program]
            [proflog.subst :as subst]))

;; Reading guide
;; -------------
;;
;; This namespace deliberately mirrors the kernel's shape so that the semantic
;; differences stay visible:
;;
;; - `sigma` / `neqs` mean the same thing as in the ordinary kernel,
;; - `residuals` is new and records deferred procedure-call obligations,
;; - `call-depth` is a bounded unfolding budget for recursive descendants below
;;   the query boundary,
;; - `existentials-as-vars?` switches the delta rule from rigid parameters to
;;   exportable object-language variables, which is what makes partial and
;;   reverse-mode answers possible.
;;
;; In effect, the ordinary kernel asks only:
;;
;;   "Can this branch be closed?"
;;
;; while the answer overlay asks:
;;
;;   "How far can this branch be closed, what bindings were learned for the
;;    designated answer variables, and which obligations remain if we stop
;;    recursive descent at the current answer budget?"

(declare prove-stateo close-agendao saved-call-closeso)

(defn saved-call-closeso
  "Succeed when one saved atom becomes callable under the current equality
   substitution and its subsidiary tableau closes.

   This makes procedure-call completeness depend on the branch state, not on
   whether the enabling equality literal happened to be expanded before or
   after the atom was saved.

   In answer mode there is one extra choice beyond the ordinary kernel:

   - if `call-depth` still permits recursive descent, actually run the call;
   - otherwise, when symbolic existential export is enabled, keep the walked
     atom as a residual obligation instead of losing it."
  [lits proof-vars sigma sigma-out neqs neqs-out residuals residuals-out prog fuel call-depth existentials-as-vars? proof]
  (let [can-descend? (or (nil? call-depth) (pos? call-depth))
        next-call-depth (support/next-call-depth call-depth)
        ;; Deferral is only meaningful in answer mode with symbolic existential
        ;; export and an actual program to call. In pure theorem-proving mode
        ;; there is nothing to export as a residual frontier.
        defer-calls? (and existentials-as-vars? prog)]
    (conde
      ;; Saved positive call: equality has now walked the atom into a callable
      ;; L-ground shape, so consume one unit of recursive descendant budget and
      ;; open the subsidiary tableau.
      [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
         (membero (list 'pos atom) lits)
         (equality/walk-atomo atom sigma walked-atom)
         (== (lcons 'app (lcons relation args)) walked-atom)
         (support/l-ground-term*o args)
         (program/call-clauseo prog walked-atom call-env body negated-body)
         (== (list 'eq-triggered-call subproof) proof)
         (== residuals residuals-out)
         (if can-descend?
           (support/step-fuelo fuel next-fuel)
           fail)
         (prove-stateo body
                       '()
                       '()
                       call-env
                       proof-vars
                       sigma
                       sigma-out
                       neqs
                       neqs-out
                       residuals
                       residuals-out
                       prog
                       next-fuel
                       next-call-depth
                       existentials-as-vars?
                       subproof))]
      ;; If we are in symbolic answer mode but have chosen not to descend, the
      ;; positive saved atom itself becomes part of the exported answer
      ;; frontier.
      [(if defer-calls?
         (fresh [atom]
           (membero (list 'pos atom) lits)
           (== sigma sigma-out)
           (== neqs neqs-out)
           (== (lcons (list 'pos atom) residuals) residuals-out)
           (== '(eq-triggered-residual-call) proof))
         fail)]
      ;; Saved negative atom can likewise be exported as a deferred obligation.
      [(if defer-calls?
         (fresh [atom]
           (membero (list 'neg atom) lits)
           (== sigma sigma-out)
           (== neqs neqs-out)
           (== (lcons (list 'neg atom) residuals) residuals-out)
           (== '(eq-triggered-residual-neg-call) proof))
         fail)]
      ;; Saved negative call: run the subsidiary tableau for the NNF negation of
      ;; the clause body if recursive budget still permits descent.
      [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
         (membero (list 'neg atom) lits)
         (equality/walk-atomo atom sigma walked-atom)
         (== (lcons 'app (lcons relation args)) walked-atom)
         (support/l-ground-term*o args)
         (program/call-clauseo prog walked-atom call-env body negated-body)
         (== (list 'eq-triggered-neg-call subproof) proof)
         (== residuals residuals-out)
         (if can-descend?
           (support/step-fuelo fuel next-fuel)
           fail)
         (prove-stateo negated-body
                       '()
                       '()
                       call-env
                       proof-vars
                       sigma
                       sigma-out
                       neqs
                       neqs-out
                       residuals
                       residuals-out
                       prog
                       next-fuel
                       next-call-depth
                       existentials-as-vars?
                       subproof))])))

(defn close-agendao
  "Close one explicit pending-formula agenda under the answer-export state.

   This is the answer-layer analogue of `proflog.kernel/close-agendao`: the
   branch work is explicit as an agenda, and `support/selecto` exposes the next
   pending obligation as a relational search choice rather than fixing a
   leftmost expansion order."
  [agenda lits env proof-vars sigma sigma-out neqs neqs-out residuals residuals-out prog fuel call-depth existentials-as-vars? proof]
  (fresh [fml unexpanded]
    (support/selecto fml agenda unexpanded)
    (let [can-descend? (or (nil? call-depth) (pos? call-depth))
          next-call-depth (support/next-call-depth call-depth)
          ;; Only open-query / answer-mode execution wants residual deferred
          ;; calls. Ordinary proof search should either descend or fail.
          defer-calls? (and existentials-as-vars? prog)]
      (conde
      ;; α-rule: both conjuncts must close on the same branch, so the sibling
      ;; conjunct is pushed onto the branch work stack. Equality-triggered saved
      ;; literal closure handles the important order-insensitive case where a
      ;; later equality unlocks an earlier saved atom.
      [(fresh [left right next-fuel prf]
         (== (list 'and left right) fml)
         (== (list 'conj prf) proof)
         (support/step-fuelo fuel next-fuel)
         (prove-stateo left
                       (lcons right unexpanded)
                       lits
                       env
                       proof-vars
                       sigma
                       sigma-out
                       neqs
                       neqs-out
                       residuals
                       residuals-out
                       prog
                       next-fuel
                       call-depth
                       existentials-as-vars?
                       prf))]

    ;; β-rule: both branches must close under one compatible proof state. The
    ;; resulting substitution threads from the first sibling into the second.
    [(fresh [left right next-fuel sigma-mid neqs-mid residuals-mid left-proof right-proof]
            (== (list 'or left right) fml)
            (== (list 'split left-proof right-proof) proof)
            (support/step-fuelo fuel next-fuel)
            (prove-stateo left
                          unexpanded
                          lits
                          env
                          proof-vars
                          sigma
                          sigma-mid
                          neqs
                          neqs-mid
                          residuals
                          residuals-mid
                          prog
                          next-fuel
                          call-depth
                          existentials-as-vars?
                          left-proof)
            (prove-stateo right
                          unexpanded
                          lits
                          env
                          proof-vars
                          sigma-mid
                          sigma-out
                          neqs-mid
                          neqs-out
                          residuals-mid
                          residuals-out
                          prog
                          next-fuel
                          call-depth
                          existentials-as-vars?
                          right-proof))]

    ;; γ-rule: instantiate a universal with an explicit free variable term and
    ;; re-enqueue the original universal so later instantiations remain
    ;; available on the branch. As in the ordinary kernel, we keep an optimized
    ;; empty-work-stack case and the general re-enqueueing case.
    [(nominal/fresh [binding-nom]
                    (nominal/fresh [free-var-nom]
                                   (fresh [body body-subst narrowed-env next-fuel prf]
                                          (== (list 'forall (nominal/tie binding-nom body)) fml)
                                          (== '() unexpanded)
                                          (== (list 'univ prf) proof)
                                          (subst/remove-bindo binding-nom env narrowed-env)
                                          (subst/subst-formulao body narrowed-env body-subst)
                                          (support/step-fuelo fuel next-fuel)
                                          (prove-stateo body-subst
                                                        '()
                                                        lits
                                                        (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                                        (lcons free-var-nom proof-vars)
                                                        sigma
                                                        sigma-out
                                                        neqs
                                                        neqs-out
                                                        residuals
                                                        residuals-out
                                                        prog
                                                        next-fuel
                                                        call-depth
                                                        existentials-as-vars?
                                                        prf))))]
    ;; General gamma case with explicit re-enqueueing of the universal.
    [(nominal/fresh [binding-nom]
                    (nominal/fresh [free-var-nom]
                                   (fresh [body body-subst narrowed-env pending next-fuel prf]
                                          (== (list 'forall (nominal/tie binding-nom body)) fml)
                                          (== (list 'univ prf) proof)
                                          (appendo unexpanded (list fml) pending)
                                          (subst/remove-bindo binding-nom env narrowed-env)
                                          (subst/subst-formulao body narrowed-env body-subst)
                                          (support/step-fuelo fuel next-fuel)
                                          (prove-stateo body-subst
                                                        pending
                                                        lits
                                                        (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                                        (lcons free-var-nom proof-vars)
                                                        sigma
                                                        sigma-out
                                                        neqs
                                                        neqs-out
                                                        residuals
                                                        residuals-out
                                                        prog
                                                        next-fuel
                                                        call-depth
                                                        existentials-as-vars?
                                                        prf))))]

    ;; Single-use universal: instantiate once on the current branch without
    ;; re-enqueueing. This is the NNF operational form produced by negating an
    ;; existential clause body for procedure-call execution.
    [(nominal/fresh [binding-nom]
                    (nominal/fresh [free-var-nom]
                                   (fresh [body body-subst narrowed-env next-fuel prf]
                                          (== (list 'once-forall (nominal/tie binding-nom body)) fml)
                                          (== (list 'once-univ prf) proof)
                                          (subst/remove-bindo binding-nom env narrowed-env)
                                          (subst/subst-formulao body narrowed-env body-subst)
                                          (support/step-fuelo fuel next-fuel)
                                          (prove-stateo body-subst
                                                        unexpanded
                                                        lits
                                                        (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                                        (lcons free-var-nom proof-vars)
                                                        sigma
                                                        sigma-out
                                                        neqs
                                                        neqs-out
                                                        residuals
                                                        residuals-out
                                                        prog
                                                        next-fuel
                                                        call-depth
                                                        existentials-as-vars?
                                                        prf))))]

    ;; δ-rule: instantiate an existential exactly once with a rigid internal
    ;; parameter in ordinary proof search. Answer export instead introduces a
    ;; fresh object-language variable so existential structure can remain
    ;; symbolic and continue constraining open queries relationally.
    ;;
    ;; This one switch is the main reason the answer overlay cannot be reduced
    ;; to "just call the ordinary proof wrapper backwards". Open-query answer
    ;; search needs existential witnesses that remain visible as symbolic output
    ;; variables, not rigid internal parameters.
    [(if existentials-as-vars?
       (nominal/fresh [binding-nom]
                      (nominal/fresh [free-var-nom]
                                     (fresh [body body-subst narrowed-env next-fuel prf]
                                            (== (list 'exists (nominal/tie binding-nom body)) fml)
                                            (== (list 'witness prf) proof)
                                            (subst/remove-bindo binding-nom env narrowed-env)
                                            (subst/subst-formulao body narrowed-env body-subst)
                                            (support/step-fuelo fuel next-fuel)
                                            (prove-stateo body-subst
                                                          unexpanded
                                                          lits
                                                          (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                                          (lcons free-var-nom proof-vars)
                                                          sigma
                                                          sigma-out
                                                          neqs
                                                          neqs-out
                                                          residuals
                                                          residuals-out
                                                          prog
                                                          next-fuel
                                                          call-depth
                                                          existentials-as-vars?
                                                          prf))))
       (nominal/fresh [binding-nom]
                      (nominal/fresh [parameter-nom]
                                     (fresh [body body-subst narrowed-env next-fuel prf]
                                            (== (list 'exists (nominal/tie binding-nom body)) fml)
                                            (== (list 'witness prf) proof)
                                            (subst/remove-bindo binding-nom env narrowed-env)
                                            (subst/subst-formulao body narrowed-env body-subst)
                                            (support/step-fuelo fuel next-fuel)
                                            (prove-stateo body-subst
                                                          unexpanded
                                                          lits
                                                          (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                                          proof-vars
                                                          sigma
                                                          sigma-out
                                                          neqs
                                                          neqs-out
                                                          residuals
                                                          residuals-out
                                                          prog
                                                          next-fuel
                                                          call-depth
                                                          existentials-as-vars?
                                                          prf)))))]

    ;; Positive equality closes immediately when the two terms cannot denote
    ;; the same free-constructor object.
    [(fresh [lit left right contradiction-proof]
            (subst/subst-formulao fml env lit)
            (== (list 'eq left right) lit)
            (equality/eq-contradictiono left right sigma contradiction-proof)
            (== sigma sigma-out)
            (== neqs neqs-out)
            (== residuals residuals-out)
            (== contradiction-proof proof))]

    ;; Otherwise positive equality extends the branch substitution. That new
    ;; information can close the branch either by violating a saved disequality
    ;; or by making two saved complementary atoms unify.
    [(fresh [lit left right sigma-mid step-proof branch-proof]
            (subst/subst-formulao fml env lit)
            (== (list 'eq left right) lit)
            (equality/unify-termo left right sigma sigma-mid step-proof)
            (equality/neq-violatedo neqs sigma-mid branch-proof)
            (== sigma-mid sigma-out)
            (support/prune-contradictory-neqso neqs sigma-mid neqs-out)
            (== residuals residuals-out)
            (== (list 'eq-step step-proof branch-proof) proof))]
    ;; Equality may also wake a saved procedure call, which is particularly
    ;; important in open queries: a previously symbolic atom may become
    ;; callable only after enough branch equalities have accumulated.
    [(fresh [lit left right sigma-mid step-proof branch-proof]
            (subst/subst-formulao fml env lit)
            (== (list 'eq left right) lit)
            (equality/unify-termo left right sigma sigma-mid step-proof)
            (equality/contradictory-atomso lits sigma-mid sigma-out branch-proof)
            (support/prune-contradictory-neqso neqs sigma-out neqs-out)
            (== residuals residuals-out)
            (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-mid step-proof branch-proof]
            (subst/subst-formulao fml env lit)
            (== (list 'eq left right) lit)
            (equality/unify-termo left right sigma sigma-mid step-proof)
            (saved-call-closeso lits proof-vars sigma-mid sigma-out neqs neqs-out residuals residuals-out prog fuel call-depth existentials-as-vars? branch-proof)
            (== (list 'eq-step step-proof branch-proof) proof))]
    [(fresh [lit left right sigma-mid step-proof next rest next-fuel prf]
            (subst/subst-formulao fml env lit)
            (== (list 'eq left right) lit)
            (equality/unify-termo left right sigma sigma-mid step-proof)
            (== (lcons next rest) unexpanded)
            (== (list 'eq-step step-proof prf) proof)
            (support/stable-neqso neqs sigma-mid)
            (support/step-fuelo fuel next-fuel)
            (prove-stateo next
                          rest
                          lits
                          env
                          proof-vars
                          sigma-mid
                          sigma-out
                          neqs
                          neqs-out
                          residuals
                          residuals-out
                          prog
                          next-fuel
                          call-depth
                          existentials-as-vars?
                          prf))]

    ;; Negative equality closes only once its two walked sides are forced equal.
    ;; Otherwise it is stored symbolically and rechecked after later bindings.
    [(fresh [lit left right]
            (subst/subst-formulao fml env lit)
            (== (list 'neq left right) lit)
            (equality/same-termo left right sigma)
            (== sigma sigma-out)
            (== neqs neqs-out)
            (== residuals residuals-out)
            (== '(refl-close) proof))]
    [(fresh [lit left right sigma-mid new-bindings binding rest step-proof]
            (subst/subst-formulao fml env lit)
            (== (list 'neq left right) lit)
            (equality/unify-termo left right sigma sigma-mid step-proof)
            (appendo new-bindings sigma sigma-mid)
            ;; A disequality closes when equality can force its two sides
            ;; equal by instantiating one or more branch-local proof variables.
            ;; Recursive constructor shapes such as pair/list disequalities may
            ;; require multiple such bindings on the same step.
            (== (lcons binding rest) new-bindings)
            (support/proof-bindingso new-bindings proof-vars)
            (== sigma-mid sigma-out)
            (support/prune-contradictory-neqso neqs sigma-mid neqs-out)
            (== residuals residuals-out)
            (== (list 'neq-close step-proof) proof))]
    ;; If the disequality remains open, we preserve it as part of the symbolic
    ;; answer state rather than discarding it. Later exported answers will turn
    ;; this store into explicit residual disequality formulas.
    [(fresh [lit left right next rest next-fuel prf]
            (subst/subst-formulao fml env lit)
            (== (list 'neq left right) lit)
            (== (lcons next rest) unexpanded)
            (== (list 'neq-store prf) proof)
            (support/step-fuelo fuel next-fuel)
            (prove-stateo next
                          rest
                          lits
                          env
                          proof-vars
                          sigma
                          sigma-out
                          (lcons [left right] neqs)
                          neqs-out
                          residuals
                          residuals-out
                          prog
                          next-fuel
                          call-depth
                          existentials-as-vars?
                          prf))]

    ;; Positive and negative atoms close against a saved complementary atom if
    ;; their walked arguments can be unified. If no direct complement exists,
    ;; the Procedure Call Rule may close the branch through a fresh subsidiary
    ;; tableau over the compiled clause body.
    [(fresh [lit atom]
            (subst/subst-formulao fml env lit)
            (== (list 'pos atom) lit)
            (support/complementary-lito lit lits sigma sigma-out proof)
            (support/prune-contradictory-neqso neqs sigma-out neqs-out)
            (== residuals residuals-out))]
    ;; In answer mode, prefer consuming remaining call-depth budget before
    ;; materializing a residual call frontier. The defer branches stay available
    ;; and still win once `call-depth` reaches zero.
    ;;
    ;; This realizes a bounded approximation of recursive answer search:
    ;;
    ;; - if descent is still allowed, keep proving the subsidiary tableau;
    ;; - if descent is no longer allowed, keep the atom itself as a symbolic
    ;;   residual obligation for the caller.
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
            (subst/subst-formulao fml env lit)
            (== (list 'pos atom) lit)
            (equality/walk-atomo atom sigma walked-atom)
            (== (lcons 'app (lcons relation args)) walked-atom)
            (support/l-ground-term*o args)
            (program/call-clauseo prog walked-atom call-env body negated-body)
            (== (list 'pos-call subproof) proof)
            (== residuals residuals-out)
            (if can-descend?
              (support/step-fuelo fuel next-fuel)
              fail)
            (prove-stateo body
                          '()
                          '()
                          call-env
                          proof-vars
                          sigma
                          sigma-out
                          neqs
                          neqs-out
                          residuals
                          residuals-out
                          prog
                          next-fuel
                          next-call-depth
                          existentials-as-vars?
                          subproof))]
    ;; Deferral with more branch work still pending: save the current atom into
    ;; the residual frontier and continue with the rest of the current branch.
    [(if defer-calls?
       (fresh [lit atom next rest next-fuel prf]
              (subst/subst-formulao fml env lit)
              (== (list 'pos atom) lit)
              (== (lcons next rest) unexpanded)
              (== (list 'defer-call prf) proof)
              (support/step-fuelo fuel next-fuel)
              (prove-stateo next
                            rest
                            lits
                            env
                            proof-vars
                            sigma
                            sigma-out
                            neqs
                            neqs-out
                            (lcons lit residuals)
                            residuals-out
                            prog
                            next-fuel
                            call-depth
                            existentials-as-vars?
                            prf))
       fail)]
    ;; Deferral when this atom is the last remaining branch task: export it
    ;; directly as the whole residual frontier.
    [(if defer-calls?
       (fresh [lit atom]
              (subst/subst-formulao fml env lit)
              (== (list 'pos atom) lit)
              (== sigma sigma-out)
              (== neqs neqs-out)
              (== (lcons lit residuals) residuals-out)
              (== '(defer-call) proof))
       fail)]
    [(fresh [lit atom]
            (subst/subst-formulao fml env lit)
            (== (list 'neg atom) lit)
            (support/complementary-lito lit lits sigma sigma-out proof)
            (support/prune-contradictory-neqso neqs sigma-out neqs-out)
            (== residuals residuals-out))]
    ;; Negative-call version of the same bounded descent / symbolic deferral
    ;; choice.
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
            (subst/subst-formulao fml env lit)
            (== (list 'neg atom) lit)
            (equality/walk-atomo atom sigma walked-atom)
            (== (lcons 'app (lcons relation args)) walked-atom)
            (support/l-ground-term*o args)
            (program/call-clauseo prog walked-atom call-env body negated-body)
            (== (list 'neg-call subproof) proof)
            (== residuals residuals-out)
            (if can-descend?
              (support/step-fuelo fuel next-fuel)
              fail)
            (prove-stateo negated-body
                          '()
                          '()
                          call-env
                          proof-vars
                          sigma
                          sigma-out
                          neqs
                          neqs-out
                          residuals
                          residuals-out
                          prog
                          next-fuel
                          next-call-depth
                          existentials-as-vars?
                          subproof))]
    ;; Defer the negative call but keep working through other pending formulas.
    [(if defer-calls?
       (fresh [lit atom next rest next-fuel prf]
              (subst/subst-formulao fml env lit)
              (== (list 'neg atom) lit)
              (== (lcons next rest) unexpanded)
              (== (list 'defer-call prf) proof)
              (support/step-fuelo fuel next-fuel)
              (prove-stateo next
                            rest
                            lits
                            env
                            proof-vars
                            sigma
                            sigma-out
                            neqs
                            neqs-out
                            (lcons lit residuals)
                            residuals-out
                            prog
                            next-fuel
                            call-depth
                            existentials-as-vars?
                            prf))
       fail)]
    ;; Defer the negative call as the final residual frontier.
    [(if defer-calls?
       (fresh [lit atom]
              (subst/subst-formulao fml env lit)
              (== (list 'neg atom) lit)
              (== sigma sigma-out)
              (== neqs neqs-out)
              (== (lcons lit residuals) residuals-out)
              (== '(defer-call) proof))
       fail)]
    ;; If no immediate closure or call step applies, the atom is saved on the
    ;; branch exactly as in the ordinary kernel so that later equality can
    ;; reopen it.
    [(fresh [lit atom next rest next-fuel prf]
            (subst/subst-formulao fml env lit)
            (== (list 'pos atom) lit)
            (== (lcons next rest) unexpanded)
            (== (list 'savefml prf) proof)
            (support/step-fuelo fuel next-fuel)
            (prove-stateo next
                          rest
                          (lcons lit lits)
                          env
                          proof-vars
                          sigma
                          sigma-out
                          neqs
                          neqs-out
                          residuals
                          residuals-out
                          prog
                          next-fuel
                          call-depth
                          existentials-as-vars?
                          prf))]
    ;; Negative saved-literal case.
    [(fresh [lit atom next rest next-fuel prf]
            (subst/subst-formulao fml env lit)
            (== (list 'neg atom) lit)
            (== (lcons next rest) unexpanded)
            (== (list 'savefml prf) proof)
            (support/step-fuelo fuel next-fuel)
            (prove-stateo next
                          rest
                          (lcons lit lits)
                          env
                          proof-vars
                          sigma
                          sigma-out
                          neqs
                          neqs-out
                          residuals
                          residuals-out
                          prog
                          next-fuel
                          call-depth
                          existentials-as-vars?
                          prf))]))))

(defn prove-stateo
  "Backward-compatible current-formula wrapper over the fair answer agenda.

   Existing callers still pass one focused formula plus the remaining pending
   branch work, but internally the answer layer now treats them as one agenda
   and schedules the next obligation relationally."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out residuals residuals-out prog fuel call-depth existentials-as-vars? proof]
  (close-agendao
    (lcons fml unexpanded)
    lits
    env
    proof-vars
    sigma
    sigma-out
    neqs
    neqs-out
    residuals
    residuals-out
    prog
    fuel
    call-depth
    existentials-as-vars?
    proof))

(defn proveo
  "Public five-argument kernel relation.

   Existing callers see the same surface signature, but each branch now starts
   with an empty equality substitution and empty disequality store.

   Unlike the answer-exporting entry points below, this wrapper does not
   designate answer variables and therefore behaves like ordinary proof search
   even though it threads residual state internally."
  ([fml unexpanded lits env proof]
   (fresh [sigma-out neqs-out residuals-out]
          (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out '() residuals-out nil nil nil false proof)))
  ([fml unexpanded lits env fuel proof]
   (fresh [sigma-out neqs-out residuals-out]
          (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out '() residuals-out nil fuel nil false proof))))

(defn prove-answero
  "Kernel relation with explicit exported answer variables.

   `answer-vars` are top-level free noms whose bindings may be learned during
   proof search and returned through `sigma-out`. Residual disequalities and
   deferred call obligations are returned through `neqs-out` and
   `residuals-out`.

   This is the answer-overlay analogue of asking the ordinary kernel for a
   proof witness, except that now we also keep the symbolic frontier visible."
  ([fml unexpanded lits env answer-vars sigma-out neqs-out residuals-out proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out nil nil 1 true proof))
  ([fml unexpanded lits env answer-vars sigma-out neqs-out residuals-out fuel proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out nil fuel 1 true proof))
  ([fml unexpanded lits env answer-vars sigma-out neqs-out residuals-out fuel call-depth proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out nil fuel call-depth true proof)))

(defn prove-programo
  "Kernel relation with an explicit compiled program for procedure calls.

   This mirrors `proflog.kernel/prove-programo` but preserves the answer-layer
   plumbing so that the same internal machine can also serve the exported
   answer-entry surfaces below."
  ([fml unexpanded lits env prog proof]
   (fresh [sigma-out neqs-out residuals-out]
          (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out '() residuals-out prog nil nil false proof)))
  ([fml unexpanded lits env prog fuel proof]
   (fresh [sigma-out neqs-out residuals-out]
          (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out '() residuals-out prog fuel nil false proof))))

(defn prove-program-answero
  "Kernel relation for query-answer export with explicit answer variables.

   This is the most direct answer-search analogue of `prove-programo`: keep the
   compiled program explicit, keep answer vars explicit, and expose the learned
   substitution plus residual frontier."
  ([fml unexpanded lits env answer-vars prog sigma-out neqs-out residuals-out proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out prog nil 1 true proof))
  ([fml unexpanded lits env answer-vars prog sigma-out neqs-out residuals-out fuel proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out prog fuel 1 true proof))
  ([fml unexpanded lits env answer-vars prog sigma-out neqs-out residuals-out fuel call-depth proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out prog fuel call-depth true proof)))

(defn prove-program-query-entryo
  "Kernel relation for top-level literal query-answer export relative to `prog`.

   The entry procedure call itself does not consume `call-depth`; that staged
   budget is reserved for recursive descendants below the query boundary.

   This is the operational bridge from the user-facing query API to the
   internal tableau engine. The top-level query atom is treated specially:

   - validate it as an immediate program call,
   - open the matching clause body or its NNF negation,
   - and start the subsidiary tableau with answer export enabled from the
     outset.

   That is why `query-answers` can talk about recursive descendants below the
   query boundary rather than charging the root query atom itself against the
   `call-depth` budget."
  ([lit answer-vars prog sigma-out neqs-out residuals-out proof]
   (prove-program-query-entryo lit answer-vars prog sigma-out neqs-out residuals-out nil 0 proof))
  ([lit answer-vars prog sigma-out neqs-out residuals-out fuel proof]
   (prove-program-query-entryo lit answer-vars prog sigma-out neqs-out residuals-out fuel 0 proof))
  ([lit answer-vars prog sigma-out neqs-out residuals-out fuel call-depth proof]
   (conde
     ;; Positive top-level query atom: open the clause body directly. Because
     ;; this is the query boundary, the root call itself does not decrement the
     ;; recursive answer-call budget.
     [(fresh [atom relation args call-env body negated-body subproof]
        (== (list 'pos atom) lit)
        (== (lcons 'app (lcons relation args)) atom)
        (support/l-ground-term*o args)
        (program/call-clauseo prog atom call-env body negated-body)
        (== (list 'query-pos-call subproof) proof)
        (prove-stateo body
                      '()
                      '()
                      call-env
                      answer-vars
                      '()
                      sigma-out
                      '()
                      neqs-out
                      '()
                      residuals-out
                      prog
                      fuel
                      call-depth
                      true
                      subproof))]
     ;; Negative top-level query atom: open the precomputed NNF negation of the
     ;; clause body, matching Fitting's Part 2 call rule.
     [(fresh [atom relation args call-env body negated-body subproof]
        (== (list 'neg atom) lit)
        (== (lcons 'app (lcons relation args)) atom)
        (support/l-ground-term*o args)
        (program/call-clauseo prog atom call-env body negated-body)
        (== (list 'query-neg-call subproof) proof)
        (prove-stateo negated-body
                      '()
                      '()
                      call-env
                      answer-vars
                      '()
                      sigma-out
                      '()
                      neqs-out
                      '()
                      residuals-out
                      prog
                      fuel
                      call-depth
                      true
                      subproof))])))

(defn prove
  "Return up to `n` proof terms closing the given greenfield formula.

   This convenience wrapper is mainly useful when exploring the answer overlay
   as a proof engine in its own right. It still runs with answer-specific state
   present internally, but no answer vars are designated for export."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
        (proveo fml '() '() '() proof)))
  ([fml n fuel]
   (run n [proof]
        (proveo fml '() '() '() fuel proof))))

(defn prove-program
  "Return up to `n` proof terms closing `fml` relative to `prog`.

   This is the answer-overlay companion to the ordinary kernel's
   `prove-program`: program calls are available, but no answer bindings are
   explicitly exported unless one of the `*-answero` relations is used."
  ([prog fml n]
   (run n [proof]
        (prove-programo fml '() '() '() prog proof)))
  ([prog fml n fuel]
   (run n [proof]
        (prove-programo fml '() '() '() prog fuel proof))))
