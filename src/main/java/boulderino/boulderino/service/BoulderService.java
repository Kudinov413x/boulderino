package boulderino.boulderino.service;

import boulderino.boulderino.dto.BoulderCreateDTO;
import boulderino.boulderino.dto.BoulderUpdateDTO;
import boulderino.boulderino.dto.BoulderResponseDTO;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.User;
import boulderino.boulderino.repository.AttemptsRepository;
import boulderino.boulderino.repository.BoulderAttemptsStats;
import boulderino.boulderino.repository.BoulderRepository;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BoulderService {

    private final BoulderRepository boulderRepository;
    private final AttemptsRepository attemptsRepository;

    public BoulderService(BoulderRepository boulderRepository, AttemptsRepository attemptsRepository) {

        this.boulderRepository = boulderRepository;
        this.attemptsRepository = attemptsRepository;
    }

    // Alle Boulder eines Users
    public List<BoulderResponseDTO> getAllBouldersByUser(User user) {

        List<Boulder> boulders = boulderRepository.findByUser(user);

        if (boulders.isEmpty()) {
            return List.of();
        }

        // Statistik für alle Boulder mit einer Query laden
        Map<Long, BoulderAttemptsStats> statsByBoulderId = attemptsRepository
                        .getStatsForBoulders(boulders)
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        BoulderAttemptsStats::getBoulderId,
                                        stats -> stats
                                )
                        );

        return boulders.stream().map(boulder ->
                        toResponseDTO(
                                boulder,
                                statsByBoulderId.get(boulder.getId())
                        )
                ).toList();
    }

    // Boulder eines Users anhand der ID finden
    public BoulderResponseDTO getBoulderById(Long id, User user) {

        Boulder boulder = boulderRepository.findById(id)
                .filter(b -> b.getUser().equals(user))
                .orElseThrow();

        BoulderAttemptsStats stats = attemptsRepository
                        .getStatsForBoulders(List.of(boulder))
                        .stream()
                        .findFirst()
                        .orElse(null);

        return toResponseDTO(boulder, stats);
    }

    // Neuen Boulder für einen User erstellen
    public Boulder createBoulder(BoulderCreateDTO request, User user) {

        Boulder boulder = new Boulder();

        boulder.setName(request.getName());
        boulder.setGrade(request.getGrade());
        boulder.setWallAngle(request.getWallAngle());
        boulder.setGripType(request.getGripType());
        boulder.setRouteCharacter(request.getRouteCharacter());
        boulder.setClimbingStyle(request.getClimbingStyle());
        boulder.setUser(user);

        return boulderRepository.save(boulder);
    }

    // Boulder eines Users ändern
    public BoulderResponseDTO updateBoulder(Long id, BoulderUpdateDTO request, User user) {

        Boulder existingBoulder = getBoulderEntityById(id, user);
        
        if (request.getName() != null && !request.getName().isBlank()) {
            existingBoulder.setName(request.getName());
        }

        if (request.getGrade() != null) {
            existingBoulder.setGrade(request.getGrade());
        }

        if (request.getWallAngle() != null) {
            existingBoulder.setWallAngle(request.getWallAngle());
        }

        if (request.getGripType() != null) {
            existingBoulder.setGripType(request.getGripType());
        }

        if (request.getRouteCharacter() != null) {
            existingBoulder.setRouteCharacter(
                    request.getRouteCharacter()
            );
        }

        if (request.getClimbingStyle() != null) {
            existingBoulder.setClimbingStyle(
                    request.getClimbingStyle()
            );
        }

        Boulder updatedBoulder = boulderRepository.save(existingBoulder);

        return toResponseDTO(updatedBoulder, null);
    }

    // Boulder eines Users löschen
    @Transactional
    public void deleteBoulder(Long id, User user) {

        Boulder existingBoulder = getBoulderEntityById(id, user);

        boulderRepository.delete(existingBoulder);
    }

    // Entity -> ResponseDTO
    private BoulderResponseDTO toResponseDTO(Boulder boulder, BoulderAttemptsStats stats) {
        BoulderResponseDTO response = new BoulderResponseDTO();

        // Grunddaten
        response.setId(boulder.getId());
        response.setName(boulder.getName());
        response.setGrade(boulder.getGrade());
        response.setWallAngle(boulder.getWallAngle());
        response.setGripType(boulder.getGripType());
        response.setRouteCharacter(boulder.getRouteCharacter());
        response.setClimbingStyle(boulder.getClimbingStyle());

        // Boulder-Status
        response.setFirstAscent(Boolean.TRUE.equals(boulder.getFirstAscent()));
        response.setFirstAscentDate(boulder.getFirstAscentDate());
        response.setFlashed(Boolean.TRUE.equals(boulder.getFlashed()));

        // Attempts-Statistik
        if (stats != null) {

            response.setTotalTries(stats.getTotalTries() != null ? stats.getTotalTries() : 0 );

            response.setTotalTops(stats.getTotalTops() != null ? stats.getTotalTops().intValue() : 0 );

            response.setTotalZones(stats.getTotalZones() != null ? stats.getTotalZones().intValue() : 0 );

        }
        else {
            response.setTotalTries(0);
            response.setTotalTops(0);
            response.setTotalZones(0);
        }
        return response;
    }

    // Hilfsmethode für Operationen, die die Entity benötigen
    private Boulder getBoulderEntityById(Long id, User user) {

        return boulderRepository.findById(id)
                .filter(
                        boulder ->
                                boulder.getUser().equals(user)
                )
                .orElseThrow();
    }
}