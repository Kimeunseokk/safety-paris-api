-- 반경 조회 1단계(사각형 BETWEEN 필터)가 전체 스캔 대신 인덱스 범위 스캔을 하도록 추가
-- 선두 컬럼 latitude로 범위를 좁히고, longitude 조건은 인덱스 안에서 걸러짐(Index Condition Pushdown)
CREATE INDEX idx_markers_lat_lng ON markers (latitude, longitude);
