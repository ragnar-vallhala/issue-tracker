package com.its.web.controller;

import com.its.web.client.ApiException;
import com.its.web.client.ItsApiClient;
import com.its.web.form.IssueForm;
import com.its.web.form.ProjectForm;
import com.its.web.session.SessionAccessor;
import com.its.web.session.SessionUser;
import com.its.web.view.Views.DashboardSummary;
import com.its.web.view.Views.IssueView;
import com.its.web.view.Views.ProjectView;
import com.its.web.view.Views.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The Project Owner's pages (SRS 7.3). Reachable only by PROJECT_OWNER (FR-UI-03). */
@Controller
@RequestMapping("/owner")
public class OwnerController {

    private final ItsApiClient api;
    private final SessionAccessor sessionAccessor;

    public OwnerController(ItsApiClient api, SessionAccessor sessionAccessor) {
        this.api = api;
        this.sessionAccessor = sessionAccessor;
    }

    /**
     * Owner dashboard (FR-UI-08).
     *
     * <p>The figures are derived from the two lists shown on the page, so a total can
     * never disagree with the rows beneath it.
     */
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        SessionUser user = currentUser(request);

        List<ProjectView> projects = api.listProjectsByOwner(user.userId());

        // One call per project. Acceptable at this scale, and honest: there is no
        // aggregate endpoint, and inventing a count in this tier would be a fiction.
        List<IssueView> issues = new ArrayList<>();
        Map<Integer, Integer> issueCounts = new LinkedHashMap<>();
        for (ProjectView project : projects) {
            List<IssueView> projectIssues = api.issuesByProject(project.projectId());
            issueCounts.put(project.projectId(), projectIssues.size());
            issues.addAll(projectIssues);
        }

        model.addAttribute("projects", projects);
        model.addAttribute("issueCounts", issueCounts);
        model.addAttribute("recentIssues", issues.stream()
                .sorted((a, b) -> compareUpdated(b, a))
                .limit(8)
                .toList());
        model.addAttribute("summary", summarise(projects.size(), issues));
        model.addAttribute("user", user);

        return "owner/dashboard";
    }

    // -------------------------------------------------------------- projects

    @GetMapping("/projects")
    public String projects(HttpServletRequest request, Model model) {
        SessionUser user = currentUser(request);
        List<ProjectView> projects = api.listProjectsByOwner(user.userId());

        Map<Integer, Integer> issueCounts = new LinkedHashMap<>();
        for (ProjectView project : projects) {
            issueCounts.put(project.projectId(), api.issuesByProject(project.projectId()).size());
        }

        model.addAttribute("projects", projects);
        model.addAttribute("issueCounts", issueCounts);
        model.addAttribute("user", user);

        return "owner/projects";
    }

    @GetMapping("/projects/new")
    public String newProject(HttpServletRequest request, Model model) {
        model.addAttribute("projectForm", new ProjectForm());
        model.addAttribute("mode", "create");
        model.addAttribute("user", currentUser(request));
        return "owner/project-form";
    }

    @PostMapping("/projects")
    public String createProject(@Valid @ModelAttribute("projectForm") ProjectForm form,
                                BindingResult binding,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes flash) {

        SessionUser user = currentUser(request);
        model.addAttribute("mode", "create");
        model.addAttribute("user", user);

        if (!validateProject(form, binding)) {
            return "owner/project-form";
        }

        try {
            // The owner comes from the session, never from the form.
            ProjectView created = api.createProject(form.getProjectName(), user.userId(),
                    form.getStartDate().toString(),
                    form.getEndDate() == null ? null : form.getEndDate().toString());

            flash.addFlashAttribute("flash", "Project \"" + created.projectName() + "\" created.");
            return "redirect:/owner/projects";

        } catch (ApiException ex) {
            applyApiErrors(ex, binding, "projectName");
            return "owner/project-form";
        }
    }

    @GetMapping("/projects/{projectId}/edit")
    public String editProject(@PathVariable Integer projectId,
                              HttpServletRequest request,
                              Model model) {

        ProjectView project = api.getProject(projectId);

        ProjectForm form = new ProjectForm();
        form.setProjectId(project.projectId());
        form.setProjectName(project.projectName());
        form.setStartDate(project.startDate());
        form.setEndDate(project.endDate());

        model.addAttribute("projectForm", form);
        model.addAttribute("mode", "edit");
        model.addAttribute("user", currentUser(request));

        return "owner/project-form";
    }

    @PostMapping("/projects/{projectId}")
    public String updateProject(@PathVariable Integer projectId,
                                @Valid @ModelAttribute("projectForm") ProjectForm form,
                                BindingResult binding,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes flash) {

        SessionUser user = currentUser(request);
        model.addAttribute("mode", "edit");
        model.addAttribute("user", user);

        if (!validateProject(form, binding)) {
            return "owner/project-form";
        }

        try {
            api.updateProject(projectId, form.getProjectName(), user.userId(),
                    form.getStartDate().toString(),
                    form.getEndDate() == null ? null : form.getEndDate().toString());

            flash.addFlashAttribute("flash", "Project updated.");
            return "redirect:/owner/projects/" + projectId;

        } catch (ApiException ex) {
            applyApiErrors(ex, binding, "projectName");
            return "owner/project-form";
        }
    }

    /**
     * The delete confirmation (FR-UI-11, FR-PRJ-12).
     *
     * <p>Shows the project name and how many issues will be destroyed with it. The
     * cascade is irreversible and reaches two other databases, so it is worth making the
     * consequence explicit before the button rather than after it.
     */
    @GetMapping("/projects/{projectId}/delete")
    public String confirmDelete(@PathVariable Integer projectId,
                                HttpServletRequest request,
                                Model model) {

        ProjectView project = api.getProject(projectId);
        List<IssueView> issues = api.issuesByProject(projectId);

        model.addAttribute("project", project);
        model.addAttribute("issueCount", issues.size());
        model.addAttribute("user", currentUser(request));

        return "owner/project-delete";
    }

    @PostMapping("/projects/{projectId}/delete")
    public String deleteProject(@PathVariable Integer projectId, RedirectAttributes flash) {
        api.deleteProject(projectId);

        flash.addFlashAttribute("flash",
                "Project deleted, along with its issues and their comments.");
        return "redirect:/owner/projects";
    }

    @GetMapping("/projects/{projectId}")
    public String projectDetail(@PathVariable Integer projectId,
                                HttpServletRequest request,
                                Model model) {

        ProjectView project = api.getProject(projectId);
        List<IssueView> issues = api.issuesByProject(projectId);

        model.addAttribute("project", project);
        model.addAttribute("issues", issues);
        model.addAttribute("userNames", nameLookup());
        model.addAttribute("statuses", Options.statuses());
        model.addAttribute("priorities", Options.priorities());
        model.addAttribute("types", Options.types());
        model.addAttribute("user", currentUser(request));

        return "owner/project-detail";
    }

    // ---------------------------------------------------------------- issues

    @GetMapping("/issues/new")
    public String newIssue(@org.springframework.web.bind.annotation.RequestParam(
                                   required = false) Integer projectId,
                           HttpServletRequest request,
                           Model model) {

        IssueForm form = new IssueForm();
        form.setProjectId(projectId);
        form.setStatus("TO_DO");
        form.setPriority("MEDIUM");
        form.setType("TASK");

        populateIssueForm(model, request);
        model.addAttribute("issueForm", form);
        model.addAttribute("mode", "create");

        return "owner/issue-form";
    }

    @PostMapping("/issues")
    public String createIssue(@Valid @ModelAttribute("issueForm") IssueForm form,
                              BindingResult binding,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes flash) {

        SessionUser user = currentUser(request);
        populateIssueForm(model, request);
        model.addAttribute("mode", "create");

        if (binding.hasErrors()) {
            return "owner/issue-form";
        }

        try {
            IssueView created = api.createIssue(form.toCreatePayload(user.userId()));
            flash.addFlashAttribute("flash", "Issue #" + created.issueId() + " created.");
            return "redirect:/issues/" + created.issueId();

        } catch (ApiException ex) {
            applyApiErrors(ex, binding, "summary");
            return "owner/issue-form";
        }
    }

    @GetMapping("/issues/{issueId}/edit")
    public String editIssue(@PathVariable Integer issueId,
                            HttpServletRequest request,
                            Model model) {

        IssueView issue = api.getIssue(issueId);

        IssueForm form = new IssueForm();
        form.setIssueId(issue.issueId());
        form.setSummary(issue.summary());
        form.setDescription(issue.description());
        form.setProjectId(issue.projectId());
        form.setAssigneeId(issue.assigneeId());
        form.setStatus(issue.status());
        form.setPriority(issue.priority());
        form.setType(issue.type());
        form.setStoryPoints(issue.storyPoints());
        form.setSprint(issue.sprint());
        form.setTags(issue.tags());

        populateIssueForm(model, request);
        model.addAttribute("issueForm", form);
        model.addAttribute("mode", "edit");

        return "owner/issue-form";
    }

    @PostMapping("/issues/{issueId}")
    public String updateIssue(@PathVariable Integer issueId,
                              @Valid @ModelAttribute("issueForm") IssueForm form,
                              BindingResult binding,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes flash) {

        populateIssueForm(model, request);
        model.addAttribute("mode", "edit");

        if (binding.hasErrors()) {
            return "owner/issue-form";
        }

        try {
            api.updateIssue(issueId, form.toUpdatePayload());
            flash.addFlashAttribute("flash", "Issue updated.");
            return "redirect:/issues/" + issueId;

        } catch (ApiException ex) {
            if (ex.getStatus().value() == 409) {
                // An illegal status transition (FR-ISS-14).
                binding.rejectValue("status", "illegal", ex.messageOrDefault(
                        "That status change is not allowed."));
                return "owner/issue-form";
            }
            applyApiErrors(ex, binding, "summary");
            return "owner/issue-form";
        }
    }

    @PostMapping("/issues/{issueId}/delete")
    public String deleteIssue(@PathVariable Integer issueId,
                              @org.springframework.web.bind.annotation.RequestParam(
                                      required = false) Integer projectId,
                              RedirectAttributes flash) {

        api.deleteIssue(issueId);
        flash.addFlashAttribute("flash", "Issue deleted, along with its comments.");

        return projectId == null
                ? "redirect:/owner/dashboard"
                : "redirect:/owner/projects/" + projectId;
    }

    // ----------------------------------------------------------------- misc

    private SessionUser currentUser(HttpServletRequest request) {
        return sessionAccessor.current(request).orElseThrow();
    }

    /**
     * The assignee picker holds only ASSIGNEE users (FR-UI-13).
     *
     * <p>Offering Project Owners here would produce a request the Issue Service rejects,
     * since it checks the role of an assignee before accepting one.
     */
    private void populateIssueForm(Model model, HttpServletRequest request) {
        SessionUser user = currentUser(request);

        model.addAttribute("projects", api.listProjectsByOwner(user.userId()));
        model.addAttribute("assignees", api.listAssignees());
        model.addAttribute("statuses", Options.statuses());
        model.addAttribute("priorities", Options.priorities());
        model.addAttribute("types", Options.types());
        model.addAttribute("user", user);
    }

    /**
     * A userId-to-name map, fetched once per page.
     *
     * <p>Resolving each row's assignee individually would be one HTTP call per issue -
     * the N+1 problem with a network hop attached (DESIGN 8.6).
     */
    private Map<Integer, String> nameLookup() {
        return api.listUsers().stream()
                .collect(Collectors.toMap(UserView::userId, UserView::name,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private DashboardSummary summarise(int projectCount, List<IssueView> issues) {
        Map<String, Long> byStatus = issues.stream()
                .filter(issue -> issue.status() != null)
                .collect(Collectors.groupingBy(IssueView::status,
                        LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> byPriority = issues.stream()
                .filter(issue -> issue.priority() != null)
                .collect(Collectors.groupingBy(IssueView::priority,
                        LinkedHashMap::new, Collectors.counting()));

        return new DashboardSummary(projectCount, issues.size(), byStatus, byPriority);
    }

    private boolean validateProject(ProjectForm form, BindingResult binding) {
        if (!form.hasValidDateRange()) {
            binding.rejectValue("endDate", "range",
                    "The end date cannot be before the start date.");
        }
        return !binding.hasErrors();
    }

    /**
     * Maps a service's field errors back onto the form.
     *
     * <p>{@code fallbackField} catches the case where the service rejected something this
     * form has no matching input for - the message still has to land somewhere visible
     * rather than vanishing into a generic error page.
     */
    private void applyApiErrors(ApiException ex, BindingResult binding, String fallbackField) {
        if (ex.fieldErrors().isEmpty()) {
            binding.rejectValue(fallbackField, "api",
                    ex.messageOrDefault("The request was rejected."));
            return;
        }

        ex.fieldErrors().forEach(fieldError -> {
            String field = mapField(fieldError.field());
            if (binding.getFieldError(field) == null) {
                try {
                    binding.rejectValue(field, "api", fieldError.reason());
                } catch (RuntimeException notOnThisForm) {
                    binding.rejectValue(fallbackField, "api", fieldError.reason());
                }
            }
        });
    }

    /** The API's field names mostly match the forms'; owner is the exception. */
    private String mapField(String apiField) {
        return "projectOwnerId".equals(apiField) ? "projectName" : apiField;
    }

    private int compareUpdated(IssueView a, IssueView b) {
        if (a.lastUpdatedOn() == null && b.lastUpdatedOn() == null) {
            return 0;
        }
        if (a.lastUpdatedOn() == null) {
            return -1;
        }
        if (b.lastUpdatedOn() == null) {
            return 1;
        }
        return a.lastUpdatedOn().compareTo(b.lastUpdatedOn());
    }
}
