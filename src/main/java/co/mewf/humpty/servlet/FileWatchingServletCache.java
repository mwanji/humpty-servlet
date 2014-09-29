package co.mewf.humpty.servlet;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.inject.Inject;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Bundle;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.spi.listeners.PipelineListener;

public class FileWatchingServletCache implements PipelineListener, ServletCache {

  private final ScheduledExecutorService watchServiceExecutor = Executors.newSingleThreadScheduledExecutor();
  private final ExecutorService pipelineExecutor = Executors.newSingleThreadExecutor();
  private final ConcurrentMap<Path, List<String>> pathBundles = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();
  private final BlockingQueue<String> bundlesToProcess = new LinkedBlockingQueue<String>();
  private WatchService watchService;
  private Path watchDir;
  private Pipeline pipeline;
  private boolean active;
  
  @Override
  public String getName() {
    return "servletCache";
  }

  @Inject
  public void configure(Configuration.Options options, Pipeline pipeline) {
    this.pipeline = pipeline;
    this.active = options.get("active", Boolean.TRUE);
    
    if (!active) {
      return;
    }
    
    this.watchDir = Paths.get(options.get("watchDir", "src/main/resources"));
    System.out.println(watchDir.toAbsolutePath());
    try {
      this.watchService = watchDir.getFileSystem().newWatchService();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      watchDir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    this.watchServiceExecutor.scheduleWithFixedDelay(() -> {
      try {
        WatchKey watchKey = watchService.take();
        Path currentDir = (Path) watchKey.watchable();
        watchKey.pollEvents().stream()
          .filter(e -> e.kind() != StandardWatchEventKinds.OVERFLOW)
          .findFirst()
          .ifPresent(event -> {
            Path key = currentDir.resolve((Path) event.context());
            pathBundles.getOrDefault(key, emptyList()).stream().filter(not(bundlesToProcess::contains)).forEach(bundlesToProcess::add);
          });
        watchKey.reset();
      } catch (ClosedWatchServiceException | InterruptedException e) {
        // ignore
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, 0, 1, TimeUnit.MICROSECONDS);
    
   this.pipelineExecutor.execute(() -> {
     while (true) {
       try {
        String bundleName = bundlesToProcess.take();
        String result = pipeline.process(bundleName);
        cache.put(bundleName, result);
       } catch (InterruptedException e) {
         break;
       } catch (Exception e) {
         e.printStackTrace();
       }
     }
   }); 
  }
  
  @Override
  public void onBundleProcessed(String asset, String name) {}
  
  @Override
  public void onAssetProcessed(String asset, String name, String assetPath, Bundle bundle) {
    if (watchDir == null) {
      return;
    }
    
    Path path = watchDir.resolve(assetPath);
    
    if (!path.toFile().exists()) {
      return;
    }
    
    pathBundles.computeIfAbsent(path, p -> {
      try {
        path.getParent().register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        ArrayList<String> bundles = new ArrayList<String>();
        bundles.add(bundle.getName());
        
        return bundles;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
  
  @Override
  public String get(String bundleName) {
    return cache.computeIfAbsent(bundleName, pipeline::process);
  }
  
  public void shutdown() {
    watchServiceExecutor.shutdownNow();
    pipelineExecutor.shutdownNow();
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  boolean contains(String bundleName) {
    return cache.containsKey(bundleName);
  }
  
  private Predicate<String> not(Predicate<String> p) {
    return p.negate();
  }
}
