package com.bkbank.ledger.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LegacyLoanAccountNumberBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyLoanAccountNumberBackfill.class);
    private static final Map<String, String> LEGACY_TO_NUMERIC_LOAN_ACCOUNTS = Map.of(
            "0338486799", "2338486799",
            "0123456789", "2123456789"
    );

    private final JdbcTemplate jdbcTemplate;

    public LegacyLoanAccountNumberBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        int loanAccounts = backfill("loan_accounts", "account_number");
        int statements = backfill("credit_card_statements", "account_number");
        int transactions = backfill("transactions", "account_number");
        int emailActions = backfill("fraud_alert_email_actions", "account_id");

        int total = loanAccounts + statements + transactions + emailActions;
        if (total > 0) {
            log.info("Backfilled legacy loan account numbers in ledger tables: loan_accounts={}, credit_card_statements={}, transactions={}, fraud_alert_email_actions={}",
                    loanAccounts, statements, transactions, emailActions);
        }
    }

    private int backfill(String tableName, String columnName) {
        int updated = 0;
        for (Map.Entry<String, String> entry : LEGACY_TO_NUMERIC_LOAN_ACCOUNTS.entrySet()) {
            updated += jdbcTemplate.update(
                    "update " + tableName + " set " + columnName + " = ? where " + columnName + " = ?",
                    entry.getValue(),
                    entry.getKey()
            );
        }
        return updated;
    }
}
