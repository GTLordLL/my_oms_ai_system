## POSTGRESQL数据库表设计 (DDL)
```sql
-- 1. 商品主表
CREATE TABLE  oms_product (
    id BIGSERIAL PRIMARY KEY, -- MySQL 的 AUTO_INCREMENT 换成 BIGSERIAL
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    main_image VARCHAR(500),
    status SMALLINT DEFAULT 1, -- TINYINT 换成 SMALLINT
    is_deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- DATETIME 换成 TIMESTAMP
);
COMMENT ON TABLE oms_product IS '商品主表';
COMMENT ON COLUMN oms_product.name IS '商品名称';

-- 2. SKU库存规格表
CREATE TABLE  oms_sku (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES oms_product(id), -- 外键写法
    sku_code VARCHAR(100) UNIQUE NOT NULL,
    specs VARCHAR(255),
    cost_price DECIMAL(10, 2) NOT NULL,
    origin_price DECIMAL(10, 2),
    stock_quantity INT DEFAULT 0,
    alert_quantity INT DEFAULT 10,
    is_deleted SMALLINT DEFAULT 0
);
COMMENT ON TABLE oms_sku IS 'SKU库存规格表';

-- 3. 订单主表
CREATE TABLE  oms_order (
    id BIGSERIAL PRIMARY KEY,
    platform_order_sn VARCHAR(100) UNIQUE NOT NULL,
    source_platform VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    pay_amount DECIMAL(10, 2) NOT NULL,
    post_fee DECIMAL(10, 2) DEFAULT 0.00,
    status SMALLINT NOT NULL DEFAULT 1,
    buyer_nick VARCHAR(100),
    receiver_name VARCHAR(50),
    receiver_mobile VARCHAR(20),
    receiver_address VARCHAR(500),
    logistics_no VARCHAR(100),
    deliver_time TIMESTAMP NULL,
    pay_time TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 订单详情表
CREATE TABLE  oms_order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES oms_order(id),
    sku_id BIGINT NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    unit_cost DECIMAL(10, 2) NOT NULL
);

-- 5. 库存流水表
CREATE TABLE  oms_stock_log (
    id BIGSERIAL PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    type SMALLINT NOT NULL,
    change_count INT NOT NULL,
    before_count INT NOT NULL,
    after_count INT NOT NULL,
    relation_id VARCHAR(100),
    operator VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. AI 智能经营报表 (PG 的优势所在)
CREATE TABLE  oms_ai_report (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL,
    report_type SMALLINT NOT NULL DEFAULT 1,
    content TEXT NOT NULL,
    reasoning TEXT,
    summary_data TEXT, 
    model_name VARCHAR(50),
    tokens_used INT,
    operator VARCHAR(50) DEFAULT 'SYSTEM',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 7. Prophet算法 销量预测结果表
CREATE TABLE oms_forecast_record (
    id BIGSERIAL PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    sku_code VARCHAR(100),
    forecast_date DATE NOT NULL,      -- 预测目标日期
    forecast_value DECIMAL(10, 2),    -- 预测销量 (yhat)
    upper_bound DECIMAL(10, 2),       -- 预测上限
    lower_bound DECIMAL(10, 2),       -- 预测下限
    compute_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 什么时候计算出来的
);
-- 为查询加速
CREATE INDEX idx_forecast_sku_date ON oms_forecast_record(sku_id, forecast_date);

-- 8. FP-growth算法 关联分析结果表
CREATE TABLE oms_association_rule (
    id BIGSERIAL PRIMARY KEY,

    -- 前项商品：用户买了什么 (存储 SKU ID 或 SKU Code，多商品用逗号分隔)
    ante_sku_ids VARCHAR(500) NOT NULL,
    ante_names VARCHAR(1000), -- 冗余存储商品名称，方便前端直接展示

    -- 后项商品：推荐买什么
    cons_sku_ids VARCHAR(500) NOT NULL,
    cons_names VARCHAR(1000),

    -- 核心算法指标
    support DECIMAL(10, 5),    -- 支持度：A和B同时出现的概率
    confidence DECIMAL(10, 5), -- 置信度：买了A之后买B的条件概率
    lift DECIMAL(10, 5),       -- 提升度：反映关联关系的强度 (必须 > 1)

    -- 业务元数据
    rule_type VARCHAR(20) DEFAULT 'SKU', -- 规则维度：SKU 或 CATEGORY
    occurrence_count INT,      -- 该规则在历史订单中出现的次数
    compute_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE oms_association_rule IS '购物篮关联规则记录表';
COMMENT ON COLUMN oms_association_rule.ante_sku_ids IS '前项商品ID集合';
COMMENT ON COLUMN oms_association_rule.cons_sku_ids IS '后项商品ID集合';
COMMENT ON COLUMN oms_association_rule.lift IS '提升度：大于1表示强关联';

-- 9. K-Means算法 RFM用户价值分层表
CREATE TABLE oms_rfm_analysis (
    id BIGSERIAL PRIMARY KEY,

    -- 用户唯一标识
    buyer_identifier VARCHAR(100) NOT NULL UNIQUE,
    buyer_nick VARCHAR(100), -- 冗余昵称方便展示

    -- 原始 RFM 指标 (Raw Data)
    recency INT,            -- R: 距离上次购买的天数 (越小越好)
    frequency INT,          -- F: 统计时间内购买的总次数 (越大越好)
    monetary DECIMAL(12, 2),-- M: 统计时间内消费的总金额 (越大越好)

    -- 算法分层结果
    cluster_label INT,      -- K-Means 聚类后的簇标签 (0, 1, 2...)
    customer_level VARCHAR(50), -- 业务层面的定义: 如 "重要价值客户", "濒临流失客户"

    -- 统计元数据
    last_order_time TIMESTAMP, -- 最近一次下单时间
    compute_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE oms_rfm_analysis IS '用户价值分层 (RFM) 结果表';
COMMENT ON COLUMN oms_rfm_analysis.buyer_identifier IS '用户唯一标识 (手机号或加密ID)';
COMMENT ON COLUMN oms_rfm_analysis.cluster_label IS 'K-Means 聚类生成的簇 ID';
COMMENT ON COLUMN oms_rfm_analysis.customer_level IS '根据簇特征定义的客户等级名称';

-- 10. 系统用户表
CREATE TABLE oms_sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    -- 直接存储角色标识，例如 'ADMIN', 'W_MANAGER', 'STAFF'
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
```sql
-- PG 不支持 ON UPDATE CURRENT_TIMESTAMP，需要用触发器实现（见下文）
-- 创建索引
CREATE INDEX idx_report_date ON oms_ai_report (report_date);
CREATE INDEX idx_report_type ON oms_ai_report (report_type);

-- 1. 定义自动更新时间的通用函数
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP; -- 将 update_time 设为当前时间
RETURN NEW;
END;
$$ language 'plpgsql';

-- 2. 绑定到 oms_ai_report 表
-- 注意：如果触发器已存在，先删再建防止冲突
DROP TRIGGER IF EXISTS trg_update_ai_report_time ON oms_ai_report;

CREATE TRIGGER trg_update_ai_report_time
    BEFORE UPDATE ON oms_ai_report
    FOR EACH ROW
    EXECUTE PROCEDURE update_timestamp();

```