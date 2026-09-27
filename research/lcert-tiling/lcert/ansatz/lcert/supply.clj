(ns lcert.supply
  "Step 2 of proflog ADR-0143, machine-checked: the sizes behind the primitive
  destructor and lazy finite supplies (METATHEORY.md §2).

  Loading this namespace runs Ansatz's elaborator and kernel on every
  definition and theorem below.  It builds on lcert.verified (codes CT and
  `nodes`) and lcert.charged (the embedded semantic sets VF).

  Codes are CT values, as in lcert.verified: a leaf [:sl l] is
  (cell l false tend tend) and a node [:sn l a b] is (cell l true a b).  A
  certificate has the shape of its code (in the model a token carries no
  information), so a CT value also stands for a certificate.

  What is modelled.
    spine k        the supply a lazy supply of k nodes denotes: a right spine
                   of k nodes whose left children are leaves (lcert.eval's
                   lazy-supply, label 0 for :a)
    parse c s      lcert.examples/parse-prim-form, recursion for recursion:
                   a leaf code costs nothing; a node code takes the supply's
                   top node apart (its token becomes the certificate node's),
                   discards its left child, and threads the rest through the
                   two subcodes, left first; an exhausted supply yields a leaf
    wfc c          c is a well-formed code: leaves have no children
    ParseOK c      from every spine of N ≥ nodes(c) nodes, parse returns c
                   itself and the spine of N − nodes(c)

  Verified here:
    spine_nodes     nodes (spine k) = k: the promised size is the real size
    parse_spine     every well-formed code is ParseOK: minting a code of k
                    nodes consumes exactly k supply nodes and leaves the rest
                    a spine — the materialization bound of METATHEORY.md §2.3
    caser_node_env  the caseR case of the fundamental lemma: a node of
                    footprint k splits into its token (footprint 1) and its
                    children (their own sizes), 1 + ‖v₁‖ + ‖v₂‖ ≤ k

  How the proofs are written.  As in lcert.charged; in addition, `exfalso`
  drops hypotheses introduced by `intro` in this Ansatz version, so a case
  that needs it is a separate lemma whose hypothesis is a parameter."
  (:require [ansatz.core :as a]
            [ansatz.kernel.env :as env]
            [ansatz.kernel.expr :as ke]
            [ansatz.kernel.name :as nm]
            [lcert.verified]
            [lcert.charged]))

;; ---------------------------------------------------------------------------
;; Kernel helpers.

(defn declared?
  "Is the named declaration in the Ansatz kernel environment?"
  [s]
  (some? (env/lookup (a/env) (nm/from-string s))))

(defn reverifies?
  "Re-run the kernel's full check of the named theorem's stored proof term."
  [s]
  (let [ci (env/lookup (a/env) (nm/from-string s))]
    (boolean (and ci (env/verifies? (a/env) (.type ci) (.value ci))))))

(defn statement "The named declaration's type, as a string." [s]
  (ke/->string (.type (env/lookup (a/env) (nm/from-string s)))))

(defn definition "The named definition's body, as a string." [s]
  (ke/->string (.value (env/lookup (a/env) (nm/from-string s)))))

(defmacro ^:private kdef
  "Define a Prop-valued function in the kernel only (see lcert.charged/kdef)."
  [nm & more]
  `(do (try (a/defn ~nm ~@more) (catch Throwable _# nil))
       (when-not (declared? ~(str nm))
         (throw (ex-info (str "kernel definition failed: " '~nm) {:name '~nm})))
       '~nm))

;; ---------------------------------------------------------------------------
;; The spine supply.

(a/defn spine [k :- Nat] CT
  (match k
    [zero (CT.cell 0 false CT.tend CT.tend)]
    [(succ j) (CT.cell 0 true (CT.cell 0 false CT.tend CT.tend) (spine j))]))

(a/theorem spine_nodes_zero [] (= (nodes (spine 0)) 0) (rfl))

(a/theorem spine_nodes_succ [j :- Nat, ih :- (= (nodes (spine j)) j)]
  (= (nodes (spine (+ j 1))) (+ j 1))
  (change (= (+ 1 (+ (nodes (CT.cell 0 false CT.tend CT.tend)) (nodes (spine j)))) (+ j 1)))
  (omega))

(a/theorem spine_nodes [k :- Nat] (= (nodes (spine k)) k)
  (induction k)
  (exact spine_nodes_zero)
  (exact (spine_nodes_succ n ih_n)))

;; ---------------------------------------------------------------------------
;; The parser.  pfin builds the certificate node from its first subtree and
;; the second subtree's result; pcont threads the rest of the supply from the
;; first subcode into the second; ptake takes the supply's top node apart.

(a/defn pfin [l :- Nat, t1 :- CT, r2 :- (Prod CT CT)] (Prod CT CT)
  (Prod.mk (CT.cell l true t1 (Prod.fst r2)) (Prod.snd r2)))

(a/defn pcont [l :- Nat, r1 :- (Prod CT CT), py :- (arrow CT (Prod CT CT))] (Prod CT CT)
  (pfin l (Prod.fst r1) (py (Prod.snd r1))))

(a/defn ptake [l :- Nat, px :- (arrow CT (Prod CT CT)), py :- (arrow CT (Prod CT CT)), s :- CT] (Prod CT CT)
  (match s
    [tend (Prod.mk (CT.cell l false CT.tend CT.tend) CT.tend)]
    [(cell l2 i2 x2 rest) (if i2 (pcont l (px rest) py) (Prod.mk (CT.cell l false CT.tend CT.tend) s))]))

(a/defn pnode [l :- Nat, i :- Bool, px :- (arrow CT (Prod CT CT)), py :- (arrow CT (Prod CT CT)), s :- CT]
  (Prod CT CT)
  (if i (ptake l px py s) (Prod.mk (CT.cell l false CT.tend CT.tend) s)))

(a/defn parse [c :- CT] (arrow CT (Prod CT CT))
  (match c
    [tend (lam [s CT] (Prod.mk CT.tend s))]
    [(cell l i x y) (lam [s CT] (pnode l i (parse x) (parse y) s))]))

;; ---------------------------------------------------------------------------
;; Well-formed codes, and the parser's exact consumption.

(kdef wfc [c :- CT] Prop
  (CT.rec (lam [x CT] Prop) False
    (lam [l Nat, i Bool, x CT, y CT, ix Prop, iy Prop]
      (Bool.rec (lam [b Bool] Prop) (And (= x CT.tend) (= y CT.tend)) (And ix iy) i))
    c))

(kdef ParseOK [c :- CT] Prop
  (forall [N Nat] (=> (<= (nodes c) N) (= (parse c (spine N)) (Prod.mk c (spine (- N (nodes c))))))))

(a/theorem parse_leaf [l :- Nat, N :- Nat]
  (= (parse (CT.cell l false CT.tend CT.tend) (spine N))
     (Prod.mk (CT.cell l false CT.tend CT.tend) (spine (- N (nodes (CT.cell l false CT.tend CT.tend))))))
  (change (= (Prod.mk (CT.cell l false CT.tend CT.tend) (spine N))
             (Prod.mk (CT.cell l false CT.tend CT.tend) (spine (- N 0)))))
  (rewrite Nat.sub_zero)
  (rfl))

;; A node code from a spine of M + 1: the top supply node is spent on the
;; certificate's node, the left subcode is minted from the spine of M, the
;; right one from what that leaves.
(a/theorem parse_node [l :- Nat, x :- CT, y :- CT, M :- Nat,
                       hx :- (= (parse x (spine M)) (Prod.mk x (spine (- M (nodes x))))),
                       hy :- (forall [K Nat] (=> (<= (nodes y) K)
                                                 (= (parse y (spine K)) (Prod.mk y (spine (- K (nodes y))))))),
                       hle :- (<= (+ (nodes x) (nodes y)) M)]
  (= (parse (CT.cell l true x y) (spine (+ M 1)))
     (Prod.mk (CT.cell l true x y) (spine (- (+ M 1) (nodes (CT.cell l true x y))))))
  (change (= (pfin l (Prod.fst (parse x (spine M))) (parse y (Prod.snd (parse x (spine M)))))
             (Prod.mk (CT.cell l true x y) (spine (- (+ M 1) (+ 1 (+ (nodes x) (nodes y))))))))
  (rewrite hx)
  (change (= (pfin l x (parse y (spine (- M (nodes x)))))
             (Prod.mk (CT.cell l true x y) (spine (- (+ M 1) (+ 1 (+ (nodes x) (nodes y))))))))
  (have hyk (<= (nodes y) (- M (nodes x)))) (omega)
  (rewrite (hy (- M (nodes x)) hyk))
  (change (= (Prod.mk (CT.cell l true x y) (spine (- (- M (nodes x)) (nodes y))))
             (Prod.mk (CT.cell l true x y) (spine (- (+ M 1) (+ 1 (+ (nodes x) (nodes y))))))))
  (have e (= (- (- M (nodes x)) (nodes y)) (- (+ M 1) (+ 1 (+ (nodes x) (nodes y)))))) (omega)
  (rewrite e)
  (rfl))

(a/theorem parse_spine_tend [hw :- (wfc CT.tend)] (ParseOK CT.tend)
  (exfalso) (exact hw))

(a/theorem parse_spine_false [l :- Nat, x :- CT, y :- CT, hw :- (wfc (CT.cell l false x y))]
  (ParseOK (CT.cell l false x y))
  (intro N hN)
  (rewrite (And.left hw))
  (rewrite (And.right hw))
  (exact (parse_leaf l N)))

(a/theorem parse_spine_true [l :- Nat, x :- CT, y :- CT,
                             ihx :- (=> (wfc x) (ParseOK x)), ihy :- (=> (wfc y) (ParseOK y)),
                             hw :- (wfc (CT.cell l true x y))]
  (ParseOK (CT.cell l true x y))
  (intro N hN)
  (have hN2 (<= (+ 1 (+ (nodes x) (nodes y))) N)) (exact hN)
  (have h1 (<= (nodes x) (- N 1))) (omega)
  (have h2 (<= (+ (nodes x) (nodes y)) (- N 1))) (omega)
  (have e (= N (+ (- N 1) 1))) (omega)
  (rewrite e)
  (exact (parse_node l x y (- N 1) (ihx (And.left hw) (- N 1) h1) (ihy (And.right hw)) h2)))

(a/theorem parse_spine_cell [l :- Nat, i :- Bool, x :- CT, y :- CT,
                             ihx :- (=> (wfc x) (ParseOK x)), ihy :- (=> (wfc y) (ParseOK y))]
  (=> (wfc (CT.cell l i x y)) (ParseOK (CT.cell l i x y)))
  (cases i)
  (exact (parse_spine_false l x y))
  (exact (parse_spine_true l x y ihx ihy)))

(a/theorem parse_spine [c :- CT] (=> (wfc c) (ParseOK c))
  (induction c)
  (exact parse_spine_tend)
  (exact (parse_spine_cell lbl inner x y ih_x ih_y)))

;; ---------------------------------------------------------------------------
;; The caseR case of the fundamental lemma (lcert.charged's sets).  The node
;; branch binds the token at footprint 1 and the children at their own sizes;
;; together they fit the scrutinee's footprint.  The leaf branch binds only a
;; label, which has no footprint.

(a/theorem caser_node_env [n :- Nat, k :- Nat, l :- Nat, x :- CT, y :- CT, tok :- Nat,
                           hv :- (VF STy.tcert n k (CT.cell l true x y))]
  (And (VF STy.ttok n 1 tok)
       (And (VF STy.tcert n (nodes x) x)
            (And (VF STy.tcert n (nodes y) y)
                 (<= (+ 1 (+ (nodes x) (nodes y))) k))))
  (exact (And.intro (Nat.le_refl 1)
                    (And.intro (Nat.le_refl (nodes x))
                               (And.intro (Nat.le_refl (nodes y)) hv)))))

(def theorem-names
  ["spine_nodes_zero" "spine_nodes_succ" "spine_nodes"
   "parse_leaf" "parse_node" "parse_spine_tend" "parse_spine_false" "parse_spine_true"
   "parse_spine_cell" "parse_spine" "caser_node_env"])
