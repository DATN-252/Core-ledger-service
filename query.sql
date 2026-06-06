SELECT
    id,
    amount,
    merchant_id,
    merchant_name,
    transaction_type
FROM transactions
WHERE
    merchant_id LIKE '%TEST%'
ORDER BY id DESC
LIMIT 10;