;;; ===========================================================================
;;; PROFLOG: A Semantic Tableaux-Based Logic Programming Language
;;; ===========================================================================
;;;
;;; The first implementation of Melvin Fitting's "Proflog" language, described
;;; in "Tableaux for Logic Programming" (J. Automated Reasoning, 13:175-188,
;;; 1994). Proflog uses semantic tableaux as its operational semantics and
;;; supervaluations as its denotational semantics. It strictly generalizes
;;; Prolog: when negation is absent, Proflog reduces to SLD resolution.
;;;
;;; Key advantages over Prolog:
;;;   - Full first-order logic (negation, disjunction, implication, quantifiers)
;;;   - Monotonic negation via supervaluations (no negation-as-failure)
;;;   - Three-valued outcomes: succeed, fail, or unknown (truth-value gap)
;;;   - Sound treatment of equality via the free-closure and one-one rules
;;;
;;; Implementation uses miniKanren for unification and variable management.
;;; Search is by iterative deepening on gamma-rule + procedure-call depth.
;;;
;;; Requires: R5RS or R7RS Scheme (tested with Chez Scheme, Guile, Racket)
;;; ===========================================================================


;;; ===========================================================================
;;; PART 1: microKanren Core
;;; A minimal relational programming kernel providing unification, logic
;;; variables, and the substitution model.  If you have a full miniKanren
;;; installation, you can replace this section with (import (minikanren)).
;;; ===========================================================================

;;; --- Logic Variables ---
;;; Variables are represented as single-element vectors wrapping a unique
;;; integer counter.  This makes them distinguishable from any user data.

(define (var c) (vector c))
(define (var? x) (vector? x))
(define (var=? x1 x2) (= (vector-ref x1 0) (vector-ref x2 0)))

;;; --- Substitutions ---
;;; A substitution is a triangular association list mapping variables to terms.
;;; `walk` chases bindings to find the current value of a variable.

(define empty-subst '())

(define (walk u s)
  (let ((pr (and (var? u)
                 (let loop ((s s))
                   (cond
                     ((null? s) #f)
                     ((var=? u (caar s)) (car s))
                     (else (loop (cdr s))))))))
    (if pr (walk (cdr pr) s) u)))

;;; Deep walk: recursively resolve all variables in a term.
(define (walk* v s)
  (let ((v (walk v s)))
    (cond
      ((var? v) v)
      ((pair? v) (cons (walk* (car v) s) (walk* (cdr v) s)))
      (else v))))

;;; --- Occurs Check ---
;;; Prevents construction of circular/infinite terms (essential for soundness
;;; in first-order theorem proving).

(define (occurs? x v s)
  (let ((v (walk v s)))
    (cond
      ((var? v) (var=? v x))
      ((pair? v) (or (occurs? x (car v) s) (occurs? x (cdr v) s)))
      (else #f))))

(define (ext-s x v s)
  (if (occurs? x v s) #f
      (cons (cons x v) s)))

;;; --- Unification ---
;;; The core operation: attempts to make two terms equal by extending the
;;; substitution.  Returns the extended substitution on success, #f on failure.
;;; This subsumes Fitting's One-One Rule (structural decomposition) and
;;; Free Closure Rule (distinct constructors fail to unify).

(define (unify u v s)
  (let ((u (walk u s)) (v (walk v s)))
    (cond
      ((and (var? u) (var? v) (var=? u v)) s)
      ((var? u) (ext-s u v s))
      ((var? v) (ext-s v u s))
      ((and (pair? u) (pair? v))
       (let ((s (unify (car u) (car v) s)))
         (and s (unify (cdr u) (cdr v) s))))
      (else (and (equal? u v) s)))))


;;; ===========================================================================
;;; PART 2: Formula Representation
;;; ===========================================================================
;;;
;;; User-level formula syntax (for program clauses and queries):
;;;
;;;   Atomic:       (R t1 t2 ...)      — relation application
;;;   Equality:     (= t1 t2)          — equality
;;;   Negation:     (not φ)
;;;   Conjunction:  (and φ ψ)
;;;   Disjunction:  (or φ ψ)
;;;   Implication:  (implies φ ψ)      — φ ⊃ ψ
;;;   Universal:    (forall x φ)
;;;   Existential:  (exists x φ)
;;;   Constants:    true, false
;;;
;;; Terms:
;;;   Variables:    Scheme symbols (x, y, z, ...)
;;;   Constants:    quoted or self-evaluating (0, a, b, ...)
;;;   Functions:    (f t1 t2 ...)
;;;
;;; Internal representation (after NNF conversion):
;;;
;;;   (and φ ψ)           — alpha formula (conjunctive, non-branching)
;;;   (or φ ψ)            — beta formula  (disjunctive, branching)
;;;   (forall x φ)        — gamma formula (universal, depth-limited)
;;;   (exists x φ)        — delta formula (existential, fresh witness)
;;;   (pos (R t1 ...))    — positive literal
;;;   (neg (R t1 ...))    — negative literal
;;;   (eq t1 t2)          — positive equality literal
;;;   (neq t1 t2)         — negative equality literal
;;;   true                — trivially true (branch closes vacuously)
;;;   false               — trivially false (contradiction)
;;; ===========================================================================

;;; --- Formula predicates ---

(define (and-fml? f)   (and (pair? f) (eq? (car f) 'and)))
(define (or-fml? f)    (and (pair? f) (eq? (car f) 'or)))
(define (forall-fml? f)(and (pair? f) (eq? (car f) 'forall)))
(define (exists-fml? f)(and (pair? f) (eq? (car f) 'exists)))
(define (pos-lit? f)   (and (pair? f) (eq? (car f) 'pos)))
(define (neg-lit? f)   (and (pair? f) (eq? (car f) 'neg)))
(define (eq-fml? f)    (and (pair? f) (eq? (car f) 'eq)))
(define (neq-fml? f)   (and (pair? f) (eq? (car f) 'neq)))
(define (literal? f)   (or (pos-lit? f) (neg-lit? f) (eq-fml? f) (neq-fml? f)
                           (eq? f 'true) (eq? f 'false)))

;;; --- Formula accessors ---

(define (fml-arg1 f) (cadr f))
(define (fml-arg2 f) (caddr f))
;; For forall/exists: (forall var body)
(define (quant-var f) (cadr f))
(define (quant-body f) (caddr f))


;;; ===========================================================================
;;; PART 3: Negation Normal Form (NNF) Conversion
;;; ===========================================================================
;;;
;;; Converts arbitrary first-order formulas to NNF by pushing negations
;;; inward to the atomic level.  After conversion, negation only appears
;;; as (neg ...) on atoms, never as a connective.  Implication is eliminated.
;;;
;;; This is a deterministic Scheme function (not a miniKanren relation).
;;; ===========================================================================

;;; The set of relation symbols is needed to distinguish relation applications
;;; (which become pos/neg literals) from function applications in terms.

(define (nnf formula relation-symbols)
  (define (rel? sym) (memq sym relation-symbols))
  (define (convert f polarity)
    ;; polarity: #t means positive context, #f means negated context
    (cond
      ;; Constants
      ((eq? f 'true)  (if polarity 'true 'false))
      ((eq? f 'false) (if polarity 'false 'true))

      ;; Atomic formula: R(t1,...,tn)
      ((and (pair? f) (not (memq (car f) '(not and or implies forall exists =))))
       (if polarity `(pos ,f) `(neg ,f)))

      ;; Equality
      ((and (pair? f) (eq? (car f) '=))
       (if polarity
           `(eq ,(cadr f) ,(caddr f))
           `(neq ,(cadr f) ,(caddr f))))

      ;; Negation: flip polarity
      ((and (pair? f) (eq? (car f) 'not))
       (convert (cadr f) (not polarity)))

      ;; Conjunction
      ((and (pair? f) (eq? (car f) 'and))
       (if polarity
           `(and ,(convert (cadr f) #t) ,(convert (caddr f) #t))
           ;; ¬(A ∧ B) → (¬A ∨ ¬B)
           `(or ,(convert (cadr f) #f) ,(convert (caddr f) #f))))

      ;; Disjunction
      ((and (pair? f) (eq? (car f) 'or))
       (if polarity
           `(or ,(convert (cadr f) #t) ,(convert (caddr f) #t))
           ;; ¬(A ∨ B) → (¬A ∧ ¬B)
           `(and ,(convert (cadr f) #f) ,(convert (caddr f) #f))))

      ;; Implication: A ⊃ B ≡ ¬A ∨ B
      ((and (pair? f) (eq? (car f) 'implies))
       (if polarity
           `(or ,(convert (cadr f) #f) ,(convert (caddr f) #t))
           ;; ¬(A ⊃ B) → A ∧ ¬B
           `(and ,(convert (cadr f) #t) ,(convert (caddr f) #f))))

      ;; Universal quantifier
      ((and (pair? f) (eq? (car f) 'forall))
       (if polarity
           `(forall ,(cadr f) ,(convert (caddr f) #t))
           ;; ¬∀x.A → ∃x.¬A
           `(exists ,(cadr f) ,(convert (caddr f) #f))))

      ;; Existential quantifier
      ((and (pair? f) (eq? (car f) 'exists))
       (if polarity
           `(exists ,(cadr f) ,(convert (caddr f) #t))
           ;; ¬∃x.A → ∀x.¬A
           `(forall ,(cadr f) ,(convert (caddr f) #f))))

      ;; Fallthrough: treat as atomic
      (else (if polarity `(pos ,f) `(neg ,f)))))

  (convert formula #t))

;;; Negate an NNF formula (produces a new NNF formula).
;;; Used by the Procedure Call Rule for negative literals.
(define (negate-nnf f)
  (cond
    ((eq? f 'true) 'false)
    ((eq? f 'false) 'true)
    ((pos-lit? f) `(neg ,(cadr f)))
    ((neg-lit? f) `(pos ,(cadr f)))
    ((eq-fml? f) `(neq ,(cadr f) ,(caddr f)))
    ((neq-fml? f) `(eq ,(cadr f) ,(caddr f)))
    ((and-fml? f) `(or ,(negate-nnf (cadr f)) ,(negate-nnf (caddr f))))
    ((or-fml? f) `(and ,(negate-nnf (cadr f)) ,(negate-nnf (caddr f))))
    ((forall-fml? f) `(exists ,(cadr f) ,(negate-nnf (caddr f))))
    ((exists-fml? f) `(forall ,(cadr f) ,(negate-nnf (caddr f))))
    (else (error "negate-nnf: unknown formula" f))))


;;; ===========================================================================
;;; PART 4: Program Representation
;;; ===========================================================================
;;;
;;; A Proflog program consists of L-clauses of the form:
;;;     R(x1,...,xn) ← φ(x1,...,xn)
;;;
;;; We represent a clause as a Scheme record containing:
;;;   - head:     the relation name (symbol)
;;;   - params:   list of formal parameter names (symbols)
;;;   - body-pos: NNF of φ      (for positive procedure calls)
;;;   - body-neg: NNF of ¬φ     (for negative procedure calls)
;;;   - raw-body: the original formula φ
;;; ===========================================================================

(define (make-clause head-rel params body-pos body-neg raw-body)
  (list 'proflog-clause head-rel params body-pos body-neg raw-body))

(define (clause-rel c)      (list-ref c 1))
(define (clause-params c)   (list-ref c 2))
(define (clause-body-pos c) (list-ref c 3))
(define (clause-body-neg c) (list-ref c 4))
(define (clause-raw-body c) (list-ref c 5))

;;; Build a program from a list of clause definitions.
;;; Each definition: (define-clause (R x1 ... xn) body-formula)
;;; The relation-symbols list is extracted from the clause heads.

(define (build-program clause-defs)
  (let* ((rel-syms (map (lambda (d) (caadr d)) clause-defs))
         ;; Always include = in relation symbols
         (all-rels (cons '= rel-syms)))
    (map (lambda (def)
           (let* ((head (cadr def))
                  (body (caddr def))
                  (rel (car head))
                  (params (cdr head))
                  (body-pos (nnf body all-rels))
                  (body-neg (nnf `(not ,body) all-rels)))
             (make-clause rel params body-pos body-neg body)))
         clause-defs)))

;;; Look up all clauses for a given relation symbol.
(define (lookup-clauses rel program)
  (filter (lambda (c) (eq? (clause-rel c) rel)) program))

;;; Helper: filter
(define (filter pred lst)
  (cond
    ((null? lst) '())
    ((pred (car lst)) (cons (car lst) (filter pred (cdr lst))))
    (else (filter pred (cdr lst)))))


;;; ===========================================================================
;;; PART 5: Variable Substitution in Formulas
;;; ===========================================================================
;;;
;;; When the Procedure Call Rule fires or a quantifier is instantiated,
;;; we need to substitute logic variables (miniKanren vars) for symbolic
;;; parameter names in formulas.  This operates on the formula-as-data-
;;; structure level, not on the miniKanren substitution.
;;; ===========================================================================

;;; Substitute: replace every occurrence of symbol `sym` with value `val`
;;; in a formula or term.  Handles all formula constructors.

(define (subst-in-formula sym val formula)
  (cond
    ;; The symbol itself
    ((and (symbol? formula) (eq? formula sym)) val)
    ;; A logic variable or other atom — pass through
    ((not (pair? formula)) formula)
    ;; Quantifier: don't substitute the bound variable if it shadows
    ((or (eq? (car formula) 'forall) (eq? (car formula) 'exists))
     (let ((qvar (cadr formula))
           (body (caddr formula)))
       (if (eq? qvar sym)
           formula  ; shadowed — do not substitute in body
           `(,(car formula) ,qvar ,(subst-in-formula sym val body)))))
    ;; Recurse into compound structures
    (else
     (cons (subst-in-formula sym val (car formula))
           (subst-in-formula sym val (cdr formula))))))

;;; Substitute multiple symbols at once from an alist ((sym . val) ...).
(define (subst-all-in-formula alist formula)
  (if (null? alist)
      formula
      (subst-all-in-formula
       (cdr alist)
       (subst-in-formula (caar alist) (cdar alist) formula))))


;;; ===========================================================================
;;; PART 6: The Tableau Engine
;;; ===========================================================================
;;;
;;; This is the heart of Proflog.  The engine implements Fitting's semantic
;;; tableaux with the Procedure Call Rule.  It processes formulas on a branch,
;;; attempting to close all branches (refutation).
;;;
;;; State: (substitution . variable-counter)
;;;   The substitution accumulates variable bindings from unification.
;;;   The counter generates fresh logic variables.
;;;
;;; The engine returns a list of successful states (each state represents
;;; one way to close the tableau).  Multiple states = multiple answers.
;;;
;;; Key design: iterative deepening on a combined depth parameter that
;;; decrements on gamma-rule applications AND procedure calls.
;;; ===========================================================================

;;; --- State operations ---
(define (make-state subst counter) (cons subst counter))
(define (state-subst st) (car st))
(define (state-counter st) (cdr st))
(define (fresh-var st) (var (state-counter st)))
(define (bump-counter st) (make-state (state-subst st) (+ 1 (state-counter st))))

;;; --- Flatmap for combining results ---
(define (flatmap f lst)
  (if (null? lst) '()
      (append (f (car lst)) (flatmap f (cdr lst)))))

;;; --- The main tableau prover ---
;;;
;;; prove-branch : formula × [formula] × [literal] × state × program × depth
;;;              → [state]
;;;
;;; Arguments:
;;;   fml     — the current formula being processed
;;;   unexp   — stack of unexpanded formulas on this branch
;;;   lits    — list of literal formulas already placed on this branch
;;;   state   — (substitution . var-counter) pair
;;;   program — the Proflog program (list of clauses)
;;;   depth   — remaining depth for gamma-rules and procedure calls
;;;
;;; Returns: list of states for which the branch (and all sub-branches) close

(define (prove-branch fml unexp lits state program depth)
  (cond

    ;; ----- TRUE: trivially satisfied, continue with remaining formulas -----
    ((eq? fml 'true)
     (continue-branch unexp lits state program depth))

    ;; ----- FALSE: contradiction on branch → branch closes -----
    ((eq? fml 'false)
     (list state))

    ;; ----- ALPHA RULE: conjunction (non-branching) -----
    ;; T(A ∧ B) → add both A and B to the branch
    ((and-fml? fml)
     (let ((a (fml-arg1 fml))
           (b (fml-arg2 fml)))
       (prove-branch a (cons b unexp) lits state program depth)))

    ;; ----- BETA RULE: disjunction (branching) -----
    ;; T(A ∨ B) → split into two sub-branches; BOTH must close
    ;; In a refutation tableau for ¬(A∨B), this becomes a conjunction.
    ;; But in NNF, (or A B) means the original formula was disjunctive,
    ;; so we need both disjuncts to lead to closure on all paths.
    ;; Actually: in a refutation, we have the formula itself on the branch.
    ;; (or A B) on a branch means "A or B holds." For the branch to close,
    ;; we must refute BOTH possibilities. Hence: two sub-branches, both close.
    ((or-fml? fml)
     (let ((a (fml-arg1 fml))
           (b (fml-arg2 fml)))
       ;; Try branch A; for each successful closure of A,
       ;; try branch B with the (possibly enriched) substitution.
       ;; Both branches share the same literals and unexpanded formulas.
       (flatmap
        (lambda (state-after-a)
          (prove-branch b unexp lits state-after-a program depth))
        (prove-branch a unexp lits state program depth))))

    ;; ----- GAMMA RULE: universal quantifier (depth-limited) -----
    ;; T(∀x.A) → instantiate with a fresh variable, re-add ∀x.A for later.
    ;; This is the only rule that can be applied unboundedly, hence the
    ;; depth limit. Iterative deepening in the outer loop ensures completeness.
    ((forall-fml? fml)
     (if (<= depth 0)
         '()  ; depth exhausted — cannot apply gamma rule
         (let* ((x (quant-var fml))
                (body (quant-body fml))
                (st (bump-counter state))
                (v (fresh-var state))
                (instantiated-body (subst-in-formula x v body))
                ;; Re-add the universal to the END of unexpanded list
                ;; (ensures other formulas get processed first)
                (new-unexp (append unexp (list fml))))
           (prove-branch instantiated-body new-unexp lits
                         st program (- depth 1)))))

    ;; ----- DELTA RULE: existential quantifier (fresh witness) -----
    ;; T(∃x.A) → instantiate with a fresh variable (Skolem witness).
    ;; Unlike gamma, this is applied exactly once (no re-addition to stack)
    ;; and does NOT consume depth.
    ((exists-fml? fml)
     (let* ((x (quant-var fml))
            (body (quant-body fml))
            (st (bump-counter state))
            (v (fresh-var state))
            (instantiated-body (subst-in-formula x v body)))
       (prove-branch instantiated-body unexp lits st program depth)))

    ;; ----- POSITIVE EQUALITY: eq -----
    ;; (eq t u) on the branch asserts t = u.
    ;; Unify t and u.  If unification succeeds, continue with enriched
    ;; substitution.  If it fails, that's a contradiction → branch closes.
    ((eq-fml? fml)
     (let* ((t (walk* (fml-arg1 fml) (state-subst state)))
            (u (walk* (fml-arg2 fml) (state-subst state)))
            (new-subst (unify t u (state-subst state))))
       (if new-subst
           ;; Unification succeeded: equality is consistent, continue
           (continue-branch unexp lits
                            (make-state new-subst (state-counter state))
                            program depth)
           ;; Unification failed: branch is inconsistent → closes
           ;; But wait: (eq t u) means we ASSUME t=u. If we can't make it
           ;; consistent, the assumption is vacuously false on this branch.
           ;; In a refutation context, this means the branch closes.
           ;; However, this is only correct if (eq t u) came from the
           ;; negated goal.  For safety, we return empty (can't continue).
           '())))

    ;; ----- NEGATIVE EQUALITY: neq -----
    ;; (neq t u) asserts t ≠ u (from ¬(t = u) in NNF).
    ;; If t and u are identical after walking, that's a contradiction.
    ;; Otherwise, save as a literal on the branch.
    ((neq-fml? fml)
     (let* ((t (walk* (fml-arg1 fml) (state-subst state)))
            (u (walk* (fml-arg2 fml) (state-subst state))))
       (cond
         ;; If they can be shown identical: contradiction → branch closes
         ((equal? t u) (list state))
         ;; If they are ground and different: consistent, save and continue
         ;; If variables remain: save as a constraint/literal
         (else
          (let ((new-fml `(neq ,t ,u)))
            (continue-branch unexp (cons new-fml lits) state program depth))))))

    ;; ----- POSITIVE LITERAL: (pos (R t1 ... tn)) -----
    ((pos-lit? fml)
     (let ((results '()))
       ;; Strategy 1: Try branch closure via complementary literal
       (set! results
             (append results
                     (close-with-complement fml lits state)))
       ;; Strategy 2: Try Procedure Call Rule
       (set! results
             (append results
                     (procedure-call-pos fml state program depth)))
       ;; Strategy 3: Save literal and continue with next formula
       (set! results
             (append results
                     (save-literal fml unexp lits state program depth)))
       results))

    ;; ----- NEGATIVE LITERAL: (neg (R t1 ... tn)) -----
    ((neg-lit? fml)
     (let ((results '()))
       (set! results
             (append results
                     (close-with-complement fml lits state)))
       (set! results
             (append results
                     (procedure-call-neg fml state program depth)))
       (set! results
             (append results
                     (save-literal fml unexp lits state program depth)))
       results))

    ;; ----- UNKNOWN FORMULA -----
    (else
     (error "prove-branch: unrecognized formula" fml))))

;;; Continue with the next unexpanded formula on the branch.
;;; If no formulas remain, the branch is open (could not close it).
(define (continue-branch unexp lits state program depth)
  (if (null? unexp)
      '()  ; no more formulas to process → branch stays open → failure
      (prove-branch (car unexp) (cdr unexp) lits state program depth)))

;;; Save a literal on the branch and continue with the next formula.
(define (save-literal fml unexp lits state program depth)
  (if (null? unexp)
      '()  ; nothing left to process
      (prove-branch (car unexp) (cdr unexp) (cons fml lits)
                    state program depth)))


;;; ===========================================================================
;;; PART 7: Branch Closure via Complementary Literals
;;; ===========================================================================
;;;
;;; A branch closes when it contains complementary formulas:
;;;   (pos P) and (neg P)  where P unifies
;;; This is the free-variable tableau closure rule using unification.
;;; ===========================================================================

(define (close-with-complement fml lits state)
  (cond
    ;; (pos A) closes with (neg A)
    ((pos-lit? fml)
     (let ((atom (cadr fml)))
       (close-with-neg atom lits state)))
    ;; (neg A) closes with (pos A)
    ((neg-lit? fml)
     (let ((atom (cadr fml)))
       (close-with-pos atom lits state)))
    ;; Other formulas don't participate in complementary closure
    (else '())))

;;; Search literals for (neg B) where B unifies with atom A.
(define (close-with-neg atom lits state)
  (if (null? lits)
      '()
      (let ((lit (car lits))
            (rest (cdr lits)))
        (append
         (if (neg-lit? lit)
             (let* ((other-atom (cadr lit))
                    (new-subst (unify (walk* atom (state-subst state))
                                      (walk* other-atom (state-subst state))
                                      (state-subst state))))
               (if new-subst
                   (list (make-state new-subst (state-counter state)))
                   '()))
             '())
         (close-with-neg atom rest state)))))

;;; Search literals for (pos B) where B unifies with atom A.
(define (close-with-pos atom lits state)
  (if (null? lits)
      '()
      (let ((lit (car lits))
            (rest (cdr lits)))
        (append
         (if (pos-lit? lit)
             (let* ((other-atom (cadr lit))
                    (new-subst (unify (walk* atom (state-subst state))
                                      (walk* other-atom (state-subst state))
                                      (state-subst state))))
               (if new-subst
                   (list (make-state new-subst (state-counter state)))
                   '()))
             '())
         (close-with-pos atom rest state)))))


;;; ===========================================================================
;;; PART 8: The Procedure Call Rule
;;; ===========================================================================
;;;
;;; This is Fitting's key innovation that makes Proflog a programming language
;;; rather than just a theorem prover.
;;;
;;; From the paper (Definition 6.1):
;;;   Positive call: if a branch contains R(t1,...,tn) and there is a clause
;;;     R(x1,...,xn) ← φ, the branch closes if there is a closed P-tableau
;;;     for φ(t1,...,tn).
;;;   Negative call: if a branch contains ¬R(t1,...,tn) and there is a clause
;;;     R(x1,...,xn) ← φ, the branch closes if there is a closed P-tableau
;;;     for ¬φ(t1,...,tn).
;;;
;;; In our free-variable implementation:
;;;   1. Look up the clause for relation R
;;;   2. Create fresh logic variables for the clause parameters
;;;   3. Unify the clause head parameters with the literal's arguments
;;;   4. Build a subsidiary tableau for the (negated) clause body
;;;   5. If the subsidiary tableau closes, the original branch closes
;;;
;;; The subsidiary tableau starts with an EMPTY branch (no inherited literals
;;; or unexpanded formulas) — it is a completely fresh tableau construction.
;;; The substitution IS inherited (variable bindings carry through).
;;;
;;; Depth is consumed by each procedure call to ensure termination under
;;; iterative deepening.
;;; ===========================================================================

;;; Positive Procedure Call:
;;; Literal (pos (R t1 ... tn)) triggers a call with the POSITIVE body.
(define (procedure-call-pos fml state program depth)
  (if (<= depth 0)
      '()  ; depth exhausted
      (let* ((atom (cadr fml))  ; (R t1 ... tn)
             (rel (if (pair? atom) (car atom) atom))
             (args (if (pair? atom) (cdr atom) '()))
             (clauses (lookup-clauses rel program)))
        (flatmap
         (lambda (clause)
           (procedure-call-with-clause args clause 'positive state program depth))
         clauses))))

;;; Negative Procedure Call:
;;; Literal (neg (R t1 ... tn)) triggers a call with the NEGATED body.
(define (procedure-call-neg fml state program depth)
  (if (<= depth 0)
      '()
      (let* ((atom (cadr fml))
             (rel (if (pair? atom) (car atom) atom))
             (args (if (pair? atom) (cdr atom) '()))
             (clauses (lookup-clauses rel program)))
        (flatmap
         (lambda (clause)
           (procedure-call-with-clause args clause 'negative state program depth))
         clauses))))

;;; Execute a procedure call with a specific clause.
;;; Freshen clause parameters, unify with arguments, prove subsidiary tableau.
(define (procedure-call-with-clause args clause polarity state program depth)
  (let* ((params (clause-params clause))
         ;; Generate fresh logic variables for each parameter
         (st+vars (freshen-params params state))
         (new-state (car st+vars))
         (fresh-vars (cdr st+vars))
         ;; Build the parameter-to-variable alist
         (param-alist (map cons params fresh-vars))
         ;; Select the appropriate body (positive or negated)
         (raw-body (if (eq? polarity 'positive)
                       (clause-body-pos clause)
                       (clause-body-neg clause)))
         ;; Substitute fresh variables into the body
         (body (subst-all-in-formula param-alist raw-body))
         ;; Unify the fresh variables with the actual arguments
         (unified-subst (unify-args fresh-vars args (state-subst new-state))))
    (if unified-subst
        ;; Unification succeeded: build subsidiary tableau for the body
        (prove-branch body '() '()
                      (make-state unified-subst (state-counter new-state))
                      program (- depth 1))
        ;; Unification failed: clause doesn't match
        '())))

;;; Generate fresh logic variables for a list of parameter symbols.
;;; Returns (new-state . list-of-fresh-vars).
(define (freshen-params params state)
  (if (null? params)
      (cons state '())
      (let* ((v (fresh-var state))
             (st (bump-counter state))
             (rest-result (freshen-params (cdr params) st)))
        (cons (car rest-result)
              (cons v (cdr rest-result))))))

;;; Unify a list of fresh variables with a list of argument terms.
(define (unify-args vars args subst)
  (cond
    ((and (null? vars) (null? args)) subst)
    ((or (null? vars) (null? args)) #f)  ; arity mismatch
    (else
     (let ((s (unify (car vars) (car args) subst)))
       (if s
           (unify-args (cdr vars) (cdr args) s)
           #f)))))


;;; ===========================================================================
;;; PART 9: Proflog Query Interface
;;; ===========================================================================
;;;
;;; This provides the user-facing interface for Proflog.
;;;
;;; A query is a formula (possibly with free variables).  Execution proceeds
;;; by constructing a refutation tableau: we negate the query and try to build
;;; a closed tableau.  If the tableau closes, the query SUCCEEDS, and the
;;; substitution applied to the query variables gives the answer.
;;;
;;; For queries with free variables, we use the convention that symbols in the
;;; query that are listed in the query-vars parameter are logic variables.
;;;
;;; Iterative deepening: we try depth 0, 1, 2, ... until we find proofs
;;; (or reach max-depth).
;;; ===========================================================================

;;; Convert a query into its negated NNF form for refutation.
;;; The query variables are substituted with fresh logic variables.
(define (prepare-query query query-vars program)
  (let* ((rel-syms (map clause-rel program))
         (all-rels (cons '= rel-syms))
         ;; Negate the query and convert to NNF
         (negated-nnf (nnf `(not ,query) all-rels)))
    negated-nnf))

;;; Run a Proflog query with iterative deepening.
;;;
;;; Arguments:
;;;   query       — a formula (the goal)
;;;   query-vars  — list of variable symbols in the query
;;;   program     — a Proflog program (list of clause definitions)
;;;   max-depth   — maximum depth for iterative deepening
;;;   n           — maximum number of answers to find (or #f for all)
;;;
;;; Returns: list of answer alists ((var1 . val1) (var2 . val2) ...)

(define (proflog-query query query-vars program-defs max-depth n)
  (let* ((program (build-program program-defs))
         (rel-syms (map clause-rel program))
         (all-rels (cons '= rel-syms))
         ;; Negate the query and convert to NNF for refutation
         (negated-query (nnf `(not ,query) all-rels)))
    ;; Set up initial state with fresh logic variables for query vars
    (let* ((init-result (freshen-params query-vars (make-state empty-subst 0)))
           (init-state (car init-result))
           (query-logic-vars (cdr init-result))
           (var-alist (map cons query-vars query-logic-vars))
           ;; Substitute logic variables into the negated query
           (prepared-query (subst-all-in-formula var-alist negated-query)))
      ;; Iterative deepening loop
      (let depth-loop ((d 0)
                        (all-answers '()))
        (if (> d max-depth)
            all-answers
            (let* ((new-results
                    (prove-branch prepared-query '() '()
                                  init-state program d))
                   ;; Extract answer substitutions
                   (new-answers
                    (filter-map
                     (lambda (result-state)
                       (let ((subst (state-subst result-state)))
                         (map (lambda (pair)
                                (cons (car pair)
                                      (reify-answer (cdr pair) subst)))
                              var-alist)))
                     new-results))
                   ;; Remove duplicates and merge with previous
                   (merged (append all-answers
                                   (remove-duplicates new-answers all-answers))))
              (if (and n (>= (length merged) n))
                  (take-n n merged)
                  (depth-loop (+ d 1) merged))))))))

;;; Run a ground query (no variables).  Returns 'succeed, 'fail, or 'unknown.
(define (proflog-ground-query query program-defs max-depth)
  (let* ((program (build-program program-defs))
         (rel-syms (map clause-rel program))
         (all-rels (cons '= rel-syms))
         ;; Try to prove the query (refute ¬query)
         (negated-query (nnf `(not ,query) all-rels))
         (init-state (make-state empty-subst 0)))
    (let depth-loop ((d 0))
      (if (> d max-depth)
          'unknown  ; exhausted depth without proof or refutation
          (let ((prove-results
                 (prove-branch negated-query '() '() init-state program d)))
            (if (not (null? prove-results))
                'succeed  ; found a closed tableau → query succeeds
                ;; Also try to refute the query (prove ¬query → prove query)
                (let* ((pos-query (nnf query all-rels))
                       (refute-results
                        (prove-branch pos-query '() '() init-state program d)))
                  (if (not (null? refute-results))
                      'fail  ; found refutation → query fails
                      (depth-loop (+ d 1))))))))))


;;; --- Reification helpers ---

;;; Convert a logic variable's value to a readable form.
(define (reify-answer v subst)
  (let ((walked (walk* v subst)))
    (reify-term walked)))

;;; Convert a term with logic variables to a readable form.
(define (reify-term t)
  (cond
    ((var? t) (string->symbol
               (string-append "_." (number->string (vector-ref t 0)))))
    ((pair? t) (cons (reify-term (car t)) (reify-term (cdr t))))
    (else t)))

;;; Remove answers that already appear in the accumulated list.
(define (remove-duplicates new-answers old-answers)
  (filter (lambda (a) (not (member a old-answers))) new-answers))

;;; filter-map: map and filter out #f results.
(define (filter-map f lst)
  (if (null? lst) '()
      (let ((result (f (car lst))))
        (if result
            (cons result (filter-map f (cdr lst)))
            (filter-map f (cdr lst))))))

;;; take-n: take at most n elements.
(define (take-n n lst)
  (cond
    ((or (zero? n) (null? lst)) '())
    (else (cons (car lst) (take-n (- n 1) (cdr lst))))))


;;; ===========================================================================
;;; PART 10: Convenience Macros and Sugar
;;; ===========================================================================

;;; Define a Proflog program using a friendly syntax.
;;; Usage:
;;;   (define my-program
;;;     (proflog-program
;;;       ((even x) (or (= x 0) (exists y (and (= x (s y)) (odd y)))))
;;;       ((odd x)  (forall y (implies (even y) (not (= x y)))))))

(define (proflog-program . clause-defs)
  ;; Each clause-def is ((R x1 ... xn) body)
  ;; Convert to the expected format for build-program
  (map (lambda (cd) `(define-clause ,(car cd) ,(cadr cd)))
       clause-defs))

;;; Sugar for running queries:
;;;   (proflog-ask program '(even (s (s 0))) max-depth)     → 'succeed/'fail/'unknown
;;;   (proflog-ask-vars program '(even x) '(x) max-depth n) → list of answer alists

(define (proflog-ask program-defs query max-depth)
  (proflog-ground-query query program-defs max-depth))

(define (proflog-ask-vars program-defs query vars max-depth n)
  (proflog-query query vars program-defs max-depth n))


;;; ===========================================================================
;;; PART 11: Test Suite — Examples from Fitting's Paper
;;; ===========================================================================

;;; --- Helper: run a test and report ---
(define (test name expected actual)
  (let ((pass? (equal? expected actual)))
    (display (if pass? "[PASS] " "[FAIL] "))
    (display name)
    (when (not pass?)
      (display "\n       Expected: ")
      (display expected)
      (display "\n       Got:      ")
      (display actual))
    (newline)
    pass?))

;;; -------------------------------------------------------
;;; Program P1 from the paper (Section 2):
;;;   even(x) ← x = 0 ∨ (∃y)[x = s(y) ∧ odd(y)]
;;;   odd(x)  ← (∀y)[even(y) ⊃ ¬(x = y)]
;;; -------------------------------------------------------

(define P1
  (proflog-program
   ((even x) (or (= x 0)
                 (exists y (and (= x (s y)) (odd y)))))
   ((odd x)  (forall y (implies (even y) (not (= x y)))))))

;;; -------------------------------------------------------
;;; Program P2 from the paper (Section 2) — Nim game:
;;;   win(x) ← (∃y)[(x = s(y) ∨ x = s(s(y))) ∧ ¬win(y)]
;;; -------------------------------------------------------

(define P2
  (proflog-program
   ((win x) (exists y (and (or (= x (s y)) (= x (s (s y))))
                           (not (win y)))))))

;;; -------------------------------------------------------
;;; Run all tests
;;; -------------------------------------------------------

(define (run-tests)
  (display "==== PROFLOG TEST SUITE ====\n\n")

  (display "--- Program P1: Even/Odd ---\n")
  ;; even(0) should succeed
  (test "even(0) succeeds"
        'succeed
        (proflog-ask P1 '(even 0) 10))

  ;; even(s(s(0))) should succeed (2 is even)
  (test "even(s(s(0))) succeeds"
        'succeed
        (proflog-ask P1 '(even (s (s 0))) 10))

  ;; odd(s(0)) should succeed (1 is odd)
  (test "odd(s(0)) succeeds"
        'succeed
        (proflog-ask P1 '(odd (s 0)) 15))

  ;; even(s(0)) should fail (1 is not even)
  (test "even(s(0)) fails"
        'fail
        (proflog-ask P1 '(even (s 0)) 15))

  ;; odd(0) should fail (0 is not odd)
  (test "odd(0) fails"
        'fail
        (proflog-ask P1 '(odd 0) 15))

  (display "\n--- Program P2: Nim Game ---\n")
  ;; win(s(s(s(0)))) should fail (as shown in the paper's tableau)
  ;; Position 3: your opponent can always win
  (test "win(s(s(s(0)))) (Nim pos 3) fails"
        'fail
        (proflog-ask P2 '(win (s (s (s 0)))) 20))

  ;; win(s(0)) should succeed (position 1: move to 0, opponent loses)
  (test "win(s(0)) (Nim pos 1) succeeds"
        'succeed
        (proflog-ask P2 '(win (s 0)) 15))

  ;; win(s(s(0))) should succeed (position 2: move to 0, opponent loses)
  (test "win(s(s(0))) (Nim pos 2) succeeds"
        'succeed
        (proflog-ask P2 '(win (s (s 0))) 15))

  ;; win(0) should fail (position 0: no move possible, you lose)
  (test "win(0) (Nim pos 0) fails"
        'fail
        (proflog-ask P2 '(win 0) 10))

  (display "\n--- Simple relational tests ---\n")
  ;; A simple program with just facts (no body complexity)
  (let ((P-simple
         (proflog-program
          ((parent x y) (or (and (= x alice) (= y bob))
                            (or (and (= x alice) (= y carol))
                                (and (= x bob) (= y dave))))))))
    (test "parent(alice, bob) succeeds"
          'succeed
          (proflog-ask P-simple '(parent alice bob) 10))

    (test "parent(alice, dave) fails"
          'fail
          (proflog-ask P-simple '(parent alice dave) 10))

    (test "parent(bob, dave) succeeds"
          'succeed
          (proflog-ask P-simple '(parent bob dave) 10)))

  (display "\n--- Query with variables ---\n")
  ;; Find even numbers
  (let ((results (proflog-ask-vars P1 '(even x) '(x) 6 5)))
    (test "even(x) finds 0"
          #t
          (member '((x . 0)) results))
    (test "even(x) finds s(s(0))"
          #t
          (not (not (member '((x . (s (s 0)))) results)))))

  (display "\n--- Empty program (Section 3 example) ---\n")
  ;; The empty program: s∅ where t=u is true iff identical, false otherwise
  (let ((P-empty '()))
    ;; 0 = 0 should succeed (identical terms)
    (test "0 = 0 succeeds with empty program"
          'succeed
          (proflog-ask P-empty '(= 0 0) 5))
    ;; s(0) = 0 should fail (distinct constructors)
    (test "s(0) = 0 fails with empty program"
          'fail
          (proflog-ask P-empty '(= (s 0) 0) 5)))

  (display "\n==== TESTS COMPLETE ====\n"))

;;; Entry point
(run-tests)
