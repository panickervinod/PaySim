package org.paysim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.paysim.parameters.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * This is a naieve approach to sanity checking any logic changes to the
 * PaySimState base class and its derivatives using a "gold standard"
 * output from a known good version of PaySim run for <b>10 steps</b>.
 */
public class SanityCheckTest {
    private static final String testLog = "/test_rawLog.csv.gz";

    private Parameters parameters;

    @BeforeEach
    void setup() {
        parameters = new Parameters("PaySim.properties");
    }

    @Test
    void throwsExceptionIfRunningTwice() {
        Assertions.assertThrows(IllegalStateException.class, () ->{
            IteratingPaySim sim = new IteratingPaySim(parameters);
            sim.run();
            sim.run();
        });
    }

    @Test
    void canAbortASimulation() throws InterruptedException {
        IteratingPaySim sim = new IteratingPaySim(parameters, 1);
        sim.run();
        Assertions.assertNotNull(sim.next());
        sim.abort();
        Assertions.assertNull(sim.next());
        Assertions.assertFalse(sim.hasNext());

        // XXX: This sucks, but we need to wait some time for the sim to finish since
        // there's no current way to pull the plug other than waiting for it to try
        Thread.sleep(500);
        int cnt = Thread.activeCount();
        Thread[] threads = new Thread[cnt];
        Thread.enumerate(threads);
        Assertions.assertFalse(Arrays.stream(threads)
                .anyMatch(thread -> thread.getName().startsWith(IteratingPaySim.WORKER_NAME)));
    }

    @Test
    void sanityCheckIteratingPaySim() throws Exception {
        Path path = Paths.get(getClass().getResource(testLog).toURI());
        GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(path));
        BufferedReader reader = new BufferedReader(new InputStreamReader(gzis));

        IteratingPaySim sim = new IteratingPaySim(parameters);
        sim.run();

        // XXX: yes, this reads all lines into memory...
        for (String line : reader.lines().collect(Collectors.toList())) {
            Assertions.assertEquals(line, sim.next().toString());
        }

        // This order is fragile due to current hasNext() logic.
        Assertions.assertNull(sim.next());
        Assertions.assertFalse(sim.hasNext());
    }
}