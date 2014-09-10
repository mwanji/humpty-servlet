package co.mewf.humpty.servlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import co.mewf.humpty.Pipeline;

public class HumptyFilterTest {


  @Test
  public void should_handle_digest_bundle_name() throws Exception {
    final Pipeline pipeline = mock(Pipeline.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    
    HumptyFilter filter = new HumptyFilter(pipeline, "/humpty/*");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletPath()).thenReturn("/humpty/singleAsset-humptya2e5f9bda12.js");
    when(pipeline.process("singleAsset.js")).thenReturn("");
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    
    filter.doFilter(request, response, mock(FilterChain.class));

    verify(pipeline).process("singleAsset.js");
  }

}
