package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /** Chatbot encadrant : besoin en langage naturel -> candidats classés par adéquation. */
    @PostMapping("/encadrant")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<Map<String, Object>> encadrant(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                chatbotService.matchCandidats(userDetails.getUsername(), body.get("message")));
    }

    /** Assistant RH : question en langage naturel -> réponse/rapport basé sur les indicateurs. */
    @PostMapping("/rh")
    @PreAuthorize("hasAnyRole('ADMIN_RH','RESPONSABLE_RH')")
    public ResponseEntity<Map<String, Object>> rh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(chatbotService.assistantRh(body.get("message")));
    }
}
