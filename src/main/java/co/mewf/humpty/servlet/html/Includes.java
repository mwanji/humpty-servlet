package co.mewf.humpty.servlet.html;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;

import co.mewf.humpty.config.Bundle;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.servlet.HumptyServletContextInitializer;
import co.mewf.humpty.spi.listeners.PipelineListener;
import co.mewf.humpty.spi.resolvers.Resolver;
import co.mewf.humpty.spi.resolvers.WebJarResolver;

public class Includes implements PipelineListener {

  private Configuration configuration;
  private List<? extends Resolver> resolvers;
  private Configuration.Mode mode;
  private final Map<String, String> bundleFingerprints = new HashMap<>();
  private String contextPath;
  private String urlPattern;

  @Override
  public String getName() {
    return "servlet";
  }
  
  @Override
  public void onAssetProcessed(String asset, String name, String assetPath, Bundle bundle) {}
  
  @Override
  public void onBundleProcessed(String bundle, String bundleName) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("md5");
      String fingerprint = DatatypeConverter.printHexBinary(messageDigest.digest(bundle.getBytes()));
      String fingerprintedBundleName = FilenameUtils.getBaseName(bundleName) + "-humpty" + fingerprint + "." + FilenameUtils.getExtension(bundleName);
      
      bundleFingerprints.put(bundleName, fingerprintedBundleName);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
  
  public String generate(String bundleName) {
    StringBuilder html = new StringBuilder();

    Bundle bundle = null;
    for (Bundle candidate : configuration.getBundles()) {
      if (candidate.accepts(bundleName)) {
        bundle = candidate;
        break;
      }
    }
    
    String urlRoot = (contextPath + urlPattern).replaceFirst("//", "/");

    if (Configuration.Mode.PRODUCTION == mode) {
      toHtml(urlRoot, bundleFingerprints.get(bundleName), html);

      return html.toString();
    }

    for (String asset : bundle.getBundleFor(bundleName)) {
      toHtml(urlRoot, bundle.getName() + "/" + asset, html);
    }

    return html.toString();
  }
  
  @Inject
  public void configure(Configuration configuration, Configuration.Mode mode, ServletContext servletContext) {
    this.configuration = configuration;
    Configuration.Options options = configuration.getOptionsFor(this);
    this.urlPattern = options.get("urlPattern", HumptyServletContextInitializer.DEFAULT_URL_PATTERN);
    this.mode = mode;
    this.resolvers = Collections.singletonList(new WebJarResolver());
    this.contextPath = servletContext.getContextPath();
  }

  private void toHtml(String contextPath, String expandedAsset, StringBuilder html) {
    String assetBaseName = expandedAsset;
    if (assetBaseName.endsWith(".js")) {
      html.append("<script src=\"");
    } else if (assetBaseName.endsWith(".css")) {
      html.append("<link rel=\"stylesheet\" href=\"");
    }
    html.append(contextPath);
    if (html.charAt(html.length() - 1) != '/') {
      html.append('/');
    }
    html.append(FilenameUtils.getPath(expandedAsset));
    html.append(FilenameUtils.getBaseName(expandedAsset));
    html.append('.').append(FilenameUtils.getExtension(expandedAsset));
    if (assetBaseName.endsWith(".js")) {
      html.append("\"></script>");
    } else if (assetBaseName.endsWith(".css")) {
      html.append("\" />");
    }
    html.append("\n");
  }
}
