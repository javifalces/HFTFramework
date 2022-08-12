package com.lambda.investing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class App {

	protected final ApplicationContext ac;
	protected final Logger logger;

	static {
		//UTC -Duser.timezone=GMT

	}

	public static void main(String[] args) {
		try {

			System.setProperty("user.timezone", "GMT");

			new App(args);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	protected App(String[] springApplicationContextFiles) {

		try {
			if (springApplicationContextFiles.length > 0) {
				ac = new ClassPathXmlApplicationContext(springApplicationContextFiles);
			} else {
				ac = new ClassPathXmlApplicationContext(new String[] { "classpath:beans.xml" });
			}
		} catch (BeansException be) {
			logger = LogManager.getLogger();
			logger.fatal("Unable to load Spring application context files", be);
			throw be;
		}

		logger = LogManager.getLogger();
		//		logger.info("");
		//
		//		logger.info("");
		//		logger.info("JVM arguments: " + ManagementFactory.getRuntimeMXBean().getInputArguments());
		//
		//		logger.info("");
		//		logger.info("Program arguments: " + Arrays.toString(springApplicationContextFiles));
		//
		//		logger.info("");
		//
		//		logger.info("System environment:");
		//		for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
		//			logger.info(entry.getKey() + " -> " + entry.getValue());
		//		}
		//
		//		logger.info("");
		//		logger.info("System properties: ");
		//		String lastElem;
		//		String elem;
		//		List<String> listPropertyValue;
		//		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
		//			elem = entry.getKey().toString();
		//			listPropertyValue = Arrays.asList(elem.split("\\."));
		//			lastElem = listPropertyValue.get(listPropertyValue.size() - 1);
		//			logger.info(entry.getKey() + " -> " + entry.getValue());
		//		}
		//		logger.info("");

	}

}

