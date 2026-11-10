package pe.edu.upeu.fia.infra;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Only fingerprinted Angular assets can be retained across deployments. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CachePolicy extends OncePerRequestFilter {
    private static final String IMMUTABLE = "public, max-age=31536000, immutable";
    private static final String PRIVATE = "no-store, max-age=0";

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean fingerprinted = path.matches("/portal/(?:main|styles|polyfills|chunk)-[A-Za-z0-9_-]{8,}\\.(?:js|css)")
                && ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()));
        String policy = fingerprinted ? IMMUTABLE : PRIVATE;
        response.setHeader("Cache-Control", policy);
        if (!fingerprinted) response.setHeader("Pragma", "no-cache");
        chain.doFilter(request, response);
        if (!response.isCommitted()) response.setHeader("Cache-Control", policy);
    }
}
