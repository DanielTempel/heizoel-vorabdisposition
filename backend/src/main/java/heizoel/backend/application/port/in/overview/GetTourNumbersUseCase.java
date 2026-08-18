package heizoel.backend.application.port.in.overview;

import java.util.List;

public interface GetTourNumbersUseCase {

    List<String> getTourNumbers(
            GetTourNumbersQuery query
    );

}
