package reposense.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.model.FileTypeTest;
import reposense.model.RepoConfiguration;
import reposense.model.RepoLocation;
import reposense.template.GitTestTemplate;
import reposense.util.TestRepoCloner;

public class GitBranchTest extends GitTestTemplate {
    protected static final String TEST_REPO_UNCOMMON_DEFAULT_GIT_LOCATION =
            "https://github.com/reposense/testrepo-UncommonDefaultBranch.git";


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
    public void getCurrentBranch_masterBranch_success() throws Exception {
        String currentBranch = GitBranch.getCurrentBranch(config.getRepoRoot());
        Assertions.assertEquals("master", currentBranch);
    }

    @Test
    public void getCurrentBranch_uncommonDefaultBranch_success() throws Exception {
        RepoConfiguration uncommonDefaultConfig = new RepoConfiguration(
                new RepoLocation(TEST_REPO_UNCOMMON_DEFAULT_GIT_LOCATION), RepoConfiguration.DEFAULT_BRANCH);
        uncommonDefaultConfig.setFormats(FileTypeTest.DEFAULT_TEST_FORMATS);
        TestRepoCloner.cloneAndBranch(uncommonDefaultConfig);
        String currentBranch = GitBranch.getCurrentBranch(uncommonDefaultConfig.getRepoRoot());
        Assertions.assertEquals("uncommon", currentBranch);
    }
}
