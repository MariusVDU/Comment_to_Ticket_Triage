package com.pulsedesk.triage.repo;

import com.pulsedesk.triage.domain.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {}

