package heizoel.backend.application.service.confirmation;


import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.application.port.in.confirmation.CreateConfirmationRequestUseCase;
import heizoel.backend.application.port.out.notification.NotificationService;
import heizoel.backend.application.port.out.workflow.NoResponseWorkflowService;
import heizoel.backend.application.port.in.confirmation.ConfirmationRequestCreationResult;
import heizoel.backend.domain.Company;
import heizoel.backend.domain.CommunicationChannel;
import heizoel.backend.application.exception.CompanyNotFoundException;
import heizoel.backend.domain.exception.MissingDigitalContactException;
import heizoel.backend.adapter.out.persistence.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateConfirmationRequestService implements CreateConfirmationRequestUseCase {

    private final CompanyRepository companyRepository;
    private final ConfirmationRequestPreparationService confirmationRequestPreparationService;
    private final NotificationService notificationService;
    private final NoResponseWorkflowService noResponseWorkflowService;

    @Override
    @Transactional
    public CreateConfirmationRequestResult createConfirmationRequest(CreateConfirmationRequestCommand command) {

        validateCommunicationChannel(command);

        Company company = companyRepository
                .findById(command.companyContext().companyId())
                .orElseThrow(() -> new CompanyNotFoundException("Company was not found."));

        ConfirmationRequestCreationResult creationResult =
                confirmationRequestPreparationService.prepareConfirmationRequest(
                        company,
                        command
                );

        if (creationResult.created()) {
            notificationService.sendConfirmationRequest(
                    creationResult.order(),
                    creationResult.confirmationRequest()
            );
            noResponseWorkflowService.startTimeoutProcess(
                    creationResult.confirmationRequest().getId(),
                    creationResult.confirmationRequest().getExpiresAt()
            );
        }

        return new CreateConfirmationRequestResult(
                creationResult.order().getExternalOrderId(),
                creationResult.order().getConfirmationStatus(),
                creationResult.created()
        );
    }

    private void validateCommunicationChannel(CreateConfirmationRequestCommand command) {
        if (command.communicationChannel() == CommunicationChannel.EMAIL
                && isBlank(command.customerEmail())) {
            throw new MissingDigitalContactException(
                    "Customer e-mail is required when communication channel is EMAIL."
            );
        }

        if ((command.communicationChannel() == CommunicationChannel.SMS
                || command.communicationChannel() == CommunicationChannel.WHATSAPP)
                && isBlank(command.customerPhoneNumber())) {
            throw new MissingDigitalContactException(
                    "Customer phone number is required when communication channel is "
                            + command.communicationChannel() + "."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

