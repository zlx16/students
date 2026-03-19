package com.haomei.haomei.repository;

import com.haomei.haomei.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    Optional<TableSession> findByToken(String token);
    Optional<TableSession> findByTableNo(Integer tableNo);
}

