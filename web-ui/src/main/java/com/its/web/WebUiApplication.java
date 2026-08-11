package com.its.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * The JSP web application (SRS section 7).
 *
 * <p>The only human-facing component. It holds sessions, renders pages and binds forms;
 * it holds no business rules and opens no database connection. Everything it knows comes
 * from the API gateway, which means a rule can never drift between what the UI enforces
 * and what the system enforces - there is only one copy of each (SRS C-07).
 *
 * <p>Extends {@link SpringBootServletInitializer} because this module is packaged as a
 * WAR; see the note in pom.xml for why JSP leaves no alternative.
 */
@SpringBootApplication
public class WebUiApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(WebUiApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(WebUiApplication.class, args);
    }
}
