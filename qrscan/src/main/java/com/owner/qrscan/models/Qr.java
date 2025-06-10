package com.owner.qrscan.models;

import jakarta.persistence.*;

@Entity
@Table(name = "Qr")
public class Qr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Qr(){};

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
