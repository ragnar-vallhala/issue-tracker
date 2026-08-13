package com.its.comment.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.its.comment.seed.DataSeeder.SeedComment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * This service calls nobody by design (DESIGN 6.1), so nothing validates an issue id at
 * runtime and a comment on a non-existent issue is simply never shown. That makes drift
 * silent rather than loud: the seed succeeds, the counts look plausible, and a thread has
 * quietly detached from its issue.
 *
 * <p>These assertions are the check that would otherwise never happen.
 */
class DataSeederTest {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * The Issue Service seeds 52 hand-written issues and then a generated block; this is
     * the highest id it produces. Repeated here because the services share no module, and
     * asserted because nothing else would notice if it changed.
     */
    private static final int HIGHEST_SEEDED_ISSUE_ID = 52 + 12 + 120 + 100 + (20 * 12);

    private static final List<SeedComment> COMMENTS = DataSeeder.COMMENTS;

    @Test
    @DisplayName("Every comment hangs off an issue the Issue Service seeds")
    void issueIdsResolve() {
        for (SeedComment comment : COMMENTS) {
            assertThat(comment.issueId())
                    .as("a comment references issue %d, which nothing seeds",
                            comment.issueId())
                    .isBetween(1, HIGHEST_SEEDED_ISSUE_ID);
        }
    }

    @Test
    @DisplayName("Every author is a seeded user")
    void authorIdsResolve() {
        for (SeedComment comment : COMMENTS) {
            assertThat(comment.authorId())
                    .as("a comment on issue %d is authored by user %d",
                            comment.issueId(), comment.authorId())
                    .isBetween(101, 170);
        }
    }

    @Test
    @DisplayName("Bodies are present and timestamps parse")
    void bodiesAndTimestampsAreWellFormed() {
        for (SeedComment comment : COMMENTS) {
            assertThat(comment.body()).isNotBlank();
            assertThat(LocalDateTime.parse(comment.createdOn(), TIMESTAMP)).isNotNull();
        }
    }

    @Test
    @DisplayName("The long thread is long enough to be the fixture it claims to be")
    void longThreadIsPresent() {
        Map<Integer, Long> byIssue = COMMENTS.stream().collect(Collectors.groupingBy(
                SeedComment::issueId, Collectors.counting()));

        assertThat(byIssue.get(50))
                .as("issue 50 is the unpaged-thread fixture")
                .isGreaterThanOrEqualTo(40L);
    }

    @Test
    @DisplayName("Threads mix authors, which is what makes the cascade interesting")
    void threadsAreNotSingleAuthor() {
        Map<Integer, List<Integer>> authorsByIssue = COMMENTS.stream()
                .collect(Collectors.groupingBy(SeedComment::issueId,
                        Collectors.mapping(SeedComment::authorId, Collectors.toList())));

        // Applying the author rule to a cascade delete would fail on any multi-author
        // thread, so at least some threads must have more than one participant.
        assertThat(authorsByIssue.values())
                .anyMatch(authors -> authors.stream().distinct().count() > 1);
    }

    @Test
    @DisplayName("Comment counts vary, so a list screen is not uniformly one or zero")
    void commentCountsVary() {
        Map<Integer, Long> byIssue = COMMENTS.stream().collect(Collectors.groupingBy(
                SeedComment::issueId, Collectors.counting()));

        assertThat(byIssue.values().stream().distinct().count()).isGreaterThan(2L);
    }
}
