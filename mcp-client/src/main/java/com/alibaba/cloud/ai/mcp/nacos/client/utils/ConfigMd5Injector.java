package com.alibaba.cloud.ai.mcp.nacos.client.utils;

import com.alibaba.nacos.api.config.ConfigService;

/**
 * @author yingzi
 * @date 2025/5/9:17:08
 */
public class ConfigMd5Injector {
    public static String getConfigMd5(ConfigService configService, String dataId, String group) throws Exception {
        // 获取配置内容
        String content = configService.getConfig(dataId, group, 3000);
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Config content is empty for dataId: " + dataId);
        }

        // 计算 MD5
        MD5Utils md5Utils = new MD5Utils();
        return md5Utils.getMd5(content);
    }
}
