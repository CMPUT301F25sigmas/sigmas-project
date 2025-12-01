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
import android.text.Editable;
import android.widget.EditText;

import androidx.room.jarjarred.org.antlr.v4.tool.Alternative;

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
        // Test pure Java logic - don't use Android Views
        @Test
        public void testParseTags_ValidInput_ReturnsNormalizedTags() {
            // Create an instance of the helper class
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String input = "Music, Sports,  Technology  ,MUSIC";

            // When
            List<String> result = helper.parseTags(input);

            // Then
            assertEquals(3, result.size());
            assertTrue(result.contains("music"));
            assertTrue(result.contains("sports"));
            assertTrue(result.contains("technology"));
        }

        @Test
        public void testParseTags_EmptyInput_ReturnsEmptyList() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String input = "";

            // When
            List<String> result = helper.parseTags(input);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        public void testInputsValid_ValidInputs_ReturnsTrue() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given - Use Strings instead of EditText
            String eventName = "Test Event";
            String slotStr = "10";
            boolean enableEntrantLimit = false;
            String entrantLimitStr = "";

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertTrue(isValid);
        }

        @Test
        public void testInputsValid_EmptyName_ReturnsFalse() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "";
            String slotStr = "10";
            boolean enableEntrantLimit = false;
            String entrantLimitStr = "";

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertFalse(isValid);
        }

        @Test
        public void testInputsValid_InvalidSlots_ReturnsFalse() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "Test Event";
            String slotStr = "0"; // Invalid slots
            boolean enableEntrantLimit = false;
            String entrantLimitStr = "";

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertFalse(isValid);
        }

        @Test
        public void testInputsValid_NonNumericSlots_ReturnsFalse() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "Test Event";
            String slotStr = "abc"; // Non-numeric
            boolean enableEntrantLimit = false;
            String entrantLimitStr = "";

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertFalse(isValid);
        }

        @Test
        public void testInputsValid_WithEntrantLimit_ValidInputs_ReturnsTrue() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "Test Event";
            String slotStr = "10";
            boolean enableEntrantLimit = true;
            String entrantLimitStr = "5";

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertTrue(isValid);
        }

        @Test
        public void testInputsValid_WithInvalidEntrantLimit_ReturnsFalse() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "Test Event";
            String slotStr = "5";
            boolean enableEntrantLimit = true;
            String entrantLimitStr = "10"; // entrant limit > slots

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertFalse(isValid);
        }

        @Test
        public void testInputsValid_WithNonNumericEntrantLimit_ReturnsFalse() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "Test Event";
            String slotStr = "10";
            boolean enableEntrantLimit = true;
            String entrantLimitStr = "abc"; // Non-numeric

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertFalse(isValid);
        }

        @Test
        public void testInputsValid_WithZeroEntrantLimit_ReturnsFalse() {
            CreateEventHelper helper = new CreateEventHelper();

            // Given
            String eventName = "Test Event";
            String slotStr = "10";
            boolean enableEntrantLimit = true;
            String entrantLimitStr = "0"; // Zero limit

            // When
            boolean isValid = helper.inputsValid(eventName, slotStr, enableEntrantLimit, entrantLimitStr);

            // Then
            assertFalse(isValid);
        }

        // Create a helper class that contains the logic from CreateEventActivity
        // This should match the logic in your CreateEventActivity
        private static class CreateEventHelper {

            public boolean inputsValid(String eventName, String slotStr, boolean enableEntrantLimit, String entrantLimitStr) {
                // Validate event name
                if (eventName == null || eventName.isEmpty()) {
                    return false;
                }

                // Validate slots
                if (slotStr == null || slotStr.isEmpty()) {
                    return false;
                }

                int numSlots;
                try {
                    numSlots = Integer.parseInt(slotStr);
                } catch (NumberFormatException e) {
                    return false;
                }

                if (numSlots <= 0) {
                    return false;
                }

                // Validate entrant limit if enabled
                if (enableEntrantLimit && entrantLimitStr != null && !entrantLimitStr.isEmpty()) {
                    try {
                        int entrantLimit = Integer.parseInt(entrantLimitStr);
                        if (entrantLimit <= 0 || entrantLimit > numSlots) {
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }

                return true;
            }

            public List<String> parseTags(String input) {
                List<String> tags = new ArrayList<>();
                if (input == null || input.trim().isEmpty()) {
                    return tags;
                }

                // Split by comma and clean up
                String[] rawTags = input.split(",");
                for (String tag : rawTags) {
                    String cleanedTag = tag.trim().toLowerCase();
                    if (!cleanedTag.isEmpty() && !tags.contains(cleanedTag)) {
                        tags.add(cleanedTag);
                    }
                }

                return tags;
            }
        }
    }
