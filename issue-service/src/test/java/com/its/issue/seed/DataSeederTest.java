package com.its.issue.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.its.issue.entity.IssueType;
import com.its.issue.entity.Priority;
import com.its.issue.entity.Status;
import com.its.issue.seed.DataSeeder.SeedIssue;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Most of this seed is now generated, and generated rows fail in ways hand-written ones do
 * not: a summary that grew past 255 characters, a status string that no longer matches the
 * enum, a project id pointing at a project the sibling service stopped seeding. All of
 * those surface at start-up against MySQL, as a failed seed and an empty database.
 *
 * <p>The fixtures matter as much as the constraints. Project 1021 exists to be a fully
 * completed project and 1013 and 1022 exist to be empty, and each is one careless edit to
 * the generator away from being neither.
 */
class DataSeederTest {

    private static final int COLUMN_LIMIT = 255;

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<SeedIssue> ISSUES = DataSeeder.ISSUES;

    @Test
    @DisplayName("Ids are unique, contiguous, and start at the workbook's 1")
    void idsAreContiguousFromOne() {
        List<Integer> ids = ISSUES.stream().map(SeedIssue::id).toList();

        assertThat(ids).doesNotHaveDuplicates().isSorted();
        assertThat(ids.get(0)).isEqualTo(1);
        assertThat(ids.get(ids.size() - 1)).isEqualTo(ISSUES.size());
    }

    @Test
    @DisplayName("No value exceeds its 255-character column")
    void valuesFitTheirColumns() {
        for (SeedIssue issue : ISSUES) {
            assertThat(issue.summary())
                    .as("summary of issue %d", issue.id())
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(COLUMN_LIMIT);

            assertOptionalFits(issue.description(), issue.id(), "description");
            assertOptionalFits(issue.sprint(), issue.id(), "sprint");
            assertOptionalFits(issue.tags(), issue.id(), "tags");
        }
    }

    private static void assertOptionalFits(String value, int issueId, String column) {
        if (value != null) {
            assertThat(value)
                    .as("%s of issue %d", column, issueId)
                    .hasSizeLessThanOrEqualTo(COLUMN_LIMIT);
        }
    }

    @Test
    @DisplayName("Every status, priority and type is a real enum constant")
    void enumStringsAreValid() {
        for (SeedIssue issue : ISSUES) {
            assertThatCode(() -> Status.valueOf(issue.status()))
                    .as("status of issue %d", issue.id()).doesNotThrowAnyException();
            assertThatCode(() -> Priority.valueOf(issue.priority()))
                    .as("priority of issue %d", issue.id()).doesNotThrowAnyException();
            assertThatCode(() -> IssueType.valueOf(issue.type()))
                    .as("type of issue %d", issue.id()).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Timestamps parse and an issue is never updated before it was created")
    void timestampsAreCoherent() {
        for (SeedIssue issue : ISSUES) {
            LocalDateTime created = LocalDateTime.parse(issue.created(), TIMESTAMP);
            LocalDateTime updated = LocalDateTime.parse(issue.updated(), TIMESTAMP);

            assertThat(updated)
                    .as("issue %d was updated before it was created", issue.id())
                    .isAfterOrEqualTo(created);
        }
    }

    @Test
    @DisplayName("Every project id is one the Project Service seeds")
    void projectIdsResolve() {
        // project_db is a separate schema with no foreign key back to here, so this
        // arithmetic - 1011 through 1024 by hand, then 20 generated from 1025 - is the
        // only thing tying the two seeders together.
        for (SeedIssue issue : ISSUES) {
            assertThat(issue.projectId())
                    .as("issue %d belongs to project %d, which nothing seeds",
                            issue.id(), issue.projectId())
                    .isBetween(1011, 1044);
        }
    }

    @Test
    @DisplayName("Every assignee is a role-1 user, and 119 stays unassigned")
    void assigneeIdsResolve() {
        for (SeedIssue issue : ISSUES) {
            Integer assignee = issue.assigneeId();
            if (assignee == null) {
                continue;
            }
            assertThat(assignee)
                    .as("issue %d is assigned to user %d", issue.id(), assignee)
                    .isBetween(102, 170)
                    // Owners cannot be assignees (FR-ISS-07): 101, 103, 107, 118 and 120
                    // by hand, then every seventh generated user from 121.
                    .isNotIn(101, 103, 107, 118, 120)
                    // 119 is the fixture for an assignee with no issues at all.
                    .isNotEqualTo(119);

            if (assignee >= 121) {
                assertThat((assignee - 121) % 7)
                        .as("user %d is a generated Project Owner, not an assignee", assignee)
                        .isNotZero();
            }
        }
    }

    @Test
    @DisplayName("The workbook's two rows are unchanged")
    void workbookRowsAreIntact() {
        assertThat(ISSUES.subList(0, 2))
                .extracting(SeedIssue::id, SeedIssue::projectId, SeedIssue::assigneeId,
                        SeedIssue::status, SeedIssue::priority, SeedIssue::type)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, 1011, 104, "TO_DO", "HIGH", "BUG"),
                        org.assertj.core.groups.Tuple.tuple(2, 1012, 102, "TO_DO", "HIGH", "BUG"));
    }

    @Test
    @DisplayName("1013 and 1022 stay empty; every issue on 1021 is DONE")
    void projectShapeFixturesHold() {
        Map<Integer, List<SeedIssue>> byProject = ISSUES.stream()
                .collect(Collectors.groupingBy(SeedIssue::projectId,
                        Collectors.toList()));

        assertThat(byProject).doesNotContainKeys(1013, 1022);
        assertThat(byProject.get(1021))
                .isNotEmpty()
                .allMatch(issue -> "DONE".equals(issue.status()));
    }

    @Test
    @DisplayName("The bulk sits on the two archive projects, not on everyone's dashboard")
    void volumeIsConcentrated() {
        Map<Integer, Long> counts = ISSUES.stream().collect(Collectors.groupingBy(
                SeedIssue::projectId, Collectors.counting()));

        // The point of concentrating volume: the owner dashboard fetches every issue of
        // every project it owns, so no ordinary project may carry an archive's worth.
        assertThat(counts.get(1023)).isGreaterThan(50);
        assertThat(counts.get(1024)).isGreaterThan(50);

        counts.forEach((projectId, count) -> {
            if (projectId != 1023 && projectId != 1024) {
                assertThat(count)
                        .as("project %d carries %d issues, which is archive-sized",
                                projectId, count)
                        .isLessThanOrEqualTo(50);
            }
        });
    }

    @Test
    @DisplayName("Generated summaries stay distinct enough to tell apart")
    void generatedSummariesDoNotAllCollide() {
        List<String> generated = ISSUES.stream()
                .filter(issue -> issue.id() >= DataSeeder.FIRST_GENERATED_ID)
                .map(SeedIssue::summary)
                .toList();

        // The three strides are chosen to give 960 combinations. Repeats are acceptable
        // in filler, a handful of distinct strings across hundreds of rows is not.
        long distinct = generated.stream().distinct().count();
        assertThat(distinct).isGreaterThan(generated.size() / 2);
    }

    @Test
    @DisplayName("The unassigned and un-sprinted gaps both occur")
    void theAwkwardShapesArePresent() {
        assertThat(ISSUES).anyMatch(issue -> issue.assigneeId() == null);
        assertThat(ISSUES).anyMatch(issue -> issue.sprint() == null);
        assertThat(ISSUES).anyMatch(issue -> issue.points() == null);
        assertThat(ISSUES).anyMatch(issue -> issue.description() == null);
        assertThat(ISSUES).anyMatch(issue -> issue.tags() == null);
        // Zero points is not the same as ungroomed, and both must survive.
        assertThat(ISSUES).anyMatch(issue -> Integer.valueOf(0).equals(issue.points()));
    }

    @Test
    @DisplayName("Every status and priority appears, so no filter renders empty")
    void everyEnumValueIsRepresented() {
        Function<SeedIssue, String> status = SeedIssue::status;
        List<String> statuses = ISSUES.stream().map(status).distinct().toList();
        List<String> priorities = ISSUES.stream().map(SeedIssue::priority).distinct().toList();
        List<String> types = ISSUES.stream().map(SeedIssue::type).distinct().toList();

        assertThat(statuses).hasSize(Status.values().length);
        assertThat(priorities).hasSize(Priority.values().length);
        assertThat(types).hasSize(IssueType.values().length);
    }
}
