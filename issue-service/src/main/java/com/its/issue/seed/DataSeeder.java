package com.its.issue.seed;

import com.its.issue.repository.IssueRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * <p>Issues 1 and 2 are the reference workbook's own rows. The workbook records
 * {@code created_by = sam.lee} for both - a username string in a column the ER diagram
 * types as an integer, referring to a user absent from its own User table. Following
 * SRS A-17 the column is an integer user id here, and the seed maps that value onto
 * Emily Sinha (101), a real Project Owner who could plausibly have raised them.
 *
 * <p>The set is built in three layers:
 *
 * <ul>
 *   <li><b>1-40</b> are hand-written and readable. Every status, priority and type is
 *       represented, and the sprint and story-point fields are populated unevenly - some
 *       issues are groomed, some are not, which is what a real backlog looks like.
 *   <li><b>41-52</b> are edge-case fixtures: values at the 255-character column limit,
 *       non-Latin text, quotes and apostrophes, a row with every nullable field null, an
 *       issue older than the rest of the dataset by years. These exist to be awkward.
 *   <li><b>53 and up</b> are generated, so the lists, filters and status meters are
 *       exercised against a few hundred rows rather than a few dozen.
 * </ul>
 *
 * <p>Projects 1013 and 1022 are left with no issues on purpose, as the empty-state and
 * cascade-delete fixtures, and every issue on project 1021 is DONE - a completed project
 * reads differently from an empty one and both are easy to get wrong. The bulk sits on
 * projects 1023 and 1024, which share a single owner, so only that owner's dashboard pays
 * for the volume.
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

    record SeedIssue(int id, String summary, String description, int projectId,
                     Integer assigneeId, int createdBy, String status,
                     String priority, String type, Integer points, String sprint,
                     String tags, String created, String updated) {
    }

    private static final List<SeedIssue> CURATED = List.of(

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
                    "2026-02-08 09:50:00"),

            // ---- edge cases ---------------------------------------------------------
            // Summary close to the 255-character column limit. Summaries appear in table
            // cells, cards, the recent-activity list and the browser title, and each of
            // those truncates at a different width - or does not truncate at all.
            new SeedIssue(41,
                    "Intermittent 504 from the reporting endpoint when a tenant with more "
                            + "than fifty thousand archived records requests a full export "
                            + "while the nightly aggregation job is running, which appears "
                            + "to be lock contention rather than a genuine timeout",
                    "Only reproducible against production-sized data.",
                    1011, 105, 101, "TO_DO", "HIGH", "BUG", 8, "Sprint 46",
                    "reporting,timeout,contention", "2026-02-11 09:15:00",
                    "2026-02-11 09:15:00"),
            // Description at the column limit, with a short summary - the opposite shape
            // to the row above, and the one that breaks fixed-height detail panels.
            new SeedIssue(42, "Session fixation on the login redirect",
                    "The session id survives the privilege change at login, so a value "
                            + "planted before authentication is still valid afterwards. "
                            + "Invalidate the session and issue a new id on every successful "
                            + "authentication, and do the same on logout.",
                    1018, 115, 103, "IN_PROGRESS", "CRITICAL", "BUG", 5, "Sprint 46",
                    "security,session,auth", "2026-02-12 10:40:00", "2026-02-13 14:05:00"),
            // Non-Latin text in the fields a person reads. The schema is utf8mb4; a
            // connection negotiated as latin1 turns this row into mojibake.
            new SeedIssue(43, "検索結果の並び順が言語設定によって変わる",
                    "The sort comparator falls back to the platform default locale, so the "
                            + "same query orders differently for different users.",
                    1020, 114, 107, "IN_REVIEW", "MEDIUM", "BUG", 5, "Sprint 45",
                    "search,i18n,locale", "2025-12-08 11:25:00", "2026-01-09 16:50:00"),
            // Quotes and an apostrophe, which prepared statements handle and string
            // concatenation does not - and which an unescaped JSP renders as entities.
            new SeedIssue(44, "\"Save & continue\" doesn't persist the user's draft",
                    "The draft is written to local storage under a key containing the "
                            + "user's display name, so any apostrophe breaks the lookup.",
                    1011, 115, 101, "TO_DO", "MEDIUM", "BUG", 3, null,
                    "forms,drafts,escaping", "2026-02-14 08:30:00", "2026-02-14 08:30:00"),
            // Every nullable column left null: no assignee, no points, no sprint, no
            // tags. The row that finds the code assuming any of them is present.
            new SeedIssue(45, "Investigate duplicate webhook deliveries",
                    null,
                    1012, null, 103, "TO_DO", "LOW", "TASK", null, null,
                    null, "2026-02-15 12:00:00", "2026-02-15 12:00:00"),
            // Years older than anything else here, and closed long after it was raised.
            // Date columns that render as "x days ago" get this one wrong.
            new SeedIssue(46, "Replace the deprecated TLS 1.0 listener",
                    "Raised during the 2019 audit and carried across three migrations "
                            + "before it was finally done.",
                    1018, 110, 103, "DONE", "HIGH", "TASK", 3, "Sprint 12",
                    "platform,security,tls", "2019-04-02 09:00:00", "2025-11-19 17:20:00"),
            // Critical and unassigned at once: the combination the triage view is
            // supposed to surface, and the one an "assigned work" filter hides.
            new SeedIssue(47, "Card details logged in plain text on validation failure",
                    "The validation error handler logs the whole request body, which on "
                            + "this endpoint includes the PAN. Log scrubbing does not run on "
                            + "this path.",
                    1014, null, 107, "TO_DO", "CRITICAL", "BUG", 5, "Sprint 46",
                    "payments,security,logging,pci", "2026-02-16 07:45:00",
                    "2026-02-16 07:45:00"),
            // Tag list at the column limit: the filter chips wrap, and a naive
            // comma-split in the UI has to cope with fifteen of them.
            new SeedIssue(48, "Consolidate the duplicated retry helpers",
                    "Four services have their own copy, three of them subtly different.",
                    1018, 116, 103, "TO_DO", "LOW", "TASK", 5, null,
                    "platform,cleanup,retry,resilience,tech-debt,refactor,shared,http,"
                            + "timeouts,backoff,jitter,circuit-breaker,observability,"
                            + "documentation,follow-up",
                    "2026-02-17 15:20:00", "2026-02-17 15:20:00"),
            // Zero story points, which is not the same as ungroomed - a chart that
            // treats 0 and null alike is wrong about one of them.
            new SeedIssue(49, "Correct the typo in the footer copyright",
                    "Says 2024. It is not 2024.",
                    1017, 109, 107, "DONE", "LOW", "TASK", 0, "Sprint 45",
                    "design-system,copy", "2026-01-05 09:10:00", "2026-01-05 10:30:00"),
            // The other end of the scale, and larger than any real sprint could hold.
            new SeedIssue(50, "Multi-region active-active deployment",
                    "Tracked as one issue so the dependencies stay visible, though it "
                            + "cannot be delivered in a single sprint.",
                    1018, null, 103, "TO_DO", "HIGH", "EPIC", 100, null,
                    "platform,availability,multi-region", "2026-02-18 11:00:00",
                    "2026-02-18 11:00:00"),
            // A sprint name far longer than the two-word ones above.
            new SeedIssue(51, "Backfill missing created_on values",
                    "Rows imported in the 2021 migration have a null timestamp, which "
                            + "sorts them to the top of every list.",
                    1015, 116, 103, "IN_PROGRESS", "MEDIUM", "TASK", 3,
                    "Sprint 46 - Billing cutover hardening and data quality",
                    "billing,data-quality,migration", "2026-02-19 13:40:00",
                    "2026-02-20 09:05:00"),
            // Created and last updated in the same second, on an issue that is DONE:
            // a closed-on-arrival duplicate, which makes the age charts honest.
            new SeedIssue(52, "Duplicate of issue 22",
                    "Filed twice from the same support escalation.",
                    1015, 105, 103, "DONE", "LOW", "BUG", 1, "Sprint 43",
                    "billing,duplicate", "2025-12-06 10:00:00", "2025-12-06 10:00:00"));

    // ------------------------------------------------------------------------------
    // Generated volume
    // ------------------------------------------------------------------------------

    /** First generated id. Everything below this is hand-written above. */
    static final int FIRST_GENERATED_ID = 53;

    /**
     * Mirrors the Project Service's seeder: its generated projects start at 1025 and run
     * for 20, and the two archive projects are 1023 and 1024. Repeated rather than shared
     * because the services deliberately have no common module (DESIGN 3).
     */
    private static final int FIRST_GENERATED_PROJECT_ID = 1025;
    private static final int GENERATED_PROJECT_COUNT = 20;
    private static final int[] ARCHIVE_PROJECT_IDS = {1023, 1024};
    private static final int[] ARCHIVE_PROJECT_SIZES = {120, 100};
    private static final int ARCHIVE_OWNER_ID = 120;

    /** The completed project: every issue on it is DONE (project 1021, owner 107). */
    private static final int COMPLETED_PROJECT_ID = 1021;
    private static final int COMPLETED_PROJECT_OWNER = 107;
    private static final int COMPLETED_PROJECT_SIZE = 12;

    /** Issues generated per generated project. */
    private static final int ISSUES_PER_GENERATED_PROJECT = 12;

    /**
     * Assignable users - every one a role-1 Assignee in {@code user_db} (FR-ISS-07). User
     * 119 is deliberately absent: an assignee with no issues is a fixture in its own
     * right. The generated block of the User Service's seeder starts at 121 and makes
     * every seventh user an owner, so the rest are assignees.
     */
    private static List<Integer> assigneeIds() {
        List<Integer> ids = new ArrayList<>(List.of(
                102, 104, 105, 106, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117));
        for (int i = 0; i < 50; i++) {
            if (i % 7 != 0) {
                ids.add(121 + i);
            }
        }
        return ids;
    }

    /** Owner ids for the generated projects, in the order the Project Service assigns. */
    private static List<Integer> generatedProjectOwners() {
        List<Integer> owners = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            if (i % 7 == 0) {
                owners.add(121 + i);
            }
        }
        return owners;
    }

    // Weighted by repetition rather than by a random draw, so the mix is visible in the
    // source: mostly open work, a healthy tail of finished work, less in review.
    private static final String[] STATUS_MIX = {
            "TO_DO", "TO_DO", "TO_DO", "IN_PROGRESS", "IN_PROGRESS",
            "IN_REVIEW", "DONE", "DONE", "DONE", "DONE"};

    private static final String[] PRIORITY_MIX = {
            "LOW", "MEDIUM", "MEDIUM", "HIGH", "HIGH", "CRITICAL", "MEDIUM"};

    private static final String[] TYPE_MIX = {"BUG", "BUG", "TASK", "STORY", "EPIC"};

    private static final Integer[] POINTS_MIX = {null, 1, 2, 3, 5, 8, 13};

    private static final String[] SUBJECTS = {
            "The export job", "The settings page", "The webhook dispatcher",
            "The audit log", "The bulk importer", "The session store",
            "The rate limiter", "The report scheduler", "The file uploader",
            "The search indexer", "The permissions check", "The email renderer"};

    private static final String[] PREDICATES = {
            "times out on large accounts",
            "drops the tenant id under load",
            "double-counts retried requests",
            "ignores the configured page size",
            "fails silently when the queue is full",
            "returns 200 on a partial write",
            "loses ordering after a restart",
            "leaks a connection on the error path",
            "rejects valid input from older clients",
            "logs the full payload at INFO"};

    private static final String[] CONTEXTS = {
            "in staging", "for enterprise tenants", "after the 4.2 upgrade",
            "on the read replica", "during the nightly window", "behind the proxy",
            "for accounts created before 2023", "under concurrent edits"};

    private static final String[] TAG_SETS = {
            "backend,reliability", "frontend,ux", "data,pipeline", "platform,tooling",
            "security,review", "performance,latency", "tech-debt,cleanup",
            "api,contract", "mobile,parity", "observability,logging"};

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Volume behind the hand-written backlog. Deterministic by construction - index
     * arithmetic against fixed tables, no randomness - so two clean checkouts seed
     * identical databases and a screenshot taken today still matches tomorrow.
     *
     * <p>The three strides over {@code SUBJECTS}, {@code PREDICATES} and {@code CONTEXTS}
     * are coprime with their lengths, which gives 960 distinct summaries before any
     * repeat - comfortably more than this generates.
     */
    private static List<SeedIssue> generatedIssues() {
        List<Integer> assignees = assigneeIds();
        List<Integer> owners = generatedProjectOwners();
        List<SeedIssue> issues = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2025, 1, 8, 9, 0);
        int id = FIRST_GENERATED_ID;
        int n = 0;

        // The completed project: same shape as the rest, but every issue is DONE.
        for (int i = 0; i < COMPLETED_PROJECT_SIZE; i++, n++) {
            issues.add(generated(id++, n, COMPLETED_PROJECT_ID, COMPLETED_PROJECT_OWNER,
                    "DONE", assignees, base));
        }

        // The two imported archives, which hold most of the volume.
        for (int p = 0; p < ARCHIVE_PROJECT_IDS.length; p++) {
            for (int i = 0; i < ARCHIVE_PROJECT_SIZES[p]; i++, n++) {
                issues.add(generated(id++, n, ARCHIVE_PROJECT_IDS[p], ARCHIVE_OWNER_ID,
                        null, assignees, base));
            }
        }

        // The generated projects, each with a modest backlog of its own.
        for (int p = 0; p < GENERATED_PROJECT_COUNT; p++) {
            int projectId = FIRST_GENERATED_PROJECT_ID + p;
            int ownerId = owners.get(p % owners.size());
            for (int i = 0; i < ISSUES_PER_GENERATED_PROJECT; i++, n++) {
                issues.add(generated(id++, n, projectId, ownerId, null, assignees, base));
            }
        }
        return issues;
    }

    /**
     * One generated row. {@code n} is the running index that drives every varying field;
     * {@code forcedStatus} pins the status where the project demands it.
     */
    private static SeedIssue generated(int id, int n, int projectId, int createdBy,
                                       String forcedStatus, List<Integer> assignees,
                                       LocalDateTime base) {

        String status = forcedStatus != null ? forcedStatus : STATUS_MIX[n % STATUS_MIX.length];
        LocalDateTime created = base.plusDays(n % 400).plusMinutes((n * 37L) % 480);
        // Untouched since it was raised if it is still TO_DO; otherwise moved on since.
        LocalDateTime updated = "TO_DO".equals(status)
                ? created
                : created.plusDays(1L + (n % 45)).plusMinutes((n * 17L) % 300);

        // Every eleventh issue is unassigned, and every ninth has no sprint - the two
        // gaps that a dashboard has to render rather than assume away.
        Integer assignee = n % 11 == 0 ? null : assignees.get(n % assignees.size());
        String sprint = n % 9 == 0 ? null : "Sprint " + (30 + (n % 18));

        return new SeedIssue(id,
                SUBJECTS[n % SUBJECTS.length] + " "
                        + PREDICATES[(n / SUBJECTS.length) % PREDICATES.length] + " "
                        + CONTEXTS[(n / (SUBJECTS.length * PREDICATES.length)) % CONTEXTS.length],
                "Imported from the historical backlog. Reproduced by the reporter but not "
                        + "yet triaged against the current release.",
                projectId, assignee, createdBy, status,
                PRIORITY_MIX[n % PRIORITY_MIX.length],
                TYPE_MIX[n % TYPE_MIX.length],
                POINTS_MIX[n % POINTS_MIX.length],
                sprint,
                TAG_SETS[n % TAG_SETS.length],
                created.format(TIMESTAMP),
                updated.format(TIMESTAMP));
    }

    static final List<SeedIssue> ISSUES = buildIssues();

    private static List<SeedIssue> buildIssues() {
        List<SeedIssue> issues = new ArrayList<>(CURATED);
        issues.addAll(generatedIssues());
        return List.copyOf(issues);
    }

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

        jdbcTemplate.batchUpdate(INSERT, ISSUES.stream()
                .map(issue -> new Object[]{
                        issue.id(), issue.summary(), issue.description(),
                        issue.projectId(), issue.assigneeId(), issue.createdBy(),
                        issue.status(), issue.priority(), issue.type(), issue.points(),
                        issue.sprint(), issue.tags(), issue.created(), issue.updated()})
                .toList());

        int nextId = ISSUES.get(ISSUES.size() - 1).id() + 1;
        jdbcTemplate.execute("ALTER TABLE issue AUTO_INCREMENT = " + nextId);

        log.info("Seeded {} issues (ids 1-{}) across {} projects; next id is {}",
                ISSUES.size(), nextId - 1,
                ISSUES.stream().map(SeedIssue::projectId).distinct().count(), nextId);
    }
}
