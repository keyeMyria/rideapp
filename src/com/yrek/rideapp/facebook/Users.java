package com.yrek.rideapp.facebook;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Users implements Serializable {
    private static final long serialVersionUID = 0L;

    private User[] data;

    public User[] getData() {
        return data;
    }

    public void setData(User[] data) {
        this.data = data;
    }
}
