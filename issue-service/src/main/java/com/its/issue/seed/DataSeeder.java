package com.its.issue.seed;

import com.its.issue.repository.IssueRepository;
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
 * <p>Issues 1 and 2 are the reference workbook's own rows. The workbook records
 * {@code created_by = sam.lee} for both - a username string in a column the ER diagram
 * types as an integer, referring to a user absent from its own User table. Following
 * SRS A-17 the column is an integer user id here, and the seed maps that value onto
 * Emily Sinha (101), a real Project Owner who could plausibly have raised them.
 *
 * <p>The rest of the set exists so the screens have something worth looking at: every
 * status, priority and type is represented, work is spread across seven projects and nine
 * assignees, and the sprint and story-point fields are populated unevenly - some issues
 * are groomed, some are not, which is what a real backlog looks like. Project 1013 is
 * left with no issues on purpose, as the empty-state and cascade-delete fixture.
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

    private record SeedIssue(int id, String summary, String description, int projectId,
                             Integer assigneeId, int createdBy, String status,
                             String priority, String type, Integer points, String sprint,
                             String tags, String created, String updated) {
    }

    private static final List<SeedIssue> ISSUES = List.of(

            // ---- the reference workbook's two rows ---------------------------------
            new SeedIssue(1, "Profile cache not updating after changes",
                    "Profile update fails to cache changes, causing outdated information "
                            + "to display for users.",
                    1011, 104, 101, "TO_DO", "HIGH", "BUG", 2, "Sprint 42",
                    "profile,cache,update", "2025-09-18 09:00:00", "2025-09-18 09:00:00"),
            new SeedIssue(2, "Notifications API failure",
                    "API integration for the notifications module is intermittently "
                            + "failing, resulting in missed alerts for users.",
                    1012, 102, 101, "TO_DO", "HIGH", "BUG", 2, "Sprint 42",
                    "notifications,api,alerts", "2025-09-18 09:05:00", "2025-09-18 09:05:00"),

            // ---- 1011 Profile Management -------------------------------------------
            new SeedIssue(3, "Avatar upload rejects PNG files over 2 MB",
                    "The size guard runs before the format check, so large PNGs are "
                            + "rejected with a misleading 'unsupported format' message.",
                    1011, 112, 101, "IN_PROGRESS", "MEDIUM", "BUG", 3, "Sprint 42",
                    "profile,upload,validation", "2025-09-22 11:20:00", "2025-10-02 16:40:00"),
            new SeedIssue(4, "Add display-name change history",
                    "Keep an audit trail of display-name changes so support can resolve "
                            + "impersonation reports.",
                    1011, 104, 101, "IN_REVIEW", "LOW", "STORY", 5, "Sprint 43",
                    "profile,audit", "2025-09-29 14:02:00", "2025-10-14 09:12:00"),
            new SeedIssue(5, "Profile page first paint over 2 seconds",
                    "Three sequential API calls block first render. They can be issued "
                            + "in parallel.",
                    1011, 102, 101, "DONE", "HIGH", "TASK", 3, "Sprint 41",
                    "profile,performance", "2025-09-19 08:45:00", "2025-10-01 17:30:00"),
            new SeedIssue(6, "Timezone dropdown lists deprecated zones",
                    "The list is built from a bundled tzdata copy that has not been "
                            + "refreshed since 2023.",
                    1011, 112, 101, "TO_DO", "LOW", "BUG", 1, "Sprint 44",
                    "profile,i18n", "2025-10-06 10:15:00", "2025-10-06 10:15:00"),
            new SeedIssue(7, "Email change should require re-verification",
                    "Changing the email address currently takes effect immediately, which "
                            + "lets an attacker with a live session move the account.",
                    1011, 104, 101, "IN_PROGRESS", "CRITICAL", "BUG", 5, "Sprint 43",
                    "profile,security,email", "2025-10-08 09:30:00", "2025-10-21 11:05:00"),
            new SeedIssue(8, "Profile completeness meter",
                    "Show a progress indicator prompting users to finish their profile.",
                    1011, null, 101, "TO_DO", "LOW", "STORY", null, null,
                    "profile,onboarding", "2025-10-11 15:40:00", "2025-10-11 15:40:00"),

            // ---- 1012 Notifications Platform ---------------------------------------
            new SeedIssue(9, "Retry storm when the push provider is degraded",
                    "Failed sends retry immediately and without a cap, which turns a "
                            + "provider blip into an outage of our own making.",
                    1012, 105, 103, "IN_PROGRESS", "CRITICAL", "BUG", 8, "Sprint 42",
                    "notifications,resilience", "2025-10-03 13:25:00", "2025-10-19 10:48:00"),
            new SeedIssue(10, "Per-channel notification preferences",
                    "Let users opt out of email without losing in-app alerts.",
                    1012, 102, 103, "IN_REVIEW", "HIGH", "STORY", 8, "Sprint 43",
                    "notifications,preferences", "2025-10-07 09:10:00", "2025-10-24 14:22:00"),
            new SeedIssue(11, "Digest emails sent at 03:00 in the user's timezone",
                    "The scheduler uses server time rather than the recipient's.",
                    1012, 105, 103, "DONE", "MEDIUM", "BUG", 3, "Sprint 41",
                    "notifications,scheduling,i18n", "2025-10-02 08:00:00", "2025-10-16 12:00:00"),
            new SeedIssue(12, "Template rendering drops non-Latin characters",
                    "The template engine is writing with the platform default charset "
                            + "instead of UTF-8.",
                    1012, 106, 103, "TO_DO", "HIGH", "BUG", 2, "Sprint 44",
                    "notifications,i18n,templates", "2025-10-15 16:30:00", "2025-10-15 16:30:00"),
            new SeedIssue(13, "Delivery receipts dashboard",
                    "Operational view of sends, bounces and failures per channel.",
                    1012, null, 103, "TO_DO", "MEDIUM", "EPIC", 13, null,
                    "notifications,observability", "2025-10-20 11:00:00", "2025-10-20 11:00:00"),
            new SeedIssue(14, "Unsubscribe link 404s for legacy campaigns",
                    "Links minted before the route change point at a path that no longer "
                            + "exists. A redirect would fix every old email at once.",
                    1012, 102, 103, "DONE", "HIGH", "BUG", 2, "Sprint 42",
                    "notifications,email,legacy", "2025-10-09 10:05:00", "2025-10-23 15:45:00"),

            // ---- 1014 Mobile Checkout ----------------------------------------------
            new SeedIssue(15, "Apple Pay sheet dismisses on network hiccup",
                    "A transient failure closes the sheet with no message, and the user "
                            + "reasonably assumes the payment went through.",
                    1014, 108, 107, "IN_PROGRESS", "CRITICAL", "BUG", 5, "Sprint 43",
                    "mobile,payments,ios", "2025-11-05 09:40:00", "2025-11-21 13:15:00"),
            new SeedIssue(16, "Saved cards not shown on Android 14",
                    "The keystore migration silently drops entries written by the "
                            + "previous version.",
                    1014, 108, 107, "TO_DO", "HIGH", "BUG", 5, "Sprint 44",
                    "mobile,payments,android", "2025-11-08 14:20:00", "2025-11-08 14:20:00"),
            new SeedIssue(17, "One-tap reorder from order history",
                    "Repeat a previous order without walking the full checkout flow.",
                    1014, 105, 107, "IN_REVIEW", "MEDIUM", "STORY", 8, "Sprint 44",
                    "mobile,checkout,ux", "2025-11-12 10:00:00", "2025-12-01 09:35:00"),
            new SeedIssue(18, "Checkout totals disagree with the cart by one cent",
                    "Tax is rounded per line in the cart and on the total at checkout.",
                    1014, 105, 107, "DONE", "HIGH", "BUG", 3, "Sprint 43",
                    "mobile,checkout,tax,rounding", "2025-11-06 11:30:00", "2025-11-25 16:20:00"),
            new SeedIssue(19, "Guest checkout for first-time buyers",
                    "Requiring an account before the first purchase is the largest single "
                            + "drop-off in the funnel.",
                    1014, null, 107, "TO_DO", "HIGH", "EPIC", 13, null,
                    "mobile,checkout,conversion", "2025-11-14 15:10:00", "2025-11-14 15:10:00"),
            new SeedIssue(20, "Regression suite for the payment sheet",
                    "Automate the fifteen manual cases QA runs before every release.",
                    1014, 106, 107, "IN_PROGRESS", "MEDIUM", "TASK", 8, "Sprint 44",
                    "mobile,testing,payments", "2025-11-18 08:50:00", "2025-12-03 10:10:00"),

            // ---- 1015 Billing Migration --------------------------------------------
            new SeedIssue(21, "Dual-write invoices to the new ledger",
                    "Write to both systems for a full billing cycle before cutting over.",
                    1015, 105, 103, "IN_PROGRESS", "CRITICAL", "TASK", 13, "Sprint 43",
                    "billing,migration", "2025-08-14 09:00:00", "2025-11-30 14:40:00"),
            new SeedIssue(22, "Proration is wrong for mid-cycle downgrades",
                    "The credit is calculated against the new plan's rate rather than the "
                            + "old one, so customers are under-credited.",
                    1015, 105, 103, "IN_REVIEW", "CRITICAL", "BUG", 8, "Sprint 43",
                    "billing,proration", "2025-09-02 10:20:00", "2025-12-05 11:25:00"),
            new SeedIssue(23, "Reconciliation report for the cutover window",
                    "Line-by-line comparison of old and new ledger output.",
                    1015, 110, 103, "TO_DO", "HIGH", "TASK", 8, "Sprint 45",
                    "billing,migration,reporting", "2025-10-28 13:45:00", "2025-10-28 13:45:00"),
            new SeedIssue(24, "Retire the legacy invoice PDF generator",
                    "Only three customers still receive the old format.",
                    1015, 110, 103, "TO_DO", "LOW", "TASK", 3, null,
                    "billing,legacy,cleanup", "2025-11-04 16:00:00", "2025-11-04 16:00:00"),
            new SeedIssue(25, "Currency rounding differs between ledgers",
                    "The old ledger rounds half-up, the new one half-even. On a month of "
                            + "invoices the two disagree by a few cents.",
                    1015, 105, 103, "DONE", "HIGH", "BUG", 5, "Sprint 42",
                    "billing,rounding,migration", "2025-09-15 09:30:00", "2025-11-12 15:00:00"),

            // ---- 1016 Search Relevance ---------------------------------------------
            new SeedIssue(26, "Exact-match results ranked below fuzzy ones",
                    "The boost applied to the fuzzy clause outweighs the exact term.",
                    1016, 111, 101, "IN_PROGRESS", "HIGH", "BUG", 5, "Sprint 45",
                    "search,ranking", "2026-01-14 10:15:00", "2026-02-02 09:45:00"),
            new SeedIssue(27, "Synonym dictionary for product categories",
                    "Users search for 'trainers' and get nothing because the catalogue "
                            + "says 'sneakers'.",
                    1016, 111, 101, "TO_DO", "MEDIUM", "STORY", 8, "Sprint 45",
                    "search,synonyms,catalogue", "2026-01-16 11:40:00", "2026-01-16 11:40:00"),
            new SeedIssue(28, "Search latency spikes above 800 ms at peak",
                    "The index is being queried without the category filter pushed down.",
                    1016, 102, 101, "TO_DO", "HIGH", "BUG", 5, "Sprint 46",
                    "search,performance", "2026-01-21 14:05:00", "2026-01-21 14:05:00"),
            new SeedIssue(29, "Track zero-result queries",
                    "Log the searches that return nothing, as the shortest path to knowing "
                            + "what the catalogue is missing.",
                    1016, 111, 101, "IN_REVIEW", "MEDIUM", "TASK", 3, "Sprint 45",
                    "search,analytics", "2026-01-19 09:20:00", "2026-02-04 16:30:00"),

            // ---- 1017 Design System ------------------------------------------------
            new SeedIssue(30, "Button focus ring invisible on dark surfaces",
                    "The ring colour is hard-coded rather than taken from the token, so it "
                            + "disappears wherever the surface is dark.",
                    1017, 109, 107, "DONE", "HIGH", "BUG", 2, "Sprint 43",
                    "design-system,accessibility,tokens", "2025-10-24 10:30:00",
                    "2025-11-14 12:15:00"),
            new SeedIssue(31, "Publish spacing tokens to npm",
                    "Applications are copying the scale by hand and drifting from it.",
                    1017, 109, 107, "IN_PROGRESS", "MEDIUM", "TASK", 5, "Sprint 44",
                    "design-system,tokens,tooling", "2025-11-02 13:10:00", "2025-12-08 10:55:00"),
            new SeedIssue(32, "Data table component",
                    "Sorting, filtering and empty states in one place, instead of four "
                            + "half-finished copies across the products.",
                    1017, 104, 107, "TO_DO", "HIGH", "EPIC", 13, "Sprint 45",
                    "design-system,components,tables", "2025-11-09 15:25:00",
                    "2025-11-09 15:25:00"),
            new SeedIssue(33, "Document the colour contrast requirements",
                    "Contributors keep proposing palettes that fail AA on body text.",
                    1017, 109, 107, "TO_DO", "LOW", "TASK", 2, null,
                    "design-system,accessibility,docs", "2025-11-15 09:05:00",
                    "2025-11-15 09:05:00"),
            new SeedIssue(34, "Icon set is inconsistent at 16px",
                    "Icons drawn on a 24px grid go blurry when scaled down; they need "
                            + "redrawing at the smaller size.",
                    1017, 109, 107, "IN_REVIEW", "MEDIUM", "TASK", 5, "Sprint 44",
                    "design-system,icons", "2025-11-20 11:50:00", "2025-12-10 14:05:00"),

            // ---- 1018 Platform Hardening -------------------------------------------
            new SeedIssue(35, "Rotate the service signing keys",
                    "The current key has been in use since the first deployment and is "
                            + "shared across environments.",
                    1018, 110, 103, "TO_DO", "CRITICAL", "TASK", 5, "Sprint 46",
                    "platform,security,secrets", "2026-02-03 09:15:00", "2026-02-03 09:15:00"),
            new SeedIssue(36, "Add request timeouts to every outbound call",
                    "A call without a read timeout waits forever, and one stalled "
                            + "dependency takes its caller down with it.",
                    1018, 110, 103, "IN_PROGRESS", "HIGH", "TASK", 5, "Sprint 46",
                    "platform,resilience,timeouts", "2026-02-04 10:40:00", "2026-02-09 15:20:00"),
            new SeedIssue(37, "Structured logs with a correlation id",
                    "One user action crosses four services; without a shared id, tracing "
                            + "it means correlating timestamps and hoping.",
                    1018, 106, 103, "IN_REVIEW", "MEDIUM", "TASK", 3, "Sprint 46",
                    "platform,observability,logging", "2026-02-05 14:00:00",
                    "2026-02-10 11:30:00"),
            new SeedIssue(38, "Health checks return 200 while the database is down",
                    "The check does not touch the datasource, so an unusable service still "
                            + "reports itself healthy and stays in the load balancer.",
                    1018, 110, 103, "TO_DO", "HIGH", "BUG", 3, "Sprint 47",
                    "platform,health,monitoring", "2026-02-06 08:25:00", "2026-02-06 08:25:00"),
            new SeedIssue(39, "Load test the gateway at 500 concurrent users",
                    "Establish where it actually falls over, before finding out in "
                            + "production.",
                    1018, null, 103, "TO_DO", "MEDIUM", "TASK", 8, null,
                    "platform,performance,testing", "2026-02-07 16:10:00", "2026-02-07 16:10:00"),
            new SeedIssue(40, "Pin base images to a digest",
                    "Floating tags mean two builds of the same commit can differ.",
                    1018, 110, 103, "DONE", "LOW", "TASK", 2, "Sprint 45",
                    "platform,builds,reproducibility", "2026-01-28 13:35:00",
                    "2026-02-08 09:50:00"));

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

        for (SeedIssue issue : ISSUES) {
            jdbcTemplate.update(INSERT, issue.id(), issue.summary(), issue.description(),
                    issue.projectId(), issue.assigneeId(), issue.createdBy(), issue.status(),
                    issue.priority(), issue.type(), issue.points(), issue.sprint(),
                    issue.tags(), issue.created(), issue.updated());
        }

        int nextId = ISSUES.get(ISSUES.size() - 1).id() + 1;
        jdbcTemplate.execute("ALTER TABLE issue AUTO_INCREMENT = " + nextId);

        log.info("Seeded {} issues (ids 1-{}); next id is {}",
                ISSUES.size(), nextId - 1, nextId);
    }
}
