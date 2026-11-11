

import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigRefreshScheduler {

    private final ContextRefresher contextRefresher;

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void refreshConfig() {

        try {
            var changedKeys = contextRefresher.refresh();

            if (!changedKeys.isEmpty()) {
                log.info("Configuration updated successfully. Changed keys: {}", changedKeys);
            }

        } catch (Exception e) {
            log.error("Failed to refresh configuration: {}", e.getMessage(), e);
        }
    }
}