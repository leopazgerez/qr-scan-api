package com.owner.qrscan.controllers;

import com.owner.qrscan.socket.SocketConnectionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SocketController {
    @Autowired
    private SocketConnectionHandler socketHandler;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("connectedClients", socketHandler.getConnectedClientsCount());
        return "index";
    }

    @GetMapping("/status")
    @ResponseBody
    public String getStatus() {
        return String.format("Servidor WebSocket funcionando. Clientes conectados: %d",
                socketHandler.getConnectedClientsCount());
    }
}
