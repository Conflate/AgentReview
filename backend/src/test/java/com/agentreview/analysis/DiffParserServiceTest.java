package com.agentreview.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentreview.common.FileChangeType;
import org.junit.jupiter.api.Test;

class DiffParserServiceTest {

	private final DiffParserService diffParserService = new DiffParserService();

	@Test
	void parseExtractsModifiedAddedAndDeletedFiles() {
		String diffText = """
				diff --git a/src/main/java/App.java b/src/main/java/App.java
				index 1111111..2222222 100644
				--- a/src/main/java/App.java
				+++ b/src/main/java/App.java
				@@ -1 +1 @@
				diff --git a/src/test/java/AppTest.java b/src/test/java/AppTest.java
				new file mode 100644
				index 0000000..3333333
				--- /dev/null
				+++ b/src/test/java/AppTest.java
				diff --git a/src/main/java/OldService.java b/src/main/java/OldService.java
				deleted file mode 100644
				index 4444444..0000000
				--- a/src/main/java/OldService.java
				+++ /dev/null
				""";

		var files = diffParserService.parse(diffText);

		assertThat(files).extracting("filePath")
				.containsExactly("src/main/java/App.java", "src/test/java/AppTest.java", "src/main/java/OldService.java");
		assertThat(files).extracting("changeType")
				.containsExactly(FileChangeType.MODIFIED, FileChangeType.ADDED, FileChangeType.DELETED);
	}

	@Test
	void parseUsesRenameTargetForRenamedFile() {
		String diffText = """
				diff --git a/src/main/java/OldName.java b/src/main/java/NewName.java
				similarity index 95%
				rename from src/main/java/OldName.java
				rename to src/main/java/NewName.java
				""";

		var files = diffParserService.parse(diffText);

		assertThat(files).hasSize(1);
		assertThat(files.get(0).filePath()).isEqualTo("src/main/java/NewName.java");
		assertThat(files.get(0).changeType()).isEqualTo(FileChangeType.RENAMED);
	}

	@Test
	void parseExtractsQuotedPathWithSpaces() {
		String diffText = """
				diff --git "a/src/main/java/Old App.java" "b/src/main/java/New App.java"
				index 1111111..2222222 100644
				--- "a/src/main/java/Old App.java"
				+++ "b/src/main/java/New App.java"
				""";

		var files = diffParserService.parse(diffText);

		assertThat(files).hasSize(1);
		assertThat(files.get(0).filePath()).isEqualTo("src/main/java/New App.java");
		assertThat(files.get(0).changeType()).isEqualTo(FileChangeType.MODIFIED);
	}
}
