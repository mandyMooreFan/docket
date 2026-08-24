package com.mbeebe.docket.identity;

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
 * Identity's slice of the archive (§11.1): the account itself, and where it is
 * signed in.
 *
 * <p>The age fact is here in the shape §9.3 stores it, and that shape is the
 * point: an adult's row says "adult, declared on a date" and nothing more, because
 * Docket holds no birth date for any adult member. A member who exports expecting
 * to find their date of birth finds the absence instead, which is the honest
 * answer and a better one.
 *
 * <p>Deliberately absent: session tokens and magic-link tokens. They are stored as
 * hashes, they are credentials rather than facts about a person, and putting a
 * live credential into a file that leaves the building would be a self-inflicted
 * wound. The session <em>list</em> — the same list §3.3 already shows — is here.
 */
@Component
@Order(10)
class AccountExport implements ExportContributor {

    private final MemberRepository members;
    private final MemberSessionRepository sessions;
    private final Clock clock;

    AccountExport(MemberRepository members, MemberSessionRepository sessions, Clock clock) {
        this.members = members;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportSection> sectionsFor(long memberId) {
        return members.findById(memberId)
                .map(member -> List.of(account(member), signedIn(member)))
                .orElseGet(List::of);
    }

    private ExportSection account(Member member) {
        return ExportSection.of("account", "Your account", List.of(ExportRecord.of("",
                List.of(ExportField.of("member_id", "Member number", member.id()),
                        ExportField.of("email", "Email address", member.email()),
                        ExportField.of("age_band", "Age band",
                                member.isMinor() ? "16 or 17 when declared" : "Adult"),
                        ExportField.of("age_declared_on", "Age declared on",
                                member.ageDeclaredOn())))));
    }

    private ExportSection signedIn(Member member) {
        return ExportSection.of("sessions", "Where you are signed in",
                "Devices with a live session. Signing out anywhere removes its row.",
                sessions.findByMemberOrderByLastUsedAtDesc(member).stream()
                        .map(session -> ExportRecord.of(session.client(), List.of(
                                ExportField.of("client", "Client", session.client()),
                                ExportField.of("started", "Started",
                                        ExportDates.at(session.createdAt(), clock)),
                                ExportField.of("last_used", "Last used",
                                        ExportDates.at(session.lastUsedAt(), clock)))))
                        .toList());
    }
}
