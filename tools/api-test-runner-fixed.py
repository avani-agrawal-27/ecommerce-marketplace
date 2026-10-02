import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, List, Optional, Tuple


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_HTTP_FILE = REPO_ROOT / "api-test.http"
RESULTS_DIR = REPO_ROOT / "test-results"

DEFAULT_BASE_URL = "http://localhost:8080/api/v1"
DEFAULT_AUTH_EMAIL = os.getenv("API_TEST_EMAIL", "customer1@example.com")
DEFAULT_AUTH_PASSWORD = os.getenv("API_TEST_PASSWORD")
DEFAULT_NON_OWNER_EMAIL = os.getenv("API_TEST_NON_OWNER_EMAIL")
DEFAULT_NON_OWNER_PASSWORD = os.getenv("API_TEST_NON_OWNER_PASSWORD")
AUTH_LOGIN_PATH = "/auth/login"


class ApiTest:
    def __init__(
        self,
        number: int,
        title: str,
        expected_status: Optional[int],
        method: str,
        url: str,
        headers: Dict[str, str],
        body: Optional[str],
        purpose: str,
    ):
        self.number = number
        self.title = title
        self.expected_status = expected_status
        self.method = method
        self.url = url
        self.headers = headers
        self.body = body
        self.purpose = purpose


class TestResult:
    def __init__(
        self,
        test: ApiTest,
        actual_status: Optional[int],
        response_body: str,
        result: str,
        error: Optional[str] = None,
    ):
        self.test = test
        self.actual_status = actual_status
        self.response_body = response_body
        self.result = result
        self.error = error


def parse_args():
    parser = argparse.ArgumentParser(
        description="Execute API tests from api-test.http"
    )

    # Support both --tests and the singular --test used in earlier commands.
    parser.add_argument(
        "--tests",
        "--test",
        dest="tests",
        required=True,
        help="Test selection. Examples: 103, 103-118, 103,105,110",
    )

    parser.add_argument(
        "--file",
        default=str(DEFAULT_HTTP_FILE),
        help="Path to HTTP test file",
    )

    parser.add_argument(
        "--base-url",
        default=os.getenv("API_BASE_URL", DEFAULT_BASE_URL),
        help="API base URL",
    )

    parser.add_argument(
        "--timeout",
        type=int,
        default=30,
        help="HTTP timeout in seconds",
    )

    parser.add_argument(
        "--auth-email",
        default=DEFAULT_AUTH_EMAIL,
        help="Owner/customer email used to obtain a fresh JWT",
    )

    parser.add_argument(
        "--auth-password",
        default=DEFAULT_AUTH_PASSWORD,
        help="Owner/customer password; preferably set API_TEST_PASSWORD",
    )

    parser.add_argument(
        "--non-owner-email",
        default=DEFAULT_NON_OWNER_EMAIL,
        help="Second-user email for non-owner tests such as 148/149",
    )

    parser.add_argument(
        "--non-owner-password",
        default=DEFAULT_NON_OWNER_PASSWORD,
        help="Second-user password for non-owner tests",
    )

    parser.add_argument(
        "--payment-test-order-id",
        default=os.getenv("PAYMENT_TEST_ORDER_ID"),
        help=(
            "Existing PENDING_PAYMENT order UUID used by payment test 141. "
            "Can also be supplied through PAYMENT_TEST_ORDER_ID."
        ),
    )

    return parser.parse_args()


def parse_test_selection(selection: str) -> List[int]:
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
                raise ValueError("Invalid range: {}".format(part))

            numbers.update(range(start, end + 1))
        else:
            numbers.add(int(part))

    return sorted(numbers)


def extract_tests(content: str, base_url: str) -> Dict[int, ApiTest]:
    marker_pattern = re.compile(
        r"^\s*(?:###\s*)?#\s*!\s*TEST\s+(\d+)\s*-\s*(.+?)\s*$",
        re.IGNORECASE | re.MULTILINE,
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

        tests[number] = parse_test_block(
            number=number,
            title=title,
            block=block,
            base_url=base_url,
        )

    return tests


def parse_test_block(
    number: int,
    title: str,
    block: str,
    base_url: str,
) -> ApiTest:
    expected_status = None
    purpose = ""

    expected_match = re.search(
        r"#\s*Expected:\s*HTTP\s+(\d+)",
        block,
        re.IGNORECASE,
    )
    if expected_match:
        expected_status = int(expected_match.group(1))

    purpose_match = re.search(
        r"#\s*Purpose:\s*(.+)",
        block,
        re.IGNORECASE,
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

        if line.startswith("#"):
            continue

        request_match = re.match(
            r"^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s+(.+)$",
            line,
            re.IGNORECASE,
        )

        if request_match and method is None:
            method = request_match.group(1).upper()
            url = request_match.group(2).strip()
            request_started = True
            body_started = False
            continue

        if request_started and not body_started and ":" in line:
            key, value = line.split(":", 1)
            headers[key.strip()] = value.strip()
            continue

        if request_started and body_started:
            body_lines.append(raw_line)

    if method is None or url is None:
        raise ValueError(
            "Could not parse HTTP request for TEST {}".format(number)
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
        purpose=purpose,
    )


def obtain_access_token(
    base_url: str,
    email: str,
    password: Optional[str],
    timeout: int,
) -> str:
    if not password:
        raise RuntimeError(
            "No API test password supplied for {}. "
            "Set the appropriate environment variable or pass the password.".format(
                email
            )
        )

    login_url = "{}{}".format(base_url.rstrip("/"), AUTH_LOGIN_PATH)
    payload = json.dumps(
        {"email": email, "password": password}
    ).encode("utf-8")

    request = urllib.request.Request(
        url=login_url,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_body = response.read().decode(
                "utf-8", errors="replace"
            )

            if response.status != 200:
                raise RuntimeError(
                    "Authentication failed for {} with HTTP {}: {}".format(
                        email, response.status, response_body
                    )
                )

    except urllib.error.HTTPError as error:
        response_body = error.read().decode(
            "utf-8", errors="replace"
        )
        raise RuntimeError(
            "Authentication failed for {} with HTTP {}: {}".format(
                email, error.code, response_body
            )
        )
    except urllib.error.URLError as error:
        raise RuntimeError(
            "Unable to reach authentication endpoint: {}".format(
                error.reason
            )
        )

    try:
        parsed = json.loads(response_body)
    except json.JSONDecodeError as error:
        raise RuntimeError(
            "Authentication endpoint returned invalid JSON for {}.".format(
                email
            )
        ) from error

    token = parsed.get("accessToken")

    if not token:
        raise RuntimeError(
            "Authentication succeeded for {} but response did not contain "
            "accessToken.".format(email)
        )

    return token


def replace_placeholders(
    value: Optional[str],
    variables: Dict[str, str],
) -> Optional[str]:
    if value is None:
        return None

    result = value

    # {{variable}} and <VARIABLE> are both supported.
    for name, replacement in variables.items():
        result = result.replace("{{" + name + "}}", replacement)
        result = result.replace("<" + name + ">", replacement)

    return result


def substitute_test(
    test: ApiTest,
    variables: Dict[str, str],
) -> ApiTest:
    headers = {
        key: replace_placeholders(value, variables)
        for key, value in test.headers.items()
    }

    return ApiTest(
        number=test.number,
        title=test.title,
        expected_status=test.expected_status,
        method=test.method,
        url=replace_placeholders(test.url, variables),
        headers=headers,
        body=replace_placeholders(test.body, variables),
        purpose=test.purpose,
    )


def inject_auth_token(
    test: ApiTest,
    access_token: str,
) -> ApiTest:
    headers = dict(test.headers)

    for key in list(headers):
        if key.lower() == "authorization":
            headers[key] = "Bearer {}".format(access_token)

    return ApiTest(
        number=test.number,
        title=test.title,
        expected_status=test.expected_status,
        method=test.method,
        url=test.url,
        headers=headers,
        body=test.body,
        purpose=test.purpose,
    )


def auth_type_for_test(test: ApiTest) -> Optional[str]:
    """
    Determine which auth identity a test requests.

    Supported markers in Authorization:
      <AUTO_AUTH> / <OWNER_AUTH>       -> owner/customer
      <NON_OWNER_AUTH> / <SECOND_AUTH> -> second user
      <SELLER_JWT>                     -> second-user credentials if supplied

    A literal Bearer token is left untouched.
    """
    for value in test.headers.values():
        upper = value.upper()

        if "AUTHORIZATION" not in value.upper() and not value.lower().startswith("bearer "):
            continue

        if "<NON_OWNER_AUTH>" in upper or "<SECOND_AUTH>" in upper:
            return "non_owner"

        if "<SELLER_JWT>" in upper:
            return "non_owner"

        if "<AUTO_AUTH>" in upper or "<OWNER_AUTH>" in upper:
            return "owner"

        # Existing convention: any Authorization header without a literal
        # token is treated as owner auth for backward compatibility.
        if value.strip().lower() == "bearer":
            return "owner"

    return None


def inject_selected_auth(
    test: ApiTest,
    owner_token: Optional[str],
    non_owner_token: Optional[str],
) -> ApiTest:
    auth_type = auth_type_for_test(test)

    if auth_type == "owner" and owner_token:
        return inject_auth_token(test, owner_token)

    if auth_type == "non_owner" and non_owner_token:
        return inject_auth_token(test, non_owner_token)

    # Preserve unauthenticated tests and literal auth headers.
    return test


def execute_test(test: ApiTest, timeout: int) -> TestResult:
    body_bytes = None

    if test.body is not None:
        body_bytes = test.body.encode("utf-8")

    request = urllib.request.Request(
        url=test.url,
        data=body_bytes,
        headers=test.headers,
        method=test.method,
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = response.status
            response_body = response.read().decode(
                "utf-8", errors="replace"
            )

            result = determine_result(
                expected=test.expected_status,
                actual=status,
            )

            return TestResult(
                test=test,
                actual_status=status,
                response_body=response_body,
                result=result,
            )

    except urllib.error.HTTPError as error:
        response_body = error.read().decode(
            "utf-8", errors="replace"
        )
        status = error.code

        result = determine_result(
            expected=test.expected_status,
            actual=status,
        )

        return TestResult(
            test=test,
            actual_status=status,
            response_body=response_body,
            result=result,
        )

    except urllib.error.URLError as error:
        return TestResult(
            test=test,
            actual_status=None,
            response_body="",
            result="BLOCKED",
            error="Unable to reach API: {}".format(error.reason),
        )

    except TimeoutError:
        return TestResult(
            test=test,
            actual_status=None,
            response_body="",
            result="BLOCKED",
            error="Request timed out.",
        )

    except Exception as error:
        return TestResult(
            test=test,
            actual_status=None,
            response_body="",
            result="BLOCKED",
            error=str(error),
        )


def determine_result(
    expected: Optional[int],
    actual: Optional[int],
) -> str:
    if expected is None:
        return "BLOCKED"

    if expected == actual:
        return "PASS"

    return "FAIL"


def extract_runtime_variables(
    test: ApiTest,
    result: TestResult,
    variables: Dict[str, str],
) -> None:
    """
    Capture IDs from successful JSON responses.

    Payment flow:
      create payment -> paymentId + orderId
      later tests can use <PAYMENT_TEST_PAYMENT_ID>
      and <PAYMENT_TEST_ORDER_ID>.

    The generic aliases are also captured:
      PAYMENT_ID
      ORDER_ID
    """
    if not result.response_body:
        return

    try:
        data = json.loads(result.response_body)
    except (ValueError, TypeError):
        return

    if not isinstance(data, dict):
        return

    if test.number == 141:
        payment_id = data.get("id")
        order_id = data.get("orderId")

        if payment_id:
            variables["PAYMENT_TEST_PAYMENT_ID"] = str(payment_id)
            variables["PAYMENT_ID"] = str(payment_id)

        if order_id:
            variables["PAYMENT_TEST_ORDER_ID"] = str(order_id)
            variables["ORDER_ID"] = str(order_id)

    # Generic useful captures for future tests.
    if data.get("id") and "ID" not in variables:
        variables["ID"] = str(data["id"])

    if data.get("orderId"):
        variables["ORDER_ID"] = str(data["orderId"])

    if data.get("paymentId"):
        variables["PAYMENT_ID"] = str(data["paymentId"])


def validate_unresolved_placeholders(test: ApiTest) -> Optional[str]:
    values = [test.url, test.body or ""]
    values.extend(test.headers.values())

    unresolved = set()

    for value in values:
        for match in re.findall(r"<([A-Z][A-Z0-9_]+)>", value):
            if match not in {"AUTO_AUTH", "OWNER_AUTH", "NON_OWNER_AUTH", "SECOND_AUTH", "SELLER_JWT"}:
                unresolved.add(match)

    if unresolved:
        return "Unresolved test placeholders: {}".format(
            ", ".join(sorted(unresolved))
        )

    return None


def redact_headers(headers: Dict[str, str]) -> Dict[str, str]:
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
            ensure_ascii=False,
        )
    except (ValueError, TypeError):
        return body


def format_test_result(result: TestResult) -> str:
    test = result.test
    lines = []

    lines.append("=" * 72)
    lines.append(
        "TEST {} - {}".format(test.number, test.title)
    )
    lines.append("=" * 72)
    lines.append("")
    lines.append("Method: {}".format(test.method))
    lines.append("URL: {}".format(test.url))
    lines.append("")
    lines.append("Expected:")

    if test.expected_status is None:
        lines.append("HTTP status not defined")
    else:
        lines.append("HTTP {}".format(test.expected_status))

    lines.append("")
    lines.append("Actual:")

    if result.actual_status is None:
        lines.append("No HTTP response")
    else:
        lines.append("HTTP {}".format(result.actual_status))

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
        lines.append("{}: {}".format(key, value))

    if test.body:
        lines.append("")
        lines.append("Request Body:")
        lines.append(test.body)

    lines.append("")
    lines.append("Response:")

    if result.error:
        lines.append(result.error)
    else:
        lines.append(format_response_body(result.response_body))

    lines.append("")
    return "\n".join(lines)


def write_report(
    results: List[TestResult],
    selection: str,
) -> Path:
    RESULTS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    safe_selection = (
        selection.replace(",", "_")
        .replace("-", "_to_")
    )

    output_file = RESULTS_DIR / "tests-{}.txt".format(safe_selection)

    lines = []
    lines.append("=" * 72)
    lines.append("API TEST EXECUTION REPORT")
    lines.append("=" * 72)
    lines.append("")
    lines.append("Selection: {}".format(selection))
    lines.append("")

    for result in results:
        lines.append(format_test_result(result))

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
    lines.append("Total:   {}".format(total))
    lines.append("Passed:  {}".format(passed))
    lines.append("Failed:  {}".format(failed))
    lines.append("Blocked: {}".format(blocked))

    if total:
        pass_rate = (passed / total) * 100
        lines.append("Pass Rate: {:.2f}%".format(pass_rate))

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
        lines.append("Failed Tests: " + ", ".join(failed_tests))

    if blocked_tests:
        lines.append("")
        lines.append("Blocked Tests: " + ", ".join(blocked_tests))

    output_file.write_text(
        "\n".join(lines),
        encoding="utf-8",
    )

    return output_file


def main():
    args = parse_args()

    try:
        selected_numbers = parse_test_selection(args.tests)
    except ValueError as error:
        print("ERROR: {}".format(error))
        sys.exit(1)

    http_file = Path(args.file)

    if not http_file.exists():
        print("ERROR: HTTP test file not found: {}".format(http_file))
        sys.exit(1)

    content = http_file.read_text(encoding="utf-8")

    try:
        tests = extract_tests(
            content=content,
            base_url=args.base_url,
        )
    except ValueError as error:
        print("ERROR: {}".format(error))
        sys.exit(1)

    if not tests:
        print("ERROR: No API tests were found in the HTTP file.")
        sys.exit(1)

    selected_tests = [
        tests[number]
        for number in selected_numbers
        if number in tests
    ]

    # Runtime variables can come from CLI/env and from previous test responses.
    variables = {}

    if args.payment_test_order_id:
        variables["PAYMENT_TEST_ORDER_ID"] = args.payment_test_order_id
        variables["ORDER_ID"] = args.payment_test_order_id

    # Determine whether owner or second-user credentials are required.
    needs_owner_auth = False
    needs_non_owner_auth = False

    for test in selected_tests:
        auth_type = auth_type_for_test(test)

        if auth_type == "owner":
            needs_owner_auth = True
        elif auth_type == "non_owner":
            needs_non_owner_auth = True

    owner_token = None
    non_owner_token = None

    if needs_owner_auth:
        try:
            owner_token = obtain_access_token(
                base_url=args.base_url,
                email=args.auth_email,
                password=args.auth_password,
                timeout=args.timeout,
            )
            print(
                "Authentication: obtained fresh owner JWT for {}".format(
                    args.auth_email
                )
            )
        except RuntimeError as error:
            print("ERROR: {}".format(error))
            sys.exit(1)

    if needs_non_owner_auth:
        if not args.non_owner_email or not args.non_owner_password:
            print(
                "ERROR: Tests require a second/non-owner user, but "
                "--non-owner-email and --non-owner-password were not supplied."
            )
            sys.exit(1)

        try:
            non_owner_token = obtain_access_token(
                base_url=args.base_url,
                email=args.non_owner_email,
                password=args.non_owner_password,
                timeout=args.timeout,
            )
            print(
                "Authentication: obtained fresh non-owner JWT for {}".format(
                    args.non_owner_email
                )
            )
        except RuntimeError as error:
            print("ERROR: {}".format(error))
            sys.exit(1)

    results = []

    for number in selected_numbers:
        if number not in tests:
            print("TEST {}: NOT FOUND".format(number))

            missing_test = ApiTest(
                number=number,
                title="NOT FOUND",
                expected_status=None,
                method="",
                url="",
                headers={},
                body=None,
                purpose="",
            )

            results.append(
                TestResult(
                    test=missing_test,
                    actual_status=None,
                    response_body="",
                    result="BLOCKED",
                    error=(
                        "TEST {} does not exist in api-test.http.".format(
                            number
                        )
                    ),
                )
            )
            continue

        # Substitute values captured from earlier tests/CLI.
        test = substitute_test(tests[number], variables)

        unresolved_error = validate_unresolved_placeholders(test)

        if unresolved_error:
            result = TestResult(
                test=test,
                actual_status=None,
                response_body="",
                result="BLOCKED",
                error=unresolved_error,
            )
            results.append(result)

            print(
                "TEST {} - BLOCKED: {}".format(
                    number, unresolved_error
                )
            )
            continue

        test = inject_selected_auth(
            test=test,
            owner_token=owner_token,
            non_owner_token=non_owner_token,
        )

        print(
            "Running TEST {} - {}".format(
                test.number, test.title
            )
        )

        result = execute_test(
            test=test,
            timeout=args.timeout,
        )

        results.append(result)

        # Capture IDs only after the response has been received.
        if result.result == "PASS":
            extract_runtime_variables(
                test=test,
                result=result,
                variables=variables,
            )

        status = (
            str(result.actual_status)
            if result.actual_status is not None
            else "-"
        )

        print(
            "  Expected: {} | Actual: {} | Result: {}".format(
                test.expected_status,
                status,
                result.result,
            )
        )

        if result.result == "PASS" and number == 141:
            if variables.get("PAYMENT_TEST_PAYMENT_ID"):
                print(
                    "  Captured paymentId: {}".format(
                        variables["PAYMENT_TEST_PAYMENT_ID"]
                    )
                )
            if variables.get("PAYMENT_TEST_ORDER_ID"):
                print(
                    "  Captured orderId: {}".format(
                        variables["PAYMENT_TEST_ORDER_ID"]
                    )
                )

    report = write_report(
        results=results,
        selection=args.tests,
    )

    print("")
    print("Report created: {}".format(report))


if __name__ == "__main__":
    main()
