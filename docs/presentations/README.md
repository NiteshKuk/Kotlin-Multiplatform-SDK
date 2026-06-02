# KmpSDK presentations

## Stakeholder deck (office / cross-department)

### Option 1 — Generate PowerPoint (.pptx)

In **Command Prompt** (not blocked by PowerShell policy):

```bat
cd /d "D:\Android Projects\Personal\Kotlin-Multiplatform-SDK"
pip install python-pptx
python tools\generate_stakeholder_ppt.py
```

Opens: **`KmpSDK-Stakeholder-Overview.pptx`** (17 slides + speaker notes)

### Option 2 — Build manually

Copy slide text from **`KmpSDK-Stakeholder-Deck.md`** into PowerPoint.

### Option 3 — Present in browser

Open **`KmpSDK-Stakeholder-Slides.html`** in Chrome/Edge → F11 fullscreen → arrow keys to advance.

### Add architecture diagram

On the architecture slide, insert:

`assets/kmpsdk-architecture-flow.png`

(from project root)

### Before presenting

- Replace `[Your name / contact]` on title and closing slides  
- Mark clearly: **personal open-source project**  
- Pick 20–25 min; skip appendix unless asked  
