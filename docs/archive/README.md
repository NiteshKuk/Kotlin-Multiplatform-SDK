# Archive notes

The previous **monolithic README** (~1600 lines) was replaced by the slim root README + `docs/*` guides.

## Recover the old full guide (if you need a snippet)

If you still have it in git history (before this docs split commit):

```bash
git log --oneline -- README.md
git show <commit-before-split>:README.md > docs/archive/full-guide.md
```

Or, if you copied it before overwrite:

```bat
copy /Y README.md.bak docs\archive\full-guide.md
```

Prefer the focused files under `docs/` for day-to-day work.
