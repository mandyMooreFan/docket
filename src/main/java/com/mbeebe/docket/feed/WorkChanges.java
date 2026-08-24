package com.mbeebe.docket.feed;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.identity.Members;
import com.mbeebe.docket.profile.Capability;
import com.mbeebe.docket.profile.CapabilityAnswer;
import com.mbeebe.docket.profile.CapabilityService;
import com.mbeebe.docket.profile.WorkChangeAnnouncer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * §5.2.3: the opt-in work-change Post. Reached only through the ticked box in
 * a Profile edit — the seam is not called otherwise, which is what keeps this
 * never-automatic and never-retroactive. The body is fixed at the moment of
 * the edit, like any other Post: it is what was published, and later company
 * renames do not rewrite it.
 */
@Service
public class WorkChanges implements WorkChangeAnnouncer {

    private final PostRepository posts;
    private final Members members;
    private final CapabilityService capabilities;
    private final Clock clock;

    WorkChanges(PostRepository posts, Members members, CapabilityService capabilities,
                Clock clock) {
        this.posts = posts;
        this.members = members;
        this.capabilities = capabilities;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void roleStarted(long memberId, String title, String companyName) {
        announce(memberId, "Started as " + line(title, companyName));
    }

    @Override
    @Transactional
    public void roleEnded(long memberId, String title, String companyName) {
        announce(memberId, "Left " + line(title, companyName));
    }

    private static String line(String title, String companyName) {
        return companyName.isBlank() ? title + "." : title + " at " + companyName + ".";
    }

    private void announce(long memberId, String body) {
        // §3.2: a work-change Post is still a Post. The edit itself always
        // stands; only the share quietly doesn't happen before Completeness.
        if (capabilities.may(memberId, Capability.POST) != CapabilityAnswer.YES) {
            return;
        }
        boolean minor = members.find(memberId).map(Member::isMinor).orElse(false);
        // §9.4: the authored-as-minor fact, fixed here like everywhere else.
        posts.save(new Post(memberId, Post.Kind.WORK_CHANGE, body, minor, clock.instant()));
    }
}
