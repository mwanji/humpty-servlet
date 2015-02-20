package co.mewf.humpty.servlet.html;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import co.mewf.humpty.config.Configuration;

import com.moandjiezana.toml.Toml;

public class IncludesTest {
  private Toml digestProduction;
  
  @Before
  public void before() throws Exception {
    digestProduction = new Toml().parse(getClass().getResourceAsStream("/humpty-digest.toml"));
  }


  @Test
  public void should_unbundle_assets_in_dev_mode() {
    Includes includes = new Includes(Configuration.load("/humpty-development.toml").getBundles(), "/context", "/humpty");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/context/humpty/tags.js/jquery.js\"></script>\n<script src=\"/context/humpty/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/context/humpty/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/context/humpty/tags.css/app2.css\" />", cssInclude);
  }

  @Test
  public void should_handle_root_context_path_in_dev_mode() {
    Includes includes = new Includes(Configuration.load("/humpty-development.toml").getBundles(), "/", "/humpty");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/humpty/tags.js/jquery.js\"></script>\n<script src=\"/humpty/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/humpty/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/humpty/tags.css/app2.css\" />", cssInclude);
  }
  
  @Test
  public void should_use_custom_url_pattern() throws Exception {
    Includes includes = new Includes(Configuration.load("/humpty-development.toml").getBundles(), "/ctx", "/custom");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");
    
    assertEquals("<script src=\"/ctx/custom/tags.js/jquery.js\"></script>\n<script src=\"/ctx/custom/tags.js/blocks.js\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/custom/tags.css/app1.css\" />\n<link rel=\"stylesheet\" href=\"/ctx/custom/tags.css/app2.css\" />", cssInclude);
  }

  @Test
  public void should_bundle_assets_in_production_mode() throws Exception {
    Includes includes = new Includes(digestProduction, "/context", "/humpty");
    
    String jsHtml = includes.generate("tags.js");
    String cssHtml = includes.generate("tags.css");

    assertEquals("<script src=\"/context/humpty/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsHtml);
    assertEquals("<link rel=\"stylesheet\" href=\"/context/humpty/" + digestProduction.getString("\"tags.css\"") + "\" />", cssHtml);
  }

  @Test
  public void should_handle_root_context_path_in_production_mode() throws Exception {
    Includes includes = new Includes(digestProduction, "/", "/humpty");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/humpty/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/humpty/" + digestProduction.getString("\"tags.css\"") + "\" />", cssInclude);
  }
  
  @Test
  public void should_use_custom_url_pattern_in_production_mode() throws Exception {
    Includes includes = new Includes(digestProduction, "/ctx", "/custom");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/ctx/custom/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/custom/" + digestProduction.getString("\"tags.css\"") + "\" />", cssInclude);
  }
  
  @Test
  public void should_handle_empty_url_pattern() throws Exception {
    Includes includes = new Includes(digestProduction, "/ctx", "");
    
    String jsInclude = includes.generate("tags.js");
    String cssInclude = includes.generate("tags.css");

    assertEquals("<script src=\"/ctx/" + digestProduction.getString("\"tags.js\"") + "\"></script>", jsInclude);
    assertEquals("<link rel=\"stylesheet\" href=\"/ctx/" + digestProduction.getString("\"tags.css\"") + "\" />", cssInclude);
  }
}
