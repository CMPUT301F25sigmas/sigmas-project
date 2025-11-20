package com.example.atlasevents;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.atlasevents.data.EventRepository;

import java.util.ArrayList;

/**
 * Activity displaying the admin’s dashboard with a list of event images.
 * <p>
 * This activity extends {@link AdminBase} to provide the navigation sidebar and
 * displays only events that include an image. Events with images are presented
 * as image cards that admins can interact with through a popup menu. The
 * activity retrieves all events from Firebase, filters those with image URLs,
 * and dynamically creates the corresponding card views.
 * </p>
 *
 * @see AdminBase
 * @see Event
 * @see EventRepository
 * @see ImageDetailsAdminActivity
 */
public class AdminImagesActivity extends AdminBase {

    /**
     * Container layout that holds all event image cards.
     */
    private LinearLayout imagesContainer;

    /**
     * Repository for fetching event data from Firebase.
     */
    private EventRepository eventRepository;

    /**
     * Scroll view containing the list of event images.
     */
    private ScrollView imagesScrollView;

    /**
     * Layout displayed when no images are available.
     */
    private LinearLayout emptyState;

    /**
     * Utility for uploading and deleting images from Firebase Storage.
     */
    ImageUploader uploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.admin_images);
        setActiveNavItem(R.id.images_icon_card);

        uploader = new ImageUploader();

        imagesContainer = findViewById(R.id.events_container_organizer);
        eventRepository = new EventRepository();

        imagesScrollView = findViewById(R.id.events_scroll_view);
        emptyState = findViewById(R.id.empty_state);

        emptyState.setVisibility(View.GONE);
        imagesScrollView.setVisibility(View.GONE);

        loadEventsFromFirebase();
    }

    /**
     * Fetches all events from Firebase and displays only those containing images.
     * <p>
     * Retrieves all events asynchronously, filters out those without an image URL,
     * and forwards the filtered list to {@link #displayImages(ArrayList)}. If no
     * events with images are found or the fetch fails, {@link #showEmptyState()} is shown.
     * </p>
     */
    private void loadEventsFromFirebase() {
        eventRepository.getAllEvents(new EventRepository.EventsCallback(){
            @Override
            public void onSuccess(ArrayList<Event> events) {
                ArrayList<Event> eventsWithImages = new ArrayList<Event>();
                for (Event event : events) {
                    if (!event.getImageUrl().isEmpty()){
                        eventsWithImages.add(event);
                    }
                }
                if (eventsWithImages.isEmpty()) {
                    showEmptyState();
                } else {
                    displayImages(eventsWithImages);
                }
            }

            @Override
            public void onFailure(Exception e) {
                showEmptyState();
            }
        });
    }

    /**
     * Displays event images as card views in the container.
     * <p>
     * Removes any existing content and inflates a card layout for each event
     * containing an image. Each card displays the event image and includes a
     * menu button that allows admins to view image details or remove the
     * associated image from the event.
     * </p>
     *
     * @param events The list of events containing images
     */
    private void displayImages(ArrayList<Event> events) {
        emptyState.setVisibility(View.GONE);
        imagesScrollView.setVisibility(View.VISIBLE);
        imagesContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View eventCard = inflater.inflate(R.layout.image_card_admin_item, imagesContainer, false);

            ImageView eventImage = eventCard.findViewById(R.id.event_image);
            ImageView menuButton = eventCard.findViewById(R.id.menu_button);

            Glide.with(this).load(event.getImageUrl()).into(eventImage);

            menuButton.setOnClickListener(v -> {
                View dropdownView = inflater.inflate(R.layout.image_dropdown, null);

                PopupWindow popupWindow = new PopupWindow(dropdownView, eventCard.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);

                popupWindow.setOutsideTouchable(true);

                popupWindow.showAsDropDown(eventCard, 0, -eventCard.getHeight()+150);

                dropdownView.findViewById(R.id.action_view_details).setOnClickListener(item -> {
                    openImageDetails(event);
                    popupWindow.dismiss();
                });

                dropdownView.findViewById(R.id.action_remove_image).setOnClickListener(item -> {
                    uploader.deleteImage(event.getImageUrl(), new ImageUploader.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            event.setImageUrl("");
                            eventRepository.updateEvent(event, success -> {
                                if (success) {
                                    loadEventsFromFirebase();
                                } else {
                                    Toast.makeText(AdminImagesActivity.this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        @Override
                        public void onFailure(String error) {
                        }
                    });
                    popupWindow.dismiss();
                });
            });

            imagesContainer.addView(eventCard);
        }
    }

    /**
     * Opens the detailed image screen for the selected event.
     * <p>
     * Launches {@link ImageDetailsAdminActivity} and passes the event ID so the
     * detailed image information can be displayed.
     * </p>
     *
     * @param event The event whose image details are to be opened
     */
    private void openImageDetails(Event event) {
        Intent intent = new Intent(this, ImageDetailsAdminActivity.class);
        intent.putExtra(EventDetailsActivity.EventKey, event.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsFromFirebase();
    }

    /**
     * Shows the empty state layout when no event images are available.
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        imagesScrollView.setVisibility(View.GONE);
    }
}