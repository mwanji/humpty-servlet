package co.mewf.humpty.servlet.html;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import co.mewf.humpty.config.Configuration;

import com.moandjiezana.toml.Toml;

public class IncludesTest {
  private final Configuration configuration = Mockito.spy(Configuration.load("/humpty-development.toml"));
  private final Configuration.GlobalOptions globalOptions = Mockito.spy(configuration.getGlobalOptions());
  private final Toml digestProduction = new Toml().parse(getClass().getResourceAsStream("/" + globalOptions.getDigestFile().toString()));
  private ClassLoader originalClassLoader;
  
  @Before
  public void before() {
    Mockito.when(configuration.getGlobalOptions()).thenReturn(globalOptions);
  }
  
  @Test
  public void should_unbundle_assets_in_dev_mode() {
    Mockito.when(globalOptions.getDigestFile()).thenReturn(Paths.get("unknown"));
    Includes includes = new Includes(configuration, "/context", "/humpty");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/context/humpty/tags.js/jquery.js\"></script>\n<script src=\"/context/humpty/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/context/humpty/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/context/humpty/tags.css/app2.css\" />", cssInclude);
  }

  @Test
  public void should_handle_root_context_path_in_dev_mode() {
    Mockito.when(globalOptions.getDigestFile()).thenReturn(Paths.get("unknown"));
    Includes includes = new Includes(configuration, "/", "/humpty");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/humpty/tags.js/jquery.js\"></script>\n<script src=\"/humpty/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/humpty/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/humpty/tags.css/app2.css\" />", cssInclude);
  }
  
  @Test
  public void should_use_custom_url_pattern() throws Exception {
    Mockito.when(globalOptions.getDigestFile()).thenReturn(Paths.get("unknown"));
    Includes includes = new Includes(configuration, "/ctx", "/custom");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");
    
    assertEquals("<script src=\"/ctx/custom/tags.js/jquery.js\"></script>\n<script src=\"/ctx/custom/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/custom/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/ctx/custom/tags.css/app2.css\" />", cssInclude);
  }

  @Test
  public void should_bundle_assets_in_production_mode() throws Exception {
    Includes includes = new Includes(configuration, "/context", "/humpty");
    
    String jsHtml = includes.generate("tags.js");
    String cssHtml = includes.generate("tags.css");

    assertEquals("<script src=\"/context/humpty/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsHtml);
    assertEquals("<link rel=\"stylesheet\" href=\"/context/humpty/" + digestProduction.getString("\"tags.css\"") + "\" />", cssHtml);
  }

  @Test
  public void should_handle_root_context_path_in_production_mode() throws Exception {
    Includes includes = new Includes(configuration, "/", "/humpty");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/humpty/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/humpty/" + digestProduction.getString("\"tags.css\"") + "\" />", cssInclude);
  }
  
  @Test
  public void should_use_custom_url_pattern_in_production_mode() throws Exception {
    Includes includes = new Includes(configuration, "/ctx", "/custom");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/ctx/custom/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/custom/" + digestProduction.getString("\"tags.css\"") + "\" />", cssInclude);
  }
  
  @Test
  public void should_handle_empty_url_pattern() throws Exception {
    Includes includes = new Includes(configuration, "/ctx", "");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/ctx/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/" + digestProduction.getString("\"tags.css\"") + "\" />", cssInclude);
  }
  
  @Test
  public void should_add_live_reload_if_watch_file_present() throws Exception {
    Mockito.when(globalOptions.getWatchFile()).thenReturn(Paths.get("humpty-watch.fake"));
    
    Path watchFile = Paths.get("src/test/resources/humpty-watch.toml");
    try {
      Files.createFile(watchFile);
      Includes includes = new Includes(configuration, "/", "/assets");
      
      String liveReloadInclude = includes.generateLiveReload();
      
      assertEquals("<script src=\"http://localhost:8765/liveReload.js\"></script>", liveReloadInclude);
    } finally {
      Files.deleteIfExists(watchFile);
    }
  }
  
  @Test
  public void should_not_add_live_reload_if_watch_file_absent() throws Exception {
    Includes includes = new Includes(configuration, "/", "/assets");
    
    String liveReloadInclude = includes.generateLiveReload();
    
    assertEquals("", liveReloadInclude);
  }
}
