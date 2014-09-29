package co.mewf.humpty.servlet;

import co.mewf.humpty.Pipeline;

public class NoopServletCache implements ServletCache {
  
  private final Pipeline pipeline;

  public NoopServletCache(Pipeline pipeline) {
    this.pipeline = pipeline;
  }

  @Override
  public String get(String bundleName) {
    return pipeline.process(bundleName);
  }

}
