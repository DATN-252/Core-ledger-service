package com.bkbank.ledger.repository;

import com.bkbank.ledger.entity.PushDeviceToken;
import com.bkbank.ledger.entity.PushDeviceToken.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {

    Optional<PushDeviceToken> findByExpoPushToken(String expoPushToken);

    Optional<PushDeviceToken> findByClient_ClientIdAndDeviceId(String clientId, String deviceId);

    List<PushDeviceToken> findByClient_ClientIdAndStatus(String clientId, TokenStatus status);
}
