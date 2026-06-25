package com.agentreview.analysis;

import com.agentreview.common.TestStatus;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class TestOutputParserService {

	private static final Pattern FAILURES_PATTERN = Pattern.compile("(?i)failures?\\s*[:=]\\s*(\\d+)");
	private static final Pattern ERRORS_PATTERN = Pattern.compile("(?i)errors?\\s*[:=]\\s*(\\d+)");

	public TestStatus detectStatus(String output) {
		if (output == null || output.isBlank()) {
			return TestStatus.NOT_RUN;
		}
		if (containsIgnoreCase(output, "BUILD FAILURE")) {
			return TestStatus.FAILED;
		}
		if (numberAfter(FAILURES_PATTERN, output).orElse(0) > 0 || numberAfter(ERRORS_PATTERN, output).orElse(0) > 0) {
			return TestStatus.FAILED;
		}
		if (containsIgnoreCase(output, "BUILD SUCCESS") && hasExplicitZeroFailuresAndErrors(output)) {
			return TestStatus.PASSED;
		}
		if (containsIgnoreCase(output, "Tests run:") && hasExplicitZeroFailuresAndErrors(output)) {
			return TestStatus.PASSED;
		}
		return TestStatus.UNKNOWN;
	}

	private boolean hasExplicitZeroFailuresAndErrors(String output) {
		OptionalInt failures = numberAfter(FAILURES_PATTERN, output);
		OptionalInt errors = numberAfter(ERRORS_PATTERN, output);
		return failures.isPresent() && failures.getAsInt() == 0
				&& errors.isPresent() && errors.getAsInt() == 0;
	}

	private OptionalInt numberAfter(Pattern pattern, String output) {
		Matcher matcher = pattern.matcher(output);
		if (!matcher.find()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(Integer.parseInt(matcher.group(1)));
	}

	private boolean containsIgnoreCase(String value, String expected) {
		return value.toLowerCase().contains(expected.toLowerCase());
	}
}
