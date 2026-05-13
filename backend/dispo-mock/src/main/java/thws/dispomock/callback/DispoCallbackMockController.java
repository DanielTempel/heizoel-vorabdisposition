package thws.dispomock.callback;


import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/dispo/confirmation-status-updates")
public class DispoCallbackMockController {

    private final List<ReceivedDispoCallback> receivedCallbacks = new CopyOnWriteArrayList<>();

    @PostMapping
    public ResponseEntity<Void> receiveStatusUpdate(
            @Valid @RequestBody DispoConfirmationStatusUpdateDto dto
    ) {
        receivedCallbacks.add(new ReceivedDispoCallback(
                Instant.now(),
                dto.externalOrderId(),
                dto.confirmationStatus().name(),
                dto.customerComment()
        ));

        log.info(
                "DISPO mock received callback: externalOrderId={}, status={}, comment={}",
                dto.externalOrderId(),
                dto.confirmationStatus(),
                dto.customerComment()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ReceivedDispoCallback> getReceivedCallbacks() {
        return receivedCallbacks;
    }

    @DeleteMapping
    public ResponseEntity<Void> clearReceivedCallbacks() {
        receivedCallbacks.clear();
        return ResponseEntity.noContent().build();
    }
}