package com.mbeebe.docket.profile;

import java.util.List;

/** The edit page's view of your own Profile — stored facts, never the entity. */
public record ProfileEdit(String name, String headline, String location, String summary,
                          Profile.Dial dial, Profile.OpenToWork openToWork,
                          List<PositionView> positions, List<EducationView> education,
                          List<SkillView> skills, String initials, Long photoImageId) {

    public boolean hasPhoto() {
        return photoImageId != null;
    }
}
