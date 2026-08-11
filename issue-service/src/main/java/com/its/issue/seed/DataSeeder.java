package com.its.issue.seed;

import com.its.issue.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Loads the reference workbook's two sample issues on an empty table (SRS 10.5).
 *
 * <p>The workbook records {@code created_by = sam.lee} for both rows - a username string
 * in a column the ER diagram types as an integer, referring to a user who does not exist
 * in the workbook's own User table. Following SRS A-17, the column is an integer user id
 * here and the seed maps that value onto Emily Sinha (101), a real role-0 user who could
 * plausibly have raised them.
 *
 * <p>Assignees 104 and 102 are taken verbatim; both are role-1 users in {@code user_db},
 * so the seeded data satisfies the assignee role check if those rows are ever revalidated.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO issue (issue_id, summary, description, project_id, assignee_id,
                               created_by, status, priority, type, story_points, sprint,
                               tags, created_on, last_updated_on)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String CREATED_ON = "2025-09-18 09:00:00";

    private final IssueRepository issueRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(IssueRepository issueRepository, JdbcTemplate jdbcTemplate) {
        this.issueRepository = issueRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (issueRepository.count() > 0) {
            log.debug("Issues already present - skipping seed");
            return;
        }

        jdbcTemplate.update(INSERT, 1,
                "Profile cache not updating after changes",
                "Profile update fails to cache changes, causing outdated information "
                        + "to display for users.",
                1011, 104, 101, "TO_DO", "HIGH", "BUG", 2, "Sprint 42",
                "profile,cache,update", CREATED_ON, CREATED_ON);

        jdbcTemplate.update(INSERT, 2,
                "Notifications API failure",
                "API integration for the notifications module is intermittently failing, "
                        + "resulting in missed alerts for users.",
                1012, 102, 101, "TO_DO", "HIGH", "BUG", 2, "Sprint 42",
                "notifications,api,alerts", CREATED_ON, CREATED_ON);

        jdbcTemplate.execute("ALTER TABLE issue AUTO_INCREMENT = 3");

        log.info("Seeded 2 reference issues (ids 1-2); next id is 3");
    }
}
