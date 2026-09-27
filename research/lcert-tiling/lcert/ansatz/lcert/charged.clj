(ns lcert.charged
  "Step 1 of proflog ADR-0143, machine-checked: the Reflect case of the
  fundamental lemma under the caller-charged cap, for every type of the
  first-order resource class 𝓕, which contains the agent types.  The paper
  proof is METATHEORY.md §1 (in research/lcert-tiling/).

  Loading this namespace runs Ansatz's elaborator and its Lean-4-compatible
  kernel on every definition and theorem below; a proof the kernel rejects
  aborts the load.  It builds on lcert.verified (codes CT, `nodes`, `budget`,
  and the strict-overhead lemma).

  What is modelled.  R4-metatheory.md §3.3 defines semantic sets Vⁿₖ(A): the
  values of type A that account for k tokens, read at the budget cap n.  Here
  those sets are defined over an embedded, non-dependent type language STy:

    tdat         ordinary data (any R-free, ◇-free type; the set is everything)
    tgood        the agent's result Σ(b :ω Act). Safe(b), as Nat with 0 < x
    tcert        R, certificates: codes CT with at most k internal nodes
    ttok         ◇, a token: present only when k ≥ 1
    tlol a b     Π(x :₁ A). B        tfun a b    Π(x :ω A). B
    tten a b     Σ(x :₁ A). B = A ⊗ B
    tbang a b    Σ(x :ω A). B, whose first component is reusable

  SCar t is the carrier (the set-theoretic values of the type's skeleton), and
  VF t n k x says x ∈ Vⁿₖ(t), clause by clause as in R4 §3.3.  Dependent
  types and T(b) are not embedded; the paper proof covers them, and they do
  not interact with the cap.

  The classes (METATHEORY.md §1.2; lcert.syntax implements them on real
  types):
    IsO  ordinary: no R, no ◇ anywhere;
    IsD  cap-free: IsO, R, ◇, and pairs of cap-free types;
    IsF  first-order: IsD; Π with an IsD input and an IsF result; Σ(ω) with an
         IsD first component and an IsF second; ⊗ with one side IsD and the
         other IsF.

  Verified here:
    VF_foot_mono         Vⁿₖ(t) ⊆ Vⁿₖ′(t) for k ≤ k′            (R4 Lemma 3.3)
    oindep               IsO t → the set ignores cap and footprint
    capfree              IsD t → the set ignores the cap
    o_sub_d, d_sub_f     O ⊆ D ⊆ 𝓕
    q_f                  Q: IsF t → Vⁿ′ₘ(t) ⊆ Vⁿₖ(t) whenever n′ ≤ n, m ≤ k ≤ n,
                         m ≤ n′ and n − n′ ≤ k − m
    charged_cap_lt       m < v ≤ n → n − v + m < n     (the recursion descends)
    charged_cap_fits     v ≤ n → m ≤ n − v + m         (Θₘ fits under it)
    charged_cap_code     the same descent for a derivation-shaped code, from
                         lcert.verified/strict_overhead
    reflect_case_charged THE KEY LEMMA: for every IsF type t, every
                         denotation `den` and derivability predicate that
                         satisfy the outer induction hypothesis at every
                         smaller cap, the reflected program's value at the
                         charged cap n − nodes(c) + budget(c) lies in Vⁿₖ(t)
                         whenever the certificate fits the footprint k ≤ n
    agent_in_F, minting_agent_in_F      the demo's agent types are in 𝓕
    shape_a_not_F, shape_b_not_F, shape_c_not_F
                         the three higher-order shapes of the assessment are
                         not (Step 3 treats them)

  How the proofs are written.  Ansatz 0.2.115 accepts a Type-valued or
  Prop-valued definition by its recursor in the kernel, but cannot compile it
  to a Clojure function; `kdef` tolerates that last step and checks that the
  constant is in the kernel.  Inductions handle their goals in constructor
  order, one lemma per case, because `try` does not catch every elaboration
  error.  Existentials are eliminated through ex_nat_elim, since the bundled
  Init has no Exists.elim."
  (:require [ansatz.core :as a]
            [ansatz.kernel.env :as env]
            [ansatz.kernel.expr :as ke]
            [ansatz.kernel.name :as nm]
            [lcert.verified]))

;; ---------------------------------------------------------------------------
;; Kernel helpers.

(defn declared?
  "Is the named declaration in the Ansatz kernel environment?"
  [s]
  (some? (env/lookup (a/env) (nm/from-string s))))

(defn reverifies?
  "Re-run the kernel's full check of the named theorem's stored proof term
  against its stored statement (ansatz.kernel.env/verifies?)."
  [s]
  (let [ci (env/lookup (a/env) (nm/from-string s))]
    (boolean (and ci (env/verifies? (a/env) (.type ci) (.value ci))))))

(defn statement
  "The named declaration's type, as a string."
  [s]
  (ke/->string (.type (env/lookup (a/env) (nm/from-string s)))))

(defmacro ^:private kdef
  "Define a Type- or Prop-valued function in the kernel only.  a/defn checks
  the definition in the kernel and then compiles it to a Clojure function;
  the compilation fails for a function whose values are types, after the
  kernel has accepted it.  That failure is ignored, and the constant's
  presence in the kernel environment is required instead."
  [nm & more]
  `(do (try (a/defn ~nm ~@more) (catch Throwable _# nil))
       (when-not (declared? ~(str nm))
         (throw (ex-info (str "kernel definition failed: " '~nm) {:name '~nm})))
       '~nm))

;; ---------------------------------------------------------------------------
;; Logic helpers the bundled Init lacks.

(a/theorem ex_nat_elim [P :- (arrow Nat Prop), Q :- Prop,
                        hex :- (Exists (lam [x Nat] (P x))),
                        hk :- (forall [x Nat] (=> (P x) Q))]
  Q
  (cases hex)
  (exact (hk w h)))

(a/theorem or_elim [P :- Prop, Q :- Prop, R :- Prop, hor :- (Or P Q), hp :- (=> P R), hq :- (=> Q R)]
  R
  (cases hor) (exact (hp h)) (exact (hq h)))

;; ---------------------------------------------------------------------------
;; The embedded types, their carriers, and the classes.

(a/inductive STy [] (tdat) (tgood) (tcert) (ttok)
  (tlol [a STy] [b STy]) (tfun [a STy] [b STy]) (tten [a STy] [b STy]) (tbang [a STy] [b STy]))

;; The carrier of a type's skeleton (R4 §3.1): certificates are codes (a
;; token carries no information in the model), functions are all functions.
(kdef SCar [t :- STy] Type
  (STy.rec (lam [x STy] Type) Nat Nat CT Nat
    (lam [a STy, b STy, ia Type, ib Type] (arrow ia ib))
    (lam [a STy, b STy, ia Type, ib Type] (arrow ia ib))
    (lam [a STy, b STy, ia Type, ib Type] (Prod ia ib))
    (lam [a STy, b STy, ia Type, ib Type] (Prod ia ib))
    t))

(kdef IsO [t :- STy] Prop
  (STy.rec (lam [x STy] Prop) True True False False
    (lam [a STy, b STy, ia Prop, ib Prop] (And ia ib))
    (lam [a STy, b STy, ia Prop, ib Prop] (And ia ib))
    (lam [a STy, b STy, ia Prop, ib Prop] (And ia ib))
    (lam [a STy, b STy, ia Prop, ib Prop] (And ia ib))
    t))

(kdef IsD [t :- STy] Prop
  (STy.rec (lam [x STy] Prop) True True True True
    (lam [a STy, b STy, ia Prop, ib Prop] (And (IsO a) (IsO b)))
    (lam [a STy, b STy, ia Prop, ib Prop] (And (IsO a) (IsO b)))
    (lam [a STy, b STy, ia Prop, ib Prop] (And ia ib))
    (lam [a STy, b STy, ia Prop, ib Prop] (And ia ib))
    t))

(kdef IsF [t :- STy] Prop
  (STy.rec (lam [x STy] Prop) True True True True
    (lam [a STy, b STy, ia Prop, ib Prop] (And (IsD a) ib))
    (lam [a STy, b STy, ia Prop, ib Prop] (And (IsD a) ib))
    (lam [a STy, b STy, ia Prop, ib Prop] (Or (And (IsD a) ib) (And ia (IsD b))))
    (lam [a STy, b STy, ia Prop, ib Prop] (And (IsD a) ib))
    t))

;; ---------------------------------------------------------------------------
;; R4 §3.3's semantic sets.  VF t n k x : x ∈ Vⁿₖ(t).
;;   Π(x :₁ A). B   f with f(a) ∈ Vₖ₊ⱼ(B) for every j ≤ n − k and a ∈ Vⱼ(A)
;;   Π(x :ω A). B   f with f(a) ∈ Vₖ(B) for every a ∈ V₀(A)
;;   A ⊗ B          (a, b) with a ∈ Vⱼ(A), b ∈ Vₖ₋ⱼ(B) for some j ≤ k
;;   Σ(x :ω A). B   (a, b) with a ∈ V₀(A), b ∈ Vₖ(B)

(kdef VF [t :- STy] (arrow Nat Nat (SCar t) Prop)
  (STy.rec (lam [t STy] (arrow Nat Nat (SCar t) Prop))
    (lam [n Nat, k Nat, x Nat] True)
    (lam [n Nat, k Nat, x Nat] (< 0 x))
    (lam [n Nat, k Nat, x CT] (<= (nodes x) k))
    (lam [n Nat, k Nat, x Nat] (<= 1 k))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          n Nat, k Nat, f (arrow (SCar a) (SCar b))]
      (forall [j Nat, x (SCar a)] (=> (<= (+ k j) n) (ia n j x) (ib n (+ k j) (f x)))))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          n Nat, k Nat, f (arrow (SCar a) (SCar b))]
      (forall [x (SCar a)] (=> (ia n 0 x) (ib n k (f x)))))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          n Nat, k Nat, p (Prod (SCar a) (SCar b))]
      (Exists (lam [j Nat] (And (<= j k) (And (ia n j (Prod.fst p)) (ib n (- k j) (Prod.snd p)))))))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          n Nat, k Nat, p (Prod (SCar a) (SCar b))]
      (And (ia n 0 (Prod.fst p)) (ib n k (Prod.snd p))))
    t))

;; ---------------------------------------------------------------------------
;; Footprint monotonicity (R4 Lemma 3.3): raising k shrinks the range of j in
;; a Π₁ clause and enlarges every target.

(a/theorem VF_foot_lol [a :- STy, b :- STy,
                        hb :- (forall [n Nat, k Nat, k2 Nat, x (SCar b)] (=> (<= k k2) (VF b n k x) (VF b n k2 x))),
                        n :- Nat, k :- Nat, k2 :- Nat, f :- (SCar (STy.tlol a b)),
                        h :- (<= k k2), hv :- (VF (STy.tlol a b) n k f)]
  (VF (STy.tlol a b) n k2 f)
  (intro j x hj hx)
  (have h1 (<= (+ k j) (+ k2 j))) (omega)
  (have h2 (<= (+ k j) n)) (omega)
  (exact (hb n (+ k j) (+ k2 j) (f x) h1 (hv j x h2 hx))))

(a/theorem VF_foot_fun [a :- STy, b :- STy,
                        hb :- (forall [n Nat, k Nat, k2 Nat, x (SCar b)] (=> (<= k k2) (VF b n k x) (VF b n k2 x))),
                        n :- Nat, k :- Nat, k2 :- Nat, f :- (SCar (STy.tfun a b)),
                        h :- (<= k k2), hv :- (VF (STy.tfun a b) n k f)]
  (VF (STy.tfun a b) n k2 f)
  (intro x hx)
  (exact (hb n k k2 (f x) h (hv x hx))))

(a/theorem VF_foot_ten [a :- STy, b :- STy,
                        hb :- (forall [n Nat, k Nat, k2 Nat, x (SCar b)] (=> (<= k k2) (VF b n k x) (VF b n k2 x))),
                        n :- Nat, k :- Nat, k2 :- Nat, p :- (SCar (STy.tten a b)),
                        h :- (<= k k2), hv :- (VF (STy.tten a b) n k p)]
  (VF (STy.tten a b) n k2 p)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j k) (And (VF a n j (Prod.fst p)) (VF b n (- k j) (Prod.snd p)))))
                      (VF (STy.tten a b) n k2 p)
                      hv
                      (lam [j Nat, hj (And (<= j k) (And (VF a n j (Prod.fst p)) (VF b n (- k j) (Prod.snd p))))]
                        (Exists.intro j (And.intro (Nat.le_trans (And.left hj) h)
                          (And.intro (And.left (And.right hj))
                                     (hb n (- k j) (- k2 j) (Prod.snd p) (Nat.sub_le_sub_right h j)
                                         (And.right (And.right hj))))))))))

(a/theorem VF_foot_bang [a :- STy, b :- STy,
                         hb :- (forall [n Nat, k Nat, k2 Nat, x (SCar b)] (=> (<= k k2) (VF b n k x) (VF b n k2 x))),
                         n :- Nat, k :- Nat, k2 :- Nat, p :- (SCar (STy.tbang a b)),
                         h :- (<= k k2), hv :- (VF (STy.tbang a b) n k p)]
  (VF (STy.tbang a b) n k2 p)
  (exact (And.intro (And.left hv) (hb n k k2 (Prod.snd p) h (And.right hv)))))

(a/theorem VF_foot_mono [t :- STy]
  (forall [n Nat, k Nat, k2 Nat, x (SCar t)] (=> (<= k k2) (VF t n k x) (VF t n k2 x)))
  (induction t)
  (all_goals (intro n k k2 x h hv))
  (all_goals (try (exact hv)))
  (exact (Nat.le_trans hv h))
  (exact (Nat.le_trans hv h))
  (exact (VF_foot_lol a b ih_b n k k2 x h hv))
  (exact (VF_foot_fun a b ih_b n k k2 x h hv))
  (exact (VF_foot_ten a b ih_b n k k2 x h hv))
  (exact (VF_foot_bang a b ih_b n k k2 x h hv)))

;; ---------------------------------------------------------------------------
;; Ordinary types ignore cap and footprint.  With k ≤ n the range j ≤ n − k
;; of a Π₁ clause is never empty, and an ordinary argument's set is the same
;; for every j, so the clause says the same at every cap.

(kdef OIndep [t :- STy] Prop
  (forall [n Nat, k Nat, n2 Nat, k2 Nat, x (SCar t)]
    (=> (<= k n) (<= k2 n2) (VF t n k x) (VF t n2 k2 x))))

(a/theorem oindep_dat [] (OIndep STy.tdat)
  (intro n k n2 k2 x hk hk2 hv) (exact hv))

(a/theorem oindep_good [] (OIndep STy.tgood)
  (intro n k n2 k2 x hk hk2 hv) (exact hv))

(a/theorem oindep_cert [ho :- (IsO STy.tcert)] (OIndep STy.tcert)
  (exfalso) (exact ho))

(a/theorem oindep_tok [ho :- (IsO STy.ttok)] (OIndep STy.ttok)
  (exfalso) (exact ho))

(a/theorem oindep_lol [a :- STy, b :- STy, ha :- (OIndep a), hb :- (OIndep b)]
  (OIndep (STy.tlol a b))
  (intro n k n2 k2 f hk hk2 hv j x hj hx)
  (have h1 (<= j n2)) (omega)
  (have h2 (<= (+ k 0) n)) (omega)
  (have h3 (<= (+ k2 j) n2)) (omega)
  (have hz (<= 0 n)) (omega)
  (exact (hb n (+ k 0) n2 (+ k2 j) (f x) h2 h3 (hv 0 x h2 (ha n2 j n 0 x h1 hz hx)))))

(a/theorem oindep_fun [a :- STy, b :- STy, ha :- (OIndep a), hb :- (OIndep b)]
  (OIndep (STy.tfun a b))
  (intro n k n2 k2 f hk hk2 hv x hx)
  (exact (hb n k n2 k2 (f x) hk hk2 (hv x (ha n2 0 n 0 x (Nat.zero_le n2) (Nat.zero_le n) hx)))))

(a/theorem oindep_ten [a :- STy, b :- STy, ha :- (OIndep a), hb :- (OIndep b)]
  (OIndep (STy.tten a b))
  (intro n k n2 k2 p hk hk2 hv)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j k) (And (VF a n j (Prod.fst p)) (VF b n (- k j) (Prod.snd p)))))
                      (VF (STy.tten a b) n2 k2 p)
                      hv
                      (lam [j Nat, hj (And (<= j k) (And (VF a n j (Prod.fst p)) (VF b n (- k j) (Prod.snd p))))]
                        (Exists.intro 0 (And.intro (Nat.zero_le k2)
                          (And.intro (ha n j n2 0 (Prod.fst p) (Nat.le_trans (And.left hj) hk) (Nat.zero_le n2)
                                         (And.left (And.right hj)))
                                     (hb n (- k j) n2 (- k2 0) (Prod.snd p) (Nat.le_trans (Nat.sub_le k j) hk)
                                         (Nat.le_trans (Nat.sub_le k2 0) hk2) (And.right (And.right hj))))))))))

(a/theorem oindep_bang [a :- STy, b :- STy, ha :- (OIndep a), hb :- (OIndep b)]
  (OIndep (STy.tbang a b))
  (intro n k n2 k2 p hk hk2 hv)
  (exact (And.intro (ha n 0 n2 0 (Prod.fst p) (Nat.zero_le n) (Nat.zero_le n2) (And.left hv))
                    (hb n k n2 k2 (Prod.snd p) hk hk2 (And.right hv)))))

(a/theorem oindep [t :- STy]
  (=> (IsO t) (OIndep t))
  (induction t)
  (intro ho) (exact oindep_dat)
  (intro ho) (exact oindep_good)
  (intro ho) (exact (oindep_cert ho))
  (intro ho) (exact (oindep_tok ho))
  (intro ho) (exact (oindep_lol a b (ih_a (And.left ho)) (ih_b (And.right ho))))
  (intro ho) (exact (oindep_fun a b (ih_a (And.left ho)) (ih_b (And.right ho))))
  (intro ho) (exact (oindep_ten a b (ih_a (And.left ho)) (ih_b (And.right ho))))
  (intro ho) (exact (oindep_bang a b (ih_a (And.left ho)) (ih_b (And.right ho)))))

;; ---------------------------------------------------------------------------
;; Cap-free types ignore the cap (not the footprint): R's and ◇'s sets never
;; mention it, ordinary Π-types ignore it, and pairs inherit it.

(kdef CapFree [t :- STy] Prop
  (forall [n Nat, n2 Nat, k Nat, x (SCar t)]
    (=> (<= k n) (<= k n2) (VF t n k x) (VF t n2 k x))))

(a/theorem capfree_dat [] (CapFree STy.tdat) (intro n n2 k x hk hk2 hv) (exact hv))
(a/theorem capfree_good [] (CapFree STy.tgood) (intro n n2 k x hk hk2 hv) (exact hv))
(a/theorem capfree_cert [] (CapFree STy.tcert) (intro n n2 k x hk hk2 hv) (exact hv))
(a/theorem capfree_tok [] (CapFree STy.ttok) (intro n n2 k x hk hk2 hv) (exact hv))

(a/theorem capfree_lol [a :- STy, b :- STy, ho :- (And (IsO a) (IsO b))] (CapFree (STy.tlol a b))
  (intro n n2 k x hk hk2 hv) (exact (oindep (STy.tlol a b) ho n k n2 k x hk hk2 hv)))

(a/theorem capfree_fun [a :- STy, b :- STy, ho :- (And (IsO a) (IsO b))] (CapFree (STy.tfun a b))
  (intro n n2 k x hk hk2 hv) (exact (oindep (STy.tfun a b) ho n k n2 k x hk hk2 hv)))

(a/theorem capfree_ten [a :- STy, b :- STy, ha :- (CapFree a), hb :- (CapFree b)] (CapFree (STy.tten a b))
  (intro n n2 k p hk hk2 hv)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j k) (And (VF a n j (Prod.fst p)) (VF b n (- k j) (Prod.snd p)))))
                      (VF (STy.tten a b) n2 k p)
                      hv
                      (lam [j Nat, hj (And (<= j k) (And (VF a n j (Prod.fst p)) (VF b n (- k j) (Prod.snd p))))]
                        (Exists.intro j (And.intro (And.left hj)
                          (And.intro (ha n n2 j (Prod.fst p) (Nat.le_trans (And.left hj) hk)
                                         (Nat.le_trans (And.left hj) hk2) (And.left (And.right hj)))
                                     (hb n n2 (- k j) (Prod.snd p) (Nat.le_trans (Nat.sub_le k j) hk)
                                         (Nat.le_trans (Nat.sub_le k j) hk2) (And.right (And.right hj))))))))))

(a/theorem capfree_bang [a :- STy, b :- STy, ha :- (CapFree a), hb :- (CapFree b)] (CapFree (STy.tbang a b))
  (intro n n2 k p hk hk2 hv)
  (exact (And.intro (ha n n2 0 (Prod.fst p) (Nat.zero_le n) (Nat.zero_le n2) (And.left hv))
                    (hb n n2 k (Prod.snd p) hk hk2 (And.right hv)))))

(a/theorem capfree [t :- STy]
  (=> (IsD t) (CapFree t))
  (induction t)
  (intro hd) (exact capfree_dat)
  (intro hd) (exact capfree_good)
  (intro hd) (exact capfree_cert)
  (intro hd) (exact capfree_tok)
  (intro hd) (exact (capfree_lol a b hd))
  (intro hd) (exact (capfree_fun a b hd))
  (intro hd) (exact (capfree_ten a b (ih_a (And.left hd)) (ih_b (And.right hd))))
  (intro hd) (exact (capfree_bang a b (ih_a (And.left hd)) (ih_b (And.right hd)))))

;; ---------------------------------------------------------------------------
;; O ⊆ D ⊆ 𝓕.

(a/theorem o_sub_d [t :- STy] (=> (IsO t) (IsD t))
  (induction t)
  (intro ho) (exact True.intro)
  (intro ho) (exact True.intro)
  (intro ho) (exact True.intro)
  (intro ho) (exact True.intro)
  (intro ho) (exact ho)
  (intro ho) (exact ho)
  (intro ho) (exact (And.intro (ih_a (And.left ho)) (ih_b (And.right ho))))
  (intro ho) (exact (And.intro (ih_a (And.left ho)) (ih_b (And.right ho)))))

(a/theorem d_sub_f [t :- STy] (=> (IsD t) (IsF t))
  (induction t)
  (intro hd) (exact True.intro)
  (intro hd) (exact True.intro)
  (intro hd) (exact True.intro)
  (intro hd) (exact True.intro)
  (intro hd) (exact (And.intro (o_sub_d a (And.left hd)) (ih_b (o_sub_d b (And.right hd)))))
  (intro hd) (exact (And.intro (o_sub_d a (And.left hd)) (ih_b (o_sub_d b (And.right hd)))))
  (intro hd) (exact (Or.inl (And.intro (And.left hd) (ih_b (And.right hd)))))
  (intro hd) (exact (And.intro (And.left hd) (ih_b (And.right hd)))))

;; ---------------------------------------------------------------------------
;; Q: the charged inclusion.  A reflected program is interpreted at the
;; charged cap n′ = n − ‖v‖ + m with its m tokens, and its caller needs the
;; value at cap n with the reflect term's footprint k ≥ ‖v‖.  The gap
;; n − n′ = ‖v‖ − m is covered by the extra footprint k − m.

(kdef QF [t :- STy] Prop
  (forall [n Nat, n2 Nat, m Nat, k Nat, x (SCar t)]
    (=> (<= n2 n) (<= m k) (<= k n) (<= m n2) (<= (- n n2) (- k m)) (VF t n2 m x) (VF t n k x))))

(a/theorem q_dat [] (QF STy.tdat) (intro n n2 m k x h1 h2 h3 h4 h5 hv) (exact hv))
(a/theorem q_good [] (QF STy.tgood) (intro n n2 m k x h1 h2 h3 h4 h5 hv) (exact hv))
(a/theorem q_cert [] (QF STy.tcert) (intro n n2 m k x h1 h2 h3 h4 h5 hv) (exact (Nat.le_trans hv h2)))
(a/theorem q_tok [] (QF STy.ttok) (intro n n2 m k x h1 h2 h3 h4 h5 hv) (exact (Nat.le_trans hv h2)))

;; A Π₁ with a cap-free input: every argument the caller may pass (j ≤ n − k)
;; is one the reflected function accepts (j ≤ n′ − m), and the result's gap
;; is unchanged.
(a/theorem q_lol [a :- STy, b :- STy, hda :- (IsD a), hb :- (QF b)] (QF (STy.tlol a b))
  (intro n n2 m k f h1 h2 h3 h4 h5 hv j x hj hx)
  (have e1 (<= j n)) (omega)
  (have e2 (<= j n2)) (omega)
  (have e3 (<= (+ m j) n2)) (omega)
  (have e4 (<= (+ m j) (+ k j))) (omega)
  (have e5 (<= (- n n2) (- (+ k j) (+ m j)))) (omega)
  (exact (hb n n2 (+ m j) (+ k j) (f x) h1 e4 hj e3 e5 (hv j x e3 (capfree a hda n n2 j x e1 e2 hx)))))

(a/theorem q_fun [a :- STy, b :- STy, hda :- (IsD a), hb :- (QF b)] (QF (STy.tfun a b))
  (intro n n2 m k f h1 h2 h3 h4 h5 hv x hx)
  (exact (hb n n2 m k (f x) h1 h2 h3 h4 h5
             (hv x (capfree a hda n n2 0 x (Nat.zero_le n) (Nat.zero_le n2) hx)))))

(a/theorem q_bang [a :- STy, b :- STy, hda :- (IsD a), hb :- (QF b)] (QF (STy.tbang a b))
  (intro n n2 m k p h1 h2 h3 h4 h5 hv)
  (exact (And.intro (capfree a hda n2 n 0 (Prod.fst p) (Nat.zero_le n2) (Nat.zero_le n) (And.left hv))
                    (hb n n2 m k (Prod.snd p) h1 h2 h3 h4 h5 (And.right hv)))))

;; A tensor whose first side is cap-free: keep the split j, and give the
;; whole gap to the second side.
(a/theorem q_ten_l_core [a :- STy, b :- STy, hda :- (IsD a), hb :- (QF b), n :- Nat, n2 :- Nat, m :- Nat, k :- Nat,
                         p :- (SCar (STy.tten a b)), j :- Nat,
                         h1 :- (<= n2 n), h2 :- (<= m k), h3 :- (<= k n), h4 :- (<= m n2), h5 :- (<= (- n n2) (- k m)),
                         hjm :- (<= j m), hja :- (VF a n2 j (Prod.fst p)), hjb :- (VF b n2 (- m j) (Prod.snd p))]
  (VF (STy.tten a b) n k p)
  (have e1 (<= j k)) (omega)
  (have e2 (<= j n2)) (omega)
  (have e3 (<= j n)) (omega)
  (have e4 (<= (- m j) (- k j))) (omega)
  (have e5 (<= (- k j) n)) (omega)
  (have e6 (<= (- m j) n2)) (omega)
  (have e7 (<= (- n n2) (- (- k j) (- m j)))) (omega)
  (exact (Exists.intro j (And.intro e1 (And.intro (capfree a hda n2 n j (Prod.fst p) e2 e3 hja)
                                                  (hb n n2 (- m j) (- k j) (Prod.snd p) h1 e4 e5 e6 e7 hjb))))))

;; A tensor whose second side is cap-free: give the whole gap to the first
;; side, by moving the split to j + (k − m).
(a/theorem q_ten_r_core [a :- STy, b :- STy, ha :- (QF a), hdb :- (IsD b), n :- Nat, n2 :- Nat, m :- Nat, k :- Nat,
                         p :- (SCar (STy.tten a b)), j :- Nat,
                         h1 :- (<= n2 n), h2 :- (<= m k), h3 :- (<= k n), h4 :- (<= m n2), h5 :- (<= (- n n2) (- k m)),
                         hjm :- (<= j m), hja :- (VF a n2 j (Prod.fst p)), hjb :- (VF b n2 (- m j) (Prod.snd p))]
  (VF (STy.tten a b) n k p)
  (have e1 (<= (+ j (- k m)) k)) (omega)
  (have e2 (<= j (+ j (- k m)))) (omega)
  (have e3 (<= (+ j (- k m)) n)) (omega)
  (have e4 (<= j n2)) (omega)
  (have e5 (<= (- n n2) (- (+ j (- k m)) j))) (omega)
  (have e6 (<= (- m j) n2)) (omega)
  (have e7 (<= (- m j) n)) (omega)
  (have e8 (<= (- m j) (- k (+ j (- k m))))) (omega)
  (exact (Exists.intro (+ j (- k m))
           (And.intro e1 (And.intro (ha n n2 j (+ j (- k m)) (Prod.fst p) h1 e2 e3 e4 e5 hja)
                                    (VF_foot_mono b n (- m j) (- k (+ j (- k m))) (Prod.snd p) e8
                                                  (capfree b hdb n2 n (- m j) (Prod.snd p) e6 e7 hjb)))))))

(a/theorem q_ten_l [a :- STy, b :- STy, hda :- (IsD a), hb :- (QF b)] (QF (STy.tten a b))
  (intro n n2 m k p h1 h2 h3 h4 h5 hv)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j m) (And (VF a n2 j (Prod.fst p)) (VF b n2 (- m j) (Prod.snd p)))))
                      (VF (STy.tten a b) n k p) hv
                      (lam [j Nat, hj (And (<= j m) (And (VF a n2 j (Prod.fst p)) (VF b n2 (- m j) (Prod.snd p))))]
                        (q_ten_l_core a b hda hb n n2 m k p j h1 h2 h3 h4 h5
                                      (And.left hj) (And.left (And.right hj)) (And.right (And.right hj)))))))

(a/theorem q_ten_r [a :- STy, b :- STy, ha :- (QF a), hdb :- (IsD b)] (QF (STy.tten a b))
  (intro n n2 m k p h1 h2 h3 h4 h5 hv)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j m) (And (VF a n2 j (Prod.fst p)) (VF b n2 (- m j) (Prod.snd p)))))
                      (VF (STy.tten a b) n k p) hv
                      (lam [j Nat, hj (And (<= j m) (And (VF a n2 j (Prod.fst p)) (VF b n2 (- m j) (Prod.snd p))))]
                        (q_ten_r_core a b ha hdb n n2 m k p j h1 h2 h3 h4 h5
                                      (And.left hj) (And.left (And.right hj)) (And.right (And.right hj)))))))

(a/theorem q_ten [a :- STy, b :- STy, iha :- (=> (IsF a) (QF a)), ihb :- (=> (IsF b) (QF b)),
                  hf :- (IsF (STy.tten a b))]
  (QF (STy.tten a b))
  (exact (or_elim (And (IsD a) (IsF b)) (And (IsF a) (IsD b)) (QF (STy.tten a b)) hf
           (lam [h (And (IsD a) (IsF b))] (q_ten_l a b (And.left h) (ihb (And.right h))))
           (lam [h (And (IsF a) (IsD b))] (q_ten_r a b (iha (And.left h)) (And.right h))))))

(a/theorem q_f [t :- STy] (=> (IsF t) (QF t))
  (induction t)
  (intro hf) (exact q_dat)
  (intro hf) (exact q_good)
  (intro hf) (exact q_cert)
  (intro hf) (exact q_tok)
  (intro hf) (exact (q_lol a b (And.left hf) (ih_b (And.right hf))))
  (intro hf) (exact (q_fun a b (And.left hf) (ih_b (And.right hf))))
  (intro hf) (exact (q_ten a b ih_a ih_b hf))
  (intro hf) (exact (q_bang a b (And.left hf) (ih_b (And.right hf)))))

;; ---------------------------------------------------------------------------
;; The charged cap, and the Reflect case.

(a/theorem charged_cap_lt [n :- Nat, v :- Nat, m :- Nat, hm :- (< m v), hv :- (<= v n)]
  (< (+ (- n v) m) n) (omega))

(a/theorem charged_cap_fits [n :- Nat, v :- Nat, m :- Nat, hv :- (<= v n)]
  (<= m (+ (- n v) m)) (omega))

(a/theorem charged_cap_code [l :- Nat, j :- CT, p :- CT, n :- Nat,
                             hc :- (<= (nodes (CT.cell l true j p)) n)]
  (< (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) n)
  (have hs (< (budget (CT.cell l true j p)) (nodes (CT.cell l true j p))))
  (exact (strict_overhead l j p))
  (omega))

;; The Reflect case of the fundamental lemma, for every type of 𝓕.
;;   den n′ c        the model's value, at cap n′, of the program decoded from
;;                   code c, run on the all-token environment Θ_budget(c)
;;   derives c       c is a derivation Check accepts at the type
;;   ih              the OUTER induction hypothesis: at every smaller cap, a
;;                   derivable code's program lies in V^n′_budget(c)
;;   c = cell l true j p   a derivation-shaped code (its root is a rule node)
;;   nodes c ≤ k ≤ n  the certificate fits the reflect term's footprint
;; Conclusion: the value at the charged cap lies in Vⁿₖ.
(a/theorem reflect_case_charged
  [t :- STy, hf :- (IsF t), den :- (arrow Nat CT (SCar t)), derives :- (arrow CT Prop),
   n :- Nat, k :- Nat, l :- Nat, j :- CT, p :- CT,
   ih :- (forall [n2 Nat, c CT] (=> (< n2 n) (derives c) (<= (budget c) n2) (VF t n2 (budget c) (den n2 c)))),
   hd :- (derives (CT.cell l true j p)),
   hk :- (<= (nodes (CT.cell l true j p)) k),
   hkn :- (<= k n)]
  (VF t n k (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) (CT.cell l true j p)))
  (have hs (< (budget (CT.cell l true j p)) (nodes (CT.cell l true j p))))
  (exact (strict_overhead l j p))
  (have e1 (< (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) n)) (omega)
  (have e2 (<= (budget (CT.cell l true j p)) (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))))) (omega)
  (have e3 (<= (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) n)) (omega)
  (have e4 (<= (budget (CT.cell l true j p)) k)) (omega)
  (have e5 (<= (- n (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))))
               (- k (budget (CT.cell l true j p))))) (omega)
  (exact (q_f t hf n (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p)))
              (budget (CT.cell l true j p)) k
              (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) (CT.cell l true j p))
              e3 e4 hkn e2 e5
              (ih (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p)))
                  (CT.cell l true j p) e1 hd e2))))

;; ---------------------------------------------------------------------------
;; Which types are in the class.

;; Agent = Π(s :₁ R). Σ(b :ω Act). Safe(b)
(a/theorem agent_in_F [] (IsF (STy.tlol STy.tcert STy.tgood))
  (exact (And.intro True.intro True.intro)))

;; A minting agent, handed a code and a supply: Π(cs :ω Syn). Agent
(a/theorem minting_agent_in_F [] (IsF (STy.tfun STy.tdat (STy.tlol STy.tcert STy.tgood)))
  (exact (And.intro True.intro (And.intro True.intro True.intro))))

;; (a) (H ⊸ Σ) ⊸ Σ, with H = R ⊸ Σ a certificate consumer
(a/theorem shape_a_not_F [h :- (IsF (STy.tlol (STy.tlol STy.tcert STy.tgood) STy.tgood))] False
  (exact (And.left (And.left h))))

;; (b) 1 ⊸ H ⊗ H
(a/theorem shape_b_not_F [h :- (IsF (STy.tlol STy.tdat (STy.tten (STy.tlol STy.tcert STy.tgood)
                                                                 (STy.tlol STy.tcert STy.tgood))))]
  False
  (exact (or_elim (And (IsD (STy.tlol STy.tcert STy.tgood)) (IsF (STy.tlol STy.tcert STy.tgood)))
                  (And (IsF (STy.tlol STy.tcert STy.tgood)) (IsD (STy.tlol STy.tcert STy.tgood)))
                  False (And.right h)
                  (lam [x (And (IsD (STy.tlol STy.tcert STy.tgood)) (IsF (STy.tlol STy.tcert STy.tgood)))]
                    (And.left (And.left x)))
                  (lam [x (And (IsF (STy.tlol STy.tcert STy.tgood)) (IsD (STy.tlol STy.tcert STy.tgood)))]
                    (And.left (And.right x))))))

;; (c) 1 ⊸ Σ(f :ω H). 1
(a/theorem shape_c_not_F [h :- (IsF (STy.tlol STy.tdat (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat)))]
  False
  (exact (And.left (And.left (And.right h)))))

(def theorem-names
  ["ex_nat_elim" "or_elim"
   "VF_foot_lol" "VF_foot_fun" "VF_foot_ten" "VF_foot_bang" "VF_foot_mono"
   "oindep_dat" "oindep_good" "oindep_cert" "oindep_tok"
   "oindep_lol" "oindep_fun" "oindep_ten" "oindep_bang" "oindep"
   "capfree_dat" "capfree_good" "capfree_cert" "capfree_tok"
   "capfree_lol" "capfree_fun" "capfree_ten" "capfree_bang" "capfree"
   "o_sub_d" "d_sub_f"
   "q_dat" "q_good" "q_cert" "q_tok" "q_lol" "q_fun" "q_bang"
   "q_ten_l_core" "q_ten_r_core" "q_ten_l" "q_ten_r" "q_ten" "q_f"
   "charged_cap_lt" "charged_cap_fits" "charged_cap_code" "reflect_case_charged"
   "agent_in_F" "minting_agent_in_F" "shape_a_not_F" "shape_b_not_F" "shape_c_not_F"])
