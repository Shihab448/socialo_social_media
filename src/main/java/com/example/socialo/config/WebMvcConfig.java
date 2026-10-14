package com.example.socialo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/** MVC configuration: auth interceptor, static uploads, CORS for WS. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired private AuthInterceptor authInterceptor;

    @Value("${app.upload.dir:src/main/resources/static/uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/home",
                        "/profile", "/profile/**",
                        "/edit-profile",
                        "/photos", "/videos", "/saved",
                        "/create-post", "/post/**",
                        "/story/**",
                        "/friends", "/friends/**",
                        "/follow/**", "/unfollow/**",
                        "/notifications", "/notifications/**",
                        "/chat", "/chat/**",
                        "/search",
                        "/settings", "/settings/**",
                        "/report", "/report/**",
                        "/help",
                        "/user-profile/**",
                        "/block/**", "/unblock/**",
                        "/block-ajax/**", "/unblock-ajax/**",
                        "/api/**",
                        "/reels", "/reels/**"
                )
                .excludePathPatterns(
                        "/",
                        "/login", "/logout",
                        "/signup",
                        "/verify-email", "/verify-email/**",
                        "/forgot-password",
                        "/reset-password",
                        "/appeal",
                        "/css/**", "/js/**",
                        "/images/**", "/uploads/**",
                        "/admin/**",
                        "/ws/**",          // WebSocket endpoint
                        "/error"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "file:" + System.getProperty("user.dir") + "/" + uploadDir + "/");
    }
}
