package com.owner.qrscan.services;

import com.owner.qrscan.models.Qr;

public interface QrService {
    void createAddress();

    Qr getAddress();

    Qr updateAddress();

    void deleteAddress();
}
