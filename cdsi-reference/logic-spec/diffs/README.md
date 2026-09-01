# Logic Specification diffs

Empty until a second Logic Specification version is registered alongside 4.6.

Once `logic-spec compare --from <old> --to <new>` is implemented (Phase 10 - see `cdsi-reference/README.md`'s "Comparing two versions" section and `tools/cdsi_reference_tools/compare_versions.py`), each comparison writes a pair of files here:

```text
<old>-to-<new>.md      # human-readable change report
<old>-to-<new>.json    # machine-readable companion
```

Generated, not hand-maintained - safe to regenerate from the two registered versions at any time.
