package heizoel.backend.confirmation.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "company",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_name",
                        columnNames = "name"
                ),
                @UniqueConstraint(
                        name = "uk_company_api_key_hash",
                        columnNames = "api_key_hash"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "api_key_hash", nullable = false)
    private String apiKeyHash;

    @Column(name = "callback_url", nullable = false, length = 1000)
    private String callbackUrl;

    public static Company create(
            String name,
            String apiKeyHash,
            String callbackUrl
    ) {
        Company company = new Company();
        company.name = name;
        company.apiKeyHash = apiKeyHash;
        company.callbackUrl = callbackUrl;
        return company;
    }
}


