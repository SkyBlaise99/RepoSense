package reposense.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reposense.model.RepoConfiguration;
import reposense.template.GitTestTemplate;

public class GitCloneTest extends GitTestTemplate {
    private static final String EXTRA_OUTPUT_FOLDER_NAME = GitCloneTest.class.getSimpleName();
    private static RepoConfiguration config;

    @BeforeAll
    public static void beforeClass() throws Exception {
        config = GitTestTemplate.beforeClass(EXTRA_OUTPUT_FOLDER_NAME);
    }

    @BeforeEach
    public void before() throws Exception {
        config = super.before(EXTRA_OUTPUT_FOLDER_NAME);
    }

    @AfterEach
    public void after() {
        super.after(config);
    }

    @Test
    public void cloneTest_validRepo_success() {
        // As the clone has been performed in the {@code GitTestTemplate},
        // this checks whether the clone has been executed successfully by performing a file system check.
        Path dir = Paths.get(config.getRepoRoot());
        Assertions.assertTrue(Files.exists(dir));
    }
}
