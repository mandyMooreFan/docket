package com.mbeebe.docket.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Identity's verdict on an email address something is about to write to. One call,
 * so that the plausibility rule, §3.3's blocked-public-inbox security rule and the
 * "is this already a Member" question cannot drift apart between the signup door
 * and the Invite (§13.3) — the third outbound-mail source (§14.2).
 *
 * <p>{@link Verdict#ALREADY_A_MEMBER} is the dangerous one. It is §8.3's rejected
 * membership oracle held in a variable, and it must never reach a person in any
 * form: not as copy, not as a status, not as a difference in what a page renders,
 * and not as a difference in which limits a request consumes. A caller answers it
 * exactly as it answers {@link Verdict#STRANGER} and only changes whether it
 * puts anything in the post.
 */
@Service
public class Addresses {

    /** Deliberately loose: §3.3 validates nothing beyond "there is an @ and a dot". */
    private static final Pattern PLAUSIBLE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public enum Verdict {

        /** Nobody has this address on Docket — the only verdict an Invite may mail. */
        STRANGER,

        NOT_AN_ADDRESS,

        /** §3.3: a world-readable mailbox is an account anyone could open. */
        BLOCKED_DOMAIN,

        /** Server-side only. Answer it exactly as you answer {@link #STRANGER}. */
        ALREADY_A_MEMBER
    }

    private final MemberRepository members;
    private final BlockedEmailDomains blockedDomains;

    Addresses(MemberRepository members, BlockedEmailDomains blockedDomains) {
        this.members = members;
        this.blockedDomains = blockedDomains;
    }

    @Transactional(readOnly = true)
    public Verdict check(String email) {
        if (email == null || !PLAUSIBLE.matcher(email).matches()) {
            return Verdict.NOT_AN_ADDRESS;
        }
        if (blockedDomains.blocks(email)) {
            return Verdict.BLOCKED_DOMAIN;
        }
        return members.findByEmailIgnoreCase(email).isPresent()
                ? Verdict.ALREADY_A_MEMBER
                : Verdict.STRANGER;
    }
}
