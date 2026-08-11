package com.its.project.seed;

import com.its.project.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Loads the reference workbook's sample projects on an empty table (SRS 10.5).
 *
 * <p>Ids 1011-1013 and owner ids 101/103 come straight from the workbook, so the seeded
 * data across the three services lines up: both owners are role-0 users in
 * {@code user_db}, satisfying FR-PRJ-02 if those rows are ever re-validated.
 *
 * <p>Project 1013 deliberately has no issues in the workbook, which makes it the fixture
 * for empty-state screens and for exercising the cascade delete against a childless
 * project.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO project (project_id, project_name, project_owner_id, start_date, end_date)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final ProjectRepository projectRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(ProjectRepository projectRepository, JdbcTemplate jdbcTemplate) {
        this.projectRepository = projectRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (projectRepository.count() > 0) {
            log.debug("Projects already present - skipping seed");
            return;
        }

        jdbcTemplate.update(INSERT, 1011, "Profile Management", 101, "2025-09-18", "2025-12-18");
        jdbcTemplate.update(INSERT, 1012, "Notifications Platform", 103, "2025-10-01", "2026-01-15");
        jdbcTemplate.update(INSERT, 1013, "User Analytics", 101, "2025-09-25", "2025-12-10");

        jdbcTemplate.execute("ALTER TABLE project AUTO_INCREMENT = 1014");

        log.info("Seeded 3 reference projects (ids 1011-1013); next id is 1014");
    }
}
