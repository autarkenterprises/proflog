(ns proflog.kernel
  "Greenfield tableau kernel with explicit equality state.

   This namespace is the ordinary proof-search core, not the answer-export
   layer. For a reader coming from Fitting's 1994 Proflog paper, the guiding
   picture is:

   - `prove-stateo` is the branch-closing tableau relation,
   - connectives and quantifiers follow the usual alpha / beta / gamma / delta
     operational reading,
   - literals are handled against an explicit branch state rather than by
     destructive side effects,
   - equality is modeled by an explicit substitution `sigma` plus a symbolic
     disequality store `neqs`,
   - and procedure calls are just another tableau step when an atom is
     sufficiently inside the object language `L`.

   Relative to the legacy experimental prover, the major structural difference
   is that equality information is not carried implicitly by branch rewriting.
   Instead:

   - gamma-introduced free proof variables are represented as `(var nom)`,
   - delta witnesses are rigid parameters `(par nom)`,
   - positive equality extends the explicit substitution,
   - negative equality may be stored symbolically until later bindings force a
     contradiction,
   - and saved literals are rechecked after each new equality step."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== appendo conde fresh lcons membero run]]
            [clojure.core.logic.nominal :as nominal]
            [proflog.ast :as ast]
            [proflog.equality :as equality]
            [proflog.gamma :as gamma]
            [proflog.kernel-support :as support]
            [proflog.program :as program]
            [proflog.subst :as subst]))

;; Reading guide
;; -------------
;;
;; The kernel keeps exactly the branch-local data that Fitting's operational
;; presentation leaves implicit:
;;
;; - `fml` / `unexpanded`: the current formula and the remaining branch work,
;; - `lits`: saved positive / negative atoms already on the branch,
;; - `env`: lexical substitution for bound variables introduced by tableau
;;   quantifier rules,
;; - `proof-vars`: the noms introduced by gamma so we can distinguish proof-time
;;   instantiations from user-visible answer variables,
;; - `sigma`: the explicit free-constructor equality substitution,
;; - `neqs`: delayed disequalities that remain open for now,
;; - `prog`: the compiled Proflog program used by the Procedure Call Rule,
;; - `fuel`: bounded micro-step control for non-closing branch progress,
;; - `proof`: the proof term witnessing the branch closure.
;;
;; The companion `proflog.answer-overlay` namespace reuses the same underlying
;; machinery but adds answer-variable export, residual deferred calls, and
;; recursive call-depth control. This file intentionally stops short of those
;; answer-oriented concerns.

(declare prove-stateo close-agendao saved-call-closeso)

(def ^:dynamic *recursive-prove-stateo*
  "Optional recursive proof dispatcher.

   The ordinary kernel leaves this unbound and recurses directly through
   `prove-stateo`. Operational layers such as ADR-0017 tabling may bind it to a
   wrapper relation so recursive branch calls are memoized without adding table
   management to the Fitting-style rule clauses below."
  nil)

(defn- recursive-prove-stateo
  [& args]
  (apply (or *recursive-prove-stateo* prove-stateo) args))

;; Re-export the structural L-groundness relation here because procedure-call
;; admissibility is part of the kernel story from the paper's perspective.
(def l-ground-termo support/l-ground-termo)

(defn saved-call-closeso
  "Succeed when one saved atom becomes callable under the current equality
   substitution and its subsidiary tableau closes.

   This is the greenfield replacement for a large part of legacy
   equality-triggered paramodulation around procedure calls. Instead of
   rewriting saved literals syntactically, we:

   1. keep atoms on the branch in `lits`,
   2. walk them through the current equality substitution `sigma`,
   3. check whether the walked atom is now an admissible procedure call,
   4. and, if so, open the subsidiary tableau for the clause body.

   The important semantic point is that procedure-call completeness should
   depend on branch state, not on whether the enabling equality happened to be
   expanded before or after the atom was saved."
  [lits proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (conde
    ;; Saved positive atom. If equality has now walked its arguments into an
    ;; admissible L-ground shape, open the subsidiary tableau for the clause
    ;; body exactly as though the call had been available when the atom first
    ;; appeared.
    [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (membero (list 'pos atom) lits)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'eq-triggered-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo body
                               '()
                               '()
                               call-env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               subproof))]
    ;; Saved negative atom. This is Fitting's "Part 2" procedure-call rule:
    ;; prove the NNF negation of the clause body rather than the body itself.
    [(fresh [atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (membero (list 'neg atom) lits)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'eq-triggered-neg-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo negated-body
                               '()
                               '()
                               call-env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               subproof))]))

(defn close-agendao
  "Close one explicit pending-formula agenda under the ordinary kernel state.

   ADR-0016 introduces a fairer internal scheduler by making the branch work
   explicit as an agenda. `support/selecto` chooses one pending formula from
   that agenda relationally, and the rest of the tableau rules operate on that
   chosen formula plus the remaining pending work."
  [agenda lits env proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (fresh [fml unexpanded]
    (support/selecto fml agenda unexpanded)
    (conde
    ;; ================================================================
    ;; Alpha rule: conjunction
    ;; ================================================================
    ;;
    ;; Fitting's tableau rule for `A and B` keeps one branch and requires both
    ;; conjuncts to close on that same branch. Operationally we prove the left
    ;; conjunct now and push the right conjunct onto the branch work stack.
    [(fresh [left right next-fuel prf]
       (== (list 'and left right) fml)
       (== (list 'conj prf) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo left
                               (lcons right unexpanded)
                               lits
                               env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               prf))]

    ;; ================================================================
    ;; Beta rule: disjunction
    ;; ================================================================
    ;;
    ;; `A or B` splits the branch. Because the branch state is explicit, the
    ;; first sibling's output substitution and disequalities thread into the
    ;; second sibling. This makes the proof term read like a genuine sequence of
    ;; branch-closing obligations rather than two disconnected searches.
    [(fresh [left right next-fuel sigma-mid neqs-mid left-proof right-proof]
       (== (list 'or left right) fml)
       (== (list 'split left-proof right-proof) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo left
                               unexpanded
                               lits
                               env
                               proof-vars
                               sigma
                               sigma-mid
                               neqs
                               neqs-mid
                               prog
                               next-fuel
                               left-proof)
       (recursive-prove-stateo right
                               unexpanded
                               lits
                               env
                               proof-vars
                               sigma-mid
                               sigma-out
                               neqs-mid
                               neqs-out
                               prog
                               next-fuel
                               right-proof))]

    ;; ================================================================
    ;; Gamma rule: universal quantifier
    ;; ================================================================
    ;;
    ;; Universals are first instantiated with a fresh proof variable `(var
    ;; nom)`, preserving the historical search behavior for most quantified
    ;; programs. Bounded generated closed terms are a fallback path below for
    ;; cases where a concrete Herbrand counterexample is required.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body body-subst narrowed-env next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== '() unexpanded)
           (== (list 'univ prf) proof)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (recursive-prove-stateo body-subst
                                   '()
                                   lits
                                   (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                   (lcons free-var-nom proof-vars)
                                   sigma
                                   sigma-out
                                   neqs
                                   neqs-out
                                   prog
                                   next-fuel
                                   prf))))]
    ;; Fitting's full gamma rule also permits closed terms from the object
    ;; language. The finite generation policy lives outside the kernel; the
    ;; kernel only asks for one candidate when the fresh-variable path is not
    ;; enough.
    [(nominal/fresh [binding-nom]
       (fresh [body body-subst narrowed-env witness-term next-fuel prf]
         (== (list 'forall (nominal/tie binding-nom body)) fml)
         (== '() unexpanded)
         (== (list 'univ prf) proof)
         (gamma/closed-term-candidateo prog fuel witness-term)
         (subst/remove-bindo binding-nom env narrowed-env)
         (subst/subst-formulao body narrowed-env body-subst)
         (support/step-fuelo fuel next-fuel)
         (recursive-prove-stateo body-subst
                                 '()
                                 lits
                                 (lcons [binding-nom witness-term] env)
                                 proof-vars
                                 sigma
                                 sigma-out
                                 neqs
                                 neqs-out
                                 prog
                                 next-fuel
                                 prf)))]
    ;; General gamma case: when there is already pending branch work, append the
    ;; original universal to the end so repeated instantiation remains possible.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body body-subst narrowed-env pending next-fuel prf]
           (== (list 'forall (nominal/tie binding-nom body)) fml)
           (== (list 'univ prf) proof)
           (appendo unexpanded (list fml) pending)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (recursive-prove-stateo body-subst
                                   pending
                                   lits
                                   (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                   (lcons free-var-nom proof-vars)
                                   sigma
                                   sigma-out
                                   neqs
                                   neqs-out
                                   prog
                                   next-fuel
                                   prf))))]
    [(nominal/fresh [binding-nom]
       (fresh [body body-subst narrowed-env witness-term pending next-fuel prf]
         (== (list 'forall (nominal/tie binding-nom body)) fml)
         (== (list 'univ prf) proof)
         (appendo unexpanded (list fml) pending)
         (gamma/closed-term-candidateo prog fuel witness-term)
         (subst/remove-bindo binding-nom env narrowed-env)
         (subst/subst-formulao body narrowed-env body-subst)
         (support/step-fuelo fuel next-fuel)
         (recursive-prove-stateo body-subst
                                 pending
                                 lits
                                 (lcons [binding-nom witness-term] env)
                                 proof-vars
                                 sigma
                                 sigma-out
                                 neqs
                                 neqs-out
                                 prog
                                 next-fuel
                                 prf)))]

    ;; ================================================================
    ;; Once-forall: single-use universal
    ;; ================================================================
    ;;
    ;; This is not a primitive from Fitting's syntax; it is the NNF operational
    ;; form we obtain when negating an existential clause body for negative
    ;; procedure calls. Unlike gamma, it does not re-enqueue itself.
    [(nominal/fresh [binding-nom]
       (fresh [body body-subst narrowed-env witness-term next-fuel prf]
         (== (list 'once-forall (nominal/tie binding-nom body)) fml)
         (== (list 'once-univ prf) proof)
         (gamma/closed-term-candidateo prog fuel witness-term)
         (subst/remove-bindo binding-nom env narrowed-env)
         (subst/subst-formulao body narrowed-env body-subst)
         (support/step-fuelo fuel next-fuel)
         (recursive-prove-stateo body-subst
                                 unexpanded
                                 lits
                                 (lcons [binding-nom witness-term] env)
                                 proof-vars
                                 sigma
                                 sigma-out
                                 neqs
                                 neqs-out
                                 prog
                                 next-fuel
                                 prf)))]
    [(nominal/fresh [binding-nom]
       (nominal/fresh [free-var-nom]
         (fresh [body body-subst narrowed-env next-fuel prf]
           (== (list 'once-forall (nominal/tie binding-nom body)) fml)
           (== (list 'once-univ prf) proof)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (recursive-prove-stateo body-subst
                                   unexpanded
                                   lits
                                   (lcons [binding-nom (ast/var-term free-var-nom)] env)
                                   (lcons free-var-nom proof-vars)
                                   sigma
                                   sigma-out
                                   neqs
                                   neqs-out
                                   prog
                                   next-fuel
                                   prf))))]

    ;; ================================================================
    ;; Delta rule: existential quantifier
    ;; ================================================================
    ;;
    ;; Ordinary proof mode uses a rigid parameter `(par nom)` as the witness.
    ;; This matches the paper's delta-rule intuition: the witness is a fresh
    ;; but fixed element of the current branch, not a freely exportable answer
    ;; variable.
    [(nominal/fresh [binding-nom]
       (nominal/fresh [parameter-nom]
         (fresh [body body-subst narrowed-env next-fuel prf]
           (== (list 'exists (nominal/tie binding-nom body)) fml)
           (== (list 'witness prf) proof)
           (subst/remove-bindo binding-nom env narrowed-env)
           (subst/subst-formulao body narrowed-env body-subst)
           (support/step-fuelo fuel next-fuel)
           (recursive-prove-stateo body-subst
                                   unexpanded
                                   lits
                                   (lcons [binding-nom (ast/par-term parameter-nom)] env)
                                   proof-vars
                                   sigma
                                   sigma-out
                                   neqs
                                   neqs-out
                                   prog
                                   next-fuel
                                   prf))))]

    ;; ================================================================
    ;; Positive equality
    ;; ================================================================
    ;;
    ;; Free-constructor equality is handled in four phases:
    ;;
    ;; 1. immediate contradiction (`eq-contradictiono`),
    ;; 2. successful unification that falsifies a saved disequality,
    ;; 3. successful unification that makes saved complementary atoms unify,
    ;; 4. successful unification that makes a saved procedure call admissible,
    ;; 5. otherwise continue with the updated substitution.
    [(fresh [lit left right contradiction-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/eq-contradictiono left right sigma contradiction-proof)
       (== sigma sigma-out)
       (== neqs neqs-out)
       (== contradiction-proof proof))]

    ;; New equality binding makes a previously saved disequality impossible.
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (equality/neq-violatedo neqs sigma-mid branch-proof)
       (== sigma-mid sigma-out)
       (support/prune-contradictory-neqso neqs sigma-mid neqs-out)
       (== (list 'eq-step step-proof branch-proof) proof))]
    ;; New equality binding makes a saved positive and negative atom unify.
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (equality/contradictory-atomso lits sigma-mid sigma-out branch-proof)
       (support/prune-contradictory-neqso neqs sigma-out neqs-out)
       (== (list 'eq-step step-proof branch-proof) proof))]
    ;; New equality binding reopens a previously saved procedure call.
    [(fresh [lit left right sigma-mid step-proof branch-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (saved-call-closeso lits proof-vars sigma-mid sigma-out neqs neqs-out prog fuel branch-proof)
       (== (list 'eq-step step-proof branch-proof) proof))]
    ;; No immediate contradiction: keep the updated equality state and continue
    ;; with the next pending formula, provided the saved disequalities still
    ;; remain genuinely open under the new substitution.
    [(fresh [lit left right sigma-mid step-proof next rest next-fuel prf]
       (subst/subst-formulao fml env lit)
       (== (list 'eq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (== (lcons next rest) unexpanded)
       (== (list 'eq-step step-proof prf) proof)
       (support/stable-neqso neqs sigma-mid)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo next
                               rest
                               lits
                               env
                               proof-vars
                               sigma-mid
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               prf))]

    ;; ================================================================
    ;; Negative equality
    ;; ================================================================
    ;;
    ;; `neq(t1, t2)` closes immediately only when the current substitution
    ;; already makes the two walked terms identical. Otherwise it either closes
    ;; by forcing equality through proof-local variables, or it is stored for
    ;; later rechecking after future equality steps.
    [(fresh [lit left right]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/same-termo left right sigma)
       (== sigma sigma-out)
       (== neqs neqs-out)
       (== '(refl-close) proof))]
    ;; The disequality can be contradicted if the branch is allowed to bind one
    ;; or more gamma-introduced proof variables. We explicitly reject closures
    ;; that would require binding user-level answer variables; only proof-time
    ;; variables may witness the contradiction here.
    [(fresh [lit left right sigma-mid new-bindings binding rest step-proof]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (equality/unify-termo left right sigma sigma-mid step-proof)
       (appendo new-bindings sigma sigma-mid)
       (== (lcons binding rest) new-bindings)
       (support/proof-bindingso new-bindings proof-vars)
       (== sigma-mid sigma-out)
       (support/prune-contradictory-neqso neqs sigma-mid neqs-out)
       (== (list 'neq-close step-proof) proof))]
    ;; Otherwise retain the disequality as a delayed symbolic obligation.
    [(fresh [lit left right next rest next-fuel prf]
       (subst/subst-formulao fml env lit)
       (== (list 'neq left right) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'neq-store prf) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo next
                               rest
                               lits
                               env
                               proof-vars
                               sigma
                               sigma-out
                               (lcons [left right] neqs)
                               neqs-out
                               prog
                               next-fuel
                               prf))]

    ;; ================================================================
    ;; Positive atoms
    ;; ================================================================
    ;;
    ;; First try ordinary complementary closure against a saved negative atom.
    ;; Failing that, Fitting's Procedure Call Rule may open a subsidiary
    ;; tableau for the body of the matching clause. If neither applies yet, the
    ;; atom is saved on the branch for possible later equality-triggered use.
    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (support/complementary-lito lit lits sigma sigma-out proof)
       (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
    ;; Positive procedure call: only admissible once equality has walked the
    ;; arguments into the object language `L`.
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'pos-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo body
                               '()
                               '()
                               call-env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               subproof))]
    ;; Save the positive atom if it cannot close or call immediately.
    [(fresh [lit atom next rest next-fuel prf]
       (subst/subst-formulao fml env lit)
       (== (list 'pos atom) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'savefml prf) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo next
                               rest
                               (lcons lit lits)
                               env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               prf))]

    ;; ================================================================
    ;; Negative atoms
    ;; ================================================================
    ;;
    ;; Symmetric to the positive case, except that the procedure call proves
    ;; the NNF negation of the clause body.
    [(fresh [lit atom]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (support/complementary-lito lit lits sigma sigma-out proof)
       (support/prune-contradictory-neqso neqs sigma-out neqs-out))]
    ;; Negative procedure call: this is Fitting's Part 2 operationalized over
    ;; the compiled clause's precomputed `negated-body`.
    [(fresh [lit atom walked-atom relation args call-env body negated-body next-fuel subproof]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (equality/walk-atomo atom sigma walked-atom)
       (== (lcons 'app (lcons relation args)) walked-atom)
       (support/l-ground-term*o args)
       (program/call-clauseo prog walked-atom call-env body negated-body)
       (== (list 'neg-call subproof) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo negated-body
                               '()
                               '()
                               call-env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               subproof))]
    ;; Save the negative atom if it cannot yet close or call.
    [(fresh [lit atom next rest next-fuel prf]
       (subst/subst-formulao fml env lit)
       (== (list 'neg atom) lit)
       (== (lcons next rest) unexpanded)
       (== (list 'savefml prf) proof)
       (support/step-fuelo fuel next-fuel)
       (recursive-prove-stateo next
                               rest
                               (lcons lit lits)
                               env
                               proof-vars
                               sigma
                               sigma-out
                               neqs
                               neqs-out
                               prog
                               next-fuel
                               prf))])))

(defn prove-stateo
  "Backward-compatible current-formula wrapper over the fair agenda kernel.

   Existing callers still pass one focused formula plus the rest of the branch
   work, but the internal engine now treats them as one explicit agenda."
  [fml unexpanded lits env proof-vars sigma sigma-out neqs neqs-out prog fuel proof]
  (close-agendao
    (lcons fml unexpanded)
    lits
    env
    proof-vars
    sigma
    sigma-out
    neqs
    neqs-out
    prog
    fuel
    proof))

(defn proveo
  "Public pure-kernel relation.

   This is the ordinary proof surface: it exposes only proof terms, not the
   intermediate equality substitution or delayed disequalities. In other words,
   the kernel is relational internally, but this wrapper deliberately hides the
   answer-oriented state that the overlay later exports explicitly."
  ([fml unexpanded lits env proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out nil nil proof)))
  ([fml unexpanded lits env fuel proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out nil fuel proof))))

(defn prove-programo
  "Pure kernel relation with an explicit compiled program for procedure calls.

   This is the direct analogue of `proveo` when the tableau may invoke
   Proflog clauses through Fitting's Procedure Call Rule."
  ([fml unexpanded lits env prog proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out prog nil proof)))
  ([fml unexpanded lits env prog fuel proof]
   (fresh [sigma-out neqs-out]
     (prove-stateo fml unexpanded lits env '() '() sigma-out '() neqs-out prog fuel proof))))

(defn prove
  "Return up to `n` proof terms closing the given greenfield formula.

   This is a convenience wrapper for theorem-proving style use: start with an
   empty branch state and ask core.logic for proof witnesses."
  ([fml] (prove fml 1))
  ([fml n]
   (run n [proof]
        (proveo fml '() '() '() proof)))
  ([fml n fuel]
   (run n [proof]
        (proveo fml '() '() '() fuel proof))))

(defn prove-program
  "Return up to `n` proof terms closing `fml` relative to `prog`.

   This keeps the program explicit and otherwise starts from the empty kernel
   state, mirroring the paper's use of a fixed Proflog program during proof
   search."
  ([prog fml n]
   (run n [proof]
        (prove-programo fml '() '() '() prog proof)))
  ([prog fml n fuel]
   (run n [proof]
        (prove-programo fml '() '() '() prog fuel proof))))
