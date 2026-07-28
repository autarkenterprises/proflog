# Willard Deductive-Apparatus Survey: Resolution and Other Candidates

Date: 2026-07-27

## Question

Review the available Dan E. Willard corpus, including published papers,
preprints, talks, and the local nachlass, for every reference to resolution and
for any other non-tableau, non-Hilbert, non-Herbrand proof mechanism that might
serve as the deductive apparatus `D` of a self-justifying axiom system (SJAS).
Sequent calculus is an explicit audit category, including cut-free and
cut-permissive variants rather than an incidental synonym search. Then
determine which mechanisms can actually support an SJAS and what a correct
implementation would have to establish.

## Decisive Answer

**Yes at the level of Willard's stated metatheorems. Resolution can serve as
the deductive apparatus of an SJAS, but only after "resolution" is made into
one exact, naturally encoded proof system.** There are two independent
affirmative statements in Willard's work:

1. In the 2011 preprint, Willard defines a generic configuration `xi_R` whose
   deduction method is a Herbrand-style method or a resolution system that
   relies on Skolemization in the same way. He states that `xi_R` is E-stable
   and therefore has a self-justifying extension recognizing its
   `Level(0R)` consistency.
2. In Section 8 of the 2020 preprint, Willard explicitly replaces semantic
   tableau with resolution. He defines ordinary `Res` and LEM-enriched `Xres`,
   then states that `ISRes(.)` is consistency-preserving while `ISXres(.)` is
   not. This is the direct resolution analog of the paper's Level-1 `Tab`
   versus `Xtab` boundary.

**Sequent calculus has two materially different answers.** First-order
**cut-free sequent calculus** is an affirmative candidate for the
total-addition/no-total-multiplication SJAS line. Willard says in 1993 that the
same analysis applies to it, says in 2001 that a Group-3 axiom may use it, and
says in the 2005 JSL paper that the `IS_D(.)` transformation is
consistency-preserving for it after the required compactification analysis.
Gentzen sequent calculus **with unrestricted deductive cuts** is Hilbert-like.
The 2001 construction does permit it as the proof predicate of a much weaker
self-justifying system, but that construction recognizes neither addition nor
multiplication as total. In the stronger Solovay comparison, a system proving
the relevant translated subtraction/division theory cannot combine this
Hilbert-like consistency claim even with total successor. It is not an
interchangeable sequent implementation for the total-addition cut-free result.

The evidentiary qualification matters. The accessible papers state these
resolution and cut-free-sequent generalizations, identify the mechanisms and
the metatheorems they instantiate, and sometimes call the omitted extension
"easy" or "obvious." They do **not** print a complete resolution-specific
stability/compactification proof. Thus the literature answers "can this be an
SJAS basis?" affirmatively, but it does not supply a finished executable or
machine-checked `D_Res`. A Proflog completion claim still has to discharge the
criteria below.

Outside the tableau/Tab-k, Hilbert, and Herbrand families excluded by the
question, no other named first-order proof calculus is established as an SJAS
basis in the accessible Willard corpus. Willard's broad 2011 remark about
natural Skolemizing deduction schemes defines a possible family, but it is not
a license to substitute an arbitrary modern resolution prover, superposition
engine, connection calculus, or proof trace. The corpus contains no
substantive mention of those named mechanisms.

Several other items occur in Willard's discussions of incompleteness:
Kreisel-Takeuti Cut-Free Analysis, definable-cut localization, passive
induction, Gentzen transfinite induction, Goedel's Dialectica interpretation,
and Artemov's infinite-schema approach. They are not alternative values of
`D` in Willard's fixed-point `IS_D(beta)` architecture. They change the logic,
the domain on which consistency is asserted, the status of consistency as an
axiom versus a theorem, or the shape of the consistency target.

## What "Deductive Apparatus" Includes

The distinction between `Res` and `Xres` is easy to miss if `D` is treated as
only an inference-rule name. Willard's 2020 Definition 3.2 defines a deductive
apparatus as the **union of its logical axioms and its inference rules**. Thus:

- `Res` is not merely a search strategy that happens to call a resolution
  routine.
- Adding all instances of the Law of Excluded Middle as logical axioms changes
  the apparatus to `Xres`, even though the terminal clause rule is still
  resolution.
- The self-referencing Group-3 sentence changes because its proof predicate
  names the whole apparatus, including those logical axioms.

This is why theorem-set equivalence does not preserve self-justification.
Proof length, proof-object structure, and the exact arithmetized proof
predicate are part of the mathematical object.

## Corpus Control and Limits

### Logic corpus searched in full

The repository corpus authority for this pass is the fetched
[`jpt4/sjas`](https://github.com/jpt4/sjas) `master` tree at commit
`18e24bc3883d2640695f968e9b48730a6c2bcce2`. Its reachable remote has one
branch and no tags. The current tree contains 27 PDF entries under
`nachlass/papers` (including the DIMACS symlink), 18 scanned PDFs under
`nachlass/collected_dew_materials`, and the additional Willard witnesses under
`lit` described below. The complete reachable filename history was also
searched so that a renamed or deleted paper would not be mistaken for an
unavailable one.

The full-text pass covered the following logic sequence:

| Year | Work | Result relevant to this survey |
| --- | --- | --- |
| 1993 | *Self-Verifying Axiom Systems* | Resolution and cut-free sequent named as natural, complete, arithmetizable cut-free proof methods; same consistency analysis asserted. |
| 1997/1998 | *Self-Reflection Principles and NP-Hardness* | `IS*` may use a cut-free sequent proof predicate. No object-level resolution calculus is proposed: "SAT-Resolution subroutine" is a SAT decision algorithm used by a `P=NP` argument. |
| 2000 | *The Semantic Tableaux Version ... Almost to Q* | Negative multiplication result said to generalize to resolution and cut-free sequent calculus. |
| 2001 | *Self-Verifying Axiom Systems, the Incompleteness Theorem and Related Reflection Principles* | Group-3 may use resolution or cut-free sequent; later negative results cover them too. |
| 2002 | *How to Extend the Semantic Tableaux and Cut-Free Versions ... Almost to Q* | Each proof method requires its own `SemPrf`-dependent `V_d`; resolution and cut-free sequent named. |
| 2002 | *Some New Exceptions for the Semantic Tableaux Version ...* | CFA/localized consistency distinguished from this SJAS architecture. |
| 2004 | *A Version of the Second Incompleteness Theorem ... Addition but not Multiplication* | Negative results and proof-list analogs extend to cut-free sequent proofs. |
| 2005 | *An Exploration ... Recognizing Solely Addition ...* | `IS_D(.)` consistency preservation explicitly extends to cut-free sequent calculus. |
| 2005 | *On the Partial Respects in which a Real Valued Arithmetic System Can Verify its Tableaux Consistency* | Sequent calculus listed as a possible `D`; no additional calculus-specific theorem. |
| 2006 | *A Generalization of the Second Incompleteness Theorem ...* | Cut-free sequent, CFA, and definable-cut contexts; no resolution occurrence. |
| 2006 | *The Axiom System I-Sigma-0 ... Herbrandized Version ...* | Skolemization/Herbrand analysis; no independent alternate calculus. |
| 2006 | *On the Available Partial Respects ... Real Valued Arithmetic* | `Tab-1` hybrid; no non-tableau candidate beyond generic cut-free comparison. |
| 2006 | *On the Nature of Goedel's Second Incompleteness Theorem* | Short survey/talk; no new candidate. |
| 2007 | *Passive Induction ...* | Cut-free predicate sequent (`LK-provability`) named; passive induction is an axiom/proof-length device, not `D`. |
| 2007 | *On the Results of a 14-Year Effort ...* | Summarizes cut-free and Level-1 results; no new apparatus. |
| 2009 | *Some Specially Formulated Axiomizations for I-Sigma-0 ...* | Detailed separation of CFA, cut-localized consistency, and global fixed-point SJAS. |
| 2011 | *A Detailed Examination ... Self-Justifying Logics* | Direct `xi_R` resolution construction and broad Skolemizing-family remark. |
| 2013 | *On the Significance ... Analytic Tableaux* | CFA and interpretational frameworks explicitly declared separate. |
| 2014 | *On the Broader Epistemological Significance ...* | Repeats the separation; primary positive route remains Tab/Tab-1. |
| 2016 | *On Introducing a New Theta Function Symbol ...* | Mentions transfinite induction, CFA, and interpretation methods as unrelated approaches. |
| 2018 | *About the Chasm Separating the Goals of Hilbert's Consistency Program ...* | Bibliographic and conceptual survey; no new alternate `D`. |
| 2020 | *How the Law of Excluded Middle Pertains ...* | Direct `Res`/`Xres` Level-1 generalization. |

The pass also screened the available non-logic works and duplicate witnesses:
the fusion-tree paper, relational-calculus paper, Willard-Fredman scan, and
Trivers-Willard biology paper. Their uses of ordinary words such as
"resolution" are not proof-theoretic.

The five Willard-named PDFs under `sjas/lit` were reconciled explicitly. The
1993 PDF is a unique mapped source. The DIMACS, theta-function, and 2020 PDFs
are byte-identical aliases of copies under `sjas/nachlass/papers`. The remaining
34-page `willard2014sjassignificanceanalytictableaux.pdf` is binary-distinct
from the mapped 2013 analytic-tableaux PDF, but whitespace-normalized extracted
text is identical, including its three ordinary `resolve*` occurrences and its
CFA/interpretational discussion.

The repository also contains resolution-related supporting literature that is
not part of the Willard occurrence corpus. In particular,
`sjas/lit/IntResolution.pdf` is Melvin Fitting's *Resolution for Intuitionistic
Logic*, not a Willard paper. Its signed, non-clausal intuitionistic procedure is
a distinct apparatus; it cannot silently supply the classical
Skolemizing-resolution details omitted by Willard. `ClausalLanguage.pdf` is by
Jan Komara and Paul J. Voda and describes a signed-tableau proof system. These
files are useful design references but are not evidence of an additional
Willard theorem.

This list was cross-checked against references [57]-[72] in Willard's 2018
paper, which he describes as the major sequence of his incompleteness and SJAS
work, and against the current
[DBLP Dan E. Willard bibliography](https://dblp.org/pid/w/DanEWillard).

The wider bibliographic control did not stop at the logic sequence. As of this
survey, DBLP's author record reports 53 publications, while OpenAlex returns 73
raw work records before duplicate preprint, conference, and journal versions
are reconciled. Every title and available abstract in those records was
screened, together with the repository's `sjas/nachlass/paperlist`. This caught
three older computer-science title occurrences that a logic-only pass would
miss:

- 1984, *Log-Logarithmic Protocols for Resolving Ethernet and Semaphore
  Conflicts*;
- 1985, *Algorithms for Resolving Conflicts in Dynamic Storage Allocation*;
  and
- 1986, *Log-Logarithmic Selection Resolution Protocols in a Multiple Access
  Channel*.

The first and third concern contention resolution in communications channels;
the second concerns fragmentation conflicts in storage allocation. None uses
"resolution" as a logical proof rule. They are nevertheless retained in the
occurrence ledger below because the instruction for this survey was not to
discard even small or clearly non-proof-theoretic mentions.

### Full-text and version gaps

An unqualified claim to have searched every word of every extant publication
version would be false. Five logic documents or publication versions were
identified but could not be searched in their exact published form:

1. The unpublished March 1994 SUNY technical report, *Self-Verifying Axiom
   Systems and the Incompleteness Theorem*. The 2001 JSL bibliography describes
   it as a 50-page expansion of the abbreviated proofs in the 1993 paper.
2. The closed 1997 KGC chapter
   [*The Tangibility Reflection Principle for Self-Verifying Axiom Systems*](https://dblp.org/rec/conf/kgc/Willard97).
3. The closed LFCS 2020 proceedings version,
   [*On the Tender Line Separating Generalizations and Boundary-Case Exceptions ...*](https://dblp.org/rec/conf/lfcs/Willard20).
4. The version of record of the closed 2021 JLC article,
   [*About the Characterization of a Fine Line ...*](https://dblp.org/rec/journals/logcom/Willard21).
5. The five-page TABLEAUX 2003 position paper, *A New Form of the Semantic
   Tableaux Version of the Second Incompleteness Theorem*, published on
   pp. 68-72 of University of Rome Technical Report RT-DIA-80-2003.

The gaps are narrower than this list alone suggests, but they remain real:

- The 2001 article is the later 60-page journal treatment of the early
  self-verification and tangibility line. It explicitly identifies both the
  1994 report and the 1997 chapter as predecessors and incorporates their
  subject matter. That is strong overlap, not a word-for-word substitute.
- The local `2025-12-22 09.55.35.pdf` scan consists of a one-page referee report
  followed by a complete, marked author manuscript whose printed pagination
  runs through page 25. Its title is the LFCS-style *On the Tender Line ...*,
  but its citation note says the LFCS paper was a shorter, less-polished
  conference version of "this article." The referee suggests replacing
  "Tender" with "Fine"; the eventual JLC title makes that change. The
  manuscript's Section 8 contains the same `Res`/`Xres` paragraphs as
  arXiv:2006.01057. It is therefore a complete author-manuscript witness from
  the LFCS/JLC publication line, apparently the submitted manuscript
  predecessor to the JLC article, but it is not either publisher's version of
  record.
- The local `2025-12-22 09.58.14.pdf` contains only the title and abstract page
  of the forthcoming JLC article on its ninth physical page. It is not a second
  copy of the full article.
- The public 28-page arXiv preprint supplies the full argument and Section 8;
  DBLP and Unpaywall classify both the LFCS and JLC publisher versions as
  closed. A GBV-hosted LFCS PDF located during the survey contains only three
  pages of front matter and contents, not Willard's chapter. The JLC table of
  contents still identifies a Section 8 named "Further generalizations," but
  neither item can replace a full-text search.
- Willard's repository CV identifies the 2003 item and says it announced one
  of the results later invited for publication. The fetched GitHub tree, every
  reachable historical path/blob, all text-extractable PDFs under `nachlass`,
  and the collected-material OCR contain neither its title nor
  `RT-DIA-80-2003`. The archived conference programme confirms the item, but
  only report metadata and contents have been located. The later 2004 and 2006
  papers cover the announced research line, but are not a word-for-word
  occurrence substitute.

Older closed database, data-structure, and communications publications are a
second, non-logic full-text limitation. Their bibliographic records, titles,
and available abstracts were screened, and the three resolution-related titles
are accounted for above. Their subject matter predates or lies outside the SJAS
programme, so they supply no candidate deductive apparatus; nevertheless this
survey does not claim a word-occurrence census inside publisher files that
could not be obtained. Any later acquisition of a gap document must trigger a
supplemental pass.

### Nachlass coverage

The current nachlass manifest catalogs 18 PDFs. One,
`DOC102924-10292024193836.pdf`, is an exact SHA-256 duplicate of
`20080318_ZCF_notes.pdf`; accordingly the collection contains 17 distinct
high-fidelity OCR texts, and all 17 were searched. The collection README's
"19 PDF files, 18 unique OCR targets" summary is stale relative to the current
manifest, files, and recorded duplicate checksum. Counts in this survey follow
those current artifacts.

The OCR is a discovery aid, not an authoritative transcription; formulae and
handwriting were checked against scans where a hit affected the classification.

| Nachlass item | Finding |
| --- | --- |
| `2020Notes.pdf` | No logical-resolution occurrence and no new alternate `D`; two ordinary-English instances of "resolved" occur in repeated note material. |
| Three March 2008 ZCF drafts | No logical resolution. The March 21 draft says a proof helps in "resolving" an old open question and uses Gentzen cut-free sequent consistency in a set-theory reflection argument, not an `IS_D(beta)` SJAS. |
| `ZFnote.pdf` | No candidate apparatus. |
| 2014 notarized notes, high and low resolution | Generic `d`, tableau, and Hilbert comparison; no new candidate. |
| Hajek correspondence | No resolution candidate; OCR quality is only fair. |
| 2014 exam | Nine uses of "resolving" refer to hash-table collisions; teaching material, not a proof calculus. |
| PhD notes | Database-era material, no candidate. |
| Computational-dynamics notes | Non-logic material, no candidate. |
| 1991 NSF report | Administrative material, no candidate. |
| 2025-12-22 scan A | One-page referee report plus a complete marked author manuscript, printed pp. 1-25, from the Tender/Fine Line publication sequence. Section 8 contains the same `Res`/`Xres` result as the arXiv preprint. This is an independent version witness, not a new theorem. |
| 2025-12-22 scan B | `Tab`, `Tab-1`, and `Xtab` notes, followed on physical p. 9 by only the title/abstract page of the forthcoming JLC article; it is not the full JLC text. |
| 2025-12-24 scan A | Biology material. |
| 2025-12-24 scan B | Artemov Step-by-Step Infinite-Schema (`SBSIS`) discussion; not a proof calculus `D`. |
| ZCF fragment | No candidate. |
| Exact March 2008 alias | Duplicate, not searched as an independent text. |

## Exhaustive Resolution Occurrence Ledger

This section accounts for every case-insensitive occurrence matching the full
word-family search
`\b(resolv[a-z-]*|resolut[a-z-]*)\b` in the accessible full-text/OCR corpus,
including false positives. It also records word-family occurrences in the
bibliographic titles and available abstracts of inaccessible older non-logic
publications. Duplicate files and overlapping publication versions are
identified as version witnesses rather than silently counted as new claims.

The initial repeatable regex pass produced 66 tokens in 21 text-bearing
paper/OCR extraction targets. A separate page-image OCR pass over the otherwise
image-only 1998 DIMACS chapter produced four more tokens. A supplemental pass
over the binary-distinct 2014 analytic-tableaux version produced the same three
tokens as its normalized-text-identical 2013 witness. Three older
bibliographic-title tokens were also inspected. These raw counts are
diagnostic, not distinct-claim totals: the initial map itself includes some
exact aliases and overlapping versions. The ledger below is authoritative
about which witnesses are duplicates. The 2020 abbreviations `Res`, `Xres`,
`ISRes`, and `ISXres` are inventoried in their own subsection but are not
included in the lexical-regex counts.

### 1984-1986: communications and storage false positives

Three bibliographic titles use the same word family outside logic:

- *Log-Logarithmic Protocols for Resolving Ethernet and Semaphore Conflicts*
  coordinates competing users of a communications channel.
- *Algorithms for Resolving Conflicts in Dynamic Storage Allocation* handles
  storage fragmentation.
- *Log-Logarithmic Selection Resolution Protocols in a Multiple Access Channel*
  gives randomized selection protocols for shared-channel contention.

Here "resolution" means resolving resource contention or allocation conflicts,
not deriving clauses or refuting a first-order formula. The 1986 SIAM abstract
confirms that its objects are multiple-access selection protocols.

### 1993: direct affirmative treatment

In *Self-Verifying Axiom Systems*:

- Printed p. 325 says the research requires an intensionally correct, natural
  inference method similar to semantic tableau or resolution. It contrasts
  these with numerically correct but intensionally incorrect "unnatural"
  deduction methods.
- Printed p. 327 says proof encodings may use tableau, resolution, or cut-free
  sequent calculus; completeness holds for each; and the proof predicate can
  be encoded as a bounded (`Delta_0` in the paper's setting) formula.
- Printed p. 332 says tableau and resolution contradiction proofs have the
  relevant subcomponent/cut-free property, compares it to Gentzen cut-free
  sequents, and says the same consistency analysis applies to resolution and
  cut-free sequent proofs.

This is not merely a passing analogy. It supplies the early proof-object,
arithmetization, completeness, and structural reasons for treating resolution
as an admissible `d`.

The same paper conjectures that cuts might later be allowed. That conjecture
must be read historically: Willard's later sharp negative results show that
unrestricted cut/LEM efficiency crosses the total-successor/total-addition
boundary relevant to that construction. The 2001 cut-permissive result succeeds
only by moving to the weaker no-total-addition profile described below.

### 1997/1998 DIMACS chapter: four false positives, no resolution `D`

In *Self-Reflection Principles and NP-Hardness*:

- Printed pp. 310-311 define `SAT(a,b)` by twice saying that an algorithm can
  "resolve any SAT problem" of a given length, then call that assumed
  deterministic SAT decision procedure a "SAT-Resolution subroutine." It is
  used under a `P=NP` hypothesis to recover bits of a proof. These three tokens
  describe one decision algorithm, not the object-language proof predicate or
  a resolution refutation calculus.
- Printed p. 316 uses "a resolution to the P=?NP Open Problem" in ordinary
  English.

The chapter does mention Herbrand, tableau, and cut-free sequent methods in
its cut-free consistency comparisons. Remark 6.3 names only Herbrand and
tableau; OCR must not be "corrected" into adding resolution there.

### 2000: direct negative-boundary extension

The final remarks of *The Semantic Tableaux Version ... Almost to Q*
(printed p. 429) say that the total-multiplication incompleteness theorem can
also be generalized to Herbrand deduction, cut-free sequent calculus, and
resolution.

This is method-specific negative evidence. It does not negate the positive
weak-arithmetic construction; it says resolution does not evade the same
arithmetic expressivity boundary.

The fusion-tree paper has four ordinary-English uses, including "final
resolution to this open question"; all are itemized in the consolidated
false-positive table below.

### 2001: direct positive and negative treatment

In the 2001 JSL paper:

- Remark 5.6 (journal p. 569) says it is easy to generalize Theorem 5.1 so the
  `IS_lambda(A)` Group-3 axioms use any cut-free method, explicitly including
  Resolution and Cut-Free Sequent Calculus.
- Section 7 (journal p. 572 onward) says its negative theorems cover all
  conventional methods, explicitly including resolution and Gentzen sequent
  calculus, and even future methods that are only locally sound and complete.
- The closing philosophical discussion uses "full resolution" and "partial
  resolution" in ordinary English.

The paper's public abstract independently summarizes that the systems can
verify their Semantic Tableaux, Herbrand, and Cut-Free consistencies:
[JSL record](https://doi.org/10.2307/2695030).

### 2002: method-specific proof predicates

In the 2002 JSL paper:

- Journal p. 469 says the negative result generalizes to Resolution and
  Cut-Free Sequent Calculus, but each method `d` requires a different
  `Pi_1` sentence `V_d`.
- Journal p. 487 repeats that replacing the proof method requires replacing
  `V` by the adjusted `V_d`.

The reason is explicit: `V_d` contains a bounded `SemPrf` subformula dependent
on the exact definition of proof. This rules out reusing a tableau `SemPrf`
predicate while feeding it a resolution trace.

### 2005: one ordinary-English occurrence

The 2005 JSL paper uses "one possible resolution for this ambiguity" while
discussing how to state Solovay's theorem. That occurrence is not a calculus.
The same paper nevertheless provides important independent positive evidence
for **cut-free sequent calculus** in Remark 1 of Section 5.

### 2011: direct `xi_R` construction

Appendix D of the 2011 preprint contains six resolution-family tokens in five
proof-theoretic passages:

- Physical pp. 49-50 state that many automated theorem provers use a variant of
  resolution to which the Ax-3 method applies.
- Footnote 32 says the result generalizes because resolution provers employ
  Skolemization analogously to Herbrand deduction.
- Footnote 33 defines `xi_R` with the usual arithmetic language, the Ax-3 base,
  a Herbrand-style or similarly Skolemizing Resolution system, and any natural
  Goedel coding.
- It says the resulting configuration is E-stable and has a self-justifying
  extension recognizing `Level(0R)` consistency.
- Table I labels the route "Resolution and/or Herbrandized analogs"; footnote
  35 says this applies essentially to any deduction scheme relying on
  Skolemization as an alternative to tableau existential elimination.

The later phrase "full resolutions to [a philosophical question]" on physical
p. 53 is ordinary English.

This route has a critical format caveat. `xi_R` is called Type Almost-M and can
prove multiplication total as a theorem, but the multiplication-totality
sentence is not a proper axiom; Table I marks Type-M "No" and Type-A "Yes" with
a footnote. It cannot be cited as evidence for a system whose reflected beta
simply contains total functional multiplication as a proper axiom.

The source is
[arXiv:1108.6330](https://arxiv.org/abs/1108.6330).

### 2020: direct `Res` versus `Xres` boundary

Section 8, physical pp. 19-20, says:

- resolution shares the relevant characteristics with tableau;
- Theorems 4.4 and 4.5 generalize when resolution replaces tableau;
- `Res-proof` means a resolution proof from the proper axioms;
- `Xres-proof` additionally admits every base-language LEM instance as a
  logical axiom;
- `ISRes(.)` is consistency-preserving, while `ISXres(.)` is not.

This is the strongest direct answer to the present question because it places
resolution inside the same Level-1 `IS_D(beta)` transformation used for the
paper's Tab/Xtab comparison.

The source is
[arXiv:2006.01057](https://arxiv.org/abs/2006.01057).

The complete marked author manuscript in the 2025-12-22 nachlass scan A
contains the same Section 8 claims. It is independent evidence that the
resolution paragraph survived in the submitted Tender/Fine Line publication
sequence, although it is not an independent theorem or a substitute for a
resolution-specific proof.

### Remaining resolve-family occurrences and false positives

The following table closes the wider word-family census. Logical occurrences
already analyzed above are noted only to distinguish them from ordinary uses.

| Work or material | Additional occurrence accounting | Classification |
| --- | --- | --- |
| 2000 tableaux paper | "had not resolved whether" appears in the introduction, in addition to the logical Resolution occurrence in the final remarks. | Ordinary English. |
| 2000 fusion-tree paper | "final resolution," "problem was resolved," "difficulty will be resolved," and "resolving Dijkstra's..." | Four data-structure/algorithm-history uses. |
| 2001 JSL paper | "full resolution" and "partial resolution" in the closing philosophical discussion, in addition to two logical Resolution occurrences. | Ordinary English. |
| 2002 JSL paper | "had not resolved fully whether" in the introduction, in addition to two logical Resolution occurrences. | Ordinary English. |
| 2002 relational-calculus paper | "procedure for resolving this query" and two instances of "resolve it." | Database-query evaluation, not theorem proving. |
| 2005 JSL paper | "one possible resolution for this ambiguity." | Ordinary English. |
| 2007 fourteen-year retrospective | Philosophical questions "never fully resolved" and earlier work "resolved a 20-year old Paris-Wilkie open question." | Ordinary English and research-history prose. |
| 2011 preprint | "never be resolved," "partially resolved," and "full resolutions" occur outside Appendix D's six logical-resolution tokens. | Ordinary philosophical prose. |
| 2013 analytic-tableaux paper and binary-distinct local 2014 version | Each has the same three uses of resolved/resolving describing how much of a conceptual challenge the incompleteness theorem settles; normalized extracted text is identical. | Ordinary philosophical prose and an overlapping version witness. |
| Two 2014 broader-epistemological versions | Each version contains "fully resolving this challenge" and "help resolve the mystery." | Overlapping publication-version witnesses, not a calculus. |
| 2016 theta paper and byte-identical `willard2017.pdf` alias | "method for resolving this problem" refers to the ISCE infinite-constant axiom/language construction; a second sentence asks whether another result would "resolve ISCE's main difficulties." | Axiom/language design, not an inference rule. The 2017 file is not a distinct occurrence source. |
| 2017 Trivers-Willard biology paper | "not resolve world-wide famine challenges." | Ordinary English outside logic. |
| `2020Notes.pdf` | Two "resolved" sentences occur in repeated note sections. | Ordinary English; no candidate `D`. |
| March 21, 2008 ZCF draft | The short proof is described as "resolving a 100 year-old standing open question." | Ordinary English; its cut-free-sequent content is classified separately below. |
| 2014 exam | Nine instances of "resolving collisions." | Hash-table collision handling, not logical resolution. |
| 2020 arXiv/local paper copies and 2025 author manuscript | Each full version has the three Section 8 logical-resolution tokens discussed above. | Overlapping witnesses for the same `Res`/`Xres` result. |

## Exhaustive Sequent-Calculus Occurrence Ledger

The sequent pass searched both direct names and vocabulary that can identify a
sequent result without the exact phrase `sequent calculus`:

`\b(sequents?|Gentzen(?:ian)?|LK(?:-provability)?|LJ|cut[- ]free|cut[- ]elimination|cut rule)\b`

Supplemental searches covered `antecedent`, `succedent`, `structural rule`,
`weakening`, `contraction`, `exchange rule`, `Gentzen-style`, and
`Gentzen-like`. The first regex produced 330 raw tokens across the mapped
extracts, the DIMACS image OCR, high-fidelity collected-material OCR, and
overlapping local versions. The count deliberately includes bibliographies,
duplicates, ordinary formula variables, and generic uses of `cut-free`; it is
a coverage diagnostic, not a count of 330 distinct claims.

The table below accounts for every raw hit group. Counts are per accessible
witness, so grouped version rows intentionally display their duplicate
contribution.

| Work or material | Raw hits | Complete disposition |
| --- | ---: | --- |
| 1993 *Self-Verifying Axiom Systems* | 19 | Direct affirmative first-order route: cut-free sequent proof codes are complete and bounded-arithmetizable, and the tableau consistency analysis is said to apply. The paper also distinguishes the Kreisel-Takeuti second-order route and cites Gentzen/cut-elimination background. |
| 1998 DIMACS chapter | 13 | Direct affirmative first-order route: the `IS*` Group-3 proof predicate may be cut-free sequent calculus. Separate passages contrast Kreisel-Takeuti second-order cut-free sequents with Willard's first-order cut-permissive system and compare definable-cut consistency with sequent calculus with cuts. One bibliography hit is Statman's Gentzen title. |
| 2000 TABLEAUX paper | 9 | The total-multiplication negative theorem is explicitly said to generalize to cut-free sequent calculus. Remaining hits are generic cut-free context, cut elimination, and references. |
| 2001 JSL paper | 24 | Both positive species occur. Cut-permissive Gentzen sequents are admitted for the weak no-total-addition TangPred/Hilbert-style construction; cut-free sequents are admitted for the total-addition construction and Group-3 generalization. Later negative theorems cover conventional Gentzen sequent proofs. CFA, cut elimination, and bibliography occurrences are separate. |
| 2002 JSL paper | 25 | Direct method-specific result: the cut-free theorem generalizes to Resolution, Herbrand, and Cut-Free Sequent Calculus, with a distinct `V_d`/proof predicate for each. Other hits explain cut-free proof-length growth and cite Gentzen/CFA. |
| 2002 TABLEAUX paper | 3 | Kreisel-Takeuti second-order cut-free sequent context and references only; no new first-order sequent theorem. |
| 2004 addition/not-multiplication paper | 19 | Direct positive and negative extension: Theorems 1 and 2 and their proof-list analogs generalize to cut-free sequent proofs, while sequent calculus with cut is the Hilbert-like comparator. Other hits distinguish definable cuts and cite prior work. |
| 2005 JSL addition paper | 22 | Direct affirmative application: Remark 1 of Section 5 says the compactification theorem makes `IS_D(.)` consistency-preserving for cut-free sequent calculus. Later statements extend negative and proof-list results to the same method. Second-order/CFA and definable-cut occurrences are comparisons. |
| 2005 simulated-real TABLEAUX paper | 20 | `D` may generically be sequent calculus, but the paper's implemented positive apparatus is Tab-k. Most hits concern limited Gentzen-style cuts, cut elimination, and the boundary at which increased deductive efficiency reactivates incompleteness. |
| 2006 APAL generalization paper | 15 | Cut-free sequent calculus is included in general cut-free negative/definable-cut analysis. Second-order Gentzen/CFA and generalized Goedel-sentence passages are not a new first-order `D`. |
| 2006 real-valued-arithmetic JSL paper | 10 | The operative system is Tab-k/Tab-1 with limited Gentzen-like cuts. Generic cut-free, Gentzen, and cut-rule discussion supplies a boundary comparison, not a new sequent construction. |
| 2006 WoLLIC Herbrand paper | 5 | Generic cut-free incompleteness history and a citation to the 2002 cut-free paper; no sequent-specific construction. |
| 2006 incompleteness talk | 1 | The sole hit is the title of the 2002 cut-free paper in the references. |
| 2007 passive-induction paper | 21 | Names the cut-free predicate sequent calculus as `LK-provability` and analyzes how passive axioms shorten such proofs. Passive induction modifies the axiom basis and proof lengths; it is not itself a calculus. Other hits concern Gentzen-style cut simulation and cut elimination. |
| 2007 fourteen-year retrospective | 8 | Summarizes the generic cut-free positive/negative line and Level-k limited Gentzen cuts; remaining hit is a prior-paper title. |
| 2009 Herbrand paper | 22 | Contrasts cut-permissive Gentzen sequents with cut-free sequent/Herbrand/tableau systems, CFA, definable cuts, and Herb-k limited cuts. The apparent `lk` in a URL is not a calculus name. No new sequent-specific construction is proved. |
| 2011 unifying preprint | 7 | Discusses Kreisel-Takeuti cut-free second-order sequents and distinguishes definable cuts from Gentzen cut. The affirmative new apparatus in this paper is `xi_R` resolution, not a new sequent route. |
| 2013 analytic-tableaux paper plus normalized-identical 2014 local copy | 12 | Six hits per version: CFA/second-order Gentzen comparison, Tab-k limited-cut discussion, and references. No new sequent construction. |
| Two 2014 broader-epistemological versions | 14 | Seven hits per version, all in the same CFA, Tab-k, Gentzen/Dialectica, and bibliography passages. They are overlapping witnesses. |
| 2016 theta paper plus byte-identical `willard2017.pdf` alias | 14 | Seven hits per copy. Gentzen transfinite induction and CFA are explicitly described as approaches unrelated to the paper's fixed-point method; the other hits are generic cut-free history and references. |
| 2018 chasm paper | 10 | Eight `cut rule` hits define the paper's linear-constrained-cut efficiency boundary; two `cut-free` hits are references. This is a proof-compression criterion, not a newly specified sequent calculus. |
| 2020 arXiv copy, byte-identical `lit` alias, and 2025 author-manuscript scan | 16 | Five substantive/bibliographic hits in each text copy classify Gentzen and Kreisel-Takeuti as separate cut-free evasions. The manuscript OCR contributes one additional false `L_j` formula-variable token. The paper's new direct apparatuses are `Res` and `Xres`. |
| `2020Notes.pdf` | 2 | One prior-paper title and one false `L_j` formula-variable token. |
| March 21, 2008 ZCF draft | 11 | Defines `ConsCF(alpha)` by absence of a cut-free Gentzen sequent proof of the empty sequent and invokes cut elimination. This is relative set-theoretic consistency for weaker finite fragments, not a fixed-point SJAS. |
| 2017 Trivers-Willard biology paper | 1 | A cut-free paper title in the bibliography; no proof-theoretic content in the biology text. |
| 2002 relational-query paper | 6 | `L_k`/`L_j` list variables extracted as `LK`/`LJ`; all six are lexical false positives. |
| PhD notes | 1 | Illegible OCR fragment read as `lj`; a false positive. |

All remaining primary-paper and collected-material targets produced zero terms
from the sequent vocabulary. The supplemental structural-rule search found no
unaccounted sequent calculus. Its `antecedent` hits in the 2001 paper are
Willard's semantic-tableau "Founding Antecedent," and isolated `weakening` or
`contraction` words occur in non-sequent senses.

## Other Candidate Mechanisms

### 1. First-order sequent calculus: yes, with a cut-sensitive split

The cumulative evidence is affirmative for two different arithmetic profiles:

- 1993: proof encodings may be cut-free sequents; the proof predicate is
  bounded-arithmetizable; the same consistency analysis applies.
- 1998: the `IS*` Group-3 proof predicate may designate cut-free sequent
  calculus.
- 2001: Group-3 can use Cut-Free Sequent Calculus.
- 2004: the negative theorems and proof-list analogs extend to cut-free
  sequent subproofs.
- 2005 JSL, Remark 1 of Section 5: after showing the required
  `theta`-compactification condition, Theorem 1 would prove `IS_D(.)`
  consistency-preserving when `D` is cut-free sequent calculus. Remark 1
  states this application but says the detailed case analysis is omitted for
  space.
- 2007: the passive-induction paper calls the cut-free predicate sequent
  version `LK-provability`, confirming the intended first-order species.

For systems that do not recognize addition as total, the 2001 paper separately
permits Gentzen's sequent calculus **with deductive cuts** wherever its
TangPred construction permits Hilbert proofs. This is a genuine
self-justifying route, but it inherits the Hilbert-side arithmetic restriction:
Solovay's boundary prevents combining that proof efficiency, the paper's
translated subtraction/division theory, and total successor. Total addition is
available only after changing to a cut-free proof predicate and TangRoot.

Thus, a future implementation must choose one row rather than merely label its
checker "sequent":

- cut-free first-order sequent calculus with the total-addition,
  no-total-multiplication profile; or
- cut-permissive Gentzen calculus with the weaker no-total-addition
  Hilbert-style profile.

Both results are conditional on one exact sequent calculus, its logical
axioms, structural rules, eigenvariable conditions, proof coding, and the
corresponding arithmetic profile.

### 2. Natural Skolemizing schemes: conditionally possible, not yet named

The 2011 footnote 35 is broader than resolution: it says the `xi_R` row applies
essentially to any deduction scheme that relies on Skolemization instead of
tableau existential elimination.

That statement is a design criterion, not a completed proof for every
Skolemizing prover. Additional rules can alter proof lengths, stability, and
arithmetization. Equality superposition is the clearest example: adding
paramodulation, rewriting, ordering restrictions, or demodulation produces a
different proof system and needs its own proof.

### 3. Propositional calculus and SAT procedures: components, not first-order `D`

The 2011 preprint's definition of a Herbrand proof explicitly uses a
propositional-calculus proof to show that a finite conjunction of instances of
the Skolemization schema has no satisfying truth assignment. This is a
component of the specified Herbrand/Skolemizing first-order route, not a
separately proposed arithmetic proof apparatus. Likewise, the 1998
"SAT-Resolution subroutine" is a decision algorithm used under a `P=NP`
hypothesis, not the proof relation named by Group-3.

A standalone propositional calculus cannot prove first-order arithmetic
theorems without a specified quantifier-elimination, instantiation, or
Skolemization layer. Once such a layer is supplied, the composite is a new
exact `D` whose proof objects and stability properties require analysis.

### 4. Kreisel-Takeuti CFA: self-consistency formalism, not a drop-in `D`

CFA is repeatedly discussed:

- 1993 describes a second-order generalization of Gentzen's cut-free sequent
  calculus that supports a translated, cut-free form of self-verification.
- The 2002 Tableaux paper says its consistency is localized and "not exactly
  relevant" to Willard's self-justifying systems.
- The 2009 paper explains that CFA uses a second-order internally constructed
  natural-number domain, treats the consistency statement as a derived theorem
  rather than a fixed-point axiom, lacks usable cut/modus ponens, and asserts a
  localized rather than global consistency property.
- 2013, 2014, and 2016 explicitly classify it as a separate approach.

CFA matters to the history and to comparisons, but adopting it would change
the logic, consistency target, and fixed-point architecture. It is not
`IS_D(beta)` with a new first-order `D`.

### 5. Definable cuts and interpretational frameworks: not `D`

A definable cut restricts the proof codes over which consistency is asserted.
Willard explicitly warns that this "cut" is unrelated to Gentzen's deductive
cut rule. The corresponding formula states that no contradiction proof code
inside a selected initial segment exists. It is therefore a localization of
the consistency claim, not an inference mechanism.

The 2009 comparison also says no natural hybrid with Willard's global
fixed-point axiom is apparent. Treating a localized theorem as if it were a
global Group-3 axiom changes the result.

### 6. Passive induction: not `D`

The 2007 paper defines passive induction as an axiom schema whose formula has
no active free variables. Under cut-free tableau, Herbrand, or cut-free
predicate sequent deduction, these apparently redundant axioms can shorten
proofs exponentially and nearly simulate a cut.

Passive induction therefore changes the proper-axiom basis and proof-length
behavior. It is an intermediate metaproof technique and axiom device, not a
proof calculus.

### 7. Gentzen transfinite induction and Goedel Dialectica: not `D`

The 2014/2016 discussions mention these as historical methods for proving or
interpreting consistency from a stronger metatheory. They do not define the
arithmetized proof relation referenced by a Willard Group-3 fixed-point axiom.

### 8. Artemov Infinite-Range/SBSIS: not `D`

The 2020 paper and the 2025-12-24 nachlass draft describe a sequence of finite
subsystems and an infinite sequence of PA theorems, each affirming consistency
for one selected finite subsystem. Willard calls this an Infinite-Ranged or
Step-by-Step Infinite-Schema approach.

It replaces one unified self-referential consistency sentence with an infinite
schema. The change is in the target and architecture, not the proof calculus.

### 9. ZCF cut-free consistency: not an SJAS construction

A March 2008 nachlass draft defines `ConsCF(alpha)` as the absence of a
cut-free Gentzen sequent proof of the empty sequent and uses cut elimination to
relate it to Hilbert inconsistency. The argument concerns stronger set theories
proving cut-free consistency of selected weaker finite fragments. It is
ordinary relative/reflection reasoning, not a self-referential
`IS_D(beta)` transformation.

### 10. Unnatural proof predicates: mathematically possible, inadmissible here

The 1993 introduction acknowledges numerically correct but intensionally
incorrect deduction methods capable of strong self-verification. It explicitly
excludes them in favor of natural methods such as tableau and resolution.

This is an important guard against a vacuous implementation: a predicate
engineered to reject contradiction certificates can satisfy a surface
SelfCons sentence while failing to represent the claimed literature proof
procedure.

### 11. Tab-k, restricted Hilbert, and LEM enrichment: boundary comparators

These are tableau/Hilbert hybrids and were outside the requested candidate
class, but they delimit it:

- `Tab-1` permits only Rank-1*/`Pi*_1`/`Sigma*_1` intermediate lemmas and is
  consistency-preserving.
- `Tab-2` is already too strong in the relevant Type-A setting.
- The 2004 `TabList`/`Tab-Q*_k-List` vocabulary is a tableau proof-list
  formalism. Its earlier `R(1,1) Hierarchy Deduction` name was renamed
  `Tab1 List`; Willard also compares it with, but distinguishes it from,
  Hajek-Paris-Pudlak-Wilkie `R`-proofs and `Q_k` proofs, which restrict
  Hilbert deduction from the opposite direction.
- A Hilbert variant restricted to suitably low-complexity intermediate
  formulae can satisfy the 2005 compactification theorem.
- Unrestricted LEM logical axioms produce `Xtab` or `Xres` and cross the 2020
  boundary.

The lesson is not simply "cut-free good, cut bad." The exact permitted
intermediate formulas, logical axioms, and resulting proof compression matter.

## Negative Vocabulary Search

The accessible corpus and all high-fidelity nachlass OCR were also searched
for the following possible calculus names:

`natural deduction`, `paramodulation`, `superposition`, `SLD-resolution`,
`hyperresolution`, `model elimination`, `connection method`, `connection
calculus`, `hypersequent`, `display calculus`, `nested sequent`, `labelled
sequent`, `calculus of structures`, `matrix method`, `proof net`, `term
rewriting`, `equational calculus`, `deduction modulo`, `epsilon substitution`,
`omega-rule`, `cyclic proof`, `deep inference`, `cutting planes`, `polynomial
calculus`, `extended Frege`, `DPLL`, `CDCL`, `SMT`, `SAT solver`, `Horn
resolution`, `unit resolution`, `linear resolution`, `ordered resolution`,
`semantic resolution`, `set-of-support`, and `input resolution`.

There were no substantive matches. The phrase `SAT-Resolution` in the 1998
chapter is the false positive explained above. "Horn clause" occurs in the
DIMACS, 2002 TABLEAUX, and 2002 JSL texts as a description of object-level
implications or search conditions, not as a Horn-resolution SJAS proposal.
`Unification` occurs in the phrase "introspective unification," not in the
resolution most-general-unifier sense. Modal and fuzzy logic occur only in
bibliography/context, and the real-valued arithmetic papers continue to use
tableau/Tab-1 rather than define an alternate calculus.

The exact phrase `natural deduction` does not occur; the apparent substring in
the 1993 OCR is "unnatural deduction." Occurrences of `Frege` designate
Hilbert-Frege deduction or historical/bibliographic context, not a separate
candidate. "Logic Programming" appears only in a bibliography. The
propositional-calculus occurrence is the Herbrand component classified in
Section 3 above.

Absence from this corpus is not a mathematical impossibility result. It means
Willard supplies no citation-level warrant for claiming those calculi already
satisfy an SJAS theorem.

## Why Resolution Can Work

The positive result is coherent for three independent reasons:

1. A conventional first-order resolution refutation is sound and
   refutation-complete after a specified clausification/Skolemization
   procedure.
2. A finite derivation can be naturally coded as a list or DAG of clauses with
   parent indexes, substitutions, and rule annotations. Checking each step is
   primitive recursive and can be represented by the bounded proof predicate
   required in Willard's weak arithmetic setting.
3. Willard states the non-generic part: the required stability or
   consistency-preservation generalization for the selected weak arithmetic
   configurations, and identifies the exact LEM enrichment that destroys the
   2020 construction. The accessible texts do not spell out the
   resolution-specific derivation in full.

Point 1 and Point 2 alone are insufficient. Many strong proof systems are
sound, complete, and arithmetizable but still incur the Second Incompleteness
Effect. Point 3 is what makes a particular resolution apparatus an SJAS basis.

The 1993 "subcomponent" explanation should also not be implemented naively.
Resolution clauses can contain substitutions and Skolem terms that are not
literal source-AST subformulas. The invariant must be stated over the selected
clausal/Skolem representation and proved for that representation. The 2011
E-stability route is one way Willard makes this dependence explicit.

## Correctness Criteria for a Resolution SJAS

A future implementation cannot be accepted merely because a host resolution
library closes a clause set or because a source-level proof translates to an
existing tableau certificate. It must satisfy all of the following.

### A. Fix the mathematical route

Choose and name one target:

- **2011 route:** `xi_R`, Ax-3, natural Skolemizing resolution, E-stability,
  and `Level(0R)` SelfCons.
- **2020 route:** ordinary `Res` as the direct Level-1 replacement for `Tab`
  in `IS_D(beta)`, with `Xres` as the negative LEM-enriched control.

Do not mix the 2011 Ax-3 axiom-format caveat with the 2020 Level-1 theorem or
silently strengthen either beta.

### B. Specify one exact calculus

The specification must settle at least:

- source formula syntax and classical semantics;
- conversion to negation normal form and clause normal form;
- quantifier treatment and Skolem symbol generation;
- standardization apart;
- clause representation and duplicate-literal policy;
- binary resolution rule;
- selected complementary literals;
- substitution and most-general-unifier representation;
- factoring, if allowed;
- equality treatment;
- initial-clause provenance from proper axioms and the negated goal;
- empty-clause/contradiction terminal condition;
- whether proofs are lists, trees, or DAGs;
- logical axioms `L_D`, especially the absence of arbitrary LEM instances for
  ordinary `Res`.

Paramodulation, superposition, demodulation, subsumption-as-inference, or other
prover features must be excluded or separately formalized and justified.

### C. Define the public proof object and checker

The public certificate must contain the actual finite resolution derivation,
not only a prover event trace or a host assertion that closure occurred. Each
derived clause must cite earlier parent nodes and carry enough substitution
data for deterministic replay.

The public checker must:

- decode the system code, theorem/formula code, and proof code;
- reconstruct the exact initial clause set for that system and target;
- replay every inference in order;
- verify freshness and standardization-apart conditions;
- verify the recorded unifier and resolvent;
- reject unavailable parents, malformed clauses, unproved extra axioms, and a
  missing empty clause;
- be the same relation named by the generated SelfCons sentence.

Search may use indexes, heuristics, or a high-performance host prover to find a
certificate. Acceptance must depend only on replay by the public arithmetized
checker.

### D. Arithmetize that exact checker

Provide a natural Goedel coding and a bounded formula `Prf_Res(t,p)` for the
same proof relation. Prove:

- coding/decoding round trips for every proof constructor;
- public checker and arithmeticized predicate agree on positive and malformed
  certificates;
- the syntactic class required by the chosen route is preserved;
- the proof object has the size/compactification or stability property used in
  the metatheorem.

A tableau `SemPrf` predicate cannot be relabeled as `Prf_Res`. A translation
from resolution to tableau is useful only if the translation itself is
formalized, measured, and shown to preserve the theorem's required bounds.

### E. Generate the system identity and SelfCons from `D_Res`

Changing `D` changes the formal system:

- system identity must include the exact resolution apparatus and logical
  axioms;
- Group-3/SelfCons must quantify over `Prf_Res`, not a compatibility proof or
  generic Proflog trace;
- the formula-code and proof-code relations must use the same symbol table and
  beta encoding as the checker;
- regenerating the apparatus must regenerate the SelfCons formula code.

This is the operational content of the 2002 warning that each method requires
its own `V_d`.

### F. Establish the metatheoretic invariant

For the 2011 route, prove the stated E-stability/`Level(0R)` hypotheses for the
implemented resolution calculus and Ax-3 presentation.

For the 2020 route, prove the direct consistency-preservation analog of
Theorem 4.4 for ordinary `Res`. The proof must apply to the implemented coding
and beta, not to an unnamed textbook resolution system.

In both cases, retain the known negative arithmetic boundary: the
method-specific Goedel effect applies once the arithmetic/proof apparatus
reaches the corresponding strong threshold.

### G. Execute boundary controls

At minimum, tests and durable evidence must show:

1. valid ordinary resolution certificates replay through `Prf_Res`;
2. malformed substitutions, parents, clauses, and terminal nodes are rejected;
3. the generated ordinary-`Res` SelfCons sentence names precisely `Prf_Res`;
4. the positive weak-arithmetic SJAS satisfies the selected consistency theorem;
5. adding all LEM instances changes the identity to `Xres`;
6. the `Xres` construction reaches the negative 2020 boundary under the stated
   beta hypotheses;
7. any total-functional-multiplication experiment uses the method-specific
   resolution proof predicate and does not import a tableau witness.

Constructed certificates are evidence only after the arithmeticized public
predicate accepts them. Conversely, an unbounded search failure is not a
mathematical non-existence proof.

## Correctness Criteria for a Sequent SJAS

A sequent implementation has the same identity burden as resolution and an
additional cut-sensitive arithmetic burden.

1. **Select the theorem route.** For the cut-free route, use the
   total-addition/no-total-multiplication profile and discharge the 2005
   compactification hypothesis for the exact calculus. For the cut-permissive
   route, use the 2001 no-total-addition TangPred/Hilbert-style profile. A proof
   with cut checked against the former profile, or a total-addition beta placed
   under the latter profile, is not one of Willard's positive results.
2. **Specify the exact calculus.** Fix one-sided versus two-sided sequents,
   single- versus multiple-succedent form, initial sequents, logical and
   structural rules, equality rules, quantifier rules, eigenvariable/freshness
   conditions, exchange/contraction/weakening policy, and proof-tree or proof-DAG
   representation. For the cut-free profile the checker must reject every cut
   node; for the cut-permissive profile the cut formula and both parent
   derivations must be explicit.
3. **Make the real sequent object public.** The Condition-B tuple must carry a
   proof code whose decoded nodes are those sequents and rules. A host proof, a
   tableau certificate, or an unverified cut-elimination claim is not a sequent
   certificate.
4. **Arithmetize the same checker.** Define a bounded
   `Prf_Seq(system, formula, proof)` relation and demonstrate agreement between
   decoding/replay and its arithmeticized formula on valid and malformed
   derivations, including eigenvariable and cut-policy failures.
5. **Regenerate system identity and SelfCons.** The calculus, logical axioms,
   cut policy, beta, formula coding, and proof coding must all contribute to the
   system code. Group-3 must quantify over `Prf_Seq`; changing from cut-free to
   cut-permissive sequents must change both system identity and generated
   SelfCons.
6. **Prove the route-specific invariant.** For the cut-free route, establish
   the exact compactification/stability property Willard invokes rather than
   citing cut elimination alone. For the cut-permissive route, establish the
   2001 TangPred construction without smuggling in total successor/addition
   axioms excluded by its boundary.
7. **Run positive and negative controls.** Accept valid sequent certificates,
   reject malformed rules and freshness violations, show the selected positive
   SJAS is consistency-preserving, then show that enabling unrestricted cut or
   strengthening the arithmetic changes the profile and reaches the applicable
   Hilbert/Goedel boundary. Proof-theoretic equivalence of theorem sets is not
   evidence that the SelfCons predicates are interchangeable.

## Consequences for Proflog

The current Proflog `D_SJAS` line is based on measured tableau proof objects.
That work cannot become a resolution SJAS by changing a tag or by attaching a
Proflog inference trace to the same tableau tree.

A resolution implementation should introduce a distinct `D_Res` profile with:

- a clause-derivation certificate;
- a measured public resolution object;
- a resolution-specific arithmeticized proof predicate;
- regenerated system/formula/proof codes;
- a resolution-specific Group-3 sentence;
- an ordinary `Res` positive control and an `Xres` negative control.

The existing tableau implementation remains useful as a semantic comparator.
It is not the source of truth for a resolution SelfCons claim.

A sequent implementation would likewise require a distinct `D_SeqCF` or
`D_SeqCut` profile. Translating a sequent derivation into the existing tableau
checker may establish theoremhood, but it does not make the tableau proof code
the sequent proof predicate named by Group-3. `D_SeqCF` and `D_SeqCut` must
also have different system identities because cut policy is part of the
deductive apparatus and selects a different positive arithmetic profile.

## Source Anchors

- Dan E. Willard,
  [*Self-Verifying Axiom Systems*](https://doi.org/10.1007/BFb0022580),
  especially printed pp. 325, 327, and 332.
- Dan E. Willard,
  [*Self-Reflection Principles and NP-Hardness*](https://dblp.org/rec/conf/dimacs/Willard96),
  for the distinct SAT-decision use of "SAT-Resolution."
- Dan E. Willard,
  [*The Semantic Tableaux Version of the Second Incompleteness Theorem Extends Almost to Robinson's Arithmetic Q*](https://doi.org/10.1007/10722086_32),
  especially printed p. 429.
- Dan E. Willard,
  [*How to Extend the Semantic Tableaux and Cut-Free Versions of the Second Incompleteness Theorem Almost to Robinson's Arithmetic Q*](https://doi.org/10.2178/jsl/1190150055),
  especially journal pp. 469 and 487.
- Dan E. Willard,
  [*An Exploration of the Partial Respects in Which an Axiom System Recognizing Solely Addition as a Total Function Can Verify Its Own Consistency*](https://doi.org/10.2178/jsl/1129642122),
  especially Remark 1 of Section 5.
- Dan E. Willard,
  [*A Detailed Examination of Methods for Unifying, Simplifying and Extending Several Results About Self-Justifying Logics*](https://arxiv.org/abs/1108.6330),
  especially Appendix D, physical pp. 49-50.
- Dan E. Willard,
  [*How the Law of Excluded Middle Pertains to the Second Incompleteness Theorem and its Boundary-Case Exceptions*](https://arxiv.org/abs/2006.01057),
  especially Definitions 3.2 and 4.3, Theorems 4.4-4.5, and Section 8.
- Dan E. Willard,
  [*On the Significance of Self-Justifying Axiom Systems from the Perspective of Analytic Tableaux*](https://arxiv.org/abs/1307.0150),
  for the separation from CFA/interpretational frameworks.
- Dan E. Willard,
  [*On How Introducing a New Theta Function Symbol ...*](https://arxiv.org/abs/1612.08071),
  for the classification of other consistency approaches as separate.
- Dan E. Willard,
  [2018 bibliography/control paper](https://arxiv.org/abs/1807.04717).
- [2001 JSL record and abstract](https://doi.org/10.2307/2695030).
- [DBLP author bibliography](https://dblp.org/pid/w/DanEWillard).
- [`jpt4/sjas` nachlass and paper-witness repository](https://github.com/jpt4/sjas/tree/18e24bc3883d2640695f968e9b48730a6c2bcce2/nachlass).
- [Willard's repository CV](https://github.com/jpt4/sjas/blob/18e24bc3883d2640695f968e9b48730a6c2bcce2/lit/dewresume.pdf),
  including the TABLEAUX 2003 position-paper record.

## Bottom Line

The literature does not leave resolution as an unmentioned speculation. It
states an affirmative Skolemizing `xi_R` route in 2011 and a direct `ISRes`
Level-1 route in 2020, while omitting their full resolution-specific
derivations. It also supplies a sharp warning: ordinary `Res` and LEM-enriched
`Xres` prove the same classical consequences but do not have the same
self-justification behavior.

First-order sequent calculus is also not merely a possibility: Willard gives
an affirmative cut-free route compatible with total addition and a separate
cut-permissive, Hilbert-like route for a weaker no-total-addition profile. The
cut policy and arithmetic profile cannot be mixed.

Therefore the correct next step for either candidate is not to reuse the
tableau proof predicate. Resolution requires one exact natural `D_Res`; sequent
calculus requires one exact `D_SeqCF` or `D_SeqCut`. In each case Proflog must
arithmetize and measure the real certificates, generate SelfCons from that
predicate, prove the selected Willard consistency-preservation invariant, and
execute the route-specific negative boundary control.
