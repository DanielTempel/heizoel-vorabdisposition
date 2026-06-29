package heizoel.backend.location.application.support;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class RemoteCallExecutor {

    public <T> Optional<T> execute(Supplier<T> remoteCall) {
        return execute(remoteCall, exception -> {
        });
    }

    public <T> Optional<T> execute(
            Supplier<T> remoteCall,
            Consumer<RestClientException> onFailure
    ) {
        try {
            return Optional.ofNullable(remoteCall.get());
        } catch (RestClientException exception) {
            onFailure.accept(exception);
            return Optional.empty();
        }
    }
}
