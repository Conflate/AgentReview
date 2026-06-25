package com.agentreview.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentreview.common.TestStatus;
import org.junit.jupiter.api.Test;

class TestOutputParserServiceTest {

	private final TestOutputParserService parser = new TestOutputParserService();

	@Test
	void detectStatusReturnsNotRunForBlankOutput() {
		assertThat(parser.detectStatus("")).isEqualTo(TestStatus.NOT_RUN);
	}

	@Test
	void detectStatusReturnsPassedForSuccessfulMavenOutput() {
		assertThat(parser.detectStatus("Tests run: 42, Failures: 0, Errors: 0, Skipped: 0 BUILD SUCCESS"))
				.isEqualTo(TestStatus.PASSED);
	}

	@Test
	void detectStatusReturnsFailedForBuildFailure() {
		assertThat(parser.detectStatus("BUILD FAILURE")).isEqualTo(TestStatus.FAILED);
	}

	@Test
	void detectStatusReturnsFailedForNonZeroFailures() {
		assertThat(parser.detectStatus("Tests run: 42, Failures: 1, Errors: 0, Skipped: 0"))
				.isEqualTo(TestStatus.FAILED);
	}

	@Test
	void detectStatusReturnsUnknownForUnrecognizedOutput() {
		assertThat(parser.detectStatus("finished command")).isEqualTo(TestStatus.UNKNOWN);
	}

	@Test
	void detectStatusDoesNotPassBuildSuccessWithoutTestCounts() {
		assertThat(parser.detectStatus("BUILD SUCCESS")).isEqualTo(TestStatus.UNKNOWN);
	}
}
