package co.mewf.humpty.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;

import co.mewf.humpty.Pipeline;

/**
 * Builds a {@link Pipeline} configured via the default TOML file.
 */
public class HumptyFilter extends HttpServlet {

  private Pipeline pipeline;
  private String urlPattern;
  
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    
    this.pipeline = (Pipeline) config.getServletContext().getAttribute(Pipeline.class.getName());
    this.urlPattern = (String) config.getServletContext().getAttribute(HumptyServletContextInitializer.class.getName());
  }
  
  @Override
  protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
    String requestUri = httpRequest.getRequestURI();
    
    String assetUri = requestUri.substring(urlPattern.length() + 1);
    
    int fingerprintIndex = assetUri.indexOf("-humpty");
    if (fingerprintIndex > -1) {
      assetUri = assetUri.substring(0, fingerprintIndex) + "." + FilenameUtils.getExtension(assetUri);
    }
    
    String processedAsset = pipeline.process(assetUri).getAsset();

    if (assetUri.endsWith(".js")) {
      httpResponse.setContentType("text/javascript");
    } else if (assetUri.endsWith(".css")) {
      httpResponse.setContentType("text/css");
    }
    
    httpResponse.getWriter().write(processedAsset);
  }
}
