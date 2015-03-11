package co.mewf.humpty.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;

import com.moandjiezana.toml.Toml;

/**
 * Builds a {@link Pipeline} configured via the default TOML file.
 */
public class HumptyFilter extends HttpServlet {

  private Pipeline pipeline;
  private String urlPattern;
  private Configuration configuration;
  private Toml cacheToml;
  
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    
    this.pipeline = (Pipeline) config.getServletContext().getAttribute(Pipeline.class.getName());
    this.urlPattern = (String) config.getServletContext().getAttribute(HumptyServletContextInitializer.class.getName());
    this.configuration = (Configuration) config.getServletContext().getAttribute(Configuration.class.getName());
    
    InputStream inputStream = getClass().getResourceAsStream("/" + configuration.getGlobalOptions().getWatchFile().toString());
    if (inputStream != null) {
      try (InputStream is = inputStream) {
        this.cacheToml = new Toml().parse(is);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
    if (this.cacheToml == null) {
      this.cacheToml = new Toml();
    }
  }
  
  @Override
  protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
    String requestUri = httpRequest.getRequestURI();
    
    String assetUri = requestUri.substring(urlPattern.length() + 1);
    
    int fingerprintIndex = assetUri.indexOf("-humpty");
    if (fingerprintIndex > -1) {
      assetUri = assetUri.substring(0, fingerprintIndex) + "." + FilenameUtils.getExtension(assetUri);
    }
    
    String cachePath = cacheToml.getString("\"" + assetUri + "\"");
    String processedAsset;
    if (cachePath != null) {
      processedAsset = new String(Files.readAllBytes(configuration.getGlobalOptions().getBuildDir().resolve(cachePath)), StandardCharsets.UTF_8);
    } else {
      processedAsset = pipeline.process(assetUri).getAsset();
    }

    if (assetUri.endsWith(".js")) {
      httpResponse.setContentType("text/javascript");
    } else if (assetUri.endsWith(".css")) {
      httpResponse.setContentType("text/css");
    }
    
    httpResponse.getWriter().write(processedAsset);
  }
}
