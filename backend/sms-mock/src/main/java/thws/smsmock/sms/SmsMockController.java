package thws.smsmock.sms;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/sms/messages")
public class SmsMockController {

    private final List<ReceivedSmsMessage> receivedMessages = new CopyOnWriteArrayList<>();

    @PostMapping
    public ResponseEntity<Void> receiveSms(
            @Valid @RequestBody SmsSendRequestDto request
    ) {
        ReceivedSmsMessage message = new ReceivedSmsMessage(
                Instant.now(),
                request.to(),
                request.text()
        );

        receivedMessages.add(message);

        log.info(
                "SMS mock received message: to={}, text={}",
                request.to(),
                request.text()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ReceivedSmsMessage> getReceivedMessages() {
        return receivedMessages;
    }

    @DeleteMapping
    public ResponseEntity<Void> clearReceivedMessages() {
        receivedMessages.clear();
        return ResponseEntity.noContent().build();
    }
}
