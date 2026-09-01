"""cdsi_reference_tools: deterministic extraction, normalization, and
validation tools for cdsi-reference.

No network access and no LLM calls happen anywhere in this package -
extraction is mechanical (PDF parsing) and interpretation (step docs,
findings) is drafted by a human or an agent working outside this tool and
reviewed before being trusted.
"""

__version__ = "0.1.0"
