package com.mbeebe.docket.jobs;

import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.leaving.ExportContributor;
import com.mbeebe.docket.leaving.ExportDates;
import com.mbeebe.docket.leaving.ExportField;
import com.mbeebe.docket.leaving.ExportRecord;
import com.mbeebe.docket.leaving.ExportSection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * The board's slice of the archive (§11.1): Applications <strong>with their
 * Outcomes</strong>, postings this member wrote, and their saved searches.
 *
 * <p>The Outcome is the second thing §11.1 names explicitly, and for the same
 * reason as the Recommendation written about you: the applicant did not provide
 * it, so it is outside Article 20 — the poster recorded it — but it is squarely
 * the applicant's personal data, so Article 15 reaches it
 * ({@code docs/data-rights.md} §2). One button, both rights.
 *
 * <p>§6.4's two facts are kept apart here exactly as the schema keeps them apart:
 * an Outcome the poster made, and the separate record that a posting closed while
 * the Application was untouched. Folding them into one "status" would be the
 * export quietly erasing the fact of the silence, which is the one thing §6.4
 * exists to prevent.
 *
 * <p>The saved search's stop token is deliberately not here. It is a credential
 * that stops mail, not a fact about a person, and §6.5 stores it raw precisely so
 * every email can carry it — an archive is not an email.
 */
@Component
@Order(50)
class JobsExport implements ExportContributor {

    private final JobApplicationRepository applications;
    private final JobPostingRepository postings;
    private final JobSearchRepository searches;
    private final Companies companies;
    private final Clock clock;

    JobsExport(JobApplicationRepository applications, JobPostingRepository postings,
               JobSearchRepository searches, Companies companies, Clock clock) {
        this.applications = applications;
        this.postings = postings;
        this.searches = searches;
        this.companies = companies;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        return List.of(applications(memberId), postings(memberId), searches(memberId));
    }

    private ExportSection applications(long memberId) {
        return ExportSection.of("applications", "Jobs you applied for",
                "What you sent, and what the person who posted the job decided.",
                applications.findByApplicantIdOrderByAppliedAtDescIdDesc(memberId).stream()
                        .map(application -> {
                            Optional<JobPosting> posting =
                                    postings.findById(application.postingId());
                            return ExportRecord.of(
                                    posting.map(JobPosting::title).orElse("A job"),
                                    List.of(
                                            ExportField.of("job_title", "Job",
                                                    posting.map(JobPosting::title).orElse("")),
                                            ExportField.of("company", "Company",
                                                    posting.map(this::companyName).orElse("")),
                                            ExportField.of("applied_on", "You applied on",
                                                    ExportDates.on(application.appliedAt(),
                                                            clock)),
                                            ExportField.of("note", "Your note",
                                                    application.note()),
                                            ExportField.of("outcome", "What they decided",
                                                    outcome(application)),
                                            ExportField.of("outcome_on", "Decided on",
                                                    ExportDates.on(application.outcomeAt(),
                                                            clock)),
                                            ExportField.of("closed_without_response",
                                                    "Closed without an answer",
                                                    ExportDates.on(
                                                            application
                                                                    .closedWithoutResponseAt(),
                                                            clock))));
                        })
                        .toList());
    }

    private ExportSection postings(long memberId) {
        return ExportSection.of("job_postings", "Jobs you posted",
                postings.findByPosterIdOrderByPostedAtDesc(memberId).stream()
                        .map(posting -> ExportRecord.of(posting.title(), List.of(
                                ExportField.of("title", "Title", posting.title()),
                                ExportField.of("company", "Company", companyName(posting)),
                                ExportField.of("location", "Location", posting.location()),
                                ExportField.of("remote_policy", "Remote policy",
                                        posting.remotePolicy()),
                                ExportField.of("salary", "Salary",
                                        posting.currency() + " " + posting.salaryMin()
                                                + "–" + posting.salaryMax()),
                                ExportField.of("description", "Description",
                                        posting.description()),
                                ExportField.of("posted_on", "Posted on",
                                        ExportDates.on(posting.postedAt(), clock)),
                                ExportField.of("closes_on", "Closes on",
                                        ExportDates.on(posting.closesAt(), clock)),
                                ExportField.of("closed_on", "Closed on",
                                        ExportDates.on(posting.closedAt(), clock)))))
                        .toList());
    }

    private ExportSection searches(long memberId) {
        return ExportSection.of("saved_searches", "Your saved searches",
                "The only email Docket sends because you asked it to.",
                searches.findByMemberIdAndStoppedAtIsNullOrderByCreatedAt(memberId).stream()
                        .map(search -> {
                            JobFilters filters = search.filters();
                            return ExportRecord.of("", List.of(
                                    ExportField.of("keyword", "Words", filters.q()),
                                    ExportField.of("location", "Location", filters.location()),
                                    ExportField.of("remote_policy", "Remote policy",
                                            filters.remote()),
                                    ExportField.of("salary_floor", "Salary floor",
                                            filters.floor()),
                                    ExportField.of("company", "Company", filters.company()),
                                    ExportField.of("companies_you_know",
                                            "Companies you know only",
                                            search.knownOnly() ? "Yes" : "No"),
                                    ExportField.of("frequency", "How often",
                                            search.frequency())));
                        })
                        .toList());
    }

    private String companyName(JobPosting posting) {
        return companies.find(posting.companyId()).map(company -> company.name()).orElse("");
    }

    /** §6.4's two facts, kept apart: a decision made, and a silence recorded. */
    private static String outcome(JobApplication application) {
        return switch (application.state()) {
            case RECEIVED -> "No decision yet";
            case ADVANCED -> "Advanced";
            case NOT_SELECTED -> "Not selected";
            case CLOSED_WITHOUT_RESPONSE -> "The posting closed without an answer";
        };
    }
}
