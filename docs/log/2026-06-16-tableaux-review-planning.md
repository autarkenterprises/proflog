# 2026-06-16: Proflog Tableau Improvement Planning

The reviewed external source was `bradleypallen/tableaux` at commit
`fa5a736090465d0ddf35362a6271d4298d668d42`
(https://github.com/bradleypallen/tableaux). Its useful contribution to
Proflog is not the Python tableau engine itself, but the project shape around
tests and diagnostics:

- a literature-oriented tableau test corpus;
- branch and closure diagnostics useful for explaining proof search;
- open-branch model or witness extraction;
- scheduling and branch-growth checks that separate semantic preservation from
  performance claims.

The planning branch `adr-proflog-tableau-improvement-planning` created four
Proflog-level proposed ADRs:

- [ADR-0112](../adr/ADR-0112-proflog-literature-tableau-golden-suite.md)
  requires a golden suite that accounts for every active upstream `tableaux`
  test and independently confirms supported expectations through Proflog.
- [ADR-0113](../adr/ADR-0113-proflog-proof-object-diagnostic-renderer.md)
  defines a read-only proof object diagnostic renderer.
- [ADR-0114](../adr/ADR-0114-proflog-open-branch-witness-extraction.md)
  defines conservative open-branch witness extraction.
- [ADR-0115](../adr/ADR-0115-proflog-proof-preserving-scheduling-benchmarks.md)
  defines semantic-preservation-first scheduling benchmarks.

The host-language engine design from `tableaux` is not adopted. Any future
implementation must remain Proflog-level, profile-transparent, and compatible
with existing proof-search semantics unless a later ADR explicitly changes
them.
