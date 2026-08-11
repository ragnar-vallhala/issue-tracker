package com.its.web.controller;

import com.its.web.client.ApiException;
import com.its.web.client.ItsApiClient;
import com.its.web.session.SessionAccessor;
import com.its.web.session.SessionUser;
import com.its.web.view.Views.CommentView;
import com.its.web.view.Views.IssueView;
import com.its.web.view.Views.ProjectView;
import com.its.web.view.Views.UserView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The issue detail page and its actions (SRS 7.5).
 *
 * <p>Shared by both roles, so it sits outside {@code /owner} and {@code /assignee} and is
 * not covered by the role interceptor. What each role may do is decided from the session
 * and rendered accordingly - and enforced, properly, by the Issue Service.
 */
@Controller
public class IssueController {

    private final ItsApiClient api;
    private final SessionAccessor sessionAccessor;

    public IssueController(ItsApiClient api, SessionAccessor sessionAccessor) {
        this.api = api;
        this.sessionAccessor = sessionAccessor;
    }

    /** Issue detail with its comment thread (FR-UI-18, FR-UI-19). */
    @GetMapping("/issues/{issueId}")
    public String detail(@PathVariable Integer issueId,
                         HttpServletRequest request,
                         Model model) {

        SessionUser user = currentUser(request);

        IssueView issue = api.getIssue(issueId);
        List<CommentView> comments = api.commentsByIssue(issueId);

        // Names for ids: three calls total for the page, not one per comment (NFR-01).
        Map<Integer, String> userNames = api.listUsers().stream()
                .collect(Collectors.toMap(UserView::userId, UserView::name,
                        (first, second) -> first, LinkedHashMap::new));

        ProjectView project = api.getProject(issue.projectId());

        model.addAttribute("issue", issue);
        model.addAttribute("project", project);
        model.addAttribute("comments", comments);
        model.addAttribute("userNames", userNames);
        model.addAttribute("user", user);

        // An Assignee sees a status control offering only legal transitions, and every
        // other field read-only (FR-UI-17). An Owner gets the full edit link.
        model.addAttribute("canEditEverything", user.isProjectOwner());
        model.addAttribute("canChangeStatus",
                user.isProjectOwner() || user.userId().equals(issue.assigneeId()));
        model.addAttribute("transitions", Options.transitionsFrom(issue.status()));

        return "issue/detail";
    }

    /**
     * An Assignee's status change (FR-UI-17).
     *
     * <p>Sends only {@code status}. The Issue Service rejects an Assignee who touches
     * anything else (FR-ISS-07), so submitting the whole issue back would fail for a
     * caller this form exists to serve.
     */
    @PostMapping("/issues/{issueId}/status")
    public String changeStatus(@PathVariable Integer issueId,
                               @RequestParam String status,
                               RedirectAttributes flash) {

        try {
            api.updateIssue(issueId, Map.of("status", status));
            flash.addFlashAttribute("flash", "Status updated.");

        } catch (ApiException ex) {
            if (ex.getStatus().value() == 409) {
                flash.addFlashAttribute("error", ex.messageOrDefault(
                        "That status change is not allowed."));
            } else if (ex.getStatus().value() == 403) {
                flash.addFlashAttribute("error",
                        "You may only update the status of issues assigned to you.");
            } else {
                throw ex;
            }
        }

        return "redirect:/issues/" + issueId;
    }

    @PostMapping("/issues/{issueId}/comments")
    public String addComment(@PathVariable Integer issueId,
                             @RequestParam String body,
                             HttpServletRequest request,
                             RedirectAttributes flash) {

        SessionUser user = currentUser(request);

        if (body == null || body.isBlank()) {
            flash.addFlashAttribute("error", "A comment cannot be empty.");
            return "redirect:/issues/" + issueId;
        }

        api.addComment(issueId, user.userId(), body.trim());
        flash.addFlashAttribute("flash", "Comment added.");

        return "redirect:/issues/" + issueId;
    }

    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Integer commentId,
                                @RequestParam Integer issueId,
                                RedirectAttributes flash) {

        try {
            api.deleteComment(commentId);
            flash.addFlashAttribute("flash", "Comment deleted.");

        } catch (ApiException ex) {
            if (ex.getStatus().value() == 403) {
                flash.addFlashAttribute("error", "You can only delete your own comments.");
            } else {
                throw ex;
            }
        }

        return "redirect:/issues/" + issueId;
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return sessionAccessor.current(request).orElseThrow();
    }
}
