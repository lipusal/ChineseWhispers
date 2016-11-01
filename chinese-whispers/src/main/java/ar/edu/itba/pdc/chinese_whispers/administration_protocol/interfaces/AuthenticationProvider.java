package ar.edu.itba.pdc.chinese_whispers.administration_protocol.interfaces;

/**
 * Created by jbellini on 1/11/16.
 * <p>
 * Interface that defines a method to get authentication information.
 */
public interface AuthenticationProvider {

    /**
     * Checks if a user with corresponding username/password exists in the authorization Map.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @return {@code true} if the user exists and the given password is correct, or {@code false} otherwise.
     */
    boolean isValidUser(String username, String password);
}
