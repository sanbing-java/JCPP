/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程：https://www.bilibili.com/cheese/play/ss942400790
 */
package sanbing.jcpp.infrastructure.queue.discovery;

import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import sanbing.jcpp.infrastructure.queue.discovery.event.OtherServiceShutdownEvent;
import sanbing.jcpp.infrastructure.util.annotation.AfterStartUp;
import sanbing.jcpp.infrastructure.util.async.JCPPThreadFactory;
import sanbing.jcpp.proto.gen.ClusterProto.ServiceInfo;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true")
@Slf4j
public class ZkDiscoveryProvider implements DiscoveryProvider, CuratorCacheListener {

    @Value("${zk.url}")
    private String zkUrl;
    @Value("${zk.retry-interval-ms}")
    private Integer zkRetryInterval;
    @Value("${zk.connection-timeout-ms}")
    private Integer zkConnectionTimeout;
    @Value("${zk.session-timeout-ms}")
    private Integer zkSessionTimeout;
    @Value("${zk.zk-dir}")
    private String zkDir;
    @Value("${zk.recalculate-delay:0}")
    private Long recalculateDelay;

    protected final ConcurrentHashMap<String, ScheduledFuture<?>> delayedTasks;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ServiceInfoProvider serviceInfoProvider;
    private final PartitionProvider partitionProvider;

    private ScheduledExecutorService zkExecutorService;
    private CuratorFramework client;
    private CuratorCache cache;
    private String nodePath;
    private String zkNodesDir;

    private volatile boolean stopped = true;

    public ZkDiscoveryProvider(ApplicationEventPublisher applicationEventPublisher,
                               ServiceInfoProvider serviceInfoProvider,
                               PartitionProvider partitionProvider) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionProvider = partitionProvider;
        delayedTasks = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        log.info("Discovery Provider Initializing...");
        Assert.hasLength(zkUrl, missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, missingProperty("zk.retry-interval-ms"));
        Assert.notNull(zkConnectionTimeout, missingProperty("zk.connection-timeout-ms"));
        Assert.notNull(zkSessionTimeout, missingProperty("zk-session-timeout-ms"));

        zkExecutorService = Executors.newSingleThreadScheduledExecutor(JCPPThreadFactory.forName("zk-discovery"));

        zkNodesDir = zkDir + "/nodes";
        initZkClient();

        log.info("Initialization completed, using ZK connect string: {}", zkUrl);
    }

    @Override
    public List<ServiceInfo> getOtherServers() {
        return cache.stream()
                .filter(cd -> !cd.getPath().equals(nodePath) && !cd.getPath().equals(zkNodesDir))
                .map(cd -> {
                    try {
                        return ServiceInfo.parseFrom(cd.getData());
                    } catch (NoSuchElementException | InvalidProtocolBufferException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @AfterStartUp(order = AfterStartUp.DISCOVERY_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (stopped) {
            log.debug("Ignoring application ready event. Service is stopped.");
            return;
        } else {
            log.info("Received application ready event. Starting current ZK node.");
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring application ready event, ZK client is not started, ZK client state [{}]", client.getState());
            return;
        }
        log.info("Going to publish current server...");
        publishCurrentServer();
        log.info("Going to recalculate partitions...");
        recalculatePartitions();

        zkExecutorService.scheduleAtFixedRate(this::publishCurrentServer, 1, 1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    public synchronized void publishCurrentServer() {
        ServiceInfo self = serviceInfoProvider.getServiceInfo();
        if (currentServerExists()) {
            log.trace("[{}] Updating ZK node for current instance: {}", self.getServiceId(), nodePath);
            client.setData().forPath(nodePath, serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo().toByteArray());
        } else {
            try {
                log.info("[{}] Creating ZK node for current instance", self.getServiceId());
                nodePath = client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                        .forPath(zkNodesDir + "/node-", self.toByteArray());
                log.info("[{}] Created ZK node for current instance: {}", self.getServiceId(), nodePath);
                client.getConnectionStateListenable().addListener(checkReconnect(self));
            } catch (Exception e) {
                log.error("Failed to create ZK node", e);
                throw new RuntimeException(e);
            }
        }
    }

    private boolean currentServerExists() {
        if (nodePath == null) {
            return false;
        }
        try {
            ServiceInfo self = serviceInfoProvider.getServiceInfo();
            ServiceInfo registeredServerInfo = ServiceInfo.parseFrom(client.getData().forPath(nodePath));
            if (self.equals(registeredServerInfo)) {
                return true;
            }
        } catch (KeeperException.NoNodeException e) {
            log.info("ZK node does not exist: {}", nodePath);
        } catch (Exception e) {
            log.error("Couldn't check if ZK node exists", e);
        }
        return false;
    }

    private ConnectionStateListener checkReconnect(ServiceInfo self) {
        return (client, newState) -> {
            log.info("[{}] ZK state changed: {}", self.getServiceId(), newState);
            if (newState == ConnectionState.LOST) {
                zkExecutorService.submit(this::reconnect);
            }
        };
    }

    private volatile boolean reconnectInProgress = false;

    private synchronized void reconnect() {
        if (!reconnectInProgress) {
            reconnectInProgress = true;
            try {
                destroyZkClient();
                initZkClient();
                publishCurrentServer();
            } catch (Exception e) {
                log.error("Failed to reconnect to ZK: {}", e.getMessage(), e);
            } finally {
                reconnectInProgress = false;
            }
        }
    }

    private void initZkClient() {
        try {
            client = CuratorFrameworkFactory.newClient(zkUrl, zkSessionTimeout, zkConnectionTimeout, new RetryForever(zkRetryInterval));
            client.start();
            client.blockUntilConnected();
            cache = CuratorCache.builder(client, zkNodesDir).build();
            cache.listenable().addListener(this);
            cache.start();
            stopped = false;
            log.info("ZK client connected");
        } catch (Exception e) {
            log.error("Failed to connect to ZK: {}", e.getMessage(), e);
            CloseableUtils.closeQuietly(cache);
            CloseableUtils.closeQuietly(client);
            throw new RuntimeException(e);
        }
    }

    private void unpublishCurrentServer() {
        try {
            if (nodePath != null) {
                client.delete().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete ZK node {}", nodePath, e);
            throw new RuntimeException(e);
        }
    }

    private void destroyZkClient() {
        stopped = true;
        try {
            unpublishCurrentServer();
        } catch (Exception ignored) {
        }
        CloseableUtils.closeQuietly(cache);
        CloseableUtils.closeQuietly(client);
        log.info("ZK client disconnected");
    }

    @PreDestroy
    public void destroy() {
        destroyZkClient();
        zkExecutorService.shutdownNow();
        log.info("Stopped zk discovery service");
    }

    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    @Override
    public void event(Type type, ChildData oldData, ChildData data) {
        if (stopped) {
            log.info("Ignoring {}. Service is stopped.", type);
            return;
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.info("Ignoring {}, ZK client is not started, ZK client state [{}]", type, client.getState());
            return;
        }

        switch (type) {
            case NODE_CREATED -> {
                if (data == null || data.getData() == null) {
                    log.info("Ignoring {} due to empty created data", type);
                    return;
                }
                String serviceId = getServiceId(type, data);

                ScheduledFuture<?> task = delayedTasks.remove(serviceId);
                if (task != null) {
                    if (task.cancel(false)) {
                        log.info("[{}] Recalculate partitions ignored. Service was restarted in time.", serviceId);
                    } else {
                        log.info("[{}] Going to recalculate partitions. Service was not restarted in time!", serviceId);
                        recalculatePartitions();
                    }
                } else {
                    log.info("[{}] Going to recalculate partitions due to adding new node.",
                            serviceId);
                    recalculatePartitions();
                }
            }
            case NODE_DELETED -> {
                if (oldData == null || oldData.getData() == null) {
                    log.info("Ignoring {} due to empty delete data", type);
                    return;
                } else if (nodePath != null && nodePath.equals(oldData.getPath())) {
                    log.info("ZK node for current instance is somehow deleted.");
                    publishCurrentServer();
                    return;
                }
                String serviceId = getServiceId(type, oldData);

                zkExecutorService.submit(() -> applicationEventPublisher.publishEvent(new OtherServiceShutdownEvent(this, serviceId)));
                ScheduledFuture<?> future = zkExecutorService.schedule(() -> {
                    log.info("[{}] Going to recalculate partitions due to removed node", serviceId);
                    ScheduledFuture<?> removedTask = delayedTasks.remove(serviceId);
                    if (removedTask != null) {
                        recalculatePartitions();
                    }
                }, recalculateDelay, TimeUnit.MILLISECONDS);
                delayedTasks.put(serviceId, future);
            }
            default -> {
            }
        }
    }

    private static String getServiceId(Type type, ChildData data) {
        ServiceInfo instance;
        try {
            instance = ServiceInfo.parseFrom(data.getData());
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to decode server instance for node {}", data.getPath(), e);
            throw new RuntimeException(e);
        }

        String serviceId = instance.getServiceId();

        log.info("Processing [{}] event for [{}]", type, serviceId);
        return serviceId;
    }

    synchronized void recalculatePartitions() {
        delayedTasks.values().forEach(future -> future.cancel(false));
        delayedTasks.clear();
        partitionProvider.recalculatePartitions(serviceInfoProvider.getServiceInfo(), getOtherServers());
    }
}
