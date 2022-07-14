package reposense.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.git.exception.CommitNotFoundException;
import reposense.model.RepoConfiguration;
import reposense.template.GitTestTemplate;
import reposense.util.TestUtil;


public class GitCheckoutTest extends GitTestTemplate {
    private static final String CLASS_NAME = GitCheckoutTest.class.getSimpleName();

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
    public void checkout_validBranch_success() {
        GitCheckout.checkout(config.getRepoRoot(), "test");
        Path branchFile = Paths.get(config.getRepoRoot(), "inTestBranch.java");
        Assertions.assertTrue(Files.exists(branchFile));
    }

    @Test
    public void checkoutBranchTest() {
        Path branchFile = Paths.get(config.getRepoRoot(), "inTestBranch.java");
        Assertions.assertFalse(Files.exists(branchFile));

        GitCheckout.checkoutBranch(config.getRepoRoot(), "test");
        Assertions.assertTrue(Files.exists(branchFile));
    }

    @Test
    public void checkoutHashTest() {
        Path newFile = Paths.get(config.getRepoRoot(), "newFile.java");
        Assertions.assertTrue(Files.exists(newFile));

        GitCheckout.checkout(config.getRepoRoot(), FIRST_COMMIT_HASH);
        Assertions.assertFalse(Files.exists(newFile));
    }

    @Test
    public void checkoutToDate_validDate_success() throws Exception {
        Path newFile = Paths.get(config.getRepoRoot(), "newFile.java");
        Assertions.assertTrue(Files.exists(newFile));

        LocalDateTime untilDate = TestUtil.getUntilDate(2018, Month.FEBRUARY.getValue(), 6);
        GitCheckout.checkoutDate(config.getRepoRoot(), config.getBranch(), untilDate, ZoneId.of(config.getZoneId()));
        Assertions.assertFalse(Files.exists(newFile));
    }

    @Test
    public void checkoutToDate_invalidDate_throwsEmptyCommitException() {
        LocalDateTime untilDate = TestUtil.getUntilDate(2015, Month.FEBRUARY.getValue(), 6);
        Assertions.assertThrows(CommitNotFoundException.class, () -> GitCheckout.checkoutDate(config.getRepoRoot(),
                config.getBranch(), untilDate, ZoneId.of(config.getZoneId())));
    }
}
