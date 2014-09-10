package co.mewf.humpty.servlet;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import co.mewf.humpty.Pipeline;
import co.mewf.humpty.config.Bundle;
import co.mewf.humpty.config.Configuration;
import co.mewf.humpty.spi.listeners.PipelineListener;

public class FileSystemWatcher implements PipelineListener {

  private final ScheduledExecutorService watchServiceExecutor = Executors.newSingleThreadScheduledExecutor();
  private final ExecutorService pipelineExecutor = Executors.newSingleThreadExecutor();
  private final ConcurrentMap<Path, String> pathBundles = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();
  private final BlockingQueue<String> bundlesToProcess = new LinkedBlockingQueue<String>();
  private WatchService watchService;
  private Path watchDir;
  
  @Override
  public String getName() {
    return "servletCache";
  }

  @Inject
  public void configure(Configuration.Options options, Pipeline pipeline) {
    this.watchDir = Paths.get(options.get("watchDir", "src/main/resources"));
    try {
      this.watchService = watchDir.getFileSystem().newWatchService();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println(watchDir.toAbsolutePath());
    try {
      watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
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
            if (pathBundles.containsKey(key)) {
              String bundle = pathBundles.get(key);
              if (!bundlesToProcess.contains(bundle)) {
                bundlesToProcess.add(bundle);
              }
            }
          });
//         System.out.println(currentDir.resolve((Path) event.context()) + " " + event.kind())
        watchKey.reset();
//        WatchEvent<?> watchEvent = watchKey.pollEvents().get(0);
//        Path fileName = (Path) watchEvent.context();
//        System.out.println(fileName + " " + watchEvent.kind());
      } catch (InterruptedException e) {
        e.printStackTrace();
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
    Path path = watchDir.resolve(assetPath);
    System.out.println("FileSystemWatcher.onAssetProcessed() path=" + path + " absolute=" + path.toAbsolutePath());
    pathBundles.computeIfAbsent(path, p -> {
        try {
          path.getParent().register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
          return bundle.getName();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
    });
  }
  
  public void shutdown() {
    watchServiceExecutor.shutdownNow();
    pipelineExecutor.shutdownNow();
    try {
      watchService.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
