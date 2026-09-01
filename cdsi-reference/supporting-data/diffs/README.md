# Supporting Data diffs

Empty until Phase 15 (semantic comparison between two Supporting Data releases) is built - see `supporting-data/README.md` and `StepIntoCDSi-Specification-Reference-Module-Plan.md`'s "Phase 15: Compare Supporting Data Releases".

Once implemented, each comparison will write a pair of files here:

```text
<old>-to-<new>.md      # human-readable change report
<old>-to-<new>.json    # machine-readable companion
```

Generated, not hand-maintained - safe to regenerate from the two registered releases at any time.
