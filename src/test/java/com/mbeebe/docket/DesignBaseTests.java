package com.mbeebe.docket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The design language base (SPEC.md §2): tokens, base layout, the app bar,
 * and the AGPL §13 Source link that must be reachable from every page.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DesignBaseTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    MockMvc mvc;

    @Test
    void everyPageCarriesTheSourceLink() throws Exception {
        for (String page : new String[] {"/", "/styleguide"}) {
            mvc.perform(get(page))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            containsString("https://github.com/mandyMooreFan/docket")));
        }
    }

    @Test
    void appBarCarriesTheWordmarkAndFourNavItems() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(stringContainsInOrder(
                        "Docket", "Feed", "Jobs", "Messages", "Network")));
    }

    @Test
    void tokensStylesheetIsServedWithBothThemes() throws Exception {
        mvc.perform(get("/css/docket.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--i900: #101418")))
                .andExpect(content().string(containsString("--g0: #0D1117")));
    }

    @Test
    void vendoredFontsAreServed() throws Exception {
        for (String font : new String[] {
                "/fonts/source-serif-4-latin.woff2",
                "/fonts/libre-franklin-latin.woff2"}) {
            mvc.perform(get(font)).andExpect(status().isOk());
        }
    }

    @Test
    void styleguideStatesTheRuleTheLanguageTurnsOn() throws Exception {
        mvc.perform(get("/styleguide"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Serif for anything a person wrote")));
    }

    @Test
    void unknownRoutesRenderTheDesignedErrorPage() throws Exception {
        mvc.perform(get("/error")
                        .accept(org.springframework.http.MediaType.TEXT_HTML)
                        .requestAttr("jakarta.servlet.error.status_code", 404)
                        .requestAttr("jakarta.servlet.error.request_uri", "/nowhere"))
                .andExpect(content().string(containsString("docket.css")))
                .andExpect(content().string(containsString("There is no page here")));
    }
}
