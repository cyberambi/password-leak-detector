package com.passwordleakdetector.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Only used when the frontend is bundled into this app's static resources
 * (the Render deployment build - see Dockerfile.render). Forwards the SPA's
 * client-side routes to index.html so a hard refresh/direct link on e.g.
 * /history doesn't 404. The bare "/" route is already served by Spring
 * Boot's built-in static welcome-page handling, and /api/** is handled by
 * the REST controllers, so neither needs listing here.
 */
@Controller
public class SpaForwardController {

    @RequestMapping({"/register", "/login", "/dashboard", "/history"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
