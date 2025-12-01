package com.example.atlasevents;

//import static org.mockito.Mockito.mock; //I commented this out because it was blocking me from running Entrant tests

import com.example.atlasevents.data.NotificationRepository;

import org.junit.Assert;
import org.junit.Test;
//import org.mockito.Mockito;

public class OrganizerTest {
    private Organizer organizer;
    private NotificationRepository mockRepository;
    private Event mockEvent;
    private Entrant mockEntrant;

    @Test
    public void createEventTest(){
        Event event = new Event();
        Organizer organizer = new Organizer();
        organizer.createEvent(event);
        Assert.assertEquals(organizer,event.getOrganizer()); //check to see if organizer became event's organizer
    }
    @Test
    public void sendSingleNotificationTest(){


    }

    @Test
    public void sendBatchNotificationTest(){

    }

}
