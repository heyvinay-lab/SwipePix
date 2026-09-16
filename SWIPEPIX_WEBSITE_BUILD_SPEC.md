# SwipePix — Official Website Architecture & Build Specification

**Domain:** `https://swipepix.heyvinay.in/`  
**Status:** AUTHORITATIVE TECHNICAL BUILD SPECIFICATION  
**Design Paradigm:** NEO-BRUTALIST SAAS + RETRO-FUTURISTIC Y2K UI + HAND-DRAWN TECH ILLUSTRATIONS  
**Target Output:** Production-ready, lightning-fast static/SPA web application with zero external backend dependencies.

---

## 1. Core Architecture & Tech Stack

### Technology Choices
- **Build Tool & Framework:** Vite 6 + React 19 + TypeScript
- **Styling Engine:** Tailwind CSS with custom Neo-Brutalist & Y2K design token extensions + vanilla CSS variables for physical button physics.
- **Routing:** Client-side URL routing supporting clean static sub-paths (`/`, `/updates`, `/donate`, `/feedback`, `/privacy`, `/about`) with automated title and meta management.
- **Data Source of Truth:** `SWIPEPIX_WEBSITE_DATA.json` imported directly as typed schema, coordinated with `src/config/swipepix.ts`.
- **Icons & Graphics:** Bespoke hand-drawn retro-futuristic SVG components (wireframe phones, floppy disks, pixel starbursts, CRT monitors, triage directional arrows, sticker badges) crafted directly in code for 0ms load time and zero external graphic dependencies.
- **Hosting Targets:** Compatible with Cloudflare Pages, GitHub Pages, Vercel, Netlify, or Firebase Hosting.

---

## 2. Directory & Folder Structure

```
SwipePix/
├── .gsd/
│   └── state.md                         # Persistent GSD state machine
├── SWIPEPIX_WEBSITE_DATA.json           # Machine-readable product truth
├── SWIPEPIX_WEBSITE_CONTENT.md          # Copy-ready content blueprint
├── SWIPEPIX_WEBSITE_BUILD_SPEC.md       # This architecture specification
├── public/
│   ├── favicon.svg                      # Neo-brutalist Y2K app icon
│   ├── robots.txt                       # Search crawler directives
│   ├── sitemap.xml                      # Structured search engine index
│   └── og-image.svg                     # High-res social sharing graph image
├── src/
│   ├── config/
│   │   └── swipepix.ts                  # Centralized download, version, & route constants
│   ├── data/
│   │   └── websiteData.ts               # Typed accessor for SWIPEPIX_WEBSITE_DATA.json
│   ├── types/
│   │   └── index.ts                     # Full TypeScript interfaces for product & content
│   ├── components/
│   │   ├── common/
│   │   │   ├── BrutalistCard.tsx        # Card with 3px border, hard shadow, and sticker slot
│   │   │   ├── BrutalistButton.tsx      # Tactile button with physical hover/active springs
│   │   │   ├── NeoBadge.tsx             # Y2K pill / rotated sticker tags
│   │   │   ├── SectionHeader.tsx        # Chunky monospace uppercase headers with accent underlines
│   │   │   └── SeoHead.tsx              # Dynamic document head & JSON-LD injector
│   │   ├── navigation/
│   │   │   ├── Navbar.tsx               # Monospace header with retro badge and mobile drawer
│   │   │   └── Footer.tsx               # High-contrast 4-column footer with status dot
│   │   ├── hero/
│   │   │   └── HeroSection.tsx          # Monumental typography, stickers, phone mockup visual
│   │   ├── demo/
│   │   │   └── SwipeInteractiveDemo.tsx # Interactive drag/touch card deck with KEEP/TRASH badges
│   │   ├── illustrations/
│   │   │   ├── RetroPhoneSvg.tsx        # Stylized Android phone with swipe vectors
│   │   │   ├── StorageMeterSvg.tsx      # Y2K storage gauge before/after declutter
│   │   │   ├── FloppyDiskSvg.tsx        # 100% offline local SQLite metaphor
│   │   │   ├── StarburstSvg.tsx         # Hand-drawn attention starbursts
│   │   │   └── TrashCanSvg.tsx          # 30-day Android platform system trash
│   │   ├── sections/
│   │   │   ├── ProblemSection.tsx       # Visual chaos of cluttered galleries
│   │   │   ├── HowItWorksSection.tsx    # 4-step linear numbered pipeline
│   │   │   ├── FeatureGridSection.tsx   # Asymmetric neo-brutalist feature cards
│   │   │   ├── ScreenshotsSection.tsx   # Live Samsung Galaxy S24 Ultra hardware captures showcase
│   │   │   ├── SafetySection.tsx        # Android system trash 4-layer trust breakdown
│   │   │   ├── PrivacySection.tsx       # Prominent black-slate section: NO INTERNET PERMISSION
│   │   │   ├── PerformanceSection.tsx   # Hardware thumbnails, 2-tier cache, progressive viewer
│   │   │   ├── TechStackSection.tsx     # Kotlin, Compose, Room, Coil 3 badge grid
│   │   │   ├── FaqSection.tsx           # Accordion FAQ with high-contrast active states
│   │   │   └── DownloadCtaSection.tsx   # Final high-energy CTA banner
│   │   └── pages/
│   │       ├── HomePage.tsx             # Complete landing experience
│   │       ├── UpdatesPage.tsx          # Release center, build specs, APK download state
│   │       ├── DonatePage.tsx           # Independent developer support & sponsorship philosophy
│   │       ├── FeedbackPage.tsx         # Validated feedback form with device details
│   │       ├── PrivacyPage.tsx          # Full technical privacy policy & permissions audit
│   │       └── AboutPage.tsx            # Product story and link to heyvinay.in
│   ├── App.tsx                          # Route coordinator & layout wrapper
│   ├── index.css                        # Neo-brutalist tokens, custom utilities, monospace typography
│   └── main.tsx                         # React 19 entry point
├── index.html                           # Root HTML shell with preloaded fonts and meta tags
├── package.json                         # Scripts and dependencies
├── tailwind.config.js                   # Custom Y2K palette, borders, and shadows
├── tsconfig.json                        # Strict TypeScript configuration
└── vite.config.ts                       # Fast ESM bundling configuration
```

---

## 3. Design System Tokens (Neo-Brutalist & Y2K)

### Color Palette
| Token | Hex Value | Semantic Usage |
|---|---|---|
| `--color-bg` | `#F5F2EA` | Warm tactile off-white page background |
| `--color-ink` | `#050505` | Jet black typography, borders, and hard shadows |
| `--color-dark-section` | `#111111` | Midnight background for Privacy and Terminal sections |
| `--color-primary` | `#2F6BFF` | Electric Blue: primary actions, brand anchors, cards |
| `--color-secondary` | `#8B5CF6` | Cyber Purple: tags, highlights, secondary cards |
| `--color-accent` | `#B8FF00` | Acid Lime: badges, stickers, success/keep highlights |
| `--color-warm` | `#FF4FD8` | Y2K Pink: special callouts, urgent indicators, stickers |
| `--color-keep` | `#B8FF00` / `#00D95A` | Swipe right keep state |
| `--color-trash` | `#FF4FD8` / `#FF2A55` | Swipe left stage-for-trash state |

### Outlines, Borders & Shadows
```css
/* Core Neo-Brutalist Border */
--border-brutal: 3px solid #050505;
--border-brutal-sm: 2px solid #050505;
--border-brutal-lg: 4px solid #050505;

/* Hard Offset Drop Shadows */
--shadow-brutal-sm: 3px 3px 0px #050505;
--shadow-brutal: 5px 5px 0px #050505;
--shadow-brutal-lg: 8px 8px 0px #050505;
--shadow-brutal-accent: 5px 5px 0px #B8FF00;
--shadow-brutal-pink: 5px 5px 0px #FF4FD8;

/* Tactile Button Physical Motion */
.btn-brutal {
  border: 3px solid #050505;
  box-shadow: 5px 5px 0px #050505;
  transition: transform 120ms cubic-bezier(0.2, 0, 0, 1), box-shadow 120ms cubic-bezier(0.2, 0, 0, 1);
}
.btn-brutal:hover {
  transform: translate(-2px, -2px);
  box-shadow: 7px 7px 0px #050505;
}
.btn-brutal:active {
  transform: translate(2px, 2px);
  box-shadow: 2px 2px 0px #050505;
}
```

### Typography System
- **Display & Headings:** Bold Geometric Monospace (`Space Mono`, `JetBrains Mono`, `IBM Plex Mono`, fallback `monospace`)
- **Stickers & Badges:** Heavy Monospace Uppercase with subtle 2° to -3° rotation transforms.
- **Body & Long-Form Copy:** High-legibility sans-serif (`Inter`, `Plus Jakarta Sans`, system `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto`) with monospace accents for technical specs, permissions, and code terms.

---

## 4. Routing Architecture & Page Hierarchy

All routes share the unified Neo-Brutalist layout with global sticky navbar and brutalist footer:

1. **`GET /` (Landing Page)**
   - Navigation
   - Hero with large headline and interactive sticker badges
   - Visual Problem Section (storage bloat vs. micro-checkbox frustration)
   - How It Works (4 linear steps)
   - Interactive Swipe Demo (draggable card deck with real-time KEEP/TRASH reactions)
   - Feature Grid (12 neo-brutalist cards highlighting verified capabilities)
   - Safety & Trust Deep Dive (4-layer system trash safeguards)
   - Privacy Fortress (High-contrast dark section: Zero internet permission)
   - Performance Engineering (Hardware thumbnails, dual-tier cache, 0ms viewer)
   - Verified Tech Stack (Kotlin, Compose, Room, Coil, MediaStore)
   - Accordion FAQ
   - Final High-Impact Download Banner
   - Footer

2. **`GET /updates` (Update Center & Direct Releases)**
   - Version 1.0 release details and build metadata (versionCode 1, API 33–35)
   - Official direct APK download trigger with cryptographic checksum display
   - Fallback state for unconfigured builds with direct link to GitHub Releases
   - Detailed changelog breakdown

3. **`GET /donate` (Developer Support)**
   - Independent open software ethos
   - What donations fund (test devices, platform updates, open-source documentation)
   - Transparent, verified channels via developer's official site (`heyvinay.in`)

4. **`GET /feedback` (Structured Feedback & Bug Report)**
   - High-contrast brutalist form with clear validation
   - Fields: Name, Email, Category, Subject, Message, Device Model, Android Version, App Version
   - Honest no-fake-backend handling: validates inputs, constructs formatted payload, provides one-tap email dispatch and clipboard copy with zero false marketing.

5. **`GET /privacy` (Dedicated Privacy Policy)**
   - Full legal & technical explanation of local-first design
   - Explicit Manifest permission table (`READ_MEDIA_IMAGES` vs. absence of `INTERNET`)
   - Explanation of Android MediaStore, sandboxed Room database, and zero analytics commitment.

6. **`GET /about` (Product & Developer Philosophy)**
   - Origin story of SwipePix
   - The anti-bloat engineering manifesto
   - Direct link to developer portfolio (`https://heyvinay.in/`)

---

## 5. Interactive Swipe Demo Specifications

- **Behavior:**
  - Card responds to mouse/touch drag along the X-axis.
  - As card is dragged to the right ($X > 0$), positive rotation (up to 12°) is applied, and the **KEEP** badge illuminates in Acid Lime (`#B8FF00`).
  - As card is dragged to the left ($X < 0$), negative rotation (up to -12°) is applied, and the **TRASH** badge illuminates in Y2K Pink (`#FF4FD8`).
  - Upon release with velocity or threshold exceedance ($|X| > 90\text{px}$), the card animates smoothly off-screen in the chosen direction and loads the next sample photo card.
  - An **Undo** button restores the discarded card with an elastic snap-back.
- **Accessibility:**
  - Manual button controls ("← TRASH", "KEEP →", "UNDO") for keyboard-only and screen reader navigation.
  - Respects `@media (prefers-reduced-motion: reduce)` by disabling drag rotations and using instant state switches.

---

## 6. SEO, Social & Structured Data

- **Primary Document Title:** `SwipePix — Privacy-First Swipe-to-Clean Android Gallery`
- **Primary Description:** `SwipePix is a local-first Android gallery and storage cleaner. Triage photos and videos with intuitive swipe gestures. 100% offline with zero cloud, zero trackers, and native system trash safety.`
- **Canonical URL:** `https://swipepix.heyvinay.in/`
- **OpenGraph & Twitter Card:** `summary_large_image` with 1200x630 branded Neo-Brutalist preview graphic.
- **Search Directives:** Complete `robots.txt` allowing all legitimate crawlers and declaring `sitemap.xml`.
- **JSON-LD Schema (`SoftwareApplication`):**
  ```json
  {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": "SwipePix",
    "operatingSystem": "Android 13, Android 14, Android 15 (API 33-35)",
    "applicationCategory": "PhotographyApplication",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "USD"
    },
    "description": "A local-first Android gallery and storage declutter application that triages photos and videos through swipe gestures — 100% offline with zero trackers and native system trash safety."
  }
  ```

---

## 7. Performance & Accessibility Targets

- **Lighthouse Targets:** 95+ across Performance, Accessibility, Best Practices, and SEO.
- **Bundle Footprint:** Minified JS bundle under 60KB gzip. Zero bloated 3D or heavy physics libraries.
- **Color Contrast:** Every text/background combination strictly satisfies WCAG AAA (black `#050505` on `#F5F2EA` = 17.5:1 ratio; white `#FFFFFF` on `#111111` = 18.2:1 ratio).
- **Keyboard Traversal:** Visible 3px electric blue focus rings (`outline: 3px solid #2F6BFF; outline-offset: 3px;`) on all interactive cards, links, and buttons.
