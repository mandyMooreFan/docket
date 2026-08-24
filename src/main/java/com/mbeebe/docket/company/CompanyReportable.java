package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.moderation.ReportableContent;
import com.mbeebe.docket.moderation.TargetKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * The company module's answer to {@link ReportableContent} (§10.2, §10.3): the
 * COMPANY kind, and only that one — §10.3's "joke/spam entities are cleaned up
 * reactively".
 *
 * <p>A Company page is on the open web (§6.1, §8.4) and no Dial governs any of
 * it, because a Company is never an actor with a Dial to turn. So the ordinary
 * visibility rule is simply "it exists and has not been removed", and
 * {@link #visibleToReporter} asks the same question the page's own controller
 * does. An absorbed Company is still reportable at its own id: the row survives
 * a merge (§10.5) and the vandalism worth reporting may be the merge itself.
 *
 * <p>{@link ReportedItem#authorId()} is empty here, and that emptiness is the
 * point: a Company page is written by many hands and named by none, so there is
 * nobody answerable for it and the ladder's member-facing rungs — Withdrawal,
 * Suspension, Termination — simply do not apply. Page vandalism is answerable
 * from the edit history against the Member who made the edit, not from here.
 */
@Component
class CompanyReportable implements ReportableContent {

    private final CompanyRepository companies;

    CompanyReportable(CompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> visibleToReporter(TargetKind kind, long id,
                                                    Optional<Member> viewer) {
        return kind == TargetKind.COMPANY
                ? companies.findById(id).filter(company -> !company.removed())
                        .map(CompanyReportable::item)
                : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportedItem> forModeration(TargetKind kind, long id) {
        return kind == TargetKind.COMPANY
                ? companies.findById(id).map(CompanyReportable::item)
                : Optional.empty();
    }

    @Override
    @Transactional
    public boolean remove(TargetKind kind, long id, Instant now) {
        if (kind != TargetKind.COMPANY) {
            return false;
        }
        companies.findById(id).ifPresent(company -> company.remove(now));
        return true;
    }

    @Override
    @Transactional
    public boolean restore(TargetKind kind, long id) {
        if (kind != TargetKind.COMPANY) {
            return false;
        }
        companies.findById(id).ifPresent(Company::restore);
        return true;
    }

    private static ReportedItem item(Company company) {
        return new ReportedItem(TargetKind.COMPANY, company.id(), Optional.empty(),
                (company.name() + "\n\n" + company.description()).strip(),
                "/companies/" + company.id(), company.removed());
    }
}
