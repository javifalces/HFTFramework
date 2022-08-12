/*
 * WekaDeeplearning4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WekaDeeplearning4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WekaDeeplearning4j.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Logger.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import weka.dl4j.Utils;
import weka.gui.FilePropertyMetadata;
import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * General logger configuration.
 *
 * @author Steven Lang
 */
@Log4j2 public class LogConfiguration implements Serializable, OptionHandler {

	private static final long serialVersionUID = 7910114399022582661L;
	/**
	 * The file that log information will be written to.
	 */
	protected File logFile = new File(
			Paths.get(WekaPackageManager.WEKA_HOME.getAbsolutePath(), "wekaDeeplearning4j.log").toString());

	/**
	 * WekaDl4j Log level.
	 */
	protected LogLevel wekaDl4jLogLevel = LogLevel.INFO;

	/**
	 * ND4J Log level.
	 */
	protected LogLevel nd4jLogLevel = LogLevel.INFO;

	/**
	 * DL4J Log level.
	 */
	protected LogLevel dl4jLogLevel = LogLevel.WARN;

	/**
	 * If logs should be appended to an existing file.
	 */
	protected boolean append = true;

	/**
	 * Get the log file
	 *
	 * @return the log file
	 */
	public File getLogFile() {
		return logFile;
	}

	/**
	 * Set the log file
	 *
	 * @param logFile the log file
	 */
	@FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.SAVE_DIALOG, directoriesOnly = false) @OptionMetadata(displayName = "log file", description =
			"The name of the log file to write loss information to "
					+ "(default = $WEKA_HOME/wekaDeeplearning4j.log).", commandLineParamName = "logFile", commandLineParamSynopsis = "-logFile <string>", displayOrder = 1) public void setLogFile(
			File logFile) {
		this.logFile = logFile;
	}

	public boolean isAppend() {
		return append;
	}

	/**
	 * Whether to append to the existing log file or not.
	 *
	 * @param append Append to the existing log file or not
	 */
	@FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.SAVE_DIALOG, directoriesOnly = false) @OptionMetadata(displayName = "append", description = "Whether to append new logs to the log file (if it exists).", commandLineParamName = "append", commandLineParamSynopsis = "-append", displayOrder = 4) public void setAppend(
			boolean append) {
		this.append = append;
	}

	/**
	 * Get the Nd4j log level.
	 *
	 * @return Nd4j log level
	 */
	public LogLevel getNd4jLogLevel() {
		return nd4jLogLevel;
	}

	/**
	 * Set the Nd4j log level.
	 *
	 * @param nd4jLogLevel The nd4j log level
	 */
	@OptionMetadata(displayName = "nd4j log level", description = "The log level for Nd4j.", commandLineParamName = "nd4jLogLevel", commandLineParamSynopsis = "-nd4jLogLevel <LogLevel>", displayOrder = 3) public void setNd4jLogLevel(
			LogLevel nd4jLogLevel) {
		this.nd4jLogLevel = nd4jLogLevel;
		updateNd4jLogLevel();
	}

	/**
	 * Update the nd4j log level.
	 */
	protected void updateNd4jLogLevel() {
		updateLogLevel("org.nd4j", nd4jLogLevel);
	}

	/**
	 * Get the Dl4j log level.
	 *
	 * @return Dl4j log level
	 */
	public LogLevel getDl4jLogLevel() {
		return dl4jLogLevel;
	}

	/**
	 * Set the Dl4j log level.
	 *
	 * @param dl4jLogLevel The nd4j log level
	 */
	@OptionMetadata(displayName = "dl4j log level", description = "The log level for Dl4j.", commandLineParamName = "dl4jLogLevel", commandLineParamSynopsis = "-dl4jLogLevel <LogLevel>", displayOrder = 2) public void setDl4jLogLevel(
			LogLevel dl4jLogLevel) {
		this.dl4jLogLevel = dl4jLogLevel;
		updateDl4jLogLevel();
	}

	/**
	 * Update the dl4j log level.
	 */
	protected void updateDl4jLogLevel() {
		updateLogLevel("org.deeplearning4j", dl4jLogLevel);
	}

	/**
	 * Get the WekaDeeplearning4j log level.
	 *
	 * @return WekaDeeplearning4j log level
	 */
	public LogLevel getWekaDl4jLogLevel() {
		return wekaDl4jLogLevel;
	}

	/**
	 * Set the WekaDeeplearning4j log level.
	 *
	 * @param wekaDl4jLogLevel The nd4j log level
	 */
	@OptionMetadata(displayName = "wekaDl4j log level", description = "The log level for WekaDeeplearning4j.", commandLineParamName = "wekaDl4jLogLevel", commandLineParamSynopsis = "-wekaDl4jLogLevel <LogLevel>", displayOrder = 1) public void setWekaDl4jLogLevel(
			LogLevel wekaDl4jLogLevel) {
		this.wekaDl4jLogLevel = wekaDl4jLogLevel;
	}

	/**
	 * Update the weka dl4j log level.
	 */
	protected void updateWekaDl4jLogLevel() {
		LoggerContext context = getLoggerContext();
		Collection<Logger> loggers = context.getLoggers();
		for (Logger logger : loggers) {
			if (logger.getName().startsWith("weka")) {
				updateLogLevel(logger.getName(), wekaDl4jLogLevel);
			}
		}
	}

	/**
	 * Access the Log4j2 logger context.
	 *
	 * @return Logger context
	 */
	private LoggerContext getLoggerContext() {
		return (LoggerContext) LogManager.getContext(false);
	}

	/**
	 * Apply the logging configuration.
	 */
	public void apply() {
		LoggerContext context = getLoggerContext();
		Configuration config = context.getConfiguration();
		ConfigurationSource configSource = config.getConfigurationSource();
		String packageHomeDir = Utils.defaultFileLocation();
		if (ConfigurationSource.NULL_SOURCE.equals(configSource)) {
			// Use log4j2.xml shipped with the package ...
			URI uri = Paths.get(packageHomeDir, "wekaDeeplearning4j", "src", "main", "resources", "log4j2.xml").toUri();
			context.setConfigLocation(uri);
			log.info("Logging configuration loaded from source: {}", uri.toString());
		}

		String fileAppenderName = "fileAppender";
		if (!context.getRootLogger().getAppenders().containsKey(fileAppenderName)) {
			// Get console appender layout
			Appender consoleAppender = context.getLogger(log.getName()).getAppenders().get("Console");
			Layout<? extends Serializable> layout = consoleAppender.getLayout();

			//      // Add file appender
			//      String filePath = resolveLogFilePath();
			//      FileAppender.Builder appenderBuilder = new FileAppender.Builder();
			//      appenderBuilder.withFileName(filePath);
			//      appenderBuilder.withAppend(append);
			//      appenderBuilder.withName(fileAppenderName);
			//      appenderBuilder.withLayout(layout);
			//      FileAppender appender = appenderBuilder.build();
			//      appender.start();
			//      context.getRootLogger().addAppender(appender);
		}
	}

	/**
	 * Resolves log file path.
	 *
	 * @return Log file path
	 */
	protected String resolveLogFilePath() {
		Environment env = Environment.getSystemWide();
		String resolved = logFile.toString();
		try {
			resolved = env.substitute(resolved);
		} catch (Exception ex) {
			// ignore
		}

		return new File(resolved).getPath();
	}

	/**
	 * Update the log level of a specific logger.
	 *
	 * @param loggerName Logger name
	 * @param level      Log level
	 */
	protected void updateLogLevel(String loggerName, LogLevel level) {
		Configurator.setLevel(loggerName, level.getLevel());
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the log configuration
	 *
	 * @return return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	@Override public void setOptions(String[] options) throws Exception {
		Option.setOptions(options, this, this.getClass());
	}

	/**
	 * Available log levels.
	 *
	 * @author Steven Lang
	 */
	public enum LogLevel {
		ALL(Level.ALL), TRACE(Level.TRACE), DEBUG(Level.DEBUG), INFO(Level.INFO), WARN(Level.WARN), ERROR(
				Level.ERROR), FATAL(Level.FATAL), OFF(Level.OFF);

		/**
		 * Internal log4j2 log level.
		 */
		private Level level;

		/**
		 * Constructor with log level from log4j2.
		 *
		 * @param level Log level
		 */
		LogLevel(Level level) {
			this.level = level;
		}

		/**
		 * Get the internal log level.
		 *
		 * @return Log level
		 */
		public Level getLevel() {
			return level;
		}
	}
}
