package com.ajaxjs.framework.spring.filter;

import com.ajaxjs.util.StrUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 避免乱码
 */
public class UTF8CharsetFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    static String PUT = "PUT";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(StrUtil.UTF8_SYMBOL);
        response.setCharacterEncoding(StrUtil.UTF8_SYMBOL);

        chain.doFilter(new GetPutData((HttpServletRequest) request), response);
    }

    @Override
    public void destroy() {
    }
}
