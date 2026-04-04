package ziply.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ziply.review.dto.request.HouseCreateRequest;
import ziply.review.dto.request.HouseUpdateRequest;
import ziply.review.dto.response.AddressInfo;
import ziply.review.dto.response.HouseListResponse;
import ziply.review.dto.response.UserAddressResponse;
import ziply.review.service.HouseService;

import java.util.UUID;

@Tag(name = "Review - 주거 탐색 집(매물) API", description = "주거 탐색 카드 내 집(매물) 추가 및 관리 API")
@RestController
@RequestMapping("/api/v1/review/card")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    @Operation(summary = "집(매물) 일괄 추가", description = "여러 개의 집 정보를 리스트로 받아 한 번에 저장합니다. 주소가 유효하지 않으면 전체 실패 처리됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "집 생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 주소 포함")
    @PostMapping("/{searchCardId}/houses")
    public ResponseEntity<List<Long>> createHouses(@PathVariable UUID searchCardId,
                                                   @AuthenticationPrincipal Long userId,
                                                   @RequestBody @Valid List<HouseCreateRequest> requests) {
        List<Long> createdIds = houseService.createHouses(searchCardId, requests, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdIds);
    }

    @Operation(summary = "탐색 카드별 집 목록 조회", description = "특정 탐색 카드에 등록된 모든 집 정보를 조회합니다.")
    @GetMapping("/{searchCardId}/houses")
    public ResponseEntity<List<HouseListResponse>> getHouses(@PathVariable UUID searchCardId,
                                                             @AuthenticationPrincipal Long userId) {
        List<HouseListResponse> responses = houseService.getHousesBySearchCard(searchCardId, userId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "집 정보 수정", description = "등록된 집의 주소 또는 방문 예정 시간을 수정합니다. 주소 변경 시 좌표 재계산 및 분석 데이터 업데이트가 수행됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "수정 성공")
    @PatchMapping("/{houseId}")
    public ResponseEntity<Void> updateHouse(@PathVariable Long houseId,
                                            @AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody HouseUpdateRequest request) {
        houseService.updateHouse(houseId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "집(매물) 삭제", description = "등록된 집 정보를 삭제합니다. 연관된 체크리스트와 분석 데이터도 함께 삭제됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping("/{houseId}")
    public ResponseEntity<Void> deleteHouse(@PathVariable Long houseId,
                                            @AuthenticationPrincipal Long userId) {
        houseService.deleteHouse(houseId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "유저의 전체 방문 주소 조회", description = "로그인한 유저가 등록했던 모든 주소를 중복 없이 가져옵니다.")
    @GetMapping("/user/addresses")
    public ResponseEntity<UserAddressResponse> getAllUserAddresses(@AuthenticationPrincipal Long userId) {
        List<AddressInfo> addressInfos = houseService.getAllUniqueAddresses(userId);
        return ResponseEntity.ok(new UserAddressResponse(addressInfos));
    }
}