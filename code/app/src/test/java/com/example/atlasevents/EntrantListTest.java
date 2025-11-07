package com.example.atlasevents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EntrantListTest {

    static class Entrant {
        private final String email;
        Entrant(String email){ this.email = email; }
        public String getEmail(){ return email; }
    }

    static class EntrantList {
        private ArrayList<Entrant> list = new ArrayList<>();
        public void addEntrant(Entrant e){ list.add(e); }
        public Entrant getEntrant(int i){ return list.get(i); }
        public int size(){ return list.size(); }
    }

//    @Test
//    public void addAndContainsTest(){
//        EntrantList entrantList = new EntrantList();
//        Entrant entrant = new Entrant();
//        entrantList.addEntrant(entrant);
//        assertTrue(entrantList.containsEntrant(entrant));
//    }

    // the methods to test notification system from entrant perspective
    private List<String> extractEmailsFromEntrantList(EntrantList list) {
        List<String> out = new ArrayList<>();
        if (list == null || list.size() == 0) return out;
        for (int i = 0; i < list.size(); i++) {
            Entrant e = list.getEntrant(i);
            if (e != null && e.getEmail() != null) {
                out.add(e.getEmail());
            }
        }
        return out;
    }

    @Test
    public void extractEmails_emptyList_returnsEmpty() {
        EntrantList list = new EntrantList();
        List<String> result = extractEmailsFromEntrantList(list);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void extractEmails_nullEntrant_skipsNulls() {
        EntrantList list = new EntrantList();
        list.addEntrant(null);
        list.addEntrant(new Entrant(null)); // entrant with null email
        list.addEntrant(new Entrant("alice@example.com"));
        List<String> result = extractEmailsFromEntrantList(list);
        assertEquals(1, result.size());
        assertEquals("alice@example.com", result.get(0));
    }

    @Test
    public void extractEmails_multiple_returnsAllInOrder() {
        EntrantList list = new EntrantList();
        list.addEntrant(new Entrant("a@x.com"));
        list.addEntrant(new Entrant("b@x.com"));
        list.addEntrant(new Entrant("c@x.com"));
        List<String> result = extractEmailsFromEntrantList(list);
        assertEquals(3, result.size());
        assertEquals("a@x.com", result.get(0));
        assertEquals("b@x.com", result.get(1));
        assertEquals("c@x.com", result.get(2));
    }
    //methods to test notification system end
}
