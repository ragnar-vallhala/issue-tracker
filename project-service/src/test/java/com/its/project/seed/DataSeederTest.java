package com.its.project.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.its.project.seed.DataSeeder.SeedProject;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the constraints the schema enforces and the ones it cannot. {@code project_name}
 * is UNIQUE and 255 characters, so a generated duplicate aborts the seed at start-up;
 * {@code project_owner_id} has no foreign key at all - the services own separate schemas -
 * so nothing but this test stops a project being owned by a user who is an Assignee, or
 * who does not exist.
 */
class DataSeederTest {

    private static final int COLUMN_LIMIT = 255;

    private static final List<SeedProject> PROJECTS = DataSeeder.PROJECTS;

    @Test
    @DisplayName("Ids are unique, contiguous, and start at the workbook's 1011")
    void idsAreContiguousFrom1011() {
        List<Integer> ids = PROJECTS.stream().map(SeedProject::id).toList();

        assertThat(ids).doesNotHaveDuplicates().isSorted();
        assertThat(ids.get(0)).isEqualTo(1011);
        assertThat(ids.get(ids.size() - 1)).isEqualTo(1010 + PROJECTS.size());
    }

    @Test
    @DisplayName("Names are unique and fit the column - the constraint is UNIQUE(255)")
    void namesAreUniqueAndFit() {
        assertThat(PROJECTS.stream().map(SeedProject::name).toList()).doesNotHaveDuplicates();

        for (SeedProject project : PROJECTS) {
            assertThat(project.name())
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(COLUMN_LIMIT);
        }
    }

    @Test
    @DisplayName("Every owner id is a Project Owner in user_db (FR-PRJ-02)")
    void everyOwnerIsAProjectOwner() {
        // The User Service seeds owners 101, 103, 107, 118 and 120 by hand, then makes
        // every seventh generated user from 121 an owner. There is no foreign key across
        // the schemas, so this arithmetic is the only thing keeping the two consistent.
        List<Integer> handWrittenOwners = List.of(101, 103, 107, 118, 120);

        for (SeedProject project : PROJECTS) {
            int ownerId = project.ownerId();
            boolean generatedOwner = ownerId >= 121
                    && ownerId < 121 + 50
                    && (ownerId - 121) % 7 == 0;

            assertThat(handWrittenOwners.contains(ownerId) || generatedOwner)
                    .as("project %d is owned by user %d, who must be a role-0 user",
                            project.id(), ownerId)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Dates parse, and an end date never precedes its start (FR-PRJ-03)")
    void dateRangesAreCoherent() {
        for (SeedProject project : PROJECTS) {
            LocalDate start = LocalDate.parse(project.start());
            if (project.end() != null) {
                assertThat(LocalDate.parse(project.end()))
                        .as("project %d ends before it starts", project.id())
                        .isAfterOrEqualTo(start);
            }
        }
    }

    @Test
    @DisplayName("The workbook's three rows are unchanged")
    void workbookRowsAreIntact() {
        assertThat(PROJECTS.subList(0, 3))
                .extracting(SeedProject::id, SeedProject::name, SeedProject::ownerId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1011, "Profile Management", 101),
                        org.assertj.core.groups.Tuple.tuple(1012, "Notifications Platform", 103),
                        org.assertj.core.groups.Tuple.tuple(1013, "User Analytics", 101));
    }

    @Test
    @DisplayName("Both open-ended and future-dated projects exist")
    void theAwkwardDateShapesArePresent() {
        assertThat(PROJECTS).anyMatch(p -> p.end() == null);
        // A project that has not started yet catches code assuming start_date is past.
        assertThat(PROJECTS).anyMatch(p -> LocalDate.parse(p.start()).getYear() >= 2026);
    }
}
