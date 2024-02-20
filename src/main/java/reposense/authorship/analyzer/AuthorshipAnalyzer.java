package reposense.authorship.analyzer;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reposense.authorship.model.CandidateLine;
import reposense.authorship.model.GitBlameLineInfo;
import reposense.git.GitBlame;
import reposense.git.GitDiff;
import reposense.git.GitLog;
import reposense.model.Author;
import reposense.model.CommitHash;
import reposense.model.RepoConfiguration;
import reposense.system.LogsManager;
import reposense.util.StringsUtil;

/**
 * Analyzes a line to find out if the author should be assigned partial or full credit.
 */
public class AuthorshipAnalyzer {
    private static final Logger logger = LogsManager.getLogger(AuthorshipAnalyzer.class);

    private static final double ORIGINALITY_THRESHOLD = 0.51;

    private static final String DIFF_FILE_CHUNK_SEPARATOR = "\ndiff --git a/.*\n";
    private static final Pattern FILE_CHANGED_PATTERN =
            Pattern.compile("\n(-){3} a?/(?<preImageFilePath>.*)\n(\\+){3} b?/(?<postImageFilePath>.*)\n");
    private static final String PRE_IMAGE_FILE_PATH_GROUP_NAME = "preImageFilePath";
    private static final String POST_IMAGE_FILE_PATH_GROUP_NAME = "postImageFilePath";
    private static final String FILE_ADDED_SYMBOL = "dev/null";
    private static final String HUNK_SEPARATOR = "\n@@ ";
    private static final int LINES_CHANGED_HEADER_INDEX = 0;
    private static final Pattern STARTING_LINE_NUMBER_PATTERN =
            Pattern.compile("-(?<preImageStartLine>\\d+),?\\d* \\+\\d+,?\\d* @@");
    private static final String PREIMAGE_START_LINE_GROUP_NAME = "preImageStartLine";
    private static final String MATCH_GROUP_FAIL_MESSAGE_FORMAT = "Failed to match the %s group for:\n%s";
    private static final int AUTHOR_NAME_OFFSET = "author ".length();
    private static final int AUTHOR_EMAIL_OFFSET = "author-mail ".length();
    private static final int FULL_COMMIT_HASH_LENGTH = 40;
    private static final int COMMIT_TIME_OFFSET = "committer-time ".length();
    private static final String ADDED_LINE_SYMBOL = "+";
    private static final String DELETED_LINE_SYMBOL = "-";

    /**
     * Analyzes the authorship of {@code lineContent} in {@code filePath}.
     * Returns {@code true} if {@code currentAuthor} should be assigned full credit, {@code false} otherwise.
     */
    public static boolean analyzeAuthorship(RepoConfiguration config, String filePath, String lineContent,
            String commitHash, Author currentAuthor) {
        // Empty lines are ignored and given full credit
        if (lineContent.isEmpty()) {
            return true;
        }

        CandidateLine deletedLine = getDeletedLineWithLowestOriginality(config, filePath, lineContent, commitHash);

        // Give full credit if there are no deleted lines found or deleted line is more than originality threshold
        if (deletedLine == null || deletedLine.getOriginalityScore() > ORIGINALITY_THRESHOLD) {
            return true;
        }

        GitBlameLineInfo deletedLineInfo = getGitBlameLineInfo(config, deletedLine);
        long sinceDateInMilliseconds = ZonedDateTime.of(config.getSinceDate(), config.getZoneId()).toEpochSecond();

        // Give full credit if author is unknown, is before since date, is in ignored list, or is an ignored file
        if (deletedLineInfo.getAuthor().equals(Author.UNKNOWN_AUTHOR)
                || deletedLineInfo.getTimestampMilliseconds() < sinceDateInMilliseconds
                || CommitHash.isInsideCommitList(deletedLineInfo.getCommitHash(), config.getIgnoreCommitList())
                || deletedLineInfo.getAuthor().isIgnoringFile(Paths.get(deletedLine.getFilePath()))) {
            return true;
        }

        // Give partial credit if currentAuthor is not the author of the previous version
        if (!currentAuthor.equals(deletedLineInfo.getAuthor())) {
            return false;
        }

        // Check the previous version as currentAuthor is the same as author of the previous version
        return analyzeAuthorship(config, deletedLine.getFilePath(), deletedLine.getLineContent(),
                deletedLineInfo.getCommitHash(), deletedLineInfo.getAuthor());
    }

    /**
     * Returns the deleted line in {@code commitHash} that has the lowest originality with {@code lineContent}.
     */
    private static CandidateLine getDeletedLineWithLowestOriginality(RepoConfiguration config, String filePath,
            String lineContent, String commitHash) {
        String gitLogResults = GitLog.getParentCommits(config.getRepoRoot(), commitHash);
        String[] parentCommits = gitLogResults.split(" ");

        CandidateLine lowestOriginalityLine = null;

        for (String parentCommit : parentCommits) {
            // Generate diff between commit and parent commit
            String gitDiffResult = GitDiff.diffCommits(config.getRepoRoot(), parentCommit, commitHash);
            String[] fileDiffResultList = gitDiffResult.split(DIFF_FILE_CHUNK_SEPARATOR);

            for (String fileDiffResult : fileDiffResultList) {
                Matcher filePathMatcher = FILE_CHANGED_PATTERN.matcher(fileDiffResult);
                if (!filePathMatcher.find()) {
                    continue;
                }

                String preImageFilePath = filePathMatcher.group(PRE_IMAGE_FILE_PATH_GROUP_NAME);
                String postImageFilePath = filePathMatcher.group(POST_IMAGE_FILE_PATH_GROUP_NAME);

                // If file was added in the commit or file name does not match
                if (preImageFilePath.equals(FILE_ADDED_SYMBOL) || !postImageFilePath.equals(filePath)) {
                    continue;
                }

                CandidateLine candidateLine = getDeletedLineWithLowestOriginalityInDiff(
                        fileDiffResult, lineContent, parentCommit, preImageFilePath);
                if (candidateLine == null) {
                    continue;
                }

                if (lowestOriginalityLine == null
                        || candidateLine.getOriginalityScore() < lowestOriginalityLine.getOriginalityScore()) {
                    lowestOriginalityLine = candidateLine;
                }
            }
        }

        return lowestOriginalityLine;
    }

    /**
     * Returns the deleted line in {@code fileDiffResult} that has the lowest originality with {@code lineContent}.
     */
    private static CandidateLine getDeletedLineWithLowestOriginalityInDiff(String fileDiffResult, String lineContent,
            String commitHash, String filePath) {
        CandidateLine lowestOriginalityLine = null;

        String[] hunks = fileDiffResult.split(HUNK_SEPARATOR);

        // skip the diff header, index starts from 1
        for (int index = 1; index < hunks.length; index++) {
            String hunk = hunks[index];

            // skip hunk if lines added in the hunk does not include lineContent
            if (!hunk.contains(ADDED_LINE_SYMBOL + lineContent)) {
                continue;
            }

            String[] linesChanged = hunk.split("\n");
            int currentPreImageLineNumber = getPreImageStartingLineNumber(linesChanged[LINES_CHANGED_HEADER_INDEX]);

            // skip the lines changed header, index starts from 1
            for (int lineIndex = 1; lineIndex < linesChanged.length; lineIndex++) {
                String lineChanged = linesChanged[lineIndex];

                if (lineChanged.startsWith(DELETED_LINE_SYMBOL)) {
                    String deletedLineContent = lineChanged.substring(DELETED_LINE_SYMBOL.length());
                    double originalityScore = computeOriginalityScore(lineContent, deletedLineContent);

                    if (lowestOriginalityLine == null
                            || originalityScore < lowestOriginalityLine.getOriginalityScore()) {
                        lowestOriginalityLine = new CandidateLine(
                                currentPreImageLineNumber, deletedLineContent, filePath, commitHash,
                                originalityScore);
                    }
                }

                if (!lineChanged.startsWith(ADDED_LINE_SYMBOL)) {
                    currentPreImageLineNumber++;
                }
            }
        }

        return lowestOriginalityLine;
    }

    /**
     * Returns the pre-image starting line number by matching the pattern inside {@code linesChangedHeader}.
     *
     * @throws AssertionError if lines changed header matcher failed to find anything.
     */
    private static int getPreImageStartingLineNumber(String linesChangedHeader) {
        Matcher linesChangedHeaderMatcher = STARTING_LINE_NUMBER_PATTERN.matcher(linesChangedHeader);

        if (!linesChangedHeaderMatcher.find()) {
            logger.severe(
                    String.format(MATCH_GROUP_FAIL_MESSAGE_FORMAT, PREIMAGE_START_LINE_GROUP_NAME, linesChangedHeader));
            throw new AssertionError(
                    "Should not have error matching line number pattern inside lines changed header!");
        }

        return Integer.parseInt(linesChangedHeaderMatcher.group(PREIMAGE_START_LINE_GROUP_NAME));
    }

    /**
     * Calculates the originality score of {@code s} with {@code baseString}.
     */
    private static double computeOriginalityScore(String s, String baseString) {
        double levenshteinDistance = StringsUtil.getLevenshteinDistance(s, baseString);
        return levenshteinDistance / baseString.length();
    }

    /**
     * Returns the git blame line info for {@code line}.
     */
    private static GitBlameLineInfo getGitBlameLineInfo(RepoConfiguration config, CandidateLine line) {
        String blameResults = GitBlame.blameLine(
                config.getRepoRoot(), line.getGitBlameCommitHash(), line.getFilePath(), line.getLineNumber());
        String[] blameResultLines = blameResults.split("\n");

        String commitHash = blameResultLines[0].substring(0, FULL_COMMIT_HASH_LENGTH);
        String authorName = blameResultLines[1].substring(AUTHOR_NAME_OFFSET);
        String authorEmail = blameResultLines[2].substring(AUTHOR_EMAIL_OFFSET).replaceAll("[<>]", "");
        long timestampMilliseconds = Long.parseLong(blameResultLines[5].substring(COMMIT_TIME_OFFSET));
        Author author = config.getAuthor(authorName, authorEmail);

        return new GitBlameLineInfo(commitHash, author, timestampMilliseconds);
    }

    public static void main(String[] args) {
        String[] testTitles = {
                "Java Statement", "Java Method", "Java Comment Line", "Markdown Line", "CSS Line"
        };
        /*
            Original
            a trivial edit
            a non-trivial edit but not enough to give full credit
            an edit just enough to give full credit
            an edit significantly bigger than the bar
         */
        String[] stringTitles = {
                "Original Version", "Trivial Edit\t", "Borderline Edit", "Full Credit Edit", "Significantly Edit"
        };

        String[][] testcases = new String[][] {
                new String[] { // java statement
                        "int limit = 0;",
                        "int limit = 10;",
                        "int depthLimit = 10;",
                        "int maxDepthLimit = 10;",
                        "int maxDepthLimit = getXXX() * getYYY();",
                },
                new String[] { // java method
                        "public void printMessage() { System.out.print(\"Hello\"); }",
                        "private void printMessage() { System.out.println(\"Hello World!\"); }",
                        "private void printMessage(String message) { System.out.println(message); }",
                        "private static void logInfo(String info) { logger.info(info); }",
                        "private static void logInfo(Level level, String message) { logger.log(level, message); }",
                },
                new String[] { // java comment line
                        "// Returns the line with the lowest score.",
                        "/** Returns the line with the lowest score. */",
                        "// Returns the deleted line with the lowest originality score.",
                        "/** Returns the deleted line with the lowest originality score. */",
                        "/** Returns the deleted line in {@code fileDiffResult} that has the lowest originality with {@code lineContent}. */",
                },
                new String[] { // markdown line
                        "The runtime decrease from 1.5h to 15 min.",
                        "The runtime **_decrease_** from 1.5h to 15 min.",
                        "The runtime **_decrease_** from `1.5` hour to `15` minute.",
                        "The runtime **_decrease_** from `1.5` hour to `15` minute by adding caching.",
                        "The runtime **_decrease_** from `1.5` hour to `15` minute by caching `git log` and `git diff` results.",
                },
                new String[] { // css line
                        "body { font-size: 16px; }",
                        "body { font-size: 20px; }",
                        "body { font-family: Arial; }",
                        "body { background-color: blue; }",
                        "body { font-size: 20px; font-family: Arial; background-color: blue; }",
                },
        };

        for (int j = 0; j < testTitles.length; j++) {
            System.out.println("========== " + testTitles[j]);

            String[] testcase = testcases[j];
            for (int i = 0; i < stringTitles.length; i++) {
                System.out.println(stringTitles[i] + "\t\t" + testcase[i]);

                if (i > 0) {
                    double originalityScore = computeOriginalityScore(testcase[i], testcase[0]);
                    System.out.println("Originality Score: " + String.format("%.2f", originalityScore));
                }
            }

            System.out.println();
        }
    }

    public static void main2(String[] args) {
    /*
    public static int factorial1(int n) {
        if (n < 0) {
            throw new RuntimeException("Factorial is not defined for negative numbers");
        }

        int result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }

        return result;
    }

    public static int factorial2(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers");
        }

        return IntStream.rangeClosed(1, n).reduce(1, (a, b) -> a * b);
    }
     */

        String[] addedStrings = new String[] {
                "    throw new RuntimeException(\"Factorial is not defined for negative numbers\");",
                "return IntStream.rangeClosed(1, n).reduce(1, (a, b) -> a * b);"
        };

        String[] deletedStrings = new String[] {
                "    throw new IllegalArgumentException(\"Factorial is not defined for negative numbers\");",
                "int result = 1;",
                "for (int i = 1; i <= n; i++) {",
                "    result *= i;",
                "}",
                "return result;"
        };

        for (String addedString : addedStrings) {
            double minOriginalityScore = Integer.MAX_VALUE;
            String mostSimilarDeletedString = "";

            for (String deletedString : deletedStrings) {
                double originalityScore = computeOriginalityScore(addedString, deletedString);

                if (originalityScore < minOriginalityScore) {
                    minOriginalityScore = originalityScore;
                    mostSimilarDeletedString = deletedString;
                }
            }

            System.out.println("Added: " + addedString);
            System.out.println("Deleted: " + mostSimilarDeletedString);
            System.out.println("Originality Score: " + minOriginalityScore);
        }
    }
}
