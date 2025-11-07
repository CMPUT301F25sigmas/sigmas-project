package com.example.atlasevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EventTest {
    @Test
    public void getSetEventNameTest(){
        Event event = new Event();
        String eventName = "Summer Music Festival";
        event.setEventName(eventName);
        assertEquals(eventName, event.getEventName());
    }

    @Test
    public void isRegistrationOpenTest(){
        Event event = new Event();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_MONTH, -1);
        event.setRegStartDate(formatter.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, 2);
        event.setRegEndDate(formatter.format(cal.getTime()));

        assertTrue(event.isRegistrationOpen());

        Event event2 = new Event();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        event2.setRegStartDate(formatter.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, 2);
        event2.setRegEndDate(formatter.format(cal.getTime()));

        assertFalse(event2.isRegistrationOpen());

        Event event3 = new Event();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -3);
        event3.setRegStartDate(formatter.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, 1);
        event3.setRegEndDate(formatter.format(cal.getTime()));

        assertFalse(event3.isRegistrationOpen());
    }

    @Test
    public void addToWaitlistTest(){
        Entrant entrant1 = new Entrant("name","email","phone","password");
        Event event = new Event();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_MONTH, -1);
        event.setRegStartDate(formatter.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, 2);
        event.setRegEndDate(formatter.format(cal.getTime()));

        event.addToWaitlist(entrant1);
        EntrantList entrantList = event.getWaitlist();
        assertTrue(entrantList.containsEntrant(entrant1));
    }
    @Test
    public void removeFromWaitlistTest(){
        Entrant entrant1 = new Entrant("name","email","phone","password");
        Event event = new Event();
        event.addToWaitlist(entrant1);
        event.removeFromWaitlist(entrant1);
        EntrantList entrantList = event.getWaitlist();
        assertFalse(entrantList.containsEntrant(entrant1));
    }
//    @Test
//    public void lotteryTest(){
//        Entrant entrant1 = new Entrant();
//        Entrant entrant2 = new Entrant();
//        Entrant entrant3 = new Entrant();
//        Entrant entrant4 = new Entrant();
//        Entrant entrant5 = new Entrant();
//        Entrant entrant6 = new Entrant();
//        Entrant entrant7 = new Entrant();
//        Entrant entrant8 = new Entrant();
//
//        Event event = new Event();
//
//        event.addToWaitlist(entrant1);
//        event.addToWaitlist(entrant2);
//        event.addToWaitlist(entrant3);
//        event.addToWaitlist(entrant4);
//        event.addToWaitlist(entrant5);
//        event.addToWaitlist(entrant6);
//        event.addToWaitlist(entrant7);
//        event.addToWaitlist(entrant8);
//        event.setSlots(5);
//        event.runLottery();
//
//        EntrantList waitList = event.getWaitlist();
//        EntrantList inviteList = event.getInviteList();
//        Assert.assertEquals(5,inviteList.size()); //check if 5 people were moved to invite list
//        Assert.assertEquals(3,waitList.size()); //check that only 3 are left in waitlist
//    }
}
