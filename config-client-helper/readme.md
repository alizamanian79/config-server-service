# Config Client Setup

Connect a Spring Boot service to the central **Config Server** and keep its configuration up to date at runtime, with no restart needed.

## How it works

```
Git config repo ──► Config Server (:8888) ──► Client service
                                                  ▲
                              ConfigRefreshScheduler polls and refreshes
                              beans annotated with @RefreshScope
```

## Prerequisites

- Config Server is running and reachable from your service.
- The config repo contains a folder for your service with `{application}.yaml` and `{application}-{profile}.yaml` (e.g. `wallet-service.yaml`, `wallet-service-dev.yaml`).
- Your service's `spring.application.name` matches that name exactly.

## Setup

### 1. Add dependencies

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. Configure `application.yaml`

```yaml
spring:
  application:
    name: wallet-service
  profiles:
    active: dev
  config:
    import: "configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
  cloud:
    config:
      fail-fast: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
```

- Set `CONFIG_SERVER_URL` per environment (in Docker: `http://config-server:8888`).
- `fail-fast: true` makes the service fail at startup if the Config Server is unreachable. To allow startup without it, use `"optional:configserver:..."` and remove `fail-fast`.

### 3. Enable scheduling and properties scanning

```java
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class MainApplicationServer {
    public static void main(String[] args) {
        SpringApplication.run(MainApplicationServer.class, args);
    }
}
```

### 4. Add `ConfigRefreshScheduler`

Place it in your configuration package:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigRefreshScheduler {

    private final ContextRefresher contextRefresher;

    @Scheduled(fixedDelayString = "${config.refresh.interval-ms:10000}")
    public void refresh() {
        try {
            Set<String> changedKeys = contextRefresher.refresh();
            if (!changedKeys.isEmpty()) {
                log.info("Configuration refreshed. Changed keys: {}", changedKeys);
            }
        } catch (Exception e) {
            log.warn("Config refresh failed: {}", e.getMessage());
        }
    }
}
```

### 5. Annotate refreshable beans

Add `@RefreshScope` to any bean that reads values via `@Value`:

```java
@RefreshScope
@RestController
@RequestMapping("/api/example")
public class ExampleController {

    @Value("${app.message:default}")
    private String message;

    @GetMapping
    public String get() {
        return message;
    }
}
```

## Verify it works

1. Check the Config Server directly:
   ```bash
   curl http://localhost:8888/wallet-service/dev
   ```
2. Start the service and call your endpoint:
   ```bash
   curl http://localhost:8080/api/example
   ```
3. Change `app.message` in the config repo, commit and push.
4. Wait one refresh interval (10s by default) and call the endpoint again. The new value should appear.

## Rules of thumb

- `@ConfigurationProperties` classes rebind automatically on refresh and do not need `@RefreshScope`.
- Never put `@RefreshScope` on `ConfigRefreshScheduler` or on beans containing `@Scheduled` methods.
- Fixed polling suits development. For production, use a longer interval or push-based refresh with Spring Cloud Bus (RabbitMQ).
- Keep Git credentials out of images and source control; inject them as environment variables.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Service can't find its config | `spring.application.name` or active profile doesn't match the config repo |
| Startup fails with connection error | `CONFIG_SERVER_URL` is wrong or unreachable from the container |
| Value doesn't change after a push | Bean is missing `@RefreshScope`, or the Config Server hasn't fetched the new commit yet |
| `ContextRefresher` bean not found | `spring-cloud-starter-config` is missing from dependencies |