# SJAS Structural Code Decoder

ADR-0067 addresses the finite-registry boundary left by ADR-0063 through
ADR-0066. Before this change, `wff/1`, formula-class predicates, `neg-pair/2`,
and identity `subst-code/2` only accepted formula codes that were generated as
part of the finite Group axiom/complement registry for one SJAS system.

The red characterization used the code for `lt(1,2)`, which is a closed
well-formed formula in the active SJAS language but not a generated axiom. The
query failures were substantive:

```text
wff(code(lt(1,2)))                    => no proof
delta-star-0-code(code(lt(1,2)))      => no proof
neg-pair(code(lt(1,2)), code(not lt)) => no proof
subst-code(code(lt(1,2)), same-code)  => no proof
```

After ADR-0067, the Willard profile parses formula-code bytes structurally:

- formula tags are decoded from the compact base-64 byte stream;
- term tags decode variables, parameters, applications, and embedded code terms;
- application symbol indexes are checked against the active SJAS system's
  finite source-time coding context;
- Delta-star-0 recognition is structural over the decoded formula tree;
- complement recognition mirrors the NNF negation rules used by Proflog; and
- identity substitution succeeds for any structurally well-formed formula code.

The finite registry remains in use for generated facts that really are finite
system membership, especially `axiom-member/2`. The important remaining
boundary is theorem proof checking: `tableau-proof/3` still needs a bridge from
theorem code to a kernel AST formula. That bridge should be removed by a later
ADR, either by converting arbitrary decoded theorem codes into kernel formulas
inside the proof profile or by implementing a fully code-level proof checker.
