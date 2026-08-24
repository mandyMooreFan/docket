package com.mbeebe.docket.company;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.profile.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
class CompanyEditingService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK);

    enum EditOutcome { SAVED, BLANK_NAME, NAME_TAKEN }

    private final CompanyRepository companies;
    private final CompanyEditRepository edits;
    private final ProfileService profiles;
    private final Clock clock;

    CompanyEditingService(CompanyRepository companies, CompanyEditRepository edits,
                          ProfileService profiles, Clock clock) {
        this.companies = companies;
        this.edits = edits;
        this.profiles = profiles;
        this.clock = clock;
    }

    /**
     * Apply an edit, one history row per changed field (§6.1). The name may not fork
     * an existing Company: two entities with one name is what the reuse-first
     * autocomplete exists to prevent, and a same-name pair that really is the same
     * employer is a merge (§10.5), not a rename.
     */
    @Transactional
    EditOutcome edit(long companyId, long editorId, String name, String description) {
        Company company = companies.findById(companyId).orElseThrow();
        String newName = name.strip();
        String newDescription = description.strip();
        if (newName.isEmpty()) {
            return EditOutcome.BLANK_NAME;
        }
        Optional<Company> sameName = companies.findByNameIgnoringCase(newName);
        if (sameName.isPresent() && !sameName.get().id().equals(company.id())) {
            return EditOutcome.NAME_TAKEN;
        }
        if (!newName.equals(company.name())) {
            record(company, editorId, CompanyEdit.Field.NAME, company.name(), newName);
            company.rename(newName);
        }
        if (!newDescription.equals(company.description())) {
            record(company, editorId, CompanyEdit.Field.DESCRIPTION,
                    company.description(), newDescription);
            company.describe(newDescription);
        }
        return EditOutcome.SAVED;
    }

    @Transactional
    void setLogo(long companyId, long editorId, long imageId) {
        Company company = companies.findById(companyId).orElseThrow();
        record(company, editorId, CompanyEdit.Field.LOGO,
                company.logoImageId() == null ? "" : String.valueOf(company.logoImageId()),
                String.valueOf(imageId));
        company.setLogo(imageId);
    }

    /**
     * The page's history, newest first (§6.1) — who changed what, when, from what to
     * what. The editor's name is shown only as far as their own Dial admits this
     * viewer (§8.5); the full answer always exists in the stored rows for
     * moderation (#38).
     */
    @Transactional(readOnly = true)
    List<CompanyEditEntry> history(long companyId, Optional<Member> viewer) {
        return edits.findByCompanyIdOrderByIdDesc(companyId).stream()
                .map(edit -> new CompanyEditEntry(
                        edit.memberId(),
                        profiles.pageFor(edit.memberId(), viewer)
                                .filter(page -> page.named())
                                .map(page -> page.name())
                                .orElse("A member"),
                        edit.field().name().toLowerCase(Locale.ROOT),
                        edit.oldValue(), edit.newValue(),
                        DAY.format(LocalDate.ofInstant(edit.editedAt(), ZoneId.systemDefault()))))
                .toList();
    }

    private void record(Company company, long editorId, CompanyEdit.Field field,
                        String oldValue, String newValue) {
        edits.save(new CompanyEdit(company.id(), editorId, field, oldValue, newValue,
                clock.instant()));
    }
}
