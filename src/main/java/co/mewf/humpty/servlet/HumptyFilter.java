package co.mewf.humpty.servlet;

import java.io.IOException;
import java.util.Optional;

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

/**
 * Builds a {@link Pipeline} configured via the default TOML file.
 */
public class HumptyFilter implements Filter {

  private final Pipeline pipeline;
  private String urlPattern;
  private ServletCache cache;
  
  public HumptyFilter(Pipeline pipeline, String urlPattern) {
    this.pipeline = pipeline;
    Optional<FileWatchingServletCache> optionalCache = pipeline.getPipelineListener(FileWatchingServletCache.class);
    this.cache = optionalCache.isPresent() ? optionalCache.get() : new NoopServletCache(pipeline);
    this.urlPattern = urlPattern.substring(0, urlPattern.length() - 2);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = ((HttpServletResponse) response);
    String servletPath = httpRequest.getServletPath();
    String requestUri = httpRequest.getRequestURI();
    
    String assetUri = requestUri.substring(urlPattern.length() + 1);
    
    int fingerprintIndex = assetUri.indexOf("-humpty");
    if (fingerprintIndex > -1) {
      assetUri = assetUri.substring(0, fingerprintIndex) + "." + FilenameUtils.getExtension(assetUri);
    }
    
    String processedAsset = cache.get(assetUri);

    if (assetUri.endsWith(".js")) {
      httpResponse.setContentType("text/javascript");
    } else if (assetUri.endsWith(".css")) {
      httpResponse.setContentType("text/css");
    }
    
    httpResponse.getWriter().write(processedAsset);
  }

  @Override
  public void destroy() {}
}
