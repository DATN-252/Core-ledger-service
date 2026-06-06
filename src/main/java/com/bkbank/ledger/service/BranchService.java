package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Branch;
import com.bkbank.ledger.entity.enums.BranchStatus;
import com.bkbank.ledger.entity.enums.BranchType;
import com.bkbank.ledger.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    public static final String HEAD_OFFICE_BRANCH_ID = "HO001";

    private final BranchRepository branchRepository;

    public List<Branch> getActiveBranches() {
        return branchRepository.findByStatusOrderByBranchNameAsc(BranchStatus.ACTIVE);
    }

    public Branch getBranch(String branchId) {
        return branchRepository.findByBranchId(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchId));
    }

    public Branch resolveBranchOrThrow(String branchId) {
        if (branchId == null || branchId.isBlank()) {
            throw new RuntimeException("Branch ID is required");
        }
        Branch branch = getBranch(branchId);
        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new RuntimeException("Branch is inactive: " + branchId);
        }
        return branch;
    }

    @Transactional
    public void ensureDefaultBranches() {
        upsertBranch("HO001", "HO", "BKBank Head Office", BranchType.HEAD_OFFICE,
                "1 Le Loi, District 1", "Ho Chi Minh City", "02899990001");
        upsertBranch("BR001", "HN01", "BKBank Hanoi Central", BranchType.BRANCH,
                "12 Trang Tien, Hoan Kiem", "Hanoi", "02499990001");
        upsertBranch("BR002", "HCM01", "BKBank Saigon Central", BranchType.BRANCH,
                "45 Nguyen Hue, District 1", "Ho Chi Minh City", "02899990002");
        upsertBranch("BR003", "DAD01", "BKBank Da Nang Riverside", BranchType.BRANCH,
                "99 Bach Dang, Hai Chau", "Da Nang", "023699990003");
        log.info("Default branches initialized");
    }

    private void upsertBranch(String branchId,
                              String branchCode,
                              String branchName,
                              BranchType branchType,
                              String addressLine,
                              String cityName,
                              String phoneNumber) {
        Branch branch = branchRepository.findByBranchId(branchId).orElseGet(Branch::new);
        branch.setBranchId(branchId);
        branch.setBranchCode(branchCode);
        branch.setBranchName(branchName);
        branch.setBranchType(branchType);
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setAddressLine(addressLine);
        branch.setCityName(cityName);
        branch.setPhoneNumber(phoneNumber);
        branchRepository.save(branch);
    }
}
