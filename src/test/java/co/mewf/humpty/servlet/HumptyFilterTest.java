package co.mewf.humpty.servlet;

import static com.github.kevinsawicki.http.HttpRequest.get;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.webjars.WebJarAssetLocator;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.mjeanroy.junit.servers.jetty.EmbeddedJetty;
import com.github.mjeanroy.junit.servers.jetty.EmbeddedJettyConfiguration;
import com.github.mjeanroy.junit.servers.rules.JettyServerRule;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.HumptyBootstrap;

public class HumptyFilterTest {

  @ClassRule
  public static final JettyServerRule SERVER = new JettyServerRule(new EmbeddedJetty(EmbeddedJettyConfiguration.builder().withPath("/ctxPath").withWebapp("src/test/webapp").build()));
  
  @ClassRule
  public static final JettyServerRule SERVER_NO_CTX_PATH = new JettyServerRule(new EmbeddedJetty(EmbeddedJettyConfiguration.builder().withWebapp("src/test/webapp").build()));
  
  private static final String APP_CSS;
  private static final String TAGS_BUNDLE;
  
  static {
    try {
      APP_CSS = new String(Files.readAllBytes(Paths.get("src", "test", "resources", "HumptyFilterTest", "app1.css")), UTF_8).trim();
      TAGS_BUNDLE = (APP_CSS + "\n" + "\n"
        + new String(Files.readAllBytes(Paths.get("src", "test", "resources", "HumptyFilterTest", "app2.css")), UTF_8)).trim();
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void should_handle_digested_bundle_without_context_path() throws Exception {
    assertBody(TAGS_BUNDLE, get(SERVER_NO_CTX_PATH.getUrl() + "humpty/tags-humptya2e5f9bda12.css"));
  }
  
  @Test
  public void should_handle_digested_bundle_with_context_path() throws Exception {
    assertBody(TAGS_BUNDLE, get(SERVER.getUrl() + "/humpty/tags-humptya2e5f9bda12.css"));
  }
  
  @Test
  public void should_handle_bundle_with_context_path() throws Exception {
    assertBody(TAGS_BUNDLE, get(SERVER.getUrl() + "/humpty/tags.css"));
  }
  
  @Test
  public void should_handle_bundle_without_context_path() throws Exception {
    assertBody(TAGS_BUNDLE, get(SERVER_NO_CTX_PATH.getUrl() + "humpty/tags.css"));
  }
  
  @Test
  public void should_handle_asset_with_context_path() throws Exception {
    assertBody(APP_CSS, get(SERVER.getUrl() + "/humpty/tags.css/app1.css"));
  }
  
  @Test
  public void should_handle_asset_without_context_path() throws Exception {
    assertBody(APP_CSS, get(SERVER_NO_CTX_PATH.getUrl() + "humpty/tags.css/app1.css"));
  }
  
  private void assertBody(String expected, HttpRequest request) {
    assertEquals(expected, request.body().trim());
  }
}
