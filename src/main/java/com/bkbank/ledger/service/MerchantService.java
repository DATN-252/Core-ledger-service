package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.CityReference;
import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.CityReferenceRepository;
import com.bkbank.ledger.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final CityReferenceRepository cityReferenceRepository;
    private final MerchantRepository merchantRepository;

    /**
     * Lấy thông tin merchant đang hoạt động.
     * Quăng lỗi nếu merchant mã không tồn tại hoặc đã bị khóa.
     */
    public Merchant getActiveMerchant(String merchantId) {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
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
                21.0285, 105.8542, hanoi);
        upsertMerchant("SP0002", "Siêu thị GO", "RETAIL",
                "38 Nguyen Van Linh", "Tan Phong", "District 7", "700000",
                10.8231, 106.6297, hcmc);
        upsertMerchant("SP0003", "Tạp hóa Xanh", "RETAIL",
                "09 Tran Phu", "Hai Chau I", "Hai Chau", "550000",
                16.0471, 108.2068, danang);
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
                                CityReference cityReference) {
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
        merchantRepository.save(merchant);
    }
}
