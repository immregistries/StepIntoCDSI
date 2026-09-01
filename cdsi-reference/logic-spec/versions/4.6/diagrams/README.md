# Repetition and Transition Diagrams

Structured documentation for the major processing loops (Phase 7 of the plan) - each loop directory has the original extracted figure, a reviewed `transitions.yaml`, a Mermaid diagram, and a short explanation of the iteration unit, entry point, exit condition, and state affected. All six required loops are documented:

`overall-chapter-4-flow/`, `relevant-patient-series-selection/`, `chapter-6-dose-evaluation/`, `chapter-6-to-7-evaluate-and-forecast/`, `chapter-8-series-selection/`, `chapter-9-vaccine-group-evaluation/`.

Every individual step package's own `transitions.yaml` (see `../steps/*/transitions.yaml`) records that one step's outgoing transitions; these loop-level diagrams re-package several step packages' transitions into the larger cycle they form together, always citing the source step package each transition came from rather than re-deriving it.
