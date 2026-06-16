# Expanded conference / CFP map (post–Tier A submissions)

**Search date:** 2026-06-16 (expanded pass; same criteria as the original plan).  
**Criteria:** Academic and industry venues (large and small, US and international) with **open or upcoming** calls where **Proflog**, **SJAS**, and/or **vendored core.logic** work is a legitimate topic—not merely a stretch keyword match.

**Already submitted (do not double-submit the same paper):**

| Venue | Artifact | Angle |
|-------|----------|-------|
| LOPSTR+PPDP 2026 | [lopstr-ppdp26/](../../lopstr-ppdp26/) | SJAS-in-Proflog system description |
| miniKanren 2026 | [mk2026/](../../mk2026/) | Proflog tableau kernel in core.logic |
| Clojure/Conj 2026 | talk proposal | Practitioner / REPL / core.logic experience |

---

## Component hooks (quick reference)

| Component | Contribution hooks |
|-----------|-------------------|
| **Proflog** | Fitting tableau LP; procedure-call subsidiary tableaux; proof-producing execution; synthesis; Fitting corpus |
| **SJAS** | Willard `IS#_D(β)`; arithmetized codes; semantic-tableau checking; correspondence (ADR-0100+); incompleteness boundary |
| **core.logic** | Stack-safe occurs check; ground-term fast paths; canonical regression (ADR-0075/0090/0093); host engine for relational kernel |

---

## Tier 1 — Best remaining targets (verify deadlines before submit)

| Venue | Location | Conf dates | Deadline (AoE unless noted) | Fit | Suggested paper |
|-------|----------|------------|----------------------------|-----|-----------------|
| **[LPAR-26](https://easychair.org/cfp/lpar26)** | Spetses, GR | 25–30 Oct 2026 | abstract **3 Jun**; paper **17 Jun 2026**; short **17 Aug** | **All three** | SJAS system (long) or kernel/tool (8 pp + artifact); short track for correspondence fragment |
| **[CSL 2027](https://csl2027.github.io/)** | Brighton, UK | 25–29 Jan 2027 | abstract **8 Jul**; paper **15 Jul 2026** | **SJAS** (+ kernel as witness) | Correspondence / proof theory — see [csl-2027-outline.md](csl-2027-outline.md) |
| **[ETAPS 2027](https://etaps.org/2027/cfp/)** | Copenhagen, DK | 10–15 Apr 2027 | **15 Oct 2026** (FoSSaCS, ESOP r2, TACAS, iFS) | Proflog + **core.logic** | FoSSaCS semantics; TACAS tool; ESOP implementation |
| **[FSTTCS 2026](https://easychair.org/cfp/FSTTCS2026)** | New Delhi, IN | 16–18 Dec 2026 | abstract **4 Jul**; paper **11 Jul 2026** | Proflog + SJAS | Track B: logic, verification, synthesis, decision procedures |
| **[VSTTE 2026](https://easychair.org/cfp/new)** | Graz, AT | 14 Sep 2026 | abstract **10 Jul**; paper **17 Jul 2026** | Proflog | Certificate-checking / proof-producing tool story (not full SJAS theory) |

---

## Tier 2 — Strong fit, June–August 2026 deadlines

### Logic programming & declarative methods

| Venue | Deadline | Components | Notes |
|-------|----------|------------|-------|
| [FROM 2026](https://resources.illc.uva.nl/LogicList/) | **7 Jun 2026** | Proflog + SJAS | Formal methods symposium; computational logic, deduction, synthesis |
| [SYNASC 2026](https://easychair.org/cfp/new) | **20 Jun 2026** | Proflog + core.logic | Symbolic computation + logic/programming track; co-located with FROM |
| [Deduktionstreffen 2026](https://resources.illc.uva.nl/LogicList/) | **30 Jun 2026** | Proflog + SJAS | German deduction meeting; **tableau provers explicitly in scope**; abstract-only |
| [HYDRA 2026](https://easychair.org/cfp/new) | **15 Jul 2026** | Proflog + core.logic | Hybrid deductive/inductive; weak unless synthesis angle leads |
| [NWPT 2026](https://easychair.org/cfp/new) | **19 Nov 2026** | core.logic + Proflog | Nordic PL **theory** workshop; engine semantics, not industry-facing |

### Automated reasoning & formal methods

| Venue | Deadline | Components | Notes |
|-------|----------|------------|-------|
| [ICFEM 2026](https://resources.illc.uva.nl/LogicList/) | abstract **1 Jun**; paper **8 Jun 2026** | Proflog + SJAS | Executable specifications, formal engineering |
| [ICTAC 2026](https://resources.illc.uva.nl/LogicList/) | abstract **8 Jun**; paper **15 Jun 2026** | Proflog + SJAS | Theoretical aspects of computing; formal methods |
| [ICTCS 2026](https://easychair.org/cfp/new) | **14 Jun 2026** | Proflog + core.logic | Italian TCS; computational logic, theorem proving |
| [SEFM 2026](https://resources.illc.uva.nl/LogicList/) | abstract **16 Jun**; paper **23 Jun 2026** | Proflog + SJAS | Software engineering & formal methods; tool papers welcome |
| [GandALF 2026](https://resources.illc.uva.nl/LogicList/) | abstract **18 Jun**; paper **22 Jun 2026** | SJAS + Proflog | Games, automata, logics, **automated deduction** |
| [RP 2026](https://resources.illc.uva.nl/LogicList/) | regular **21 Jun 2026**; pres-only **26 Jul** | Proflog | Reachability / search / termination in proof search |
| [RADICAL 2026](https://easychair.org/cfp/radical2026) | **12 Jun 2026** | Proflog (stretch) | Concurrency + logic; LP semantics angle only |
| [LANMR 2026](https://easychair.org/cfp/LANMR2026) | **5 Jun 2026** | **All three** | Latin America; **tableaux**, ASP, theorem proving, declarative programming explicitly listed |
| [LMPL 2026](https://lmpl26.hotcrp.com/) | **27 Jun 2026** (ET) | Proflog (stretch) | SPLASH/ISSTA; only if LLM-assisted proof search is the lead |

### Foundations, weak arithmetic, proof theory

| Venue | Deadline | Components | Notes |
|-------|----------|------------|-------|
| [Colloquium Logicum 2026](https://resources.illc.uva.nl/LogicList/) | **30 Jun 2026** (abstracts) | **SJAS** | Mathematical logic / foundations; incompleteness & self-reference |
| [JAF 2026](https://jaf45warsaw.wfz.uw.edu.pl/) | **15 Jun 2026** | **SJAS** | **Weak arithmetics** — natural home for Willard boundary material |
| [Trends in Logic XXVI](https://resources.illc.uva.nl/LogicList/) | **31 Jul 2026** | **SJAS** | Non-classical / philosophical logic; weak arithmetic & self-reference |
| [NCL'26](https://easychair.org/cfp/new) (Toruń) | **25 Jun 2026** | SJAS | Non-classical logics; proof theory, automated reasoning |
| [Proof Society 2026](https://resources.illc.uva.nl/LogicList/) | reg **4 Jun**; upload **7 Jun 2026** | SJAS | School + contributed workshop; structural proof theory, proof systems |
| [FilMat 2026](https://resources.illc.uva.nl/LogicList/) | (check site) | SJAS (philosophical) | Philosophy of mathematics: applicability, ontology, reasoning |
| [Amsterdam Colloquium 2026](https://events.illc.uva.nl/AC/AC2026) | **1 Sep 2026** | SJAS (weak) | 2-page abstract; semantics/non-classical logics workshop |

### August–November 2026

| Venue | Deadline | Components | Notes |
|-------|----------|------------|-------|
| [SBMF 2026](https://easychair.org/cfp/sbmf2026) | abstract **31 Jul**; paper **7 Aug 2026** | Proflog + SJAS | Brazilian FM; **short papers = system descriptions** |
| [FMAS 2026](https://easychair.org/cfp/new) | abstract **14 Aug**; paper **17 Aug 2026** | Proflog (weak) | Autonomous systems — stretch unless certifiable reasoning |
| [LNGAI 2026](https://easychair.org/cfp/new) | **6 Jul 2026** | SJAS (philosophical) | Logic-based AI; not proof-engine focused |
| [AILA 2027](https://easychair.org/cfp/new) | **30 Nov 2026** | SJAS + Proflog | AI + logic; 2027 cycle |
| [FSEN 2027](https://easychair.org/cfp/new) | **28 Oct 2026** | Proflog + SJAS | Software engineering + formal methods |

---

## Tier 3 — Partial fit / specialized / regional

| Venue | Deadline | Fit | Notes |
|-------|----------|-----|-------|
| [TyDe 2026 extended abstracts](https://tydeworkshop.org/) | **24 Jun 2026** | Proflog (weak) | Typed/proof-carrying frontends if you have a thin story; full papers closed |
| [FeSchi 2027](https://resources.illc.uva.nl/LogicList/) | **11 Jun 2026** | Proflog + SJAS | David Basin festschrift; security/FM/logic mix |
| [CICM 2026](https://easychair.org/cfp/new) | closed **15 Apr 2026** | Proflog + SJAS | Intelligent computer mathematics; next cycle |
| [FMCAD 2026](https://easychair.org/cfp/new) | closed **11 May 2026** | Proflog | Hardware/SW verification; certificate angle |
| [MFCS 2026](https://mfcs2026.irif.fr/) | closed **24 Apr 2026** | core.logic (weak) | TCS foundations |
| [LPNMR 2026](https://easychair.org/cfp/new) | closed **17 May 2026** | Proflog | Logic programming + non-monotonic reasoning; Klagenfurt Sep 2026 |
| [RuleML+RR 2026](https://resources.illc.uva.nl/LogicList/) | closed **8 May 2026** | Proflog / SJAS | Rules and reasoning |
| [WLP 2026](https://resources.illc.uva.nl/LogicList/newsitem.php?id=12535) | closed **5 May 2026** | Proflog + core.logic | German LP community; consider **2027** cycle |
| [II Workshop Substructural Logics](https://resources.illc.uva.nl/LogicList/) | **9–10 Jun 2026** (check) | SJAS (weak) | Tübingen; proof-theoretic adjacent |
| [Computable90](https://resources.illc.uva.nl/LogicList/) | (check site) | SJAS | Gödel/Turing anniversary; foundations |
| [CCA 2026](https://resources.illc.uva.nl/LogicList/) | closed | weak | Computability/analysis |
| [MCU 2026](https://resources.illc.uva.nl/LogicList/) | closed **10 May 2026** | weak | Machines, computations, universality |

---

## Journal & special-issue tracks (not conferences)

| Outlet | Deadline | Fit | Notes |
|--------|----------|-----|-------|
| [JLC — Women in Logic 10](https://womeninlogic.org/workshops/2026/special-issue/) | **15 Aug 2026** | All three | First author must identify as female; logic programming, automated deduction, proof theory in scope |
| Connexive Logic (J. Applied Non-Classical Logics) | **30 Sep 2026** | SJAS (niche) | LogicList announcement |
| TCS special issue — Universality in Logics and Physics | **31 Dec 2026** | SJAS + Proflog | LogicList announcement |

---

## Competition & tool-evaluation tracks

| Event | Registration | Fit | Notes |
|-------|--------------|-----|-------|
| [CASC-J13](https://aarinc.org/Newsletters/150-2026-05.html) | **29 Jun 2026** | Proflog (unlikely) | ATP competition at IJCAR/FLoC; only if packaged as FOL prover |
| ProoVer-2026 | **29 Jun 2026** | Proflog (stretch) | Proof **verifier** competition; certificate checker angle |
| TermComp 2026 | **26 Jun 2026** | weak | Termination competition |

---

## Schools & abstract-only forums (networking / dry runs)

| Event | Deadline | Relevance |
|-------|----------|-----------|
| [Autumn School Proof and Computation](https://resources.illc.uva.nl/LogicList/) | apply **1 Jun 2026** | Proof theory, constructive math, program extraction |
| [FoPSS 2026](https://resources.illc.uva.nl/LogicList/) | register early **1 Jun 2026** | FLoC summer school; types, proof assistants, neurosymbolic |
| [Proof Society 2026](https://resources.illc.uva.nl/LogicList/) | **7 Jun 2026** | Contributed talks; structural proof theory |
| [Deduktionstreffen 2026](https://resources.illc.uva.nl/LogicList/) | **30 Jun 2026** | Tableau provers in scope; informal German-community venue |
| [WPM26](https://resources.illc.uva.nl/LogicList/) | invitation / register **1 Aug 2026** | Proof mining; not SJAS-primary |
| SAT/SMT/AR Summer School | register **1 Jun / 6 Jul 2026** | Automated reasoning literacy |

---

## Industry & community (talks, not proceedings)

| Venue | Status (Jun 2026) | Fit |
|-------|-------------------|-----|
| **Clojure/Conj 2026** | **submitted** | Primary Clojure + core.logic outlet |
| [EuroClojure 2027](https://2027.euroclojure.org/) | mailing list only; **19–21 May 2027**, Prague | Next European Clojure talk cycle |
| Heart of Clojure | no 2026 CFP found (last: 2024) | Community FP/Clojure; monitor |
| Lambda World 2026 | CFP closed Mar 2026 | FP conference Málaga; hybrid |
| Lambda Days / Functional Conf (Poland/India) | verify independently | FP industry; experience reports |
| Code Mesh / LambdaConf | no confirmed 2026 CFP | Historical FP/industry cross-lang events |
| [Strange Loop](https://strangeloop.org/) | **defunct** (final 2023) | — |
| IU Logic Seminar (see [lopstr-ppdp26/intent.txt](../../lopstr-ppdp26/intent.txt)) | local | SJAS dry run |

---

## Next-cycle strategic targets (not yet open / closed for 2026)

| Venue | Expected window | Components | Notes |
|-------|-----------------|------------|-------|
| **PADL 2027** | ~Oct 2026 (POPL week Jan 2027) | Proflog + core.logic | Declarative languages; TPLP journal path for LP papers |
| **PEPM 2027** | ~Oct 2026 | Proflog | Partial evaluation / program manipulation / synthesis |
| **ICFP 2027** | ~Feb 2027 | core.logic | PACMPL; relational/functional semantics |
| **OOPSLA / PACMPL 2027** | R1 ~Oct 2026 | core.logic perf | PL research; narrow fit |
| **ITP 2027** | CFP TBD (host bidding closed Mar 2026) | SJAS certificates | Interactive proving; proof pearls |
| **TABLEAUX + FroCoS 2027** | CFP likely late 2026 | Proflog + SJAS | Host bids closed Mar 2026 |
| **IJCAR 2028** | site proposals due **6 Jul 2026** | — | Host bid, not paper |
| **ICLP 2027** | not announced (2026 was FLoC) | Proflog + core.logic | Premier LP conference |
| **JELIA 2027** | not announced (2025 was last) | SJAS + Proflog | Logics in AI |
| **FLoC 2028** | ~Feb 2027 submissions (typical) | **All three** | IJCAR, ITP, LICS, FSCD, ICLP cluster |
| **AiML 2027** | ~Feb 2027 | SJAS (modal stretch) | Modal/proof-theoretic logic |
| **WLP 2027** | ~Apr 2027 | Proflog + core.logic | German LP workshop at INFORMATIK |

---

## Recommended submission matrix (post–LOPSTR/mk/Conj)

| Paper story | Best remaining targets | De-emphasize |
|-------------|------------------------|--------------|
| **SJAS correspondence / proof theory** (ADR-0100+) | CSL 2027 → LPAR → Colloquium Logicum → Trends in Logic → JAF | Clojure/Conj, LMPL |
| **SJAS system / builder** (extend LOPSTR line) | LPAR regular → SBMF short → ICFEM → ETAPS iFS | miniKanren (already submitted) |
| **Proflog kernel / core.logic engine** | LPAR tool → NWPT 2026 → ETAPS TACAS/FoSSaCS → PADL 2027 | SJAS theory venues |
| **Proof synthesis & certificates** (ADR-0095) | LPAR → VSTTE → SEFM → GandALF | TyDe, LNGAI |
| **Practitioner / REPL / workflow** | (Conj submitted) → EuroClojure 2027 talk → local seminars | CSL, JAF |

---

## Sources used for expansion

Primary listings (same methodology as original plan, widened):

- [AAR Newsletter #150 (May 2026)](https://aarinc.org/Newsletters/150-2026-05.html)
- [LogicList 2026 archive](https://resources.illc.uva.nl/LogicList/?year=2026)
- [EasyChair Smart CFP](https://easychair.org/cfp) (keyword scan: logic, proof, programming, formal, tableau, synthesis)
- [ETAPS 2027 joint CFP](https://etaps.org/2027/cfp/)
- Official SIGPLAN pages where applicable

**Always re-check deadlines on the venue site before submitting** — secondary listings lag (e.g. LOPSTR moved to 31 May 2026 after earlier announcements).

---

## Practical notes

1. **No double submission:** LOPSTR (SJAS system) and mk2026 (kernel) remain distinct theses; LPAR/CSL variants should differ in claims, not just page count.
2. **LPAR has two bites:** regular/tool by **17 Jun 2026**, informal short presentations by **17 Aug 2026**.
3. **Abstract-only venues** (Colloquium Logicum, Deduktionstreffen, JAF, Trends in Logic) suit SJAS incompleteness material when a full paper is not ready.
4. **Journal track:** JLC WiL special issue (**15 Aug 2026**) accepts logic programming and proof theory if eligibility criteria are met.
