package pl.smarthouse.comfort.scheduler;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.smarthouse.comfort.configuration.ModuleConfiguration;
import pl.smarthouse.comfort.service.ActionService;
import pl.smarthouse.comfort.service.ModuleManagerService;
import pl.smarthouse.comfort.service.ModuleService;
import pl.smarthouse.loghandler.model.ErrorDto;
import pl.smarthouse.loghandler.service.LogService;
import pl.smarthouse.loghandler.utils.LogUtils;
import pl.smarthouse.module.config.ModuleConfig;
import pl.smarthouse.module.response.ModuleResponse;
import pl.smarthouse.module.utils.ModelMapper;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;

@ConditionalOnProperty(value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@AllArgsConstructor
@Service
public class EventScheduler {

  private static final String NO_IP_FOUND = "No IP found for mac address %s";
  private static final int MAX_RETRY_MS = 10 * 60 * 1000;
  private static int retryMs = 5000;
  ModuleConfiguration moduleConfig;
  ModuleManagerService moduleManagerService;
  LogService logService;
  ModuleService moduleService;
  ActionService actionService;

  @Scheduled(fixedDelay = 10000)
  public void eventScheduler() {
    retrieveModuleIP()
        .flatMap(ignore -> sendCommand())
        .doOnNext(
            moduleResponse -> {
              ModelMapper.copyResponseData(moduleConfig.getModuleConfig(), moduleResponse);
              actionService.run();
            })
        .doOnNext(ignore -> sendCommand())
        .doOnError(
            throwable -> {
              final ErrorDto errorDto =
                  LogUtils.error(moduleConfig.getModuleConfig().getType(), throwable.getMessage());
              logService.error(errorDto);
            })
        .subscribe();
  }

  private Mono<String> retrieveModuleIP() {
    final ModuleConfig config = moduleConfig.getModuleConfig();
    return Mono.justOrEmpty(config.getBaseUrl())
        .switchIfEmpty(
            Mono.defer(() -> moduleManagerService.getDBModuleIpAddress(config.getMacAddress())))
        .switchIfEmpty(
            Mono.error(new ConnectException(String.format(NO_IP_FOUND, config.getMacAddress()))))
        .doOnSuccess(
            (ip) -> {
              config.setBaseUrl(ip);
              retryMs = 5000;
            })
        .doOnError(
            throwable -> {
              retryMs = retryMs * 2;
              if (retryMs > MAX_RETRY_MS) {
                retryMs = MAX_RETRY_MS;
              }
              final ErrorDto errorDto = LogUtils.error(config.getType(), throwable.getMessage());
              logService.error(errorDto);
            })
        .retryWhen(Retry.fixedDelay(1, Duration.ofMillis(retryMs)));
  }

  private Mono<ModuleResponse> sendCommand() {
    return moduleService
        .sendCommandToModule()
        .doOnError(
            WebClientResponseException.class,
            ex -> {
              // When no configuration
              if (ex.getStatusCode() == HttpStatus.NOT_IMPLEMENTED) {
                moduleService.sendConfigurationToModule().subscribe();
                logService.info(
                    LogUtils.info(moduleConfig.getModuleConfig().getType(), "Send configuration"));
              } else {
                final ErrorDto errorDto =
                    LogUtils.error(moduleConfig.getModuleConfig().getType(), ex.getMessage());
                logService.error(errorDto);
              }
            });
  }
}
