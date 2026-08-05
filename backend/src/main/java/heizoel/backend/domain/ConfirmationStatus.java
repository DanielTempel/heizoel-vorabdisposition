package heizoel.backend.domain;

public enum ConfirmationStatus {
    SENT,
    CONFIRMED,
    REJECTED,
    NO_RESPONSE;

    public static ConfirmationStatus fromRequest(
            boolean active,
            CustomerResponseType responseType
    ) {
        if (responseType == null) {
            return active
                    ? SENT
                    : NO_RESPONSE;
        }

        return switch (responseType) {
            case CONFIRM -> CONFIRMED;
            case REJECT -> REJECTED;
        };
    }
}
