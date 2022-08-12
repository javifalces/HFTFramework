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
 * Preferences.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j;

import org.deeplearning4j.nn.conf.WorkspaceMode;

/**
 * Preferences class for Deeplearning4j/Nd4j static settings that should be used across the
 * package.
 *
 * @author Steven Lang
 */
public class Preferences {

	/**
	 * Global workspace mode
	 */
	public static WorkspaceMode WORKSPACE_MODE = WorkspaceMode.ENABLED;
}