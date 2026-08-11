package com.its.issue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.its.issue.client.CommentGateway;
import com.its.issue.client.ReferenceGateway;
import com.its.issue.dto.request.IssueUpdateRequest;
import com.its.issue.entity.Issue;
import com.its.issue.entity.IssueType;
import com.its.issue.entity.Priority;
import com.its.issue.entity.Status;
import com.its.issue.exception.IssueExceptions.ForbiddenOperationException;
import com.its.issue.exception.IssueExceptions.IllegalStateTransitionException;
import com.its.issue.repository.IssueRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** The two rules that govern how an issue may change: who may change it, and to what. */
@ExtendWith(MockitoExtension.class)
class IssueWorkflowTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ReferenceGateway referenceGateway;

    @Mock
    private CommentGateway commentGateway;

    private IssueServiceImpl service() {
        return new IssueServiceImpl(issueRepository, referenceGateway, commentGateway);
    }

    private Issue issue(Status status, Integer assigneeId) {
        Issue issue = new Issue("Profile cache not updating after changes",
                "Profile update fails to cache changes.", 1011, assigneeId, 101,
                status, Priority.HIGH, IssueType.BUG, 2, "Sprint 42", "profile,cache");
        ReflectionTestUtils.setField(issue, "issueId", 1);
        return issue;
    }

    private IssueUpdateRequest statusOnly(Status status) {
        return new IssueUpdateRequest(null, null, null, null, status,
                null, null, null, null, null);
    }

    @Nested
    @DisplayName("Status transitions (FR-ISS-14)")
    class Transitions {

        @Test
        @DisplayName("Ordinary transitions are unconstrained")
        void allowsOrdinaryTransitions() {
            Issue issue = issue(Status.TO_DO, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));
            when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

            service().update(1, statusOnly(Status.IN_PROGRESS), CallerIdentity.anonymous());

            assertThat(issue.getStatus()).isEqualTo(Status.IN_PROGRESS);
        }

        @Test
        @DisplayName("An issue cannot slide from DONE back into IN_PROGRESS")
        void refusesTransitionOutOfDone() {
            Issue issue = issue(Status.DONE, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));

            assertThatThrownBy(() -> service().update(
                    1, statusOnly(Status.IN_PROGRESS), CallerIdentity.anonymous()))
                    .isInstanceOf(IllegalStateTransitionException.class)
                    .hasMessageContaining("reopened to TO_DO");

            assertThat(issue.getStatus()).isEqualTo(Status.DONE);
            verify(issueRepository, never()).save(any());
        }

        @Test
        @DisplayName("A DONE issue can be explicitly reopened to TO_DO")
        void allowsExplicitReopen() {
            Issue issue = issue(Status.DONE, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));
            when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

            // Reopening stays possible - it just has to be deliberate, so that it reads
            // as a decision rather than an ordinary status nudge.
            service().update(1, statusOnly(Status.TO_DO), CallerIdentity.anonymous());

            assertThat(issue.getStatus()).isEqualTo(Status.TO_DO);
        }

        @Test
        @DisplayName("Setting DONE to DONE again is a no-op, not a conflict")
        void allowsIdempotentDone() {
            Issue issue = issue(Status.DONE, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));
            when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service().update(
                    1, statusOnly(Status.DONE), CallerIdentity.anonymous()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Assignee restrictions (FR-ISS-07)")
    class AssigneeRules {

        private final CallerIdentity carlos = CallerIdentity.of(104, "ASSIGNEE");

        @Test
        @DisplayName("An Assignee may change the status of their own issue")
        void assigneeMayChangeOwnStatus() {
            Issue issue = issue(Status.TO_DO, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));
            when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

            service().update(1, statusOnly(Status.IN_PROGRESS), carlos);

            assertThat(issue.getStatus()).isEqualTo(Status.IN_PROGRESS);
        }

        @Test
        @DisplayName("An Assignee may not change any other field")
        void assigneeMayNotChangeOtherFields() {
            Issue issue = issue(Status.TO_DO, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));

            IssueUpdateRequest sneaky = new IssueUpdateRequest(
                    null, null, null, null, Status.IN_PROGRESS,
                    Priority.CRITICAL, null, null, null, null);

            assertThatThrownBy(() -> service().update(1, sneaky, carlos))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("only update the status");

            assertThat(issue.getPriority()).isEqualTo(Priority.HIGH);
            verify(issueRepository, never()).save(any());
        }

        @Test
        @DisplayName("An Assignee may not touch an issue assigned to somebody else")
        void assigneeMayNotTouchOthersIssues() {
            Issue issue = issue(Status.TO_DO, 102);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));

            // Without this check any Assignee could close every issue in the system,
            // since a bare status change passes the field restriction.
            assertThatThrownBy(() -> service().update(1, statusOnly(Status.DONE), carlos))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("assigned to them");

            assertThat(issue.getStatus()).isEqualTo(Status.TO_DO);
        }

        @Test
        @DisplayName("A Project Owner is not restricted")
        void ownerMayChangeAnything() {
            Issue issue = issue(Status.TO_DO, 104);
            when(issueRepository.findById(1)).thenReturn(Optional.of(issue));
            when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

            IssueUpdateRequest full = new IssueUpdateRequest(
                    "Rewritten summary", null, null, null, Status.IN_REVIEW,
                    Priority.CRITICAL, null, 5, null, null);

            service().update(1, full, CallerIdentity.of(101, "PROJECT_OWNER"));

            assertThat(issue.getSummary()).isEqualTo("Rewritten summary");
            assertThat(issue.getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(issue.getStoryPoints()).isEqualTo(5);
        }
    }
}
