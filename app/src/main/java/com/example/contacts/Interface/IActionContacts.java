package com.example.contacts.Interface;

import android.content.Context;

import com.example.contacts.Contact;

import java.util.ArrayList;

public interface IActionContacts {
    default ArrayList<Contact> getContacts(){
        return new ArrayList<Contact>();
    }
    default ArrayList<Contact> getContactsFromCSV(){
        return new ArrayList<Contact>();
    }
    default void saveContactsToCSV(){}
    default void deleteContact(Context c ,Contact contact){}
    default void addContactToRemoveList(Contact contact){}
    default void deleteContactFromRemoveList(Contact contact){}
}
