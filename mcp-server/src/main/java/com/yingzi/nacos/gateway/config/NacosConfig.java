package com.yingzi.nacos.gateway.config;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
//
@Configuration
@LoadBalancerClients({
  @LoadBalancerClient("mcp-restful-provider"),
//  @LoadBalancerClient("mcp-restful-second-provider")
})
public class NacosConfig {

//    @Autowired
//    private RestfulServicesConfig restfulServicesConfig;
//
//    @Autowired
//    private ApplicationContext applicationContext;

//    @PostConstruct
//    public void init() {
//        // 获取 BeanDefinitionRegistry
//        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
//
//        for (String serviceName : restfulServicesConfig.getRestfulServices()) {
//            // 创建 LoadBalancerClientSpecification 的 BeanDefinition
//            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
//            beanDefinition.setBeanClass(LoadBalancerClientSpecification.class);
//            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, serviceName);
//            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, new Class<?>[] { NacosConfig.class });
//
//            // 注册 BeanDefinition
//            String beanName = "loadBalancerClientSpecification_" + serviceName;
//            registry.registerBeanDefinition(beanName, beanDefinition);
//        }
//    }

}
