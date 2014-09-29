package co.mewf.humpty.servlet;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.webjars.WebJarAssetLocator;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.HumptyBootstrap;

public class HumptyFilterTest {

  private final Pipeline pipeline = new HumptyBootstrap("/humpty-servlet-cache.toml").createPipeline();
  private final FileWatchingServletCache cache = pipeline.getPipelineListener(FileWatchingServletCache.class).get();

  @Test
  public void should_handle_digest_bundle_name() throws Exception {
//    final Pipeline pipeline = new Pipeline(asList(new Bundle("singleAsset.js", asList("blocks"))), Configuration.Mode.DEVELOPMENT, emptyList(), emptyList(), emptyList(), emptyList(), asList(cache));
    HttpServletResponse response = mock(HttpServletResponse.class);
    
    HumptyFilter filter = new HumptyFilter(pipeline, "/humpty/*");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/humpty/tags-humptya2e5f9bda12.js");
//    when(pipeline.process("singleAsset.js")).thenReturn("");
//    when(cache.get(anyString())).thenReturn(Optional.empty());
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    
    Assert.assertFalse(cache.contains("tags.js"));
    
    filter.doFilter(request, response, mock(FilterChain.class));
    
    assertTrue("Asset was not correctly processed", cache.contains("tags.js"));
  }
  
  @Test
  public void should_reprocess_bundles_when_contained_asset_changes() throws Exception {
    pipeline.process("modifiable.css");
    
    Path app1 = Paths.get("src/test/resources/" + new WebJarAssetLocator().getFullPath("app1.css"));
    Path path = Paths.get("src/test/resources/" + new WebJarAssetLocator().getFullPath("modifiable.css"));
    
    String originalModifiable = new String(Files.readAllBytes(path));
    
    Files.write(path, "a".getBytes());
    
    Thread.sleep(1000);
    
    String cached = cache.get("modifiable.css");

    try {
      Assert.assertEquals(new String(Files.readAllBytes(app1)) + "\n" + new String(Files.readAllBytes(path)).trim(), cached.trim());
    } finally {
      Files.write(path, originalModifiable.getBytes());
    }
  }
  
  @After
  public void after() {
    cache.shutdown();
  }
}
