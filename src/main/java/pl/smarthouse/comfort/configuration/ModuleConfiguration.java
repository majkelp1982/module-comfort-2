package pl.smarthouse.comfort.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import pl.smarthouse.module.config.ModuleConfig;
import pl.smarthouse.module.config.model.ModuleConfigDto;
import pl.smarthouse.module.sensors.model.SensorDao;
import pl.smarthouse.module.sensors.model.enums.SensorAction;
import pl.smarthouse.module.sensors.model.sensorBME280SPI.SensorBME280SPIDao;
import pl.smarthouse.module.utils.ModelMapper;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static pl.smarthouse.comfort.constants.Module.*;
import static pl.smarthouse.comfort.constants.Sensors.BME280;

@Configuration
@Setter
@Getter
public class ModuleConfiguration {

  private final ModuleConfig moduleConfig;

  public ModuleConfiguration() {
    moduleConfig =
        ModuleConfig.builder()
            .type(MODULE_NAME)
            .version(VERSION)
            .macAddress(MAC_ADDRESS)
            .sensorDaoSet(getSensorsDao())
            .build();
  }

  public ModuleConfigDto getModuleConfigDto() {
    return ModelMapper.getConfigDto(moduleConfig);
  }

  private Set<SensorDao> getSensorsDao() {
    final Set<SensorDao> sensorDaoSet = new HashSet<>();
    sensorDaoSet.add(
        SensorBME280SPIDao.builder()
            .name(BME280)
            .csPin(4)
            .lastActionTimeStamp(LocalDateTime.MIN)
            .action(SensorAction.READ)
            .build());
    return sensorDaoSet;
  }

  @Bean
  WebClient webClient() {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient()))
        .build();
  }

  private HttpClient httpClient() {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .responseTimeout(Duration.ofMillis(5000))
        .doOnConnected(
            conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));
  }
}
