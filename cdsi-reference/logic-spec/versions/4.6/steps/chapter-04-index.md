# Chapter 4: Processing Model

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 30-31 (chapter overview only - see linked step packages for each subsection). Table 4-1 (Logic Specification Processing Steps), Figure 4-1 (Logic Specification Processing Model), Figure 4-2 (Refinement of Patient Series).

## Overview

**[SPEC]** "At a very simple level, the major logical steps involved in the immunization evaluation and forecasting engine can be described in two parts. The first part is very mechanical in nature and focuses on gathering and prepping all of the required data. The second part uses the data gathered earlier to generate the evaluation and forecast." Table 4-1 lists the six sections below as the top-level activities; Figure 4-1 shows them as a process model, and Figure 4-2 shows how the working set of patient series narrows at each stage: Antigen Series → Relevant Patient Series → Scorable Patient Series → Prioritized Patient Series → Best Patient Series.

## Subordinate steps

| Section | Title | Status |
| --- | --- | --- |
| [4.1](04-01-gather-necessary-data/index.md) | Gather Necessary Data | reviewed |
| [4.2](04-02-organize-immunization-history/index.md) | Organize Immunization History | draft |
| [4.3](04-03-create-relevant-patient-series/index.md) | Create Relevant Patient Series | draft |
| [4.4](04-04-evaluate-and-forecast-all-relevant-patient-series/index.md) | Evaluate and Forecast all Relevant Patient Series | draft |
| [4.5](04-05-select-best-patient-series/index.md) | Select Best Patient Series | draft |
| [4.6](04-06-identify-and-evaluate-vaccine-group/index.md) | Identify and Evaluate Vaccine Group | draft |

Two of these (4.3, 4.5) are, in the code, loop drivers around Chapter 5's and Chapter 8's actual decision logic rather than decision steps themselves - see their own index.md files.
