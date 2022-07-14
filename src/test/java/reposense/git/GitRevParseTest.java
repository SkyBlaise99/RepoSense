package reposense.git;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.git.exception.GitBranchException;
import reposense.model.RepoConfiguration;
import reposense.template.GitTestTemplate;

public class GitRevParseTest extends GitTestTemplate {
    private static final String CLASS_NAME = GitRevParseTest.class.getSimpleName();

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
    public void assertBranchExists_withExistingBranch_success() throws Exception {
        config.setBranch("master");
        GitRevParse.assertBranchExists(config, Paths.get(config.getRepoRoot()));
    }

    @Test
    public void assertBranchExists_withNonExistentBranch_throwsGitBranchException() {
        config.setBranch("nonExistentBranch");
        Assertions.assertThrows(GitBranchException.class, () -> GitRevParse.assertBranchExists(config,
                Paths.get(config.getRepoRoot())));
    }
}
