package bg.deck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Everything about the scheduled jobs that is worth changing without a build.
 *
 * <p>Defaults live here rather than only in {@code application.yml}, so a
 * profile that says nothing still starts with sensible timings.
 *
 * @param poolSize       threads kept for scheduled work
 * @param shutdownGrace  how long a shutdown waits for a job to finish
 * @param expiredLinks   the job that retires links nobody opened
 */
@ConfigurationProperties(prefix = "deck.scheduling")
public record SchedulingProperties(
        @DefaultValue("2") int poolSize,
        @DefaultValue("PT30S") Duration shutdownGrace,
        @DefaultValue Job expiredLinks
) {

    /**
     * One job's timing.
     *
     * @param interval     the gap between the end of one run and the start of
     *                     the next — a delay, not a rate, so a slow run can
     *                     never overlap the one behind it
     * @param initialDelay how long after startup the first run happens, leaving
     *                     the application to finish coming up first
     */
    public record Job(
            @DefaultValue("PT5M") Duration interval,
            @DefaultValue("PT1M") Duration initialDelay
    ) {
    }
}
