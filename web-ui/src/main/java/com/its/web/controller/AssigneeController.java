package com.its.web.controller;

import com.its.web.client.ItsApiClient;
import com.its.web.session.SessionAccessor;
import com.its.web.session.SessionUser;
import com.its.web.view.Views.DashboardSummary;
import com.its.web.view.Views.IssueView;
import com.its.web.view.Views.ProjectView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The Assignee's pages (SRS 7.4). Reachable only by ASSIGNEE (FR-UI-03). */
@Controller
@RequestMapping("/assignee")
public class AssigneeController {

    private final ItsApiClient api;
    private final SessionAccessor sessionAccessor;

    public AssigneeController(ItsApiClient api, SessionAccessor sessionAccessor) {
        this.api = api;
        this.sessionAccessor = sessionAccessor;
    }

    /**
     * Assignee dashboard (FR-UI-15).
     *
     * <p>Reached through {@code /api/issues/assignee/{id}} rather than by fetching every
     * issue and filtering here - the service owns that query, and pulling the whole table
     * across the network to discard most of it would not scale past the sample data.
     */
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        SessionUser user = currentUser(request);
        List<IssueView> issues = api.issuesByAssignee(user.userId());

        Map<String, List<IssueView>> grouped = issues.stream()
                .collect(Collectors.groupingBy(
                        issue -> issue.status() == null ? "UNKNOWN" : issue.status(),
                        LinkedHashMap::new, Collectors.toList()));

        // Ordered by workflow, and including the empty columns - a board that hides its
        // empty states makes "nothing in review" look like a rendering failure.
        Map<String, List<IssueView>> board = new LinkedHashMap<>();
        Options.statuses().keySet().forEach(status ->
                board.put(status, grouped.getOrDefault(status, List.of())));

        model.addAttribute("board", board);
        model.addAttribute("statusLabels", Options.statuses());
        model.addAttribute("issues", issues);
        model.addAttribute("summary", summarise(issues));
        model.addAttribute("projectNames", projectNames());
        model.addAttribute("currentSprintCount", issues.stream()
                .filter(issue -> issue.sprint() != null && !issue.sprint().isBlank())
                .count());
        model.addAttribute("user", user);

        return "assignee/dashboard";
    }

    /** The full list, with client-side filters (FR-UI-16). */
    @GetMapping("/issues")
    public String issues(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String priority,
                         @RequestParam(required = false) Integer projectId,
                         HttpServletRequest request,
                         Model model) {

        SessionUser user = currentUser(request);
        List<IssueView> issues = api.issuesByAssignee(user.userId());

        List<IssueView> filtered = issues.stream()
                .filter(issue -> status == null || status.isBlank()
                        || status.equals(issue.status()))
                .filter(issue -> priority == null || priority.isBlank()
                        || priority.equals(issue.priority()))
                .filter(issue -> projectId == null || projectId.equals(issue.projectId()))
                .toList();

        model.addAttribute("issues", filtered);
        model.addAttribute("totalCount", issues.size());
        model.addAttribute("statuses", Options.statuses());
        model.addAttribute("priorities", Options.priorities());
        model.addAttribute("projectNames", projectNames());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedProjectId", projectId);
        model.addAttribute("user", user);

        return "assignee/issues";
    }

    private SessionUser currentUser(HttpServletRequest request) {
        return sessionAccessor.current(request).orElseThrow();
    }

    /** One call, so issue rows can show a project name without a lookup each. */
    private Map<Integer, String> projectNames() {
        return api.listProjects().stream()
                .collect(Collectors.toMap(ProjectView::projectId, ProjectView::projectName,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private DashboardSummary summarise(List<IssueView> issues) {
        Map<String, Long> byStatus = issues.stream()
                .filter(issue -> issue.status() != null)
                .collect(Collectors.groupingBy(IssueView::status,
                        LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> byPriority = issues.stream()
                .filter(issue -> issue.priority() != null)
                .collect(Collectors.groupingBy(IssueView::priority,
                        LinkedHashMap::new, Collectors.counting()));

        long projectCount = issues.stream()
                .map(IssueView::projectId)
                .distinct()
                .count();

        return new DashboardSummary((int) projectCount, issues.size(), byStatus, byPriority);
    }
}
