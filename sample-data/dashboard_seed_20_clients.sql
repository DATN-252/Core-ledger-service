-- Active: 1770868476303@@autorack.proxy.rlwy.net@50993@railway
-- Dashboard seed data for core-ledger-service
-- Purpose: create 20 clients, savings accounts, loan accounts, merchants,
-- transactions, and monthly statements so the dashboard has realistic data.
-- Safe to rerun: this script only clears previously seeded rows with the
-- prefixes CLI_DASH_, S51xx, C51xx, SPD00x, and DASH26-.

BEGIN;

DELETE FROM credit_card_statements WHERE account_number LIKE 'C51%';

DELETE FROM transactions
WHERE
    account_number LIKE 'S51%'
    OR account_number LIKE 'C51%'
    OR payment_id LIKE 'DASH26-%'
    OR idempotency_key LIKE 'DASH26-%';

DELETE FROM loan_accounts WHERE account_number LIKE 'C51%';

DELETE FROM savings_accounts WHERE account_number LIKE 'S51%';

DELETE FROM clients WHERE client_id LIKE 'CLI_DASH_%';

DELETE FROM merchants
WHERE
    merchant_id IN (
        'FRD001',
        'SPD001',
        'SPD002',
        'SPD003',
        'SPD004',
        'SPD005'
    );

DELETE FROM city_reference
WHERE city_code LIKE 'DASH_%'
   OR city_code = 'FRD_RVA';

INSERT INTO
    city_reference (
        city_code,
        city_name,
        country,
        population,
        latitude,
        longitude
    )
VALUES (
        'DASH_HCM',
        'Ho Chi Minh City',
        'Vietnam',
        9200000,
        10.7758,
        106.7009
    ),
    (
        'DASH_HN',
        'Ha Noi',
        'Vietnam',
        8400000,
        21.0285,
        105.8542
    ),
    (
        'DASH_DN',
        'Da Nang',
        'Vietnam',
        1230000,
        16.0544,
        108.2022
    ),
    (
        'DASH_HP',
        'Hai Phong',
        'Vietnam',
        2050000,
        20.8449,
        106.6881
    ),
    (
        'DASH_CT',
        'Can Tho',
        'Vietnam',
        1240000,
        10.0452,
        105.7469
    ),
    (
        'FRD_RVA',
        'Richmond',
        'USA',
        5927,
        37.480372,
        -77.34958
    )
ON CONFLICT (city_code) DO
UPDATE
SET
    city_name = EXCLUDED.city_name,
    country = EXCLUDED.country,
    population = EXCLUDED.population,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude;

INSERT INTO
    merchants (
        merchant_id,
        name,
        category,
        address_line,
        ward,
        district,
        postal_code,
        latitude,
        longitude,
        city_reference_id,
        status,
        created_at,
        updated_at
    )
SELECT v.merchant_id, v.name, v.category, v.address_line, v.ward, v.district, v.postal_code, v.latitude, v.longitude, c.id, 'ACTIVE', TIMESTAMP '2025-11-01 09:00:00', TIMESTAMP '2026-03-20 09:00:00'
FROM (
        VALUES (
                'SPD001', 'Metro Fresh', 'grocery_pos', '120 Nguyen Hue', 'Ben Nghe', 'District 1', '700000', 10.7768, 106.7002, 'DASH_HCM'
            ), (
                'SPD002', 'City Pharmacy', 'health_fitness', '88 Ba Trieu', 'Le Dai Hanh', 'Hai Ba Trung', '100000', 21.0129, 105.8493, 'DASH_HN'
            ), (
                'SPD003', 'Blue Cinema', 'entertainment', '25 Tran Phu', 'Hai Chau 1', 'Hai Chau', '550000', 16.0678, 108.2213, 'DASH_DN'
            ), (
                'SPD004', 'Harbor Cafe', 'food_dining', '9 Tran Hung Dao', 'Hoang Van Thu', 'Hong Bang', '180000', 20.8611, 106.6823, 'DASH_HP'
            ), (
                'SPD005', 'River Bookstore', 'shopping_net', '14 Hoa Binh', 'Tan An', 'Ninh Kieu', '900000', 10.0358, 105.7806, 'DASH_CT'
            ), (
                'FRD001', 'High Risk Electronics', 'shopping_pos', '901 Broad St', 'Downtown', 'Richmond', '23219', 37.480372, -77.34958, 'FRD_RVA'
            )
    ) AS v (
        merchant_id, name, category, address_line, ward, district, postal_code, latitude, longitude, city_code
    )
    JOIN city_reference c ON c.city_code = v.city_code
ON CONFLICT (merchant_id) DO
UPDATE
SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    address_line = EXCLUDED.address_line,
    ward = EXCLUDED.ward,
    district = EXCLUDED.district,
    postal_code = EXCLUDED.postal_code,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    city_reference_id = EXCLUDED.city_reference_id,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

DO $$
DECLARE
    names TEXT[] := ARRAY[
        'Nguyen Van An', 'Tran Thi Bich', 'Le Quoc Bao', 'Pham Minh Chau', 'Vo Duc Dat',
        'Hoang Thu Em', 'Bui Gia Han', 'Dang Khai Nam', 'Doan Linh Phuong', 'Ngo Quoc Huy',
        'Phan Bao Ngoc', 'Duong Nhat Anh', 'Mai Gia Linh', 'Truong Minh Khoa', 'Ly Bao Tram',
        'Nguyen Hoang Son', 'Tran Thanh Vy', 'Le Minh Thu', 'Vo Quang Hiep', 'Pham Bao Uyen'
    ];
    cities TEXT[] := ARRAY['Ha Noi', 'Ho Chi Minh City', 'Da Nang', 'Hai Phong', 'Can Tho'];
    districts TEXT[] := ARRAY['Ba Dinh', 'District 7', 'Hai Chau', 'Le Chan', 'Ninh Kieu'];
    wards TEXT[] := ARRAY['Kim Ma', 'Tan Phong', 'Hai Chau 1', 'An Bien', 'Tan An'];
    merchant_ids TEXT[] := ARRAY['SPD001', 'SPD002', 'SPD003', 'SPD004', 'SPD005'];
    merchant_names TEXT[] := ARRAY['Metro Fresh', 'City Pharmacy', 'Blue Cinema', 'Harbor Cafe', 'River Bookstore'];
    merchant_locations TEXT[] := ARRAY[
        '120 Nguyen Hue, Ben Nghe, District 1, Ho Chi Minh City, Vietnam, 700000',
        '88 Ba Trieu, Le Dai Hanh, Hai Ba Trung, Ha Noi, Vietnam, 100000',
        '25 Tran Phu, Hai Chau 1, Hai Chau, Da Nang, Vietnam, 550000',
        '9 Tran Hung Dao, Hoang Van Thu, Hong Bang, Hai Phong, Vietnam, 180000',
        '14 Hoa Binh, Tan An, Ninh Kieu, Can Tho, Vietnam, 900000'
    ];
    merchant_lats DOUBLE PRECISION[] := ARRAY[10.7768, 21.0129, 16.0678, 20.8611, 10.0358];
    merchant_lngs DOUBLE PRECISION[] := ARRAY[106.7002, 105.8493, 108.2213, 106.6823, 105.7806];
    v_client_pk BIGINT;
    v_savings_balance DOUBLE PRECISION;
    v_loan_balance DOUBLE PRECISION;
    v_client_code TEXT;
    v_savings_account TEXT;
    v_loan_account TEXT;
    v_phone TEXT;
    v_id_number TEXT;
    v_created_at TIMESTAMP;
    v_city_idx INT;
    v_billing_day INT;
    v_credit_limit DOUBLE PRECISION;
    v_card_network TEXT;
    v_auth_code TEXT;
    v_stan TEXT;
    v_rrn TEXT;
    v_i INT;
    v_m1 INT;
    v_m2 INT;
    v_m3 INT;
    v_m4 INT;
    v_amt DOUBLE PRECISION;
BEGIN
    FOR v_i IN 1..20 LOOP
        v_client_code := 'CLI_DASH_' || LPAD(v_i::TEXT, 3, '0');
        v_savings_account := 'S51' || LPAD(v_i::TEXT, 2, '0');
        v_loan_account := 'C51' || LPAD(v_i::TEXT, 2, '0');
        v_phone := '0901' || LPAD((100000 + v_i)::TEXT, 6, '0');
        v_id_number := '07920' || LPAD(v_i::TEXT, 7, '0');
        v_created_at := TIMESTAMP '2025-10-05 09:00:00' + ((v_i - 1) * INTERVAL '6 day');
        v_city_idx := ((v_i - 1) % 5) + 1;
        v_billing_day := CASE (v_i - 1) % 5
            WHEN 0 THEN 5
            WHEN 1 THEN 10
            WHEN 2 THEN 15
            WHEN 3 THEN 20
            ELSE 25
        END;
        v_credit_limit := 6000 + (v_i * 450);
        v_card_network := CASE WHEN v_i % 2 = 1 THEN 'VISA' ELSE 'MASTERCARD' END;

        INSERT INTO clients (
            client_id, full_name, date_of_birth, gender, email, phone_number, address,
            id_number, id_type, status, city, country, occupation, employer_name,
            employer_address, employment_type, monthly_income, years_at_current_job,
            created_at, updated_at
        )
        VALUES (
            v_client_code,
            names[v_i],
            DATE '1985-01-01' + ((v_i - 1) * 410),
            CASE WHEN v_i % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
            'dash' || LPAD(v_i::TEXT, 2, '0') || '@bkbank.local',
            v_phone,
            (20 + v_i)::TEXT || ' ' || CASE v_city_idx
                WHEN 1 THEN 'Giang Vo Street'
                WHEN 2 THEN 'Nguyen Huu Tho Street'
                WHEN 3 THEN 'Tran Phu Street'
                WHEN 4 THEN 'To Hieu Street'
                ELSE 'Hoa Binh Boulevard'
            END,
            v_id_number,
            'NATIONAL_ID',
            'ACTIVE',
            cities[v_city_idx],
            'Vietnam',
            CASE WHEN v_i % 3 = 0 THEN 'Software Engineer'
                 WHEN v_i % 3 = 1 THEN 'Account Manager'
                 ELSE 'Retail Owner'
            END,
            CASE WHEN v_i % 3 = 0 THEN 'BK Digital'
                 WHEN v_i % 3 = 1 THEN 'Viet Commerce'
                 ELSE 'Freelance'
            END,
            wards[v_city_idx] || ', ' || districts[v_city_idx] || ', ' || cities[v_city_idx],
            CASE WHEN v_i % 3 = 2 THEN 'SELF_EMPLOYED' ELSE 'FULL_TIME' END,
            1200 + (v_i * 95),
            1 + (v_i % 8),
            v_created_at,
            v_created_at
        )
        RETURNING id INTO v_client_pk;

        INSERT INTO savings_accounts (account_number, balance, currency, status, client_id, created_at, updated_at)
        VALUES (v_savings_account, 0, 'USD', 'ACTIVE', v_client_pk, v_created_at + INTERVAL '1 day', v_created_at + INTERVAL '1 day');

        INSERT INTO loan_accounts (
            account_number, principal, principal_outstanding, currency,
            billing_day_of_month, payment_due_days, minimum_payment_rate,
            minimum_payment_floor, status, client_id, created_at, updated_at
        )
        VALUES (
            v_loan_account, v_credit_limit, 0, 'USD', v_billing_day, 20, 5, 15,
            'ACTIVE', v_client_pk, v_created_at + INTERVAL '2 day', v_created_at + INTERVAL '2 day'
        );

        v_savings_balance := 900 + (v_i * 140);

        v_amt := 900 + (v_i * 35);
        v_savings_balance := v_savings_balance + v_amt;
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, response_code, response_message, status
        )
        VALUES (
            v_savings_account, 'SAVINGS', 'DEPOSIT', v_amt, 'USD',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-01',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-01',
            'BRANCH', MAKE_TIMESTAMP(2025, 12, 5 + ((v_i - 1) % 10), 9, 15, 0),
            'Monthly salary deposit', v_savings_balance,
            NULL, NULL, cities[v_city_idx] || ', Vietnam',
            NULL, NULL, 'NAPAS', '00', 'Approved', 'SUCCESS'
        );

        v_amt := 90 + (v_i * 7);
        v_savings_balance := v_savings_balance - v_amt;
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, auth_code, response_code, response_message, status
        )
        VALUES (
            v_savings_account, 'SAVINGS', 'WITHDRAWAL', v_amt, 'USD',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-02',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-02',
            'ATM', MAKE_TIMESTAMP(2025, 12, 10 + ((v_i - 1) % 10), 18, 5, 0),
            'ATM cash withdrawal', v_savings_balance,
            'ATM-DASH-' || v_city_idx, 'BKBank ATM', cities[v_city_idx] || ', Vietnam',
            NULL, NULL, 'NAPAS', 'ATM' || LPAD(v_i::TEXT, 3, '0'), '00', 'Approved', 'SUCCESS'
        );

        v_amt := 980 + (v_i * 40);
        v_savings_balance := v_savings_balance + v_amt;
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, response_code, response_message, status
        )
        VALUES (
            v_savings_account, 'SAVINGS', 'DEPOSIT', v_amt, 'USD',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-03',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-03',
            'BRANCH', MAKE_TIMESTAMP(2026, 1, 20 + ((v_i - 1) % 8), 9, 0, 0),
            'Salary top-up', v_savings_balance,
            NULL, NULL, cities[v_city_idx] || ', Vietnam',
            NULL, NULL, 'NAPAS', '00', 'Approved', 'SUCCESS'
        );

        v_amt := 120 + (v_i * 9);
        v_savings_balance := v_savings_balance - v_amt;
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, auth_code, response_code, response_message, status
        )
        VALUES (
            v_savings_account, 'SAVINGS', 'WITHDRAWAL', v_amt, 'USD',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-04',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-04',
            'POS', MAKE_TIMESTAMP(2026, 2, 6 + ((v_i - 1) % 10), 18, 20, 0),
            'Debit card purchase', v_savings_balance,
            merchant_ids[((v_i + 1) % 5) + 1], merchant_names[((v_i + 1) % 5) + 1], merchant_locations[((v_i + 1) % 5) + 1],
            merchant_lats[((v_i + 1) % 5) + 1], merchant_lngs[((v_i + 1) % 5) + 1],
            'NAPAS', 'DB' || LPAD(v_i::TEXT, 4, '0'), '00', 'Approved', 'SUCCESS'
        );

        v_amt := 1050 + (v_i * 50);
        v_savings_balance := v_savings_balance + v_amt;
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, response_code, response_message, status
        )
        VALUES (
            v_savings_account, 'SAVINGS', 'DEPOSIT', v_amt, 'USD',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-05',
            'DASH26-SAV-' || LPAD(v_i::TEXT, 2, '0') || '-05',
            'MOBILE', MAKE_TIMESTAMP(2026, 3, 8 + ((v_i - 1) % 8), 8, 45, 0),
            'Incoming transfer', v_savings_balance,
            NULL, NULL, cities[v_city_idx] || ', Vietnam',
            NULL, NULL, 'NAPAS', '00', 'Approved', 'SUCCESS'
        );

        v_loan_balance := 0;
        v_m1 := ((v_i - 1) % 5) + 1;
        v_m2 := (v_m1 % 5) + 1;
        v_m3 := (v_m2 % 5) + 1;
        v_m4 := (v_m3 % 5) + 1;

        v_amt := 80 + (v_i * 6);
        v_loan_balance := v_loan_balance + v_amt;
        v_auth_code := 'A' || LPAD(v_i::TEXT, 5, '0');
        v_stan := LPAD((810000 + v_i)::TEXT, 6, '0');
        v_rrn := '2601' || LPAD(v_i::TEXT, 8, '0');
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, auth_code, stan, rrn, external_reference,
            response_code, response_message, status
        )
        VALUES (
            v_loan_account, 'LOAN', 'CHARGE', v_amt, 'USD',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-01',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-01',
            'POS', MAKE_TIMESTAMP(2026, 1, 7 + ((v_i - 1) % 8), 12, 10, 0),
            'Credit card charge', v_loan_balance,
            merchant_ids[v_m1], merchant_names[v_m1], merchant_locations[v_m1],
            merchant_lats[v_m1], merchant_lngs[v_m1], v_card_network,
            v_auth_code, v_stan, v_rrn, 'ORDER-' || LPAD(v_i::TEXT, 4, '0') || '-A',
            '00', 'Approved', 'SUCCESS'
        );

        v_amt := 45 + (v_i * 5);
        v_loan_balance := v_loan_balance + v_amt;
        v_auth_code := 'B' || LPAD(v_i::TEXT, 5, '0');
        v_stan := LPAD((820000 + v_i)::TEXT, 6, '0');
        v_rrn := '2602' || LPAD(v_i::TEXT, 8, '0');
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, auth_code, stan, rrn, external_reference,
            response_code, response_message, status
        )
        VALUES (
            v_loan_account, 'LOAN', 'CHARGE', v_amt, 'USD',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-02',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-02',
            'ONLINE', MAKE_TIMESTAMP(2026, 1, 18 + ((v_i - 1) % 6), 20, 25, 0),
            'E-commerce checkout', v_loan_balance,
            merchant_ids[v_m2], merchant_names[v_m2], merchant_locations[v_m2],
            merchant_lats[v_m2], merchant_lngs[v_m2], v_card_network,
            v_auth_code, v_stan, v_rrn, 'ORDER-' || LPAD(v_i::TEXT, 4, '0') || '-B',
            '00', 'Approved', 'SUCCESS'
        );

        v_amt := 35 + (v_i * 3);
        v_loan_balance := v_loan_balance - v_amt;
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, location,
            card_network, response_code, response_message, status
        )
        VALUES (
            v_loan_account, 'LOAN', 'PAYMENT', v_amt, 'USD',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-03',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-03',
            'MOBILE', MAKE_TIMESTAMP(2026, 2, 4 + ((v_i - 1) % 8), 9, 30, 0),
            'Credit card payment', v_loan_balance,
            cities[v_city_idx] || ', Vietnam',
            v_card_network, '00', 'Approved', 'SUCCESS'
        );

        v_amt := 110 + (v_i * 7);
        v_loan_balance := v_loan_balance + v_amt;
        v_auth_code := 'C' || LPAD(v_i::TEXT, 5, '0');
        v_stan := LPAD((830000 + v_i)::TEXT, 6, '0');
        v_rrn := '2603' || LPAD(v_i::TEXT, 8, '0');
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, auth_code, stan, rrn, external_reference,
            response_code, response_message, status
        )
        VALUES (
            v_loan_account, 'LOAN', 'CHARGE', v_amt, 'USD',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-04',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-04',
            'POS', MAKE_TIMESTAMP(2026, 2, 13 + ((v_i - 1) % 8), 19, 5, 0),
            'Weekend charge', v_loan_balance,
            merchant_ids[v_m3], merchant_names[v_m3], merchant_locations[v_m3],
            merchant_lats[v_m3], merchant_lngs[v_m3], v_card_network,
            v_auth_code, v_stan, v_rrn, 'ORDER-' || LPAD(v_i::TEXT, 4, '0') || '-C',
            '00', 'Approved', 'SUCCESS'
        );

        IF v_i % 4 = 0 THEN
            INSERT INTO transactions (
                account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
                channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
                latitude, longitude, card_network, response_code, response_message, status
            )
            VALUES (
                v_loan_account, 'LOAN', 'CHARGE', 950 + (v_i * 20), 'USD',
                'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-05F',
                'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-05F',
                'ONLINE', MAKE_TIMESTAMP(2026, 3, 16 + ((v_i - 1) % 3), 14, 40, 0),
                'Failed charge: Credit limit exceeded', v_loan_balance,
                merchant_ids[v_m4], merchant_names[v_m4], merchant_locations[v_m4],
                merchant_lats[v_m4], merchant_lngs[v_m4], v_card_network,
                '51', 'Credit limit exceeded', 'FAILED'
            );
        END IF;

        v_amt := 130 + (v_i * 8);
        v_loan_balance := v_loan_balance + v_amt;
        v_auth_code := 'D' || LPAD(v_i::TEXT, 5, '0');
        v_stan := LPAD((840000 + v_i)::TEXT, 6, '0');
        v_rrn := '2604' || LPAD(v_i::TEXT, 8, '0');
        INSERT INTO transactions (
            account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
            channel, transaction_date, description, balance_after, merchant_id, merchant_name, location,
            latitude, longitude, card_network, auth_code, stan, rrn, external_reference,
            response_code, response_message, status
        )
        VALUES (
            v_loan_account, 'LOAN', 'CHARGE', v_amt, 'USD',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-05',
            'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-05',
            CASE WHEN v_i % 2 = 0 THEN 'POS' ELSE 'ONLINE' END,
            MAKE_TIMESTAMP(2026, 3, 3 + ((v_i - 1) % 8), 17, 45, 0),
            'Monthly spend', v_loan_balance,
            merchant_ids[v_m4], merchant_names[v_m4], merchant_locations[v_m4],
            merchant_lats[v_m4], merchant_lngs[v_m4], v_card_network,
            v_auth_code, v_stan, v_rrn, 'ORDER-' || LPAD(v_i::TEXT, 4, '0') || '-D',
            '00', 'Approved', 'SUCCESS'
        );

        IF v_i % 2 = 0 THEN
            v_amt := 55 + (v_i * 4);
            v_loan_balance := v_loan_balance - v_amt;
            INSERT INTO transactions (
                account_number, account_type, transaction_type, amount, currency, payment_id, idempotency_key,
                channel, transaction_date, description, balance_after, location,
                card_network, response_code, response_message, status
            )
            VALUES (
                v_loan_account, 'LOAN', 'PAYMENT', v_amt, 'USD',
                'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-06',
                'DASH26-LOAN-' || LPAD(v_i::TEXT, 2, '0') || '-06',
                'MOBILE', MAKE_TIMESTAMP(2026, 3, 9 + ((v_i - 1) % 6), 8, 20, 0),
                'Statement payment', v_loan_balance,
                cities[v_city_idx] || ', Vietnam',
                v_card_network, '00', 'Approved', 'SUCCESS'
            );
        END IF;

        UPDATE savings_accounts
        SET balance = v_savings_balance,
            updated_at = TIMESTAMP '2026-03-20 08:00:00'
        WHERE account_number = v_savings_account;

        UPDATE loan_accounts
        SET principal_outstanding = v_loan_balance,
            updated_at = TIMESTAMP '2026-03-20 08:00:00'
        WHERE account_number = v_loan_account;
    END LOOP;
END $$;

INSERT INTO
    credit_card_statements (
        account_number,
        statement_period_start,
        statement_period_end,
        billing_date,
        due_date,
        previous_balance,
        total_charges,
        total_payments,
        minimum_due,
        new_balance,
        available_credit_at_billing,
        transaction_count,
        statement_status,
        paid_amount_after_statement,
        remaining_minimum_due,
        remaining_balance,
        last_payment_date,
        created_at,
        updated_at
    )
WITH
    seeded_loans AS (
        SELECT
            account_number,
            principal,
            billing_day_of_month,
            payment_due_days,
            minimum_payment_rate,
            minimum_payment_floor
        FROM loan_accounts
        WHERE
            account_number LIKE 'C51%'
    ),
    periods AS (
        SELECT
            sl.account_number,
            sl.principal,
            sl.payment_due_days,
            sl.minimum_payment_rate,
            sl.minimum_payment_floor,
            CASE
                WHEN sl.billing_day_of_month <= 20 THEN MAKE_DATE(
                    2026,
                    3,
                    sl.billing_day_of_month
                )
                ELSE MAKE_DATE(
                    2026,
                    2,
                    sl.billing_day_of_month
                )
            END AS billing_date,
            CASE
                WHEN sl.billing_day_of_month <= 20 THEN MAKE_DATE(
                    2026,
                    2,
                    sl.billing_day_of_month
                )
                ELSE MAKE_DATE(
                    2026,
                    1,
                    sl.billing_day_of_month
                )
            END AS previous_billing_date
        FROM seeded_loans sl
    ),
    statement_values AS (
        SELECT
            p.account_number,
            (
                p.previous_billing_date + INTERVAL '1 day'
            )::DATE AS statement_period_start,
            p.billing_date AS statement_period_end,
            p.billing_date,
            (
                p.billing_date + p.payment_due_days
            ) AS due_date,
            COALESCE(prev.previous_balance, 0) AS previous_balance,
            COALESCE(period_tx.total_charges, 0) AS total_charges,
            COALESCE(period_tx.total_payments, 0) AS total_payments,
            COALESCE(
                period_tx.transaction_count,
                0
            ) AS transaction_count,
            p.principal,
            p.minimum_payment_rate,
            p.minimum_payment_floor,
            COALESCE(
                after_payments.paid_after_statement,
                0
            ) AS paid_after_statement,
            after_payments.last_payment_date
        FROM
            periods p
            LEFT JOIN LATERAL (
                SELECT t.balance_after AS previous_balance
                FROM transactions t
                WHERE
                    t.account_number = p.account_number
                    AND t.account_type = 'LOAN'
                    AND t.status = 'SUCCESS'
                    AND t.transaction_date < (
                        p.previous_billing_date + INTERVAL '1 day'
                    )
                ORDER BY t.transaction_date DESC, t.id DESC
                LIMIT 1
            ) prev ON TRUE
            LEFT JOIN LATERAL (
                SELECT
                    SUM(
                        CASE
                            WHEN t.transaction_type = 'CHARGE' THEN t.amount
                            ELSE 0
                        END
                    ) AS total_charges,
                    SUM(
                        CASE
                            WHEN t.transaction_type = 'PAYMENT' THEN t.amount
                            ELSE 0
                        END
                    ) AS total_payments,
                    COUNT(*) AS transaction_count
                FROM transactions t
                WHERE
                    t.account_number = p.account_number
                    AND t.account_type = 'LOAN'
                    AND t.status = 'SUCCESS'
                    AND t.transaction_date >= (
                        p.previous_billing_date + INTERVAL '1 day'
                    )
                    AND t.transaction_date < (
                        p.billing_date + INTERVAL '1 day'
                    )
            ) period_tx ON TRUE
            LEFT JOIN LATERAL (
                SELECT
                    SUM(t.amount) AS paid_after_statement,
                    MAX(t.transaction_date) AS last_payment_date
                FROM transactions t
                WHERE
                    t.account_number = p.account_number
                    AND t.account_type = 'LOAN'
                    AND t.transaction_type = 'PAYMENT'
                    AND t.status = 'SUCCESS'
                    AND t.transaction_date >= (
                        p.billing_date + INTERVAL '1 day'
                    )
                    AND t.transaction_date < TIMESTAMP '2026-03-21 00:00:00'
            ) after_payments ON TRUE
    )
SELECT
    sv.account_number,
    sv.statement_period_start,
    sv.statement_period_end,
    sv.billing_date,
    sv.due_date,
    ROUND(
        sv.previous_balance::NUMERIC,
        2
    )::DOUBLE PRECISION,
    ROUND(sv.total_charges::NUMERIC, 2)::DOUBLE PRECISION,
    ROUND(sv.total_payments::NUMERIC, 2)::DOUBLE PRECISION,
    ROUND(
        CASE
            WHEN (
                sv.previous_balance + sv.total_charges - sv.total_payments
            ) <= 0 THEN 0
            ELSE LEAST(
                (
                    sv.previous_balance + sv.total_charges - sv.total_payments
                ),
                GREATEST(
                    (
                        sv.previous_balance + sv.total_charges - sv.total_payments
                    ) * sv.minimum_payment_rate / 100.0,
                    sv.minimum_payment_floor
                )
            )
        END::NUMERIC,
        2
    )::DOUBLE PRECISION,
    ROUND(
        (
            sv.previous_balance + sv.total_charges - sv.total_payments
        )::NUMERIC,
        2
    )::DOUBLE PRECISION,
    ROUND(
        (
            sv.principal - (
                sv.previous_balance + sv.total_charges - sv.total_payments
            )
        )::NUMERIC,
        2
    )::DOUBLE PRECISION,
    sv.transaction_count,
    CASE
        WHEN GREATEST(
            (
                sv.previous_balance + sv.total_charges - sv.total_payments
            ) - sv.paid_after_statement,
            0
        ) <= 0 THEN 'PAID'
        WHEN DATE '2026-03-20' > sv.due_date
        AND GREATEST(
            LEAST(
                (
                    sv.previous_balance + sv.total_charges - sv.total_payments
                ),
                GREATEST(
                    (
                        sv.previous_balance + sv.total_charges - sv.total_payments
                    ) * sv.minimum_payment_rate / 100.0,
                    sv.minimum_payment_floor
                )
            ) - sv.paid_after_statement,
            0
        ) > 0 THEN 'OVERDUE'
        WHEN sv.paid_after_statement > 0 THEN 'PARTIALLY_PAID'
        ELSE 'OPEN'
    END,
    ROUND(
        sv.paid_after_statement::NUMERIC,
        2
    )::DOUBLE PRECISION,
    ROUND(
        GREATEST(
            LEAST(
                (
                    sv.previous_balance + sv.total_charges - sv.total_payments
                ),
                GREATEST(
                    (
                        sv.previous_balance + sv.total_charges - sv.total_payments
                    ) * sv.minimum_payment_rate / 100.0,
                    sv.minimum_payment_floor
                )
            ) - sv.paid_after_statement,
            0
        )::NUMERIC,
        2
    )::DOUBLE PRECISION,
    ROUND(
        GREATEST(
            (
                sv.previous_balance + sv.total_charges - sv.total_payments
            ) - sv.paid_after_statement,
            0
        )::NUMERIC,
        2
    )::DOUBLE PRECISION,
    sv.last_payment_date,
    (
        sv.billing_date + TIME '18:00:00'
    )::TIMESTAMP,
    TIMESTAMP '2026-03-20 09:00:00'
FROM statement_values sv
ON CONFLICT (account_number, billing_date) DO
UPDATE
SET
    statement_period_start = EXCLUDED.statement_period_start,
    statement_period_end = EXCLUDED.statement_period_end,
    due_date = EXCLUDED.due_date,
    previous_balance = EXCLUDED.previous_balance,
    total_charges = EXCLUDED.total_charges,
    total_payments = EXCLUDED.total_payments,
    minimum_due = EXCLUDED.minimum_due,
    new_balance = EXCLUDED.new_balance,
    available_credit_at_billing = EXCLUDED.available_credit_at_billing,
    transaction_count = EXCLUDED.transaction_count,
    statement_status = EXCLUDED.statement_status,
    paid_amount_after_statement = EXCLUDED.paid_amount_after_statement,
    remaining_minimum_due = EXCLUDED.remaining_minimum_due,
    remaining_balance = EXCLUDED.remaining_balance,
    last_payment_date = EXCLUDED.last_payment_date,
    updated_at = EXCLUDED.updated_at;

COMMIT;
