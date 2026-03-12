---
name: ui-craft
description: >-
  Design and build UI with intentional craft, not defaults. Covers intent-first
  process, domain exploration, aesthetic direction, token architecture, and
  craft quality checks. Use when creating pages, components, dashboards, admin
  panels, data interfaces, landing pages, forms, or any visual interface.
  Trigger on: "UI", "frontend", "page", "component", "dashboard", "layout",
  "CSS", "design", "admin panel", "landing page", "form", "data table",
  "介面", "畫面", "前端", "頁面", "元件", "樣式", "設計", "表單".
  Also use when the user says "build me a page" or describes any visual
  interface requirement.
metadata:
  author: samzhu
  version: 1.1.0
---

## Scope

**Use for:** Dashboards, admin panels, SaaS apps, tools, settings pages, data interfaces, landing pages, marketing components.

---

## Intent First

Before touching code, answer these concretely — not in your head.

**Who is this human?** Not "users." The actual person. Where are they when they open this? What's on their mind?

**What must they accomplish?** The verb. Grade submissions. Find the broken deployment. Approve the payment.

**What should this feel like?** "Clean and modern" means nothing. Warm like a notebook? Cold like a terminal? Dense like a trading floor?

If you cannot answer with specifics, stop. Ask the user. Do not default.

### Every Choice Must Be A Choice

For every decision, explain WHY — layout, color temperature, typeface, spacing, hierarchy. If your answer is "it's common" or "it works," you've defaulted.

**The test:** Swap your choices for common alternatives. If the design doesn't feel meaningfully different, you never made real choices.

### Intent Must Be Systemic

Intent shapes every decision. If the intent is warm: surfaces, text, borders, accents, typography — all warm. Check your output against your stated intent. Does every token reinforce it?

---

## Product Domain Exploration

Do not propose any direction until you produce all four:

1. **Domain:** Concepts, metaphors, vocabulary from this product's world. Minimum 5.
2. **Color world:** What colors exist naturally in this domain? If this product were a physical space, what would you see? List 5+.
3. **Signature:** One element that could only exist for THIS product.
4. **Defaults:** 3 obvious choices for this interface type. You can't avoid patterns you haven't named.

Your direction must reference domain concepts, colors from exploration, your signature, and what replaces each default.

---

## Aesthetic Direction

### Typography

Choose fonts that are distinctive and characterful. Pair a display font with a refined body font. Avoid generic fonts like Arial, Inter, Roboto. Never converge on common choices across generations.

### Color & Theme

Commit to a cohesive aesthetic. Use CSS variables for consistency. Dominant colors with sharp accents outperform timid, evenly-distributed palettes. Your palette should feel like it came FROM the product's world, not applied TO it.

### Motion

Prioritize CSS-only solutions for HTML, Motion library for React. Focus on high-impact moments: one well-orchestrated page load with staggered reveals creates more delight than scattered micro-interactions. Use scroll-triggering and hover states that surprise.

### Spatial Composition

Unexpected layouts. Asymmetry. Overlap. Diagonal flow. Grid-breaking elements. Generous negative space OR controlled density.

### NEVER

- Generic AI aesthetics (Inter/Roboto, purple gradients on white)
- Predictable layouts and component patterns
- Cookie-cutter design lacking context-specific character
- Same output twice — vary themes, fonts, aesthetics across generations

---

## Where Defaults Hide

Defaults disguise themselves as infrastructure. Everything is design.

**Typography feels like a container** — but it IS your design. A bakery tool and a trading terminal both need "readable type" — the type that's warm is not the type that's precise.

**Navigation feels like scaffolding** — but it IS your product. A page floating in space is a component demo, not software.

**Data feels like presentation** — but a number on screen is not design. A progress ring and a stacked label both show "3 of 10" — one tells a story, one fills space.

**Token names feel like implementation** — but `--ink` and `--parchment` evoke a world. `--gray-700` and `--surface-2` evoke a template.

---

## The Mandate

Before showing the user, run these checks:

1. **Swap test:** Swap the typeface/layout for your usual ones. Would anyone notice? Where they wouldn't — you defaulted.
2. **Squint test:** Blur your eyes. Can you perceive hierarchy? Does anything jump out harshly?
3. **Signature test:** Point to five specific elements where your signature appears. Not "the overall feel" — actual components.
4. **Token test:** Read your CSS variables aloud. Do they sound like this product's world?

If any check fails, iterate before showing.

---

## Before Writing Each Component

**Every time** you write UI code, state:

```
Intent: [who, what they do, how it should feel]
Palette: [colors from exploration — WHY they fit]
Depth: [borders / shadows / layered — WHY]
Surfaces: [elevation scale — WHY this color temperature]
Typography: [typeface — WHY it fits the intent]
Spacing: [base unit]
```

If you can't explain WHY for each, you're defaulting. Stop and think.

---

## Craft Foundations

### Subtle Layering

Surfaces stack with whisper-quiet shifts. Higher elevation = slightly lighter (dark mode) or uses shadow (light mode). Sidebars: same background as canvas, not different. Inputs: slightly darker (inset). Borders: low-opacity rgba, not solid hex.

### Token Architecture

Every color traces to primitives: foreground (4-level text hierarchy), background (surface elevation), border (separation hierarchy), brand, semantic. No random hex values.

### Depth

Choose ONE approach: borders-only, subtle shadows, layered shadows, or surface color shifts. Don't mix.

### States

Every interactive element: default, hover, active, focus, disabled. Data: loading, empty, error. Missing states feel broken.

For detailed guidance, see `references/principles.md`, `references/example.md`.

---

## Avoid

- Harsh borders, dramatic surface jumps, inconsistent spacing
- Mixed depth strategies, missing interaction states
- Dramatic drop shadows, pure white cards on colored backgrounds
- Gradients/color for decoration, multiple accent colors
- Different hues for different surfaces — same hue, shift lightness only

---

## Workflow

### Communication

Be invisible. Don't announce modes. Jump into work.

### Suggest + Ask

```
"Domain: [5+ concepts]
Color world: [5+ colors from domain]
Signature: [unique element]
Rejecting: [default 1] -> [alt], [default 2] -> [alt], [default 3] -> [alt]

Direction: [approach connecting to the above]"

[Ask: "Does that direction feel right?"]
```

### If Project Has system.md

Read `.interface-design/system.md` and apply. Decisions are made.

### If No system.md

1. Explore domain — produce all four required outputs
2. Propose — direction must reference all four
3. Confirm — get user buy-in
4. Build — apply principles
5. **Evaluate** — run mandate checks before showing
6. Offer to save

---

## After Completing a Task

Always offer: "Want me to save these patterns for future sessions?"

If yes, write to `.interface-design/system.md`: direction, depth strategy, spacing base, key component patterns.

---

## Deep Dives

- `references/principles.md` — Token architecture, surface levels, code examples
- `references/critique.md` — Post-build craft critique protocol
- `references/example.md` — Subtle layering examples
- `references/validation.md` — system.md management

## Commands

- `/ui-craft:status` — Current system state
- `/ui-craft:audit` — Check code against system
- `/ui-craft:extract` — Extract patterns from code
- `/ui-craft:critique` — Critique your build for craft, then rebuild what defaulted
