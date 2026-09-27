(ns lcert.ho-forms
  "Assessor's demo forms (not part of jpt4/sjas): certificate-consuming
  functions at higher order, and a self-similar delegating agent."
  (:require [clojure.walk :as walk]
            [lcert.examples :as ex]))

(def sigma
  "Σ(x :ω Nat). T(x ≠ 0): an action x with evidence that it is safe (≠ 0)."
  '(Sigma [x w Nat] (T (rec-nat [z Bool] ff [k y] tt x))))

(def H
  "A certificate consumer: given a certificate of sigma and its evidence,
  return the certified pair."
  (list 'Pi '[s 1 R] (list '-o (list 'T (list 'chk '(print s) (list 'code sigma))) sigma)))

(def G
  "The canonical consumer: reflect the certificate it is handed."
  (list 'fn '[s 1 R] (list 'fn ['e 1 (list 'T (list 'chk '(print s) (list 'code sigma)))]
                           (list 'reflect sigma 's 'e))))

;; (a) third order: F receives g, and g's argument h consumes certificates.
(def Fa-type (list '-o (list '-o H sigma) sigma))
(def Fa (list 'fn ['g 1 (list '-o H sigma)] (list 'g G)))

;; (b) two consumers side by side in the result.
(def Fb-type (list '-o 'Unit (list 'tensor H H)))
(def Fb (list 'fn '[u 1 Unit] (list 'pair (list 'tensor H H) G G)))

;; (c) a reusable consumer in the result.
(def Fc-type (list '-o 'Unit (list 'Sigma ['f 'w H] 'Unit)))
(def Fc (list 'fn '[u 1 Unit] (list 'pair (list 'Sigma ['f 'w H] 'Unit) G 'star)))

(defn use-consumer
  "Surface program: inspect certificate w at sigma and hand it to consumer h;
  return the action, or `fail` if w does not check."
  [h w fail]
  (list 'inspect 'Nat w (list 'code sigma)
        '[y e2] (list 'let-pair 'Nat '[x1 p] (list (list h 'y) 'e2) 'x1)
        '[y e2] fail))

(defn plus [a b] (list 'rec-nat '[z Nat] a '[k y] '(succ y) b))

(def driver-a
  "Given certificates cf (of Fa) and w (of a sigma pair): trust cf, and pass
  the result a g that feeds w to whatever consumer it is given.  Returns the
  sigma pair, or the safe pair (7, ⋆) / (9, ⋆) if a check fails."
  (list 'fn '[cf 1 R]
        (list 'fn '[w 1 R]
              (list 'inspect sigma 'cf (list 'code Fa-type)
                    '[x e] (list (list 'reflect Fa-type 'x 'e)
                                 (list 'fn ['h 1 H]
                                       (list 'inspect sigma 'w (list 'code sigma)
                                             '[y e2] '((h y) e2)
                                             '[y e2] (list 'pair sigma 7 'star))))
                    '[x e] (list 'pair sigma 9 'star)))))

(def driver-b
  (list 'fn '[cf 1 R]
        (list 'fn '[w1 1 R]
              (list 'fn '[w2 1 R]
                    (list 'inspect 'Nat 'cf (list 'code Fb-type)
                          '[x e] (list 'let-pair 'Nat '[h1 h2] (list (list 'reflect Fb-type 'x 'e) 'star)
                                       (plus (use-consumer 'h1 'w1 70) (use-consumer 'h2 'w2 80)))
                          '[x e] 9)))))

(def driver-c
  (list 'fn '[cf 1 R]
        (list 'fn '[w1 1 R]
              (list 'fn '[w2 1 R]
                    (list 'inspect 'Nat 'cf (list 'code Fc-type)
                          '[x e] (list 'let-pair 'Nat '[f q] (list (list 'reflect Fc-type 'x 'e) 'star)
                                       (plus (use-consumer 'f 'w1 70) (use-consumer 'f 'w2 80)))
                          '[x e] 9)))))

;; (d) a self-similar agent.  Agent = Π(s :₁ R). sigma: given a supply,
;; return a safe action.  The agent reads its supply as a node whose left
;; child is a certificate of a successor agent and whose right child is the
;; successor's supply.  It checks the certificate at Agent; if it checks, it
;; trusts it (reflect) and runs the successor on the rest; otherwise, or on a
;; leaf, it takes the known-safe action 1.

(def agent-type (list 'Pi '[s 1 R] sigma))

(def fallback (list 'pair sigma 1 'star))

(def agent-template
  "The agent, with its abbreviations unexpanded: SIGMA = sigma, AGENT =
  agent-type, K = ◇ ⊗ (R ⊗ R), OUT = the definable destructor of R,
  FALLBACK = (pair SIGMA 1 star)."
  '(fn [s 1 R]
      (let-pair SIGMA [b q] (OUT s)
        (let-pair SIGMA [a f] q
          ((elim-bool [zz (-o (-o (T zz) K) SIGMA)] b
             (fn [g 1 (-o (T tt) K)]
               (let-pair SIGMA [d uv] (g star)
                 (let-pair SIGMA [l r] uv
                   (inspect SIGMA l (code AGENT)
                     [x e] ((reflect AGENT x e) r)
                     [x e] FALLBACK))))
             (fn [g 1 (-o (T ff) K)] FALLBACK))
           f)))))

(def agent-form
  (walk/postwalk-replace
   {'K ex/K-type 'SIGMA sigma 'AGENT agent-type 'FALLBACK fallback 'OUT ex/out-form}
   '(fn [s 1 R]
      (let-pair SIGMA [b q] (OUT s)
        (let-pair SIGMA [a f] q
          ((elim-bool [zz (-o (-o (T zz) K) SIGMA)] b
             (fn [g 1 (-o (T tt) K)]
               (let-pair SIGMA [d uv] (g star)
                 (let-pair SIGMA [l r] uv
                   (inspect SIGMA l (code AGENT)
                     [x e] ((reflect AGENT x e) r)
                     [x e] FALLBACK))))
             (fn [g 1 (-o (T ff) K)] FALLBACK))
           f))))))

(def leaf-agent
  "The last successor: takes action 2 without delegating."
  (list 'fn '[s 1 R] (list 'pair sigma 2 'star)))

;; (e) a minting agent (proflog ADR-0143 Step 2).  MintAgent = Π(cs :ω Syn).
;; Π(s :₁ R). sigma: given a list of codes and a raw supply of tokens, it
;; mints the first code into a certificate from the supply (the parser on the
;; primitive destructor), checks it at MintAgent, trusts it, and runs it on
;; the rest of the list and the rest of the supply; on an empty list, or a
;; code that is not a MintAgent's, it takes the known-safe action 1.  A list
;; is a right spine of :a nodes whose left children are the codes.

(def mint-agent-type (list 'Pi '[cs w Syn] agent-type))

(defn mint-agent-with
  "The minting agent, built on the given parser form."
  [parse-form]
  (walk/postwalk-replace
   {'SIGMA sigma 'MINT mint-agent-type 'PARSE parse-form 'FALLBACK fallback}
   '(fn [cs w Syn]
      (rec-syn [x (-o R SIGMA)]
        [a] (fn [s 1 R] FALLBACK)
        [a c1 c2 y1 y2]
        (fn [s 1 R]
          (let-pair SIGMA [t rest] (PARSE c1 s)
            (inspect SIGMA t (code MINT)
              [x e] (((reflect MINT x e) c2) rest)
              [x e] FALLBACK)))
        cs))))

(def mint-agent-form
  "The minting agent on the primitive destructor: each supply node it takes
  is one caseR."
  (mint-agent-with ex/parse-prim-form))

(def mint-leaf-form
  "The last successor: takes action 2, whatever it is handed."
  (list 'fn '[cs w Syn] (list 'fn '[s 1 R] (list 'pair sigma 2 'star))))

(defn code-list
  "A list of codes as a code: a right spine of :a nodes."
  [codes]
  (reduce (fn [rest c] [:sn :a c rest]) [:sl :a] (reverse codes)))
