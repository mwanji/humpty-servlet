package co.mewf.humpty.servlet;

import java.io.File;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.config.Configuration.Options;
import co.mewf.humpty.config.HumptyBootstrap;
import co.mewf.humpty.servlet.html.Includes;
import co.mewf.humpty.spi.PipelineElement;

import com.moandjiezana.toml.Toml;

public class HumptyServletContextInitializer implements ServletContainerInitializer, PipelineElement {
  
  public static final String DEFAULT_URL_PATTERN = "/humpty";
  
  @Override
  public String getName() {
    return "servlet";
  }

  @Override
  public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
    Configuration configuration = Configuration.load("humpty.toml");
    HumptyBootstrap humptyBootstrap = new HumptyBootstrap(configuration, ctx);
    Options options = configuration.getOptionsFor(this);
    Pipeline pipeline = humptyBootstrap.createPipeline();
    String urlPattern = options.get("urlPattern", DEFAULT_URL_PATTERN);
    File humptyDigestFile = configuration.getGlobalOptions().getDigestFile().toFile();
    Includes includes;
    if (humptyDigestFile.exists()) {
      includes = new Includes(new Toml().parse(humptyDigestFile), ctx.getContextPath(), urlPattern);
    } else {
      includes = new Includes(configuration.getBundles(), ctx.getContextPath(), urlPattern);
      ServletRegistration.Dynamic registration = ctx.addServlet("humptyFilter", HumptyFilter.class);
      registration.addMapping(urlPattern + "/*");
    }

    ctx.setAttribute(Includes.class.getName(), includes);
    ctx.setAttribute(Pipeline.class.getName(), pipeline);
    ctx.setAttribute(HumptyServletContextInitializer.class.getName(), urlPattern);
  }
}
