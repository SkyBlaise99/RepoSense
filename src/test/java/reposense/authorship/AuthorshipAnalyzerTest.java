package reposense.authorship;

import static reposense.parser.ArgsParser.DEFAULT_ORIGINALITY_THRESHOLD;

import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.authorship.model.FileInfo;
import reposense.git.GitCheckout;
import reposense.model.Author;
import reposense.model.RepoConfiguration;
import reposense.template.GitTestTemplate;
import reposense.util.TestUtil;

public class AuthorshipAnalyzerTest extends GitTestTemplate {
    private static final LocalDateTime SINCE_DATE = TestUtil.getSinceDate(2024, Month.MARCH.getValue(), 1);
    private static final LocalDateTime UNTIL_DATE = TestUtil.getUntilDate(2024, Month.APRIL.getValue(), 1);
    private static final String TEST_FILENAME_1 = "test1.txt";
    private static final String TEST_FILENAME_2 = "test2.txt";
    private static final String BRANCH_NAME = "highest-contributing-author";
    private static final Author SHICHEN_AUTHOR = new Author(SHICHEN_AUTHOR_NAME);
    private static final Author SECOND_AUTHOR = new Author(SHICHEN2_AUTHOR_NAME);

    private RepoConfiguration config;

    @BeforeEach
    public void before() throws Exception {
        super.before();

        config = configs.get();

        config.setBranch(BRANCH_NAME);
        GitCheckout.checkoutBranch(config.getRepoRoot(), config.getBranch());

        config.setSinceDate(SINCE_DATE);
        config.setUntilDate(UNTIL_DATE);
        config.setZoneId(TIME_ZONE_ID);

        config.addAuthorNamesToAuthorMapEntry(SHICHEN_AUTHOR, SHICHEN_AUTHOR_NAME);
        config.addAuthorNamesToAuthorMapEntry(SECOND_AUTHOR, SHICHEN2_AUTHOR_NAME);
    }

    @Test
    public void analyzeAuthorship_noOverwrite_success() {
        FileInfo fileInfo = analyzeTextFile(TEST_FILENAME_1);

        Assertions.assertEquals(SHICHEN_AUTHOR, fileInfo.getLine(1).getAuthor());
        Assertions.assertEquals(SHICHEN_AUTHOR, fileInfo.getLine(2).getAuthor());
        Assertions.assertEquals(SECOND_AUTHOR, fileInfo.getLine(3).getAuthor());
    }

    @Test
    public void analyzeAuthorship_overwriteFakeAuthor_success() {
        FileInfo fileInfo = analyzeTextFile(TEST_FILENAME_2);

        // the first line is a minor edit hence authorship remains on SHICHEN_AUTHOR
        // the second line is a major edit hence authorship goes over to SECOND_AUTHOR
        Assertions.assertEquals(SHICHEN_AUTHOR, fileInfo.getLine(1).getAuthor());
        Assertions.assertEquals(SECOND_AUTHOR, fileInfo.getLine(2).getAuthor());

        // Although line is added by SECOND_AUTHOR, it is similar to first line of SHICHEN_AUTHOR, hence credit goes to
        // SHICHEN_AUTHOR
        Assertions.assertEquals(SHICHEN_AUTHOR, fileInfo.getLine(3).getAuthor());
    }

    private FileInfo analyzeTextFile(String relativePath) {
        FileInfoExtractor fileInfoExtractor = new FileInfoExtractor();
        FileInfo fileInfo = fileInfoExtractor.generateFileInfo(config, relativePath);

        FileInfoAnalyzer fileInfoAnalyzer = new FileInfoAnalyzer();
        fileInfoAnalyzer.analyzeTextFile(config, fileInfo, true, DEFAULT_ORIGINALITY_THRESHOLD);

        return fileInfo;
    }
}
