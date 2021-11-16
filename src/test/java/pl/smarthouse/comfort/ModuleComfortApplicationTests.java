package pl.smarthouse.comfort;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import pl.smarthouse.comfort.configuration.ModuleConfiguration;
import pl.smarthouse.comfort.service.ModuleManagerService;
import pl.smarthouse.loghandler.service.LogService;
import pl.smarthouse.loghandler.utils.LogUtils;
import pl.smarthouse.module.config.ModuleConfig;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;

@SpringBootTest
@TestPropertySource(properties = "app.scheduling.enable=false")
class ModuleComfortApplicationTests {
	@Autowired
	ModuleConfiguration moduleConfig;
	@Autowired
	ModuleManagerService moduleManagerService;
	@Autowired
	LogService logService;


	private static final String NO_IP_FOUND = "No IP found for mac address %s";
	private static final int MAX_RETRY_MS = 10 * 60 * 1000;

	private static int retryMs = 5000;


	@Test
	void webFluxTest() throws InterruptedException {
		retrieveModuleIP().subscribe();
		Thread.sleep(5000);

		System.out.println(moduleConfig.getModuleConfig().getBaseUrl());
	}

	private Mono<String> retrieveModuleIP() {
		ModuleConfig config = moduleConfig.getModuleConfig();
		return Mono.justOrEmpty(config.getBaseUrl())
				.switchIfEmpty(moduleManagerService.getDBModuleIpAddress(config.getMacAddress()))
				.switchIfEmpty(Mono.error(new ConnectException(String.format(NO_IP_FOUND, config.getMacAddress()))))
				.doOnSuccess((ip) -> config.setBaseUrl(ip))
				.doOnError(
						throwable -> {
							logService.error(LogUtils.error(config.getType(), throwable.getMessage()));
						})
				.retryWhen(Retry.fixedDelay(1, Duration.ofMillis(retryMs)))
				.doOnSuccess(ignore -> retryMs = 1000)
				.doOnError(
						throwable -> {
							retryMs = retryMs * 2;
							if (retryMs > MAX_RETRY_MS) {
								retryMs = MAX_RETRY_MS;
							}
							logService.error(LogUtils.error(config.getType(), throwable.getMessage()));
						});
	}
}
