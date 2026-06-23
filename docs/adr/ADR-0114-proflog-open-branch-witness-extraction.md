# ADR-0114: Proflog Open-Branch Witness Extraction

- Status: accepted
- Date: 2026-06-16
- Branch: `adr-0114-proflog-open-branch-witness-extraction`
- AAR: [AAR-0114](../aar/AAR-0114-proflog-open-branch-witness-extraction.md)

## Context

Open branches are often the most useful explanation of a failed closure:
they describe a consistent branch, a countermodel candidate, or at least the
residual obligations that prevented refutation. The reviewed `tableaux` project
extracts simple models from open branches. Proflog should adopt the diagnostic
idea, not that implementation.

The Proflog version must be conservative. It should produce witnesses only for
fragments where the branch content has a clear interpretation, and it should
report explicit limitations everywhere else.

## Decision

Add diagnostic witness extraction for supported open-branch fragments. The
public surface should be a small diagnostic namespace, for example
`proflog.diagnostics.witness`, whose primary operation returns a structured
status map:

- `:witness` with the extracted assignment or countermodel-style summary;
- `:closed` when no witness exists because the branch is closed;
- `:unsupported` when the branch contains constructs outside the extractor's
  sound fragment;
- `:ambiguous` when additional profile or artifact metadata is required.

The first implementation should target the smallest reliable fragment, such as
ground propositional branches over finite atoms and standard classical
connectives. Later branches may broaden the supported surface, but every new
construct must be accompanied by correctness tests.

## Consequences

- Failed or incomplete proof searches become easier to explain.
- Witness extraction remains diagnostic and does not decide proof acceptance.
- Unsupported constructs become visible as explicit statuses rather than
  misleading partial witnesses.
- The golden suite can use witness extraction to confirm open-branch examples
  once ADR-0114 is implemented.

## Test Obligations

- Red tests must require an open supported branch to produce the expected
  witness assignment.
- Red tests must require a closed branch to return `:closed` and no witness.
- Red tests must require contradictory branch data to be rejected rather than
  converted into a witness.
- Red tests must require unsupported branch constructs to return `:unsupported`
  with a reason.
- Tests must show that witness extraction does not change proof search,
  acceptance, or answer reification.

## Exit Criteria

- A pure diagnostic witness API exists.
- The initial supported fragment is precisely documented.
- Open, closed, contradictory, and unsupported cases are covered by tests.
- Any integration with renderer or golden-suite diagnostics remains read-only.
- AAR-0114 records supported fragments, rejected fragments, and examples.
