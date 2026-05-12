package heizoel.backend.customer.domain.entity;

import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.customer.domain.CustomerResponseType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "customer_response")
@Getter
@Setter
@NoArgsConstructor
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
}