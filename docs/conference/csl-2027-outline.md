# CSL 2027 paper outline: SJAS tableau correspondence

**Target venue:** [CSL 2027](https://csl2027.github.io/) (Brighton, 25–29 Jan 2027)  
**Deadlines:** abstract 8 Jul 2026; paper 15 Jul 2026 (AoE)  
**Format:** LIPIcs, ≤15 pages (+ appendices)  
**Status:** outline for July 2026 drafting (not yet submitted)

## Working title

**Structural Correspondence between Proflog Tableau Certificates and Willard Semantic-Tableau Proofs (First Fragment)**

## One-sentence claim

Over a sharply bounded first fragment of SJAS proof certificates, Proflog's structural proof checker accepts exactly the encoded trees that satisfy Willard's semantic-tableau proof predicate `SemPrf_D` for the selected ordinary-tableau apparatus `D`, with anti-compression and constructor-unreachability discharging the omitted cases.

## Relation to other submissions

| Artifact | Venue | Overlap |
|----------|-------|---------|
| [lopstr-ppdp26/](../../lopstr-ppdp26/) | LOPSTR+PPDP 2026 | **System / implementation** — builder, codes, worked examples; states correspondence as future work |
| [mk2026/](../../mk2026/) | miniKanren 2026 | **Kernel only** — no SJAS |
| This paper | CSL 2027 | **Proof-theoretic correspondence** — theorem, soundness/completeness, bounded fragment |

No double submission: LOPSTR describes the artifact; CSL proves the biconditional over the first fragment.

## Source material (repo)

- [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md) — decision and theorem target
- [docs/log/2026-06-13-sjas-tableau-correspondence-proof.md](../log/2026-06-13-sjas-tableau-correspondence-proof.md) — full proof draft
- [ADR-0096](../adr/ADR-0096-sjas-correspondence-fragment-audit.md) — first fragment definition
- [ADR-0097](../adr/ADR-0097-sjas-structural-proof-tree-audit.md) — tree shape / size audit
- [ADR-0098](../adr/ADR-0098-sjas-equality-fragment-reachability.md) — equality constructors unreachable
- [ADR-0099](../adr/ADR-0099-sjas-track2a-completion.md) — procedure-call / quantifier unreachable
- [ADR-0103](../adr/ADR-0103-sjas-proof-attempts-a-b.md) — Path A/B negative space
- [ADR-0104](../adr/ADR-0104-dsjas-track2c.md) — extended `D_SJAS` program (optional appendix pointer)

## Audience and CSL topics

Primary: **proof theory**, **logic programming and constraints**, **automated deduction**, **foundations of programming languages**.  
Secondary: **program synthesis** (proof-code synthesis boundary), **realizability** (implementation as executable witness suite).

## Section outline

### 1. Introduction (1.5 pp)

- Gödel incompleteness is intensional: proof procedure, coding, and language matter (Willard SJAS).
- Proflog implements semantic tableaux as operational semantics; SJAS proof predicates are encoded tableau checkers.
- **Contribution:** first-fragment biconditional `ProflogAccepts ↔ SemPrf_D` with explicit trust boundaries.
- Contrast: theorem-level equivalence insufficient; anti-compression (5J) and tree shape matter for self-reference.

### 2. Background (2 pp)

- Willard `IS#_D(β)`, U-grounding, ordinary semantic tableaux `D` (8 rules + closure; cite willard2001).
- Proflog kernel: subsidiary tableaux, proof terms vs host unification.
- Encoded certificates: formula-bearing structural trees (no rule-tag bytes); compact base-64 codes.
- **Not in scope:** full U-grounding numerals, skeletal-tag certificates, unbounded synthesis.

### 3. Covered domain (1 pp)

- Profiles `:willard-sjas-tableau0`, `:willard-sjas-level1`.
- First fragment: formula-bearing nodes + bare `sjas-axiom` citation.
- Track 2a unreachability: equality, disequality, procedure-call, quantifier tag constructors cannot appear in accepted first-fragment certificates (cite ADR-0098/0099).
- Beta validity as axiom basis (trust boundary, ADR-0072).

### 4. Willard side: `SemPrf_D` (1.5 pp)

- Candidate trees, prenex* root convention, closure.
- Conventional Tableaux Encoding Requirement (≥ 5J bits).
- Inductive definition aligned with extracted source text.

### 5. Proflog side: structural checker (1.5 pp)

- Relation `sjas-structural-proof-check-state-decodedo` / public `tableau-proof/3` pipeline.
- Branch state: agenda, lits, env, sigma, neqs, formula-byte proof tree.
- Local node acceptance = decoded formula + child structure (no oracle to host clause table).

### 6. Correspondence theorem (2 pp)

**Theorem (first fragment):**  
`Acc(P,S,F) ↔ SemPrf_D(decode(P), S, F)` under covered domain and encoding discipline.

- **Soundness:** induction on accepted checker derivations → each step matches a `D` rule; leaves closed.
- **Completeness:** every finite `D`-proof tree encodes to certificate accepted by checker (representability, not efficient synthesis).
- **Anti-compression lemma:** accepted certificates respect 5J bit bound; links to Willard's encoding requirement.
- **Irrelevance lemmas:** prenex* root normalization; profile-only atoms do not affect fragment.

### 7. Operational witnesses (1 pp)

- Per-rule witness tests (each `D` rule exercised).
- Positive/negative acceptance selectors; proof-size regression.
- Command block mirroring Track 2b suite (`lein test-proflog-extended` selectors).

### 8. Limitations and extensions (1 pp)

- Direct examination, not Coq/Isabelle-checked.
- Extended apparatus `D_SJAS` (Track 2c) — cite ADR-0104 as ongoing.
- Path B negative results (ADR-0103): where biconditional fails outside fragment.
- Open: full skeletal tags, U-grounding codes, proof synthesis at scale.

### 9. Related work (0.5 pp)

- Willard SJAS corpus; Fitting Proflog; alphaLeanTAP / miniKanren provers; LFMTP-style logical frameworks.

### 10. Conclusion (0.5 pp)

- Executable correspondence narrows intensional gap between programming and self-justifying arithmetic.
- First fragment is honest boundary; extension roadmap.

## Appendix (optional, not in page count if LIPIcs allows)

- Per-rule correspondence table (checker clause ↔ Willard rule).
- Full trust-boundary checklist (9 criteria from correspondence proof doc).

## Drafting checklist (July 2026)

- [ ] De-anonymize implementation references (post-LOPSTR artifact public).
- [ ] Port per-rule table from `2026-06-13-sjas-tableau-correspondence-proof.md`.
- [ ] Confirm LIPIcs style file and HotCRP link when open.
- [ ] Run extended + Track 2b witness suite; record counts in §7.
- [ ] Single-author vs multi-author decision.
- [ ] Check overlap with LOPSTR camera-ready (Aug 2026): cite as companion system description.

## Risk notes for PC

- **Strength:** Novel bridge between Willard incompleteness engineering and executable LP.
- **Weakness:** Not machine-checked; bounded fragment may read as narrow — foreground unreachability proofs as principled exclusion, not omission.
- **Mitigation:** Clear trust boundaries; operational witness suite; companion artifact paper at LOPSTR.
