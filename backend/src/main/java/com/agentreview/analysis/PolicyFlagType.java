package com.agentreview.analysis;

public enum PolicyFlagType {
	PROTECTED_PATH_CHANGED("Protected path changed"),
	CI_WORKFLOW_CHANGED("CI or workflow file changed"),
	DEPENDENCY_MANIFEST_CHANGED("Dependency manifest changed"),
	SOURCE_FILE_DELETED("Source file deleted"),
	RESTRICTED_COMMAND_USED("Restricted command used"),
	TESTS_FAILED("Submitted tests failed"),
	NO_TEST_OUTPUT("No test output submitted"),
	TEST_OUTPUT_INCONCLUSIVE("Submitted test output did not prove tests passed");

	private final String displayName;

	PolicyFlagType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
