package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Transaction;
import com.bkbank.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to persisting transactions independently of the main business transaction.
 * This is crucial for logging declined transactions when the main transaction rolls back due to an exception.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLoggingService {

    private final TransactionRepository transactionRepository;
    private final TransactionNotificationEventPublisher transactionNotificationEventPublisher;

    /**
     * Saves a transaction in a NEW transaction boundary.
     * Even if the calling method throws an exception and rolls back, this save will persist.
     *
     * @param transaction The transaction to save
     * @return The saved transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction logTransaction(Transaction transaction) {
        log.debug("Persisting transaction log independently: {}", transaction.getDescription());
        Transaction saved = transactionRepository.save(transaction);
        transactionNotificationEventPublisher.publish(saved.getId());
        return saved;
    }
}
