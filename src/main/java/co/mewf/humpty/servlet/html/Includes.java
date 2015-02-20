package co.mewf.humpty.servlet.html;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import co.mewf.humpty.config.Bundle;

import com.moandjiezana.toml.Toml;

public class Includes {

  private final List<Bundle> bundles;
  private final Toml digest;
  private final String urlRoot;

  public Includes(Toml digest, String contextPath, String urlPattern) {
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
      return toHtml(bundleName, digest.getString("\"" + bundleName + "\""));
    }

    Bundle bundle = bundles.stream().filter(b -> b.accepts(bundleName)).findFirst().orElseThrow(() -> new IllegalArgumentException("No bundle defined with name: " + bundleName));
    
    return bundle.stream().map(asset -> toHtml(bundleName, bundle.getName() + "/" + asset)).collect(Collectors.joining("\n"));
  }
  
  private String toHtml(String bundleName, String expandedAsset) {
    StringBuilder html = new StringBuilder();
    
    if (bundleName.endsWith(".js")) {
      js(expandedAsset, html);
    } else if (bundleName.endsWith(".css")) {
      css(expandedAsset, html);
    }
    
    return html.toString();
  }
  
  private void js(String expandedAsset, StringBuilder html) {
    html.append("<script src=\"")
    .append(urlRoot);
    if (html.charAt(html.length() - 1) != '/') {
      html.append('/');
    }
    html.append(FilenameUtils.getPath(expandedAsset))
      .append(FilenameUtils.getBaseName(expandedAsset))
      .append('.').append(FilenameUtils.getExtension(expandedAsset))
      .append("\"></script>");
  }
  
  private void css(String expandedAsset, StringBuilder html) {
    html.append("<link rel=\"stylesheet\" href=\"")
      .append(urlRoot);
    if (html.charAt(html.length() - 1) != '/') {
      html.append('/');
    }
    html.append(FilenameUtils.getPath(expandedAsset))
      .append(FilenameUtils.getBaseName(expandedAsset))
      .append('.').append(FilenameUtils.getExtension(expandedAsset))
      .append("\" />");
  }
}
