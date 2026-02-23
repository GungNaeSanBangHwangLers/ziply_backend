package ziply.review.domain;

import jakarta.persistence.*;
import java.util.UUID;
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

    public SearchCard(Long userId, String title, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = LocalDateTime.now();
        this.status = SearchCardStatus.PLANNED;
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