package com.its.user.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.its.user.entity.Role;
import com.its.user.seed.DataSeeder.SeedUser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The seed list stopped being a literal the moment most of it was generated, and a
 * generated row breaks differently from a hand-written one: it fails at start-up, against
 * MySQL, on a column constraint no unit test otherwise touches. A duplicate email or a
 * profile one character over the limit aborts the whole seed and leaves an empty database
 * that looks like a configuration problem.
 *
 * <p>These assertions are the schema's constraints restated in Java, so the failure lands
 * here instead.
 */
class DataSeederTest {

    /** Matches {@code @Column(length = 255)} on name, email and profile. */
    private static final int COLUMN_LIMIT = 255;

    private static final List<SeedUser> USERS = DataSeeder.USERS;

    @Test
    @DisplayName("Ids are unique, contiguous, and start at the workbook's 101")
    void idsAreContiguousFrom101() {
        List<Integer> ids = USERS.stream().map(SeedUser::id).toList();

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids.get(0)).isEqualTo(101);
        assertThat(ids.get(ids.size() - 1)).isEqualTo(100 + USERS.size());
        // Contiguity is what makes the AUTO_INCREMENT reset in run() correct: it takes
        // the last id and adds one.
        assertThat(ids).isSorted();
    }

    @Test
    @DisplayName("Emails are unique - the column is UNIQUE and a clash aborts the seed")
    void emailsAreUnique() {
        assertThat(USERS.stream().map(SeedUser::email).toList()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("No value exceeds its 255-character column")
    void valuesFitTheirColumns() {
        for (SeedUser user : USERS) {
            assertThat(user.name()).hasSizeLessThanOrEqualTo(COLUMN_LIMIT);
            assertThat(user.email()).hasSizeLessThanOrEqualTo(COLUMN_LIMIT);
            if (user.profile() != null) {
                assertThat(user.profile()).hasSizeLessThanOrEqualTo(COLUMN_LIMIT);
            }
        }
    }

    @Test
    @DisplayName("The workbook's four rows are unchanged")
    void workbookRowsAreIntact() {
        assertThat(USERS.subList(0, 4))
                .extracting(SeedUser::id, SeedUser::name, SeedUser::role)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101, "Emily Sinha", Role.PROJECT_OWNER),
                        org.assertj.core.groups.Tuple.tuple(102, "Michael Patel", Role.ASSIGNEE),
                        org.assertj.core.groups.Tuple.tuple(103, "Priya Jackson", Role.PROJECT_OWNER),
                        org.assertj.core.groups.Tuple.tuple(104, "Carlos Singh", Role.ASSIGNEE));
    }

    @Test
    @DisplayName("Generated owners follow the rule the Project Service mirrors")
    void generatedOwnersFollowTheSharedRule() {
        // The Project Service picks owners for its generated projects by recomputing
        // this. If the rule changes here and not there, projects end up owned by
        // Assignees - so the rule is asserted rather than assumed.
        List<Integer> owners = USERS.stream()
                .filter(u -> u.id() >= DataSeeder.FIRST_GENERATED_ID)
                .filter(u -> u.role() == Role.PROJECT_OWNER)
                .map(SeedUser::id)
                .toList();

        assertThat(owners).isNotEmpty();
        for (int ownerId : owners) {
            assertThat((ownerId - DataSeeder.FIRST_GENERATED_ID) % DataSeeder.OWNER_EVERY)
                    .isZero();
        }
    }

    @Test
    @DisplayName("The edge-case fixtures other services depend on are present")
    void edgeCaseFixturesArePresent() {
        // 118 owns nothing and 119 is assigned nothing; both are empty-state fixtures,
        // and both are easy to destroy by "tidying up" a seeder elsewhere.
        assertThat(USERS).anyMatch(u -> u.id() == 118 && u.role() == Role.PROJECT_OWNER);
        assertThat(USERS).anyMatch(u -> u.id() == 119 && u.role() == Role.ASSIGNEE);
        // 120 owns the archive projects the Issue Service loads its bulk into.
        assertThat(USERS).anyMatch(u -> u.id() == 120 && u.role() == Role.PROJECT_OWNER);
        // A null profile has to survive: the column is nullable and the UI must cope.
        assertThat(USERS).anyMatch(u -> u.profile() == null);
    }
}
