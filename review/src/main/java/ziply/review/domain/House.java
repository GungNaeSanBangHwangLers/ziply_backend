package ziply.review.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "houses")
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_card_id")
    private SearchCard searchCard;

    private String address;

    @Column(name = "region_name")
    private String regionName;   // 카카오 API region_3depth_name (법정동, 예: 상도동)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HouseStatus status = HouseStatus.BEFORE;

    private LocalDateTime visitDateTime;
    private Double latitude;
    private Double longitude;

    // 측정 데이터 연관관계
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Measurement> measurements = new ArrayList<>();

    // 이미지 데이터 연관관계
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HouseImage> houseImages = new ArrayList<>();

    // 상태 업데이트 메서드
    public void updateStatus(HouseStatus status) {
        this.status = status;
    }

    // 정보 수정 메서드
    public void update(String address, LocalDateTime visitDateTime) {
        if (address != null && !address.trim().isEmpty()) {
            this.address = address;
        }
        if (visitDateTime != null) {
            this.visitDateTime = visitDateTime;
        }
    }

    // 방문 일정에 따른 상태 동기화 로직
    public void syncStatus(LocalDate today) {
        if (this.status == HouseStatus.AFTER) {
            return;
        }

        if (this.visitDateTime == null) {
            this.status = HouseStatus.BEFORE;
            return;
        }

        HouseStatus calculatedStatus;
        LocalDate visitDate = this.visitDateTime.toLocalDate();

        if (today.isBefore(visitDate)) {
            calculatedStatus = HouseStatus.BEFORE;
        } else {
            calculatedStatus = HouseStatus.IN_PROGRESS;
        }

        this.status = calculatedStatus;
    }
}