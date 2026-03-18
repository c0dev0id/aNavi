# UI_LIBRARY.md

Custom UI component library for aNavi. Defines behavior independently from
placement — change a component once, every instance updates.

All components are Canvas-drawn Android Views. No XML layouts, no Material,
no AppCompat. Consistent rendering across OEMs.

## Design Constraints

- Glove-friendly: large touch targets (min 56dp)
- Glanceable: high contrast, monospace, readable at speed
- Canvas-drawn: no inflation, no theme interference
- Composable: components combine — `IconButton + Menu`, `Ring + Menu`, etc.

---

## Components

### IconButton

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
- Menu trigger (hamburger, A icon) — `IconButton + Menu`
- Toggle (camera lock) — `IconButton` alone with on/off state
- Spoke item — `IconButton` inside a `Spoke`

All IconButtons share identical rendering, animation, and sizing. Only icon
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
- `title` — header title text
- `items` — list of menu entries:
  - `label`, `icon` (optional), `enabled`, `action`
  - Toggle variant: `checked` state (true/false) shown as checkmark
  - `submenu` — nested item list (opens a child Menu)

**Header:**
Split layout — title on one side, optional action icons on the other.
The context menu uses action icons (copy coordinate, quick search).
All other menus use a single-split header (title only).

**Scroll:** If items exceed visible area, vertical scroll within the Menu
frame. No paging.

**Submenu behavior:** TBD — needs first ride to decide between push,
replace-in-place, or inline expand. Spec will be updated after real-world
testing with gloves.

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

Not reused with IconButton (different trigger model — hold vs tap).

---

### Spoke

A row of IconButtons that fly out from a center point in one direction.

**Behavior:**
- Triggered by parent (e.g. SearchRing tap)
- Buttons animate outward sequentially (staggered) from center
- Retract: reverse animation on dismiss
- Direction is configurable — a left Spoke and right Spoke are the
  same component, mirrored

**Properties:**
- `direction` — angle or cardinal direction to expand toward
- `items` — ordered list of IconButtons to fly out
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
- Visual: circular icon button (distinct from IconButton in shape — round
  vs square), but shares sizing and press feedback

**Properties:**
- `icon` — search icon or other indicator
- `position` — typically bottom-center

---

### Crosshair

Screen-center marker for precision targeting when panning the map.

**Behavior:**
- Always at screen center
- Alpha-fades to invisible when close to the GPS location puck
  (effectively invisible in camera-lock mode, visible when panning)
- Fade threshold: distance in screen pixels between center and puck

**Visual:**
- Four short lines radiating from center with a gap (cross pattern)
- Thin stroke, high contrast against map

**Properties:**
- `fadeDistance` — screen-pixel distance at which alpha reaches 1.0
- `strokeWidth` — line thickness
- `size` — overall cross extent

---

### UpdaterCard

Small persistent card showing version and build info.

**Visual:**
- Compact rounded rect with semi-transparent dark background
- Small monospace text showing version string
- Positioned bottom-right, above the app launcher button

**Properties:**
- `text` — version/build string to display

---

## Compositions

Components combine to define complete UI elements. Behavior comes from
the component, placement is separate.

```
IconButton(icon: ≡)     + Menu(right, down)           → navigation menu
IconButton(icon: A)     + Menu(left, up)               → app menu
IconButton(icon: loc)                                  → camera toggle
Ring                    + Menu(right, down)             → map context menu
                          header: "Navigate" + [📋, 🔍]
                          items: Navigate, Place drag line
SearchRing              + Spoke(left) + Spoke(right)   → search bar
```

### Menu Content

**Navigation menu** (hamburger, top-left):
- Open GPX, Export GPX, Save Location, Favorites, Import POIs, Clear Track
- POI search items enabled only when track is loaded

**Context menu** (long-press on map):
- Header: "Navigate" + action icons (copy coordinates, quick search)
- Items: Navigate, Place drag line

**App launcher menu** (A icon, bottom-right):
- Items: list of launchable apps
- "Add app" item → submenu of available apps

## Placement

Where compositions are placed on screen. Changing placement does not
change behavior — components bring their full behavior set.

```
┌──────────────────────────────────────┐
│ [≡]                        [loc]     │
│  └─ Menu(→↓)                         │
│                                      │
│                                      │
│               ╋                      │  ╋ = Crosshair (fades near puck)
│            (long press)              │
│             ◎ → Menu(→↓)             │
│                                      │
│      ←Spoke─[🔍]─Spoke→             │
│                         [build]      │  build = UpdaterCard
│                              [A]     │
│                         Menu(←↑) ─┘  │
└──────────────────────────────────────┘

≡    = IconButton (hamburger)     top-left
loc  = IconButton (camera lock)   top-right
╋    = Crosshair                  screen center
◎    = Ring                       at touch point
🔍   = SearchRing                 bottom-center
build= UpdaterCard                bottom-right
A    = IconButton (app menu)      bottom-right
```
