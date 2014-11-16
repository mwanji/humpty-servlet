package co.mewf.humpty.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.Configuration.Mode;

/**
 * Builds a {@link Pipeline} configured via the default TOML file.
 */
public class HumptyFilter implements Filter {

  private final Pipeline pipeline;
  private final String urlPattern;
  private final Mode mode;
  
  public HumptyFilter(Pipeline pipeline, String urlPattern, Configuration.Mode mode) {
    this.pipeline = pipeline;
    this.mode = mode;
    this.urlPattern = urlPattern;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = ((HttpServletResponse) response);
    String requestUri = httpRequest.getRequestURI();
    
    String assetUri = requestUri.substring(urlPattern.length() + 1);
    
    int fingerprintIndex = assetUri.indexOf("-humpty");
    if (fingerprintIndex > -1) {
      assetUri = assetUri.substring(0, fingerprintIndex) + "." + FilenameUtils.getExtension(assetUri);
    }
    
    String processedAsset = pipeline.process(assetUri);

    if (assetUri.endsWith(".js")) {
      httpResponse.setContentType("text/javascript");
    } else if (assetUri.endsWith(".css")) {
      httpResponse.setContentType("text/css");
    }
    
    if (mode == Configuration.Mode.PRODUCTION) {
      
    }
    
    httpResponse.getWriter().write(processedAsset);
  }
  
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void destroy() {}
}
