package pl.smarthouse.comfort.service;

import org.springframework.stereotype.Service;
import pl.smarthouse.modulemanager.configuration.ModuleManagerConfiguration;
import reactor.core.publisher.Mono;

@Service
public class ModuleManagerService {
  ModuleManagerConfiguration moduleManagerConfiguration = new ModuleManagerConfiguration();

  public Mono<String> getDBModuleIpAddress(final String macAddress) {
    return moduleManagerConfiguration
        .webClient()
        .get()
        .uri("/ip?macAddress=" + macAddress)
        .retrieve()
        .bodyToMono(String.class);
  }
}
