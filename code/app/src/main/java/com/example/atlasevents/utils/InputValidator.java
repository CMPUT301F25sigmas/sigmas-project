package com.example.atlasevents.utils;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * Utility class for validating user inputs throughout the Atlas Events application.
 * <p>
 * Provides centralized validation logic for all input types including strings,
 * numbers, dates, and combined form validations. All validation methods return
 * a {@link ValidationResult} object containing validation status and error messages.
 * </p>
 *
 * @see ValidationResult
 */
public class InputValidator {

    // Validation rule constants
    // Password constraints
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    // Name constraints
    public static final int MIN_NAME_LENGTH = 1;
    public static final int MAX_NAME_LENGTH = 100;

    // Email constraints
    public static final int MAX_EMAIL_LENGTH = 255;

    // Phone constraints
    public static final int MAX_PHONE_LENGTH = 20;

    // Text field constraints
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int MAX_LOCATION_LENGTH = 200;

    // Numeric constraints
    public static final int MIN_SLOTS = 1;
    public static final int MAX_SLOTS = 10000;
    public static final int MIN_ENTRANT_LIMIT = 1;
    public static final int MAX_ENTRANT_LIMIT = 100000;

    // Email regex pattern
    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    // Phone regex pattern (supports international format with optional +, spaces, dashes, parentheses)
    private static final String PHONE_PATTERN =
            "^[+]?[(]?[0-9]{1,4}[)]?[-\\s]?[0-9]{1,4}[-\\s]?[0-9]{1,9}$";

    // Name pattern (letters, spaces, hyphens, apostrophes)
    private static final String NAME_PATTERN = "^[a-zA-Z\\s'-]+$";

    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
    private static final Pattern phonePattern = Pattern.compile(PHONE_PATTERN);
    private static final Pattern namePattern = Pattern.compile(NAME_PATTERN);

    /**
     * Result of a validation operation.
     * Contains whether the validation passed and an error message if it failed.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        private final int errorCode;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.errorCode = 0;
        }

        public ValidationResult(boolean isValid, String errorMessage, int errorCode) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validates a name field.
     * <p>
     * Checks that the name is not empty, within length limits, and contains
     * only valid characters (letters, spaces, hyphens, apostrophes).
     * </p>
     *
     * @param name The name to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.error("Name is required");
        }

        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH) {
            return ValidationResult.error("Name must be at least " + MIN_NAME_LENGTH + " character(s)");
        }

        if (trimmed.length() > MAX_NAME_LENGTH) {
            return ValidationResult.error("Name must be no more than " + MAX_NAME_LENGTH + " characters");
        }

        if (!namePattern.matcher(trimmed).matches()) {
            return ValidationResult.error("Name can only contain letters, spaces, hyphens, and apostrophes");
        }

        return ValidationResult.success();
    }

    /**
     * Validates an email address.
     * <p>
     * Checks that the email is not empty, within length limits, and matches
     * a valid email format pattern.
     * </p>
     *
     * @param email The email to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("Email is required");
        }

        String trimmed = email.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            return ValidationResult.error("Email must be no more than " + MAX_EMAIL_LENGTH + " characters");
        }

        if (!emailPattern.matcher(trimmed).matches()) {
            return ValidationResult.error("Please enter a valid email address");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a phone number.
     * <p>
     * Checks that the phone number (if provided) is within length limits and
     * matches a valid phone format pattern. Phone numbers are optional in
     * some contexts (e.g., sign-up).
     * </p>
     *
     * @param phone The phone number to validate
     * @param required Whether the phone number is required
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validatePhone(String phone, boolean required) {
        if (phone == null || phone.trim().isEmpty()) {
            if (required) {
                return ValidationResult.error("Phone number is required");
            }
            return ValidationResult.success(); // Optional field
        }

        String trimmed = phone.trim();
        // Remove common formatting characters for validation
        String digitsOnly = trimmed.replaceAll("[^0-9+]", "");

        if (digitsOnly.length() < 10) {
            return ValidationResult.error("Phone number must contain at least 10 digits");
        }

        if (trimmed.length() > MAX_PHONE_LENGTH) {
            return ValidationResult.error("Phone number must be no more than " + MAX_PHONE_LENGTH + " characters");
        }

        if (!phonePattern.matcher(trimmed).matches()) {
            return ValidationResult.error("Please enter a valid phone number");
        }

        return ValidationResult.success();
    }

    /**
     * Validates an email or phone number (for sign-in).
     * <p>
     * Accepts either a valid email address or a valid phone number.
     * </p>
     *
     * @param emailOrPhone The email or phone to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateEmailOrPhone(String emailOrPhone) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            return ValidationResult.error("Email or phone number is required");
        }

        String trimmed = emailOrPhone.trim();
        // Try email first
        ValidationResult emailResult = validateEmail(trimmed);
        if (emailResult.isValid()) {
            return ValidationResult.success();
        }

        // Try phone
        ValidationResult phoneResult = validatePhone(trimmed, true);
        if (phoneResult.isValid()) {
            return ValidationResult.success();
        }

        return ValidationResult.error("Please enter a valid email address or phone number");
    }

    /**
     * Validates a password.
     * <p>
     * Checks that the password meets minimum length requirements and contains
     * at least one letter. Additional strength requirements can be added.
     * </p>
     *
     * @param password The password to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Password is required");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.error("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return ValidationResult.error("Password must be no more than " + MAX_PASSWORD_LENGTH + " characters");
        }

        // Check for at least one letter
        if (!password.matches(".*[a-zA-Z].*")) {
            return ValidationResult.error("Password must contain at least one letter");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a password that may be optional (for profile updates).
     * <p>
     * If the password is empty, returns success (password won't be updated).
     * If provided, validates it meets requirements.
     * </p>
     *
     * @param password The password to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validatePasswordOptional(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.success(); // Optional - empty means don't update password
        }

        return validatePassword(password);
    }

    /**
     * Validates a message/notification text.
     * <p>
     * Checks that the message is not empty and within maximum length limits.
     * </p>
     *
     * @param message The message to validate
     * @param maxLength Maximum allowed length
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateMessage(String message, int maxLength) {
        if (message == null || message.trim().isEmpty()) {
            return ValidationResult.error("Message is required");
        }

        if (message.length() > maxLength) {
            return ValidationResult.error("Message must be no more than " + maxLength + " characters");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a location/address field.
     * <p>
     * Checks that the location is not empty and within maximum length limits.
     * </p>
     *
     * @param location The location to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return ValidationResult.error("Location is required");
        }

        if (location.trim().length() > MAX_LOCATION_LENGTH) {
            return ValidationResult.error("Location must be no more than " + MAX_LOCATION_LENGTH + " characters");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a description field (optional).
     * <p>
     * If provided, checks that the description is within maximum length limits.
     * </p>
     *
     * @param description The description to validate
     * @param maxLength Maximum allowed length
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateDescription(String description, int maxLength) {
        if (description == null) {
            return ValidationResult.success(); // Optional field
        }

        if (description.length() > maxLength) {
            return ValidationResult.error("Description must be no more than " + maxLength + " characters");
        }

        return ValidationResult.success();
    }

    /**
     * Validates that a string represents a positive integer.
     * <p>
     * Safely parses the string and checks that it's a positive integer.
     * </p>
     *
     * @param value The string value to validate
     * @param fieldName The name of the field (for error messages)
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validatePositiveInteger(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(fieldName + " is required");
        }

        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue <= 0) {
                return ValidationResult.error(fieldName + " must be a positive number");
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error(fieldName + " must be a valid number");
        }
    }

    /**
     * Validates that a string represents an integer within a specified range.
     * <p>
     * Safely parses the string and checks that it's within the min-max range.
     * </p>
     *
     * @param value The string value to validate
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @param fieldName The name of the field (for error messages)
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateIntegerRange(String value, int min, int max, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(fieldName + " is required");
        }

        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue < min) {
                return ValidationResult.error(fieldName + " must be at least " + min);
            }
            if (intValue > max) {
                return ValidationResult.error(fieldName + " must be no more than " + max);
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error(fieldName + " must be a valid number");
        }
    }

    /**
     * Validates that a string represents an optional positive integer.
     * <p>
     * If the value is empty, returns success (field is optional).
     * If provided, validates it's a positive integer.
     * </p>
     *
     * @param value The string value to validate
     * @param fieldName The name of the field (for error messages)
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validatePositiveIntegerOptional(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.success(); // Optional field
        }

        return validatePositiveInteger(value, fieldName);
    }

    /**
     * Validates that a date range is valid (start date before end date).
     *
     * @param startDate The start date
     * @param endDate The end date
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateDateRange(Date startDate, Date endDate) {
        if (startDate == null) {
            return ValidationResult.error("Start date is required");
        }

        if (endDate == null) {
            return ValidationResult.error("End date is required");
        }

        if (startDate.after(endDate)) {
            return ValidationResult.error("Start date must be before end date");
        }

        return ValidationResult.success();
    }

    /**
     * Validates that an event date is in the future.
     *
     * @param eventDate The event date to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateEventDate(Date eventDate) {
        if (eventDate == null) {
            return ValidationResult.error("Event date is required");
        }

        Date now = new Date();
        if (eventDate.before(now) || eventDate.equals(now)) {
            return ValidationResult.error("Event date must be in the future");
        }

        return ValidationResult.success();
    }

    /**
     * Validates the registration period relative to the event date.
     * <p>
     * Ensures:
     * - Registration start date is before registration end date
     * - Registration end date is before event date
     * </p>
     *
     * @param regStartDate Registration start date
     * @param regEndDate Registration end date
     * @param eventDate Event date
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateRegistrationPeriod(Date regStartDate, Date regEndDate, Date eventDate) {
        if (regStartDate == null) {
            return ValidationResult.error("Registration start date is required");
        }

        if (regEndDate == null) {
            return ValidationResult.error("Registration end date is required");
        }

        if (eventDate == null) {
            return ValidationResult.error("Event date is required");
        }

        // Check registration start < registration end
        if (regStartDate.after(regEndDate) || regStartDate.equals(regEndDate)) {
            return ValidationResult.error("Registration start date must be before registration end date");
        }

        // Check registration end < event date
        if (regEndDate.after(eventDate) || regEndDate.equals(eventDate)) {
            return ValidationResult.error("Registration end date must be before event date");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a time string format.
     * <p>
     * Checks that the time string matches the expected format (hh:mm a).
     * </p>
     *
     * @param time The time string to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return ValidationResult.error("Time is required");
        }

        // Time format should be "hh:mm a" (e.g., "02:30 PM")
        String timePattern = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]\\s?(AM|PM|am|pm)$";
        if (!time.matches(timePattern)) {
            return ValidationResult.error("Please enter a valid time");
        }

        return ValidationResult.success();
    }

    /**
     * Validates slots field for events.
     *
     * @param slots The slots value as a string
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateSlots(String slots) {
        return validateIntegerRange(slots, MIN_SLOTS, MAX_SLOTS, "Number of slots");
    }

    /**
     * Validates entrant limit field for events (optional).
     *
     * @param entrantLimit The entrant limit value as a string
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateEntrantLimit(String entrantLimit) {
        if (entrantLimit == null || entrantLimit.trim().isEmpty()) {
            return ValidationResult.success(); // Optional field
        }

        return validateIntegerRange(entrantLimit, MIN_ENTRANT_LIMIT, MAX_ENTRANT_LIMIT, "Entrant limit");
    }

    /**
     * Validates event name field.
     *
     * @param eventName The event name to validate
     * @return ValidationResult indicating success or failure with error message
     */
    public static ValidationResult validateEventName(String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            return ValidationResult.error("Event name is required");
        }

        String trimmed = eventName.trim();
        if (trimmed.length() < MIN_NAME_LENGTH) {
            return ValidationResult.error("Event name must be at least " + MIN_NAME_LENGTH + " character(s)");
        }

        if (trimmed.length() > MAX_NAME_LENGTH) {
            return ValidationResult.error("Event name must be no more than " + MAX_NAME_LENGTH + " characters");
        }

        return ValidationResult.success();
    }
}

