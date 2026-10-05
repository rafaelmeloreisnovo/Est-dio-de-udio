#!/usr/bin/env python3
# Copyright (c) 2026 Rafael Melo Reis.
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
"""Fail-closed, no-network audit of the audio app's authorial/client boundary."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "contracts" / "rafpolimata-authorial-client-consumer.v1.json"

EXPECTED_CONTROL_SHA = "ab60b2dc3444ce34136e718ab13a1f3654cd50cd"
EXPECTED_GRADLE_SHA = "f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def load_contract(path: Path = CONTRACT) -> dict[str, Any]:
    return json.loads(read(path))


def source_java_files() -> list[Path]:
    return sorted((ROOT / "app" / "src" / "main" / "java").rglob("*.java"))


def source_cpp_files() -> list[Path]:
    return sorted((ROOT / "app" / "src" / "main" / "cpp").glob("*.c"))


def validate(contract: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if contract.get("schema") != "rafaelia.audio.authorial-client-consumer/v1":
        errors.append("schema")
    if contract.get("claim_allowed") is not False:
        errors.append("claim_allowed")
    if contract.get("spdx_license_identifier") != "LicenseRef-RAFCODE-Research-Commercial-0.1":
        errors.append("spdx_license_identifier")

    control = contract.get("control_contract", {})
    if control.get("repository") != "rafaelmeloreisnovo/RafPolimata":
        errors.append("control_repository")
    if control.get("path") != "rafci/authorial_clients.v1.json":
        errors.append("control_path")
    if control.get("sha") != EXPECTED_CONTROL_SHA:
        errors.append("control_sha")
    if not re.fullmatch(r"[0-9a-f]{40}", str(control.get("sha", ""))):
        errors.append("control_sha_shape")
    if control.get("body_copied") is not False:
        errors.append("provider_body_copy")
    if control.get("authority_transferred") is not False:
        errors.append("authority_transfer")
    if control.get("runtime_network_fetch_required") is not False:
        errors.append("runtime_network_fetch")

    local = contract.get("local_profile", {})
    if local.get("third_party_java_dependencies") != 0:
        errors.append("third_party_java_dependencies")
    if local.get("androidx") is not False:
        errors.append("androidx")
    if local.get("kotlin_app_runtime") is not False:
        errors.append("kotlin_app_runtime")
    if local.get("r8") != "OFF" or local.get("resource_shrink") != "OFF":
        errors.append("r8_or_resource_shrink")
    if local.get("gradle", {}).get("sha256") != EXPECTED_GRADLE_SHA:
        errors.append("gradle_sha")
    if local.get("jni", {}).get("implementation_files") != 1:
        errors.append("jni_contract_count")
    if local.get("jni", {}).get("canonical_edge") != "app/src/main/cpp/jni_bridge.c":
        errors.append("jni_contract_edge")

    license_boundary = contract.get("license_boundary", {})
    if license_boundary.get("project_source") != "LICENSE_RESEARCH_COMMERCIAL.md":
        errors.append("project_license_source")
    if license_boundary.get("provider_licenses_changed") is not False:
        errors.append("provider_license_change")
    if license_boundary.get("provider_source_vendored_by_this_change") is not False:
        errors.append("provider_source_vendored")
    if license_boundary.get("provider_notice_removed") is not False:
        errors.append("provider_notice_removed")
    if license_boundary.get("technical_control_replaces_legal_rights") is not False:
        errors.append("technical_control_replaces_legal_rights")

    root_gradle = read(ROOT / "build.gradle")
    app_gradle = read(ROOT / "app" / "build.gradle")
    toolchain = read(ROOT / "ci" / "rafaelia-toolchain.sh")
    cmake = read(ROOT / "app" / "src" / "main" / "cpp" / "CMakeLists.txt")

    required_root = "id 'com.android.application' version '8.9.2' apply false"
    if required_root not in root_gradle:
        errors.append("agp_identity")

    for literal in (
        "compileSdk 36",
        "minSdk 29",
        "targetSdk 36",
        "abiFilters 'armeabi-v7a', 'arm64-v8a'",
        "version '3.22.1'",
    ):
        if literal not in app_gradle:
            errors.append("app_gradle:" + literal)

    if re.search(r"(?m)^\s*(implementation|api|compileOnly|runtimeOnly|kapt|ksp)\s", app_gradle):
        errors.append("gradle_runtime_dependency_declaration")
    if re.search(r"minifyEnabled\s+true|shrinkResources\s+true", app_gradle):
        errors.append("r8_enabled")

    for literal in (
        'local version="8.11.1"',
        f'local sha256="{EXPECTED_GRADLE_SHA}"',
        '"platforms;android-36"',
        '"build-tools;36.0.0"',
        '"ndk;27.2.12479018"',
        '"cmake;3.22.1"',
    ):
        if literal not in toolchain:
            errors.append("toolchain:" + literal)

    for literal in ("-ffreestanding", "-fno-builtin", "-nostdinc", "-fno-stack-protector"):
        if literal not in cmake:
            errors.append("cmake_core_flag:" + literal)

    jni_files = []
    for path in source_cpp_files():
        if "JNIEXPORT" in read(path):
            jni_files.append(path.relative_to(ROOT).as_posix())
    if jni_files != ["app/src/main/cpp/jni_bridge.c"]:
        errors.append("jni_files:" + ",".join(jni_files))

    forbidden_import = re.compile(r"(?m)^\s*import\s+(androidx|kotlin|com\.google)\.")
    for path in source_java_files():
        if forbidden_import.search(read(path)):
            errors.append("external_java_import:" + path.relative_to(ROOT).as_posix())

    if not (ROOT / "LICENSE_RESEARCH_COMMERCIAL.md").is_file():
        errors.append("license_file_missing")

    return errors


def selftest(contract: dict[str, Any]) -> None:
    assert validate(contract) == [], validate(contract)

    bad = copy.deepcopy(contract)
    bad["claim_allowed"] = True
    assert "claim_allowed" in validate(bad)

    bad = copy.deepcopy(contract)
    bad["control_contract"]["authority_transferred"] = True
    assert "authority_transfer" in validate(bad)

    bad = copy.deepcopy(contract)
    bad["local_profile"]["third_party_java_dependencies"] = 1
    assert "third_party_java_dependencies" in validate(bad)

    bad = copy.deepcopy(contract)
    bad["license_boundary"]["provider_notice_removed"] = True
    assert "provider_notice_removed" in validate(bad)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--contract", type=Path, default=CONTRACT)
    parser.add_argument("--selftest", action="store_true")
    parser.add_argument("--receipt", type=Path)
    args = parser.parse_args()

    contract = load_contract(args.contract)
    errors = validate(contract)
    if errors:
        print(json.dumps({"state": "FAIL", "errors": errors}, indent=2, sort_keys=True))
        return 1
    if args.selftest:
        selftest(contract)

    receipt = {
        "schema": "rafaelia.audio.authorial-client-gate-receipt/v1",
        "state": "PASS_SOURCE_CONTRACT_ONLY",
        "contract_sha256": sha256_text(json.dumps(contract, sort_keys=True, separators=(",", ":"))),
        "control_contract_sha": contract["control_contract"]["sha"],
        "java_third_party_dependencies": 0,
        "androidx": 0,
        "kotlin_app_runtime": 0,
        "r8_shrink": 0,
        "jni_implementation_files": 1,
        "provider_body_copied": False,
        "provider_license_changed": False,
        "network_used": False,
        "apk_build": "TOKEN_VAZIO_AT_THIS_GATE",
        "runtime_execution": "TOKEN_VAZIO",
        "physical_execution": "TOKEN_VAZIO",
        "claim_allowed": False,
    }
    if args.receipt:
        args.receipt.parent.mkdir(parents=True, exist_ok=True)
        args.receipt.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(receipt, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
