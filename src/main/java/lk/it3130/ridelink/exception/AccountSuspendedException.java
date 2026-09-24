package lk.it3130.ridelink.exception;

public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException(String message) {
        super(message);
    }
}
