package com.envmonitor.repository;

import com.envmonitor.entity.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    Optional<SensorReading> findFirstByDeviceIdOrderByRecordedAtDesc(String deviceId);

    List<SensorReading> findByDeviceIdAndRecordedAtAfterOrderByRecordedAtAsc(
            String deviceId, LocalDateTime start, Pageable pageable);

    List<SensorReading> findByDeviceIdAndRecordedAtAfterOrderByRecordedAtDesc(
            String deviceId, LocalDateTime start, Pageable pageable);

    List<SensorReading> findTop50ByDeviceIdOrderByRecordedAtDesc(String deviceId);

    @Query(value = "SELECT COUNT(*) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start",
            nativeQuery = true)
    long countInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query(value = "SELECT MAX(mq135) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start",
            nativeQuery = true)
    Double maxMqInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query(value = "SELECT MIN(mq135) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start",
            nativeQuery = true)
    Double minMqInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query(value = "SELECT AVG(mq135) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start",
            nativeQuery = true)
    Double avgMqInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query(value = "SELECT AVG(temperature) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start",
            nativeQuery = true)
    Double avgTempInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query(value = "SELECT AVG(humidity) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start",
            nativeQuery = true)
    Double avgHumInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);

    @Query(value = "SELECT COUNT(*) FROM sensor_reading WHERE device_id = :deviceId AND recorded_at > :start AND pir = 0",
            nativeQuery = true)
    long countPresentInRange(@Param("deviceId") String deviceId, @Param("start") LocalDateTime start);
}
