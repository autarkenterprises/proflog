# JAF 2026 — alternative abstract draft

> Alternative take on the JAF 2026 contributed talk, saved alongside the primary
> [jaf-2026-abstract.tex](jaf-2026-abstract.tex). Anonymized for JAF (name,
> affiliation, and email are submitted separately as `surname.txt`, per the
> venue's submission instructions). This draft leads with the EA-stability and
> counting-lemma quantitative content (ADR-0108 / ADR-0111); the `.tex` leads
> with the U-grounding / `IS#_D(β)` framing. Choose one — or merge the
> distinctive content — before submission.

## Executable Self-Justification: A Tableau Logic-Programming Correspondent for Willard's Self-Justifying Arithmetic

Willard's self-justifying (self-verifying) axiom systems occupy one of the few
inhabited niches on the far side of Gödel's Second Incompleteness Theorem: by
replacing multiplication-as-a-total-operation with a base of *non-increasing*
"grounding" functions — subtraction, division, logarithm, root, maximum,
counting — and reasoning through a semantic-tableau deduction method **d**, such
a system can prove a Hilbert-style consistency statement for its own deductive
apparatus without lapsing into inconsistency; the diagonal argument "starves"
precisely because the system cannot prove multiplication total. Willard isolates
the quantitative conditions that make this work — centrally the **EA-stability**
of a configuration ξ = (L, Δ₀, B, d, G) (A- and E-stability over the
Normed(a,b) / Good(N) / ♯ machinery and Fact D.3's open-branch lemma), which
suffices for self-justification (Theorem 5.9), together with a *Conventional
Tableaux Encoding Requirement* that a proof code spend at least 5J bits on its J
function/application occurrences.

We pose a computational question about this framework: **can a self-justifying
system be placed in correspondence with an executable artifact — a logic-
programming language — and what does carrying out that correspondence require and
reveal?** We report a greenfield relational implementation of *Proflog*, a
tableau-based logic-programming kernel (the tableau analogue, after Fitting, of
Prolog's resolution procedure), and an explicitly **selected** deductive
apparatus D_SJAS realized on it. Over a first fragment we establish a
clause-by-clause correspondence between Proflog acceptance and Willard's tableau
provability SemPrf_D, and we re-derive the load-bearing quantitative content as
**executable audits** against the running implementation: a *counting lemma* that
derives the ≥ 5J size floor — in fact ≥ 18J bits for axiom citations and
≥ 24N + 36J bits for N-node structural proofs — directly from the proof-code byte
grammar; quantitative EA-stability for the selected composite proof-object
measure; soundness of every D_SJAS tableau rule with respect to the Normed
standard model; and standard-model soundness of the grounding-arithmetic
primitives.

We are deliberate about boundaries. D_SJAS is an explicitly labelled *selected*
variant, not Willard's literal D; the correspondence is proved over a first
fragment; the size results stand under an explicit code-injectivity hypothesis
and the standing β-validity premise; and the audits are executable but not
machine-checked. The exercise nonetheless converts several of Willard's abstract
requirements into concrete, re-runnable checks, and ties self-provable syntactic
consistency to the computational system of a programming language. We close by
situating self-justifying systems within a broader study of **autarkic**
(self-powered) formal systems — for which self-provable consistency is a
paradigmatic self-powered capability — and by stating the open problems it
raises: discharging code-injectivity and the β-validity boundary, extending the
correspondence beyond the first fragment, and mechanizing the audits.

**Keywords:** weak arithmetic, self-justifying axiom systems, semantic tableaux,
EA-stability, proof coding, logic programming, incompleteness.

### Selected references

1. D. E. Willard, *Self-Verifying Axiom Systems, the Incompleteness Theorem and
   Related Reflection Principles*, J. Symbolic Logic **66** (2001), 536–596.
2. D. E. Willard, self-justifying logics manuscript, arXiv:1108.6330.
3. M. Fitting, *Tableaux for Logic Programming*, J. Automated Reasoning **13**
   (1994), 175–188.
