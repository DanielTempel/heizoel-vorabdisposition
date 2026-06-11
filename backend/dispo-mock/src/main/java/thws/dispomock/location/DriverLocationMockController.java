package thws.dispomock.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/dispo/confirmation-status-updates/tracking/orders")
public class DriverLocationMockController {

    private static final List<double[]> DRIVER_ROUTE = List.of(
            new double[]{9.8820D, 49.8166D},
            new double[]{9.8974D, 49.8108D},
            new double[]{9.9149D, 49.8040D},
            new double[]{9.9281D, 49.7975D}
    );

    private final Map<String, AtomicInteger> trackingRouteProgress = new ConcurrentHashMap<>();

    @GetMapping("/{externalOrderId}/driver-location")
    public DriverLocationResponseDto getDriverLocation(
            @PathVariable String externalOrderId
    ) {
        AtomicInteger routeIndex = trackingRouteProgress.computeIfAbsent(
                externalOrderId,
                ignored -> new AtomicInteger(0)
        );
        int currentIndex = routeIndex.getAndUpdate(index -> (index + 1) % DRIVER_ROUTE.size());
        double[] routePoint = DRIVER_ROUTE.get(currentIndex);

        return new DriverLocationResponseDto(
                externalOrderId,
                routePoint[0],
                routePoint[1],
                Instant.now()
        );
    }
}
