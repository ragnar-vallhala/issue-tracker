package com.its.comment.seed;

import com.its.comment.repository.CommentRepository;
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
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO comment (issue_id, author_id, body, created_on)
            VALUES (?, ?, ?, ?)
            """;

    private record SeedComment(int issueId, int authorId, String body, String createdOn) {
    }

    private static final List<SeedComment> COMMENTS = List.of(
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
                    "2025-12-01 09:35:00"));

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

        for (SeedComment comment : COMMENTS) {
            jdbcTemplate.update(INSERT, comment.issueId(), comment.authorId(),
                    comment.body(), comment.createdOn());
        }

        log.info("Seeded {} comments across {} issues", COMMENTS.size(),
                COMMENTS.stream().map(SeedComment::issueId).distinct().count());
    }
}
