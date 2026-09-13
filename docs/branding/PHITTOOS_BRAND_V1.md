# Phittoos Brand Specification v1

This document locks the official brand identity, visual specs, color specifications, and vector geometry for the Phittoos offline peer-to-peer settlement application.

---

## 1. Core Brand Identity

* **App Name**: Phittoos
* **Primary Symbol**: The corrected Indian Rupee symbol (**`₹`**) centered within two directional outer transaction arrows (symmetrical, counter-clockwise circular flows).
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
5. **Corrected Rupee Symbol (₹)**:
   - **Top Bar**: `M46.5,45.5 L61.5,45.5 L61.5,47.5 L46.5,47.5 Z`
   - **Middle Bar**: `M46.5,49.5 L60.5,49.5 L60.5,51.5 L46.5,51.5 Z`
   - **Corrected Loop & Diagonal Leg**: `M50.5,45.5 L53,45.5 L53,53.5 C55.5,53.5 57.5,52.5 57.5,50 L57.5,49.5 L59.5,49.5 L59.5,50 C59.5,53.8 56.8,55.5 53,55.5 L53,56.5 L58.5,62.5 L55.5,62.5 L48.5,54.5 L48.5,45.5 Z`
   - **Fill Color**: `#FFFFFF`

### Design Metric Boundaries
* **Emblem Extents**: Horizontal: $X=32$ to $X=76$ (total width `44dp`), Vertical: $Y=40$ to $Y=68$ (total height `28dp`).
* **Symmetry**: Perfect horizontal alignment across $Y=54$ (vertical center). Arcs sit precisely `10dp` above and below the center line (at $Y=44$ and $Y=64$).
* **Safe Zone Compliance**: Maximum radial distance of any foreground coordinate from center $(54, 54)$ is exactly `22dp`. This sits safely within the Android **`33dp` Safe Zone Circle**, guaranteeing zero clipping across circular, square, squircle, or custom adaptive launcher shape masks.

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
* **Task 17E**: Integration of the logo and tagline on the Onboarding/Intro step.
* **Task 17F**: Splash branding layout and standard platform transition wiring.
* **Task 17G**: Comprehensive multi-device visual inspection, edge-to-edge layout checks, and final Git commit.
