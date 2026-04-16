package com.fintech.transactionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class DailyLimitExceededException extends RuntimeException {

    public DailyLimitExceededException(String message) {
        super(message);
    }

    public DailyLimitExceededException(Double alreadySpent, Double limit, Double requested) {
        super(String.format(
                "Daily transaction limit exceeded. Limit: %.2f, Already transferred today: %.2f, Requested: %.2f",
                limit, alreadySpent, requested));
    }
}
