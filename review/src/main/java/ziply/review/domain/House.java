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

    @Builder
    public House(SearchCard searchCard, String address, LocalDateTime visitDateTime) {
        this.searchCard = searchCard;
        this.address = address;
        this.visitDateTime = visitDateTime;
    }

    public void assignSearchCard(SearchCard searchCard) {
        this.searchCard = searchCard;
    }
}