# KmpSDK — Stakeholder presentation (slide outline)

Use this file to build or edit PowerPoint manually, or run:

```powershell
pip install python-pptx
python tools/generate_stakeholder_ppt.py
```

Output: `docs/presentations/KmpSDK-Stakeholder-Overview.pptx`

**Tip:** Insert `assets/kmpsdk-architecture-flow.png` on slide 7 (Architecture).

---

## Slide 1 — Title

**KmpSDK**  
Headless Kotlin Multiplatform SDK for Android & iOS

- Personal open-source project
- Published on Maven Central · v1.0.0
- Presenter: [Your name / role]

*Speaker note: Clarify this is a personal initiative, not a mandated company product unless leadership adopts it.*

---

## Slide 2 — Agenda

1. Mobile delivery challenges  
2. What KmpSDK is (and is not)  
3. Architecture overview  
4. Three integration paths  
5. Business value  
6. Availability & next steps  
7. Q&A  

---

## Slide 3 — The challenge

- Android and iOS teams often **duplicate** networking, auth, caching, offline logic  
- Every feature repeats boilerplate → **slower releases**  
- UI differs by platform; **infrastructure problems are the same**  
- Harder to maintain consistency and onboard new developers  

---

## Slide 4 — What is KmpSDK?

- **Kotlin Multiplatform** shared library (Android + iOS)  
- **Headless** — infrastructure only; **you own all UI**  
- Plug-and-play: init, modules, networking, optional offline sync  
- **Open source** on GitHub  
- **Maven Central** — standard dependency for any KMP app  

---

## Slide 5 — What it is NOT

- Not a finished product or UI kit  
- Not no-code — developers still build screens & business logic  
- Not automatically an enterprise standard (unless your org adopts it)  
- Not Android-only — one shared module serves both platforms  

---

## Slide 6 — Architecture (use diagram)

**Three layers:**

1. **Host app** — Compose / SwiftUI / XML, ViewModels, navigation  
2. **KmpSDK** — Core, Data, Domain, Presentation (no widgets)  
3. **Platform** — Android & iOS native drivers  

**Integration:** Maven → `KmpSdk.init()` → feature modules → your UI  

*[Insert architecture diagram image here]*

---

## Slide 7 — SDK vs app team responsibilities

| KmpSDK | Your team |
|--------|-----------|
| HTTP, auth, logging | Brand, UX, screens |
| Cache, offline queue (optional) | DTOs, use cases |
| MVI contracts | ViewModels wiring UI |
| Sync helpers (Path C) | SQL schema when offline needed |

---

## Slide 8 — Three paths (decision)

**Per feature, choose one:**

| Path | When | App SQL? |
|------|------|----------|
| **A — Online only** | Login, forms, simple APIs | No |
| **B — SDK HTTP cache** | OK to show last API response offline | No |
| **C — Offline-first** | Must work without network (catalog, field) | Yes |

---

## Slide 9 — Path A (simple)

- Call `KmpSdk.networkClient` from use case  
- **Fastest** integration  
- Example: About page, OTP, settings  

---

## Slide 10 — Path C (powerful)

- Your database + sync repository  
- SDK: offline queue, dirty sync, `bindSyncList`  
- Example: Product catalog, orders, inspections  

---

## Slide 11 — Key features

- Auth & token refresh  
- Configurable offline / cache behaviour  
- Multi-environment (dev / staging / prod)  
- Telemetry & remote config hooks  
- Security: cert pinning, redacted logs  
- Tooling: feature generator CLI  

---

## Slide 12 — Business value

- **Faster delivery** — shared mobile infrastructure  
- **Lower duplication** — one KMP investment vs two native stacks  
- **Consistency** — same patterns for errors, auth, sync  
- **Quality** — documented paths, PR-reviewed open source  
- **No vendor lock-in** — Apache 2.0, public repo  

---

## Slide 13 — Who benefits

- Mobile engineers  
- Engineering managers (predictability)  
- QA (consistent offline/error behaviour)  
- Architecture / platform teams  
- Business units waiting on mobile features  

---

## Slide 14 — Availability

- **Maven:** `in.co.niteshkukreja:kmp-sdk:1.0.0`  
- **GitHub:** https://github.com/NiteshKuk/Kotlin-Multiplatform-SDK  
- **License:** Apache 2.0  
- **Docs:** README with Paths A / B / C  

---

## Slide 15 — Governance

- Personal project; **collaborators via PR**  
- **Owner-only** Maven releases  
- Your organization can: review, pilot, fork, or adopt formally  

---

## Slide 16 — Next steps

1. Share README with mobile leads  
2. Pick **one pilot feature** (Path A or C)  
3. Schedule **30-min developer demo**  
4. Collect feedback for roadmap  

**Ask:** Sponsor a pilot squad?  

---

## Slide 17 — Thank you / Q&A

- GitHub link  
- Maven coordinates  
- [Your contact: email / Slack / LinkedIn]  

**Questions?**

---

## Appendix (backup slides)

### Technical — Maven dependency

```kotlin
implementation("in.co.niteshkukreja:kmp-sdk:1.0.0")
```

### Technical — Init (simplified)

```kotlin
KmpSdk.init(context) {
    baseUrl = "https://api.example.com"
    install(UserFeatureModule)
}
```

### FAQ — Is it production-ready?

Published v1.0.0 on Maven Central; adoption should follow your org’s usual security and architecture review.

### FAQ — Do we need SQL for every screen?

**No.** Path A/B avoid app SQL; Path C only where offline data is required.
