# Design System — LockSite

Derived from the supplied reference (soft pastel cards on a light grey canvas, no shadows).
Every value below is a token implemented in `ui/theme/`; a raw value outside this file is
a bug.

## 1. Design contract

```
GRID      : single column · side margin 20 · card padding 20 · card gap 16 · section gap 32
SPACING   : 4 8 12 16 24 32 48   (base 4, rhythm 8; inside group 8, between groups 16–24,
                                  between sections 32–48)
TYPE      : 12 13 15 20 28 40 · line-height 1.5 body / 1.4 title / 1.2 display
            one family (system), weights 400 / 500 / 600 only
COLOUR    : canvas #F2F2F2 · surface #FFFFFF · muted #F6F6F7 · outline #E8E8EA
            teal #BFE7E8 (status field) · green #CDE9AB (completed) · yellow #F8EC94 (one primary action)
            ink #17181A · muted ink #5F6066 · faint #8E8F95
RADIUS    : 16 controls · 24 cards · 28 large cards · full pills
ELEVATION : none, everywhere. Grouping comes from colour fields and gaps, never shadows.
MOTION    : 120 / 200 ms, ease-out in, ease-in out. No decorative animation in M1.
```

## 2. Why these numbers

| Decision | Rule applied |
|---|---|
| Gap inside a card 8, between cards 16 | Proximity ratio ≥ 2:1 — grouping must be visible without borders |
| Yellow only for the primary action | Von Restorff: exactly one focal point per screen |
| Three bottom destinations | Hick: fewer choices, faster decisions (≤7 hard limit) |
| 44dp circular targets, 8dp apart | Fitts: minimum target size and separation |
| Status stated in words next to colour | WCAG: never colour alone (PRD NFR-005) |
| No shadows | Reference style + Prägnanz: flat fields read as one shape |

## 3. Colour usage rules

- **Teal** — the protection status field. One per screen, dashboard only.
- **Green** — completed / locked states (badges, success card, "last locked" block).
- **Yellow** — exactly one primary action per screen. Never decorative.
- **Danger** — validation errors and the irreversibility warning. Never used for a button
  that the user *should* press; the confirm button uses ink, not red, so it reads as
  deliberate rather than alarming.
- Text on pastel fields always uses the matching dark ink (teal-ink, green-ink, yellow-ink)
  for contrast above 7:1.

## 4. Components

| Component | File | Notes |
|---|---|---|
| `SoftCard` | `ui/components/Surfaces.kt` | Flat surface, radius 24, padding 20 |
| `PillButton` | `ui/components/Buttons.kt` | Primary action, 52dp, pill, disabled at 45% alpha |
| `GhostButton` | `ui/components/Buttons.kt` | Secondary action, white surface |
| `PillChip` | `ui/components/Buttons.kt` | Status chip: icon + text, never colour alone |
| `IconCircle` | `ui/components/Buttons.kt` | 44dp round icon target |
| `FlatTextField` | `ui/components/FlatTextField.kt` | White surface + hairline, no Material underline |
| `GreetingHeader` | `ui/components/Headers.kt` | Avatar placeholder + two-line greeting + action |
| `DetailHeader` | `ui/components/Headers.kt` | Back + title (+ optional trailing) |
| `LockSiteBottomBar` | `ui/components/BottomBar.kt` | Three destinations, yellow pill for the active one |

## 5. Screen-level hierarchy

Each screen has exactly one primary action:

| Screen | Primary action | Focal element |
|---|---|---|
| Onboarding | "Mulai atur" | Headline |
| Dashboard | "Tambah domain" | Teal status card |
| Add domain | "Kunci domain" | Input field |
| Locked list | "Tambah domain" | The list itself |
| Settings | none | The information cards |

## 6. States implemented

default · pressed (Compose ripple-free clickable) · disabled (45% alpha) · loading
(`saving` flag disables the primary button) · empty (per screen) · error (validation text,
load-error card) · success (green confirmation card). Focus-visible is provided by the
system IME for text fields.

## 7. Known limitations (M1)

- **No dark theme.** The palette is a light pastel system; a dark variant needs its own
  contrast pass and is not invented here.
- No motion beyond platform defaults — deliberate for M1.
- Launcher icon is a placeholder shape, not a finished brand mark.
- Contrast was checked by eye against the tokens above; it has not yet been measured with
  a tool. Tracked for the accessibility pass before beta (M5).
