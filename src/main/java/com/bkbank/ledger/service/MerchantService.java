package com.bkbank.ledger.service;

import com.bkbank.ledger.dto.MerchantCreateRequest;
import com.bkbank.ledger.entity.CityReference;
import com.bkbank.ledger.entity.Branch;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.CityReferenceRepository;
import com.bkbank.ledger.repository.BranchRepository;
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
import java.util.Locale;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final CityReferenceRepository cityReferenceRepository;
    private final BranchRepository branchRepository;
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

    @Transactional
    public Merchant createMerchant(MerchantCreateRequest request) {
        if (request.getMerchantId() == null || request.getMerchantId().isBlank()) {
            throw new RuntimeException("Merchant ID is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Merchant name is required");
        }
        if (request.getCityName() == null || request.getCityName().isBlank()) {
            throw new RuntimeException("City name is required");
        }
        if (request.getCountry() == null || request.getCountry().isBlank()) {
            throw new RuntimeException("Country is required");
        }
        if (request.getCityPopulation() == null || request.getCityPopulation() <= 0) {
            throw new RuntimeException("City population must be greater than 0");
        }
        if (merchantRepository.findByMerchantId(request.getMerchantId()).isPresent()) {
            throw new RuntimeException("Merchant ID already exists");
        }

        CityReference cityReference = resolveCityReference(request);

        Merchant merchant = new Merchant();
        merchant.setMerchantId(request.getMerchantId().trim());
        merchant.setName(request.getName().trim());
        merchant.setCategory(trimToNull(request.getCategory()));
        merchant.setAddressLine(trimToNull(request.getAddressLine()));
        merchant.setWard(trimToNull(request.getWard()));
        merchant.setDistrict(trimToNull(request.getDistrict()));
        merchant.setPostalCode(trimToNull(request.getPostalCode()));
        merchant.setLatitude(request.getLatitude());
        merchant.setLongitude(request.getLongitude());
        merchant.setCityReference(cityReference);
        merchant.setStatus(Merchant.MerchantStatus.ACTIVE);
        merchant.setSettlementAccountNumber(resolveSettlementAccountNumber(request));
        merchant.setSettlementAccountName(trimToNull(request.getSettlementAccountName()) != null
                ? request.getSettlementAccountName().trim()
                : merchant.getName());
        merchant.setSettlementBankName(trimToNull(request.getSettlementBankName()) != null
                ? request.getSettlementBankName().trim()
                : Merchant.DEFAULT_SETTLEMENT_BANK_NAME);
        merchant.setSettlementAccount(ensureSettlementAccount(merchant));
        return merchantRepository.save(merchant);
    }
    
    /**
     * Dành cho việc khởi tạo dữ liệu mẫu lúc boot hệ thống
     */
    @Transactional
    public void createDemoMerchantsIfNotExist() {
        CityReference hanoi = upsertCity("VN_HAN", "Hanoi", "Vietnam", 8_400_000, 21.0285, 105.8542);
        CityReference hcmc = upsertCity("VN_HCM", "Ho Chi Minh City", "Vietnam", 9_300_000, 10.8231, 106.6297);
        CityReference danang = upsertCity("VN_DAD", "Da Nang", "Vietnam", 1_250_000, 16.0471, 108.2068);
        CityReference haiphong = upsertCity("VN_HPH", "Hai Phong", "Vietnam", 2_050_000, 20.8449, 106.6881);
        CityReference cantho = upsertCity("VN_CTH", "Can Tho", "Vietnam", 1_240_000, 10.0452, 105.7469);
        CityReference richmondFraudTest = upsertCity("US_RVA_FRD", "Richmond", "USA", 5_927, 37.480372, -77.34958);

        upsertMerchant("SP0001", "Điện lực EVN", "UTILITY",
                "11 Cua Bac", "Truc Bach", "Ba Dinh", "100000",
                21.0285, 105.8542, hanoi, "1100000001");
        upsertMerchant("SP0002", "Siêu thị GO", "RETAIL",
                "38 Nguyen Van Linh", "Tan Phong", "District 7", "700000",
                10.8231, 106.6297, hcmc, "1100000002");
        upsertMerchant("SP0003", "Tạp hóa Xanh", "RETAIL",
                "09 Tran Phu", "Hai Chau I", "Hai Chau", "550000",
                16.0471, 108.2068, danang, "1100000003");
        upsertMerchant("SPD001", "Metro Fresh", "grocery_pos",
                "120 Nguyen Hue", "Ben Nghe", "District 1", "700000",
                10.7768, 106.7002, hcmc, "1200000001");
        upsertMerchant("SPD002", "City Pharmacy", "health_fitness",
                "88 Ba Trieu", "Le Dai Hanh", "Hai Ba Trung", "100000",
                21.0129, 105.8493, hanoi, "1200000002");
        upsertMerchant("SPD003", "Blue Cinema", "entertainment",
                "25 Tran Phu", "Hai Chau 1", "Hai Chau", "550000",
                16.0678, 108.2213, danang, "1200000003");
        upsertMerchant("SPD004", "Harbor Cafe", "food_dining",
                "9 Tran Hung Dao", "Hoang Van Thu", "Hong Bang", "180000",
                20.8611, 106.6823, haiphong, "1200000004");
        upsertMerchant("SPD005", "River Bookstore", "shopping_net",
                "14 Hoa Binh", "Tan An", "Ninh Kieu", "900000",
                10.0358, 105.7806, cantho, "1200000005");
        upsertMerchant("FRD001", "High Risk Electronics", "shopping_pos",
                "901 Broad St", "Downtown", "Richmond", "23219",
                37.480372, -77.34958, richmondFraudTest, "1300000001");
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
        account.setBranch(settlementClient.getHomeBranch());
        return savingsAccountRepository.save(account);
    }

    private CityReference resolveCityReference(MerchantCreateRequest request) {
        return cityReferenceRepository
                .findByCityNameIgnoreCaseAndCountryIgnoreCase(request.getCityName().trim(), request.getCountry().trim())
                .orElseGet(() -> {
                    CityReference city = new CityReference();
                    city.setCityCode(buildCityCode(request.getMerchantId(), request.getCityName(), request.getCountry()));
                    city.setCityName(request.getCityName().trim());
                    city.setCountry(request.getCountry().trim());
                    city.setPopulation(request.getCityPopulation());
                    city.setLatitude(request.getLatitude());
                    city.setLongitude(request.getLongitude());
                    return cityReferenceRepository.save(city);
                });
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
            client.setHomeBranch(resolveMerchantBranch());
            client.setCity(merchant.getCityReference() != null ? merchant.getCityReference().getCityName() : "Merchant City");
            client.setCountry(merchant.getCityReference() != null ? merchant.getCityReference().getCountry() : "Vietnam");
            return clientRepository.save(client);
        });
    }

    private Branch resolveMerchantBranch() {
        return branchRepository.findByBranchId(BranchService.HEAD_OFFICE_BRANCH_ID).orElse(null);
    }

    private String buildMerchantPhone(String merchantId) {
        String digits = merchantId != null ? merchantId.replaceAll("\\D", "") : "";
        String padded = digits + "0000000";
        return ("090" + padded).substring(0, 10);
    }

    private String resolveSettlementAccountNumber(MerchantCreateRequest request) {
        String explicit = trimToNull(request.getSettlementAccountNumber());
        if (explicit != null) {
            return explicit;
        }
        String digits = request.getMerchantId().replaceAll("\\D", "");
        String padded = digits + "0000000000";
        return ("13" + padded).substring(0, 10);
    }

    private String buildCityCode(String merchantId, String cityName, String country) {
        String base = ((country != null ? country : "") + "_" + (cityName != null ? cityName : "") + "_" + (merchantId != null ? merchantId : ""))
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return base.length() <= 32 ? base : base.substring(0, 32);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
