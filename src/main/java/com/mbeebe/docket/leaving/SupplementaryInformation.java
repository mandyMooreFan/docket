package com.mbeebe.docket.leaving;

import java.util.List;

/**
 * The Article 15 half of the one button (§11.1).
 *
 * <p>Article 20 portability hands over a copy of what you provided. Article 15
 * access is the bigger right and has no lawful-basis gate: it reaches
 * third-party-authored data about you, and it obliges the controller to supply
 * <em>supplementary information</em> — purposes, recipients, retention, source,
 * and the rights that exist ({@code docs/data-rights.md} §4). §11.1's decision is
 * that members do not know the difference and should not have to, so both arrive
 * in the same archive from the same button.
 *
 * <p>The words live here rather than in the template because the JSON and the
 * readable page must say the same thing, and two copies of a compliance statement
 * is one copy too many. {@code MessagingService.CLOSED_NOTE} is the same call.
 *
 * <p><strong>Honest where it has to be.</strong> Two items below say something
 * uncomfortable rather than nothing. The lawful bases are undetermined — the spec
 * marks it ⚠️ at §15.2 and {@code data-rights.md} §8 lists it as unverified — and
 * an archive that invented one would be worse than an archive that says so. And
 * the derived things (Capabilities, effective visibility) are named as absent with
 * the reason, because "we left something out" discovered later reads as
 * concealment, while "here is what is not in here, and why" is the answer.
 */
final class SupplementaryInformation {

    /** One heading and its paragraph, as it appears on the page and in the JSON. */
    record Item(String key, String heading, String text) {
    }

    private SupplementaryInformation() {
    }

    static List<Item> items() {
        return List.of(
                new Item("what_this_is", "What this is",
                        "A copy of everything Docket has stored about you, taken at the moment "
                                + "you asked for it. It covers both of the rights people usually "
                                + "mean at once: a portable copy of what you gave us, and access "
                                + "to what we hold about you — including things other people "
                                + "wrote."),
                new Item("purposes", "What we use your data for",
                        "Running Docket, and nothing else. Your profile is the page you publish. "
                                + "Your posts and replies go to the people you are connected to. "
                                + "Your messages go to the person you sent them to. Your "
                                + "applications go to the person who posted the job. Your email "
                                + "address signs you in, and sends you the job alerts you asked "
                                + "for. Your age is used to work out which protections apply to "
                                + "you, and for nothing else, ever."),
                new Item("recipients", "Who else sees it",
                        "Other members, exactly as far as you chose: your profile setting decides "
                                + "who can read your profile and your posts; a message reaches one "
                                + "person; an application reaches the person who posted the job. "
                                + "Docket does not sell data, does not share it with advertisers, "
                                + "and has no advertisers to share it with. It reaches our hosting "
                                + "and email providers only in the course of running the service."),
                new Item("retention", "How long we keep it",
                        "While your account exists. If you delete your account, your profile and "
                                + "the things you published on your own go straight away. What "
                                + "stays is described on the page you deleted from: your side of "
                                + "each conversation, the recommendations you wrote, the replies "
                                + "you left under other people's posts — all marked as written by "
                                + "a former member. Deleted data is put beyond use in our backups "
                                + "until those backups roll over on their normal schedule, and "
                                + "then it is gone from those too."),
                new Item("source", "Where it came from",
                        "You typed nearly all of it. The rest came from what you did here — when "
                                + "you connected to someone, when you applied, when you sent a "
                                + "message. Two kinds of thing were written by other people: "
                                + "recommendations written about you, and the outcomes on your "
                                + "applications. Both are in this archive."),
                new Item("not_in_here", "What is not in here, and why",
                        "Docket does not store conclusions. Whether your profile is complete, who "
                                + "can currently see it, and what you are allowed to do are all "
                                + "worked out fresh every time they are asked, from the facts in "
                                + "this archive — they are never written down anywhere, so there "
                                + "is no stored copy of them to give you. Data of that kind is "
                                + "outside the portability right as a category. We are telling you "
                                + "rather than quietly leaving it out."),
                new Item("lawful_basis", "The legal basis",
                        "Being straight with you: Docket has not yet finished determining which "
                                + "legal basis each part of this processing rests on, and it has "
                                + "not been reviewed by a lawyer. That work is not done, and this "
                                + "archive is not going to pretend otherwise."),
                new Item("your_rights", "What you can do next",
                        "You can correct anything wrong on your profile by editing it. You can "
                                + "delete your account at any time, and you never have to download "
                                + "this first. You can complain to us about how we have handled "
                                + "your data — we will acknowledge it within 30 days and tell you "
                                + "what we decided — and you can complain to the Information "
                                + "Commissioner's Office whether or not you complain to us."));
    }
}
