package com.d111.PrePay.repository;

import com.d111.PrePay.model.PartyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface PartyRequestRepository extends JpaRepository<PartyRequest,Long> {
}
