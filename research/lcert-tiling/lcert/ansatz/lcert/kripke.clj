(ns lcert.kripke
  "Step 3 of proflog ADR-0143, machine-checked: a world-indexed Kripke model
  in which the Reflect case holds at EVERY type, which settles the
  higher-order case (METATHEORY.md §3).

  Loading this namespace runs Ansatz's elaborator and kernel on every
  definition and theorem below.  It builds on lcert.verified (codes CT,
  `nodes`, `budget`, strict overhead) and lcert.charged (the embedded types
  STy, their carrier SCar, R4's fixed-cap sets VF, ex_nat_elim).

  The model.  A world w bounds the tokens alive in a run: the caller's
  tokens, everything the rest of the program holds, and the value's own.
  Tokens are never created, only burnt (reflect burns a certificate's ‖v‖
  tokens and hands its program m < ‖v‖ fresh ones), so the world only shrinks
  as a run proceeds.  VW t w k x says x ∈ V_w^k(t): x is a value of type t
  accounting for k of the w live tokens.

    tdat, tgood, tcert, ttok   as in VF (the world does not matter)
    Π(x :₁ A). B    f such that for every LATER world w₂ ≤ w, every j with
                    k + j ≤ w₂ and every a ∈ V_{w₂}^j(A), some burn
                    b ≤ k + j has f(a) ∈ V_{w₂−b}^{k+j−b}(B)
    Π(x :ω A). B    the same, with the argument at footprint 0
    A ⊗ B           a split j ≤ k of the footprint, both parts in world w
    Σ(x :ω A). B    the first component at footprint 0

  Two features make reflection work at every type.  Quantifying over later
  worlds makes every set shrink-closed in the world (vw_world_mono), so a
  value built inside a reflected program — judged in the smaller world after
  the reflection — is good for the whole rest of the run.  The existential
  burn lets a result live in a smaller world than its call started in, which
  is exactly what a call that reflects does.  R4's fixed-cap sets VF have
  neither, and fail at the higher-order shapes (fixed_rejects_c).

  Verified here:
    vw_world_mono     V_w^k(t) ⊆ V_{w'}^k(t) for w' ≤ w, every type
    vw_foot_mono      V_w^k(t) ⊆ V_w^{k'}(t) for k ≤ k', every type
    reflect_case_world  THE KEY LEMMA: for EVERY type t — no class
                      hypothesis — any denotation satisfying the outer
                      hypothesis (every derivable program at a smaller cap
                      lands, from any world it fits, in the model after
                      some burn) puts the reflected program's value at the
                      charged cap n − nodes(c) + budget(c) in V_{w−b}^{k−b}(t)
                      for some burn b ≤ k, whenever the caller's world is
                      within the cap and its certificate within its footprint
    app_case_world, pair_case_world
                      application and tensor introduction: burns compose
    fixed_accepts_c, fixed_rejects_c
                      at shape (c), 1 ⊸ Σ(f :ω H). 1, the program's value is
                      in R4's V at the charged cap 0 with footprint 0, but not
                      at the caller's cap 1 with footprint 1: the inclusion Q
                      of lcert.charged fails there, as the assessment found
    world_accepts_c, shape_c_world_ok
                      the world model accepts that same value through its
                      Reflect case: in the world left after the reflection
                      (0 live tokens) its consumer never meets a certificate
                      with nodes

  How the proofs are written.  As in lcert.charged and lcert.supply; in
  addition, Ansatz 0.2.115's omega builds an ill-typed proof term (or runs
  out of heap) on goals with deeply nested truncated subtraction, and on some
  goals whose atoms are function applications.  So the arithmetic of a case
  is stated additively (a burn b from world w leaves w₁ with w₁ + b = w) or
  proved once over plain variables (reflect_arith_*) and instantiated."
  (:require [ansatz.core :as a]
            [ansatz.kernel.env :as env]
            [ansatz.kernel.expr :as ke]
            [ansatz.kernel.name :as nm]
            [lcert.verified :refer [nodes]]
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

(defn statement
  "The named declaration's type, as a string, in full: ->string elides
  subterms nested more than 20 deep unless started at a negative depth."
  [s]
  (ke/->string (.type (env/lookup (a/env) (nm/from-string s))) -100000))

(defmacro ^:private kdef
  "Define a Prop-valued (or otherwise uncompilable) function in the kernel
  only (see lcert.charged/kdef)."
  [nm & more]
  `(do (try (a/defn ~nm ~@more) (catch Throwable _# nil))
       (when-not (declared? ~(str nm))
         (throw (ex-info (str "kernel definition failed: " '~nm) {:name '~nm})))
       '~nm))

;; ---------------------------------------------------------------------------
;; The world-indexed sets.  VW t w k x : x ∈ V_w^k(t).

(kdef VW [t :- STy] (arrow Nat Nat (SCar t) Prop)
  (STy.rec (lam [t STy] (arrow Nat Nat (SCar t) Prop))
    (lam [w Nat, k Nat, x Nat] True)
    (lam [w Nat, k Nat, x Nat] (< 0 x))
    (lam [w Nat, k Nat, x CT] (<= (nodes x) k))
    (lam [w Nat, k Nat, x Nat] (<= 1 k))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          w Nat, k Nat, f (arrow (SCar a) (SCar b))]
      (forall [w2 Nat, j Nat, x (SCar a)]
        (=> (<= w2 w) (<= (+ k j) w2) (ia w2 j x)
            (Exists (lam [bb Nat] (And (<= bb (+ k j)) (ib (- w2 bb) (- (+ k j) bb) (f x))))))))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          w Nat, k Nat, f (arrow (SCar a) (SCar b))]
      (forall [w2 Nat, x (SCar a)]
        (=> (<= w2 w) (<= k w2) (ia w2 0 x)
            (Exists (lam [bb Nat] (And (<= bb k) (ib (- w2 bb) (- k bb) (f x))))))))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          w Nat, k Nat, p (Prod (SCar a) (SCar b))]
      (Exists (lam [j Nat] (And (<= j k) (And (ia w j (Prod.fst p)) (ib w (- k j) (Prod.snd p)))))))
    (lam [a STy, b STy, ia (arrow Nat Nat (SCar a) Prop), ib (arrow Nat Nat (SCar b) Prop),
          w Nat, k Nat, p (Prod (SCar a) (SCar b))]
      (And (ia w 0 (Prod.fst p)) (ib w k (Prod.snd p))))
    t))

;; ---------------------------------------------------------------------------
;; World monotonicity: by construction, since function clauses quantify over
;; every later world.

(kdef WorldMono [t :- STy] Prop
  (forall [w Nat, w2 Nat, k Nat, x (SCar t)] (=> (<= w2 w) (VW t w k x) (VW t w2 k x))))

(a/theorem vw_world_base [t :- STy, h :- (forall [w Nat, w2 Nat, k Nat, x (SCar t)] (=> (VW t w k x) (VW t w2 k x)))]
  (WorldMono t)
  (intro w w2 k x hle hv) (exact (h w w2 k x hv)))

(a/theorem vw_world_lol [a :- STy, b :- STy] (WorldMono (STy.tlol a b))
  (intro w w2 k f hle hv w3 j x h3 hj hx)
  (exact (hv w3 j x (Nat.le_trans h3 hle) hj hx)))

(a/theorem vw_world_fun [a :- STy, b :- STy] (WorldMono (STy.tfun a b))
  (intro w w2 k f hle hv w3 x h3 hk hx)
  (exact (hv w3 x (Nat.le_trans h3 hle) hk hx)))

(a/theorem vw_world_ten [a :- STy, b :- STy, ha :- (WorldMono a), hb :- (WorldMono b)] (WorldMono (STy.tten a b))
  (intro w w2 k p hle hv)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j k) (And (VW a w j (Prod.fst p)) (VW b w (- k j) (Prod.snd p)))))
                      (VW (STy.tten a b) w2 k p) hv
                      (lam [j Nat, hj (And (<= j k) (And (VW a w j (Prod.fst p)) (VW b w (- k j) (Prod.snd p))))]
                        (Exists.intro j (And.intro (And.left hj)
                          (And.intro (ha w w2 j (Prod.fst p) hle (And.left (And.right hj)))
                                     (hb w w2 (- k j) (Prod.snd p) hle (And.right (And.right hj))))))))))

(a/theorem vw_world_bang [a :- STy, b :- STy, ha :- (WorldMono a), hb :- (WorldMono b)] (WorldMono (STy.tbang a b))
  (intro w w2 k p hle hv)
  (exact (And.intro (ha w w2 0 (Prod.fst p) hle (And.left hv)) (hb w w2 k (Prod.snd p) hle (And.right hv)))))

(a/theorem vw_world_mono [t :- STy] (WorldMono t)
  (induction t)
  (exact (vw_world_base STy.tdat (lam [w Nat, w2 Nat, k Nat, x Nat, hv (VW STy.tdat w k x)] hv)))
  (exact (vw_world_base STy.tgood (lam [w Nat, w2 Nat, k Nat, x Nat, hv (VW STy.tgood w k x)] hv)))
  (exact (vw_world_base STy.tcert (lam [w Nat, w2 Nat, k Nat, x CT, hv (VW STy.tcert w k x)] hv)))
  (exact (vw_world_base STy.ttok (lam [w Nat, w2 Nat, k Nat, x Nat, hv (VW STy.ttok w k x)] hv)))
  (exact (vw_world_lol a b))
  (exact (vw_world_fun a b))
  (exact (vw_world_ten a b ih_a ih_b))
  (exact (vw_world_bang a b ih_a ih_b)))

;; ---------------------------------------------------------------------------
;; Footprint monotonicity: a function may be charged more than it needs.

(kdef FootMono [t :- STy] Prop
  (forall [w Nat, k Nat, k2 Nat, x (SCar t)] (=> (<= k k2) (VW t w k x) (VW t w k2 x))))

(a/theorem vw_foot_dat [] (FootMono STy.tdat) (intro w k k2 x h hv) (exact hv))
(a/theorem vw_foot_good [] (FootMono STy.tgood) (intro w k k2 x h hv) (exact hv))
(a/theorem vw_foot_cert [] (FootMono STy.tcert) (intro w k k2 x h hv) (exact (Nat.le_trans hv h)))
(a/theorem vw_foot_tok [] (FootMono STy.ttok) (intro w k k2 x h hv) (exact (Nat.le_trans hv h)))

(a/theorem vw_foot_lol [a :- STy, b :- STy, hb :- (FootMono b)] (FootMono (STy.tlol a b))
  (intro w k k2 f h hv w2 j x h2 hj hx)
  (have hkj (<= (+ k j) w2)) (omega)
  (exact (ex_nat_elim (lam [bb Nat] (And (<= bb (+ k j)) (VW b (- w2 bb) (- (+ k j) bb) (f x))))
                      (Exists (lam [bb Nat] (And (<= bb (+ k2 j)) (VW b (- w2 bb) (- (+ k2 j) bb) (f x)))))
                      (hv w2 j x h2 hkj hx)
                      (lam [bb Nat, hbb (And (<= bb (+ k j)) (VW b (- w2 bb) (- (+ k j) bb) (f x)))]
                        (Exists.intro bb (And.intro (Nat.le_trans (And.left hbb) (Nat.add_le_add_right h j))
                          (hb (- w2 bb) (- (+ k j) bb) (- (+ k2 j) bb) (f x)
                              (Nat.sub_le_sub_right (Nat.add_le_add_right h j) bb) (And.right hbb))))))))

(a/theorem vw_foot_fun [a :- STy, b :- STy, hb :- (FootMono b)] (FootMono (STy.tfun a b))
  (intro w k k2 f h hv w2 x h2 hk2 hx)
  (have hk (<= k w2)) (omega)
  (exact (ex_nat_elim (lam [bb Nat] (And (<= bb k) (VW b (- w2 bb) (- k bb) (f x))))
                      (Exists (lam [bb Nat] (And (<= bb k2) (VW b (- w2 bb) (- k2 bb) (f x)))))
                      (hv w2 x h2 hk hx)
                      (lam [bb Nat, hbb (And (<= bb k) (VW b (- w2 bb) (- k bb) (f x)))]
                        (Exists.intro bb (And.intro (Nat.le_trans (And.left hbb) h)
                          (hb (- w2 bb) (- k bb) (- k2 bb) (f x) (Nat.sub_le_sub_right h bb) (And.right hbb))))))))

(a/theorem vw_foot_ten [a :- STy, b :- STy, hb :- (FootMono b)] (FootMono (STy.tten a b))
  (intro w k k2 p h hv)
  (exact (ex_nat_elim (lam [j Nat] (And (<= j k) (And (VW a w j (Prod.fst p)) (VW b w (- k j) (Prod.snd p)))))
                      (VW (STy.tten a b) w k2 p) hv
                      (lam [j Nat, hj (And (<= j k) (And (VW a w j (Prod.fst p)) (VW b w (- k j) (Prod.snd p))))]
                        (Exists.intro j (And.intro (Nat.le_trans (And.left hj) h)
                          (And.intro (And.left (And.right hj))
                                     (hb w (- k j) (- k2 j) (Prod.snd p) (Nat.sub_le_sub_right h j)
                                         (And.right (And.right hj))))))))))

(a/theorem vw_foot_bang [a :- STy, b :- STy, hb :- (FootMono b)] (FootMono (STy.tbang a b))
  (intro w k k2 p h hv)
  (exact (And.intro (And.left hv) (hb w k k2 (Prod.snd p) h (And.right hv)))))

(a/theorem vw_foot_mono [t :- STy] (FootMono t)
  (induction t)
  (exact vw_foot_dat) (exact vw_foot_good) (exact vw_foot_cert) (exact vw_foot_tok)
  (exact (vw_foot_lol a b ih_b)) (exact (vw_foot_fun a b ih_b))
  (exact (vw_foot_ten a b ih_b)) (exact (vw_foot_bang a b ih_b)))

;; ---------------------------------------------------------------------------
;; The Reflect case.
;;
;; The reflect term's context splits as k₁ + k₂ ≤ k ≤ w ≤ n.  Evaluating the
;; certificate burns b₁ ≤ k₁ and leaves v with ‖v‖ ≤ k₁ − b₁; evaluating the
;; evidence burns b₂ ≤ k₂.  The reflection burns ‖v‖ tokens and gives the
;; decoded program budget(c) fresh ones, so the world after it is
;;     w₃ = w − (b₁ + b₂ + (‖v‖ − budget(c)))  ≤  n − ‖v‖ + budget(c) = n₂,
;; the charged cap, and w₃ ≥ budget(c): the program's own tokens fit.  The
;; OUTER hypothesis at n₂ < n puts the program's value in
;; V_{w₃−b₄}^{budget(c)−b₄}(t) for some b₄; with the total burn
;; b = b₁ + b₂ + (‖v‖ − budget(c)) + b₄ the caller's world w − b is exactly
;; w₃ − b₄, and footprint monotonicity lifts budget(c) − b₄ to k − b.  No
;; property of t is used but footprint monotonicity, which every type has.

;; The arithmetic, over plain variables (V = ‖v‖, M = budget(c)).
(a/theorem reflect_arith_1 [n :- Nat, w :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, b1 :- Nat, b2 :- Nat, V :- Nat, M :- Nat,
                            hs :- (< M V), hwn :- (<= w n), hkw :- (<= k w), hk :- (<= (+ k1 k2) k),
                            hb1 :- (<= b1 k1), hv :- (<= V (- k1 b1)), hb2 :- (<= b2 k2)]
  (< (+ (- n V) M) n) (omega))

(a/theorem reflect_arith_2 [n :- Nat, w :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, b1 :- Nat, b2 :- Nat, V :- Nat, M :- Nat,
                            hs :- (< M V), hwn :- (<= w n), hkw :- (<= k w), hk :- (<= (+ k1 k2) k),
                            hb1 :- (<= b1 k1), hv :- (<= V (- k1 b1)), hb2 :- (<= b2 k2)]
  (<= (- w (+ (+ b1 b2) (- V M))) (+ (- n V) M)) (omega))

(a/theorem reflect_arith_3 [n :- Nat, w :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, b1 :- Nat, b2 :- Nat, V :- Nat, M :- Nat,
                            hs :- (< M V), hwn :- (<= w n), hkw :- (<= k w), hk :- (<= (+ k1 k2) k),
                            hb1 :- (<= b1 k1), hv :- (<= V (- k1 b1)), hb2 :- (<= b2 k2)]
  (<= M (- w (+ (+ b1 b2) (- V M)))) (omega))

(a/theorem reflect_arith_4 [n :- Nat, w :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, b1 :- Nat, b2 :- Nat, V :- Nat, M :- Nat,
                            hs :- (< M V), hwn :- (<= w n), hkw :- (<= k w), hk :- (<= (+ k1 k2) k),
                            hb1 :- (<= b1 k1), hv :- (<= V (- k1 b1)), hb2 :- (<= b2 k2)]
  (= (+ (- w (+ (+ b1 b2) (- V M))) (+ (+ b1 b2) (- V M))) w) (omega))

(a/theorem reflect_arith_5 [n :- Nat, w :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, b1 :- Nat, b2 :- Nat, V :- Nat, M :- Nat,
                            hs :- (< M V), hwn :- (<= w n), hkw :- (<= k w), hk :- (<= (+ k1 k2) k),
                            hb1 :- (<= b1 k1), hv :- (<= V (- k1 b1)), hb2 :- (<= b2 k2)]
  (<= (+ (+ (+ b1 b2) (- V M)) M) k) (omega))

;; After the outer hypothesis: world w₃ + c = w, where c is the burn before
;; the program runs, and the program's own burn b₄.
(a/theorem reflect_world_core
  [t :- STy, x :- (SCar t), w :- Nat, k :- Nat, w3 :- Nat, c :- Nat, m :- Nat, b4 :- Nat,
   hw :- (= (+ w3 c) w), hck :- (<= (+ c m) k), hb4 :- (<= b4 m),
   hx :- (VW t (- w3 b4) (- m b4) x)]
  (Exists (lam [bb Nat] (And (<= bb k) (VW t (- w bb) (- k bb) x))))
  (have e1 (= (- w (+ c b4)) (- w3 b4))) (omega)
  (have e2 (<= (- m b4) (- k (+ c b4)))) (omega)
  (have e3 (<= (+ c b4) k)) (omega)
  (have hx2 (VW t (- w (+ c b4)) (- m b4) x))
  (rewrite e1)
  (exact (Exists.intro (+ c b4) (And.intro e3 (vw_foot_mono t (- w (+ c b4)) (- m b4) (- k (+ c b4)) x e2 hx2))))
  (exact hx))

(a/theorem reflect_case_world
  [t :- STy, den :- (arrow Nat CT (SCar t)), derives :- (arrow CT Prop),
   n :- Nat, w :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, b1 :- Nat, b2 :- Nat, l :- Nat, j :- CT, p :- CT,
   ih :- (forall [n2 Nat, c CT]
           (=> (< n2 n) (derives c)
               (forall [w3 Nat] (=> (<= w3 n2) (<= (budget c) w3)
                 (Exists (lam [b4 Nat] (And (<= b4 (budget c)) (VW t (- w3 b4) (- (budget c) b4) (den n2 c)))))))))
   hd :- (derives (CT.cell l true j p)),
   hwn :- (<= w n), hkw :- (<= k w), hk :- (<= (+ k1 k2) k), hb1 :- (<= b1 k1),
   hv :- (<= (nodes (CT.cell l true j p)) (- k1 b1)), hb2 :- (<= b2 k2)]
  (Exists (lam [bb Nat] (And (<= bb k)
    (VW t (- w bb) (- k bb)
        (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) (CT.cell l true j p))))))
  (have hs (< (budget (CT.cell l true j p)) (nodes (CT.cell l true j p))))
  (exact (strict_overhead l j p))
  (exact (ex_nat_elim
           (lam [b4 Nat] (And (<= b4 (budget (CT.cell l true j p)))
             (VW t (- (- w (+ (+ b1 b2) (- (nodes (CT.cell l true j p)) (budget (CT.cell l true j p))))) b4)
                 (- (budget (CT.cell l true j p)) b4)
                 (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) (CT.cell l true j p)))))
           (Exists (lam [bb Nat] (And (<= bb k)
             (VW t (- w bb) (- k bb)
                 (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) (CT.cell l true j p))))))
           (ih (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p))) (CT.cell l true j p)
               (reflect_arith_1 n w k k1 k2 b1 b2 (nodes (CT.cell l true j p)) (budget (CT.cell l true j p))
                                hs hwn hkw hk hb1 hv hb2)
               hd
               (- w (+ (+ b1 b2) (- (nodes (CT.cell l true j p)) (budget (CT.cell l true j p)))))
               (reflect_arith_2 n w k k1 k2 b1 b2 (nodes (CT.cell l true j p)) (budget (CT.cell l true j p))
                                hs hwn hkw hk hb1 hv hb2)
               (reflect_arith_3 n w k k1 k2 b1 b2 (nodes (CT.cell l true j p)) (budget (CT.cell l true j p))
                                hs hwn hkw hk hb1 hv hb2))
           (lam [b4 Nat, hb (And (<= b4 (budget (CT.cell l true j p)))
                                 (VW t (- (- w (+ (+ b1 b2) (- (nodes (CT.cell l true j p)) (budget (CT.cell l true j p))))) b4)
                                     (- (budget (CT.cell l true j p)) b4)
                                     (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p)))
                                          (CT.cell l true j p))))]
             (reflect_world_core t (den (+ (- n (nodes (CT.cell l true j p))) (budget (CT.cell l true j p)))
                                        (CT.cell l true j p))
                                 w k
                                 (- w (+ (+ b1 b2) (- (nodes (CT.cell l true j p)) (budget (CT.cell l true j p)))))
                                 (+ (+ b1 b2) (- (nodes (CT.cell l true j p)) (budget (CT.cell l true j p))))
                                 (budget (CT.cell l true j p)) b4
                                 (reflect_arith_4 n w k k1 k2 b1 b2 (nodes (CT.cell l true j p))
                                                  (budget (CT.cell l true j p)) hs hwn hkw hk hb1 hv hb2)
                                 (reflect_arith_5 n w k k1 k2 b1 b2 (nodes (CT.cell l true j p))
                                                  (budget (CT.cell l true j p)) hs hwn hkw hk hb1 hv hb2)
                                 (And.left hb) (And.right hb))))))

;; ---------------------------------------------------------------------------
;; Application and tensor introduction: the function (or first component) is
;; evaluated first, from world w to w₁ = w − b₁, then the argument (or second
;; component), to w₂ = w₁ − b₂; the call burns b₃ more.

(a/theorem app_world_core
  [b :- STy, y :- (SCar b), w :- Nat, w2 :- Nat, k :- Nat, kf :- Nat, ku :- Nat, b1 :- Nat, b2 :- Nat, b3 :- Nat,
   hw :- (= (+ (+ w2 b2) b1) w), hk :- (<= (+ (+ kf b1) (+ ku b2)) k), hb3 :- (<= b3 (+ kf ku)),
   hy :- (VW b (- w2 b3) (- (+ kf ku) b3) y)]
  (Exists (lam [bb Nat] (And (<= bb k) (VW b (- w bb) (- k bb) y))))
  (have e1 (= (- w (+ (+ b1 b2) b3)) (- w2 b3))) (omega)
  (have e2 (<= (- (+ kf ku) b3) (- k (+ (+ b1 b2) b3)))) (omega)
  (have e3 (<= (+ (+ b1 b2) b3) k)) (omega)
  (have hy2 (VW b (- w (+ (+ b1 b2) b3)) (- (+ kf ku) b3) y))
  (rewrite e1)
  (exact (Exists.intro (+ (+ b1 b2) b3)
           (And.intro e3 (vw_foot_mono b (- w (+ (+ b1 b2) b3)) (- (+ kf ku) b3) (- k (+ (+ b1 b2) b3)) y e2 hy2))))
  (exact hy))

(a/theorem app_case_world
  [a :- STy, b :- STy, f :- (SCar (STy.tlol a b)), u :- (SCar a),
   w :- Nat, w1 :- Nat, w2 :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, kf :- Nat, ku :- Nat, b1 :- Nat, b2 :- Nat,
   hw1 :- (= (+ w1 b1) w), hw2 :- (= (+ w2 b2) w1), hkf :- (= (+ kf b1) k1), hku :- (= (+ ku b2) k2),
   hk :- (<= (+ k1 k2) k), hkw :- (<= k w),
   hf :- (VW (STy.tlol a b) w1 kf f), hu :- (VW a w2 ku u)]
  (Exists (lam [bb Nat] (And (<= bb k) (VW b (- w bb) (- k bb) (f u)))))
  (have e1 (<= w2 w1)) (omega)
  (have e2 (<= (+ kf ku) w2)) (omega)
  (have e3 (= (+ (+ w2 b2) b1) w)) (omega)
  (have e4 (<= (+ (+ kf b1) (+ ku b2)) k)) (omega)
  (exact (ex_nat_elim (lam [b3 Nat] (And (<= b3 (+ kf ku)) (VW b (- w2 b3) (- (+ kf ku) b3) (f u))))
                      (Exists (lam [bb Nat] (And (<= bb k) (VW b (- w bb) (- k bb) (f u)))))
                      (hf w2 ku u e1 e2 hu)
                      (lam [b3 Nat, hb (And (<= b3 (+ kf ku)) (VW b (- w2 b3) (- (+ kf ku) b3) (f u)))]
                        (app_world_core b (f u) w w2 k kf ku b1 b2 b3 e3 e4 (And.left hb) (And.right hb))))))

(a/theorem pair_case_world
  [a :- STy, b :- STy, x :- (SCar a), y :- (SCar b),
   w :- Nat, w1 :- Nat, w2 :- Nat, k :- Nat, k1 :- Nat, k2 :- Nat, ka :- Nat, kb :- Nat, b1 :- Nat, b2 :- Nat,
   hw1 :- (= (+ w1 b1) w), hw2 :- (= (+ w2 b2) w1), hka :- (= (+ ka b1) k1), hkb :- (= (+ kb b2) k2),
   hk :- (<= (+ k1 k2) k), hkw :- (<= k w),
   hx :- (VW a w1 ka x), hy :- (VW b w2 kb y)]
  (Exists (lam [bb Nat] (And (<= bb k) (VW (STy.tten a b) (- w bb) (- k bb) (Prod.mk x y)))))
  (have e1 (= (- w (+ b1 b2)) w2)) (omega)
  (have e2 (<= (+ b1 b2) k)) (omega)
  (have e3 (<= ka (- k (+ b1 b2)))) (omega)
  (have e4 (<= kb (- (- k (+ b1 b2)) ka))) (omega)
  (have e5 (<= w2 w1)) (omega)
  (have hp (VW (STy.tten a b) (- w (+ b1 b2)) (- k (+ b1 b2)) (Prod.mk x y)))
  (rewrite e1)
  (exact (Exists.intro (+ b1 b2) (And.intro e2 hp)))
  (exact (Exists.intro ka (And.intro e3 (And.intro (vw_world_mono a w1 w2 ka x e5 hx)
                                                   (vw_foot_mono b w2 kb (- (- k (+ b1 b2)) ka) y e4 hy))))))

;; ---------------------------------------------------------------------------
;; Shape (c), 1 ⊸ Σ(f :ω H). 1 with H = R ⊸ Σ(b :ω Act). Safe(b).
;;
;; Its reflected program, run at the charged cap, hands out a reusable
;; consumer that is right on every certificate the run can still hold and
;; wrong beyond: hsmall answers 1 (safe) on a certificate with no nodes and
;; 0 (unsafe) otherwise.  The scenario: a certificate of 1 node declaring 0
;; tokens, reflected by a caller of footprint 1 at cap 1, so the charged cap
;; is 1 − 1 + 0 = 0.

(a/defn hsmall [s :- CT] Nat (- 1 (nodes s)))

(a/defn gc [u :- Nat] (Prod (arrow CT Nat) Nat) (Prod.mk hsmall 0))

(a/theorem one_node []
  (= (nodes (CT.cell 0 true (CT.cell 0 false CT.tend CT.tend) (CT.cell 0 false CT.tend CT.tend))) 1)
  (rfl))

(a/theorem vf_hsmall_0 [] (VF (STy.tlol STy.tcert STy.tgood) 0 0 hsmall)
  (intro j s hj hs)
  (have hs2 (<= (nodes s) j)) (exact hs)
  (change (< 0 (- 1 (nodes s))))
  (omega))

;; R4's sets at the charged cap 0, footprint 0: the value is in them.
(a/theorem fixed_accepts_c [] (VF (STy.tlol STy.tdat (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat)) 0 0 gc)
  (intro j x hj hx)
  (exact (And.intro vf_hsmall_0 True.intro)))

;; R4's sets at the caller's cap 1, footprint 1: the value is not, since the
;; reusable consumer must be right on every certificate of up to 1 node.  So
;; V⁰₀ ⊄ V¹₁ here although the gap 1 − 0 ≤ 1 − 0: the inclusion Q fails.
(a/theorem fixed_rejects_c [h :- (VF (STy.tlol STy.tdat (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat)) 1 1 gc)]
  False
  (have h1 (VF (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat) 1 (+ 1 0) (gc 0)))
  (exact (h 0 0 (Nat.le_refl 1) True.intro))
  (have h2 (VF (STy.tlol STy.tcert STy.tgood) 1 0 hsmall))
  (exact (And.left h1))
  (have h3 (< 0 (hsmall (CT.cell 0 true (CT.cell 0 false CT.tend CT.tend) (CT.cell 0 false CT.tend CT.tend)))))
  (exact (h2 1 (CT.cell 0 true (CT.cell 0 false CT.tend CT.tend) (CT.cell 0 false CT.tend CT.tend))
             (Nat.le_refl 1) (Nat.le_of_eq one_node)))
  (have h4 (< 0 0))
  (exact h3)
  (exact (Nat.lt_irrefl 0 h4)))

;; The world model: in a world of at most 0 live tokens, hsmall only ever
;; meets certificates with no nodes.
(a/theorem vw_hsmall [w3 :- Nat, hw :- (<= w3 0)] (VW (STy.tlol STy.tcert STy.tgood) w3 0 hsmall)
  (intro w4 j s h4 hj hs)
  (have hs2 (<= (nodes s) j)) (exact hs)
  (have e (< 0 (- 1 (nodes s)))) (omega)
  (exact (Exists.intro 0 (And.intro (Nat.zero_le (+ 0 j)) e))))

(a/theorem world_accepts_c [] (VW (STy.tlol STy.tdat (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat)) 0 0 gc)
  (intro w2 j x h2 hj hx)
  (have hw2 (<= (- w2 0) 0)) (omega)
  (exact (Exists.intro 0 (And.intro (Nat.zero_le (+ 0 j)) (And.intro (vw_hsmall (- w2 0) hw2) True.intro)))))

;; Through the Reflect case: caller world 1, footprint 1; the reflection
;; leaves world 0 (w₃ + 1 = 1) and the program burns nothing more.
(a/theorem shape_c_world_ok []
  (Exists (lam [bb Nat] (And (<= bb 1)
    (VW (STy.tlol STy.tdat (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat)) (- 1 bb) (- 1 bb) gc))))
  (exact (reflect_world_core (STy.tlol STy.tdat (STy.tbang (STy.tlol STy.tcert STy.tgood) STy.tdat)) gc
                             1 1 0 1 0 0 (rfl) (Nat.le_refl 1) (Nat.le_refl 0) world_accepts_c)))

(def theorem-names
  ["vw_world_base" "vw_world_lol" "vw_world_fun" "vw_world_ten" "vw_world_bang" "vw_world_mono"
   "vw_foot_dat" "vw_foot_good" "vw_foot_cert" "vw_foot_tok"
   "vw_foot_lol" "vw_foot_fun" "vw_foot_ten" "vw_foot_bang" "vw_foot_mono"
   "reflect_arith_1" "reflect_arith_2" "reflect_arith_3" "reflect_arith_4" "reflect_arith_5"
   "reflect_world_core" "reflect_case_world"
   "app_world_core" "app_case_world" "pair_case_world"
   "one_node" "vf_hsmall_0" "fixed_accepts_c" "fixed_rejects_c"
   "vw_hsmall" "world_accepts_c" "shape_c_world_ok"])
