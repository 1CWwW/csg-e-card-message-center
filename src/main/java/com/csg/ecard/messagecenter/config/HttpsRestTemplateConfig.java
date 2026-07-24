package com.csg.ecard.messagecenter.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * 配置https 的resttemplate
 * @author Miiarms
 * @version 1.0
 * @data 2021/7/7
 */
@Configuration
public class HttpsRestTemplateConfig {

    @Primary
    @Bean(name = "httpsRestTemplate")
    public RestTemplate httpsRestTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // 信任所有证书
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (arg0, arg1) -> true)
                .build();

        // 关闭域名校验
        SSLConnectionSocketFactory sslSocketFactory =
                SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();

        // 创建连接池
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .setMaxConnTotal(200)
                        .setMaxConnPerRoute(50)
                        .build();

        // 超时配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }


}
