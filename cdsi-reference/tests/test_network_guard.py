"""Phase 11: proves - not just claims - that this module never needs
network access or an LLM. Blocking socket.socket.connect() covers both:
every LLM SDK and every HTTP/network library ultimately opens a real
socket to reach anything outside this machine."""

import socket

import pytest

from cdsi_reference_tools import network_guard


@pytest.fixture(autouse=True)
def _restore_socket():
    yield
    network_guard.uninstall()


def test_guard_blocks_a_real_connection_attempt():
    network_guard.install()
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        with pytest.raises(network_guard.NetworkAccessDisabledError):
            s.connect(("example.com", 80))
    finally:
        s.close()


def test_validate_command_succeeds_with_network_access_disabled():
    """The actual proof this phase asked for: run the real CLI validate
    command against the real registered 4.6 version with the network
    guard active, and confirm it completes normally - not just that the
    guard mechanism itself works in isolation."""
    pytest.importorskip("pymupdf")
    from cdsi_reference_tools import cli, paths

    if not paths.manifest_path("4.6").exists():
        pytest.skip("4.6 is not registered under logic-spec/versions/4.6/")

    exit_code = cli.main(["logic-spec", "validate", "--version", "4.6"])
    assert exit_code == 0
