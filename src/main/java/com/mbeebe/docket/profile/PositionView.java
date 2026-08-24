package com.mbeebe.docket.profile;

/** One dated entry as a template renders it; company is empty when there is none. */
public record PositionView(long id, String title, String company, Long companyId, String when,
                           String description, boolean current) {

    public boolean atCompany() {
        return !company.isEmpty();
    }
}
