package com.its.comment.seed;

import com.its.comment.repository.CommentRepository;
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
 * Loads the development dataset on an empty table.
 *
 * <p>Neither source document defines comments at all (SRS A-01), so there is no reference
 * data to reproduce here - these threads exist so the issue detail page has something to
 * render, and so the comment count on an issue is sometimes zero and sometimes not.
 *
 * <p>The issue ids are the ones this service's seeded siblings create. Nothing is
 * validated against them: this service calls nobody, deliberately, because the Issue
 * Service already calls in and an outbound call would close a cycle (DESIGN 6.1). A
 * comment against an issue that does not exist is simply never shown.
 *
 * <p>Threads deliberately mix authors, including Project Owners and Assignees on the same
 * issue, which is what makes the cascade delete interesting: applying the author rule to
 * it would fail on any thread with more than one participant.
 *
 * <p>Three layers, as in the sibling seeders: hand-written threads that read as
 * conversations, one deliberately long thread on issue 50 - forty-odd comments, which is
 * where an issue detail page that renders every comment inline starts to hurt - and
 * generated comments across the bulk issues so the counts on a list screen are not all
 * zero.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO comment (issue_id, author_id, body, created_on)
            VALUES (?, ?, ?, ?)
            """;

    record SeedComment(int issueId, int authorId, String body, String createdOn) {
    }

    private static final List<SeedComment> CURATED = List.of(
            new SeedComment(1, 104,
                    "Reproduced on Safari 17 and Firefox 122. The stale value comes back "
                            + "for about ninety seconds after saving, which lines up with "
                            + "the CDN TTL rather than anything in our cache.",
                    "2025-09-19 10:12:00"),
            new SeedComment(1, 101,
                    "Good catch. If it is the CDN then the fix is a cache-control header "
                            + "on the profile response, not an eviction call.",
                    "2025-09-19 11:45:00"),
            new SeedComment(1, 112,
                    "Confirmed - the response has no Cache-Control at all, so the CDN is "
                            + "applying its default.",
                    "2025-09-22 09:30:00"),

            new SeedComment(2, 102,
                    "The provider's status page shows two incidents in the same window. "
                            + "Worth checking whether we retry at all before we blame them.",
                    "2025-09-20 14:20:00"),
            new SeedComment(2, 103,
                    "We do not. See issue 9 - the retry work should probably land first.",
                    "2025-09-21 08:55:00"),

            new SeedComment(7, 104,
                    "Raising this to critical. A session that survives an email change is "
                            + "an account takeover, not an inconvenience.",
                    "2025-10-08 10:15:00"),
            new SeedComment(7, 101,
                    "Agreed. Verification link to the new address, and the change does not "
                            + "apply until it is clicked.",
                    "2025-10-08 16:40:00"),
            new SeedComment(7, 106,
                    "I will add the takeover case to the regression suite once the flow "
                            + "settles.",
                    "2025-10-21 11:05:00"),

            new SeedComment(9, 105,
                    "The retry loop has no cap and no backoff. During the last incident we "
                            + "sent 40x our normal volume at a provider that was already "
                            + "struggling.",
                    "2025-10-04 09:25:00"),
            new SeedComment(9, 103,
                    "So we turned their bad ten minutes into our bad hour. Exponential "
                            + "backoff with a ceiling, and a circuit breaker.",
                    "2025-10-06 13:10:00"),
            new SeedComment(9, 110,
                    "Happy to help with the breaker - the same pattern is going into the "
                            + "platform work under 1018.",
                    "2025-10-19 10:48:00"),

            new SeedComment(15, 108,
                    "The sheet closes on any error, including a timeout. From the user's "
                            + "side that is indistinguishable from a completed payment.",
                    "2025-11-06 11:00:00"),
            new SeedComment(15, 107,
                    "That is the worst possible failure mode for a payment screen. Keep "
                            + "the sheet open and show the error inside it.",
                    "2025-11-07 09:20:00"),
            new SeedComment(15, 106,
                    "Reproducible on a throttled connection - about one in five attempts.",
                    "2025-11-21 13:15:00"),

            new SeedComment(18, 105,
                    "Cart rounds each line, checkout rounds the total. Pick one - I would "
                            + "round once, at the end.",
                    "2025-11-07 15:30:00"),
            new SeedComment(18, 107,
                    "Round at the end, and add a test with the basket from the support "
                            + "ticket so it stays fixed.",
                    "2025-11-10 10:05:00"),

            new SeedComment(22, 105,
                    "Confirmed against three real accounts: the credit uses the new plan's "
                            + "rate. Every mid-cycle downgrade since June is affected.",
                    "2025-09-04 11:15:00"),
            new SeedComment(22, 103,
                    "Then this needs a backfill as well as a fix. Can you size the affected "
                            + "set?",
                    "2025-09-05 09:40:00"),
            new SeedComment(22, 105,
                    "About 1,400 accounts, averaging £2.80 under-credited. Not large "
                            + "individually, but it is our error and it should be corrected.",
                    "2025-12-05 11:25:00"),

            new SeedComment(26, 111,
                    "The fuzzy clause carries a boost of 2.0 and the exact term 1.0, which "
                            + "is exactly backwards.",
                    "2026-01-15 10:30:00"),
            new SeedComment(26, 101,
                    "Do we know why? If it was deliberate there may be a query it was "
                            + "compensating for.",
                    "2026-01-16 14:20:00"),
            new SeedComment(26, 111,
                    "Traced it to a typo tolerance experiment that was never reverted.",
                    "2026-02-02 09:45:00"),

            new SeedComment(30, 109,
                    "The ring is hard-coded to a light-mode value. It should read from the "
                            + "focus token like everything else.",
                    "2025-10-25 09:50:00"),
            new SeedComment(30, 104,
                    "This one fails WCAG outright on the dark surfaces, so it is worth "
                            + "doing before the component library ships.",
                    "2025-11-14 12:15:00"),

            new SeedComment(36, 110,
                    "Auditing the outbound calls now. Roughly half have no read timeout, "
                            + "which means they inherit an effectively infinite one.",
                    "2026-02-05 09:30:00"),
            new SeedComment(36, 103,
                    "Set a default at the client factory rather than per call site, so new "
                            + "calls inherit it without anyone remembering to.",
                    "2026-02-09 15:20:00"),

            new SeedComment(38, 110,
                    "The check returns a static 200 without touching the datasource, so a "
                            + "service with a dead connection pool stays in the load "
                            + "balancer indefinitely.",
                    "2026-02-06 09:10:00"),

            new SeedComment(21, 105,
                    "Dual-write is running in staging. One full cycle before we consider "
                            + "cutting over.",
                    "2025-11-30 14:40:00"),
            new SeedComment(32, 104,
                    "Four products have built their own table component. Worth collecting "
                            + "the requirements from all four before designing this one.",
                    "2025-11-10 09:15:00"),
            new SeedComment(17, 105,
                    "Reorder needs to re-price at today's rates, not the original ones. "
                            + "Worth stating on the confirmation screen.",
                    "2025-12-01 09:35:00"),

            // Non-Latin text and an apostrophe, on the issues raised for exactly that.
            new SeedComment(43, 114,
                    "並び順はロケール依存のコンパレータが原因です。照合順序を明示的に指定すれば直ります。",
                    "2025-12-09 09:40:00"),
            new SeedComment(43, 107,
                    "Then let us pin the collation rather than relying on the JVM default - "
                            + "the same bug will come back on any host with a different locale.",
                    "2025-12-11 15:10:00"),
            new SeedComment(44, 115,
                    "It is the apostrophe in O'Connor. The key is built by concatenation, so "
                            + "the quote terminates it early and the lookup misses.",
                    "2026-02-14 09:15:00"),
            new SeedComment(47, 115,
                    "Confirmed the PAN reaches the log. Treating this as an incident, not a "
                            + "bug - the logs need scrubbing as well as the code fixing.",
                    "2026-02-16 08:20:00"));

    // ------------------------------------------------------------------------------
    // Generated volume
    // ------------------------------------------------------------------------------

    /**
     * The long thread, on the multi-region epic (issue 50). An issue detail page that
     * renders every comment inline and unpaged is perfectly comfortable at three and
     * noticeably less so at forty-five, which is the point of this fixture.
     */
    private static final int LONG_THREAD_ISSUE_ID = 50;
    private static final int LONG_THREAD_SIZE = 45;

    /**
     * Mirrors the Issue Service's seeder: hand-written issues run to 52, and the
     * generated block adds 12 for the completed project, 120 and 100 for the two
     * archives, and 12 for each of 20 generated projects. Repeated rather than shared
     * because the services deliberately have no common module (DESIGN 3).
     *
     * <p>Nothing is validated against these ids - this service calls nobody, by design -
     * so the cost of drift is a comment attached to an issue that does not exist, which
     * is simply never shown. Cheap, but worth keeping right.
     */
    private static final int FIRST_GENERATED_ISSUE_ID = 53;
    private static final int GENERATED_ISSUE_COUNT = 12 + 120 + 100 + (20 * 12);

    /** Authors for generated comments - every one a seeded user id in {@code user_db}. */
    private static final int[] AUTHOR_POOL = {
            101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
            113, 114, 115, 116, 117, 120};

    private static final String[] THREAD_OPENERS = {
            "Reproduced on the current release. Attaching the request id from the failing "
                    + "run in case it helps.",
            "This has been raised twice before and closed as not reproducible both times. "
                    + "It reproduces under load, which is probably why.",
            "Had a look this morning - the cause is upstream of where the error surfaces, "
                    + "so the stack trace is misleading.",
            "Confirmed against a copy of production data. It does not happen with the "
                    + "sample dataset, which explains why the tests are green.",
            "Not urgent, but it is going to keep costing us support tickets until it is "
                    + "fixed."};

    private static final String[] THREAD_REPLIES = {
            "Agreed on the diagnosis. The fix is small; the test that proves it is not.",
            "Can we size this before it goes into a sprint? It looks like a one-liner and "
                    + "those are usually the expensive ones.",
            "Picking this up. Should have something to review by the end of the week.",
            "Blocked on the platform work - once timeouts are in place this becomes much "
                    + "easier to reason about.",
            "Closing the loop: deployed, and the error rate is flat since.",
            "Worth a follow-up issue for the cleanup rather than growing this one."};

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Deterministic by construction - index arithmetic, no randomness - so two clean
     * checkouts seed identical databases.
     */
    private static List<SeedComment> generatedComments() {
        List<SeedComment> comments = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2025, 3, 4, 10, 0);

        for (int i = 0; i < LONG_THREAD_SIZE; i++) {
            String body = i == 0
                    ? "Opening this up for design input. Multi-region touches every service, "
                            + "so I would rather have the argument here than in four separate "
                            + "pull requests."
                    : (i % 2 == 0
                            ? THREAD_OPENERS[i % THREAD_OPENERS.length]
                            : THREAD_REPLIES[i % THREAD_REPLIES.length]);

            comments.add(new SeedComment(LONG_THREAD_ISSUE_ID,
                    AUTHOR_POOL[i % AUTHOR_POOL.length],
                    body,
                    base.plusDays(i * 3L).plusMinutes((i * 43L) % 420).format(TIMESTAMP)));
        }

        // Roughly half the generated issues carry a thread, and those that do carry one
        // to three comments. A list screen where every count is the same reads as a
        // rendering fault rather than as data.
        for (int i = 0; i < GENERATED_ISSUE_COUNT; i++) {
            if (i % 2 == 1) {
                continue;
            }
            int issueId = FIRST_GENERATED_ISSUE_ID + i;
            int depth = 1 + (i % 3);
            LocalDateTime raised = base.plusDays(i % 380);

            for (int c = 0; c < depth; c++) {
                comments.add(new SeedComment(issueId,
                        AUTHOR_POOL[(i + c) % AUTHOR_POOL.length],
                        c == 0
                                ? THREAD_OPENERS[i % THREAD_OPENERS.length]
                                : THREAD_REPLIES[(i + c) % THREAD_REPLIES.length],
                        raised.plusDays(c * 2L).plusMinutes((i * 29L + c * 55L) % 500)
                                .format(TIMESTAMP)));
            }
        }
        return comments;
    }

    static final List<SeedComment> COMMENTS = buildComments();

    private static List<SeedComment> buildComments() {
        List<SeedComment> comments = new ArrayList<>(CURATED);
        comments.addAll(generatedComments());
        return List.copyOf(comments);
    }

    private final CommentRepository commentRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(CommentRepository commentRepository, JdbcTemplate jdbcTemplate) {
        this.commentRepository = commentRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (commentRepository.count() > 0) {
            log.debug("Comments already present - skipping seed");
            return;
        }

        jdbcTemplate.batchUpdate(INSERT, COMMENTS.stream()
                .map(comment -> new Object[]{
                        comment.issueId(), comment.authorId(),
                        comment.body(), comment.createdOn()})
                .toList());

        log.info("Seeded {} comments across {} issues", COMMENTS.size(),
                COMMENTS.stream().map(SeedComment::issueId).distinct().count());
    }
}
