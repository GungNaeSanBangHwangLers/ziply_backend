package ziply.review.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "houses")
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_card_id")
    private SearchCard searchCard;

    private String address;

    @Enumerated(EnumType.STRING)
    private HouseStatus status = HouseStatus.BEFORE;

    private LocalDateTime visitDateTime;

    private Double latitude;
    private Double longitude;

    // === [추가] Measurement와의 1:N 연관관계 설정 ===
    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Measurement> measurements = new ArrayList<>();

    @Builder
    public House(SearchCard searchCard, String address, LocalDateTime visitDateTime, Double latitude,
                 Double longitude) {
        this.searchCard = searchCard;
        this.address = address;
        this.visitDateTime = visitDateTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // === [추가] 상태 변경을 위한 Setter 성격의 메서드 ===
    public void updateStatus(HouseStatus status) {
        this.status = status;
    }

    // === [추가] 연관관계 편의 메서드 (객체 양방향 연결용) ===
    public void addMeasurement(Measurement measurement) {
        this.measurements.add(measurement);
        measurement.assignHouse(this);
    }

    public void assignSearchCard(SearchCard searchCard) {
        this.searchCard = searchCard;
    }

    public void update(String address, LocalDateTime visitDateTime) {
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
        if (visitDateTime != null) {
            this.visitDateTime = visitDateTime;
        }
    }

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