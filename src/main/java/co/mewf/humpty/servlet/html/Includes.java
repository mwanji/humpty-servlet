package co.mewf.humpty.servlet.html;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import co.mewf.humpty.config.Bundle;
import co.mewf.humpty.digest.DigestPipelineListener;

public class Includes {

  private final List<Bundle> bundles;
  private final DigestPipelineListener digest;
  private final String urlRoot;

  public Includes(DigestPipelineListener digest, String contextPath, String urlPattern) {
    this.digest = digest;
    this.urlRoot = (contextPath + urlPattern).replaceFirst("//", "/");
    this.bundles = null;
  }
  
  public Includes(List<Bundle> bundles, String contextPath, String urlPattern) {
    this.bundles = bundles;
    this.urlRoot = (contextPath + urlPattern).replaceFirst("//", "/");
    this.digest = null;
  }
  
  public String generate(String bundleName) {
    if (digest != null) {
      return toHtml(urlRoot, digest.getDigest(bundleName));
    }

    Bundle bundle = bundles.stream().filter(b -> b.accepts(bundleName)).findFirst().orElseThrow(() -> new IllegalArgumentException("No bundle defined with name: " + bundleName));
    
    return bundle.stream().map(asset -> toHtml(urlRoot, bundle.getName() + "/" + asset)).collect(Collectors.joining("\n"));
  }
  
  private String toHtml(String contextPath, String expandedAsset) {
    StringBuilder html = new StringBuilder();
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
    
    return html.toString();
  }
}
