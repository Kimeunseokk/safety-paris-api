package com.safetyparis.safetyparis_api.repository;

import com.safetyparis.safetyparis_api.entity.Marker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MarkerRepository extends JpaRepository<Marker, Long> {

    // 1단계: 원을 감싸는 사각형으로 BETWEEN 필터 → idx_markers_lat_lng 범위 스캔으로 후보만 남김
    // 2단계: 남은 후보만 ST_Distance_Sphere로 실제 거리 계산해 원 밖(사각형 모서리) 제거
    // POINT 인자 순서는 (경도, 위도) - ST_Distance_Sphere가 x=경도, y=위도로 해석함
    @Query(value = """
            SELECT * FROM markers
            WHERE latitude BETWEEN :minLat AND :maxLat
              AND longitude BETWEEN :minLng AND :maxLng
              AND ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lng, :lat)) <= :radius
            """, nativeQuery = true)
    List<Marker> findWithinRadius(double lat, double lng, int radius,
                                  double minLat, double maxLat, double minLng, double maxLng);
}
