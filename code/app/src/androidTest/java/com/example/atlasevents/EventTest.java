package com.example.atlasevents;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class EventTest {

    @Test
    public void addToWaitlistTest(){
        Entrant entrant1 = new Entrant("name","email","phone","password");
        Event event = new Event();
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
    @Test
    public void lotteryTest(){
        Entrant entrant1 = new Entrant();
        Entrant entrant2 = new Entrant();
        Entrant entrant3 = new Entrant();
        Entrant entrant4 = new Entrant();
        Entrant entrant5 = new Entrant();
        Entrant entrant6 = new Entrant();
        Entrant entrant7 = new Entrant();
        Entrant entrant8 = new Entrant();

        Event event = new Event();

        event.addToWaitlist(entrant1);
        event.addToWaitlist(entrant2);
        event.addToWaitlist(entrant3);
        event.addToWaitlist(entrant4);
        event.addToWaitlist(entrant5);
        event.addToWaitlist(entrant6);
        event.addToWaitlist(entrant7);
        event.addToWaitlist(entrant8);
        event.setSlots(5);
        event.runLottery();

        EntrantList waitList = event.getWaitlist();
        EntrantList inviteList = event.getInviteList();
        Assert.assertEquals(5,inviteList.size()); //check if 5 people were moved to invite list
        Assert.assertEquals(3,waitList.size()); //check that only 3 are left in waitlist
    }
}
