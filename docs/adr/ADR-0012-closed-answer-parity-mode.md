# ADR-0012: Closed-Answer Parity Mode

- Status: accepted
- Date: 2026-04-23
- Branch: `adr-0012-closed-answer-parity-mode`
- AAR: pending

## Context

ADR-0011 pushed the default greenfield open-answer path substantially closer to
the relational kernel:

- top-level literal queries now enter directly in the kernel,
- defer-vs-descend is a kernel choice rather than an answer-layer rewrite,
- and completion-aware ranking lets later closed append answers displace earlier
  symbolic frontiers.

That improved the append family, but it did not close parity with legacy.

- `append` still needs a larger raw-proof budget before some concrete families
  surface.
- `reverse([a,b], r)` still exports symbolic frontiers such as `r = []` and
  `r = [a]` with residual obligations rather than the closed legacy answer
  `r = [b,a]`.

This leaves two competing goals in tension:

- preserve the more relationally pure generic open-answer API,
- but also provide an extensional parity search path that can keep running until
  only closed answers remain.

The repository should isolate that second goal in its own ADR and branch so the
project can answer a clean architectural question: is a specialty parity mode
actually necessary, or can later relational-performance work subsume it?

## Decision

- Add a dedicated closed-answer parity search mode for program queries.
- The new mode should keep searching past symbolic frontiers until it can export
  answers with no residual procedure-call obligations.
- The first parity target is answers whose residuals are empty or contain only
  admissible disequalities; if legacy comparison shows that even residual `neq`
  witnesses are too permissive, tighten the mode to require fully empty
  residuals.
- Keep this mode separate from the default `query-answers` API rather than
  silently changing the generic symbolic contract.
- Treat this ADR as an architectural probe as well as a feature:
  - if it proves necessary for practical parity, its branch may be pulled into
    later relational-performance work,
  - if later kernel work makes it unnecessary, the branch should document that
    finding explicitly rather than being promoted by inertia.

## Consequences

- The project gains a direct way to ask extensional parity questions without
  overloading the meaning of the generic symbolic answer API.
- The new mode will likely need much larger `fuel`, `call-depth`, and raw-proof
  budgets than the default open-answer path.
- This ADR intentionally accepts longer runtimes. Its purpose is not to keep the
  fast or ordinary extended suites short; its purpose is to provide a principled
  parity track for long-running reverse and append probes.
- The main risk is architectural duplication: if this branch solves parity only
  by adding a separate search mode, the repository may end up carrying both a
  generic symbolic API and a parity-only API indefinitely.
- That duplication is acceptable only if ADR-0013 fails to recover the same
  closure through a more unified relational path.

## Test Obligations

- Add failing parity-mode regressions for:
  - `reverse([a,b], r)` exporting only the closed answer `r = [b,a]`,
  - `append(x, y, [a,b,c])` exporting all four legacy splits,
  - nested suffix and nested inverse append families where current generic
    `query-answers` still remains symbolic.
- Add a regression that confirms the default `query-answers` API remains
  symbolic and unchanged while the parity mode keeps searching for closure.
- Add one long-running selector or alias for parity-mode probes so these tests
  do not silently leak into the ordinary fast path.

## Exit Criteria

- A dedicated closed-answer parity mode exists and is documented.
- Long-running parity regressions for reverse and append are written and
  reproducible.
- The repo documents whether residual `neq` witnesses count as "closed enough"
  for parity purposes, or whether parity requires completely empty residuals.
- The branch concludes explicitly whether this mode is:
  - necessary and worth carrying forward, or
  - only a temporary scaffold to be folded into ADR-0013 work.
