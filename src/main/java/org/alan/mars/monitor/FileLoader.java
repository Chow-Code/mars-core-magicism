package org.alan.mars.monitor;

import java.io.File;

/**
 * Created on 2017/6/2.
 *
 * @author Alan
 * @since 1.0
 */
public interface FileLoader {

    void load(File file, boolean isNew);
}
