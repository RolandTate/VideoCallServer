package com.example.videocall;

import java.io.Serializable;
import java.util.List;

public class UserList implements Serializable {
    List<User> users = null;

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
