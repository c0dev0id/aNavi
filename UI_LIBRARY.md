# UI_LIBRARY.md

Custom UI component library for aNavi. Defines behavior independently from
placement — change a component once, every instance updates.

All components are Canvas-drawn Android Views. No XML layouts, no Material,
no AppCompat. Consistent rendering across OEMs.

## Design Constraints

- Glove-friendly: large touch targets (min 56dp)
- Glanceable: high contrast, monospace, readable at speed
- Canvas-drawn: no inflation, no theme interference
- Composable: components combine — `Button + Menu`, `Ring + Menu`, etc.

---

## Components

### Button

A square icon on screen. The atomic control element.

**States:** idle → pressed → active (e.g. menu open, toggle on)

**Properties:**
- `icon` — drawable/vector to render
- `size` — default 56dp
- `position` — screen anchor (e.g. top-left, bottom-right, top-right)

**Behavior:**
- Tap → triggers attached action or composition
- Press feedback (scale or alpha)

**Used as:**
- Menu trigger (hamburger, A icon) — `Button + Menu`
- Toggle (camera lock) — `Button` alone with on/off state
- Spoke item — `Button` inside a `Spoke`

All Buttons share identical rendering, animation, and sizing. Only icon
and position differ.

---

### Menu

A surface of items that folds out from an anchor point. The richest
component — defines all content structure and interaction.

**Invariants (identical across all instances):**
- Fold-out animation (timing, easing, duration)
- Fold-in / dismiss animation
- Inner margins and item spacing
- Title bar position and style
- Item layout (icon + label), hit targets (min 56dp height)
- Enabled / disabled item appearance
- Dismiss triggers: tap outside, back gesture, anchor re-tap

**Configuration:**
- `expandH` — horizontal expansion direction (left / right)
- `expandV` — vertical expansion direction (up / down)
- `anchor` — the View or point it attaches to
- `items` — list of menu entries:
  - `label`, `icon` (optional), `enabled`, `action`
  - `submenu` — nested item list (opens a child Menu)

**Submenu behavior:** TBD — needs first ride to decide between push,
replace-in-place, or inline expand. Spec will be updated after real-world
testing with gloves.

**Scroll:** If items exceed visible area, vertical scroll within the Menu
frame. No paging.

---

### Ring

A radial fill indicator for long-press activation on the map surface.

**Behavior:**
- Long press begins → Ring appears at touch point, fills clockwise
- Fill completes → triggers action (e.g. opens a Menu)
- Lift before complete → Ring cancels and fades out
- Visual: arc/circle stroke that fills over a configurable duration

**Properties:**
- `duration` — fill time (default ~800ms)
- `radius` — ring size

**Used as:**
- `Ring + Menu` — long-press context menu on the map

Not reused with Button (different trigger model — hold vs tap).

---

### Spoke

A row of Buttons that fly out from a center point in one direction.

**Behavior:**
- Triggered by parent (e.g. SearchRing tap)
- Buttons animate outward sequentially (staggered) from center
- Retract: reverse animation on dismiss
- Direction is configurable — a left Spoke and right Spoke are the
  same component, mirrored

**Properties:**
- `direction` — angle or cardinal direction to expand toward
- `items` — ordered list of Buttons to fly out
- `spacing` — gap between items

**Used as:**
- `SearchRing + Spoke(left) + Spoke(right)` — search flyout bar

---

### SearchRing

The center circle that Spokes radiate from. Sits on-screen as a
persistent control.

**Behavior:**
- Tap → Spokes extend outward
- Tap again or tap outside → Spokes retract
- Visual: circular icon button (distinct from Button in shape — round
  vs square), but shares sizing and press feedback

**Properties:**
- `icon` — search icon or other indicator
- `position` — typically bottom-center

---

## Compositions

Components combine to define complete UI elements. Behavior comes from
the component, placement is separate.

```
Button(icon: ≡)         + Menu(right, down)           → navigation menu
Button(icon: A)         + Menu(left, up)               → app menu
Button(icon: location)                                 → camera toggle
Ring                    + Menu(right, down)             → map context menu
SearchRing              + Spoke(left) + Spoke(right)   → search bar
```

## Placement

Where compositions are placed on screen. Changing placement does not
change behavior — components bring their full behavior set.

```
┌──────────────────────────────────────┐
│ [≡]                        [loc]     │
│  └─ Menu(→↓)                         │
│                                      │
│                                      │
│            (long press)              │
│             ◎ → Menu(→↓)             │
│                                      │
│                                      │
│      ←Spoke─[🔍]─Spoke→             │
│                              [A]     │
│                         Menu(←↑) ─┘  │
└──────────────────────────────────────┘

≡    = Button (hamburger)     top-left
loc  = Button (camera lock)   top-right
◎    = Ring                   at touch point
🔍   = SearchRing             bottom-center
A    = Button (app menu)      bottom-right
```
