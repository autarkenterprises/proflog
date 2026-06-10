# AAR-0089: SJAS Group-3 Presented Code Representation

- Date: 2026-06-10
- ADR: [ADR-0089](../adr/ADR-0089-sjas-group3-presented-code-representation.md)
- Branch: `adr-0087-sjas-selfcons-fixedpoint-basis`, merged into
  `adr-0073-sjas-correspondence-program`

## Outcome

Group-3 reconstruction now preserves the public code representation selected
by the presented `system-code` term, in the Tableau-0 and Level-1
axiom-member paths, the proof-free core paths, the Level-1 `AxiomConj(s)`
fixed-point skeleton check, and proof-free system-code validation. The
representation kind is inferred and propagated relationally; no host
shortcut was introduced.

The slice was executed by a parallel agent (commit `0bc5b79` on
`adr-0087-sjas-selfcons-fixedpoint-basis`, built on `e18f7b7`) and was
reviewed and adopted by merge during the 2026-06-09/10 audit. Because the
only commit between the slice's base and the merge target (`5a7a73d`) was
documentation-only, the merged tree's code is identical to the tree on
which the evidence below was produced. The slice is numbered ADR-0089
because ADR-0087 and ADR-0088 were already assigned on the audit line; the
source branch name predates that assignment.

## Evidence

As recorded in
[the slice note](../log/2026-06-09-sjas-group3-presented-code-representation.md):

- Red: `sjas-tableau0-group-three-rejects-wrong-public-code-representation`
  failed before implementation (`1:38.95`). An initial Level-1 `AxiomConj`
  regression was stopped at about nine minutes as too broad for a focused
  red selector and replaced with a direct Group-3 membership regression.
- Green focused selectors (elapsed): Tableau-0 and Level-1
  wrong-representation rejection (`2:50.09`, `3:15.99`), substitution-axiom
  system-code validation (`2:47.38`), walked equality-state reconstruction
  (`3:16.21`), Tableau-0 `AxiomConj` zero-one target (`2:36.26`), Tableau-0
  and Level-1 Group-3 citations (`2:44.07`, `2:58.16`), Level-1 skeleton
  code (`2:15.69`), U-Grounding tableau proof codes (`2:04.21`), U-Grounding
  Level-1 fixed point (`2:00.32`), U-Grounding code format (`1:53.53`).
- Broad gates on the slice tree: `lein test-proflog-fast` `10:12.39` and
  `lein test-proflog-extended` `22:34.93`, both 0 failures (run under
  parallel-audit machine load; compare the audit-line gate baselines of
  `5:51.24`/`14:26.15`).

## Follow-up

- The Level-1 citation and skeleton selectors in this evidence ran on top of
  the ADR-0087 Pi-star-1 pair restriction, independently confirming that
  shape through the presented-representation path.
- The wrong-representation rejection selectors are natural additions to the
  ADR-0088 re-baseline envelope list.
