package com.ithwx.personalknowledgebase.library.infrastructure;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WebPageFetcher {

    private static final int MAX_CONTENT_BYTES = 2 * 1024 * 1024;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public FetchedWebPage fetch(String url) {
        URI uri = parsePublicUrl(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "PersonalKnowledgeBase/1.0")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("网页请求失败，状态码：" + response.statusCode());
            }
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAX_CONTENT_BYTES + 1);
                if (bytes.length > MAX_CONTENT_BYTES) {
                    throw new IllegalArgumentException("网页内容超过大小限制");
                }
                String type = response.headers().firstValue("content-type").orElse("");
                return parseContent(uri, type, bytes);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("网页请求被中断", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("网页请求失败", exception);
        }
    }

    FetchedWebPage parseContent(URI uri, String contentType, byte[] bytes) {
        if (contentType.startsWith("text/plain")) {
            return page(uri, uri.getHost(), new String(bytes, StandardCharsets.UTF_8));
        }
        if (!contentType.startsWith("text/html")) {
            throw new IllegalArgumentException("只支持 HTML 或纯文本网页");
        }
        org.jsoup.nodes.Document document = Jsoup.parse(
                new String(bytes, StandardCharsets.UTF_8), uri.toString());
        document.select("script, style, nav, footer, header, aside, form").remove();
        Element main = document.selectFirst("main, article");
        Element content = main == null ? document.body() : main;
        String title = document.title().isBlank() ? uri.getHost() : document.title();
        return page(uri, title, content == null ? "" : content.text());
    }

    private FetchedWebPage page(URI uri, String title, String text) {
        if (text.isBlank()) {
            throw new IllegalArgumentException("网页中没有可提取的正文");
        }
        return new FetchedWebPage(uri.toString(), title.strip(), text.strip());
    }

    private URI parsePublicUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("网页地址格式不正确");
        }
        if (!("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalArgumentException("只支持公开的 HTTP/HTTPS 网址");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                    throw new IllegalArgumentException("不允许访问本机或内网地址");
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法解析网页地址");
        }
        return uri;
    }

    public record FetchedWebPage(String url, String title, String text) {
    }
}
