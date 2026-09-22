package com.envmonitor.repository;

import com.envmonitor.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findTop50ByDeviceIdOrderByCreatedAtDesc(String deviceId);
}
