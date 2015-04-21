package ru.mmb.datacollector.converter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataConverter {
	private static final Logger logger = LogManager.getLogger(DataConverter.class);

	private static DataConverterThread converterThread;
	private static final BlockingQueue<ConvertRequest> converterQueue = new ArrayBlockingQueue<DataConverter.ConvertRequest>(
			100);

	public static void offerRequest() {
		converterQueue.offer(new ConvertRequest());
	}

	public static void init() {
		converterThread = new DataConverterThread(converterQueue);
		converterThread.start();
		logger.info("data converter thread started");
	}

	public static void stop() {
		converterThread.terminate();
	}

	public static class ConvertRequest {
	}
}
