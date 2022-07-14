package reposense.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.model.RepoConfiguration;
import reposense.template.GitTestTemplate;


public class GitDiffTest extends GitTestTemplate {
    private static final String CLASS_NAME = GitDiffTest.class.getSimpleName();

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
    public void diffCommit_validCommitHash_success() {
        String diffResult = GitDiff.diffCommit(config.getRepoRoot(),
                FAKE_AUTHOR_BLAME_TEST_FILE_COMMIT_08022018.toString());
        Assertions.assertFalse(diffResult.isEmpty());
    }

    @Test
    public void diffCommit_emptyCommitHash_emptyResult() {
        String diffResult = GitDiff.diffCommit(config.getRepoRoot(), LATEST_COMMIT_HASH);
        Assertions.assertTrue(diffResult.isEmpty());
    }

    @Test
    public void diffCommit_latestCommitHash_emptyResult() {
        String diffResult = GitDiff.diffCommit(config.getRepoRoot(), "");
        Assertions.assertTrue(diffResult.isEmpty());
    }

    @Test
    public void diffCommit_nonexistentCommitHash_throwsRunTimeException() {
        Assertions.assertThrows(RuntimeException.class, () -> GitDiff.diffCommit(config.getRepoRoot(),
                NONEXISTENT_COMMIT_HASH));
    }

    @Test
    public void diffCommit_commitContainingSubmodule_ignoresSubmodule() {
        GitCheckout.checkout(config.getRepoRoot(),
                "850-GitDiffTest-diffCommit_commitContainingSubmodule_ignoresSubmodule");
        String diffResult = GitDiff.diffCommit(config.getRepoRoot(), EMPTY_TREE_HASH);
        Assertions.assertFalse(diffResult.contains("Subproject commit"));
    }
}
