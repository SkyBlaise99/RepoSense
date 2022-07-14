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
    private static RepoConfiguration config;

    @BeforeAll
    public static void beforeAll() throws Exception {
        config = beforeClass();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        config = super.before();
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
