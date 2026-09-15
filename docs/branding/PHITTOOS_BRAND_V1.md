# Phittoos Brand Specification v1

This document locks the official brand identity, visual specs, color specifications, and vector geometry for the Phittoos offline peer-to-peer settlement application.

---

## 1. Core Brand Identity

* **App Name**: Phittoos
* **Primary Symbol**: The corrected Indian Rupee symbol (**`₹`**) centered within two directional outer transaction arrows (symmetrical, clockwise circular flows).
* **Official Tagline**: *"Tere mere hisaab… Phittoos!"* (translating to "Your ledger, my ledger... Settled/Phittoos!").

---

## 2. Official Logo Palette

These color codes are locked strictly for **branding, launcher icons, splash screens, and in-app logo representations**. They do **not** override or change the existing high-contrast accessible UI color schemes:

* **Brand Background**: `#0F172A` (Solid Deep Slate)
* **Central Symbol (Rupee)**: `#FFFFFF` (Solid White)
* **Upper Directional Arrow (Lent/Outflow)**: `#10B981` (Emerald Green)
* **Lower Directional Arrow (Borrowed/Inflow)**: `#F97316` (Coral Orange)

---

## 3. Approved Vector Geometry

Retrieved directly from the approved corrected candidate (`logo_preview/logo_comparison_preview.html`):

### Viewport Specifications
* **Width**: `108dp`
* **Height**: `108dp`
* **Viewport Dimensions**: `108 x 108`

### Path Data Definitions
1. **Top Directional Arrow (Arc)**:
   - **Path**: `M37,44 A20,20 0 0,1 71,44`
   - **Stroke Color**: `#10B981`
   - **Stroke Width**: `4dp`
   - **Stroke Cap**: `round`
   - **Fill**: `none` (`#00000000`)
2. **Top Arrow Head**:
   - **Path**: `M70,40 L76,45 L70,50 Z`
   - **Fill Color**: `#10B981`
3. **Bottom Directional Arrow (Arc)**:
   - **Path**: `M71,64 A20,20 0 0,1 37,64`
   - **Stroke Color**: `#F97316`
   - **Stroke Width**: `4dp`
   - **Stroke Cap**: `round`
   - **Fill**: `none` (`#00000000`)
4. **Bottom Arrow Head**:
   - **Path**: `M38,68 L32,63 L38,58 Z`
   - **Fill Color**: `#F97316`
5. **Standardized Rupee Symbol (₹)**:
   - **Unified Vector Path (Apache 2.0 License, Google Material Symbols)**: `M56.156,62.500 L47.750,55.889 L47.750,54.000 L52.125,54.000 Q53.781,54.000 54.985,53.186 T56.438,51.167 L46.500,51.167 L46.500,49.278 L56.062,49.278 Q55.531,48.451 54.484,47.921 T52.125,47.389 L46.500,47.389 L46.500,45.500 L61.500,45.500 L61.500,47.389 L57.438,47.389 Q57.875,47.790 58.219,48.263 T58.750,49.278 L61.500,49.278 L61.500,51.167 L58.969,51.167 Q58.719,53.174 56.781,54.532 T52.125,55.889 L51.219,55.889 L59.625,62.500 Z`
   - **Fill Color**: `#FFFFFF`
   - **Source/License Attribution**: Converted from Google Material Symbols glyph (Apache License 2.0). The incorrect left straight vertical pillar has been entirely eliminated to preserve pure geometric symmetry and traditional ₹ glyph anatomy.

### Design Metric Boundaries
* **Emblem Extents**: Horizontal: $X=32$ to $X=76$ (total width `44dp`).
* **Symmetry**: Perfect horizontal alignment across $Y=54$ (vertical center). Arcs sit precisely `10dp` above and below the center line (at $Y=44$ and $Y=64$).
* **Measured Radius & Clipping Considerations**: Adaptive mask boundary clipping and safe-zone verification is pending visual/physical device checks in Task 17G.

---

## 4. Usage Guidelines

### A. Launcher Icons (`ic_launcher`)
* **Format**: Android Adaptive Icon.
* **Background Layer**: Solid `#0F172A` Slate.
* **Foreground Layer**: Symmetrical Emerald & Coral arrows with the corrected White Rupee emblem. No in-app typography, slogans, background rings, or gradients.

### B. Onboarding Screen Layout
* Display the approved white central symbol on the brand background alongside the app title "Phittoos" and the locked tagline *"Tere mere hisaab… Phittoos!"*.

### C. Splash Screen
* A clean, solid background matching `#0F172A` centering the approved logo symbol. (System window bar and startup transitions are deferred to Task 17F).

### D. Monochrome Silhouette Asset
* **Concept**: A dedicated outline silhouette containing clean negative spaces to keep both arrows, arrowheads, and the Rupee symbol recognizable against native system tint overlays.
* **Constraint**: Do not use an opaque circular badge or solid shape block that masks the internal details under monochrome theme settings.

---

## 5. Architectural Guardrails & Preservations

* **Accessible UI Palette**: Keep the existing accessible user interface theme (emerald active components, dark gray helper texts, white backgrounds, coral notification banners) completely isolated from the brand logo specifications.
* **Financial Meaning**: Emerald Green consistently represents money lent (or outflows), while Coral Orange represents money borrowed (or inflows).
* **Celebration & Business Flow**: The core settlement mechanics, "Phittoos!" animation triggers, preference persistence, and navigation structures must remain untouched.

---

## 6. Implementation Sequence

* **Task 17D**: Launcher assets creation (legacy, round, adaptive, and monochrome configurations).
* **Task 17E**: Home and onboarding branding consistency (integrating the logo, app name, and tagline on both the Home Dashboard and Onboarding/Intro steps).
* **Task 17F**: Splash branding layout and standard platform transition wiring.
* **Task 17G**: Comprehensive multi-device visual inspection, edge-to-edge layout checks, and final Git commit.
