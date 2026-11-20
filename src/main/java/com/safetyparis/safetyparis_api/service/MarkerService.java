package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.MarkerDetailResponseDto;
import com.safetyparis.safetyparis_api.dto.MarkerListResponseDto;
import com.safetyparis.safetyparis_api.entity.Marker;
import com.safetyparis.safetyparis_api.repository.MarkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkerService {
    // 1단계 사각형과 2단계 거리 계산(ST_Distance_Sphere)이 같은 지구 반지름 기준이어야 사각형이 원을 빠짐없이 감싼다.
    // 흔히 쓰는 근사값 111320m/도로 나누면 사각형 반변이 약 2,996.6m가 되어, 동서남북 경계 근처(약 3.4m 폭) 마커가 누락됨
    static final double EARTH_RADIUS_M = 6_370_986; // MySQL ST_Distance_Sphere 기본 반지름
    static final double METERS_PER_DEGREE = EARTH_RADIUS_M * Math.PI / 180; // 위도 1도 ≈ 111,195m

    private final MarkerRepository markerRepository;

    public MarkerDetailResponseDto getMarkerDetail(Long id) {
        Marker marker = markerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 마커입니다. id=" + id));
        return new  MarkerDetailResponseDto(marker);
    }

    // 위도 1도의 길이는 어디서나 같지만, 경도 1도는 위도가 높을수록 cos(위도)만큼 짧아짐
    // ponytail: 경도 ±180(날짜변경선)을 넘는 사각형은 나누지 않음 - 파리 대상 서비스라 해당 없음, 전 세계로 넓히면 두 구간으로 분할
    public List<MarkerListResponseDto> getMarkersWithinRadius(double lat, double lng, int radius) {
        double dLat = radius / METERS_PER_DEGREE;
        double dLng = radius / (METERS_PER_DEGREE * Math.cos(Math.toRadians(lat)));
        return markerRepository.findWithinRadius(lat, lng, radius,
                        lat - dLat, lat + dLat, lng - dLng, lng + dLng).stream()
                .map(MarkerListResponseDto::new)
                .toList();
    }
}
