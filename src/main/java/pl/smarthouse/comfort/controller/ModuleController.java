package pl.smarthouse.comfort.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.smarthouse.comfort.configuration.ModuleConfiguration;
import pl.smarthouse.comfort.service.ModuleService;
import pl.smarthouse.module.command.ModuleCommand;
import pl.smarthouse.module.config.model.ModuleConfigDto;
import pl.smarthouse.module.utils.CommandUtils;

@AllArgsConstructor
@RestController
public class ModuleController {

  ModuleConfiguration moduleConfiguration;
  ModuleService moduleService;

  @GetMapping(value = "/config", produces = "application/json")
  public ModuleConfigDto getConfiguration() {
    return moduleConfiguration.getModuleConfigDto();
  }

  @GetMapping(value = "/command", produces = "application/json")
  public ModuleCommand getCommand() {
    return CommandUtils.getCommandBody(moduleConfiguration.getModuleConfig());
  }
}
