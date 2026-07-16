-- 节点管理数据表
CREATE TABLE node (
    `aid` BIGINT(11) NOT NULL COMMENT '企业ID',
    `id` BIGINT(11) NOT NULL COMMENT '节点ID',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '节点名称',
    `address` VARCHAR(255) NOT NULL COMMENT '节点IP',
    `port` INT(5) DEFAULT 0 COMMENT '端口号',
    `hostname` VARCHAR(255) DEFAULT NULL COMMENT '主机名',
    `os` VARCHAR(128) DEFAULT NULL COMMENT '操作系统信息',
    `version` VARCHAR(64) DEFAULT NULL COMMENT 'worker版本号',
    `status` TINYINT(1) DEFAULT 0 COMMENT '节点状态：0-离线, 1-运行中，2-异常',
    `signature` VARCHAR(255) DEFAULT NULL COMMENT '请求签名',
    `region` VARCHAR(100) DEFAULT NULL COMMENT '所在区域',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注信息',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    INDEX `idx_id` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '节点管理数据表';

-- 智能体管理数据表
CREATE TABLE assistant (
    `aid` BIGINT(11) NOT NULL COMMENT '企业ID',
    `id` BIGINT(11) NOT NULL COMMENT 'worker ID',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '主机名',
    `node_id` VARCHAR(64) NOT NULL COMMENT '所属节点ID',
    `status` TINYINT(4) DEFAULT 0 COMMENT '状态：0-离线, 1-运行中',
    `mode` TINYINT(4) DEFAULT 0 COMMENT '模型：0-标准, 1-代理',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注信息',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    INDEX `idx_id` (id),
    INDEX `idx_node_id` (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '智能体管理数据表';

-- 智能体模型服务商数据表
CREATE TABLE provider (
    `aid` BIGINT(11) NOT NULL COMMENT '企业ID',
    `id` BIGINT(11) NOT NULL COMMENT '模型服务商ID',
    `type` VARCHAR(64) DEFAULT 0 COMMENT '模型服务商类型',
    `status` TINYINT(4) DEFAULT 0 COMMENT '状态：0-关闭, 1-开启',
    `is_system` TINYINT(4) DEFAULT 0 COMMENT '是否为系统模型：0-系统, 1-自定义',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '模型服务商名称',
    `url` VARCHAR(64) NOT NULL COMMENT '模型服务商URL',
    `models` TEXT DEFAULT NULL COMMENT '模型服务商模型列表',
    `api_keys` TEXT DEFAULT NULL COMMENT '模型服务商密钥列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '智能体模型服务商数据表';
