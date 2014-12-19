package co.mewf.humpty.servlet;

import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.Configuration.Options;
import co.mewf.humpty.config.HumptyBootstrap;
import co.mewf.humpty.digest.DigestPipelineListener;
import co.mewf.humpty.servlet.html.Includes;
import co.mewf.humpty.spi.PipelineElement;

public class HumptyServletContextInitializer implements ServletContainerInitializer, PipelineElement {
  
  public static final String DEFAULT_URL_PATTERN = "/humpty";
  
  @Override
  public String getName() {
    return "servlet";
  }

  @Override
  public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
    Configuration configuration = Configuration.load("/humpty.toml");
    HumptyBootstrap humptyBootstrap = new HumptyBootstrap(configuration, ctx);
    Options options = configuration.getOptionsFor(this);
    Pipeline pipeline = humptyBootstrap.createPipeline();
    String urlPattern = options.get("urlPattern", DEFAULT_URL_PATTERN);
    Configuration.Mode mode = Configuration.Mode.valueOf(configuration.getOptionsFor(humptyBootstrap).get("mode", Configuration.Mode.PRODUCTION.toString()));
    Optional<DigestPipelineListener> optionalDigest = pipeline.getPipelineListener(DigestPipelineListener.class);
    Includes includes = mode == Configuration.Mode.DEVELOPMENT || !optionalDigest.isPresent() ? new Includes(configuration.getBundles(), ctx.getContextPath(), urlPattern) : new Includes(optionalDigest.get(), ctx.getContextPath(), urlPattern);

    ctx.setAttribute(Pipeline.class.getName(), pipeline);
    ctx.setAttribute(HumptyServletContextInitializer.class.getName(), urlPattern);
    ctx.setAttribute(Configuration.Mode.class.getName(), mode);
    ctx.setAttribute(Includes.class.getName(), includes);
    
    if (mode == Configuration.Mode.PRODUCTION) {
      configuration.getBundles().forEach(b -> pipeline.process(b.getName()));
    }
    
    if (mode != Configuration.Mode.EXTERNAL) {
      ServletRegistration.Dynamic registration = ctx.addServlet("humptyFilter", HumptyFilter.class);
      registration.addMapping(urlPattern + "/*");
    }
  }
}
