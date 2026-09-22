package com.envmonitor.repository;

import com.envmonitor.entity.PresenceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresenceEventRepository extends JpaRepository<PresenceEvent, Long> {

    List<PresenceEvent> findTop100ByDeviceIdOrderByEventTimeDesc(String deviceId);

    List<PresenceEvent> findTop200ByDeviceIdAndEventTimeAfterOrderByEventTimeAsc(
            String deviceId, java.time.LocalDateTime start);
}
