package com.its.web.config;

import com.its.web.interceptor.AuthInterceptor;
import com.its.web.interceptor.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Interceptor wiring and the handful of view-only routes.
 *
 * <p>The exclusion list is the entire public surface of this application: sign-up, login,
 * logout, the error pages and static assets. Everything else requires a session by
 * default (FR-UI-01).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, RoleInterceptor roleInterceptor) {
        this.authInterceptor = authInterceptor;
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/", "/login", "/logout", "/signup",
                        "/error", "/error/**",
                        // Health must answer without a session: it is what start-all.sh
                        // and any container probe poll, and a 302 to the login page is
                        // not a health check.
                        "/actuator/**",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico")
                .order(1);

        // Only the role-scoped areas; the shared issue pages are reachable by both roles
        // and decide what to show from the session instead.
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/owner/**", "/assignee/**")
                .order(2);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/error/403").setViewName("error/403");
        registry.addViewController("/error/404").setViewName("error/404");
        registry.addViewController("/error/500").setViewName("error/500");
        registry.addViewController("/error/503").setViewName("error/503");
    }
}
