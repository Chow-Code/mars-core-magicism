package org.alan.mars.monitor;

import java.io.File;

/**
 * 文件改变通知器
 *
 * Created on 2019/7/24.
 *
 * @author Alan
 * @since 1.0
 */
public interface FileChangeListener {

    String getFileName();

    void onChange(File file);
}
