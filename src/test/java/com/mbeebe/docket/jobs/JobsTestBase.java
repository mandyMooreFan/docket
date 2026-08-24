package com.mbeebe.docket.jobs;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.mbeebe.docket.company.Companies;
import com.mbeebe.docket.graph.GraphTestBase;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Jobs-suite plumbing on top of the graph's connect/complete machinery: members
 * with a current Position at a named Company, the §6.2 work verification that
 * completes the trust gate, and posting itself. The Postgres container and
 * GreenMail are shared across every suite in the run, so every email here is
 * prefixed "jobs-", every company name is suite-unique, every company gets its
 * own mail domain (a shared domain would auto-merge them, §6.1), and every
 * assertion is row- or mailbox-scoped, never a global count.
 */
public abstract class JobsTestBase extends GraphTestBase {

    private static final Pattern VERIFY_LINK = Pattern.compile("/verify/([A-Za-z0-9_-]+)");
    private static final Pattern JOB_URL = Pattern.compile("/jobs/(\\d+)");

    @Autowired
    protected Companies companies;

    /** A complete member whose Profile claims a current Position at the Company. */
    protected Cookie employeeAt(String email, String name, String companyName) throws Exception {
        Cookie session = signUpAndIn(email);
        mvc.perform(post("/profile/basics").cookie(session)
                        .param("name", name).param("headline", "A headline")
                        .param("location", "").param("summary", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/profile/positions").cookie(session)
                        .param("title", "A role").param("company", companyName)
                        .param("startMonth", "1").param("startYear", "2020")
                        .param("description", ""))
                .andExpect(status().is3xxRedirection());
        return session;
    }

    /** Runs the §6.2 flow end to end: request the link at the domain, consume it. */
    protected void verifyWorkAt(Cookie session, long companyId, String address) throws Exception {
        mvc.perform(post("/companies/" + companyId + "/verify").cookie(session)
                        .param("address", address))
                .andExpect(status().is3xxRedirection());
        var messages = greenMail.getReceivedMessages();
        String body = GreenMailUtil.getBody(messages[messages.length - 1]);
        Matcher matcher = VERIFY_LINK.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("No verify link in mail body:\n" + body);
        }
        mvc.perform(post("/verify").param("token", matcher.group(1)))
                .andExpect(status().is3xxRedirection());
    }

    /** A member holding the whole §6.3 gate: current Position + Work verification. */
    protected Cookie posterAt(String email, String name, String companyName, String domain)
            throws Exception {
        Cookie session = employeeAt(email, name, companyName);
        verifyWorkAt(session, companies.named(companyName).id(), "someone@" + domain);
        return session;
    }

    /** Posts a job with sane defaults and returns its id from the redirect. */
    protected long postJob(Cookie session, long companyId, String title) throws Exception {
        return postJob(session, companyId, title, "45000", "60000", "GBP");
    }

    protected long postJob(Cookie session, long companyId, String title,
                           String min, String max, String currency) throws Exception {
        String redirect = mvc.perform(post("/jobs").cookie(session)
                        .param("companyId", String.valueOf(companyId))
                        .param("title", title)
                        .param("location", "Leeds")
                        .param("remotePolicy", "HYBRID")
                        .param("salaryMin", min)
                        .param("salaryMax", max)
                        .param("currency", currency)
                        .param("description", "Real work for real pay."))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        Matcher matcher = JOB_URL.matcher(redirect);
        if (!matcher.find()) {
            throw new AssertionError("Posting did not land on a posting page: " + redirect);
        }
        return Long.parseLong(matcher.group(1));
    }

    /**
     * Every message GreenMail holds for one recipient — mailbox-scoped, never
     * global. Decoded via getContent(), not getBody(): quoted-printable soft
     * line breaks would otherwise split the very phrases under assertion.
     */
    protected List<String> mailBodiesFor(String recipient) throws Exception {
        List<String> bodies = new ArrayList<>();
        for (MimeMessage message : greenMail.getReceivedMessages()) {
            for (var address : message.getAllRecipients()) {
                if (address.toString().equals(recipient)) {
                    bodies.add(message.getContent().toString());
                }
            }
        }
        return bodies;
    }
}
