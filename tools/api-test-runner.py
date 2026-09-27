import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Optional


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_HTTP_FILE = REPO_ROOT / "api-test.http"
RESULTS_DIR = REPO_ROOT / "test-results"

DEFAULT_BASE_URL = "http://localhost:8080/api/v1"
DEFAULT_AUTH_EMAIL = os.getenv("API_TEST_EMAIL", "customer1@example.com")
DEFAULT_AUTH_PASSWORD = os.getenv("API_TEST_PASSWORD")
AUTH_LOGIN_PATH = "/auth/login"


@dataclass
class ApiTest:
    number: int
    title: str
    expected_status: Optional[int]
    method: str
    url: str
    headers: dict
    body: Optional[str]
    purpose: str


@dataclass
class TestResult:
    test: ApiTest
    actual_status: Optional[int]
    response_body: str
    result: str
    error: Optional[str] = None


def parse_args():
    parser = argparse.ArgumentParser(
        description="Execute API tests from api-test.http"
    )

    parser.add_argument(
        "--tests",
        required=True,
        help="Test selection. Examples: 103, 103-118, 103,105,110"
    )

    parser.add_argument(
        "--file",
        default=str(DEFAULT_HTTP_FILE),
        help="Path to HTTP test file"
    )

    parser.add_argument(
        "--base-url",
        default=os.getenv("API_BASE_URL", DEFAULT_BASE_URL),
        help="API base URL"
    )

    parser.add_argument(
        "--timeout",
        type=int,
        default=30,
        help="HTTP timeout in seconds"
    )

    parser.add_argument(
        "--auth-email",
        default=DEFAULT_AUTH_EMAIL,
        help="Email used to obtain a fresh JWT for authenticated tests"
    )

    parser.add_argument(
        "--auth-password",
        default=DEFAULT_AUTH_PASSWORD,
        help="Password used to obtain a fresh JWT; preferably set API_TEST_PASSWORD"
    )

    return parser.parse_args()


def parse_test_selection(selection: str) -> list[int]:
    numbers = set()

    for part in selection.split(","):
        part = part.strip()

        if not part:
            continue

        if "-" in part:
            start_text, end_text = part.split("-", 1)

            start = int(start_text)
            end = int(end_text)

            if start > end:
                raise ValueError(
                    f"Invalid range: {part}"
                )

            numbers.update(range(start, end + 1))

        else:
            numbers.add(int(part))

    return sorted(numbers)


def extract_tests(content: str, base_url: str) -> dict[int, ApiTest]:
    """
    Parse tests using markers such as:

    # ! TEST 103 - Get Cart - EMPTY CART

    Also accepts:

    ### ! Test 102 - ...
    """

    marker_pattern = re.compile(
        r"^\s*(?:###\s*)?#\s*!\s*TEST\s+(\d+)\s*-\s*(.+?)\s*$",
        re.IGNORECASE | re.MULTILINE
    )

    matches = list(marker_pattern.finditer(content))

    tests = {}

    for index, match in enumerate(matches):
        number = int(match.group(1))
        title = match.group(2).strip()

        start = match.end()
        end = (
            matches[index + 1].start()
            if index + 1 < len(matches)
            else len(content)
        )

        block = content[start:end]

        test = parse_test_block(
            number=number,
            title=title,
            block=block,
            base_url=base_url
        )

        tests[number] = test

    return tests


def parse_test_block(
    number: int,
    title: str,
    block: str,
    base_url: str
) -> ApiTest:

    expected_status = None
    purpose = ""

    expected_match = re.search(
        r"#\s*Expected:\s*HTTP\s+(\d+)",
        block,
        re.IGNORECASE
    )

    if expected_match:
        expected_status = int(expected_match.group(1))

    purpose_match = re.search(
        r"#\s*Purpose:\s*(.+)",
        block,
        re.IGNORECASE
    )

    if purpose_match:
        purpose = purpose_match.group(1).strip()

    lines = block.splitlines()

    method = None
    url = None
    headers = {}
    body_lines = []

    request_started = False
    body_started = False

    for raw_line in lines:
        line = raw_line.strip()

        if not line:
            if request_started:
                body_started = True

            continue

        # Ignore comments.
        if line.startswith("#"):
            continue

        # HTTP request line.
        request_match = re.match(
            r"^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s+(.+)$",
            line,
            re.IGNORECASE
        )

        if request_match and method is None:
            method = request_match.group(1).upper()
            url = request_match.group(2).strip()
            request_started = True
            body_started = False
            continue

        # Headers.
        if request_started and not body_started and ":" in line:
            key, value = line.split(":", 1)

            headers[key.strip()] = value.strip()
            continue

        # Body.
        if request_started and body_started:
            body_lines.append(raw_line)

    if method is None or url is None:
        raise ValueError(
            f"Could not parse HTTP request for TEST {number}"
        )

    url = url.replace("{{baseUrl}}", base_url)

    body = "\n".join(body_lines).strip()

    if not body:
        body = None

    return ApiTest(
        number=number,
        title=title,
        expected_status=expected_status,
        method=method,
        url=url,
        headers=headers,
        body=body,
        purpose=purpose
    )


def obtain_access_token(
    base_url: str,
    email: str,
    password: Optional[str],
    timeout: int
) -> str:
    """Obtain a fresh JWT for authenticated API tests."""
    if not password:
        raise RuntimeError(
            "No API test password supplied. Set API_TEST_PASSWORD "
            "or pass --auth-password."
        )

    login_url = f"{base_url.rstrip('/')}{AUTH_LOGIN_PATH}"
    payload = json.dumps({
        "email": email,
        "password": password
    }).encode("utf-8")

    request = urllib.request.Request(
        url=login_url,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_body = response.read().decode(
                "utf-8",
                errors="replace"
            )

            if response.status != 200:
                raise RuntimeError(
                    f"Authentication failed with HTTP {response.status}: "
                    f"{response_body}"
                )

    except urllib.error.HTTPError as error:
        response_body = error.read().decode(
            "utf-8",
            errors="replace"
        )
        raise RuntimeError(
            f"Authentication failed with HTTP {error.code}: {response_body}"
        ) from error
    except urllib.error.URLError as error:
        raise RuntimeError(
            f"Unable to reach authentication endpoint: {error.reason}"
        ) from error

    try:
        parsed = json.loads(response_body)
    except json.JSONDecodeError as error:
        raise RuntimeError(
            "Authentication endpoint returned invalid JSON."
        ) from error

    token = parsed.get("accessToken")

    if not token:
        raise RuntimeError(
            "Authentication succeeded but response did not contain accessToken."
        )

    return token


def inject_auth_token(
    test: ApiTest,
    access_token: str
) -> ApiTest:
    """Replace a test's Authorization header with a fresh JWT."""
    headers = dict(test.headers)

    has_authorization = any(
        key.lower() == "authorization"
        for key in headers
    )

    if not has_authorization:
        return test

    for key in list(headers):
        if key.lower() == "authorization":
            headers[key] = f"Bearer {access_token}"

    return ApiTest(
        number=test.number,
        title=test.title,
        expected_status=test.expected_status,
        method=test.method,
        url=test.url,
        headers=headers,
        body=test.body,
        purpose=test.purpose
    )


def execute_test(test: ApiTest, timeout: int) -> TestResult:
    body_bytes = None

    if test.body is not None:
        body_bytes = test.body.encode("utf-8")

    request = urllib.request.Request(
        url=test.url,
        data=body_bytes,
        headers=test.headers,
        method=test.method
    )

    try:
        with urllib.request.urlopen(
            request,
            timeout=timeout
        ) as response:

            status = response.status
            response_body = response.read().decode(
                "utf-8",
                errors="replace"
            )

            result = determine_result(
                expected=test.expected_status,
                actual=status
            )

            return TestResult(
                test=test,
                actual_status=status,
                response_body=response_body,
                result=result
            )

    except urllib.error.HTTPError as error:
        response_body = error.read().decode(
            "utf-8",
            errors="replace"
        )

        status = error.code

        result = determine_result(
            expected=test.expected_status,
            actual=status
        )

        return TestResult(
            test=test,
            actual_status=status,
            response_body=response_body,
            result=result
        )

    except urllib.error.URLError as error:
        return TestResult(
            test=test,
            actual_status=None,
            response_body="",
            result="BLOCKED",
            error=f"Unable to reach API: {error.reason}"
        )

    except TimeoutError:
        return TestResult(
            test=test,
            actual_status=None,
            response_body="",
            result="BLOCKED",
            error="Request timed out."
        )

    except Exception as error:
        return TestResult(
            test=test,
            actual_status=None,
            response_body="",
            result="BLOCKED",
            error=str(error)
        )


def determine_result(
    expected: Optional[int],
    actual: Optional[int]
) -> str:

    if expected is None:
        return "BLOCKED"

    if expected == actual:
        return "PASS"

    return "FAIL"


def redact_headers(headers: dict) -> dict:
    redacted = {}

    for key, value in headers.items():
        if key.lower() == "authorization":
            redacted[key] = "Bearer <REDACTED>"
        else:
            redacted[key] = value

    return redacted


def format_response_body(body: str) -> str:
    if not body:
        return "<empty response body>"

    try:
        parsed = json.loads(body)

        return json.dumps(
            parsed,
            indent=2,
            ensure_ascii=False
        )

    except json.JSONDecodeError:
        return body


def format_test_result(result: TestResult) -> str:
    test = result.test

    lines = []

    lines.append("=" * 72)
    lines.append(
        f"TEST {test.number} - {test.title}"
    )
    lines.append("=" * 72)
    lines.append("")

    lines.append(f"Method: {test.method}")
    lines.append(f"URL: {test.url}")
    lines.append("")

    lines.append("Expected:")
    if test.expected_status is None:
        lines.append("HTTP status not defined")
    else:
        lines.append(f"HTTP {test.expected_status}")

    lines.append("")

    lines.append("Actual:")

    if result.actual_status is None:
        lines.append("No HTTP response")
    else:
        lines.append(f"HTTP {result.actual_status}")

    lines.append("")

    lines.append("Result:")
    lines.append(result.result)

    if test.purpose:
        lines.append("")
        lines.append("Purpose:")
        lines.append(test.purpose)

    lines.append("")

    lines.append("Request Headers:")

    for key, value in redact_headers(test.headers).items():
        lines.append(f"{key}: {value}")

    if test.body:
        lines.append("")
        lines.append("Request Body:")
        lines.append(test.body)

    lines.append("")

    lines.append("Response:")

    if result.error:
        lines.append(result.error)
    else:
        lines.append(
            format_response_body(result.response_body)
        )

    lines.append("")

    return "\n".join(lines)


def write_report(
    results: list[TestResult],
    selection: str
) -> Path:

    RESULTS_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    safe_selection = selection.replace(
        ",",
        "_"
    ).replace(
        "-",
        "_to_"
    )

    output_file = (
        RESULTS_DIR /
        f"tests-{safe_selection}.txt"
    )

    lines = []

    lines.append("=" * 72)
    lines.append("API TEST EXECUTION REPORT")
    lines.append("=" * 72)
    lines.append("")
    lines.append(f"Selection: {selection}")
    lines.append("")

    for result in results:
        lines.append(
            format_test_result(result)
        )

    passed = sum(
        1 for result in results
        if result.result == "PASS"
    )

    failed = sum(
        1 for result in results
        if result.result == "FAIL"
    )

    blocked = sum(
        1 for result in results
        if result.result == "BLOCKED"
    )

    total = len(results)

    lines.append("=" * 72)
    lines.append("SUMMARY")
    lines.append("=" * 72)
    lines.append("")
    lines.append(f"Total:   {total}")
    lines.append(f"Passed:  {passed}")
    lines.append(f"Failed:  {failed}")
    lines.append(f"Blocked: {blocked}")

    if total:
        pass_rate = (passed / total) * 100
        lines.append(
            f"Pass Rate: {pass_rate:.2f}%"
        )

    failed_tests = [
        str(result.test.number)
        for result in results
        if result.result == "FAIL"
    ]

    blocked_tests = [
        str(result.test.number)
        for result in results
        if result.result == "BLOCKED"
    ]

    if failed_tests:
        lines.append("")
        lines.append(
            "Failed Tests: " +
            ", ".join(failed_tests)
        )

    if blocked_tests:
        lines.append("")
        lines.append(
            "Blocked Tests: " +
            ", ".join(blocked_tests)
        )

    output_file.write_text(
        "\n".join(lines),
        encoding="utf-8"
    )

    return output_file


def main():
    args = parse_args()

    try:
        selected_numbers = parse_test_selection(
            args.tests
        )
    except ValueError as error:
        print(f"ERROR: {error}")
        sys.exit(1)

    http_file = Path(args.file)

    if not http_file.exists():
        print(
            f"ERROR: HTTP test file not found: {http_file}"
        )
        sys.exit(1)

    content = http_file.read_text(
        encoding="utf-8"
    )

    tests = extract_tests(
        content=content,
        base_url=args.base_url
    )

    if not tests:
        print(
            "ERROR: No API tests were found in the HTTP file."
        )
        sys.exit(1)

    results = []

    # Obtain one fresh JWT only when the selected tests contain an
    # Authorization header. Tests without Authorization remain unauthenticated.
    selected_tests = [
        tests[number]
        for number in selected_numbers
        if number in tests
    ]

    requires_auth = any(
        any(key.lower() == "authorization" for key in test.headers)
        for test in selected_tests
    )

    access_token = None

    if requires_auth:
        try:
            access_token = obtain_access_token(
                base_url=args.base_url,
                email=args.auth_email,
                password=args.auth_password,
                timeout=args.timeout
            )
            print(f"Authentication: obtained fresh JWT for {args.auth_email}")
        except RuntimeError as error:
            print(f"ERROR: {error}")
            sys.exit(1)

    for number in selected_numbers:

        if number not in tests:
            print(
                f"TEST {number}: NOT FOUND"
            )

            missing_test = ApiTest(
                number=number,
                title="NOT FOUND",
                expected_status=None,
                method="",
                url="",
                headers={},
                body=None,
                purpose=""
            )

            results.append(
                TestResult(
                    test=missing_test,
                    actual_status=None,
                    response_body="",
                    result="BLOCKED",
                    error=(
                        f"TEST {number} does not exist "
                        "in api-test.http."
                    )
                )
            )

            continue

        test = tests[number]

        if access_token is not None:
            test = inject_auth_token(
                test=test,
                access_token=access_token
            )

        print(
            f"Running TEST {test.number} - {test.title}"
        )

        result = execute_test(
            test=test,
            timeout=args.timeout
        )

        results.append(result)

        status = (
            str(result.actual_status)
            if result.actual_status is not None
            else "-"
        )

        print(
            f"  Expected: {test.expected_status} | "
            f"Actual: {status} | "
            f"Result: {result.result}"
        )

    report = write_report(
        results=results,
        selection=args.tests
    )

    print("")
    print(
        f"Report created: {report}"
    )


if __name__ == "__main__":
    main()