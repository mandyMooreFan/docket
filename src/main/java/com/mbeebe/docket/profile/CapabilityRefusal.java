package com.mbeebe.docket.profile;

/**
 * What a gate says when it refuses, in the product's own voice.
 *
 * <p>This exists because of one sentence in §10.3: "a capability never earned and a
 * capability withdrawn are different states, and the member is told which they are in".
 * Before moderation, every gate could hard-code the same remedy — finish your Profile —
 * because that was the only reason a gate ever said no. It is now one of two, and the
 * other one has a completely different remedy: not "do more", but "appeal, once".
 *
 * <p>Telling a Member with a complete Profile to go and complete it would be worse than
 * unhelpful. It would be the product lying to them about their own account, which is
 * the same objection §10.3 raises against shadowbanning and refuses on the same
 * grounds. Keeping both sentences in one place is what stops the eight gates drifting
 * apart on it.
 */
public final class CapabilityRefusal {

    private CapabilityRefusal() {
    }

    /**
     * The sentence for a refusal, or empty when the answer was not a refusal at all.
     *
     * @param whatItIs how the gate names the thing in ordinary words — "Posting",
     *                 "Messaging", "Replying" — so the sentence reads as that page's
     *                 own rather than as a generic error.
     */
    public static String sentence(CapabilityAnswer answer, String whatItIs) {
        return switch (answer) {
            case YES -> "";
            case NOT_YET_EARNED -> whatItIs + " opens when your profile is complete.";
            case WITHDRAWN -> whatItIs + " was withdrawn by a moderation decision. "
                    + "Your profile is not the problem and finishing it will not bring "
                    + "this back — see /appeals for what was decided and why.";
        };
    }
}
