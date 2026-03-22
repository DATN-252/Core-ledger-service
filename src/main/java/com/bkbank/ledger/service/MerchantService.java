package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Client;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.entity.SavingsAccount;
import com.bkbank.ledger.entity.enums.AccountStatus;
import com.bkbank.ledger.entity.enums.ClientStatus;
import com.bkbank.ledger.entity.enums.Gender;
import com.bkbank.ledger.entity.enums.IdType;
import com.bkbank.ledger.repository.ClientRepository;
import com.bkbank.ledger.repository.MerchantRepository;
import com.bkbank.ledger.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final ClientRepository clientRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    /**
     * Lấy thông tin merchant đang hoạt động.
     * Quăng lỗi nếu merchant mã không tồn tại hoặc đã bị khóa.
     */
    public Merchant getActiveMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findDetailedByMerchantId(merchantId)
                .or(() -> merchantRepository.findByMerchantId(merchantId))
                .orElseThrow(() -> new RuntimeException("Đơn vị chấp nhận thanh toán không hợp lệ hoặc chưa liên kết (Merchant ID: " + merchantId + ")"));
                
        if (merchant.getStatus() != Merchant.MerchantStatus.ACTIVE) {
            throw new RuntimeException("Giao dịch bị từ chối do Đơn vị chấp nhận thanh toán đã ngừng hoạt động.");
        }
        
        return merchant;
    }
    
    /**
     * Dành cho việc khởi tạo dữ liệu mẫu lúc boot hệ thống
     */
    @Transactional
    public void createDemoMerchantsIfNotExist() {
        List<Merchant> demoMerchants = List.of(
                buildDemoMerchant("SP0001", "Điện lực EVN", "UTILITY", "1100000001"),
                buildDemoMerchant("SP0002", "Siêu thị GO", "RETAIL", "1100000002"),
                buildDemoMerchant("SP0003", "Tạp hóa Xanh", "RETAIL", "1100000003")
        );

        for (Merchant demoMerchant : demoMerchants) {
            Merchant merchant = merchantRepository.findByMerchantId(demoMerchant.getMerchantId())
                    .orElseGet(Merchant::new);
            merchant.setMerchantId(demoMerchant.getMerchantId());
            merchant.setName(demoMerchant.getName());
            merchant.setCategory(demoMerchant.getCategory());
            merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
            merchant.setSettlementAccountNumber(demoMerchant.getSettlementAccountNumber());
            merchant.setSettlementAccountName(demoMerchant.getSettlementAccountName());
            merchant.setSettlementBankName(demoMerchant.getSettlementBankName());
            merchant.setSettlementAccount(ensureSettlementAccount(demoMerchant));
            merchantRepository.save(merchant);
        }

        log.info("Demo merchants ensured successfully.");
    }

    private Merchant buildDemoMerchant(String merchantId,
                                       String merchantName,
                                       String category,
                                       String settlementAccountNumber) {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(merchantId);
        merchant.setName(merchantName);
        merchant.setCategory(category);
        merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        merchant.setSettlementAccountNumber(settlementAccountNumber);
        merchant.setSettlementAccountName(merchantName);
        merchant.setSettlementBankName(Merchant.DEFAULT_SETTLEMENT_BANK_NAME);
        return merchant;
    }

    private SavingsAccount ensureSettlementAccount(Merchant merchant) {
        String accountNumber = merchant.getSettlementAccountNumber();
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }

        SavingsAccount existingAccount = savingsAccountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (existingAccount != null) {
            if (existingAccount.getStatus() == null || existingAccount.getStatus() == AccountStatus.PENDING) {
                existingAccount.setStatus(AccountStatus.ACTIVE);
                existingAccount.setLockReason(null);
                existingAccount = savingsAccountRepository.save(existingAccount);
            }
            return existingAccount;
        }

        Client settlementClient = ensureSettlementClient(merchant);

        SavingsAccount account = new SavingsAccount();
        account.setAccountNumber(accountNumber);
        account.setBalance(0.0);
        account.setCurrency("USD");
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(settlementClient);
        return savingsAccountRepository.save(account);
    }

    private Client ensureSettlementClient(Merchant merchant) {
        String merchantClientId = "MERCHANT_" + merchant.getMerchantId();
        return clientRepository.findByClientId(merchantClientId).orElseGet(() -> {
            Client client = new Client();
            client.setClientId(merchantClientId);
            client.setFullName(merchant.getSettlementAccountName() != null && !merchant.getSettlementAccountName().isBlank()
                    ? merchant.getSettlementAccountName()
                    : merchant.getName());
            client.setDateOfBirth(LocalDate.of(2000, 1, 1));
            client.setGender(Gender.OTHER);
            client.setEmail("merchant+" + merchant.getMerchantId().toLowerCase() + "@bkbank.local");
            client.setPhoneNumber(buildMerchantPhone(merchant.getMerchantId()));
            client.setAddress("Settlement profile for " + merchant.getName());
            client.setIdNumber("MER-" + merchant.getMerchantId());
            client.setIdType(IdType.NATIONAL_ID);
            client.setStatus(ClientStatus.ACTIVE);
            client.setCity("Merchant City");
            client.setCountry("Vietnam");
            return clientRepository.save(client);
        });
    }

    private String buildMerchantPhone(String merchantId) {
        String digits = merchantId != null ? merchantId.replaceAll("\\D", "") : "";
        String padded = (digits + "0000000");
        return ("090" + padded).substring(0, 10);
    }
}
