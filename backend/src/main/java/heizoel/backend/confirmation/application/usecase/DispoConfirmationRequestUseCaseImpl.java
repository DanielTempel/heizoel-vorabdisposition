package heizoel.backend.confirmation.application.usecase;


import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestCommand;
import heizoel.backend.confirmation.application.port.in.confirmation.CreateConfirmationRequestResult;
import heizoel.backend.confirmation.application.port.in.confirmation.DispoConfirmationRequestUseCase;
import heizoel.backend.confirmation.application.port.out.notification.NotificationService;
import heizoel.backend.confirmation.application.port.out.persistence.CompanyRepositoryPort;
import heizoel.backend.confirmation.application.port.out.workflow.NoResponseWorkflowService;
import heizoel.backend.confirmation.application.model.ConfirmationRequestCreationResult;
import heizoel.backend.confirmation.application.service.ConfirmationRequestPreparationService;
import heizoel.backend.domain.model.Company;
import heizoel.backend.domain.model.enumeration.CommunicationChannel;
import heizoel.backend.domain.exception.CompanyNotFoundException;
import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.domain.exception.MissingDigitalContactException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispoConfirmationRequestUseCaseImpl implements DispoConfirmationRequestUseCase {

    private final CompanyRepositoryPort companyRepository;
    private final ConfirmationRequestPreparationService confirmationRequestPreparationService;
    private final NotificationService notificationService;
    private final NoResponseWorkflowService noResponseWorkflowService;

    @Override
    @Transactional
    public CreateConfirmationRequestResult createConfirmationRequest(CreateConfirmationRequestCommand command) {

        validateDeliveryWindow(command);
        validateCommunicationChannel(command);

        Company company = companyRepository.findById(command.companyContext().companyId())
                .orElseThrow(() -> new CompanyNotFoundException("Company was not found."));

        ConfirmationRequestCreationResult creationResult =
                confirmationRequestPreparationService.prepareConfirmationRequest(
                        company,
                        command
                );

        if (creationResult.created()) {
            notificationService.sendConfirmationRequest(
                    creationResult.orderSnapshot(),
                    creationResult.confirmationRequest()
            );
            noResponseWorkflowService.startTimeoutProcess(
                    creationResult.confirmationRequest().getId(),
                    creationResult.confirmationRequest().getExpiresAt()
            );
        }

        return new CreateConfirmationRequestResult(
                creationResult.orderSnapshot().getExternalOrderId(),
                creationResult.orderSnapshot().getConfirmationStatus(),
                creationResult.created()
        );
    }

    private void validateDeliveryWindow(CreateConfirmationRequestCommand command) {
        if (!command.deliveryWindowStart().isBefore(command.deliveryWindowEnd())) {
            throw new InvalidDeliveryWindowException(
                    "Delivery window start must be before delivery window end."
            );
        }
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

