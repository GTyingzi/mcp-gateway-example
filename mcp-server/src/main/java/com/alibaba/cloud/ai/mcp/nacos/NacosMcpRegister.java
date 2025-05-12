//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibaba.cloud.ai.mcp.nacos;

import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos.model.McpServerInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.McpToolsInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.RemoteServerConfigInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.ServiceRefInfo;
import com.alibaba.cloud.ai.mcp.nacos.model.ToolMetaInfo;
import com.alibaba.cloud.ai.mcp.nacos.utils.JsonUtils;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.utils.StringUtils;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yingzi.nacos.gateway.utils.ConfigMd5Injector;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

public class NacosMcpRegister implements ApplicationListener<WebServerInitializedEvent> {
    private static final Logger log = LoggerFactory.getLogger(NacosMcpRegister.class);
    private final String toolsGroup = "mcp-tools";
    private final String toolsConfigSuffix = "-mcp-tools.json";
    private final String configNamespace = "nacos-default-mcp";
    private final String serverGroup = "mcp-server";

    private final Long TIME_OUT_MS = 3000L;
    private String type;
    private NacosMcpRegistryProperties nacosMcpProperties;
    private McpSchema.Implementation serverInfo;
    private McpAsyncServer mcpAsyncServer;
    private CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> tools;
    private Map<String, ToolMetaInfo> toolsMeta;
    private McpSchema.ServerCapabilities serverCapabilities;
    private ConfigService configService;

    public NacosMcpRegister(McpAsyncServer mcpAsyncServer, NacosMcpRegistryProperties nacosMcpProperties, String type) {
        this.mcpAsyncServer = mcpAsyncServer;
        log.info("Mcp server type: {}", type);
        this.type = type;
        this.nacosMcpProperties = nacosMcpProperties;

        try {
            Class<?> clazz = Class.forName("io.modelcontextprotocol.server.McpAsyncServer$AsyncServerImpl");
            Field delegateField = McpAsyncServer.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegateInstance = delegateField.get(mcpAsyncServer);

            this.serverInfo = mcpAsyncServer.getServerInfo();
            this.serverCapabilities = mcpAsyncServer.getServerCapabilities();

            Field toolsField = clazz.getDeclaredField("tools");
            toolsField.setAccessible(true);
            this.tools = (CopyOnWriteArrayList)toolsField.get(delegateInstance);
            this.toolsMeta = new HashMap();
            this.tools.forEach((toolRegistration) -> {
                ToolMetaInfo toolMetaInfo = new ToolMetaInfo();
                this.toolsMeta.put(toolRegistration.tool().name(), toolMetaInfo);
            });
            Properties configProperties = nacosMcpProperties.getNacosProperties();
//            configProperties.put("namespace", "9ba5f1aa-b37d-493b-9057-72918a40ef35");
            this.configService = new NacosConfigService(configProperties);
            if (this.serverCapabilities.tools() != null) {
                String toolsInNacosContent = this.configService.getConfig(this.serverInfo.name() + "-mcp-tools.json", toolsGroup, TIME_OUT_MS);
                if (toolsInNacosContent != null) {
                    this.updateTools(toolsInNacosContent);
                }

                List<McpSchema.Tool> toolsNeedtoRegister = this.tools.stream().map(McpServerFeatures.AsyncToolSpecification::tool).toList();
                McpToolsInfo mcpToolsInfo = new McpToolsInfo();
                mcpToolsInfo.setTools(toolsNeedtoRegister);
                mcpToolsInfo.setToolsMeta(this.toolsMeta);
                String toolsConfigContent = JsonUtils.serialize(mcpToolsInfo);
                boolean isPublishSuccess = this.configService.publishConfig(this.serverInfo.name() + "-mcp-tools.json", toolsGroup, toolsConfigContent);
                if (!isPublishSuccess) {
                    log.error("Publish tools config to nacos failed.");
                    throw new Exception("Publish tools config to nacos failed.");
                }

                this.configService.addListener(this.serverInfo.name() + "-mcp-tools.json", toolsGroup, new Listener() {
                    public void receiveConfigInfo(String configInfo) {
                        NacosMcpRegister.this.updateTools(configInfo);
                    }

                    public Executor getExecutor() {
                        return null;
                    }
                });
            }

            String serverInfoContent = this.configService.getConfig(this.serverInfo.name() + "-mcp-server.json", serverGroup, TIME_OUT_MS);
            String serverDescription = this.serverInfo.name();
            if (serverInfoContent != null) {
                Map<String, Object> serverInfoMap = (Map)JsonUtils.deserialize(serverInfoContent, Map.class);
                if (serverInfoMap.containsKey("description")) {
                    serverDescription = (String)serverInfoMap.get("description");
                }
            }

            McpServerInfo mcpServerInfo = new McpServerInfo();
            mcpServerInfo.setName(this.serverInfo.name());
            mcpServerInfo.setVersion(this.serverInfo.version());
            mcpServerInfo.setDescription(serverDescription);
            mcpServerInfo.setEnabled(true);
            if ("stdio".equals(this.type)) {
                mcpServerInfo.setProtocol("local");
            } else {
                ServiceRefInfo serviceRefInfo = new ServiceRefInfo();
                serviceRefInfo.setNamespaceId(nacosMcpProperties.getServiceNamespace());
                serviceRefInfo.setServiceName(this.serverInfo.name() + "-mcp-service");
                serviceRefInfo.setGroupName(nacosMcpProperties.getServiceGroup());
                RemoteServerConfigInfo remoteServerConfigInfo = new RemoteServerConfigInfo();
                remoteServerConfigInfo.setServiceRef(serviceRefInfo);
                String contextPath = nacosMcpProperties.getSseExportContextPath();
                if (StringUtils.isBlank(contextPath)) {
                    contextPath = "";
                }

                remoteServerConfigInfo.setExportPath(contextPath + "/sse");
                mcpServerInfo.setRemoteServerConfig(remoteServerConfigInfo);
                mcpServerInfo.setProtocol("mcp-sse");
            }

            if (this.serverCapabilities.tools() != null) {
                mcpServerInfo.setToolsDescriptionRef(this.serverInfo.name() + "-mcp-tools.json");
            }

            boolean isPublishSuccess = this.configService.publishConfig(this.serverInfo.name() + "-mcp-server.json", serverGroup, JsonUtils.serialize(mcpServerInfo));
            if (!isPublishSuccess) {
                log.error("Publish mcp server info to nacos failed.");
                throw new Exception("Publish mcp server info to nacos failed.");
            }

            log.info("Register mcp server info to nacos successfully");
        } catch (Exception var18) {
            log.error("Failed to register mcp server to nacos", var18);
        }

    }

    private void updateToolDescription(McpServerFeatures.AsyncToolSpecification localToolRegistration, McpSchema.Tool toolInNacos, List<McpServerFeatures.AsyncToolSpecification> toolsRegistrationNeedToUpdate) throws JsonProcessingException {
        Boolean changed = false;
        if (localToolRegistration.tool().description() != null && !localToolRegistration.tool().description().equals(toolInNacos.description())) {
            changed = true;
        }

        String localInputSchemaString = JsonUtils.serialize(localToolRegistration.tool().inputSchema());
        Map<String, Object> localInputSchemaMap = (Map)JsonUtils.deserialize(localInputSchemaString, Map.class);
        Map<String, Object> localProperties = (Map)localInputSchemaMap.get("properties");
        String nacosInputSchemaString = JsonUtils.serialize(toolInNacos.inputSchema());
        Map<Object, Object> nacosInputSchemaMap = (Map)JsonUtils.deserialize(nacosInputSchemaString, Map.class);
        Map<String, Object> nacosProperties = (Map)nacosInputSchemaMap.get("properties");
        Iterator var11 = localProperties.keySet().iterator();

        while(var11.hasNext()) {
            String key = (String)var11.next();
            if (nacosProperties.containsKey(key)) {
                Map<String, Object> localProperty = (Map)localProperties.get(key);
                Map<String, Object> nacosProperty = (Map)nacosProperties.get(key);
                String localDescription = (String)localProperty.get("description");
                String nacosDescription = (String)nacosProperty.get("description");
                if (nacosDescription != null && !nacosDescription.equals(localDescription)) {
                    localProperty.put("description", nacosDescription);
                    changed = true;
                }
            }
        }

        if (changed) {
            McpSchema.Tool toolNeededUpdate = new McpSchema.Tool(localToolRegistration.tool().name(), toolInNacos.description(), JsonUtils.serialize(localInputSchemaMap));
            toolsRegistrationNeedToUpdate.add(new McpServerFeatures.AsyncToolSpecification(toolNeededUpdate, localToolRegistration.call()));
        }

    }

    private void updateTools(String toolsInNacosContent) {
        try {
            boolean changed = false;
            McpToolsInfo toolsInfo = (McpToolsInfo)JsonUtils.deserialize(toolsInNacosContent, McpToolsInfo.class);
            List<McpSchema.Tool> toolsInNacos = toolsInfo.getTools();
            if (!this.toolsMeta.equals(toolsInfo.getToolsMeta())) {
                changed = true;
                this.toolsMeta = toolsInfo.getToolsMeta();
            }

            List<McpServerFeatures.AsyncToolSpecification> toolsRegistrationNeedToUpdate = new ArrayList();
            Map<String, McpSchema.Tool> toolsInNacosMap = (Map)toolsInNacos.stream().collect(Collectors.toMap(McpSchema.Tool::name, (tool) -> {
                return tool;
            }));
            Iterator var7 = this.tools.iterator();

            McpServerFeatures.AsyncToolSpecification toolRegistration;
            while(var7.hasNext()) {
                toolRegistration = (McpServerFeatures.AsyncToolSpecification)var7.next();
                String name = toolRegistration.tool().name();
                if (toolsInNacosMap.containsKey(name)) {
                    McpSchema.Tool toolInNacos = (McpSchema.Tool)toolsInNacosMap.get(name);
                    this.updateToolDescription(toolRegistration, toolInNacos, toolsRegistrationNeedToUpdate);
                    break;
                }
            }

            var7 = toolsRegistrationNeedToUpdate.iterator();

            while(true) {
                while(var7.hasNext()) {
                    toolRegistration = (McpServerFeatures.AsyncToolSpecification)var7.next();

                    for(int i = 0; i < this.tools.size(); ++i) {
                        if (((McpServerFeatures.AsyncToolSpecification)this.tools.get(i)).tool().name().equals(toolRegistration.tool().name())) {
                            this.tools.set(i, toolRegistration);
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) {
                    log.info("tools description updated");
                }

                if (changed && this.serverCapabilities.tools().listChanged()) {
                    this.mcpAsyncServer.notifyToolsListChanged().block();
                }
                break;
            }
        } catch (Exception var11) {
            log.error("Failed to update tools according to nacos", var11);
        }

    }

    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (!"stdio".equals(this.type) && this.nacosMcpProperties.isServiceRegister()) {
            try {
                int port = event.getWebServer().getPort();
                NamingService namingService = new NacosNamingService(this.nacosMcpProperties.getNacosProperties());
                Instance instance = new Instance();
                Map<String, String> metadata = new HashMap();

                // 配置Mcp Server信息的MD5
                String serverConfigMd5 = ConfigMd5Injector.getConfigMd5(configService, this.serverInfo.name() + "-mcp-server.json", serverGroup);
                metadata.put("server.md5", serverConfigMd5);
                // 配置对应的工具信息
                String toolConfig = configService.getConfig(this.serverInfo.name() + "-mcp-tools.json", toolsGroup, TIME_OUT_MS);
                McpToolsInfo toolsInfo = JsonUtils.deserialize(toolConfig, McpToolsInfo.class);
                List<String> toolNames = toolsInfo.getTools().stream().map(McpSchema.Tool::name).collect(Collectors.toList());
                metadata.put("tools.names", String.join(",", toolNames));

                instance.setIp(this.nacosMcpProperties.getIp());
                instance.setPort(port);
                instance.setEphemeral(this.nacosMcpProperties.isServiceEphemeral());
                instance.setMetadata(metadata);

                namingService.registerInstance(this.serverInfo.name() + "-mcp-service", this.nacosMcpProperties.getServiceGroup(), instance);
                log.info("Register mcp server service to nacos successfully");
            } catch (NacosException var5) {
                log.error("Failed to register mcp server service to nacos", var5);
            } catch (Exception ignored) {
            }

        } else {
            log.info("No need to register mcp server service to nacos");
        }
    }
}
