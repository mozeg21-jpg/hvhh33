package com.news.kimo.utils;

import java.util.regex.Pattern;

/**
 * Utility class for input validation.
 * Provides methods for validating emails, passwords, names, phones,
 * URLs, and other user inputs.
 */
public class ValidationUtils {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_BIO_LENGTH = 160;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{7,15}$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[\\p{L}\\s\\-.']{2,50}$"
    );

    private ValidationUtils() {
        // Prevent instantiation
    }

    /**
     * Check if the given email is valid.
     *
     * @param email The email string to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Check if the given password meets the minimum requirements.
     * Minimum 6 characters.
     *
     * @param password The password string to validate
     * @return true if the password is valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * Check if the given name is valid.
     * Must contain only letters, spaces, hyphens, dots, and apostrophes.
     * Length must be between 2 and 50 characters.
     *
     * @param name The name string to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Check if the given phone number is valid.
     * Accepts international format with optional '+' prefix.
     *
     * @param phone The phone number string to validate
     * @return true if the phone number is valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Check if the given URL is valid.
     * Accepts http, https, and ftp URLs.
     *
     * @param url The URL string to validate
     * @return true if the URL is valid, false otherwise
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * Check if the given string is not null and not empty/blank.
     *
     * @param text The string to check
     * @return true if the string is not null and not empty/blank
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Check if the given bio text is valid.
     * Maximum 160 characters.
     *
     * @param bio The bio string to validate
     * @return true if the bio is valid (null is valid), false if exceeds max length
     */
    public static boolean isValidBio(String bio) {
        if (bio == null) {
            return true; // Bio is optional
        }
        return bio.length() <= MAX_BIO_LENGTH;
    }
}
