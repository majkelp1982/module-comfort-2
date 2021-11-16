package pl.smarthouse.comfort.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import pl.smarthouse.comfort.configuration.ModuleConfiguration;
import pl.smarthouse.comfort.utils.DateTimeUtils;
import pl.smarthouse.loghandler.service.LogService;
import pl.smarthouse.loghandler.utils.LogUtils;
import pl.smarthouse.module.GPO.model.PinDao;
import pl.smarthouse.module.sensors.model.sensorBME280SPI.SensorBME280SPIResponse;
import pl.smarthouse.module.utils.ModelMapper;
import reactor.core.publisher.Mono;

import static pl.smarthouse.comfort.constants.Sensors.BME280;

@Service
public class ActionService {

	ModuleConfiguration moduleConfig;
	ExternalModuleService externalModuleService;
	LogService logService;

	private SensorBME280SPIResponse bme280;
	private PinDao pinDao;

	public ActionService(
			final ModuleConfiguration moduleConfig,
			final ExternalModuleService externalModuleService,
			final LogService logService) {
		this.moduleConfig = moduleConfig;
		this.externalModuleService = externalModuleService;
		this.logService = logService;
	}

	public Mono<Void> run() {
		try {
			updateModuleData();
		} catch (final JsonProcessingException e) {
			Mono.error(e);
		}

			System.out.println(DateTimeUtils.printWithTime(bme280));
		//		temporarySendToExternalModule();
		return Mono.empty();
	}

	private void updateModuleData() throws JsonProcessingException {
		bme280 = SensorBME280SPIResponse.map(ModelMapper.findSensor(moduleConfig.getModuleConfig(), BME280).getResponse());
	}

	private void temporarySendToExternalModule() {
		externalModuleService
				.sendBME280DataToExternalModule(bme280)
				.doOnSuccess(
						s -> {
							System.out.println(s);
						})
				.doOnError(error -> logService.error(LogUtils.error(moduleConfig.getModuleConfig().getType(), error.getMessage())).subscribe())
				.subscribe();
	}
}
