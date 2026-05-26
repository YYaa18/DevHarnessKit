SELECT
    o.order_no,
    o.customer_no,
    c.customer_name,
    o.order_status,
    o.total_amount
FROM shop_order o
JOIN shop_customer c ON c.customer_no = o.customer_no
WHERE 1 = 1
/* if customerNo */
  AND o.customer_no LIKE :customerNo
/* if status */
  AND o.order_status = :status
ORDER BY o.created_at DESC
