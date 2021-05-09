package crypto.com.marketdata;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import org.agrona.CloseHelper;
import org.agrona.concurrent.SigInt;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final int STREAM_ID = 88;
    private static final String CHANNEL = "aeron:udp?endpoint=localhost:8888";
    private static final long LINGER_TIMEOUT_MS = 0;
    private static final boolean EMBEDDED_MEDIA_DRIVER = true;

    private static final byte[][] stocks = new byte[][]{
            "TSLA".getBytes(StandardCharsets.UTF_8),
            "APPL".getBytes(StandardCharsets.UTF_8),
            "MCST".getBytes(StandardCharsets.UTF_8),
            "AMZN".getBytes(StandardCharsets.UTF_8),
            "GOOG".getBytes(StandardCharsets.UTF_8)
    };

    private static final double[] expectedReturns = new double[]{
            0.0563,
            0.0463,
            0.3013,
            0.0393,
            0.9963
    };

    private static final double[] annualizedStdDevs = new double[]{
            0.53,
            0.73,
            0.33,
            0.39,
            0.93
    };

    private static final double[] initialPrices = new double[]{
            684.90,
            132.54,
            251.86,
            3386.49,
            2343.08
    };

    private static final Random normalDistribution = new Random();

    public static final int CONSTANT = 7257600;

    public static void main(final String[] args) throws InterruptedException {
        System.out.println("Publishing to " + CHANNEL + " on stream id " + STREAM_ID);
        final MediaDriver driver = EMBEDDED_MEDIA_DRIVER ? MediaDriver.launchEmbedded() : null;
        final Aeron.Context ctx = new Aeron.Context();
        if (EMBEDDED_MEDIA_DRIVER) {
            ctx.aeronDirectoryName(driver.aeronDirectoryName());
        }
        try (Aeron aeron = Aeron.connect(ctx); Publication publication = aeron.addPublication(CHANNEL, STREAM_ID)) {
            final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocate(256));
            final AtomicBoolean running = new AtomicBoolean(true);
            SigInt.register(() -> running.set(false));
            while (running.get()) {
                for (int i = 0; i < stocks.length; i++) {
                    int timeToWait = ThreadLocalRandom.current().nextInt(500, 2000);
                    System.out.println("Next price update in " + timeToWait + "ms");
                    TimeUnit.MILLISECONDS.sleep(timeToWait);
                    double nextPrice = getNextPrice(i, timeToWait / 1000.0);
                    buffer.putBytes(0, stocks[i]);
                    buffer.putDouble(stocks[i].length, nextPrice);
                    if (publication.isConnected()) {
                        publication.offer(buffer);
                    }
                }
            }

            if (LINGER_TIMEOUT_MS > 0) {
                System.out.println("Lingering for " + LINGER_TIMEOUT_MS + " milliseconds...");
                Thread.sleep(LINGER_TIMEOUT_MS);
            }
        }

        CloseHelper.close(driver);
    }

    private static double getNextPrice(int i, double timeDelta) {
        return initialPrices[i]
                + initialPrices[i]
                * (expectedReturns[i] * timeDelta / CONSTANT
                + annualizedStdDevs[i] * normalDistribution.nextGaussian() * Math.sqrt(timeDelta / CONSTANT));
    }
}

