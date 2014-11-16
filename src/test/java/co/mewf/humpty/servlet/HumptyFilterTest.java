package co.mewf.humpty.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.webjars.WebJarAssetLocator;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.HumptyBootstrap;

public class HumptyFilterTest {

  private final Pipeline pipeline = new HumptyBootstrap("/humpty-production.toml").createPipeline();

  @Test
  public void should_handle_digest_bundle_name() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    
    HumptyFilter filter = new HumptyFilter(pipeline, "/humpty", Configuration.Mode.DEVELOPMENT);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/humpty/tags-humptya2e5f9bda12.css");
    StringWriter writer = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(writer));
    
    filter.doFilter(request, response, mock(FilterChain.class));
    
    WebJarAssetLocator locator = new WebJarAssetLocator();
    String expected = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(locator.getFullPath("app1.css")));
    expected += "\n";
    expected += IOUtils.toString(getClass().getClassLoader().getResourceAsStream(locator.getFullPath("app2.css")));
    
    assertEquals(expected.trim(), writer.toString().trim());
  }
}
