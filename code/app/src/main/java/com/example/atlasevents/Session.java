package com.example.atlasevents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Handles user session management within the Atlas Events application.
 * <p>
 * This class provides methods to store, retrieve, and remove user login
 * information using {@link SharedPreferences}. It maintains the logged-in user's
 * email address to persist session data between app launches.
 * </p>
 * <p>
 * It also offers functionality to log out the user and redirect them to the
 * {@link SignInActivity}, ensuring proper session cleanup.
 * </p>
 *
 * @see SharedPreferences
 * @see SignInActivity
 */
public class Session {

    /**
     * Name of the shared preferences file used for storing session information.
     */
    private static final String prefs = "AtlasPrefs";

    /**
     * Key used to store the logged-in user's email address.
     */
    private static final String emailKey = "loggedInEmail";

    /**
     * Reference to the shared preferences used to persist session data.
     */
    private final SharedPreferences sharedPreferences;

    /**
     * Editor used to modify the shared preferences data.
     */
    private final SharedPreferences.Editor sharedPreferencesEditor;

    /**
     * Creates a new {@code Session} instance using the provided application context.
     * <p>
     * Initializes the shared preferences and its editor to enable persistent storage
     * of user session data.
     * </p>
     *
     * @param context the context used to access shared preferences
     */
    public Session(Context context) {
        sharedPreferences = context.getSharedPreferences(prefs, Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    /**
     * Stores the user's email in shared preferences.
     * <p>
     * This method is typically called upon successful authentication to mark
     * the user as logged in.
     * </p>
     *
     * @param email the email address of the logged-in user
     */
    public void setUserEmail(String email) {
        sharedPreferencesEditor.putString(Session.emailKey, email);
        sharedPreferencesEditor.apply();
    }

    /**
     * Retrieves the currently logged-in user's email.
     *
     * @return the email address of the logged-in user, or {@code null} if no user is logged in
     */
    public String getUserEmail() {
        return sharedPreferences.getString(emailKey, null);
    }

    /**
     * Checks if a user is currently logged in.
     * <p>
     * Determines login status based on the presence of a stored email in
     * shared preferences.
     * </p>
     *
     * @return {@code true} if a user is logged in; {@code false} otherwise
     */
    public boolean isLoggedIn() {
        return getUserEmail() != null;
    }

    /**
     * Logs out the current user.
     * <p>
     * Removes the stored email from shared preferences to clear session data,
     * effectively ending the user's session.
     * </p>
     */
    public void logout() {
        sharedPreferencesEditor.remove(emailKey);
        sharedPreferencesEditor.apply();
    }

    /**
     * Logs out the current user and redirects them to the {@link SignInActivity}.
     * <p>
     * This method clears the session, launches the sign-in screen as a new task,
     * and finishes the current activity to prevent the user from navigating back.
     * </p>
     *
     * @param activity the current activity context used for redirection
     */
    public void logoutAndRedirect(Activity activity) {
        logout();
        Intent intent = new Intent(activity, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
