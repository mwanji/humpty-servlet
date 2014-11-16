package co.mewf.humpty.servlet.html;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.security.MessageDigest;

import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.webjars.WebJarAssetLocator;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.HumptyBootstrap;

public class IncludesTest {

  private final WebJarAssetLocator locator = new WebJarAssetLocator();

  @Test
  public void should_unbundle_assets_in_dev_mode() {
    Pipeline pipeline = new HumptyBootstrap("/humpty-development.toml", servletContext("/context")).createPipeline();
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/context/humpty/tags.js/jquery.js\"></script>\n<script src=\"/context/humpty/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/context/humpty/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/context/humpty/tags.css/app2.css\" />", cssInclude);
  }

  @Test
  public void should_handle_root_context_path_in_dev_mode() {
    Pipeline pipeline = new HumptyBootstrap("/humpty-development.toml", servletContext("/")).createPipeline();
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/humpty/tags.js/jquery.js\"></script>\n<script src=\"/humpty/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/humpty/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/humpty/tags.css/app2.css\" />", cssInclude);
  }
  
  @Test
  public void should_use_custom_url_pattern() throws Exception {
    Pipeline pipeline = new HumptyBootstrap("/should_use_custom_url_pattern.toml", servletContext("/ctx")).createPipeline();
    
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");
    
    assertEquals("<script src=\"/ctx/custom/tags.js/jquery.js\"></script>\n<script src=\"/ctx/custom/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/custom/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/ctx/custom/tags.css/app2.css\" />", cssInclude);
  }

  @Test
  public void should_bundle_assets_in_production_mode() throws Exception {
    Pipeline pipeline = new HumptyBootstrap("/humpty-production.toml", servletContext("/context")).createPipeline();
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    pipeline.process("tags.js");
    pipeline.process("tags.css");
    
    String jsHtml = includes.generate("tags.js");
    String cssHtml = includes.generate("tags.css");

    assertEquals("<script src=\"/context/humpty/tags-humpty" + hash("jquery.min.js", "blocks.js") + ".js\"></script>", jsHtml);
    assertEquals("<link rel=\"stylesheet\" href=\"/context/humpty/tags-humpty" + hash("app1.css", "app2.css") + ".css\" />", cssHtml);
  }

  @Test
  public void should_handle_root_context_path_in_production_mode() throws Exception {
    Pipeline pipeline = new HumptyBootstrap("/humpty-production.toml", servletContext("/")).createPipeline();
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    pipeline.process("tags.js");
    pipeline.process("tags.css");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/humpty/tags-humpty" + hash("jquery.min.js", "blocks.js") + ".js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/humpty/tags-humpty" + hash("app1.css", "app2.css") + ".css\" />", cssInclude);
  }
  
  @Test
  public void should_use_custom_url_pattern_in_production_mode() throws Exception {
    Pipeline pipeline = new HumptyBootstrap("/should_use_custom_url_pattern_in_production_mode.toml", servletContext("/ctx")).createPipeline();
    
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    pipeline.process("tags.js");
    pipeline.process("tags.css");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/ctx/custom/tags-humpty" + hash("jquery.min.js", "blocks.js") + ".js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/custom/tags-humpty" + hash("app1.css", "app2.css") + ".css\" />", cssInclude);
  }
  
  @Test
  public void should_handle_empty_url_pattern() throws Exception {
    Pipeline pipeline = new HumptyBootstrap("/should_handle_empty_url_pattern.toml", servletContext("/ctx")).createPipeline();
    
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
    pipeline.process("tags.js");
    pipeline.process("tags.css");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/ctx/tags-humpty" + hash("jquery.min.js", "blocks.js") + ".js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/tags-humpty" + hash("app1.css", "app2.css") + ".css\" />", cssInclude);
  }

  @Test
  public void should_use_prebuilt_bundle_names_in_external_mode() throws Exception {
    Pipeline pipeline = new HumptyBootstrap("/should_use_prebuilt_bundle_names_in_external_mode.toml", servletContext("/")).createPipeline();
    
    Includes includes = pipeline.getPipelineListener(Includes.class).get();
    
//    pipeline.process("tags.css");
//    pipeline.process("tags.js");
    
    String cssInclude = includes.generate("tags.css");
    String jsInclude = includes.generate("tags.js");

    assertEquals("<link rel=\"stylesheet\" href=\"/assets/tags-humpty123.css\" />", cssInclude);
    assertEquals("<script src=\"/assets/tags-humpty456.js\"></script>", jsInclude);
  }
  private ServletContext servletContext(String contextPath) {
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getContextPath()).thenReturn(contextPath);

    return servletContext;
  }
  
  private String hash(String... paths) throws Exception {
    MessageDigest messageDigest = MessageDigest.getInstance("md5");

    for (String path : paths) {
      try (InputStream is = getClass().getClassLoader().getResourceAsStream(locator.getFullPath(path))) {
        messageDigest.update(IOUtils.toByteArray(is));
        messageDigest.update("\n".getBytes());
      }
    }
    
    return DatatypeConverter.printHexBinary(messageDigest.digest());
  }
}
