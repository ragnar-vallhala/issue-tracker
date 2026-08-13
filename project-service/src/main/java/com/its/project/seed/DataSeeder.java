package com.its.project.seed;

import com.its.project.repository.ProjectRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * in {@code user_db}, satisfying FR-PRJ-02 if those rows are ever revalidated.
 *
 * <p>Beyond the workbook the set is built in layers - 1014-1018 give the owner dashboards
 * a real portfolio, 1019-1022 are edge-case fixtures, 1023-1024 hold the imported bulk,
 * and 1025 upwards are generated. Two properties are worth preserving when editing:
 *
 * <ul>
 *   <li>Projects 1013 and 1022 keep no issues, as in the workbook. They are the fixtures
 *       for empty-state screens and for a cascade delete against a childless project.
 *   <li>The bulk projects are owned by one account (120) rather than spread across all of
 *       them. The owner dashboard fetches every issue of every project it owns, so
 *       concentrating volume keeps that one account slow on purpose and every other
 *       dashboard quick.
 * </ul>
 *
 * <p>The date ranges deliberately vary - some finished, some running, one not yet started,
 * and several with no end date at all.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO project (project_id, project_name, project_owner_id, start_date, end_date)
            VALUES (?, ?, ?, ?, ?)
            """;

    /** First generated id. Everything below this is hand-written above. */
    static final int FIRST_GENERATED_ID = 1025;

    /** How many projects to generate on top of the hand-written set. */
    static final int GENERATED_COUNT = 20;

    /**
     * Mirrors the User Service's seeder: its generated users start at 121, run for 50,
     * and every seventh is a Project Owner. Repeated rather than shared because the
     * services deliberately have no common module (DESIGN 3) - the cost of that choice is
     * exactly this kind of duplication, and the constraint it protects is FR-PRJ-02,
     * every project_owner_id resolving to a role-0 user.
     */
    private static final int FIRST_GENERATED_USER_ID = 121;
    private static final int GENERATED_USER_COUNT = 50;
    private static final int OWNER_EVERY = 7;

    /** The owner of the two bulk archive projects (user 120). */
    private static final int ARCHIVE_OWNER_ID = 120;

    record SeedProject(int id, String name, int ownerId, String start, String end) {
    }

    private static final List<SeedProject> CURATED = List.of(
            // --- the reference workbook's three rows ---------------------------------
            new SeedProject(1011, "Profile Management", 101, "2025-09-18", "2025-12-18"),
            new SeedProject(1012, "Notifications Platform", 103, "2025-10-01", "2026-01-15"),
            new SeedProject(1013, "User Analytics", 101, "2025-09-25", "2025-12-10"),

            // --- additional projects, spread across the three owners -----------------
            new SeedProject(1014, "Mobile Checkout", 107, "2025-11-03", "2026-03-27"),
            new SeedProject(1015, "Billing Migration", 103, "2025-08-11", "2026-02-06"),
            new SeedProject(1016, "Search Relevance", 101, "2026-01-12", "2026-06-30"),
            new SeedProject(1017, "Design System", 107, "2025-10-20", "2026-04-17"),
            new SeedProject(1018, "Platform Hardening", 103, "2026-02-02", null),

            // --- edge cases ----------------------------------------------------------
            // A name close to the 255-character column limit. Project names are shown in
            // breadcrumbs, table cells and the filter dropdown, and each one truncates
            // differently - or fails to.
            new SeedProject(1019,
                    "Consolidation of the legacy reporting stack into the unified analytics "
                            + "platform, including the retirement of the nightly export jobs "
                            + "and the migration of every saved report to the new query "
                            + "service ahead of the datacentre exit",
                    103, "2025-07-14", "2026-09-30"),
            // Non-Latin characters in a name that also has to round-trip through a URL
            // and a form field.
            new SeedProject(1020, "検索基盤 / Recherche Européenne", 107, "2025-12-01", "2026-08-14"),
            // Every issue on this project is DONE: the fixture for a completed project,
            // where the status meter is a single full bar and the "open work" panels are
            // all empty even though the project itself is not.
            new SeedProject(1021, "Cookie Consent Rollout", 107, "2025-06-02", "2025-11-28"),
            // No issues, no end date, and it has not started yet - the second empty-state
            // fixture, and the one that catches code assuming start_date is in the past.
            new SeedProject(1022, "Warehouse Replatform", 101, "2026-11-02", null),

            // --- bulk, owned by 120 so the other dashboards stay quick ---------------
            new SeedProject(1023, "Legacy Backlog Import 2019-2024", ARCHIVE_OWNER_ID,
                    "2025-01-06", null),
            new SeedProject(1024, "Support Queue Archive", ARCHIVE_OWNER_ID,
                    "2025-03-17", null));

    private static final String[] THEMES = {
            "Customer Portal", "Partner API", "Inventory Sync", "Fraud Signals",
            "Content Delivery", "Identity Federation", "Warehouse Automation",
            "Pricing Engine", "Loyalty Programme", "Field Operations"};

    /**
     * Owner ids for the generated projects, derived from the User Service's rule rather
     * than listed, so the two seeders cannot drift into referencing a user who is not an
     * owner - or does not exist.
     */
    private static List<Integer> generatedOwnerIds() {
        List<Integer> owners = new ArrayList<>();
        for (int i = 0; i < GENERATED_USER_COUNT; i++) {
            if (i % OWNER_EVERY == 0) {
                owners.add(FIRST_GENERATED_USER_ID + i);
            }
        }
        return owners;
    }

    /**
     * Volume behind the hand-written portfolio. Deterministic by construction - index
     * arithmetic, no randomness - so two clean checkouts seed identical databases.
     *
     * <p>Names pair a theme with a phase number, which is what keeps them unique: the
     * column has a UNIQUE constraint and a duplicate would fail the whole seed.
     */
    private static List<SeedProject> generatedProjects() {
        List<Integer> owners = generatedOwnerIds();
        List<SeedProject> projects = new ArrayList<>(GENERATED_COUNT);
        LocalDate base = LocalDate.of(2025, 2, 3);

        for (int i = 0; i < GENERATED_COUNT; i++) {
            LocalDate start = base.plusDays(i * 11L);
            // Every third project is left open-ended, so the "no end date" path is
            // exercised by more than the one hand-written row.
            String end = i % 3 == 0 ? null : start.plusDays(120L + (i % 5) * 45L).toString();

            projects.add(new SeedProject(
                    FIRST_GENERATED_ID + i,
                    THEMES[i % THEMES.length] + " Phase " + (i / THEMES.length + 1),
                    owners.get(i % owners.size()),
                    start.toString(),
                    end));
        }
        return projects;
    }

    static final List<SeedProject> PROJECTS = buildProjects();

    private static List<SeedProject> buildProjects() {
        List<SeedProject> projects = new ArrayList<>(CURATED);
        projects.addAll(generatedProjects());
        return List.copyOf(projects);
    }

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

        jdbcTemplate.batchUpdate(INSERT, PROJECTS.stream()
                .map(project -> new Object[]{
                        project.id(), project.name(), project.ownerId(),
                        project.start(), project.end()})
                .toList());

        int nextId = PROJECTS.get(PROJECTS.size() - 1).id() + 1;
        jdbcTemplate.execute("ALTER TABLE project AUTO_INCREMENT = " + nextId);

        log.info("Seeded {} projects (ids 1011-{}); next id is {}",
                PROJECTS.size(), nextId - 1, nextId);
    }
}
