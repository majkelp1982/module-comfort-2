package pl.smarthouse.comfort.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.smarthouse.module.sensors.model.sensorBME280SPI.SensorBME280SPIResponse;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ExternalModuleService {
  WebClient externalModuleWebClient;

  public Mono<String> sendBME280DataToExternalModule(final SensorBME280SPIResponse response) {
    return externalModuleWebClient
        .post()
        .uri("/action")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(getCommand(response))
        .retrieve()
        .bodyToMono(String.class);
  }

  private String getCommand(final SensorBME280SPIResponse response) {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode node = mapper.createObjectNode();
    node.put("zoneNumber", 4);
    node.put("temperature", response.getTemperature());
    node.put("humidity", response.getHumidity());
    String result = null;
    try {
      result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    } catch (final JsonProcessingException e) {
      e.printStackTrace();
    }
    return result;
  }
}
