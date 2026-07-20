package heizoel.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "customer_response")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmation_request_id", nullable = false, unique = true)
    private ConfirmationRequest confirmationRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false)
    private CustomerResponseType responseType;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public static CustomerResponse create(
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType,
            String comment,
            Instant receivedAt
    ) {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.confirmationRequest = confirmationRequest;
        customerResponse.responseType = responseType;
        customerResponse.comment = comment;
        customerResponse.receivedAt = receivedAt;
        return customerResponse;
    }

}
