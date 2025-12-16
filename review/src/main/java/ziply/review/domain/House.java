package ziply.review.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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

    private LocalDateTime visitDateTime;

    private Double latitude;
    private Double longitude;

    @Builder
    public House(SearchCard searchCard, String address, LocalDateTime visitDateTime, Double latitude, Double longitude) {
        this.searchCard = searchCard;
        this.address = address;
        this.visitDateTime = visitDateTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void assignSearchCard(SearchCard searchCard) {
        this.searchCard = searchCard;
    }
}