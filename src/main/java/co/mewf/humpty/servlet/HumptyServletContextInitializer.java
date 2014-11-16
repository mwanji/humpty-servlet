package co.mewf.humpty.servlet;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.Configuration.Options;
import co.mewf.humpty.config.HumptyBootstrap;
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
    String urlPattern = options.get("urlPattern", DEFAULT_URL_PATTERN);
    Pipeline pipeline = humptyBootstrap.createPipeline();
    pipeline.getPipelineListener(Includes.class).ifPresent(i -> ctx.setAttribute(Includes.class.getName(), i));

    Configuration.Mode mode = Configuration.Mode.valueOf(configuration.getOptionsFor(humptyBootstrap).get("mode", Configuration.Mode.PRODUCTION.toString()));
    
    if (mode == Configuration.Mode.PRODUCTION) {
      configuration.getBundles().forEach(b -> pipeline.process(b.getName()));
    }
    
    if (mode != Configuration.Mode.EXTERNAL) {
      FilterRegistration filterRegistration = ctx.addFilter("humptyFilter", new HumptyFilter(pipeline, urlPattern, mode));
      filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, urlPattern + "/*");
    }
  }
}
