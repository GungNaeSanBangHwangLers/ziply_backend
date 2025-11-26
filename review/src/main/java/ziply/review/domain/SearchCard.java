package ziply.review.domain;

import jakarta.persistence.*;
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public SearchCard(Long userId, String title, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = SearchCardStatus.IN_PROGRESS;
    }

    public void addBasePoint(BasePoint basePoint) {
        this.basePoints.add(basePoint);
        basePoint.assignSearchCard(this);
    }
}