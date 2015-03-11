package co.mewf.humpty.servlet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import org.junit.Test;
import org.webjars.WebJarAssetLocator;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.HumptyBootstrap;

public class HumptyFilterTest {

  private final Pipeline pipeline = new HumptyBootstrap("/humpty-production.toml", new WebJarAssetLocator(new HashSet<>(Arrays.asList("HumptyFilterTest/app1.css", "HumptyFilterTest/app2.css", "HumptyFilterTest/blocks.js", "HumptyFilterTest/modifiable.css")))).createPipeline();

  @Test
  public void should_handle_digest_bundle_name() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getAttribute(Pipeline.class.getName())).thenReturn(pipeline);
    when(servletContext.getAttribute(Configuration.class.getName())).thenReturn(Configuration.load("humpty-production.toml"));
    when(servletContext.getAttribute(HumptyServletContextInitializer.class.getName())).thenReturn("/humpty");
    ServletConfig servletConfig = mock(ServletConfig.class);
    when(servletConfig.getServletContext()).thenReturn(servletContext);
    
    
    HumptyFilter filter = new HumptyFilter();
    filter.init(servletConfig);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/humpty/tags-humptya2e5f9bda12.css");
    StringWriter writer = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(writer));
    
    filter.doGet(request, response);
    
    String expected = new String(Files.readAllBytes(Paths.get("src", "test", "resources", "HumptyFilterTest", "app1.css")), UTF_8);
    expected += "\n";
    expected += new String(Files.readAllBytes(Paths.get("src", "test", "resources", "HumptyFilterTest", "app2.css")), UTF_8);
    
    assertEquals(expected.trim(), writer.toString().trim());
  }
}
