/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BCLog {
    public static final Logger logger = LoggerFactory.getLogger("BuildCraft");

    /** Deactivate constructor */
    private BCLog() {}

    public static void logErrorAPI(Throwable error, Class<?> classFile) {
        StringBuilder msg = new StringBuilder("API error! Please update your mods. Error: ");
        msg.append(error);
        StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace.length > 0) {
            msg.append(", ").append(stackTrace[0]);
        }

        logger.error(msg.toString());

        if (classFile != null) {
            msg.append("API error: ").append(classFile.getSimpleName()).append(" is loaded from ")
                .append(classFile.getProtectionDomain().getCodeSource().getLocation());
            logger.error(msg.toString());
        }
    }
}
