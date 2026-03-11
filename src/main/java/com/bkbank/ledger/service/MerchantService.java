package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Merchant;
import com.bkbank.ledger.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

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
        if (merchantRepository.count() == 0) {
            log.info("No merchants found in DB. Creating demo merchants...");
            merchantRepository.save(new Merchant(null, "SP0001", "Điện lực EVN", "UTILITY", Merchant.MerchantStatus.ACTIVE, null, null));
            merchantRepository.save(new Merchant(null, "SP0002", "Siêu thị GO", "RETAIL", Merchant.MerchantStatus.ACTIVE, null, null));
            merchantRepository.save(new Merchant(null, "SP0003", "Tạp hóa Xanh", "RETAIL", Merchant.MerchantStatus.ACTIVE, null, null));
            log.info("Demo merchants created successfully.");
        }
    }
}
