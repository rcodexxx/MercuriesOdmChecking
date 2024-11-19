package com.firsttech.insurance.odmchecking.service.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.SSLContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

public final class HttpUtil {


    public HttpUtil() {
    }

    public HttpResponse httpRequestGet(String inputUri, Map<String, String> headers) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        HttpGet request = new HttpGet(inputUri);
        for (String key : headers.keySet()) {
            request.setHeader(key, headers.get(key));
        }

        CloseableHttpClient httpClient = getHttpClient();
        HttpClientContext httpContext = HttpClientContext.create();
        return httpClient.execute(request, httpContext);

    }

    public HttpResponse httpRequestPost(String inputUri, String inputPayload, Map<String, String> headers) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpPost request = new HttpPost(inputUri);
        for (String key : headers.keySet()) {
            request.setHeader(key, headers.get(key));
        }
        request.setEntity(new StringEntity(inputPayload, ContentType.APPLICATION_JSON));

        CloseableHttpClient httpClient = getHttpClient();
        HttpClientContext httpContext = HttpClientContext.create();
        HttpResponse response = httpClient.execute(request, httpContext);
        return response;
    }

    public Registry<ConnectionSocketFactory> getRegistry()
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLContext sslContext = null;
        sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        }).build();

        // SSLContext sslContext = SSLContexts.custom().build();
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
            new String[]{"TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);

        return RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslConnectionSocketFactory).build();
    }

    public CloseableHttpClient getHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager(getRegistry());
        clientConnectionManager.setMaxTotal(1000);
        clientConnectionManager.setDefaultMaxPerRoute(200);
        return HttpClients.custom().setConnectionManager(clientConnectionManager)
            .setRedirectStrategy(new LaxRedirectStrategy()).build();
    }


}
