package org.alan.mars.curator;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.MarsContext;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.config.ZookeeperConfig;
import org.alan.mars.data.MarsConst;
import org.alan.mars.message.NetAddress;
import org.alan.mars.micservice.MicServiceManager;
import org.alan.mars.monitor.FileLoader;
import org.alan.mars.monitor.FileMonitor;
import org.alan.mars.utils.NetworkUtils;
import org.alan.mars.utils.RandomUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created on 2017/3/6.
 *
 * @author alan
 * @since 1.0
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class NodeManager implements ApplicationRunner, CommandLineRunner, MarsCuratorListener, MarsNodeListener, FileLoader {

    private final NodeConfig nodeConfig;
    private final MarsCurator marsCurator;
    private final ZookeeperConfig zkConfig;
    private final FileMonitor fileMonitor;
    private final MicServiceManager micServiceManager;
    public static final String CONFIG_FILE_PATH = "config/nodeConfig.json";
    public String nodePath;

    @Override
    public void marsCuratorRefreshed(MarsCurator marsCurator) {
        register();
    }

    @Override
    public void run(String... strings) {
        obConfig();
    }

    @Override
    public void run(ApplicationArguments args) {
        autoLoadConfig(args);
    }

    public void autoLoadConfig(ApplicationArguments args) {
        String name = getOption(args, "cluster.name");
        if (StrUtil.isNotEmpty(name)) {
            log.info("[节点管理] 使用参数来命名本节点: {}", name);
            nodeConfig.setName(name);
        } else {
            if (StrUtil.isEmpty(nodeConfig.getName())) {
                //使用hostname来命名
                String nodeName;
                if (MarsContext.isRelease()) {
                    try {
                        nodeName = InetAddress.getLocalHost().getCanonicalHostName();
                        log.info("[节点管理] 正式环境使用HostName命名本节点: {}", nodeName);
                    } catch (UnknownHostException e) {
                        nodeName = nodeConfig.getType().toLowerCase() + "_" + RandomUtil.randomString(10);
                        log.info("[节点管理] 使用随机数来命名本节点: {}", nodeName);
                    }
                } else {
                    nodeName = nodeConfig.getType().toLowerCase() + "_" + RandomUtil.randomString(10);
                    log.info("[节点管理] 使用随机数来命名本节点: {}", nodeName);
                }
                nodeConfig.setName(nodeName);
            } else {
                log.info("[节点管理] 使用yaml来命名本节点: {}", nodeConfig.getName());
            }
        }
        String ip = getOption(args, "cluster.host");

        NetAddress tcpAddress = nodeConfig.getTcpAddress();
        if (tcpAddress == null) {
            tcpAddress = new NetAddress();
        }
        if (ip != null) {
            tcpAddress.setHost(ip);
            if (nodeConfig.getHttpAddress() != null) {
                nodeConfig.getHttpAddress().setHost(ip);
            }
            log.info("[节点管理] 使用参数来设置本节点IP地址: {}", ip);
        } else {
            if (StrUtil.isEmpty(tcpAddress.getHost())) {
                String privateHost = NetworkUtils.getPrivateHost();
                log.info("[节点管理] 使用网卡信息来设置本节点IP地址: {}", privateHost);
                tcpAddress.setHost(privateHost);
                if (nodeConfig.getHttpAddress() != null) {
                    nodeConfig.getHttpAddress().setHost(privateHost);
                }
            } else {
                log.info("[节点管理] 使用yaml设置本节点IP地址: {}", tcpAddress.getHost());
            }
        }
        String option = getOption(args, "cluster.port");
        if (StrUtil.isNumeric(option)) {
            int port = Integer.parseInt(option);
            log.info("[节点管理] 使用参数来设置本节点TCP端口: {}", port);
            tcpAddress.setPort(port);
        } else {
            if (tcpAddress.getPort() == null) {
                int random = RandomUtil.random(10000, 65535);
                log.info("[节点管理] 使用随机数设置本节点TCP端口: {}", random);
                nodeConfig.getTcpAddress().setPort(random);
            } else {
                log.info("[节点管理] 使用yaml设置本节点TCP端口: {}", tcpAddress.getPort());
            }
        }
        nodeConfig.setNum(RandomUtil.random(1000000));

    }

    public String getOption(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null) {
            return null;
        }
        return values.stream().findAny().orElse(null);
    }

    public void obConfig() {
        fileMonitor.addFileObserver(CONFIG_FILE_PATH, this, true);
    }

    @Override
    public void load(File file, boolean isNew) {
        try {
            log.info("on file change filename = {}，isNew={}", file.getName(), isNew);
            readConfig(file);
        } catch (Exception e) {
            log.warn("on file change err,filename = " + file.getName() + "，isNew=" + isNew, e);
        }

    }

    public void readConfig(File file) {
        if (!file.exists()) {
            log.info("无法加载nodeConfig, 文件不存在");
            return;
        }
        FileReader fileReader = new FileReader(file);
        String content = fileReader.readString().replaceAll("(//.*)|(/\\*[\\s\\S]*?\\*/)", "");
        JSONObject jsonObject = JSONUtil.parseObj(content);
        if (jsonObject.containsKey("weight")) {
            int weight = jsonObject.getInt("weight");
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
                List<Long> javaList = whiteIdArray.toList(Long.TYPE);
                nodeConfig.setWhiteIdList(javaList.stream().mapToLong(Long::longValue).toArray());
            }
        }
        if (jsonObject.containsKey("workPoolNum")) {
            int workPoolNum = jsonObject.getInt("workPoolNum");
            nodeConfig.setWorkPoolNum(workPoolNum);
        }
        update();
    }

    private void register() {
        try {
            String path = MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR + nodeConfig.getType() + MarsConst.SEPARATOR + nodeConfig.getName();
            log.info("[节点管理] 节点注册 {}", path);

            //添加微服务
            if (nodeConfig.showMicService) {
                nodeConfig.setMicServiceMessageTypes(micServiceManager.messageTypes);
            } else {
                nodeConfig.setMicServiceMessageTypes(null);
            }
            String nc = JSONUtil.toJsonPrettyStr(nodeConfig);
            nodePath = marsCurator.addPath(path, nc.getBytes(StandardCharsets.UTF_8), false);
            marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node register fail.", e);
        }
    }

    public void update() {
        try {
            String path = MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR + nodeConfig.getType() + MarsConst.SEPARATOR + nodeConfig.getName();
            log.info("node update,path is {}", path);

            //添加微服务
            if (nodeConfig.showMicService) {
                nodeConfig.setMicServiceMessageTypes(micServiceManager.messageTypes);
            } else {
                nodeConfig.setMicServiceMessageTypes(null);
            }

            String nc = JSONUtil.toJsonPrettyStr(nodeConfig);
            nodePath = marsCurator.updatePath(path, nc.getBytes(StandardCharsets.UTF_8), false);
            //marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node update fail.", e);
        }
    }

    public MarsNode getMarNode(NodeType nodeType) {
        String nodePath = MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public MarsNode getMarNode(String nodeType) {
        String nodePath = MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public String getMarNodePath(String nodeType, String nodeName) {
        return MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath() + MarsConst.SEPARATOR + nodeType + MarsConst.SEPARATOR + nodeName;
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
