;; ============================================================================
;; αleanTAP-EP: A Declarative Logic Programming Language
;;              based on Tableau Methods, with Equality
;; ============================================================================
;;
;; This extends αleanTAP-E with Fitting's Procedure Call Rule, transforming
;; the theorem prover into a logic programming language in the style of
;; Proflog (Fitting, "Tableaux for Logic Programming", J. Automated
;; Reasoning 13, 1994).
;;
;; THE KEY IDEA (from Fitting's paper, Section 6):
;;
;;   In a standard tableau prover, a branch closes when it contains
;;   complementary literals A and ¬A.  Fitting's insight is to add a
;;   second way to close a branch: by *calling a program definition*.
;;
;;   A "program" P is a set of clauses  R(x₁,...,xₙ) ← φ(x₁,...,xₙ)
;;   where φ can be ANY first-order formula (not just Horn clauses!).
;;   The semantics is biconditional: R(t) holds iff φ(t) holds.
;;
;;   The PROCEDURE CALL RULE says a branch is closed if:
;;
;;     (1) It contains a positive atom R(t₁,...,tₙ) of L, there is a
;;         clause R(x) ← φ(x) in P, and there exists a closed P-tableau
;;         for φ(t₁,...,tₙ).
;;
;;         [R(t) is asserted, but φ(t) is unsatisfiable, contradicting
;;          the definition R(t) ↔ φ(t).]
;;
;;     (2) It contains a negated atom ¬R(t₁,...,tₙ) of L, there is a
;;         clause R(x) ← φ(x) in P, and there exists a closed P-tableau
;;         for ¬φ(t₁,...,tₙ).
;;
;;         [¬R(t) is asserted, but φ(t) is valid, contradicting
;;          the definition R(t) ↔ φ(t).]
;;
;;   Each procedure call spawns a SUBSIDIARY TABLEAU — a completely fresh
;;   proof obligation with its own branches, literals, and expansion
;;   stack.  The subsidiary tableau can itself invoke procedure calls,
;;   giving us recursion.
;;
;; WHY THIS MATTERS:
;;
;;   αleanTAP is already a pure relation (runs forwards and backwards).
;;   Adding Proflog-style procedure calls to a relational prover gives
;;   capabilities beyond either Prolog or standard theorem proving:
;;
;;   - Forward:  Given program + query, compute whether query succeeds
;;   - Backward: Given program + partial query, GENERATE succeeding queries
;;   - Sideways: Given partial program + query, SYNTHESIZE clause bodies
;;
;;   This is, to our knowledge, the first implementation of Proflog,
;;   and it inherits αleanTAP's declarative flexibility.
;;
;; FORMULA GRAMMAR (extended with existential quantifier):
;;
;;   Fml  → (and Fml Fml) | (or Fml Fml)
;;        | (forall (tie Nom Fml)) | (exists (tie Nom Fml))
;;        | Lit
;;   Lit  → (pos Term) | (neg Term) | (eq Term Term) | (neq Term Term)
;;   Term → (var Nom) | (app Symbol Term*)
;;
;; PROGRAM CLAUSE:
;;
;;   Clause → [Symbol [Nom ...] Fml]
;;
;;   A clause is a vector: [rel-symbol [param-noms...] body-formula]
;;   The noms in the params list are formal parameters.  The body
;;   uses (var nomᵢ) to reference them.
;;
;;   Example:
;;     ['even [a]
;;      (or (eq (var a) (app zero))
;;          (exists (tie b (and (eq (var a) (app s (var b)))
;;                              (pos (app odd (var b)))))))]
;;
;; PROOF STEPS (new):
;;   (proc-call R . prf)      — positive procedure call on relation R
;;   (neg-proc-call R . prf)  — negative procedure call on relation R
;;   (subst-call R . prf)     — substitutivity-augmented positive proc call
;;   (neg-subst-call R . prf) — substitutivity-augmented negative proc call
;;   (witness . prf)          — existential witness introduction (δ rule)
;;   (free-close)             — free closure: distinct constructors clash
;;   (eq-neq-close)           — eq/neq complementary closure
;;   (decompose . prf)        — injectivity: decompose same-head eq into sub-eqs
;;   (para-free-close)        — paramodulated free closure: rewrite one side of eq via branch eqs to clash
;;   (eq-refl-close)          — neq closed via one-one derived equalities
;;
;; ============================================================================

(ns cljtap.alphaleantap-ep
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer :all :rename {appendo logic-appendo, membero logic-membero}]
            [clojure.core.logic.nominal :refer [tie hash]]))

;; In core.logic.nominal 1.0.1, `nom` is a constructor function, not a macro.
;; We define `nom` as a macro that introduces fresh nominal bindings using the
;; nominal namespace's `fresh` macro, which creates Nom objects (rigid names).
;;
;;   (nom a goal)         — introduce fresh nom `a`, then run goal
;;   (nom a b c goal)     — introduce fresh noms `a`, `b`, `c`, then run goal
;;   (nom a (nom p goal)) — nesting also works (expands inside-out)
(defmacro nom [& args]
  (let [syms (butlast args)
        body (last args)]
    `(clojure.core.logic.nominal/fresh [~@syms] ~body)))

;; ============================================================================
;; Part 1: Core Helper Relations
;; ============================================================================

(defn lookupo
  "Look up the value associated with nom `a` in environment `env`."
  [a env out]
  (fresh [first rest]
    (conde
      [(== (lcons [a out] rest) env)]
      [(== (lcons first rest) env)
       (lookupo a rest out)])))

(defn appendo
  "Relational list append."
  [ls s out]
  (conde
    [(== '() ls) (== s out)]
    [(fresh [a d r]
       (== (lcons a d) ls)
       (== (lcons a r) out)
       (appendo d s r))]))

(defn membero
  "Relational membership check using sound unification."
  [x ls]
  (fresh [a d]
    (== (lcons a d) ls)
    (conde
      [(== a x)]
      [(membero x d)])))

;; ============================================================================
;; Part 2: Substitution Relations
;; ============================================================================

(declare subst-term*o)

(defn subst-termo
  "Substitute values for tagged noms (var a) in a term, using environment."
  [fml env out]
  (conde
    [(fresh [a]
       (== ['var a] fml)
       (lookupo a env out))]
    [(fresh [f d r]
       (== (lcons 'app (lcons f d)) fml)
       (== (lcons 'app (lcons f r)) out)
       (subst-term*o d env r))]))

(defn subst-term*o
  "Substitute across a list of terms."
  [tm* env out]
  (conde
    [(== '() tm*) (== '() out)]
    [(fresh [e1 e2 r1 r2]
       (== (lcons e1 e2) tm*)
       (== (lcons r1 r2) out)
       (subst-termo e1 env r1)
       (subst-term*o e2 env r2))]))

(defn subst-lito
  "Substitute in a literal: (pos t), (neg t), (eq t1 t2), (neq t1 t2)."
  [fml env out]
  (conde
    [(fresh [l r]
       (== ['pos l] fml)
       (== ['pos r] out)
       (subst-termo l env r))]
    [(fresh [l r]
       (== ['neg l] fml)
       (== ['neg r] out)
       (subst-termo l env r))]
    [(fresh [l1 l2 r1 r2]
       (== ['eq l1 l2] fml)
       (== ['eq r1 r2] out)
       (subst-termo l1 env r1)
       (subst-termo l2 env r2))]
    [(fresh [l1 l2 r1 r2]
       (== ['neq l1 l2] fml)
       (== ['neq r1 r2] out)
       (subst-termo l1 env r1)
       (subst-termo l2 env r2))]))

;; ============================================================================
;; Part 3: Equality Reasoning — Free Closure, Injectivity, Paramodulation
;; ============================================================================
;;
;; Extended from αleanTAP-E with Fitting's Section 5 rules for weak
;; Herbrand models.  In a weak Herbrand model, function symbols are
;; interpreted as FREE constructors:
;;
;;   (i)   DISJOINTNESS: If f ≠ g, then f(t₁,...,tₙ) ≠ g(s₁,...,sₘ)
;;         for all terms t, s.  Distinct constructors have disjoint ranges.
;;
;;   (ii)  INJECTIVITY (One-One):  f(t₁,...,tₙ) = f(s₁,...,sₙ) implies
;;         t₁ = s₁ ∧ ... ∧ tₙ = sₙ.  Constructors are one-to-one.
;;
;; These give us three new capabilities in the prover:
;;
;;   - FREE CLOSURE (clash): A literal (eq (app f ...) (app g ...))
;;     with f ≠ g closes the branch immediately.
;;
;;   - ONE-ONE DECOMPOSITION: An eq literal with the same head yields
;;     pairwise sub-equalities that are injected into the paramodulation
;;     machinery, enabling rewriting with derived equalities.
;;
;;   - SUBSTITUTIVITY FOR PROC CALLS: Branch equalities (including
;;     one-one derived ones) can rewrite a literal's arguments before
;;     firing a procedure call, bridging the gap between rigid δ-rule
;;     parameters and concrete constructor terms.
;;
;; Together these make Proflog programs with constructors (zero/s,
;; nil/cons, etc.) work correctly — the "expected limitation" is gone.

;; --- 3a. Free Closure ---

(defn free-closureo
  "Succeed if (eq t1 t2) is unsatisfiable in any weak Herbrand model.
   Two terms with different head constructor symbols can never be equal.
   
   SOUNDNESS GUARD: both heads must be genuine Clojure symbols (not noms).
   A nom (δ-parameter) represents an arbitrary domain element and could
   denote any term, so (eq (app p) (app s x)) must NOT be treated as
   a clash — p might equal s(x) in some model."
  [t1 t2]
  (fresh [s1 s2 a1 a2]
    (== (lcons 'app (lcons s1 a1)) t1)
    (== (lcons 'app (lcons s2 a2)) t2)
    (project [s1 s2]
      (if (and (symbol? s1) (symbol? s2) (not= s1 s2))
        succeed
        fail))))

;; --- 3b. One-One Decomposition ---

(defn one-one-pairso
  "Given two argument lists from same-head terms, produce pairwise
   equality pairs (both directions) for use by the paramodulation engine.
   
   E.g., (app s (app zero)) = (app s (app p))
     args1 = [(app zero)],  args2 = [(app p)]
     pairs = [[(app zero) (app p)] [(app p) (app zero)]]"
  [args1 args2 pairs]
  (conde
    [(== '() args1) (== '() args2) (== '() pairs)]
    [(fresh [t u r1 r2 rest-pairs]
       (== (lcons t r1) args1)
       (== (lcons u r2) args2)
       (== (lcons [t u] (lcons [u t] rest-pairs)) pairs)
       (one-one-pairso r1 r2 rest-pairs))]))

;; --- 3c. Equality Decomposition (for injectivity expansion rule) ---
;;
;; Builds a conjunction of sub-equalities from paired argument lists.
;; Used by the decomposition rule in proveo to expand:
;;   (eq (app f t₁…tₙ) (app f s₁…sₙ)) → (and (eq t₁ s₁) … (eq tₙ sₙ))

(defn decompose-eq-argso
  "Build a conjunction formula of pairwise sub-equalities.
   
   [t₁]       [s₁]       → (eq t₁ s₁)
   [t₁ t₂]    [s₁ s₂]    → (and (eq t₁ s₁) (eq t₂ s₂))
   [t₁ t₂ t₃] [s₁ s₂ s₃] → (and (eq t₁ s₁) (and (eq t₂ s₂) (eq t₃ s₃)))
   
   Fails on empty argument lists (handled by refl-close at the eq level)
   and on mismatched arities (which is itself a free closure violation)."
  [args1 args2 result]
  (conde
    ;; Base case: single argument pair
    [(fresh [a1 a2]
       (== (lcons a1 '()) args1)
       (== (lcons a2 '()) args2)
       (== ['eq a1 a2] result))]
    ;; Recursive: first pair, then conjunction with rest
    [(fresh [a1 a2 r1 r2 rest-eq]
       (== (lcons a1 r1) args1)
       (== (lcons a2 r2) args2)
       ;; Ensure r1 is non-empty (otherwise base case matches)
       (fresh [_ __]
         (== (lcons _ __) r1))
       (decompose-eq-argso r1 r2 rest-eq)
       (== ['and ['eq a1 a2] rest-eq] result))]))

;; --- 3d. Collect equalities (with one-one decomposition) ---

(defn collect-eqso
  "Collect all usable equality pairs from branch literals.
   
   For each (eq t1 t2):
     - Always yields [t1 t2] and [t2 t1]                (top-level)
     - If same head: also yields [arg_i arg_j] pairs     (one-one rule)
   
   The one-one derived pairs are interleaved into the equality list
   so that the paramodulation machinery (eq-membero, rewrite-term-with-eqso)
   can use them for rewriting without any special-casing."
  [lits eqs]
  (conde
    [(== '() lits) (== '() eqs)]
    ;; Equality literal: extract top-level + one-one decomposed pairs
    [(fresh [l1 l2 rest-lits rest-eqs mid-eqs]
       (== (lcons ['eq l1 l2] rest-lits) lits)
       (collect-eqso rest-lits rest-eqs)
       ;; Top-level pair (both directions)
       (== (lcons [l1 l2] (lcons [l2 l1] mid-eqs)) eqs)
       (conde
         ;; Same head constructor: also inject one-one pairs
         [(fresh [f a1 a2 oo-pairs]
            (== (lcons 'app (lcons f a1)) l1)
            (== (lcons 'app (lcons f a2)) l2)
            (one-one-pairso a1 a2 oo-pairs)
            (appendo oo-pairs rest-eqs mid-eqs))]
         ;; Different heads or non-decomposable: just the rest
         [(== mid-eqs rest-eqs)]))]
    ;; Non-equality literal: skip
    [(fresh [lit rest-lits]
       (== (lcons lit rest-lits) lits)
       (fresh [tag _]
         (conde
           [(== ['pos tag] lit)]
           [(== ['neg tag] lit)]
           [(== ['neq tag _] lit)])
         (collect-eqso rest-lits eqs)))]))

;; --- 3e. Rewriting (paramodulation engine) ---

(declare rewrite-term*o)

(defn rewrite-termo [t lhs rhs out]
  (conde
    [(== t lhs) (== out rhs)]
    [(fresh [f args args-out]
       (== (lcons 'app (lcons f args)) t)
       (== (lcons 'app (lcons f args-out)) out)
       (rewrite-term*o args lhs rhs args-out))]))

(defn rewrite-term*o [terms lhs rhs out]
  (fresh [t1 rest r1 rest-out]
    (== (lcons t1 rest) terms)
    (conde
      [(rewrite-termo t1 lhs rhs r1)
       (== (lcons r1 rest) out)]
      [(== (lcons t1 rest-out) out)
       (rewrite-term*o rest lhs rhs rest-out)])))

(defn rewrite-lito [lit lhs rhs out]
  (conde
    [(fresh [t t-out]
       (== ['pos t] lit)
       (== ['pos t-out] out)
       (rewrite-termo t lhs rhs t-out))]
    [(fresh [t t-out]
       (== ['neg t] lit)
       (== ['neg t-out] out)
       (rewrite-termo t lhs rhs t-out))]))

;; --- 3f. Equality-aware membership (for closure) ---

(defn selecto
  "Non-deterministically select an element `x` from `lst`, with `rest`
   being `lst` with that one occurrence of `x` removed.
   Purely relational — uses only == and conde, no disequality constraints."
  [x lst rest]
  (conde
    [(fresh [t]
       (== (lcons x t) lst)
       (== t rest))]
    [(fresh [h t r]
       (== (lcons h t) lst)
       (== (lcons h r) rest)
       (selecto x t r))]))

(defn eq-membero
  "Check if `neg` (a literal) can be found in `lits` after zero or more
   rewrite steps using equality pairs from `remaining`.

   Non-deterministic: tries EVERY equality pair at each step via selecto,
   enabling multi-step rewriting chains (e.g., a→b→c via transitivity).

   Each equality pair is used at most once per chain (selecto removes the
   chosen pair from `remaining`), guaranteeing termination on cyclic equality
   sets without an arbitrary step limit."
  [neg lits remaining]
  (conde
    [(membero neg lits)]
    [(fresh [pair lhs rhs neg-rewritten rest]
       (selecto pair remaining rest)
       (== [lhs rhs] pair)
       (rewrite-lito neg lhs rhs neg-rewritten)
       (eq-membero neg-rewritten lits rest))]))

;; --- 3g. Substitutivity for terms (multi-argument) ---
;;
;; Rewrites one or more arguments of a term using equality pairs from
;; the collected set.  Each argument is independently rewritten (or not)
;; using a possibly different equality pair.
;;
;; This is critical for multi-argument constructors and relations:
;;   Branch has (eq (app zero) (app p1)) and (eq (app nil) (app p2))
;;   Literal: (pos (app member (app p1) (app p2)))
;;   Rewrite: p1 → zero, p2 → nil (independently)
;;   Proc call fires on member(zero, nil) instead of member(p1, p2)
;;
;; The `someo` variant guarantees at least one argument is actually
;; rewritten, preventing overlap with the plain procedure call rule.

(defn rewrite-args-maybeo
  "Rewrite zero or more arguments using equality pairs from `eqs`.
   Each argument is independently either kept or rewritten using one pair."
  [args eqs new-args]
  (conde
    [(== '() args) (== '() new-args)]
    [(fresh [a rest a-out rest-out]
       (== (lcons a rest) args)
       (== (lcons a-out rest-out) new-args)
       (conde
         ;; Rewrite this arg using one equality pair
         [(fresh [pair lhs rhs]
            (membero pair eqs)
            (== [lhs rhs] pair)
            (rewrite-termo a lhs rhs a-out))]
         ;; Keep this arg unchanged
         [(== a a-out)])
       (rewrite-args-maybeo rest eqs rest-out))]))

(defn rewrite-args-someo
  "Rewrite one or more arguments using equality pairs from `eqs`.
   Guarantees at least one argument is rewritten — if the current arg
   is kept unchanged, at least one later arg must be rewritten."
  [args eqs new-args]
  (fresh [a rest a-out rest-out]
    (== (lcons a rest) args)
    (== (lcons a-out rest-out) new-args)
    (conde
      ;; Rewrite this arg; rest get zero-or-more rewrites
      [(fresh [pair lhs rhs]
         (membero pair eqs)
         (== [lhs rhs] pair)
         (rewrite-termo a lhs rhs a-out))
       (rewrite-args-maybeo rest eqs rest-out)]
      ;; Keep this arg; must still rewrite at least one later
      [(== a a-out)
       (rewrite-args-someo rest eqs rest-out)])))

(defn rewrite-term-with-eqso
  "Rewrite term `t` by independently rewriting one or more of its
   arguments using equality pairs from `eqs`.
   
   Each argument position can use a different equality pair, enabling
   multi-argument terms like R(p₁, p₂) to have both arguments
   rewritten simultaneously.  At least one argument must be rewritten
   (guaranteed by rewrite-args-someo), preventing overlap with the
   plain procedure call rules."
  [t eqs out]
  (fresh [f args new-args]
    (== (lcons 'app (lcons f args)) t)
    (== (lcons 'app (lcons f new-args)) out)
    (rewrite-args-someo args eqs new-args)))

(defn eq-neq-closeo
  "Multi-step neq closure: rewrite t1 toward t2 using ONE OR MORE equality
   pairs from `eqs` until t1 becomes identical to t2.

   Requires at least one rewriting step — trivial t1==t2 reflexivity is
   handled by the refl-close rule and must not overlap here.  This
   separation prevents duplicate solutions in backward-running mode.

   This handles transitivity chains:
     Branch has a=b, b=c.  (neq a c) → rewrite a→b → rewrite b→c → (neq c c) → close.

   Also handles one-one derived pairs:
     Branch has s(a)=s(b).  (neq a b) → rewrite a→b via one-one → (neq b b) → close.

   Each equality pair is used at most once per chain (`remaining` shrinks at
   each step via selecto), guaranteeing termination on cyclic equality sets
   without an arbitrary step limit."
  [t1 t2 remaining]
  (fresh [pair lhs rhs t1-rw rest]
    (selecto pair remaining rest)
    (== [lhs rhs] pair)
    (rewrite-termo t1 lhs rhs t1-rw)
    (conde
      [(== t1-rw t2)]
      [(eq-neq-closeo t1-rw t2 rest)])))

;; --- 3h. Paramodulated free closure ---
;;
;; Detects transitive constructor clashes that require equality reasoning:
;;
;;   Branch: (eq (app a) (app p)) — saved to lits.
;;   Current eq: (eq (app b) (app p)).
;;   eqs include: [(app p), (app a)].
;;   Rewrite t2=(app p) → (app a): yields (eq (app b) (app a)).
;;   free-closureo fires: b ≠ a ✓
;;
;; Requires at least one rewriting step — direct clashes are already
;; handled by the free-close rule.  Each equality pair is used at most once
;; per chain (selecto shrinks remaining), preventing cycling.

(defn para-free-closeo
  "Paramodulated free closure: rewrite one side of (eq t1 t2) using branch
   equalities in one or more steps until the result clashes with the other
   side via free-closureo.

   Tries rewriting t1 or t2 independently at each step.  Recursion handles
   multi-step chains (e.g., p→q→a when branch has p=q and q=a).

   Each equality pair is used at most once per chain (selecto removes the
   chosen pair from `remaining`), guaranteeing termination without an
   arbitrary step limit.

   Soundness: every rewriting step uses only equalities already on the branch,
   and free-closureo's symbol? guard ensures only genuine constructor symbols
   (not δ-parameters) are treated as distinct."
  [t1 t2 remaining]
  (fresh [pair lhs rhs rest]
    (selecto pair remaining rest)
    (== [lhs rhs] pair)
    (conde
      ;; Rewrite t1 one step, then clash-check or recurse
      [(fresh [t1-rw]
         (rewrite-termo t1 lhs rhs t1-rw)
         (conde
           [(free-closureo t1-rw t2)]
           [(para-free-closeo t1-rw t2 rest)]))]
      ;; Rewrite t2 one step, then clash-check or recurse
      [(fresh [t2-rw]
         (rewrite-termo t2 lhs rhs t2-rw)
         (conde
           [(free-closureo t1 t2-rw)]
           [(para-free-closeo t1 t2-rw rest)]))])))

;; ============================================================================
;; Part 4: Formula Negation (NNF-preserving)
;; ============================================================================
;;
;; The negative procedure call (Part 2 of Fitting's rule) requires
;; computing ¬φ when the branch contains ¬R(t) and the clause is
;; R(x) ← φ(x).  We need to negate φ while staying in NNF.
;;
;; This is a pure relation: it runs both ways.

(defn negate-formulao
  "Compute the NNF negation of a formula.  Pure relation.
   
   ¬(and A B)           = (or ¬A ¬B)             — De Morgan
   ¬(or A B)            = (and ¬A ¬B)            — De Morgan
   ¬(forall (tie a P))  = (exists (tie a ¬P))    — quantifier dual
   ¬(exists (tie a P))  = (forall (tie a ¬P))    — quantifier dual
   ¬(pos t)             = (neg t)                — literal negation
   ¬(neg t)             = (pos t)
   ¬(eq t1 t2)          = (neq t1 t2)
   ¬(neq t1 t2)         = (eq t1 t2)"
  [fml neg-fml]
  (conde
    ;; Conjunction ↔ Disjunction (De Morgan)
    [(fresh [a b na nb]
       (== ['and a b] fml)
       (== ['or na nb] neg-fml)
       (negate-formulao a na)
       (negate-formulao b nb))]

    ;; Disjunction ↔ Conjunction (De Morgan)
    [(fresh [a b na nb]
       (== ['or a b] fml)
       (== ['and na nb] neg-fml)
       (negate-formulao a na)
       (negate-formulao b nb))]

    ;; Universal ↔ Existential
    [(nom a
       (fresh [body neg-body]
         (== ['forall (tie a body)] fml)
         (== ['exists (tie a neg-body)] neg-fml)
         (negate-formulao body neg-body)))]

    ;; Existential ↔ Universal
    [(nom a
       (fresh [body neg-body]
         (== ['exists (tie a body)] fml)
         (== ['forall (tie a neg-body)] neg-fml)
         (negate-formulao body neg-body)))]

    ;; Positive literal ↔ Negative literal
    [(fresh [t]
       (== ['pos t] fml)
       (== ['neg t] neg-fml))]
    [(fresh [t]
       (== ['neg t] fml)
       (== ['pos t] neg-fml))]

    ;; Equality ↔ Disequality
    [(fresh [t1 t2]
       (== ['eq t1 t2] fml)
       (== ['neq t1 t2] neg-fml))]
    [(fresh [t1 t2]
       (== ['neq t1 t2] fml)
       (== ['eq t1 t2] neg-fml))]))

;; ============================================================================
;; Part 5: Program Clause Lookup and Instantiation
;; ============================================================================
;;
;; A Proflog program is a list of clauses.  Each clause defines one
;; relation symbol.  Fitting requires at most one clause per relation
;; (Definition 2.1), which simplifies lookup.
;;
;; Clause representation:
;;
;;   A clause is a list:  [rel-symbol [nom₁ ... nomₙ] body-formula]
;;
;;   The noms in the params list are the formal parameters.  The body
;;   uses (var nomᵢ) to reference them.
;;
;; On a procedure call for R(t₁,...,tₙ):
;;   1. Look up the clause for R
;;   2. Create an env mapping each param nom to the corresponding arg term
;;   3. Launch a subsidiary proveo with this env and the clause body

(defn lookup-clauseo
  "Find the clause for relation symbol `rel` in program `prog`.
   Yields the parameter noms and body formula."
  [rel prog params body]
  (fresh [clause rest]
    (== (lcons clause rest) prog)
    (conde
      [(== [rel params body] clause)]
      [(fresh [other-rel _params _body]
         (== [other-rel _params _body] clause)
         ;; Only proceed if other-rel is NOT rel (no overlap)
         ;; In practice, Fitting requires one clause per rel,
         ;; so we just search the list.
         (lookup-clauseo rel rest params body))])))

(defn bind-argso
  "Create an environment mapping param noms to argument terms.
   (bind-argso [a b] [t1 t2] env) => env = [[a t1] [b t2]]
   
   The params are noms from the clause definition.
   The args are substituted terms from the call site."
  [params args env]
  (conde
    [(== '() params) (== '() args) (== '() env)]
    [(fresh [p ps a as rest-env]
       (== (lcons p ps) params)
       (== (lcons a as) args)
       (== (lcons [p a] rest-env) env)
       (bind-argso ps as rest-env))]))

;; ============================================================================
;; Part 6: The Main Prover — αleanTAP-EP (proveo)
;; ============================================================================
;;
;; proveo is now extended with:
;;   - A `program` argument (the Proflog program P)
;;   - The existential quantifier rule (δ rule)
;;   - Fitting's Procedure Call Rule (positive and negative)
;;
;; Arguments:
;;   fml     — current formula being expanded
;;   unexp   — stack of unexpanded formulas on this branch
;;   lits    — literals already on this branch
;;   env     — nom → value mappings (for ∀/∃ instantiation)
;;   program — the Proflog program (list of clauses)
;;   proof   — proof term recording steps taken

(defn proveo
  [fml unexp lits env program proof]
  (conde
    ;; ================================================================
    ;; CONJUNCTION (α-rule): expand (and e1 e2)
    ;; ================================================================
    [(fresh [e1 e2 prf]
       (== ['and e1 e2] fml)
       (== (lcons 'conj prf) proof)
       (proveo e1 (lcons e2 unexp) lits env program prf))]

    ;; ================================================================
    ;; DISJUNCTION (β-rule): split (or e1 e2) into two branches
    ;; ================================================================
    [(fresh [e1 e2 prf1 prf2]
       (== ['or e1 e2] fml)
       (== ['split prf1 prf2] proof)
       (proveo e1 unexp lits env program prf1)
       (proveo e2 unexp lits env program prf2))]

    ;; ================================================================
    ;; UNIVERSAL QUANTIFIER (γ-rule): instantiate (forall (tie a body))
    ;; Generate fresh logic variable, bind a→x, re-enqueue formula
    ;; ================================================================
    [(nom a
       (fresh [x body unexp1 prf]
         (== ['forall (tie a body)] fml)
         (== (lcons 'univ prf) proof)
         (appendo unexp (list fml) unexp1)
         (proveo body unexp1 lits (lcons [a x] env) program prf)))]

    ;; ================================================================
    ;; EXISTENTIAL QUANTIFIER (δ-rule): witness (exists (tie a body))
    ;;
    ;; NEW in αleanTAP-EP.  Fitting's classical tableau δ-rule:
    ;; introduce a new parameter (Skolem constant) for the witness.
    ;;
    ;; In nominal logic, a fresh nom IS a new, globally unique name.
    ;; We create a fresh nom `p` and bind a → (app p) in env,
    ;; representing the Skolem witness as a nullary application of
    ;; the fresh parameter symbol.
    ;;
    ;; Unlike the γ-rule, we do NOT re-enqueue the formula:
    ;; an existential is used exactly once.
    ;; ================================================================
    [(nom a
       (nom p  ;; fresh parameter — the Skolem witness
         (fresh [body prf]
           (== ['exists (tie a body)] fml)
           (== (lcons 'witness prf) proof)
           (proveo body unexp lits (lcons [a ['app p]] env) program prf))))]

    ;; ================================================================
    ;; LITERAL CASES (including Free Closure & Procedure Call Rules)
    ;; ================================================================
    [(fresh [lit]
       (subst-lito fml env lit)
       (conde
         ;; ---- Standard complementary closure ----
         [(fresh [tm neg]
            (== ['close] proof)
            (conde
              [(== ['pos tm] lit) (== ['neg tm] neg)]
              [(== ['neg tm] lit) (== ['pos tm] neg)])
            (membero neg lits))]

         ;; ---- Reflexivity closure ----
         [(fresh [t]
            (== ['neq t t] lit)
            (== ['refl-close] proof))]

         ;; ============================================================
         ;; FREE CLOSURE RULE (Fitting §5 — Disjointness)
         ;; ============================================================
         ;; (eq (app f ...) (app g ...)) with f ≠ g is unsatisfiable.
         ;; Distinct constructors have disjoint ranges in weak Herbrand
         ;; models, so the equation can never hold.
         ;;
         ;; SOUNDNESS GUARD: Both f and g must be genuine constructor
         ;; symbols (Clojure symbols), NOT δ-parameters (noms).
         ;; A δ-parameter p represents an arbitrary domain element
         ;; and could denote any term, so (eq (app p) (app s x))
         ;; must NOT clash.
         ;; ============================================================
         [(fresh [t1 t2]
            (== ['eq t1 t2] lit)
            (== ['free-close] proof)
            (free-closureo t1 t2))]

         ;; ============================================================
         ;; EQ/NEQ COMPLEMENTARY CLOSURE
         ;; ============================================================
         ;; When the current literal is (eq t1 t2) and (neq t1 t2) or
         ;; (neq t2 t1) is already on the branch, the branch is
         ;; contradictory — we assert both t1=t2 and t1≠t2.
         ;;
         ;; This is the dual of "neq on branch + eq in lits" which is
         ;; already handled by eq-refl-close.  Without this rule, the
         ;; conjunction (and (neq ...) (eq ...)) with the neq processed
         ;; first would fail to close, creating an order-dependence bug.
         ;; ============================================================
         [(fresh [t1 t2]
            (== ['eq t1 t2] lit)
            (== ['eq-neq-close] proof)
            (conde
              [(membero ['neq t1 t2] lits)]
              [(membero ['neq t2 t1] lits)]))]

         ;; ============================================================
         ;; INJECTIVITY DECOMPOSITION (Fitting §5 — One-One Expansion)
         ;; ============================================================
         ;; When the current literal is (eq (app f t₁…tₙ) (app f s₁…sₙ))
         ;; with the SAME head f and at least one argument, we exploit
         ;; injectivity: f(t) = f(s) iff t₁=s₁ ∧ … ∧ tₙ=sₙ.
         ;;
         ;; The equality is REPLACED by the conjunction of sub-equalities,
         ;; which proveo then processes as a new formula.  This cascades:
         ;; f(g(a)) = f(g(b)) decomposes to g(a) = g(b), which itself
         ;; decomposes to a = b, which free-closes if a ≠ b.
         ;;
         ;; Unlike the ephemeral one-one pairs in collect-eqso (which
         ;; only aid rewriting), this rule creates ACTUAL sub-formulas
         ;; that enter the proof search — enabling nested decomposition
         ;; and interaction with other rules.
         ;;
         ;; SOUNDNESS GUARD: head must be a genuine constructor symbol,
         ;; not a δ-parameter nom.  (δ-parameters are always nullary
         ;; (app p) so the argument-presence check suffices, but we
         ;; guard explicitly for safety.)
         ;; ============================================================
         [(fresh [f args1 args2 decomposed prf]
            (== ['eq (lcons 'app (lcons f args1))
                     (lcons 'app (lcons f args2))] lit)
            ;; Must have arguments to decompose (not just (app f) = (app f))
            (fresh [_ __]
              (== (lcons _ __) args1))
            ;; Head must be a genuine symbol
            (project [f]
              (if (symbol? f) succeed fail))
            (decompose-eq-argso args1 args2 decomposed)
            (== (lcons 'decompose prf) proof)
            (proveo decomposed unexp lits env program prf))]

         ;; ============================================================
         ;; PARAMODULATED FREE CLOSURE (transitive constructor clash)
         ;; ============================================================
         ;; When the current literal is (eq t1 t2) and branch equalities
         ;; (from lits) can rewrite one side in one or more steps to
         ;; clash with the other side, the branch is contradictory.
         ;;
         ;; Example:
         ;;   lits: (eq (app a) (app p))      — a is symbol, p is nom
         ;;   current: (eq (app b) (app p))
         ;;   eqs: {[(app p),(app a)], [(app a),(app p)]}
         ;;   Rewrite t2=(app p) → (app a): get (eq (app b) (app a))
         ;;   free-closureo fires: b ≠ a ✓
         ;;
         ;; This enables member(b, cons(a, nil)) to fail when b≠a:
         ;; after decomposing cons(a,nil)=cons(p,q) into a=p ∧ nil=q,
         ;; the branch (eq b p) can be resolved via p→a to get (eq b a).
         ;; ============================================================
         [(fresh [t1 t2 eqs]
            (== ['eq t1 t2] lit)
            (== ['para-free-close] proof)
            (collect-eqso lits eqs)
            (para-free-closeo t1 t2 eqs))]

         ;; ============================================================
         ;; NEQ CLOSURE VIA EQUALITY REWRITING
         ;; ============================================================
         ;; If the current literal is (neq t1 t2) and branch equalities
         ;; (including one-one derived pairs) can rewrite t1 to t2 in
         ;; one or more steps, then we have (neq t t) → contradiction.
         ;;
         ;; Uses eq-neq-closeo for multi-step rewriting, enabling
         ;; transitivity chains: a=b, b=c closes (neq a c) via a→b→c.
         ;; ============================================================
         [(fresh [t1 t2 eqs]
            (== ['neq t1 t2] lit)
            (== ['eq-refl-close] proof)
            (collect-eqso lits eqs)
            (eq-neq-closeo t1 t2 eqs))]

         ;; ---- Paramodulation closure (pos/neg) ----
         ;; Now enhanced: collect-eqso produces one-one derived pairs,
         ;; so paramodulation automatically uses injectivity.
         [(fresh [tm neg eqs]
            (== ['para-close] proof)
            (conde
              [(== ['pos tm] lit) (== ['neg tm] neg)]
              [(== ['neg tm] lit) (== ['pos tm] neg)])
            (collect-eqso lits eqs)
            (eq-membero neg lits eqs))]

         ;; ============================================================
         ;; PROCEDURE CALL RULE — POSITIVE (Fitting §6, Part 1)
         ;; ============================================================
         ;; Branch has (pos (app R args...)).  Look up clause for R,
         ;; bind params→args, close if subsidiary tableau on body closes.
         ;; ============================================================
         [(fresh [R args params body call-env prf]
            (== ['pos (lcons 'app (lcons R args))] lit)
            (lookup-clauseo R program params body)
            (bind-argso params args call-env)
            (== (lcons 'proc-call (lcons R prf)) proof)
            (proveo body '() '() call-env program prf))]

         ;; ============================================================
         ;; PROCEDURE CALL RULE — NEGATIVE (Fitting §6, Part 2)
         ;; ============================================================
         ;; Branch has (neg (app R args...)).  Look up clause, negate
         ;; body, close if subsidiary tableau on ¬body closes.
         ;; ============================================================
         [(fresh [R args params body call-env neg-body prf]
            (== ['neg (lcons 'app (lcons R args))] lit)
            (lookup-clauseo R program params body)
            (bind-argso params args call-env)
            (negate-formulao body neg-body)
            (== (lcons 'neg-proc-call (lcons R prf)) proof)
            (proveo neg-body '() '() call-env program prf))]

         ;; ============================================================
         ;; SUBSTITUTIVITY-AUGMENTED PROCEDURE CALL — POSITIVE
         ;; ============================================================
         ;; When branch equalities (including one-one derived pairs) can
         ;; rewrite one or more of the literal's arguments, apply the
         ;; rewrites and fire the proc call with rewritten args.
         ;;
         ;; Multi-argument: each argument position is independently
         ;; rewritable using a different equality pair.  R(p₁, p₂)
         ;; with eqs p₁=a, p₂=b rewrites to R(a, b) in one step.
         ;;
         ;; This bridges δ-parameters and concrete constructors:
         ;;   Branch: (eq (app s (app zero)) (app s (app p)))
         ;;   One-one: pair [(app zero), (app p)]
         ;;   Literal: (pos (app odd (app p)))
         ;;   Rewrite: (app odd (app p)) → (app odd (app zero))
         ;;   Proc call: odd(zero) instead of odd(p)
         ;; ============================================================
         [(fresh [R args params body call-env prf
                  tm new-tm new-args eqs]
            (== ['pos tm] lit)
            (== (lcons 'app (lcons R args)) tm)
            (collect-eqso lits eqs)
            (rewrite-term-with-eqso tm eqs new-tm)
            (== (lcons 'app (lcons R new-args)) new-tm)
            (lookup-clauseo R program params body)
            (bind-argso params new-args call-env)
            (== (lcons 'subst-call (lcons R prf)) proof)
            (proveo body '() '() call-env program prf))]

         ;; ============================================================
         ;; SUBSTITUTIVITY-AUGMENTED PROCEDURE CALL — NEGATIVE
         ;; ============================================================
         ;; Same as positive, but for (neg (app R args...)).
         ;; Each argument independently rewritable.
         ;; ============================================================
         [(fresh [R args params body call-env neg-body prf
                  tm new-tm new-args eqs]
            (== ['neg tm] lit)
            (== (lcons 'app (lcons R args)) tm)
            (collect-eqso lits eqs)
            (rewrite-term-with-eqso tm eqs new-tm)
            (== (lcons 'app (lcons R new-args)) new-tm)
            (lookup-clauseo R program params body)
            (bind-argso params new-args call-env)
            (negate-formulao body neg-body)
            (== (lcons 'neg-subst-call (lcons R prf)) proof)
            (proveo neg-body '() '() call-env program prf))]

         ;; ---- Continue expansion (savefml) ----
         [(fresh [next unexp1 prf]
            (== (lcons next unexp1) unexp)
            (== (lcons 'savefml prf) proof)
            (proveo next unexp1 (lcons lit lits) env program prf))]))]))

;; ============================================================================
;; Part 7: Top-Level Interface
;; ============================================================================

(defn query-succeeds
  "A query A succeeds with program P if there is a closed P-tableau for ¬A.
   (Fitting, Definition 6.1)
   
   Returns proof(s) if the query succeeds, nil otherwise."
  ([program query]
   (query-succeeds program query 1))
  ([program query n]
   (run n [proof]
     (fresh [neg-query]
       (negate-formulao query neg-query)
       (proveo neg-query '() '() '() program proof)))))

(defn query-fails
  "A query A fails with program P if there is a closed P-tableau for A.
   (Fitting, Definition 6.1)
   
   Returns proof(s) if the query fails, nil otherwise."
  ([program query]
   (query-fails program query 1))
  ([program query n]
   (run n [proof]
     (proveo query '() '() '() program proof))))

(defn prove
  "Direct tableau proof: find closed P-tableau for formula.
   For backward compatibility with αleanTAP-E (empty program)."
  ([formula]
   (prove formula 1))
  ([formula n]
   (prove '() formula n))
  ([program formula n]
   (run n [proof]
     (proveo formula '() '() '() program proof))))

;; ============================================================================
;; Part 8: Program Construction Helpers
;; ============================================================================
;;
;; Building Proflog programs requires creating clauses with nominal
;; parameters.  These helpers make it more convenient.

(defmacro defclause
  "Define a Proflog clause using nom bindings.
   
   Usage:
     (defclause even-clause 'even [a]
       '(or (eq (var ~a) (app zero))
            (exists ...)))
   
   Expands to a clause vector [rel-symbol [noms...] body]."
  [name rel params & body]
  `(def ~name
     (let [~@(mapcat (fn [p] [p `(clojure.core.logic.nominal/nom (clojure.core.logic/lvar '~p))]) params)]
       [~rel [~@params] ~@body])))

;; ============================================================================
;; Part 9: Worked Examples
;; ============================================================================

(comment
  ;; ==========================================================================
  ;; EXAMPLE 1: Even and Odd (Fitting, Section 2, Program P1)
  ;; ==========================================================================
  ;;
  ;; Language L: constant 'zero, function 's (successor),
  ;;             relations 'even and 'odd (and equality)
  ;;
  ;; Fitting's original:
  ;;   even(x) ← x = 0 ∨ (∃y)[x = s(y) ∧ odd(y)]
  ;;   odd(x)  ← (∀y)[even(y) ⊃ ¬(x = y)]
  ;;
  ;; In our NNF representation, the odd clause body becomes:
  ;;   ∀y. ¬even(y) ∨ ¬(x = y)   [since (A ⊃ B) = (¬A ∨ B) in NNF]
  ;;   = ∀y. (neg (app even (var y))) ∨ (neq (var x) (var y))
  ;;
  ;; Note: The negative literal (neg (app even (var y))) triggers a
  ;; NEGATIVE procedure call — the prover will negate the even body and
  ;; try to refute it, effectively trying to prove even(y) is true,
  ;; which would contradict the assertion ¬even(y).
  ;;
  ;; For simplicity, we can also write odd more directly:
  ;;   odd(x) ← (∃y)[x = s(y) ∧ even(y)]

  ;; --- Using the simpler mutually recursive definition ---
  ;; (This is more natural for demonstration, though Fitting's
  ;;  original uses the ∀/⊃/¬ form to showcase full first-order logic)

  ;; The program must be built inside a `run` form so noms are properly
  ;; scoped within the logic monad.

  ;; Query: is even(s(s(zero))) true?
  ;; We expect SUCCESS because 2 is even.
  (run 1 [proof]
    (nom a b c d  ;; noms for clause params
      (let [even-clause ['even [a]
                         '(or (eq (var a) (app zero))
                              (exists (tie b (and (eq (var a) (app s (var b)))
                                                  (pos (app odd (var b)))))))]
            odd-clause  ['odd [c]
                         '(exists (tie d (and (eq (var c) (app s (var d)))
                                              (pos (app even (var d))))))]
            program     [even-clause odd-clause]
            ;; Query: even(s(s(zero)))
            ;; To check if this succeeds, we need a closed tableau for
            ;; ¬even(s(s(zero))), i.e., (neg (app even (app s (app s (app zero)))))
            query-negated '(neg (app even (app s (app s (app zero)))))]
        (proveo query-negated '() '() '() program proof))))

  ;; Query: is odd(s(s(s(zero)))) true?  (3 is odd — should succeed)
  ;; Query: is even(s(zero)) true?  (1 is not even — should fail or diverge)


  ;; ==========================================================================
  ;; EXAMPLE 2: Nim game (Fitting, Section 2, Program P2)
  ;; ==========================================================================
  ;;
  ;; win(x) ← (∃y)[(x = s(y) ∨ x = s(s(y))) ∧ ¬win(y)]
  ;;
  ;; You can lower the number by 1 or 2.  The player who reaches 0 loses.
  ;; win(n) means: if it's your turn and the number is n, you can win.
  ;;
  ;; win(0) = false   (no move, you lose)
  ;; win(1) = true    (move to 0, opponent loses)
  ;; win(2) = true    (move to 0, opponent can't respond)
  ;; win(3) = false   (move to 1 or 2, both winning for opponent)
  ;; win(4) = true    (move to 3, opponent in losing position)

  ;; Query: does win(s(s(s(zero)))) FAIL? (win(3) should be false)
  ;; We build a closed P2-tableau for win(s(s(s(zero)))).
  (run 1 [proof]
    (nom a b
      (let [win-clause ['win [a]
                         ['exists (tie b
                           ['and ['or ['eq ['var a] ['app 's ['var b]]]
                                      ['eq ['var a] ['app 's ['app 's ['var b]]]]]
                                 ['neg ['app 'win ['var b]]]])]]
            program     [win-clause]
            ;; A closed tableau for win(s(s(s(0)))) shows win(3) is false
            formula     ['pos ['app 'win ['app 's ['app 's ['app 's ['app 'zero]]]]]]]
        (proveo formula '() '() '() program proof))))


  ;; ==========================================================================
  ;; EXAMPLE 3: Backward running — generate even numbers
  ;; ==========================================================================
  ;;
  ;; Because αleanTAP-EP is a pure relation, we can ask:
  ;; "For which x does even(x) succeed?"
  ;;
  ;; (run 5 [x]
  ;;   (nom a b c d
  ;;     (let [program ...]
  ;;       (fresh [neg-query proof]
  ;;         (negate-formulao ['pos ['app 'even x]] neg-query)
  ;;         (proveo neg-query '() '() '() program proof)))))
  ;;
  ;; This should generate: (app zero), (app s (app s (app zero))), ...


  ;; ==========================================================================
  ;; EXAMPLE 4: Equality + Procedure Calls
  ;; ==========================================================================
  ;;
  ;; member(x, cons(x, _))      ← true
  ;; member(x, cons(_, rest))   ← member(x, rest)
  ;;
  ;; In Proflog style (one clause per relation):
  ;; member(x, l) ← (∃h)(∃t)[l = cons(h, t) ∧ (x = h ∨ member(x, t))]
  ;;
  ;; This combines equality reasoning with procedure calls naturally:
  ;; the equality l = cons(h, t) destructures the list, and then either
  ;; x = h (found it) or we recurse.
  )

;; ============================================================================
;; Part 10: Design Notes
;; ============================================================================
;;
;; THE ARCHITECTURE OF A PROCEDURE CALL
;; =====================================
;;
;; When proveo processes a literal (pos (app R args...)):
;;
;;   1. subst-lito replaces noms with their bound values in the current env
;;   2. The result is matched against (pos (app R args...))
;;   3. lookup-clauseo finds R's clause in the program: [R [params] body]
;;   4. bind-argso creates a fresh env: {param₁ → arg₁, ..., paramₙ → argₙ}
;;   5. proveo is called RECURSIVELY on `body` with:
;;        - EMPTY unexp  (fresh proof obligation, not continuation of branch)
;;        - EMPTY lits   (no inherited context — the subsidiary tableau
;;                         is independent)
;;        - FRESH env    (only the clause parameter bindings)
;;        - SAME program (recursive calls are possible)
;;   6. If the subsidiary proveo succeeds (body is unsatisfiable),
;;      the original branch is closed.
;;
;; For the negative case (neg (app R args...)):
;;   Steps 1-4 are the same.
;;   5. negate-formulao computes ¬body in NNF
;;   6. proveo is called on ¬body with fresh state
;;   7. If ¬body is unsatisfiable (body is valid), branch closes.
;;
;;
;; WHY SUBSIDIARY TABLEAUX START FRESH
;; ====================================
;;
;; This is a crucial design point.  The subsidiary tableau does NOT
;; inherit the current branch's literals or unexpanded formulas.
;; This matches Fitting's paper exactly: the Procedure Call Rule
;; says "there exists a closed tableau for φ(t)" — a complete,
;; self-contained tableau, not a continuation of the current one.
;;
;; This has deep consequences:
;;   - Procedure calls are MODULAR: the subsidiary proof is independent
;;   - No "spooky action at a distance" between branches
;;   - The soundness proof (Fitting, Section 7) depends on this isolation
;;
;; However, unification variables CAN flow between the calling and
;; subsidiary tableaux (via the argument terms).  This is how
;; procedure calls communicate results back: the subsidiary proof
;; may instantiate logic variables that appear in the caller's context.
;;
;;
;; THE δ-RULE (EXISTENTIAL QUANTIFIER)
;; ====================================
;;
;; αleanTAP originally only needed the γ-rule (∀) because input
;; formulas were pre-Skolemized.  Proflog clause bodies can contain
;; ∃ (as in the win and even examples), and more critically,
;; negate-formulao turns ∀ into ∃ (for negative procedure calls).
;; So the δ-rule is essential.
;;
;; Our δ-rule uses a fresh nom as the Skolem parameter, wrapped in
;; (app p) to make it a proper term.  The nom is globally unique,
;; satisfying the requirement that the parameter be "new" (not
;; occurring elsewhere on the branch).
;;
;; Key difference from the γ-rule:
;;   - γ (∀): introduces a LOGIC VARIABLE (can be unified later)
;;            and RE-ENQUEUES the formula for potential re-instantiation
;;   - δ (∃): introduces a FIXED PARAMETER (nom, cannot be unified)
;;            and does NOT re-enqueue (one witness suffices)
;;
;;
;; INTERACTION WITH EQUALITY
;; ==========================
;;
;; Equality and procedure calls interact in three important ways:
;;
;; 1. EQUALITY IN CLAUSE BODIES:
;;    Clause bodies can use (eq ...) and (neq ...) freely.
;;    The even/odd example uses x = 0 and x = s(y).
;;    The member example uses l = cons(h, t) for list destructuring.
;;    All equality rules (reflexivity, paramodulation, free closure)
;;    are available inside subsidiary tableaux.
;;
;; 2. FITTING'S FREE CLOSURE RULE (Section 5):
;;    Weak Herbrand models require that distinct function symbols have
;;    non-overlapping ranges and are injective.  This means:
;;      - 0 ≠ s(x)         for any x  (disjointness / clash)
;;      - f(x) ≠ g(y)      if f ≠ g   (different function symbols)
;;      - f(x) = f(y) → x = y         (injectivity / decomposition)
;;    Our implementation provides:
;;      (a) FREE CLOSURE (clash): (eq (app f ...) (app g ...)) with f ≠ g
;;          closes the branch immediately.  Proof step: 'free-close.
;;          SOUNDNESS GUARD: uses `project` to verify both heads are
;;          genuine Clojure symbols, not δ-parameter noms.  A nom
;;          represents an arbitrary domain element and could denote any
;;          term, so (eq (app nom_p) (app s x)) must NOT clash.
;;      (b) INJECTIVITY DECOMPOSITION: (eq (app f t₁..tₙ) (app f s₁..sₙ))
;;          with same head f EXPANDS into a conjunction of sub-equalities
;;          (and (eq t₁ s₁) ... (eq tₙ sₙ)).  This creates actual
;;          sub-formulas that enter the proof search, enabling cascading
;;          decomposition: f(g(a)) = f(g(b)) → g(a) = g(b) → a = b
;;          → free-close.  Proof step: 'decompose.
;;      (c) ONE-ONE PAIRS IN PARAMODULATION: The same injectivity principle
;;          also injects pairwise sub-equalities [tᵢ, sᵢ] into the
;;          rewriting engine via enhanced collect-eqso, enabling
;;          paramodulation and substitutivity to use derived equalities
;;          without explicit formula expansion.
;;      (d) NEQ CLOSURE: (neq t1 t2) on the branch, combined with
;;          one-one derived equalities, can rewrite t1 → t2 to yield
;;          (neq t t) → refl-close.  Proof step: 'eq-refl-close.
;;      (e) EQ/NEQ COMPLEMENTARY CLOSURE: When (eq t1 t2) is the current
;;          literal and (neq t1 t2) or (neq t2 t1) is already on the
;;          branch, the contradiction is detected directly.  This prevents
;;          order-dependent failures where the neq was processed first.
;;          Proof step: 'eq-neq-close.
;;
;; 3. SUBSTITUTIVITY-AUGMENTED PROCEDURE CALLS:
;;    When a δ-rule introduces a fresh parameter p and the branch has
;;    an equality like s(zero) = s(p), one-one decomposition yields the
;;    pair [zero, p].  If the current literal is (pos (app odd (app p))),
;;    substitutivity rewrites (app p) → (app zero) in the arguments,
;;    enabling a procedure call on odd(zero) instead of the rigid odd(p).
;;    Without this, subsidiary tableaux would receive parameters they
;;    cannot resolve.  Proof steps: 'subst-call, 'neg-subst-call.
;;
;;    Multi-argument support: for terms with multiple arguments like
;;    member(p₁, p₂), each argument is independently rewritable using
;;    a possibly different equality pair from the branch.  The relations
;;    rewrite-args-someo / rewrite-args-maybeo handle this by mapping
;;    over the argument list, with someo guaranteeing at least one
;;    argument is actually rewritten (preventing overlap with plain
;;    procedure call rules).  Essential for binary constructors like
;;    cons and multi-arity relations.
;;
;;
;; THREE-VALUED SEMANTICS AND DIVERGENCE
;; ======================================
;;
;; Fitting's Proflog uses three-valued supervaluation semantics:
;;   - true:  query succeeds (closed tableau for ¬A exists)
;;   - false: query fails    (closed tableau for A exists)
;;   - ⊥:     undefined      (neither tableau closes — infinite search)
;;
;; In our implementation, the ⊥ case manifests as non-termination of
;; the core.logic search.  This is analogous to how Prolog loops on
;; undefined queries.  The key example from Fitting:
;;
;;   p ← ¬p
;;
;; Neither p nor ¬p can be established — the procedure call creates
;; an infinite chain of subsidiary tableaux.  In our relational
;; setting, `run 1` will simply not return.
;;
;;
;; ON FITTING'S "GROUND ATOM OF L" RESTRICTION
;; =============================================
;;
;; Fitting requires that procedure calls apply only to ground atoms
;; of L (not Lpar — the language extended with parameters).  This
;; prevents "program P from knowing about" Skolem constants introduced
;; during tableau expansion.
;;
;; In our free-variable setting, this restriction is relaxed for
;; LOGIC VARIABLES: procedure calls can apply to terms containing
;; logic variables, and the subsidiary tableau may instantiate them
;; via unification.  This enables backward-running queries.
;;
;; However, the restriction is CRITICAL for NOMINAL PARAMETERS
;; introduced by the δ-rule.  When a subsidiary tableau receives
;; an argument containing a nom (e.g., from ∃-elimination in a
;; negated body), the procedure call enters Lpar territory.  The
;; subsidiary tableau then reasons about a "parameter" that has no
;; definition in the program's Herbrand universe.
;;
;; Example: In the nim game, ¬win(p) where p is a δ-witness:
;;   - The neg proc-call negates the body and introduces ∀y over
;;     neq-expressions involving p.
;;   - Since p is a rigid nom distinct from all constructors (zero, s),
;;     the neq expressions (neq (app p) (app s ...)) are trivially
;;     true — but the tableau cannot CLOSE on trivially true formulas.
;;   - The proof search diverges exploring the universal quantifier.
;;
;; This is Fitting's Section 8 open problem: restricting procedure
;; calls to ground atoms of L.  Our free closure rules (clash and
;; decomposition) resolve the GROUND cases completely — e.g.,
;; win(0) fails, win(s(0)) succeeds, odd(0) fails — but the
;; parameter cases require either:
;;   (a) A groundness check before procedure calls, or
;;   (b) "Trivially true" neq closure rules for parameter terms,
;;       which would need careful soundness analysis.
;;
;; Nominal logic's scoping helps: noms introduced in subsidiary
;; tableaux cannot "leak" into the caller's context via unification.
;;
;;
;; COMPARISON WITH PROLOG
;; =======================
;;
;; Proflog differs from Prolog in several fundamental ways:
;;
;;   PROLOG                          PROFLOG (αleanTAP-EP)
;;   ─────                           ──────
;;   Horn clauses only               Full first-order logic in bodies
;;   SLD-resolution                  Tableau expansion + procedure call
;;   Closed-world assumption         Open-world (supervaluation)
;;   Negation as failure             Classical negation (¬ in bodies)
;;   Definite clause semantics       Three-valued supervaluation model
;;   One direction (forward only)    Pure relation (forward, backward,
;;                                    sideways)
;;
;; The cost: Proflog is less efficient than Prolog, because full
;; first-order tableau expansion is more expensive than SLD-resolution.
;; The gain: Proflog is more expressive and more declarative.
;;
;; ============================================================================
