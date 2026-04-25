(ns proflog.answer-overlay
  "Extracted answer-oriented overlay above the greenfield proof kernel.

   This namespace preserves the answer-mode execution path that used to live in
   `proflog.kernel`: exported answer vars, existential-as-variable behavior,
   residual deferred calls, and recursive answer-call budgeting. ADR-0015 moves
   that flow out of the ordinary proof kernel so the pure proof surface remains
   directly accessible."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== appendo conde fail fresh lcons membero run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.kernel-support :as support]
            [proflog.program :as program]
            [proflog.subst :as subst]))

(declare prove-stateo saved-call-closeso)

(defn saved-call-closeso
  "Succeed when one saved atom becomes callable under the current equality
   substitution and its subsidiary tableau closes.

   This makes procedure-call completeness depend on the branch state, not on
   whether the enabling equality literal happened to be expanded before or
   after the atom was saved."
  [lits proof-vars sigma sigma-out neqs neqs-out residuals residuals-out prog fuel call-depth existentials-as-vars? proof]
  (let [can-descend? (or (nil? call-depth) (pos? call-depth))
        next-call-depth (support/next-call-depth call-depth)
        defer-calls? (and existentials-as-vars? prog)]
    (conde
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
      [(if defer-calls?
         (fresh [atom]
           (membero (list 'pos atom) lits)
           (== sigma sigma-out)
           (== neqs neqs-out)
           (== (lcons (list 'pos atom) residuals) residuals-out)
           (== '(eq-triggered-residual-call) proof))
         fail)]
      [(if defer-calls?
         (fresh [atom]
           (membero (list 'neg atom) lits)
           (== sigma sigma-out)
           (== neqs neqs-out)
           (== (lcons (list 'neg atom) residuals) residuals-out)
           (== '(eq-triggered-residual-neg-call) proof))
         fail)]
      
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

(defn prove-stateo
  "Relational tableau prover with explicit equality and disequality state.

   Arguments:
   - `fml`: current formula to process
   - `unexpanded`: remaining formulas on the current branch
   - `lits`: saved positive and negative atoms on the branch
   - `env`: nominal substitution for lexical binders
   - `sigma`: input substitution for free proof variables `(var nom)`
   - `sigma-out`: output substitution after the branch closes
   - `neqs`: input symbolic disequality store
   - `neqs-out`: output symbolic disequality store
   - `prog`: compiled Proflog program, or nil for theorem-proving mode
   - `proof`: proof term describing the closure"
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out residuals residuals-out prog fuel call-depth existentials-as-vars? proof]
  (let [can-descend? (or (nil? call-depth) (pos? call-depth))
        next-call-depth (support/next-call-depth call-depth)
        defer-calls? (and existentials-as-vars? prog)]
    (conde
      ;; α-rule: both conjuncts must close on the same branch, so the sibling
      ;; conjunct is pushed onto the branch work stack. Equality-triggered saved
      ;; literal closure handles the important order-insensitive case where a
      ;; later equality unlocks an earlier saved atom.
      [(fresh [left right prf]
         (== (list 'and left right) fml)
         (== (list 'conj prf) proof)
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
                       fuel
                       call-depth
                       existentials-as-vars?
                       prf))]

    ;; β-rule: both branches must close under one compatible proof state. The
    ;; resulting substitution threads from the first sibling into the second.
    [(fresh [left right sigma-mid neqs-mid residuals-mid left-proof right-proof]
            (== (list 'or left right) fml)
            (== (list 'split left-proof right-proof) proof)
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
                          fuel
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
                          fuel
                          call-depth
                          existentials-as-vars?
                          right-proof))]

    ;; γ-rule: instantiate a universal with an explicit free variable term and
    ;; re-enqueue the original universal so later instantiations remain
    ;; available on the branch.
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
                                   (fresh [body body-subst narrowed-env prf]
                                          (== (list 'once-forall (nominal/tie binding-nom body)) fml)
                                          (== (list 'once-univ prf) proof)
                                          (subst/remove-bindo binding-nom env narrowed-env)
                                          (subst/subst-formulao body narrowed-env body-subst)
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
                                                        fuel
                                                        call-depth
                                                        existentials-as-vars?
                                                        prf))))]

    ;; δ-rule: instantiate an existential exactly once with a rigid internal
    ;; parameter in ordinary proof search. Answer export instead introduces a
    ;; fresh object-language variable so existential structure can remain
    ;; symbolic and continue constraining open queries relationally.
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
    [(fresh [lit left right sigma-mid step-proof next rest prf]
            (subst/subst-formulao fml env lit)
            (== (list 'eq left right) lit)
            (equality/unify-termo left right sigma sigma-mid step-proof)
            (== (lcons next rest) unexpanded)
            (== (list 'eq-step step-proof prf) proof)
            (support/stable-neqso neqs sigma-mid)
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
                          fuel
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
    [(fresh [lit left right next rest prf]
            (subst/subst-formulao fml env lit)
            (== (list 'neq left right) lit)
            (== (lcons next rest) unexpanded)
            (== (list 'neq-store prf) proof)
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
                          fuel
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
    [(if defer-calls?
       (fresh [lit atom next rest prf]
              (subst/subst-formulao fml env lit)
              (== (list 'pos atom) lit)
              (== (lcons next rest) unexpanded)
              (== (list 'defer-call prf) proof)
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
                            fuel
                            call-depth
                            existentials-as-vars?
                            prf))
       fail)]
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
    [(if defer-calls?
       (fresh [lit atom next rest prf]
              (subst/subst-formulao fml env lit)
              (== (list 'neg atom) lit)
              (== (lcons next rest) unexpanded)
              (== (list 'defer-call prf) proof)
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
                            fuel
                            call-depth
                            existentials-as-vars?
                            prf))
       fail)]
    [(if defer-calls?
       (fresh [lit atom]
              (subst/subst-formulao fml env lit)
              (== (list 'neg atom) lit)
              (== sigma sigma-out)
              (== neqs neqs-out)
              (== (lcons lit residuals) residuals-out)
              (== '(defer-call) proof))
       fail)]
    [(fresh [lit atom next rest prf]
            (subst/subst-formulao fml env lit)
            (== (list 'pos atom) lit)
            (== (lcons next rest) unexpanded)
            (== (list 'savefml prf) proof)
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
                          fuel
                          call-depth
                          existentials-as-vars?
                          prf))]
    [(fresh [lit atom next rest prf]
            (subst/subst-formulao fml env lit)
            (== (list 'neg atom) lit)
            (== (lcons next rest) unexpanded)
            (== (list 'savefml prf) proof)
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
                          fuel
                          call-depth
                          existentials-as-vars?
                          prf))])))

(defn proveo
  "Public five-argument kernel relation.

   Existing callers see the same surface signature, but each branch now starts
   with an empty equality substitution and empty disequality store."
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
   `residuals-out`."
  ([fml unexpanded lits env answer-vars sigma-out neqs-out residuals-out proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out nil nil 1 true proof))
  ([fml unexpanded lits env answer-vars sigma-out neqs-out residuals-out fuel proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out nil fuel 1 true proof))
  ([fml unexpanded lits env answer-vars sigma-out neqs-out residuals-out fuel call-depth proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out nil fuel call-depth true proof)))

(defn prove-programo
  "Kernel relation with an explicit compiled program for procedure calls."
  ([fml unexpanded lits env prog proof]
   (fresh [sigma-out neqs-out residuals-out]
          (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out '() residuals-out prog nil nil false proof)))
  ([fml unexpanded lits env prog fuel proof]
   (fresh [sigma-out neqs-out residuals-out]
          (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out '() residuals-out prog fuel nil false proof))))

(defn prove-program-answero
  "Kernel relation for query-answer export with explicit answer variables."
  ([fml unexpanded lits env answer-vars prog sigma-out neqs-out residuals-out proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out prog nil 1 true proof))
  ([fml unexpanded lits env answer-vars prog sigma-out neqs-out residuals-out fuel proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out prog fuel 1 true proof))
  ([fml unexpanded lits env answer-vars prog sigma-out neqs-out residuals-out fuel call-depth proof]
   (prove-stateo fml unexpanded lits env answer-vars '() sigma-out '() neqs-out '() residuals-out prog fuel call-depth true proof)))

(defn prove-program-query-entryo
  "Kernel relation for top-level literal query-answer export relative to `prog`.

   The entry procedure call itself does not consume `call-depth`; that staged
   budget is reserved for recursive descendants below the query boundary."
  ([lit answer-vars prog sigma-out neqs-out residuals-out proof]
   (prove-program-query-entryo lit answer-vars prog sigma-out neqs-out residuals-out nil 0 proof))
  ([lit answer-vars prog sigma-out neqs-out residuals-out fuel proof]
   (prove-program-query-entryo lit answer-vars prog sigma-out neqs-out residuals-out fuel 0 proof))
  ([lit answer-vars prog sigma-out neqs-out residuals-out fuel call-depth proof]
   (conde
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
  "Return up to `n` proof terms closing the given greenfield formula."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
        (proveo fml '() '() '() proof)))
  ([fml n fuel]
   (run n [proof]
        (proveo fml '() '() '() fuel proof))))

(defn prove-program
  "Return up to `n` proof terms closing `fml` relative to `prog`."
  ([prog fml n]
   (run n [proof]
        (prove-programo fml '() '() '() prog proof)))
  ([prog fml n fuel]
   (run n [proof]
        (prove-programo fml '() '() '() prog fuel proof))))
