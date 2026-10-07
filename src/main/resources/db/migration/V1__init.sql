-- 초기 스키마: 개발 DB(ddl-auto: update로 생성된 테이블)의 구조를 그대로 옮김 (mysqldump --no-data 기준)
-- 이미 실행된 마이그레이션 파일은 절대 수정하지 말 것 - 변경은 V2__..., V3__... 새 파일로 추가

CREATE TABLE users (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    email    VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     ENUM('ADMIN', 'USER') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE help_locations (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255) NOT NULL,
    type         ENUM('EMBASSY', 'HOSPITAL', 'POLICE') NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    latitude     DOUBLE       NOT NULL,
    longitude    DOUBLE       NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 관리자가 승인한 제보 (지도에 노출). 인상착의는 전부 선택 입력이라 NULL 허용
CREATE TABLE markers (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    latitude             DOUBLE       NOT NULL,
    longitude            DOUBLE       NOT NULL,
    age_group            VARCHAR(255) DEFAULT NULL,
    gender               VARCHAR(255) DEFAULT NULL,
    clothing             VARCHAR(255) DEFAULT NULL,
    race                 VARCHAR(255) DEFAULT NULL,
    head_count           INT          DEFAULT NULL,
    height               VARCHAR(255) DEFAULT NULL,
    build                VARCHAR(255) DEFAULT NULL,
    has_beard            BIT(1)       DEFAULT NULL,
    has_glasses          BIT(1)       DEFAULT NULL,
    location_description VARCHAR(255) NOT NULL,
    stolen_items         VARCHAR(255) DEFAULT NULL,
    story_content        VARCHAR(255) NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 사용자 제보 원본 (승인 전 PENDING). 승인 시 같은 값으로 markers에 복사됨
CREATE TABLE reports (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id              BIGINT       NOT NULL,
    status               ENUM('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
    latitude             DOUBLE       NOT NULL,
    longitude            DOUBLE       NOT NULL,
    age_group            VARCHAR(255) DEFAULT NULL,
    gender               VARCHAR(255) DEFAULT NULL,
    clothing             VARCHAR(255) DEFAULT NULL,
    race                 VARCHAR(255) DEFAULT NULL,
    head_count           INT          DEFAULT NULL,
    height               VARCHAR(255) DEFAULT NULL,
    build                VARCHAR(255) DEFAULT NULL,
    has_beard            BIT(1)       DEFAULT NULL,
    has_glasses          BIT(1)       DEFAULT NULL,
    location_description VARCHAR(255) NOT NULL,
    stolen_items         VARCHAR(255) DEFAULT NULL,
    story_content        VARCHAR(255) NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reports_user_id (user_id),
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
