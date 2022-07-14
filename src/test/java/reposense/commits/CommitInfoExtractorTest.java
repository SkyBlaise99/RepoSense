package reposense.commits;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.commits.model.CommitInfo;
import reposense.model.RepoConfiguration;
import reposense.template.GitTestTemplate;
import reposense.util.TestUtil;

public class CommitInfoExtractorTest extends GitTestTemplate {
    private static final String CLASS_NAME = CommitInfoExtractorTest.class.getSimpleName();

    private static RepoConfiguration config;

    @BeforeAll
    public static void beforeAll() throws Exception {
        config = beforeClass(CLASS_NAME);
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        config = super.before(CLASS_NAME);
    }

    @AfterEach
    public void afterEach() {
        super.after(config);
    }

    @Test
    public void withContentTest() {
        List<CommitInfo> commits = CommitInfoExtractor.extractCommitInfos(config);
        Assertions.assertFalse(commits.isEmpty());
    }

    @Test
    public void withoutContentTest() {
        LocalDateTime sinceDate = TestUtil.getSinceDate(2050, Month.JANUARY.getValue(), 1);
        config.setSinceDate(sinceDate);

        List<CommitInfo> commits = CommitInfoExtractor.extractCommitInfos(config);
        Assertions.assertTrue(commits.isEmpty());
    }
}
