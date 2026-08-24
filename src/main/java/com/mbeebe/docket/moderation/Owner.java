package com.mbeebe.docket.moderation;

import com.mbeebe.docket.identity.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Who reviews (§10.1): the owner, one person, working a reactive queue.
 *
 * <p>Configuration rather than a role table, and that is the honest shape rather than a
 * shortcut. §10.1 decided "one person, one timezone" as an accepted cost and said
 * plainly that "appointing moderators later is a change to this decision, made when
 * there are people to appoint". A roles table would quietly pre-build the thing that
 * decision deferred; an address in configuration cannot be handed out by accident.
 *
 * <p>Unset means nobody. The queue then 404s for everyone, which is the right failure:
 * an instance with no named owner has no one to answer a Report, and pretending
 * otherwise would put the §10.1 statement in the product's mouth untruthfully.
 */
@Service
class Owner {

    private final String ownerEmail;

    Owner(@Value("${docket.owner-email:}") String ownerEmail) {
        this.ownerEmail = ownerEmail == null ? "" : ownerEmail.trim();
    }

    boolean is(Member member) {
        return !ownerEmail.isBlank() && ownerEmail.equalsIgnoreCase(member.email());
    }

    boolean is(Optional<Member> member) {
        return member.filter(this::is).isPresent();
    }

    /**
     * The published contact for illegal content seen logged-out (§10.2) and for
     * authorities (§15.3's DSA Arts. 11/12 contact points). The same address, named as
     * such, because there is one person and inventing a second inbox would imply a
     * second desk.
     */
    Optional<String> publishedAddress() {
        return ownerEmail.isBlank() ? Optional.empty() : Optional.of(ownerEmail);
    }
}
