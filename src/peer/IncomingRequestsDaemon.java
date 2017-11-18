package peer;

import Util.Pair;
import dto.SendPiecedFileRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by xrusa on 28/5/2017.
 *
 * This class will be responsible for holding all incoming requests by the other peers.
 * It is responsible for waiting the appropriate amount of time between the first request and the call of the send function.
 * The class is supplied with a function to call when there are requests to be served.
 * This call is done on another thread in order not to block the daemon thread. For example, we don't want to delay
 * the next bulk of requests if the previous is not served yet due to low network speed.
 */
public class IncomingRequestsDaemon {

    /**
     * This executor service is tha daemon service. It is responsible for scanning the sorted set, finding the
     * requests to be served, delegating to the other executor service that will call the actual handling method,
     * removing the old requests and sleeping the appropriate amount of time before waking up and repeating.
     */
    private final ExecutorService requestDaemonExecutor;

    /**
     * This set is sorted according to the time that the requests were sent to this peer. This helps the daemon
     * to iterate on the set and stop at the first request of which the time sent is greater than the
     * time of the first request + wait time.
     */
    private final SortedSet<Pair<Instant, SendPiecedFileRequest>> incomingRequestsWithTimestamp;

    /**
     * This is the time that the daemon should wait between the first request before collecting the requests
     * that were sent between the time of the first request and that time + waitTimeMs. It is different between
     * peers and seeders and hence can be changed later.
     */
    private long waitTimeMs;

    /**
     * This is the method that should be called when there is at least one request to be served. It is a method
     * that returns null and takes as input a collection of requests. This method will run on a different thread
     * so that it doesn't delay the daemon.
     */
    private Consumer<Collection<SendPiecedFileRequest>> handlerMethod;

    public IncomingRequestsDaemon() {
        requestDaemonExecutor = Executors.newSingleThreadExecutor();
        // ConcurrentSkipListSet is a data structure that is suitable for multithreaded applications
        // that need a sorted set.
        incomingRequestsWithTimestamp = new ConcurrentSkipListSet<>(Comparator.comparing(Pair::getLeft));
    }

    /**
     * Starts the daemon that will check for incoming requests.
     */
    public void startDaemon() {
        requestDaemonExecutor.submit(this::doDaemonLoop);
    }

    public void stopDaemon() {
        requestDaemonExecutor.shutdownNow();
    }

    private void doDaemonLoop() {
        // this will run while the executor service is alive
        while (!requestDaemonExecutor.isShutdown() && !Thread.currentThread().isInterrupted()) {
            Collection<SendPiecedFileRequest> requestsToHandle = findRequestsToServe();
            if (!requestsToHandle.isEmpty()) {
                // call the handler method from another thread.
                CompletableFuture.runAsync(() -> handlerMethod.accept(requestsToHandle));
            }
            waitForMoreConnections();
        }
    }

    private void waitForMoreConnections() {
        // wake up at exactly the next latest time, or if not such time exists, wake up at waitTimeMs
        // to recheck for incoming requests.
        Instant wakeUpInstant = findLatestTime().orElse(Instant.now().plusMillis(waitTimeMs));
        try {
            Thread.sleep(Instant.now().until(wakeUpInstant, ChronoUnit.MILLIS));
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private Collection<SendPiecedFileRequest> findRequestsToServe() {
        Collection<SendPiecedFileRequest> requestsToHandle = new ArrayList<>();
        findLatestTime()
                // do the calculations only if the latest time is equal to the current time or before the current time.
                // if that is not true, then the daemon should wait for more requests that may arrive
                .filter(latestTime -> latestTime.equals(Instant.now()) || latestTime.isBefore(Instant.now()))
                .ifPresent(latestTime -> {
            Iterator<Pair<Instant, SendPiecedFileRequest>> it = incomingRequestsWithTimestamp.iterator();
            while (it.hasNext()) {
                Pair<Instant, SendPiecedFileRequest> currentRequest = it.next();
                if (currentRequest.getLeft().isBefore(latestTime)) {
                    // if the time is before the latest time, add the request to the returning list and
                    // remove it from the set.
                    requestsToHandle.add(currentRequest.getRight());
                    it.remove();
                } else {
                    break;
                }
            }
        });

        return requestsToHandle;
    }

    private Optional<Instant> findLatestTime() {
        Iterator<Pair<Instant, SendPiecedFileRequest>> it = incomingRequestsWithTimestamp.iterator();
        Instant latestTime = null;
        if (it.hasNext()) {

            Pair<Instant, SendPiecedFileRequest> currentRequest = it.next();
            // the latest time is the time of the first request + the wait time ms
            latestTime = currentRequest.getLeft().plusMillis(waitTimeMs);
        }
        return Optional.ofNullable(latestTime);
    }

    /**
     * Adds a request to the requests queue.
     * @param request The request to add.
     */
    public void addRequest(SendPiecedFileRequest request) {
        incomingRequestsWithTimestamp.add(new Pair<>(Instant.now(), request));
    }

    public long getWaitTimeMs() {
        return waitTimeMs;
    }

    public void setWaitTimeMs(long waitTimeMs) {
        this.waitTimeMs = waitTimeMs;
    }

    public Consumer<Collection<SendPiecedFileRequest>> getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(Consumer<Collection<SendPiecedFileRequest>> handlerMethod) {
        this.handlerMethod = handlerMethod;
    }
}
