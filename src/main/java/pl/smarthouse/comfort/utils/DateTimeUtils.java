package pl.smarthouse.comfort.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
	public static String printWithTime(final Object message) {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))+" "+message.toString();
	}
}
