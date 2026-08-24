package com.mbeebe.docket.profile;

import java.util.List;

/** What the template gets of a Profile — fully loaded, never an entity (§14.2). */
public record ProfilePage(long memberId, boolean owner, String name, String headline,
                          String location, String summary, String initials, boolean openToWork,
                          List<PositionView> positions, List<EducationView> education,
                          List<SkillView> skills, Completeness completeness,
                          Profile.Dial dial, Profile.OpenToWork openToWorkAudience,
                          boolean indexable) {

    public boolean named() {
        return !name.isBlank();
    }
}
