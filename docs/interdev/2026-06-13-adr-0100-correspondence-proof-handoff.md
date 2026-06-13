# Inter-Developer Note: ADR-0100 Track 2b Correspondence Proof (First Fragment)

Date: 2026-06-13
From: Track 2a agent (worktrees)
To: Codex / Track 2b owner (main worktree; ADR-0096/0097)
Subject: A bounded correspondence proof landed on Track 2b — coordination

## What happened

At the user's direction I completed the **Track 2b correspondence proof over
the first fragment** as [ADR-0100](../adr/ADR-0100-sjas-correspondence-proof.md).
Track 2b is your track (ADR-0096 fragment boundary, ADR-0097 structural
proof-tree audit; the `adr-0073-track2b-formal-correspondence` worktree). This
note is so you can build on it rather than duplicate it, or contest it.

## What it claims (and does not)

- **Claims:** `ProflogAccepts(P,S,F) ⟺ SemPrf_D(decode(P),S,F)` with the ≥5J-bit
  encoding lower bound, **over the first fragment only** (formula-bearing
  structural certificates + bare `sjas-axiom`), via a per-rule match of every
  `sjas-structural-proof-check-state-decodedo` clause to a Willard `D` rule
  (willard2001, 8 rules + closure + prenex* root). Proof by *direct
  examination*, leaning on your ADR-0096 fragment boundary, ADR-0097 size audit,
  and the ADR-0098/0099 unreachability results that exclude every other
  constructor.
- **Does not claim:** the unbounded-domain theorem (constructors admitted as
  primitives/macros), the U-Grounding format, a machine-checked mechanization,
  or a discharged beta-validity boundary. These are the named follow-ups.

## How it fits your Track 2b

- The proof document `docs/log/2026-06-13-sjas-tableau-correspondence-proof.md`
  §3 is a clause→rule table keyed to source line numbers; it is the structured
  skeleton for either a mechanization or the unbounded-domain extension.
- It does not modify the kernel/checker/encoder — audit + tests + proof prose
  only, like ADR-0096/0097.
- If you disagree with the bounded scope, the medium (direct examination), or
  any per-rule match, this is the place to push back; I have not touched your
  ADR-0096/0097 artifacts.

## Suggested division from here

- Track 2b owner: unbounded-domain extension and/or mechanization (the harder,
  open part), building on §3.
- Either: U-Grounding `decode`/size extension; beta-validity discharge.
