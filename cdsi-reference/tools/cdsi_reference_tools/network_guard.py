"""Phase 11: makes "no network access, no LLM" (this module's own design
principle, stated in its README since Phase 1) an enforced runtime
guarantee instead of just a claim nobody re-checks.

Blocking socket.socket.connect() is enough to cover both halves at once:
urllib, http.client, requests, and every LLM SDK all eventually open a
real socket to talk to anything - block that one chokepoint and no
higher-level library can reach a network address or an LLM API, no
matter which one a future change introduces.

Installed for every `logic-spec` CLI invocation (see cli.py's main()) -
not merely available for tests to opt into - so a network dependency
accidentally introduced by future work fails loudly and immediately
instead of silently working on a machine that happens to have
connectivity and then failing mysteriously in an offline CI job.
"""

import socket

_real_connect = socket.socket.connect


class NetworkAccessDisabledError(RuntimeError):
    pass


def _blocked_connect(self, *args, **kwargs):
    raise NetworkAccessDisabledError(
        "cdsi_reference_tools attempted a network connection. This module's design principle "
        "(see README.md) is that it makes no network calls and no LLM calls - every command runs "
        "on the checked-in source PDF and repository files alone. If new functionality genuinely "
        "needs a live connection (e.g., a one-time PDF-fetching helper), it must be its own "
        "explicitly-invoked script outside the `logic-spec` command tree, never reachable from "
        "extract/validate/compare."
    )


def install() -> None:
    socket.socket.connect = _blocked_connect


def uninstall() -> None:
    socket.socket.connect = _real_connect
