(ns proflog.fitting.tableau
  "Sections 4–6 of M. Fitting, \"Tableaus for Logic Programming\" (1993),
  and the systematic construction of §7, as one pure core.logic relation:
  `tableauo`.

  `(tableauo P X fuel cert)` holds when `cert` describes a closed P-tableau
  for the sentence X (Definition 6.1: a tableau built with the Branch
  Extension Rules of §4, the Equality Rules of §5 and the Procedure Call Rule
  of §6).

  - With `cert` fresh and `fuel` ground, it is a prover. It searches for a
    closed tableau whose branches use at most `fuel` choices each.
  - With `cert` ground (and `fuel` fresh), it is a checker. Every clause
    unifies the certificate's rule tag, or the shape of the sentence being
    expanded, before anything else, so a check does not search.

  Prover and checker are the same code, so they cannot disagree (ADR-0149,
  Decision §2).

  Purity
  ------
  Only `==`, `conde`, `fresh` and relations built from them (`defne`,
  `conso`, `appendo`, `membero`) are used. There is no `!=`, no nominal
  logic, no `fd`, no `conda`/`condu` and no `project`. Each side condition
  the paper states negatively is a lookup in a finite table built by
  `proflog.fitting.syntax`:

  - \"distinct constant/function symbols of L\" (Free Closure, §5);
  - \"an occurrence of the variable being substituted\" (§2), using the
    names table;
  - \"a new parameter\" (δ, §4): the branch counts its parameters, so
    `(par . k)` is new by construction.

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
  Fitting's completeness proof (Proposition 7.7) uses \"a systematic tableau
  construction\" in the style of Smullyan and of Fitting's own book, adapted
  to §5 and §6. The relation is that construction.

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

  The normal form, and why it loses nothing
  -----------------------------------------
  The relation accepts the tableaus of this construction only, not every
  closed tableau Fitting's rules allow. A tableau that applies rules in
  another order, or makes a step the restrictions below exclude, is
  rejected. For every closed P-tableau, though, there is one in this normal
  form, so t_P is unchanged (Theorem 7.2). The restrictions:

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
  - Each closed term is given to a γ sentence at most once on a branch,
    because a second instance adds nothing.
  - No literal is added twice, and no equation t = t is added.
  - Every closure uses a literal that is new since the previous saturation
    point, or one derived from such a literal. A closure from older literals
    alone was already possible at the previous saturation point with more
    fuel. The tableau that closes there is smaller, so nothing is lost. This
    also stops one branch from closing in the same way at every later
    saturation point, which would multiply the search for its β sibling.

  Fuel
  ----
  `fuel` is a unary list, `(1 1 1)`. Each γ application, procedure call,
  One-One step (which adds all n component equations) and Substitutivity
  step uses one unit, and a γ term uses one more unit for each function
  symbol in it, so γ(0) costs 1 and γ(s(s(0))) costs 3. The bound is per
  branch path, so each run is a finite search. The query API
  (`proflog.fitting.query`) runs the relation at a fuel bound. In a check,
  `fuel` is fresh and unconstrained."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :refer [== conde fresh defne lcons llist conso appendo membero]]))

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

(defn- distinct-nameso
  "The leaf s (a variable or a constant) is not the variable x."
  [P x s]
  (membero [x s] (:distinct-names P)))

(defn- distinct-relationso
  "R and S are distinct relation symbols of L (equality included)."
  [P R S]
  (membero [R S] (:distinct-relations (:language P))))

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

(defn- belowo
  "The unary number j is less than k."
  [j k]
  (fresh [k']
    (== k (lcons 1 k'))
    (conde
     [(== j k')]
     [(belowo j k')])))

(declare closed-termo)

(defn- closed-termso
  "ts is a list of closed terms, one for each 1 in arity, paid for from fuel
  with fuel' left over."
  [P k fuel arity ts fuel']
  (conde
   [(== arity ()) (== ts ()) (== fuel' fuel)]
   [(fresh [n u us fuel1]
      (== arity (lcons 1 n))
      (== ts (lcons u us))
      (closed-termo P k fuel u fuel1)
      (closed-termso P k fuel1 n us fuel'))]))

(defn closed-termo
  "t is a closed term built from the symbols of L and the parameters below
  k, the parameters already on the branch. Each function symbol in t costs
  one unit of fuel, and fuel' is what is left, so in prove mode this
  enumerates a finite set."
  [P k fuel t fuel']
  (conde
   [(constanto P t) (== fuel' fuel)]
   [(fresh [j] (== t (lcons 'par j)) (belowo j k) (== fuel' fuel))]
   [(fresh [fuel1 f arity ts]
      (== fuel (lcons 1 fuel1))
      (== t (lcons f ts))
      (functiono P f arity)
      (closed-termso P k fuel1 arity ts fuel'))]))

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

(defn- gamma-termo
  "t is the closed term of Lpar given to γ, and k' the branch's parameter
  count afterwards. The term is a closed term of L or a parameter already on
  the branch, or, where `new-parameter-allowedo` says so, the new parameter
  (par . k). fuel' is the fuel left after paying for the term's function
  symbols."
  [P k fuel t k' fuel']
  (conde
   [(closed-termo P k fuel t fuel') (== k' k)]
   [(new-parameter-allowedo P k)
    (== t (lcons 'par k))
    (== k' (lcons 1 k))
    (== fuel' fuel)]))

;;; =====================================================================
;;; Telling ground terms and literals apart
;;;
;;; The systematic construction never adds a literal that is already on the
;;; branch, and never adds an equation t = t (the only use of Reflexivity is
;;; closure against ¬(t = t)). On ground syntax "different" is structural, so
;;; it is a pure relation over the distinctness tables of L.

(defn- applicationo
  "t is f(t1,...,tn) for a function symbol f of L."
  [P t]
  (fresh [f arity ts]
    (== t (lcons f ts))
    (functiono P f arity)))

(defn- parametero
  "t is a parameter."
  [t]
  (fresh [k] (== t (lcons 'par k))))

(defn- differ-unaryo
  "The unary numbers j and k are different."
  [j k]
  (conde
   [(== j ()) (fresh [k'] (== k (lcons 1 k')))]
   [(== k ()) (fresh [j'] (== j (lcons 1 j')))]
   [(fresh [j' k']
      (== j (lcons 1 j'))
      (== k (lcons 1 k'))
      (differ-unaryo j' k'))]))

(declare differ-termso)

(defn differento
  "t and u are different closed terms of Lpar. The clauses are exclusive,
  so on ground terms there is at most one answer."
  [P t u]
  (conde
   [(distinct-constantso P t u)]
   [(constanto P t) (applicationo P u)]
   [(applicationo P t) (constanto P u)]
   [(constanto P t) (parametero u)]
   [(parametero t) (constanto P u)]
   [(applicationo P t) (parametero u)]
   [(parametero t) (applicationo P u)]
   [(fresh [j k]
      (== t (lcons 'par j))
      (== u (lcons 'par k))
      (differ-unaryo j k))]
   [(fresh [f g ts us]
      (== t (lcons f ts))
      (== u (lcons g us))
      (distinct-functionso P f g))]
   [(fresh [f arity ts us]
      (== t (lcons f ts))
      (== u (lcons f us))
      (functiono P f arity)
      (differ-termso P ts us))]))

(defn- differ-termso
  "The equally long lists of closed terms ts and us differ at some place;
  the first such place is the one found."
  [P ts us]
  (fresh [t tr u ur]
    (conso t tr ts)
    (conso u ur us)
    (conde
     [(differento P t u)]
     [(== t u) (differ-termso P tr ur)])))

(defn- differ-atomso
  "A and B are different ground atoms."
  [P A B]
  (fresh [R S ts us]
    (== A (lcons R ts))
    (== B (lcons S us))
    (relationo P R)
    (relationo P S)
    (conde
     [(distinct-relationso P R S)]
     [(== R S) (differ-termso P ts us)])))

(defn- differ-literalso
  "X and Y are different ground literals."
  [P X Y]
  (conde
   [(differ-atomso P X Y)]
   [(atomo P X) (fresh [B] (== Y ['not B]))]
   [(fresh [A] (== X ['not A])) (atomo P Y)]
   [(fresh [A B]
      (== X ['not A])
      (== Y ['not B])
      (differ-atomso P A B))]))

(defn- not-on-brancho
  "The ground literal X is not among lits."
  [P X lits]
  (conde
   [(== lits ())]
   [(fresh [Y more]
      (conso Y more lits)
      (differ-literalso P X Y)
      (not-on-brancho P X more))]))

(defn- new-literalo
  "Adding X to a branch with literals lits adds something: X is not on the
  branch, and X is not an equation t = t."
  [P X lits]
  (fresh [R ts]
    (conde
     [(fresh [t u] (== X ['= t u]) (differento P t u))]
     [(== X (lcons R ts)) (distinct-relationso P R '=)]
     [(fresh [A] (== X ['not A]))])
    (not-on-brancho P X lits)))

(defn- add-new-literalso
  "lits' is lits extended by those of es that are new (`new-literalo`), and
  added lists exactly those."
  [P lits es lits' added]
  (conde
   [(== es ()) (== lits' lits) (== added ())]
   [(fresh [e more]
      (conso e more es)
      (conde
       ;; e = e, nothing to add.
       [(fresh [t] (== e ['= t t]))
        (add-new-literalso P lits more lits' added)]
       ;; e is already on the branch.
       [(fresh [t u] (== e ['= t u]) (differento P t u))
        (membero e lits)
        (add-new-literalso P lits more lits' added)]
       [(fresh [added']
          (new-literalo P e lits)
          (== added (lcons e added'))
          (add-new-literalso P (lcons e lits) more lits' added'))]))]))

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

(defn- equationso
  "es is (t1 = u1, ..., tn = un)."
  [ts us es]
  (conde
   [(== ts ()) (== us ()) (== es ())]
   [(fresh [t tr u ur er]
      (conso t tr ts)
      (conso u ur us)
      (conso ['= t u] er es)
      (equationso tr ur er))]))

(defn one-oneo
  "The One-One Rule (§5): from f(t1,...,tn) = f(u1,...,un), with f a
  function symbol of L, the branch may be extended by ti = ui. es holds all
  n of them, which is n applications of the rule (the systematic
  construction charges one unit of fuel for all of them)."
  [P e es]
  (fresh [f arity ts us]
    (== e ['= (lcons f ts) (lcons f us)])
    (functiono P f arity)
    (equationso ts us es)))

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
;;; The branch: Table 2 (§4), closure (§4, §5, §6), and the systematic
;;; construction (§7)

(declare brancho)

(defn- callo
  "The Procedure Call Rule (§6). A tableau branch is closed if:

  1. it contains a ground atom R(t1,...,tn) of L, there is a clause
     R(x1,...,xn) ← ϕ(x1,...,xn) in P, and there exists a closed tableau for
     ϕ(t1,...,tn);
  2. it contains a negated ground atom ¬R(t1,...,tn) of L, there is such a
     clause, and there exists a closed tableau for ¬ϕ(t1,...,tn).

  \"The ground atom R(t1,...,tn) or the negated ground atom ¬R(t1,...,tn),
  must be of L, not of Lpar\": `l-termso` rejects parameters. Equality has no
  clause, so it is never called. cert describes the subsidiary tableau."
  [P A fuel cert]
  (fresh [R ts xs phi root]
    (conde
     [(== A (lcons R ts))
      (clauseo P R xs phi)
      (l-termso P ts)
      (instanceo P phi xs ts root)]
     [(fresh [phi-t]
        (== A ['not (lcons R ts)])
        (clauseo P R xs phi)
        (l-termso P ts)
        (instanceo P phi xs ts phi-t)
        (== root ['not phi-t]))])
    ;; A subsidiary tableau is a new tableau for a sentence of L.
    (brancho P [root] () () () () fuel cert)))

;;; Every closure, and every literal an equality step derives, uses the
;;; branch's literals. `new` holds those added since the branch was last
;;; saturated, together with those derived from them. A closure that uses
;;; only older literals was already possible at the previous saturation point,
;;; with at least as much fuel, so the systematic construction requires each
;;; closure to use a new literal. This loses no closed tableau, and it stops
;;; one branch from closing in the same way at every later saturation point.

(defn- new-premiseo
  "Of the two premises A and B, both among lits, at least one is new. The
  clauses are exclusive. Each finds its premises with `membero` before
  testing them, so that the tests run on ground literals."
  [P A B lits new]
  (conde
   [(membero A new) (membero B lits)]
   [(membero B new) (membero A lits) (not-on-brancho P A new)]))

(defn- conclusiono
  "A literal X derived from premises A and B: lits' and new' are the
  branch's literals with X added, and X counts as new when a premise is."
  [P A B X lits new lits' new']
  (conde
   [(new-premiseo P A B lits new)
    (== lits' (lcons X lits))
    (== new' (lcons X new))]
   [(not-on-brancho P A new)
    (not-on-brancho P B new)
    (membero A lits)
    (membero B lits)
    (== lits' (lcons X lits))
    (== new' new)]))

(defn- closeo
  "The saturated branch with literals lits, of which new are new, is
  closed. cert says how."
  [P lits new fuel cert]
  (conde
   ;; §4: "a branch is closed if it contains ... both A and ¬A for some
   ;; sentence A."
   [(fresh [A]
      (== cert [:close A])
      (new-premiseo P A ['not A] lits new))]
   ;; §5 Reflexivity: "for any closed term t of Lpar, t = t can be added to
   ;; the end of any tableau branch". With ¬(t = t) on the branch, the
   ;; branch then has A and ¬A.
   [(fresh [t]
      (== cert [:reflexivity t])
      (membero ['not ['= t t]] new))]
   ;; §5 Free Closure.
   [(fresh [e]
      (== cert [:free-closure e])
      (membero e new)
      (free-closureo P e))]
   ;; §6 Procedure Call Rule.
   [(fresh [A sub fuel']
      (== cert [:call A sub])
      (== fuel (lcons 1 fuel'))
      (membero A new)
      (callo P A fuel' sub))]
   ;; §5 One-One: extend the branch with those ti = ui that are new, then
   ;; close it. The added equations are new exactly when e is, so `new`
   ;; stays a part of `lits`.
   [(fresh [e es lits' new' added rest fuel']
      (== cert [:one-one e rest])
      (== fuel (lcons 1 fuel'))
      (membero e lits)
      (one-oneo P e es)
      (add-new-literalso P lits es lits' added)
      (fresh [first-added more] (== added (lcons first-added more)))
      (conde
       [(membero e new) (appendo added new new')]
       [(not-on-brancho P e new) (== new' new)])
      (closeo P lits' new' fuel' rest))]
   ;; §5 Substitutivity: extend the branch with X', if it is new, then close
   ;; it.
   [(fresh [e X X' lits' new' rest fuel']
      (== cert [:substitutivity e X X' rest])
      (== fuel (lcons 1 fuel'))
      (membero e lits)
      (membero X lits)
      (substitutivityo P e X X')
      (new-literalo P X' lits)
      (conclusiono P e X X' lits new lits' new')
      (closeo P lits' new' fuel' rest))]))

(defn- unusedo
  "The closed term t is none of the terms in used."
  [P t used]
  (conde
   [(== used ())]
   [(fresh [u more]
      (conso u more used)
      (differento P t u)
      (unusedo P t more))]))

(defn- saturatedo
  "The branch has expanded everything but its γ sentences. It closes, or γ
  is applied once more (Table 2: γ / γ(t), for any closed term t of Lpar).

  Each entry of gammas is [γ used], with used the terms γ has already been
  given on this branch. A second instance with the same term would add
  nothing, so the term must be unused."
  [P gammas lits new k fuel cert]
  (conde
   [(closeo P lits new fuel cert)]
   [(fresh [gamma used others x matrix t instance fuel1 fuel' k' gammas' rest]
      (== cert [:gamma t rest])
      (== fuel (lcons 1 fuel1))
      (conso [gamma used] others gammas)
      (gammao gamma x matrix)
      (gamma-termo P k fuel1 t k' fuel')
      (unusedo P t used)
      (substo P matrix x t instance)
      ;; The γ sentence stays on the branch for later use, at the back of
      ;; the queue. The branch is saturated, so nothing is new any more.
      (appendo others [[gamma (lcons t used)]] gammas')
      (brancho P [instance] gammas' lits () k' fuel' rest))]))

(defn- expando
  "Expand Z, the head of the branch's queue, by the rule of Table 2 that
  fits it. The rest of the queue is todo."
  [P Z todo gammas lits new k fuel cert]
  (conde
   ;; ¬¬Z / Z
   [(fresh [Y rest]
      (== cert [:not-not rest])
      (== Z ['not ['not Y]])
      (brancho P (lcons Y todo) gammas lits new k fuel rest))]
   ;; ¬true / false
   [(fresh [rest]
      (== cert [:not-true rest])
      (== Z ['not true])
      (brancho P (lcons false todo) gammas lits new k fuel rest))]
   ;; ¬false / true
   [(fresh [rest]
      (== cert [:not-false rest])
      (== Z ['not false])
      (brancho P (lcons true todo) gammas lits new k fuel rest))]
   ;; α / α1, α2
   [(fresh [a1 a2 rest]
      (== cert [:alpha rest])
      (alphao Z a1 a2)
      (brancho P (llist a1 a2 todo) gammas lits new k fuel rest))]
   ;; β / β1 | β2
   [(fresh [b1 b2 left right]
      (== cert [:beta left right])
      (betao Z b1 b2)
      (brancho P (lcons b1 todo) gammas lits new k fuel left)
      (brancho P (lcons b2 todo) gammas lits new k fuel right))]
   ;; δ / δ(p), "for a new parameter p": "p is to be a parameter that has
   ;; not previously occurred on the branch". The branch has introduced the
   ;; parameters below k, and no other parameter can be on it (its root is a
   ;; sentence of L), so (par . k) is new.
   [(fresh [x matrix instance rest]
      (== cert [:delta rest])
      (deltao Z x matrix)
      (substo P matrix x (lcons 'par k) instance)
      (brancho P (lcons instance todo) gammas lits new (lcons 1 k) fuel rest))]
   ;; "a branch is closed if it contains an explicit contradiction: false"
   [(== cert [:false])
    (== Z false)]
   ;; A γ sentence is kept and applied when the branch is saturated.
   [(fresh [x matrix gammas']
      (gammao Z x matrix)
      (appendo gammas [[Z ()]] gammas')
      (brancho P todo gammas' lits new k fuel cert))]
   ;; true: no rule applies, and it cannot close a branch.
   [(== Z true)
    (brancho P todo gammas lits new k fuel cert)]
   ;; A literal joins the branch's literals, as a new one.
   [(literalo P Z)
    (brancho P todo gammas (lcons Z lits) (lcons Z new) k fuel cert)]))

(defn- brancho
  "The branch with queue todo, γ sentences gammas, literals lits (new of
  them new since it was last saturated) and parameter count k closes, as
  cert describes."
  [P todo gammas lits new k fuel cert]
  (conde
   [(== todo ())
    (saturatedo P gammas lits new k fuel cert)]
   [(fresh [Z more]
      (conso Z more todo)
      (expando P Z more gammas lits new k fuel cert))]))

;;; =====================================================================
;;; The relation

(defn tableauo
  "cert describes a closed P-tableau for the sentence X of L (§4, §6).

  P is a program from `proflog.fitting.syntax/program`. Its names table must
  cover X, which `syntax/with-sentence` arranges. With cert ground, this
  checks it. With cert fresh and fuel a ground unary list, it searches for a
  certificate whose branches use at most that much fuel."
  [P X fuel cert]
  (brancho P [X] () () () () fuel cert))

(defn fuel
  "The unary fuel list for n."
  [n]
  (apply list (repeat n 1)))
