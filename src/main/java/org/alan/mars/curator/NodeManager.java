/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.curator;

import cn.hutool.core.io.file.FileReader;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.config.ZookeeperConfig;
import org.alan.mars.data.MarsConst;
import org.alan.mars.micservice.MicServiceManager;
import org.alan.mars.monitor.FileLoader;
import org.alan.mars.monitor.FileMonitor;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Created on 2017/3/6.
 *
 * @author alan
 * @since 1.0
 */
@Component
@Order(3)
@Slf4j
public class NodeManager implements CommandLineRunner, MarsCuratorListener, MarsNodeListener, FileLoader {

    @Autowired
    public NodeConfig nodeConfig;

    @Autowired
    private MarsCurator marsCurator;
    @Autowired
    private ZookeeperConfig zkConfig;

    public String nodePath;

    public String configFile = "config/nodeConfig.json";
    @Autowired
    public FileMonitor fileMonitor;

    @Autowired
    public MicServiceManager micServiceManager;

    public NodeManager() {
    }

    @Override
    public void marsCuratorRefreshed(MarsCurator marsCurator) {
        register();
    }

    @Override
    public void run(String... strings) throws Exception {
        obConfig();
    }

    public void obConfig() {
        fileMonitor.addFileObserver(configFile, this, true);
    }

    @Override
    public void load(File file, boolean isNew) {
        try {
            log.info("on file change filename = {}，isNew={}", file.getName(), isNew);
            readConfig(file);
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("on file change err,filename = " + file.getName() + "，isNew=" + isNew, e);
        }

    }
    public void readConfig(File file) {
        FileReader fileReader = new FileReader(file);
        String content = fileReader.readString();
        JSONObject jsonObject = JSON.parseObject(content);
        if (jsonObject.containsKey("weight")) {
            int weight = jsonObject.getIntValue("weight");
            nodeConfig.setWeight(weight);
        }
        if (jsonObject.containsKey("whiteIpList")) {
            JSONArray whiteIpArray = jsonObject.getJSONArray("whiteIpList");
            if (whiteIpArray != null) {
                nodeConfig.setWhiteIpList(whiteIpArray.stream().map(i -> (String) i).toArray(String[]::new));
            }
        }
        if (jsonObject.containsKey("whiteIdList")) {
            JSONArray whiteIdArray = jsonObject.getJSONArray("whiteIdList");
            if (whiteIdArray != null) {
                nodeConfig.setWhiteIdList(ArrayUtils.toPrimitive(whiteIdArray.toArray(new Integer[0])));
            }
        }
        update();
    }

    private void register() {
        try {
            String path = MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR +
                    nodeConfig.getType() + MarsConst.SEPARATOR +
                    nodeConfig.getName();
            log.info("node register,path is {}", path);

            //添加微服务
            if (nodeConfig.showMicService) {
                nodeConfig.setMicServiceMessageTypes(micServiceManager.messageTypes);
            }else{
                nodeConfig.setMicServiceMessageTypes(null);
            }
            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.addPath(path, nc.getBytes(StandardCharsets.UTF_8), false);
            marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node register fail.", e);
        }
    }

    public void update() {
        try {
            String path = MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR +
                    nodeConfig.getType() + MarsConst.SEPARATOR +
                    nodeConfig.getName();
            log.info("node update,path is {}", path);

            //添加微服务
            if (nodeConfig.showMicService) {
                nodeConfig.setMicServiceMessageTypes(micServiceManager.messageTypes);
            }else{
                nodeConfig.setMicServiceMessageTypes(null);
            }

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.updatePath(path, nc.getBytes(StandardCharsets.UTF_8), false);
            //marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node update fail.", e);
        }
    }

    public MarsNode getMarNode(NodeType nodeType) {
        String nodePath = MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath()
                + MarsConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public MarsNode getMarNode(String nodeType) {
        String nodePath = MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath()
                + MarsConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public String getMarNodePath(String nodeType, String nodeName) {
        return MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath()
                + MarsConst.SEPARATOR + nodeType + MarsConst.SEPARATOR + nodeName;
    }

    @Override
    public void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode) {
        switch (nodeChangeType) {
            case NODE_ADD:

                break;
            case NODE_REMOVE:
                log.warn("本节点被异常移除");
                register();
                break;
        }
    }
}
