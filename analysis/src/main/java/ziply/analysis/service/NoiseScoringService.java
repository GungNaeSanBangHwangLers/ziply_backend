package ziply.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NoiseScoringService {

    public int calculateNoiseScore(Double latitude, Double longitude) {
        int dayScore = (int) (Math.random() * 31) + 60;
        return dayScore;
    }


}