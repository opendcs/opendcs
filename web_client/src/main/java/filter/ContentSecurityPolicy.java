package filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

/**
 * Servlet Filter implementation class ContentSecurityPolicy
 */
@WebFilter("/*")
public class ContentSecurityPolicy implements Filter {
    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
            FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);  /* Let request continue chain filter */
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }
}
