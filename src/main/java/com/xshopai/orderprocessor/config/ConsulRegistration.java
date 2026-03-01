package com.xshopai.orderprocessor.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Consul self-registration component.
 * Registers on startup, deregisters on shutdown.
 * Only active when CONSUL_URL environment variable is set.
 */
@Component
public class ConsulRegistration {

  private static final Logger logger = LoggerFactory.getLogger(ConsulRegistration.class);

  @Value("${server.port:8007}")
  private int port;

  private final String consulUrl = System.getenv("CONSUL_URL") != null ? System.getenv("CONSUL_URL") : "";
  private final String host = System.getenv("HOST") != null ? System.getenv("HOST") : "localhost";
  private String serviceId = "";

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  @PostConstruct
  public void register() {
    if (consulUrl.isEmpty())
      return;

    String serviceName = "order-processor-service";
    String address = "0.0.0.0".equals(host) ? "localhost" : host;
    serviceId = serviceName + "-" + address + "-" + port;

    String json = String.format("""
        {
            "ID": "%s",
            "Name": "%s",
            "Address": "%s",
            "Port": %d,
            "Check": {
                "HTTP": "http://%s:%d/health",
                "Interval": "10s",
                "Timeout": "5s",
                "DeregisterCriticalServiceAfter": "30s"
            }
        }
        """, serviceId, serviceName, address, port, address, port);

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(consulUrl + "/v1/agent/service/register"))
          .header("Content-Type", "application/json")
          .PUT(HttpRequest.BodyPublishers.ofString(json))
          .timeout(Duration.ofSeconds(5))
          .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        logger.info("[Consul] Registered {} ({}) at {}:{}", serviceName, serviceId, address, port);
      } else {
        logger.warn("[Consul] Registration failed: {}", response.statusCode());
      }
    } catch (Exception e) {
      logger.warn("[Consul] Registration failed (Consul unavailable): {}", e.getMessage());
    }
  }

  @PreDestroy
  public void deregister() {
    if (consulUrl.isEmpty() || serviceId.isEmpty())
      return;

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(consulUrl + "/v1/agent/service/deregister/" + serviceId))
          .PUT(HttpRequest.BodyPublishers.noBody())
          .timeout(Duration.ofSeconds(5))
          .build();

      httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      logger.info("[Consul] Deregistered {}", serviceId);
    } catch (Exception e) {
      // Best-effort — service is shutting down anyway
    }
  }
}
