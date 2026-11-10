(ns proflog.fitting.tableau
  "Sections 4–6 of M. Fitting, \"Tableaus for Logic Programming\" (1993),
  and the systematic construction of §7, as one pure core.logic relation:
  `tableauo`. Definition 6.1 is the relation `queryo`.

  `(tableauo P X cert)` holds when `cert` describes a closed P-tableau for
  the sentence X (Definition 6.1: a tableau built with the Branch Extension
  Rules of §4, the Equality Rules of §5 and the Procedure Call Rule of §6).

  - With `cert` fresh, it is a prover. It searches for a closed tableau,
    with no bound, as Fitting's rules have none.
  - With `cert` ground, it is a checker. Every clause unifies the
    certificate's rule tag, or the shape of the sentence being expanded,
    before anything else, so a check does not search.

  Prover and checker are the same code, so they cannot disagree (ADR-0149,
  Decision §2).

  Purity
  ------
  Only `==`, `conde`, `fresh` and relations built from them (`defne`,
  `conso`, `appendo`, `membero`) are used. There is no `!=`, no nominal
  logic, no `fd`, no `conda`/`condu` and no `project`. Each side condition
  the paper states negatively is a lookup in a finite table built by
  `proflog.fitting.syntax`, or holds by construction:

  - \"distinct constant/function symbols of L\" (Free Closure, §5);
  - \"an occurrence of the variable being substituted\" (§2), using the
    names table;
  - \"a new parameter\" (δ, §4): the branch counts its parameters, so
    `(par . k)` is new by construction.

  The construction's own conditions (a literal not already on the branch,
  a term a γ sentence has not had) are structural difference of ground
  expressions, over the table of distinct symbols (`differento`).

  Representation
  --------------
  Formulas are written as in `proflog.fitting.syntax`. Rules build new
  formulas with vectors, which unify with lists and are `=` to them.
  `syntax/pretty` prints them as lists.

  Every formula placed on a branch is a sentence of Lpar. γ, δ, the
  Procedure Call Rule and Substitutivity insert only closed terms, so §2's
  substitution needs no renaming (ADR-0149, Decision §2).

  Systematic construction (§7)
  ----------------------------
  Fitting's completeness proof (Proposition 7.7) adapts \"a systematic
  tableau construction method\" from Smullyan or from his own book to §5
  and §6, and \"we skip the details\". The relation is such a construction.

  1. A branch keeps a queue of sentences not yet expanded (`todo`), its γ
     sentences (`gammas`), its literals (`lits`), and the parameters it
     has introduced (`k`, a unary count).
  2. The head of `todo` is expanded by the one rule of Table 2 that fits its
     shape: ¬¬, ¬true, ¬false, α, β or δ. `false` closes the branch. `true`
     needs nothing. A γ sentence joins `gammas`, and a literal joins `lits`.
     None of these is a choice.
  3. When `todo` is empty the branch is *saturated*. It then either closes,
     or applies γ to the γ sentence at the head of `gammas` with some closed
     term and moves that sentence to the back. The rotation makes the
     construction fair: every γ sentence is reached again.
  4. A saturated branch closes by:
     - both A and ¬A (§4);
     - Reflexivity against ¬(t = t) (§5);
     - Free Closure (§5);
     - the Procedure Call Rule (§6), which needs a closed subsidiary
       tableau.

     Before closing it may add literals by One-One and Substitutivity (§5).

  The certificate records exactly the choices: which closure, which γ term,
  and which equality steps. It also records a tag for every rule application,
  so a certificate reads as the tableau it describes.

  What the construction leaves out, and why that loses nothing
  ------------------------------------------------------------
  The relation accepts the tableaus of this construction only, not every
  closed tableau Fitting's rules allow. A tableau that applies rules in
  another order, or makes a step the construction does not make, is
  rejected. For every closed P-tableau, though, the construction makes one,
  so t_P is unchanged (Theorem 7.2). What it does not do:

  - Closure on A and ¬A is checked on literals only. A closure on compound A
    is reached after decomposing A, as in Fitting's book.
  - Substitutivity rewrites literals only, which suffices for ground equality
    reasoning. One-One and Substitutivity happen only on the way to a
    closure, because a derived literal can feed nothing but closures and
    further equality steps.
  - Reflexivity is used only to close against ¬(t = t). t = t is never
    called (equality has no clause), never closes by Free Closure, and
    rewriting t to t changes nothing. Fitting also uses t = t to derive
    symmetry (u = t from t = u). That use is not needed here, because Free
    Closure and Substitutivity already accept an equation in either
    orientation, and ¬(u = t) with t = u becomes ¬(u = u) by rewriting.
  - γ applies to the γ sentence at the head of the queue only. A tableau
    that needs another γ sentence first reaches it by giving the ones ahead
    of it a term they have not had, because more sentences never stop a
    branch from closing. This is also why a certificate need name only the
    term.
  - γ's closed term is drawn from the closed terms of L and the parameters
    already on the branch. A parameter that is not yet on the branch can be
    renamed to one of these terms throughout the subtree (ADR-0149, Decision
    §2, substitution lemma). There are two exceptions, where γ uses a new
    parameter `(par . k)` and counts it:
    - when L has no constant and the branch no parameter, so that there is
      no such term at all;
    - when L has no function symbol, so that there are only finitely many
      such terms. The γ sentence at the head of the queue could otherwise
      use up its terms and block every γ sentence behind it.

  Three more restrictions keep the construction from repeating itself:
  - Each closed term is given to a γ sentence at most once on a branch,
    because a second instance adds nothing.
  - An equality step adds no literal that is already on the branch, and no
    equation t = t.
  - Every closure uses a literal that is new since the previous saturation
    point, or one derived from such a literal. A closure from older literals
    alone was already possible at the previous saturation point, in a
    smaller tableau with fewer choices on the branch, so nothing is lost.
    This also stops one branch from closing in the same way at every later
    saturation point, which would multiply the search for its β sibling.

  These three are not needed to find a closed tableau, only to find it in
  reasonable time. Without them, the search for the paper's own §6 tableau
  ran out of memory after 20 minutes (ADR-0149, revision 8).

  No bound, and fuel as a wrapper
  -------------------------------
  The construction is written once, with open recursion (`construction`).
  Its choices, the steps that can repeat without end on one path, are taken
  at the construction's next stage:
  - a γ instance;
  - a procedure call;
  - a One-One step;
  - a Substitutivity step;
  - each argument of a γ term, in the term generator.

  `tableauo` makes the next stage the construction itself, so it has no
  bound. Its search finds every closed tableau, because core.logic's
  interleaving search reaches every answer at a finite depth. It need not
  end when there is none.

  `fueled-tableauo` gives the same construction n stages, so a branch makes
  at most n choices, and no γ term is deeper than the number of choices
  left after its γ instance. Each such search is finite, so it can end with
  \"no closed tableau within n\". Forced steps (¬¬, ¬true, ¬false, α, β,
  δ, setting aside γ sentences and literals) and the closures that need no
  further step are free (ADR-0149, revision 7)."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== conde fresh defne lcons llist conso appendo membero fail]]))

;;; =====================================================================
;;; Finite tables of L and of the program (built by proflog.fitting.syntax)

(defn- constanto
  "c is a constant symbol of L."
  [P c]
  (membero c (:constants (:language P))))

(defn- functiono
  "f is a function symbol of L, with arity a unary list."
  [P f arity]
  (membero [f arity] (:functions (:language P))))

(defn- relationo
  "R is a relation symbol of L (equality included)."
  [P R]
  (fresh [arity] (membero [R arity] (:relations (:language P)))))

(defn- distinct-constantso
  "c and d are distinct constant symbols of L."
  [P c d]
  (membero [c d] (:distinct-constants (:language P))))

(defn- distinct-functionso
  "f and g are distinct function symbols of L."
  [P f g]
  (membero [f g] (:distinct-functions (:language P))))

(defn- symbolo
  "a is a symbol that a ground literal of Lpar can contain."
  [P a]
  (membero a (:symbols (:language P))))

(defn- distinct-symbolso
  "a and b are different symbols that a ground literal of Lpar can contain."
  [P a b]
  (membero [a b] (:distinct-symbols (:language P))))

(defn- distinct-nameso
  "The leaf s (a variable or a constant) is not the variable x."
  [P x s]
  (membero [x s] (:distinct-names P)))

(defn- clauseo
  "P has the clause R(x1,...,xn) ← ϕ, with xs = (x1 ... xn)."
  [P R xs phi]
  (membero [R xs phi] (:clauses P)))

;;; =====================================================================
;;; §2. "We write ϕ(t1,...,tn) to indicate the result of substituting
;;; t1,...,tn for all free occurrences of x1,...,xn in ϕ(x1,...,xn)."

(declare subst-termo)

(defn- subst-termso
  "ts' is the list of terms ts, with t substituted for x in each."
  [P ts x t ts']
  (conde
   [(== ts ()) (== ts' ())]
   [(fresh [s ss s' ss']
      (conso s ss ts)
      (conso s' ss' ts')
      (subst-termo P s x t s')
      (subst-termso P ss x t ss'))]))

(defn subst-termo
  "s' is the term s with the closed term t substituted for the variable x."
  [P s x t s']
  (conde
   ;; An occurrence of x.
   [(== s x) (== s' t)]
   ;; Another variable, or a constant.
   [(distinct-nameso P x s) (== s' s)]
   ;; A parameter of Lpar.
   [(fresh [k] (== s (lcons 'par k)) (== s' s))]
   ;; f(s1,...,sn): substitute in the arguments.
   [(fresh [f arity as as']
      (== s (lcons f as))
      (functiono P f arity)
      (== s' (lcons f as'))
      (subst-termso P as x t as'))]))

(defn substo
  "out is ϕ(t): the formula phi with the closed term t substituted for all
  free occurrences of the variable x (§2)."
  [P phi x t out]
  (conde
   [(== phi true) (== out true)]
   [(== phi false) (== out false)]
   [(fresh [X X']
      (== phi ['not X])
      (== out ['not X'])
      (substo P X x t X'))]
   [(fresh [c X Y X' Y']
      (== phi [c X Y])
      (membero c '(and or implies))
      (== out [c X' Y'])
      (substo P X x t X')
      (substo P Y x t Y'))]
   [(fresh [q y Phi]
      (== phi [q y Phi])
      (membero q '(forall exists))
      (conde
       ;; x is bound here, so it has no free occurrence below.
       [(== y x) (== out phi)]
       [(fresh [Phi']
          (distinct-nameso P y x)
          (== out [q y Phi'])
          (substo P Phi x t Phi'))]))]
   ;; An atomic formula R(s1,...,sn), equality included.
   [(fresh [R ts ts']
      (== phi (lcons R ts))
      (relationo P R)
      (== out (lcons R ts'))
      (subst-termso P ts x t ts'))]))

(defn- instanceo
  "out is ϕ(t1,...,tn), for xs = (x1 ... xn) and ts = (t1 ... tn), all ti
  closed. Substituting one variable at a time is the same as substituting
  them simultaneously, because no ti contains a variable."
  [P phi xs ts out]
  (conde
   [(== xs ()) (== ts ()) (== out phi)]
   [(fresh [x xr t tr phi1]
      (conso x xr xs)
      (conso t tr ts)
      (substo P phi x t phi1)
      (instanceo P phi1 xr tr out))]))

;;; =====================================================================
;;; §4, Table 1: Sentence Categories.
;;;
;;;  Conjunctive        Disjunctive         Universal         Existential
;;;  α       α1  α2     β        β1  β2     γ       γ(t)      δ       δ(t)
;;;  (X∧Y)   X   Y      ¬(X∧Y)   ¬X  ¬Y     (∀x)Φ   Φ(t)      (∃x)Φ   Φ(t)
;;;  ¬(X∨Y)  ¬X  ¬Y     (X∨Y)    X   Y      ¬(∃x)Φ  ¬Φ(t)     ¬(∀x)Φ  ¬Φ(t)
;;;  ¬(X⊃Y)  X   ¬Y     (X⊃Y)    ¬X  Y

(defne alphao
  "α is conjunctive, with components α1 and α2."
  [alpha alpha1 alpha2]
  ([['and X Y] X Y])
  ([['not ['or X Y]] ['not X] ['not Y]])
  ([['not ['implies X Y]] X ['not Y]]))

(defne betao
  "β is disjunctive, with components β1 and β2."
  [beta beta1 beta2]
  ([['not ['and X Y]] ['not X] ['not Y]])
  ([['or X Y] X Y])
  ([['implies X Y] ['not X] Y]))

(defne gammao
  "γ is universal. Its instance γ(t) is matrix(t): Φ(t) or ¬Φ(t), the
  result of substituting t for x in matrix."
  [gamma x matrix]
  ([['forall x Phi] x Phi])
  ([['not ['exists x Phi]] x ['not Phi]]))

(defne deltao
  "δ is existential. Its instance δ(t) is matrix(t): Φ(t) or ¬Φ(t)."
  [delta x matrix]
  ([['exists x Phi] x Phi])
  ([['not ['forall x Phi]] x ['not Phi]]))

(defn- atomo
  "A is an atomic sentence R(t1,...,tn) (equality included)."
  [P A]
  (fresh [R ts]
    (== A (lcons R ts))
    (relationo P R)))

(defn- literalo
  "Z is an atomic sentence or the negation of one."
  [P Z]
  (conde
   [(atomo P Z)]
   [(fresh [A] (== Z ['not A]) (atomo P A))]))

;;; =====================================================================
;;; Closed terms
;;;
;;; The γ term generator itself is part of the construction below, because
;;; each argument of a term is a recursive call that a bound must reach.

(defn- belowo
  "The unary number j is less than k."
  [j k]
  (fresh [k']
    (== k (lcons 1 k'))
    (conde
     [(== j k')]
     [(belowo j k')])))

(declare l-termso)

(defn- l-termo
  "t is a closed term of L: no parameters."
  [P t]
  (conde
   [(constanto P t)]
   [(fresh [f arity ts]
      (== t (lcons f ts))
      (functiono P f arity)
      (l-termso P ts))]))

(defn- l-termso [P ts]
  (conde
   [(== ts ())]
   [(fresh [u us] (conso u us ts) (l-termo P u) (l-termso P us))]))

(defn- new-parameter-allowedo
  "γ may use a new parameter: L has no function symbol (only finitely many
  closed terms), or L has no constant and the branch no parameter (no
  closed term at all). Both are properties of L and k known in advance."
  [P k]
  (conde
   [(== (:functions (:language P)) ())]
   [(== (:constants (:language P)) ()) (== k ())]))

;;; =====================================================================
;;; §5. Rules for equality

(defn free-closureo
  "The Free Closure Rule (§5). A branch is closed if it contains:

  1. c = d where c and d are distinct constant symbols of L;
  2. f(t1,...,tn) = c where f is a function symbol of L and c is a constant
     symbol of L;
  3. f(t1,...,tn) = g(u1,...,uk) where f and g are distinct function symbols
     of L.

  Case 2 is also accepted as c = f(t1,...,tn). That is derived symmetry
  (\"if a branch contains t = u, a derived rule allows us to add u = t. We
  assume this when convenient\"), and the paper's own §6 example closes
  0 = s(d) by the Free Closure Rule. Cases 1 and 3 are already symmetric."
  [P e]
  (conde
   [(fresh [c d]
      (== e ['= c d])
      (distinct-constantso P c d))]
   [(fresh [f arity ts c]
      (== e ['= (lcons f ts) c])
      (functiono P f arity)
      (constanto P c))]
   [(fresh [f arity ts c]
      (== e ['= c (lcons f ts)])
      (functiono P f arity)
      (constanto P c))]
   [(fresh [f g ts us]
      (== e ['= (lcons f ts) (lcons g us)])
      (distinct-functionso P f g))]))

(defn- corresponding-argumentso
  "t and u are the arguments at the same place i of the argument lists ts
  and us, for some i."
  [ts us t u]
  (fresh [t1 tr u1 ur]
    (conso t1 tr ts)
    (conso u1 ur us)
    (conde
     [(== t t1) (== u u1)]
     [(corresponding-argumentso tr ur t u)])))

(defn one-oneo
  "The One-One Rule (§5): \"If f is a function symbol of L, a branch
  containing f(t1,...,tn) = f(u1,...,un) may be extended by adding ti = ui
  to the end, for any i = 1,...,n.\" e is the equation on the branch, and
  e' is the ti = ui added."
  [P e e']
  (fresh [f arity ts us t u]
    (== e ['= (lcons f ts) (lcons f us)])
    (functiono P f arity)
    (== e' ['= t u])
    (corresponding-argumentso ts us t u)))

(declare replace-in-termso)

(defn- replace-in-termo
  "s' is s with one occurrence of t replaced by u."
  [P s t u s']
  (conde
   ;; This occurrence.
   [(== s t) (== s' u)]
   ;; An occurrence inside f(s1,...,sn), f a function symbol of L.
   [(fresh [f arity as as']
      (== s (lcons f as))
      (functiono P f arity)
      (== s' (lcons f as'))
      (replace-in-termso P as t u as'))]))

(defn- replace-in-termso
  "ts' is the list ts with one occurrence of t, in one of its terms,
  replaced by u."
  [P ts t u ts']
  (fresh [s ss s' ss']
    (conso s ss ts)
    (conde
     [(replace-in-termo P s t u s') (== ts' (lcons s' ss))]
     [(replace-in-termso P ss t u ss') (== ts' (lcons s ss'))])))

(defn- replace-in-literalo
  "X' is the literal X with one occurrence of t replaced by u."
  [P X t u X']
  (conde
   [(fresh [R ts ts']
      (== X (lcons R ts))
      (relationo P R)
      (== X' (lcons R ts'))
      (replace-in-termso P ts t u ts'))]
   [(fresh [A A']
      (== X ['not A])
      (== X' ['not A'])
      (replace-in-literalo P A t u A'))]))

(defn substitutivityo
  "The Substitutivity Rule (§5): \"Let X and X' be sentences of Lpar, where X'
  is like X except that an occurrence of a closed term t in X has been
  replaced by an occurrence of the closed term u. Then, if X and t = u occur
  on a tableau branch, X' can be added to the branch end.\"

  The equation is e. Right-left replacement, using u = t, is accepted too:
  \"It is easy to show that right-left replacement is a derivable rule\", and
  the paper's §6 example uses it at line (7)."
  [P e X X']
  (fresh [t u]
    (conde
     [(== e ['= t u])]
     [(== e ['= u t])])
    (replace-in-literalo P X t u X')))

;;; =====================================================================
;;; What the construction does not repeat
;;;
;;; None of this is in the paper. The systematic construction (§7) uses it
;;; to avoid repeating itself, and none of it loses a closed tableau (see
;;; the namespace docstring):
;;;
;;; - an equality step adds only a literal that is not already on the
;;;   branch, and never an equation t = t;
;;; - each γ sentence is given each term at most once on a branch;
;;; - every closure uses a literal that is new since the branch was last
;;;   saturated, or one derived from such a literal.
;;;
;;; Without any of them, the search for the paper's own §6 tableau ran out
;;; of memory after 20 minutes. With only the first, it took 11 times as
;;; long, and the ⊥ tests 5 to 70 times as long (ADR-0149, revision 8). Ground literals and terms are different when
;;; they differ as expressions, which is a pure relation over the table of
;;; distinct symbols.

(defn differento
  "x and y are different ground expressions of Lpar: two different symbols,
  a symbol and a list, lists of different lengths, or lists that differ at
  some place. The clauses are exclusive, so on ground expressions there is
  at most one answer."
  [P x y]
  (conde
   [(symbolo P x) (distinct-symbolso P x y)]
   [(symbolo P x) (fresh [a d] (== y (lcons a d)))]
   [(fresh [a d] (== x (lcons a d))) (symbolo P y)]
   [(== x ()) (fresh [a d] (== y (lcons a d)))]
   [(fresh [a d] (== x (lcons a d))) (== y ())]
   [(fresh [a d b e]
      (== x (lcons a d))
      (== y (lcons b e))
      (conde
       [(differento P a b)]
       [(== a b) (differento P d e)]))]))

(defn- not-amongo
  "The ground expression x is different from each member of xs: a literal
  that is not on the branch, or a term that γ has not been given."
  [P x xs]
  (conde
   [(== xs ())]
   [(fresh [y more]
      (conso y more xs)
      (differento P x y)
      (not-amongo P x more))]))

(defn- new-literalo
  "Adding the literal X to a branch with literals lits adds something: X is
  not on the branch, and X is not an equation t = t."
  [P X lits]
  (fresh [R ts t u]
    (conde
     [(== X ['= t u]) (differento P t u)]
     [(== X (lcons R ts)) (distinct-symbolso P R '=)])
    (not-amongo P X lits)))

(defn- new-premiseo
  "Of the two premises A and B, both among lits, at least one is new. The
  clauses are exclusive. Each finds its premises with `membero` before
  testing them, so that the tests run on ground literals."
  [P A B lits new]
  (conde
   [(membero A new) (membero B lits)]
   [(membero B new) (membero A lits) (not-amongo P A new)]))

(defn- conclusiono
  "A literal X derived from premises A and B: lits' and new' are the
  branch's literals with X added, and X counts as new when a premise is."
  [P A B X lits new lits' new']
  (conde
   [(new-premiseo P A B lits new)
    (== lits' (lcons X lits))
    (== new' (lcons X new))]
   [(not-amongo P A new)
    (not-amongo P B new)
    (membero A lits)
    (membero B lits)
    (== lits' (lcons X lits))
    (== new' new)]))

;;; =====================================================================
;;; The branch: Table 2 (§4), closure (§4, §5, §6), and the systematic
;;; construction (§7)

;;; The construction is written once, with open recursion. Most of its
;;; steps are forced. The others are choices that can be repeated without
;;; end on one path:
;;;
;;; - a γ instance (with its term);
;;; - a procedure call;
;;; - a One-One step;
;;; - a Substitutivity step;
;;; - each argument of a γ term, in the term generator.
;;;
;;; Each choice is taken at the construction's next stage, written
;;; `(then :relation args...)`. Everything after a choice, up to the next
;;; one, stays at that stage.
;;;
;;; `tableauo` ties the knot so that the next stage is the construction
;;; itself: Fitting's rules, with no bound. `fueled-tableauo` ties it through
;;; n stages, then a stage at which every choice fails at once. Both run
;;; these rules, so a certificate the fueled relation finds is one the
;;; unbounded relation accepts.

(defn- construction
  "The relations of the systematic construction, as a map from names to
  relations. `next-stage` is a delay of the map whose relations take the
  choices."
  [next-stage]
  (letfn [(then [relation & args]
            ;; A choice, taken at the construction's next stage.
            (apply (relation (force next-stage)) args))

          ;; -------------------------------------------------------------
          ;; Closed terms for γ

          ;; ts is a list of closed terms, one for each 1 in arity. Each is
          ;; generated at the next stage, so a bound on the stages bounds the
          ;; depth of a term.
          (closed-termso [P k arity ts]
            (conde
             [(== arity ()) (== ts ())]
             [(fresh [n u us]
                (== arity (lcons 1 n))
                (== ts (lcons u us))
                (then :closed-termo P k u)
                (closed-termso P k n us))]))

          ;; t is a closed term built from the symbols of L and the
          ;; parameters below k, the parameters already on the branch.
          (closed-termo [P k t]
            (conde
             [(constanto P t)]
             [(fresh [j] (== t (lcons 'par j)) (belowo j k))]
             [(fresh [f arity ts]
                (== t (lcons f ts))
                (functiono P f arity)
                (closed-termso P k arity ts))]))

          ;; t is the closed term of Lpar given to γ, and k' the branch's
          ;; parameter count afterwards. The term is built from the symbols
          ;; of L and the parameters already on the branch, or, where
          ;; `new-parameter-allowedo` says so, it is the new parameter
          ;; (par . k), which the branch then counts.
          (gamma-termo [P k t k']
            (conde
             [(closed-termo P k t) (== k' k)]
             [(new-parameter-allowedo P k)
              (== t (lcons 'par k))
              (== k' (lcons 1 k))]))

          ;; -------------------------------------------------------------
          ;; The choices

          ;; Table 2: γ / γ(t), "for any closed term t of Lpar", applied to
          ;; the γ sentence at the head of the queue gammas. Each entry of
          ;; gammas is [γ used], with used the terms γ has been given on this
          ;; branch, and t is not one of them.
          (gamma-stepo [P gammas lits k cert]
            (fresh [gamma used others x matrix t instance k' gammas' rest]
              (== cert [:gamma t rest])
              (conso [gamma used] others gammas)
              (gammao gamma x matrix)
              (gamma-termo P k t k')
              (not-amongo P t used)
              (substo P matrix x t instance)
              ;; The γ sentence stays on the branch for later use, at the
              ;; back of the queue. The branch is saturated, so nothing is
              ;; new any more.
              (appendo others [[gamma (lcons t used)]] gammas')
              (brancho P [instance] gammas' lits () k' rest)))

          ;; §6. The Procedure Call Rule. A tableau branch is closed if:
          ;;
          ;; 1. it contains a ground atom R(t1,...,tn) of L, there is a
          ;;    clause R(x1,...,xn) ← ϕ(x1,...,xn) in P, and there exists a
          ;;    closed tableau for ϕ(t1,...,tn);
          ;; 2. it contains a negated ground atom ¬R(t1,...,tn) of L, there
          ;;    is such a clause, and there exists a closed tableau for
          ;;    ¬ϕ(t1,...,tn).
          ;;
          ;; "The ground atom R(t1,...,tn) or the negated ground atom
          ;; ¬R(t1,...,tn), must be of L, not of Lpar": `l-termso` rejects
          ;; parameters. Equality has no clause, so it is never called. The
          ;; called literal A is new, and sub describes the subsidiary
          ;; tableau, whose root is ϕ(t1,...,tn) or ¬ϕ(t1,...,tn).
          (call-stepo [P new cert]
            (fresh [A sub R ts xs phi phi-t root]
              (== cert [:call A sub])
              (membero A new)
              (conde
               [(== A (lcons R ts)) (== root phi-t)]
               [(== A ['not (lcons R ts)]) (== root ['not phi-t])])
              (clauseo P R xs phi)
              (l-termso P ts)
              (instanceo P phi xs ts phi-t)
              ;; A subsidiary tableau is a new tableau for a sentence of L.
              (brancho P [root] () () () () sub)))

          ;; §5 One-One: extend the branch with ti = ui, if it is new, then
          ;; close it.
          (one-one-stepo [P lits new cert]
            (fresh [e e' lits' new' rest]
              (== cert [:one-one e e' rest])
              (membero e lits)
              (one-oneo P e e')
              (new-literalo P e' lits)
              ;; One-One has one premise, e, given here as both premises.
              (conclusiono P e e e' lits new lits' new')
              (closeo P lits' new' rest)))

          ;; §5 Substitutivity: extend the branch with X', if it is new, then
          ;; close it.
          (substitutivity-stepo [P lits new cert]
            (fresh [e X X' lits' new' rest]
              (== cert [:substitutivity e X X' rest])
              (membero e lits)
              (membero X lits)
              (substitutivityo P e X X')
              (new-literalo P X' lits)
              (conclusiono P e X X' lits new lits' new')
              (closeo P lits' new' rest)))

          ;; -------------------------------------------------------------
          ;; The saturated branch with literals lits, of which new are new,
          ;; is closed. cert says how.
          (closeo [P lits new cert]
            (conde
             ;; §4: "a branch is closed if it contains ... both A and ¬A for
             ;; some sentence A."
             [(fresh [A]
                (== cert [:close A])
                (new-premiseo P A ['not A] lits new))]
             ;; §5 Reflexivity: "for any closed term t of Lpar, t = t can be
             ;; added to the end of any tableau branch". With ¬(t = t) on
             ;; the branch, the branch then has A and ¬A.
             [(fresh [t]
                (== cert [:reflexivity t])
                (membero ['not ['= t t]] new))]
             ;; §5 Free Closure.
             [(fresh [e]
                (== cert [:free-closure e])
                (membero e new)
                (free-closureo P e))]
             ;; §6 Procedure Call Rule.
             [(then :call-stepo P new cert)]
             ;; §5 One-One, then closure.
             [(then :one-one-stepo P lits new cert)]
             ;; §5 Substitutivity, then closure.
             [(then :substitutivity-stepo P lits new cert)]))

          ;; -------------------------------------------------------------
          ;; The branch has expanded everything but its γ sentences. It
          ;; closes, or γ is applied once more.
          (saturatedo [P gammas lits new k cert]
            (conde
             [(closeo P lits new cert)]
             [(then :gamma-stepo P gammas lits k cert)]))

          ;; -------------------------------------------------------------
          ;; Expand Z, the head of the branch's queue, by the rule of
          ;; Table 2 that fits it. The rest of the queue is todo.
          (expando [P Z todo gammas lits new k cert]
            (conde
             ;; ¬¬Z / Z
             [(fresh [Y rest]
                (== cert [:not-not rest])
                (== Z ['not ['not Y]])
                (brancho P (lcons Y todo) gammas lits new k rest))]
             ;; ¬true / false
             [(fresh [rest]
                (== cert [:not-true rest])
                (== Z ['not true])
                (brancho P (lcons false todo) gammas lits new k rest))]
             ;; ¬false / true
             [(fresh [rest]
                (== cert [:not-false rest])
                (== Z ['not false])
                (brancho P (lcons true todo) gammas lits new k rest))]
             ;; α / α1, α2
             [(fresh [a1 a2 rest]
                (== cert [:alpha rest])
                (alphao Z a1 a2)
                (brancho P (llist a1 a2 todo) gammas lits new k rest))]
             ;; β / β1 | β2
             [(fresh [b1 b2 left right]
                (== cert [:beta left right])
                (betao Z b1 b2)
                (brancho P (lcons b1 todo) gammas lits new k left)
                (brancho P (lcons b2 todo) gammas lits new k right))]
             ;; δ / δ(p), "for a new parameter p": "p is to be a parameter
             ;; that has not previously occurred on the branch". The branch
             ;; has introduced the parameters below k, and no other parameter
             ;; can be on it (its root is a sentence of L), so (par . k) is
             ;; new.
             [(fresh [x matrix instance rest]
                (== cert [:delta rest])
                (deltao Z x matrix)
                (substo P matrix x (lcons 'par k) instance)
                (brancho P (lcons instance todo) gammas lits new (lcons 1 k) rest))]
             ;; "a branch is closed if it contains an explicit
             ;; contradiction: false"
             [(== cert [:false])
              (== Z false)]
             ;; A γ sentence is kept and applied when the branch is
             ;; saturated.
             [(fresh [x matrix gammas']
                (gammao Z x matrix)
                (appendo gammas [[Z ()]] gammas')
                (brancho P todo gammas' lits new k cert))]
             ;; true: no rule applies, and it cannot close a branch.
             [(== Z true)
              (brancho P todo gammas lits new k cert)]
             ;; A literal joins the branch's literals, as a new one.
             [(literalo P Z)
              (brancho P todo gammas (lcons Z lits) (lcons Z new) k cert)]))

          ;; -------------------------------------------------------------
          ;; The branch with queue todo, γ sentences gammas, literals lits
          ;; (new of them new since it was last saturated) and parameter
          ;; count k closes, as cert describes.
          (brancho [P todo gammas lits new k cert]
            (conde
             [(== todo ())
              (saturatedo P gammas lits new k cert)]
             [(fresh [Z more]
                (conso Z more todo)
                (expando P Z more gammas lits new k cert))]))]

    {:brancho brancho
     :closed-termo closed-termo
     :gamma-stepo gamma-stepo
     :call-stepo call-stepo
     :one-one-stepo one-one-stepo
     :substitutivity-stepo substitutivity-stepo}))

;;; =====================================================================
;;; The relation

(def ^:private unbounded
  "The construction tied to itself: Fitting's rules, with no bound."
  (construction (delay unbounded)))

(def ^:private exhausted
  "The stage after the last: every choice fails. `fail` is the goal that
  has no answer."
  (let [no-choice (fn [& _] fail)]
    {:closed-termo no-choice
     :gamma-stepo no-choice
     :call-stepo no-choice
     :one-one-stepo no-choice
     :substitutivity-stepo no-choice}))

(def ^:private stage
  "The construction with n stages after it, then `exhausted`. One map for
  each n."
  (memoize
   (fn [n]
     (construction (delay (if (pos? n) (stage (dec n)) exhausted))))))

(defn tableauo
  "cert describes a closed P-tableau for the sentence X of L (§4, §6).

  P is a program from `proflog.fitting.syntax/program`. Its names table must
  cover X, which `syntax/with-sentence` arranges; a ground literal of L is
  always covered. With cert ground, this checks it. With cert fresh, it
  searches for one, with no bound: if there is a closed tableau the search
  finds it, and if there is none the search may not end."
  [P X cert]
  ((:brancho unbounded) P [X] () () () () cert))

(defn fueled-tableauo
  "tableauo, for the closed tableaus with at most n choices on each branch
  (γ instances, procedure calls, One-One and Substitutivity steps), and
  with no γ term deeper than the number of choices left after its γ
  instance. n is a natural number. The search is finite."
  [P X n cert]
  {:pre [(nat-int? n)]}
  ((:brancho (stage n)) P [X] () () () () cert))

;;; =====================================================================
;;; Definition 6.1

(defn- definition-6-1
  "Definition 6.1, for the closed-tableau relation closed-tableauo."
  [closed-tableauo P A status cert]
  (conde
   [(== status :succeeds) (closed-tableauo P ['not A] cert)]
   [(== status :fails) (closed-tableauo P A cert)]))

(defn queryo
  "Definition 6.1: \"A query A succeeds with program P if there is a closed
  P-tableau for ¬A. A query A fails with program P if there is a closed
  P-tableau for A.\"

  status is :succeeds or :fails, and cert is the closed tableau that shows
  it. A is a ground literal of L.
  - With status fresh, the two searches are interleaved, so the first
    closed tableau on either side answers.
  - With status ground, only that side is searched.
  - With cert ground, it is checked.

  \"It is possible for a query A to neither succeed nor fail\". The search
  for such a query does not end."
  [P A status cert]
  (definition-6-1 tableauo P A status cert))

(defn fueled-queryo
  "queryo over `fueled-tableauo` with the bound n, so the search is finite."
  [P A n status cert]
  (definition-6-1 (fn [P X cert] (fueled-tableauo P X n cert)) P A status cert))
