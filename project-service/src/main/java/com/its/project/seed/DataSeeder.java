package com.its.project.seed;

import com.its.project.repository.ProjectRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Loads the development dataset on an empty table (SRS 10.5).
 *
 * <p>Projects 1011-1013 and their owner ids come straight from the reference workbook, so
 * the seeded data lines up across services: every owner id here resolves to a role-0 user
 * in {@code user_db}, satisfying FR-PRJ-02 if those rows are ever revalidated. The
 * remainder extend the set so the owner dashboards have a real portfolio to show.
 *
 * <p>Project 1013 deliberately keeps no issues, as in the workbook: it is the fixture for
 * empty-state screens and for exercising the cascade delete against a childless project.
 * The date ranges deliberately vary - some finished, some running, one not yet started.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO project (project_id, project_name, project_owner_id, start_date, end_date)
            VALUES (?, ?, ?, ?, ?)
            """;

    private record SeedProject(int id, String name, int ownerId, String start, String end) {
    }

    private static final List<SeedProject> PROJECTS = List.of(
            // --- the reference workbook's three rows ---------------------------------
            new SeedProject(1011, "Profile Management", 101, "2025-09-18", "2025-12-18"),
            new SeedProject(1012, "Notifications Platform", 103, "2025-10-01", "2026-01-15"),
            new SeedProject(1013, "User Analytics", 101, "2025-09-25", "2025-12-10"),

            // --- additional projects, spread across the three owners -----------------
            new SeedProject(1014, "Mobile Checkout", 107, "2025-11-03", "2026-03-27"),
            new SeedProject(1015, "Billing Migration", 103, "2025-08-11", "2026-02-06"),
            new SeedProject(1016, "Search Relevance", 101, "2026-01-12", "2026-06-30"),
            new SeedProject(1017, "Design System", 107, "2025-10-20", "2026-04-17"),
            new SeedProject(1018, "Platform Hardening", 103, "2026-02-02", null));

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

        for (SeedProject project : PROJECTS) {
            jdbcTemplate.update(INSERT, project.id(), project.name(),
                    project.ownerId(), project.start(), project.end());
        }

        int nextId = PROJECTS.get(PROJECTS.size() - 1).id() + 1;
        jdbcTemplate.execute("ALTER TABLE project AUTO_INCREMENT = " + nextId);

        log.info("Seeded {} projects (ids 1011-{}); next id is {}",
                PROJECTS.size(), nextId - 1, nextId);
    }
}
