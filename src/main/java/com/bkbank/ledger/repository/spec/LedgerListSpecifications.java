package com.bkbank.ledger.repository.spec;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.LoanAccount;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class LedgerListSpecifications {

    private LedgerListSpecifications() {
    }

    public static Specification<Client> clientList(String q, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notLike(root.get("clientId"), "MERCHANT_%"));

            if (hasText(q)) {
                String pattern = likePattern(q);
                Join<Object, Object> branchJoin = root.join("homeBranch", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("clientId")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phoneNumber")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("city"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(branchJoin.get("branchId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(branchJoin.get("branchName"), "")), pattern)
                ));
            }

            if (hasEnumFilter(status)) {
                predicates.add(cb.equal(root.get("status").as(String.class), normalizeEnumFilter(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<LoanAccount> loanList(String q, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<LoanAccount, Client> clientJoin = root.join("client", JoinType.LEFT);
            Join<Object, Object> branchJoin = root.join("branch", JoinType.LEFT);

            if (hasText(q)) {
                String pattern = likePattern(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accountNumber")), pattern),
                        cb.like(cb.lower(cb.coalesce(clientJoin.get("fullName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(clientJoin.get("clientId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("currency"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(branchJoin.get("branchId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(branchJoin.get("branchName"), "")), pattern)
                ));
            }

            if (hasEnumFilter(status)) {
                predicates.add(cb.equal(root.get("status").as(String.class), normalizeEnumFilter(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<SavingsAccount> savingsList(String q, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<SavingsAccount, Client> clientJoin = root.join("client", JoinType.LEFT);
            Join<Object, Object> branchJoin = root.join("branch", JoinType.LEFT);

            if (hasText(q)) {
                String pattern = likePattern(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("accountNumber")), pattern),
                        cb.like(cb.lower(cb.coalesce(clientJoin.get("fullName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(clientJoin.get("clientId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("currency"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(branchJoin.get("branchId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(branchJoin.get("branchName"), "")), pattern)
                ));
            }

            if (hasEnumFilter(status)) {
                predicates.add(cb.equal(root.get("status").as(String.class), normalizeEnumFilter(status)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Transaction> transactionList(String q, String status, String transactionType, String accountType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(q)) {
                String pattern = likePattern(q);
                predicates.add(cb.or(
                        cb.like(root.get("id").as(String.class), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("paymentId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("idempotencyKey"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("merchantId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("merchantName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("accountNumber"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("location"), "")), pattern)
                ));
            }

            if (hasEnumFilter(status)) {
                predicates.add(cb.equal(root.get("status"), normalizeEnumFilter(status)));
            }

            if (hasEnumFilter(transactionType)) {
                predicates.add(cb.equal(root.get("transactionType"), normalizeEnumFilter(transactionType)));
            }

            if (hasEnumFilter(accountType)) {
                predicates.add(cb.equal(root.get("accountType"), normalizeEnumFilter(accountType)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Merchant> merchantList(String q, String status, String category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> cityJoin = root.join("cityReference", JoinType.LEFT);
            query.distinct(true);

            if (hasText(q)) {
                String pattern = likePattern(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("merchantId")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("category"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("addressLine"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("district"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("ward"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(cityJoin.get("cityName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("settlementAccountNumber"), "")), pattern)
                ));
            }

            if (hasEnumFilter(status)) {
                predicates.add(cb.equal(root.get("status").as(String.class), normalizeEnumFilter(status)));
            }

            if (hasText(category) && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(cb.lower(cb.coalesce(root.get("category"), "")), category.trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean hasEnumFilter(String value) {
        return hasText(value) && !"ALL".equalsIgnoreCase(value.trim());
    }

    private static String normalizeEnumFilter(String value) {
        return value.trim().toUpperCase();
    }

    private static String likePattern(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
