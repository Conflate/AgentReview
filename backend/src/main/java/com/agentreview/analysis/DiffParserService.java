package com.agentreview.analysis;

import com.agentreview.analysis.dto.ParsedChangedFile;
import com.agentreview.common.FileChangeType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DiffParserService {

	private static final Pattern DIFF_HEADER_PATTERN = Pattern.compile("^diff --git (\"(?:\\\\.|[^\"])+\"|\\S+) (\"(?:\\\\.|[^\"])+\"|\\S+)$");

	public List<ParsedChangedFile> parse(String diffText) {
		List<ParsedChangedFile> files = new ArrayList<>();
		ParsedFile currentFile = null;
		for (String line : diffText.split("\\R")) {
			if (line.startsWith("diff --git ")) {
				if (currentFile != null) {
					files.add(currentFile.toParsedChangedFile());
				}
				currentFile = new ParsedFile(extractPathFromDiffHeader(line), FileChangeType.MODIFIED);
			} else if (currentFile != null && line.startsWith("new file mode")) {
				currentFile.changeType = FileChangeType.ADDED;
			} else if (currentFile != null && line.startsWith("deleted file mode")) {
				currentFile.changeType = FileChangeType.DELETED;
			} else if (currentFile != null && line.startsWith("rename to ")) {
				currentFile.filePath = stripGitPrefix(line.substring("rename to ".length()).trim());
				currentFile.changeType = FileChangeType.RENAMED;
			} else if (currentFile != null && line.startsWith("rename from ")) {
				currentFile.changeType = FileChangeType.RENAMED;
			}
		}
		if (currentFile != null) {
			files.add(currentFile.toParsedChangedFile());
		}
		return files.stream()
				.filter(file -> !file.filePath().isBlank())
				.toList();
	}

	private String extractPathFromDiffHeader(String line) {
		Matcher matcher = DIFF_HEADER_PATTERN.matcher(line);
		if (!matcher.matches()) {
			return "";
		}
		return stripGitPrefix(matcher.group(2));
	}

	private String stripGitPrefix(String value) {
		String path = value;
		if (path.startsWith("\"") && path.endsWith("\"") && path.length() > 1) {
			path = path.substring(1, path.length() - 1);
			path = path.replace("\\\"", "\"").replace("\\\\", "\\");
		}
		if (path.startsWith("b/") || path.startsWith("a/")) {
			return path.substring(2);
		}
		return path;
	}

	private static class ParsedFile {
		private String filePath;
		private FileChangeType changeType;

		private ParsedFile(String filePath, FileChangeType changeType) {
			this.filePath = filePath;
			this.changeType = changeType;
		}

		private ParsedChangedFile toParsedChangedFile() {
			return new ParsedChangedFile(filePath, changeType);
		}
	}
}
