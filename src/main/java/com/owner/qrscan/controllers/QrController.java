package com.owner.qrscan.controllers;

import com.owner.qrscan.services.QrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class QrController {
    @Autowired
    private QrService qrService;

    @GetMapping("/tasks/getQr")
    public ResponseEntity<Object> getQr() {
        return ResponseEntity.ok(null);
    }
}
