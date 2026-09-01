<!--
Copy this alongside a finding.yaml using the shape shown in the
reference-module plan's Phase 9 (id, status, category, spec_sections,
source_pages, tables, business_rules, code_locations, fits_cases, summary,
evidence, interpretation). This Markdown file is the human-readable
narrative; finding.yaml is the machine-readable record.

category must be one of:
  IMPLEMENTATION_MISMATCH   - StepIntoCDSi appears inconsistent with the spec.
  SPECIFICATION_AMBIGUITY   - the spec doesn't establish a clear result.
  SUPPORTING_DATA_CONFLICT  - Logic Spec and Supporting Data disagree.
  FITS_DIFFERENCE           - FITS expectations disagree with another source.

A finding stays "draft"/"open" until a human (or a reviewed process)
confirms it. Do not let a failing FITS case alone justify "confirmed."
-->

# <Finding ID>: <one-line summary>

**Status:** open
**Category:** <one of the four above>

## Evidence

<Source-based evidence only: quote the spec, quote the Supporting Data, cite the FITS case, cite the code. No interpretation yet.>

## Interpretation

<Your analysis, clearly labeled as analysis, not fact.>

## Affected

- Spec sections: <...>
- Code locations: <...>
- FITS cases: <...>
