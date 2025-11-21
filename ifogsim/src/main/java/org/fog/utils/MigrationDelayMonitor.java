package org.fog.utils;

public class MigrationDelayMonitor {
	
	private static double migrationDelay = 0.0;
	
	public static double getMigrationDelay() {
		return migrationDelay;
	}

	public static void setMigrationDelay(double migrationDelayReceived) {
		migrationDelay += migrationDelayReceived;
	}
}
