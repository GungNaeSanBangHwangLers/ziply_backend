package ziply.review.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "search_cards")
public class SearchCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "searchCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasePoint> basePoints = new ArrayList<>();

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private SearchCardStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "searchCard")
    private List<House> houses = new ArrayList<>();

    private String pastAddress;
    private Double pastLatitude;
    private Double pastLongitude;

    @ElementCollection
    @CollectionTable(name = "past_advantages", joinColumns = @JoinColumn(name = "search_card_id"))
    @Column(name = "advantage")
    private List<String> pastAdvantages = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "past_disadvantages", joinColumns = @JoinColumn(name = "search_card_id"))
    @Column(name = "disadvantage")
    private List<String> pastDisadvantages = new ArrayList<>();

    @Builder
    public SearchCard(Long userId, String title, LocalDate startDate, LocalDate endDate,
                      String pastAddress, Double pastLatitude, Double pastLongitude,
                      List<String> pastAdvantages, List<String> pastDisadvantages) {
        this.userId = userId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
        this.status = SearchCardStatus.PLANNED;

        this.pastAddress = pastAddress;
        this.pastLatitude = pastLatitude;
        this.pastLongitude = pastLongitude;

        this.pastAdvantages = (pastAdvantages != null) ? pastAdvantages : new ArrayList<>();
        this.pastDisadvantages = (pastDisadvantages != null) ? pastDisadvantages : new ArrayList<>();
    }

    public void addBasePoint(BasePoint basePoint) {
        this.basePoints.add(basePoint);
        basePoint.assignSearchCard(this);
    }

    public void syncStatus(LocalDate today) {
        if (this.startDate == null || this.endDate == null) return;

        SearchCardStatus calculatedStatus;

        if (today.isBefore(this.startDate)) {
            calculatedStatus = SearchCardStatus.PLANNED;
        } else if (today.isAfter(this.endDate)) {
            calculatedStatus = SearchCardStatus.COMPLETED;
        } else {
            calculatedStatus = SearchCardStatus.IN_PROGRESS;
        }

        this.status = calculatedStatus;
    }
}