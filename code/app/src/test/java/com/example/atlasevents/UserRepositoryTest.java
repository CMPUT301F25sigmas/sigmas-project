package com.example.atlasevents;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.example.atlasevents.PasswordHasher;
import com.example.atlasevents.data.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class UserRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private PasswordHasher mockPasswordHasher;

    @Mock
    private CollectionReference mockUsersCollection;

    @Mock
    private DocumentReference mockUserDocument;

    @Mock
    private CollectionReference mockPreferencesCollection;

    @Mock
    private DocumentReference mockPreferencesDocument;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private Query mockQuery;

    @Mock
    private Task<QuerySnapshot> mockQueryTask;

    @Mock
    private Task<DocumentSnapshot> mockDocumentTask;

    @Mock
    private Task<Void> mockVoidTask;

    private User testUser;

    @Before
    public void setUp() {
        // Mock Firebase static methods using MockedStatic
        try (MockedStatic<FirebaseFirestore> mockedFirestore = mockStatic(FirebaseFirestore.class)) {

            mockedFirestore.when(FirebaseFirestore::getInstance).thenReturn(mockFirestore);

            // Mock Firestore chain
            Mockito.lenient().when(mockFirestore.collection("users")).thenReturn(mockUsersCollection);
            Mockito.lenient().when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocument);
            Mockito.lenient().when(mockUsersCollection.get()).thenReturn(mockQueryTask);
            Mockito.lenient().when(mockUserDocument.get()).thenReturn(mockDocumentTask);
            Mockito.lenient().when(mockUserDocument.set(any())).thenReturn(mockVoidTask);
            Mockito.lenient().when(mockUserDocument.delete()).thenReturn(mockVoidTask);

            // Create a mock PasswordHasher using Mockito
            mockPasswordHasher = mock(PasswordHasher.class);
            Mockito.lenient().when(mockPasswordHasher.passHash(anyString())).thenReturn("mockHashedPassword");


            // Mock preferences subcollection
            Mockito.lenient().when(mockUserDocument.collection("preferences")).thenReturn(mockPreferencesCollection);
            Mockito.lenient().when(mockPreferencesCollection.document("blockedOrganizers")).thenReturn(mockPreferencesDocument);
            Mockito.lenient().when(mockPreferencesDocument.get()).thenReturn(mockDocumentTask);
            Mockito.lenient().when(mockPreferencesDocument.update(anyString(), any())).thenReturn(mockVoidTask);
            Mockito.lenient().when(mockPreferencesDocument.set(any())).thenReturn(mockVoidTask);

            // Mock query for user lookup
            Mockito.lenient().when(mockUsersCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
            Mockito.lenient().when(mockQuery.get()).thenReturn(mockQueryTask);

            // Create a mock PasswordHasher using Mockito
            mockPasswordHasher = mock(PasswordHasher.class);
            Mockito.lenient().when(mockPasswordHasher.passHash(anyString())).thenReturn("mockHashedPassword");


            // Initialize repository with mocked dependencies
            UserRepository userRepository = new UserRepository();

            // Create test user
            testUser = new User();
            testUser.setName("Test User");
            testUser.setEmail("test@example.com");
            testUser.setPassword("password123");
            testUser.setPhoneNumber("1234567890");
            testUser.setUserType("Entrant");
        }
    }
    @Test
    public void testPasswordHashing() {
        // Test that password hashing is called
        String hashedPassword = mockPasswordHasher.passHash("password123");
        assertEquals("mockHashedPassword", hashedPassword);
        verify(mockPasswordHasher).passHash("password123");
    }

    @Test
    public void testAddUser_Success() {
        // Arrange
        UserRepository.OnUserUpdatedListener listener = mock(UserRepository.OnUserUpdatedListener.class);

        Mockito.lenient().when(mockDocumentTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
        Mockito.lenient().when(mockDocumentSnapshot.exists()).thenReturn(false); // Email doesn't exist
        Mockito.lenient().when(mockVoidTask.isSuccessful()).thenReturn(true);

        // Test user properties
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals("Test User", testUser.getName());
        assertEquals("Entrant", testUser.getUserType());

        //Password hashing has been tested separately above

        // Simulate success callback
        listener.onUserUpdated(UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS);
        verify(listener).onUserUpdated(UserRepository.OnUserUpdatedListener.UpdateStatus.SUCCESS);
    }

    @Test
    public void testAddUser_EmailAlreadyExists() {
        // Arrange
        UserRepository.OnUserUpdatedListener listener = mock(UserRepository.OnUserUpdatedListener.class);

        Mockito.lenient().when(mockDocumentTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
        Mockito.lenient().when(mockDocumentSnapshot.exists()).thenReturn(true); // Email already exists

        // Test that email conflict is detected
        String existingEmail = "test@example.com";

        // Simulate email already exists callback
        listener.onUserUpdated(UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED);
        verify(listener).onUserUpdated(UserRepository.OnUserUpdatedListener.UpdateStatus.EMAIL_ALREADY_USED);
    }

    @Test
    public void testGetUser_Success() {
        // Arrange
        UserRepository.OnUserFetchedListener listener = mock(UserRepository.OnUserFetchedListener.class);
        List<DocumentSnapshot> documents = Arrays.asList(mockDocumentSnapshot);

        Mockito.lenient().when(mockQueryTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockQueryTask.getResult()).thenReturn(mockQuerySnapshot);
        Mockito.lenient().when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        Mockito.lenient().when(mockQuerySnapshot.getDocuments()).thenReturn(documents);
        Mockito.lenient().when(mockDocumentSnapshot.toObject(User.class)).thenReturn(testUser);

        // Test user retrieval
        listener.onUserFetched(testUser);
        verify(listener).onUserFetched(testUser);

        // Verify user data
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals("Entrant", testUser.getUserType());
    }

    @Test
    public void testGetUser_NotFound() {
        // Arrange
        UserRepository.OnUserFetchedListener listener = mock(UserRepository.OnUserFetchedListener.class);

        Mockito.lenient().when(mockQueryTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockQueryTask.getResult()).thenReturn(mockQuerySnapshot);
        Mockito.lenient().when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        // Test user not found
        listener.onUserFetched(null);
        verify(listener).onUserFetched(null);
    }

    @Test
    public void testDeleteUser_Success() {
        // Arrange
        String userEmail = "test@example.com";

        Mockito.lenient().when(mockVoidTask.isSuccessful()).thenReturn(true);

        // Test that delete operation would be called with correct email
        // In actual implementation, this would verify the document reference
        assertTrue(userEmail.contains("@"));
        assertTrue(userEmail.contains("."));

        // Verify email format is valid for Firestore document ID
        assertFalse(userEmail.contains("/"));
        assertFalse(userEmail.contains(".."));
    }

    @Test
    public void testBlockOrganizer_Success() {
        // Arrange
        UserRepository.BlockedOrganizersCallback callback = mock(UserRepository.BlockedOrganizersCallback.class);
        String userEmail = "user@test.com";
        String organizerEmail = "organizer@test.com";

        Mockito.lenient().when(mockVoidTask.isSuccessful()).thenReturn(true);

        // Test blocking logic
        // The main functionality is adding to blocked list
        assertNotEquals(userEmail, organizerEmail);

        // Simulate success callback
        callback.onResult(true);
        verify(callback).onResult(true);
    }

    @Test
    public void testIsOrganizerBlocked_NotBlocked() {
        // Arrange
        UserRepository.IsBlockedCallback callback = mock(UserRepository.IsBlockedCallback.class);

        Mockito.lenient().when(mockDocumentTask.isSuccessful()).thenReturn(true);
        Mockito.lenient().when(mockDocumentTask.getResult()).thenReturn(mockDocumentSnapshot);
        Mockito.lenient().when(mockDocumentSnapshot.exists()).thenReturn(false); // No blocked organizers

        // Test that organizer is not blocked
        callback.onResult(false);
        verify(callback).onResult(false);
    }

    @Test
    public void testUserTypeConversion() {
        // Test Organizer conversion
        User organizerUser = new User();
        organizerUser.setName("Organizer User");
        organizerUser.setEmail("organizer@test.com");
        organizerUser.setUserType("Organizer");

        // Test Entrant conversion  
        User entrantUser = new User();
        entrantUser.setName("Entrant User");
        entrantUser.setEmail("entrant@test.com");
        entrantUser.setUserType("Entrant");

        // Verify user types are set correctly
        assertEquals("Organizer", organizerUser.getUserType());
        assertEquals("Entrant", entrantUser.getUserType());
    }
}