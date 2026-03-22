package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.CityReference;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.CityReferenceRepository;
import com.bkbank.ledger.entity.Client;
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

    private final CityReferenceRepository cityReferenceRepository;
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
        CityReference hanoi = upsertCity("VN_HAN", "Hanoi", "Vietnam", 8_400_000, 21.0285, 105.8542);
        CityReference hcmc = upsertCity("VN_HCM", "Ho Chi Minh City", "Vietnam", 9_300_000, 10.8231, 106.6297);
        CityReference danang = upsertCity("VN_DAD", "Da Nang", "Vietnam", 1_250_000, 16.0471, 108.2068);

        upsertMerchant("SP0001", "Điện lực EVN", "UTILITY",
                "11 Cua Bac", "Truc Bach", "Ba Dinh", "100000",
                21.0285, 105.8542, hanoi, "1100000001");
        upsertMerchant("SP0002", "Siêu thị GO", "RETAIL",
                "38 Nguyen Van Linh", "Tan Phong", "District 7", "700000",
                10.8231, 106.6297, hcmc, "1100000002");
        upsertMerchant("SP0003", "Tạp hóa Xanh", "RETAIL",
                "09 Tran Phu", "Hai Chau I", "Hai Chau", "550000",
                16.0471, 108.2068, danang, "1100000003");
        log.info("Demo merchants and city reference data initialized successfully.");
    }

    private CityReference upsertCity(String cityCode,
                                     String cityName,
                                     String country,
                                     Integer population,
                                     Double latitude,
                                     Double longitude) {
        CityReference city = cityReferenceRepository.findByCityCode(cityCode).orElseGet(CityReference::new);
        city.setCityCode(cityCode);
        city.setCityName(cityName);
        city.setCountry(country);
        city.setPopulation(population);
        city.setLatitude(latitude);
        city.setLongitude(longitude);
        return cityReferenceRepository.save(city);
    }

    private void upsertMerchant(String merchantId,
                                String name,
                                String category,
                                String addressLine,
                                String ward,
                                String district,
                                String postalCode,
                                Double latitude,
                                Double longitude,
                                CityReference cityReference,
                                String settlementAccountNumber) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId).orElseGet(Merchant::new);
        merchant.setMerchantId(merchantId);
        merchant.setName(name);
        merchant.setCategory(category);
        merchant.setAddressLine(addressLine);
        merchant.setWard(ward);
        merchant.setDistrict(district);
        merchant.setPostalCode(postalCode);
        merchant.setLatitude(latitude);
        merchant.setLongitude(longitude);
        merchant.setCityReference(cityReference);
        merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        merchant.setSettlementAccountNumber(settlementAccountNumber);
        merchant.setSettlementAccountName(name);
        merchant.setSettlementBankName(Merchant.DEFAULT_SETTLEMENT_BANK_NAME);
        merchant.setSettlementAccount(ensureSettlementAccount(merchant));
        merchantRepository.save(merchant);
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
            client.setCity(merchant.getCityReference() != null ? merchant.getCityReference().getCityName() : "Merchant City");
            client.setCountry(merchant.getCityReference() != null ? merchant.getCityReference().getCountry() : "Vietnam");
            return clientRepository.save(client);
        });
    }

    private String buildMerchantPhone(String merchantId) {
        String digits = merchantId != null ? merchantId.replaceAll("\\D", "") : "";
        String padded = digits + "0000000";
        return ("090" + padded).substring(0, 10);
    }
}
