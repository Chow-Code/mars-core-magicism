package org.alan.mars.monitor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2017/6/2.
 *
 * @author Alan
 * @since 1.0
 */
@Component
@Slf4j
public class FileMonitor extends FileAlterationListenerAdaptor implements CommandLineRunner, ApplicationListener<ContextRefreshedEvent> {
    private final FileAlterationMonitor monitor = new FileAlterationMonitor();
    private final Map<String, FileLoader> fileLoaders = new HashMap<>();
    public final Map<String, FileAlterationObserver> fileObserver = new HashMap<>();
    public final Map<String, List<FileChangeListener>> fileChangeListenerMap = new HashMap<>();

    public void start() {
        log.info("[文件监视器]启动");
        try {
            monitor.start();
        } catch (Exception e) {
            log.warn("[文件监视器]启动失败...", e);
        }
    }
    /**
     * 添加文件监听器
     */
    public void addFileObserver(String fileName, FileLoader fileLoader, boolean load) {
        log.info("[文件监视器]添加文件监听, fileName={}, load={}", fileName, load);
        if (fileName == null || fileName.isEmpty()) {
            log.error("[文件监视器]添加文件监听错误, 参数不能为空");
            return;
        }
        Path filePath = Paths.get(fileName);
        String dirName = filePath.getParent().toString();
        String fName = filePath.getFileName().toString();
        addDirectoryObserver(dirName, null);
        if (fileLoader != null) {
            fileLoaders.put(fName, fileLoader);
        }
        if (load) {
            onFileChange(new File(fileName));
        }
    }

    /**
     * 添加文件监听器
     */
    public void addDirectoryObserver(String dirName, FileLoader fileLoader) {
        if (!fileObserver.containsKey(dirName)) {
            log.info("[文件监视器]添加目录监听, dirName={}", dirName);
            File file = new File(dirName);
            FileAlterationObserver observer = new FileAlterationObserver(file);
            observer.addListener(this);
            try {
                observer.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            monitor.addObserver(observer);
            fileObserver.putIfAbsent(dirName, observer);
            if (fileLoader != null) {
                fileLoaders.putIfAbsent(dirName, fileLoader);
            }
        }
    }


    @Override
    public void onFileChange(File file) {
        try {
            String fileName = file.getName();
            log.info("[文件监视器]监听到文件改变，fileName=" + fileName);
            FileLoader fileListener = fileLoaders.get(fileName);
            if (fileListener != null) {
                fileListener.load(file, false);
            } else {
                String path = file.getPath();
                fileLoaders.forEach((key, value) -> {
                    log.info("[文件监视器] path = {}, boolean = {}", path, path.contains(key));
                    if (path.contains(key)) {
                        value.load(file, false);
                    }
                });
            }
            List<FileChangeListener> fileChangeListeners = fileChangeListenerMap.get(fileName);
            if (fileChangeListeners != null && !fileChangeListeners.isEmpty()) {
                for (FileChangeListener fcl : fileChangeListeners) {
                    try {
                        fcl.onChange(file);
                    } catch (Exception e) {
                        log.warn("[文件监视器]文件改变通知上层异常", e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[文件监视器]文件修改处理异常", e);
        }

    }

    @Override
    public void onFileCreate(File file) {
        try {
            String fileName = file.getName();
            if (fileName.startsWith("~")) {
                return;
            }
            log.info("[文件监视器]监听到文件创建，fileName=" + fileName);
            FileLoader fileListener = fileLoaders.get(fileName);
            if (fileListener != null) {
                fileListener.load(file, true);
            } else {
                String path = file.getPath();
                fileLoaders.forEach((key, value) -> {
                    if (path.contains(key)) {
                        value.load(file, false);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("[文件监视器]文件创建处理异常", e);
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("[文件监视器]扫描上层文件监听器...");
        Map<String, FileChangeListener> maps = event.getApplicationContext().getBeansOfType(FileChangeListener.class);
        if (!maps.isEmpty()) {
            maps.values().forEach(f -> {
                log.info(f.getFileName() + "->" + f.getClass());
                List<FileChangeListener> list = fileChangeListenerMap.computeIfAbsent(f.getFileName(), k -> new ArrayList<>());
                list.add(f);
            });
        }
    }

    @Override
    public void run(String... args) {
        start();
    }
}
