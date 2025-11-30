package com.example.atlasevents;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.EditText;

import com.example.atlasevents.data.EventRepository;
import com.example.atlasevents.data.UserRepository;
import com.example.atlasevents.utils.ImageUploader;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CreateEventTest {

    @Mock
    private UserRepository mockUserRepo;
    @Mock
    private EventRepository mockEventRepo;
    @Mock
    private ImageUploader mockUploader;
    @Mock
    private Session mockSession;
    @Mock
    private Organizer mockOrganizer;
    @Mock
    private Context mockContext;

    private CreateEventActivity activitySpy;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create a spy of the activity
        activitySpy = spy(new CreateEventActivity());
        
        // Set up mock behavior
        when(mockSession.getUserEmail()).thenReturn("test@example.com");
        doAnswer(invocation -> {
            // Skip the callback interaction since we can't access it directly
            return null;
        }).when(mockUserRepo).getOrganizer(anyString(), any());

        // Set the mocked dependencies
        activitySpy.userRepo = mockUserRepo;
        activitySpy.eventRepo = mockEventRepo;
        activitySpy.uploader = mockUploader;
        activitySpy.session = mockSession;
        
        // Mock context methods if needed
        doReturn(mockContext).when(activitySpy).getApplicationContext();
    }

    @Test
    public void testInputsValid_ValidInputs_ReturnsTrue() {
        // Given
        EditText name = new EditText(mockContext);
        name.setText("Test Event");
        EditText slots = new EditText(mockContext);
        slots.setText("10");
        
        // When
        boolean isValid = activitySpy.inputsValid(name, slots, false, "");

        // Then
        assertTrue(isValid);
    }

    @Test
    public void testInputsValid_EmptyName_ReturnsFalse() {
        // Given
        EditText name = new EditText(mockContext);
        name.setText("");
        EditText slots = new EditText(mockContext);
        slots.setText("10");
        
        // When
        boolean isValid = activitySpy.inputsValid(name, slots, false, "");

        // Then
        assertFalse(isValid);
    }

    @Test
    public void testInputsValid_InvalidSlots_ReturnsFalse() {
        // Given
        EditText name = new EditText(mockContext);
        name.setText("Test Event");
        EditText slots = new EditText(mockContext);
        slots.setText("0"); // Invalid slots
        
        // When
        boolean isValid = activitySpy.inputsValid(name, slots, false, "");

        // Then
        assertFalse(isValid);
    }

    @Test
    public void testInputsValid_WithEntrantLimit_ValidInputs_ReturnsTrue() {
        // Given
        EditText name = new EditText(mockContext);
        name.setText("Test Event");
        EditText slots = new EditText(mockContext);
        slots.setText("10");
        
        // When
        boolean isValid = activitySpy.inputsValid(name, slots, true, "5");

        // Then
        assertTrue(isValid);
    }

    @Test
    public void testInputsValid_WithInvalidEntrantLimit_ReturnsFalse() {
        // Given
        EditText name = new EditText(mockContext);
        name.setText("Test Event");
        EditText slots = new EditText(mockContext);
        slots.setText("5");
        
        // When (entrant limit > slots)
        boolean isValid = activitySpy.inputsValid(name, slots, true, "10");

        // Then
        assertFalse(isValid);
    }

    @Test
    public void testParseTags_ValidInput_ReturnsNormalizedTags() {
        // Given
        String input = "Music, Sports,  Technology  ,MUSIC"; // Note spaces and case variations

        // When
        List<String> result = activitySpy.parseTags(input);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("music"));
        assertTrue(result.contains("sports"));
        assertTrue(result.contains("technology"));
    }

    @Test
    public void testParseTags_EmptyInput_ReturnsEmptyList() {
        // Given
        String input = "";

        // When
        List<String> result = activitySpy.parseTags(input);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFinish_WithNewImageNotSaved_DeletesImage() {
        // Given
        activitySpy.imageURL = "http://example.com/image.jpg";
        activitySpy.eventSaved = false;
        
        doAnswer(invocation -> {
            ImageUploader.DeleteCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(mockUploader).deleteImage(anyString(), any());

        // When
        activitySpy.finish();

        // Then
        verify(mockUploader).deleteImage("http://example.com/image.jpg", any());
    }

    @Test
    public void testFinish_WithEventSaved_DoesNotDeleteImage() {
        // Given
        activitySpy.imageURL = "http://example.com/image.jpg";
        activitySpy.eventSaved = true;

        // When
        activitySpy.finish();

        // Then
        verify(mockUploader, never()).deleteImage(anyString(), any());
    }
}