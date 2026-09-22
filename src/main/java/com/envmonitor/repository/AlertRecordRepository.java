package com.envmonitor.repository;

import com.envmonitor.entity.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findTop50ByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<AlertRecord> findByDeviceIdAndResolvedFalseOrderByCreatedAtDesc(String deviceId);
}
