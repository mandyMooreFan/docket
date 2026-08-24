package com.mbeebe.docket.company;

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

/**
 * Company edits, in the archive (§11.1) and staying put at Termination (§11.2).
 *
 * <p>An edit to a Company page is the member's own act and their own personal data
 * — who, when, from what, to what — so it is theirs to take. It is also the record
 * that makes vandalism answerable (§6.1, §10.5), on a page that belongs to nobody,
 * and it is the same shape as a Recommendation they wrote: authored into somebody
 * else's record, where removing it would leave a hole in a history other people
 * rely on. So there is deliberately no {@code Departure} in this package — the
 * absence is the decision, and this javadoc is where it is written down.
 *
 * <p>For a logo edit the values are image ids rather than words, which is what the
 * rows hold; the archive says so rather than pretending otherwise.
 */
@Component
@Order(60)
class CompanyExport implements ExportContributor {

    private final CompanyEditRepository edits;
    private final CompanyRepository companies;
    private final Clock clock;

    CompanyExport(CompanyEditRepository edits, CompanyRepository companies, Clock clock) {
        this.edits = edits;
        this.companies = companies;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        return List.of(ExportSection.of("company_edits", "Company pages you edited",
                "These stay on the company's history if you delete your account, "
                        + "marked as made by a former member.",
                edits.findByMemberIdOrderByIdAsc(memberId).stream()
                        .map(edit -> ExportRecord.of(companyName(edit), List.of(
                                ExportField.of("company", "Company", companyName(edit)),
                                ExportField.of("field", "What you changed",
                                        edit.field().name().toLowerCase(java.util.Locale.UK)),
                                ExportField.of("from", "From", edit.oldValue()),
                                ExportField.of("to", "To", edit.newValue()),
                                ExportField.of("edited_on", "Edited on",
                                        ExportDates.at(edit.editedAt(), clock)))))
                        .toList()));
    }

    private String companyName(CompanyEdit edit) {
        return companies.findById(edit.companyId()).map(Company::name).orElse("");
    }
}
