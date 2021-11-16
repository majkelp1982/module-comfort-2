package pl.smarthouse.comfort.service;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.smarthouse.comfort.configuration.ModuleConfiguration;
import pl.smarthouse.module.command.ModuleCommand;
import pl.smarthouse.module.response.ModuleResponse;
import pl.smarthouse.module.utils.CommandUtils;
import pl.smarthouse.module.utils.ModelMapper;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ModuleService {
  private final WebClient webClient;
  ModuleConfiguration moduleConfig;

  public Mono<ModuleResponse> sendCommandToModule() {
    final ModuleCommand moduleCommand = CommandUtils.getCommandBody(moduleConfig.getModuleConfig());
    if (moduleCommand.getPinCommandSet().isEmpty()
        && moduleCommand.getSensorCommandSet().isEmpty()) {
      return Mono.empty();
    }
    return webClient
        .post()
        .uri(moduleConfig.getModuleConfig().getBaseUrl() + "/action")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CommandUtils.getCommandBody(moduleConfig.getModuleConfig()))
        .retrieve()
        .bodyToMono(ModuleResponse.class);
  }

  public Mono<String> sendConfigurationToModule() {
    return webClient
        .post()
        .uri(moduleConfig.getModuleConfig().getBaseUrl() + "/configuration")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(ModelMapper.getConfigDto(moduleConfig.getModuleConfig()))
        .retrieve()
        .bodyToMono(String.class);
  }
}
