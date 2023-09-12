package reposense.authorship.model;

/**
 * Stores the information of a candidate line used in {@code AuthorshipAnalyzer}.
 */
public class CandidateLine {
    private final int lineNumber;
    private final String lineContent;
    private final String filePath;
    private final String gitBlameCommitHash;
    private final double similarityScore;

    public CandidateLine(int lineNumber, String lineContent, String filePath, String gitBlameCommitHash,
            double similarityScore) {
        this.lineNumber = lineNumber;
        this.lineContent = lineContent;
        this.filePath = filePath;
        this.gitBlameCommitHash = gitBlameCommitHash;
        this.similarityScore = similarityScore;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLineContent() {
        return lineContent;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getGitBlameCommitHash() {
        return gitBlameCommitHash;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CandidateLine)) {
            return false;
        }

        CandidateLine candidateLine = (CandidateLine) o;

        return lineNumber == candidateLine.lineNumber
                && lineContent.equals(candidateLine.lineContent)
                && filePath.equals(candidateLine.filePath)
                && gitBlameCommitHash.equals(candidateLine.gitBlameCommitHash)
                && Double.compare(similarityScore, candidateLine.similarityScore) == 0;
    }
}
