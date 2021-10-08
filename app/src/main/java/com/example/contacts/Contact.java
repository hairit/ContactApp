package com.example.contacts;

public class Contact {
    private String name;
    private String phone;
    private boolean isSelected;

    public Contact(String name,String phone){
        this.name=name;
        this.phone=phone;
        this.isSelected=false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
