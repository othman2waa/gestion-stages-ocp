package com.OCP.Gestion_Stages.Controller;

import com.OCP.Gestion_Stages.Service.interfaces.NotificationService;
import com.OCP.Gestion_Stages.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMesNotifications(Authentication auth) {
        List<Notification> notifs = notificationService.getMesNotifications(auth.getName());
        return ResponseEntity.ok(notifs.stream().map(this::toMap).toList());
    }

    @GetMapping("/non-lues")
    public ResponseEntity<List<Map<String, Object>>> getMesNonLues(Authentication auth) {
        List<Notification> notifs = notificationService.getMesNonLues(auth.getName());
        return ResponseEntity.ok(notifs.stream().map(this::toMap).toList());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countNonLues(Authentication auth) {
        long count = notificationService.countNonLues(auth.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/lue")
    public ResponseEntity<Void> marquerLue(@PathVariable Long id, Authentication auth) {
        notificationService.marquerLue(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/lire-tout")
    public ResponseEntity<Void> marquerToutesLues(Authentication auth) {
        notificationService.marquerToutesLues(auth.getName());
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("titre", n.getTitre());
        map.put("message", n.getMessage());
        map.put("type", n.getType());
        map.put("lue", n.getLue());
        map.put("lien", n.getLien());
        map.put("createdAt", n.getCreatedAt());
        return map;
    }
}
